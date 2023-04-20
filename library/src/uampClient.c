/*
 * Copyright (c) 2009-2023 Ryan Vogt <rvogt@ualberta.ca>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION
 * OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

#define _ISOC99_SOURCE

#include "uampClient.h"

#include "errors.h"
#include "ioBuffer.h"
#include "queues.h"
#include "socketWrapper.h"
#include "states.h"

#include <limits.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

/*
 * We only support a single version of the UAMP/MVISP protocol: version 2.
 */
#define SUPPORTED_VERSION ((uint8_t)0x80)

/*
 * Performs the initial two-byte handshake between UAMP client and UAMP server,
 * or MVISP client and MVISP server.  Returns 0 on success or a negative number
 * on error.
 */
#define HANDSHAKE_UAMP (1)
#define HANDSHAKE_MVISP (0)
static int performHandshake(struct uampClient *client, int isUAMP,
                            uint32_t supportedFeatures);

void uampInitialize(struct uampClient *client) {
  client->fd = -1;
  client->agents = NULL;
  client->numChanges = 0;
}

int uampConnect(struct uampClient *client, const char *hostname,
                unsigned short port, int numAgents, double timeLimit,
                long seed, uint32_t supportedFeatures) {
  int ret;
  uint8_t response;
  int wasErr = 0;

  /* Enable uampTerminate to be called, then verify user input */
  uampInitialize(client);
  if (numAgents <= 0 || numAgents > UINT32_MAX)
    ERROR(isErr, wasErr, ERROR_INVALID_NUM_AGENTS);
  if (timeLimit < 0.0 || timeLimit > UAMP_MAX_TIME)
    ERROR(isErr, wasErr, ERROR_INVALID_TIME_LIMIT);

  /* Allocate memory and set numAgents, timeLimit, and numStates */
  client->agents =
      (struct uampAgent *)calloc(numAgents, sizeof(struct uampAgent));
  if (client->agents == NULL)
    ERROR(isErr, wasErr, ERROR_OUT_OF_MEMORY);
  client->numAgents = (uint32_t)numAgents;
  client->timeLimit = (uint32_t)llround(timeLimit * 1000.0);
  client->numStates = (uint32_t)0;

  /* Connect to the UAMP server and do the initial handshake */
  client->fd = callSocket(hostname, port);
  ERROR_CHECK(isErr, wasErr, client->fd);
  ret = performHandshake(client, HANDSHAKE_UAMP, supportedFeatures);
  ERROR_CHECK(isErr, wasErr, ret);

  /* Send the simulation request */
  beginWrite(&(client->commBuf), sizeof(uint32_t) * 3);
  ret = socketWrite32(&(client->commBuf), client->fd, client->numAgents);
  ERROR_CHECK(isErr, wasErr, ret);
  ret = socketWrite32(&(client->commBuf), client->fd, client->timeLimit);
  ERROR_CHECK(isErr, wasErr, ret);
  ret = socketWrite32(&(client->commBuf), client->fd, (uint32_t)seed);
  ERROR_CHECK(isErr, wasErr, ret);

  /* Read the reply */
  ret = socketRead(client->fd, &response, sizeof(uint8_t));
  ERROR_CHECK(isErr, wasErr, ret);
  if (response == (uint8_t)0x01)
    ERROR(isErr, wasErr, ERROR_SIMULATION_DENIED);
  else if (response != (uint8_t)0x00)
    ERROR(isErr, wasErr, ERROR_SIMULATION_RESPONSE_BAD);

  /* Read initial locations from server */
  client->smallestCurrentTime = client->largestLastTime = (uint32_t)0;
  ret = initializeQueues(client);
  ERROR_CHECK(isErr, wasErr, ret);

isErr:
  if (wasErr) {
    if (client->fd >= 0)
      close(client->fd);
    client->fd = -1;
    if (client->agents != NULL) {
      free(client->agents);
      client->agents = NULL;
    }
  }
  return wasErr;
}

int mvispConnect(struct uampClient *client, const char *hostname,
                 unsigned short port, int *numAgents, double *timeLimit,
                 const char **stateNames, int numStates,
                 mvispCallback acceptFunc, uint32_t supportedFeatures) {
  uint32_t naInput, tlInput;
  uint32_t *nameLengths = NULL;
  int ret, na;
  double tl;
  int wasErr = 0;

  /* Enable uampTerminate to be called, then verify user input */
  uampInitialize(client);
  ret = verifyStates(stateNames, numStates, &nameLengths);
  ERROR_CHECK(isErr, wasErr, ret);

  /* Connect to the MVISP server and do the initial handshake */
  client->fd = callSocket(hostname, port);
  ERROR_CHECK(isErr, wasErr, client->fd);
  ret = performHandshake(client, HANDSHAKE_MVISP, supportedFeatures);
  ERROR_CHECK(isErr, wasErr, ret);

  /* Read the simulation specification */
  beginRead(&(client->commBuf), sizeof(uint32_t) * 2);
  ERROR_CHECK(isErr, wasErr, ret);
  ret = socketRead32(&(client->commBuf), client->fd, &naInput);
  ERROR_CHECK(isErr, wasErr, ret);
  ret = socketRead32(&(client->commBuf), client->fd, &tlInput);
  ERROR_CHECK(isErr, wasErr, ret);
  if (naInput == 0)
    ERROR(isErr, wasErr, ERROR_MVISP_NO_AGENTS);

  /*
   * Test if we're okay with the specification.  If we're not, we send a
   * SPECIFICATION_DENIED message (the 32-bit zero value) and disconnect.
   * Here, the error handling at the end of the function will disconnect us.
   */
  na = (int)naInput;
  tl = ((double)tlInput) / 1000.0;
  if (numAgents != NULL)
    *numAgents = na;
  if (timeLimit != NULL)
    *timeLimit = tl;
  if (naInput > INT_MAX || (acceptFunc != NULL && acceptFunc(na, tl) != 0)) {
    beginWrite(&(client->commBuf), sizeof(uint32_t));
    ret = socketWrite32(&(client->commBuf), client->fd, (uint32_t)0);
    ERROR_CHECK(isErr, wasErr, ret);
    ERROR(isErr, wasErr, ERROR_SIMULATION_DENIED);
  }

  /* Allocate memory and set numAgents, timeLimit, and numStates */
  client->agents = (struct uampAgent *)calloc(na, sizeof(struct uampAgent));
  if (client->agents == NULL)
    ERROR(isErr, wasErr, ERROR_OUT_OF_MEMORY);
  client->numAgents = naInput;
  client->timeLimit = tlInput;
  client->numStates = (uint32_t)numStates;

  /* Send the state specification message and read initial locations */
  ret = writeStates(client, stateNames, numStates, nameLengths);
  ERROR_CHECK(isErr, wasErr, ret);
  client->smallestCurrentTime = client->largestLastTime = (uint32_t)0;
  ret = initializeQueues(client);
  ERROR_CHECK(isErr, wasErr, ret);

isErr:
  if (wasErr) {
    if (client->fd >= 0)
      close(client->fd);
    client->fd = -1;
    if (client->agents != NULL) {
      free(client->agents);
      client->agents = NULL;
    }
  }
  if (nameLengths != NULL)
    free(nameLengths);
  return wasErr;
}

int uampTerminate(struct uampClient *client) {
  int ret;
  int wasErr = 0;

  /*
   * If we are connected, flush any outstanding state changes then send the
   * termination command.
   */
  if (client->fd >= 0) {
    if (client->numChanges != 0) {
      ret = flushStateChanges(client);
      ERROR_CHECK(isErr, wasErr, ret);
    }

    beginWrite(&(client->commBuf), sizeof(uint8_t) + sizeof(uint32_t));
    ret = socketWrite8(&(client->commBuf), client->fd, (uint8_t)0x00);
    ERROR_CHECK(isErr, wasErr, ret);
    ret = socketWrite32(&(client->commBuf), client->fd, (uint32_t)0);
    ERROR_CHECK(isErr, wasErr, ret);
  }

isErr:
  if (client->fd >= 0) {
    close(client->fd);
    client->fd = -1;
  }
  if (client->agents != NULL) {
    free(client->agents);
    client->agents = NULL;
  }
  return wasErr;
}

void uampCurrentCommand(struct uampClient *client, int agentID,
                        struct uampCommand *command) {
  struct uampUpdate *last, *current;

  ASSERT(agentID >= 0 && agentID < client->numAgents, "Invalid agent ID");
  last = getPreviousUpdate(client, agentID);
  current = getCurrentUpdate(client, agentID);

  command->agentID = agentID;
  command->fromX = ((double)(last->x)) / 1000.0;
  command->fromY = ((double)(last->y)) / 1000.0;
  command->fromZ = ((double)(last->z)) / 1000.0;
  command->fromTime = ((double)(last->time)) / 1000.0;
  command->toX = ((double)(current->x)) / 1000.0;
  command->toY = ((double)(current->y)) / 1000.0;
  command->toZ = ((double)(current->z)) / 1000.0;
  command->toTime = ((double)(current->time)) / 1000.0;
  command->present = (int)(last->present);
}

int uampIntersectCommand(struct uampClient *client, int agentID,
                         struct uampCommand *command) {
  struct uampUpdate *last, *current;
  double deltaX, deltaY, deltaZ, deltaT, frac;

  /* Ensure that the current state of the command buffer allows for this call
   */
  ASSERT(agentID >= 0 && agentID < client->numAgents, "Invalid agent ID");
  if (client->largestLastTime > client->smallestCurrentTime)
    return ERROR_NO_INTERSECTION;
  last = getPreviousUpdate(client, agentID);
  current = getCurrentUpdate(client, agentID);

  command->agentID = agentID;

  /*
   * currentTime[agentID] > lastTime[agentID], unless currentTime[agentID] == 0
   * (which happens if we have never advanced).  In this case,
   * smallestCurrentTime == 0; and, by the above assertion,
   * largestLastTime <= smallestCurrentTime = 0, so largestLastTime = 0.
   */
  if (current->time == 0) {
    command->fromTime = command->toTime = 0.0;
    command->fromX = command->toX = ((double)(current->x)) / 1000.0;
    command->fromY = command->toY = ((double)(current->y)) / 1000.0;
    command->fromZ = command->toZ = ((double)(current->z)) / 1000.0;
    command->present = (int)(current->present);
    return 0;
  }

  /*
   * If we reach here, we are guaranteed that
   * currentTime[agentID] > lastTime[agentID], so we can interpolate between
   * these times.
   */
  deltaX = ((double)(current->x)) - ((double)(last->x));
  deltaY = ((double)(current->y)) - ((double)(last->y));
  deltaZ = ((double)(current->z)) - ((double)(last->z));
  deltaT = ((double)(current->time)) - ((double)(last->time));

  /* Compute the "from" interpolation */
  command->fromTime = ((double)(client->largestLastTime)) / 1000.0;
  frac =
      (((double)(client->largestLastTime)) - ((double)(last->time))) / deltaT;
  command->fromX = (((double)(last->x)) + (frac * deltaX)) / 1000.0;
  command->fromY = (((double)(last->y)) + (frac * deltaY)) / 1000.0;
  command->fromZ = (((double)(last->z)) + (frac * deltaZ)) / 1000.0;

  /* Compute the "to" interpolation */
  command->toTime = ((double)(client->smallestCurrentTime)) / 1000.0;
  frac = (((double)(client->smallestCurrentTime)) - ((double)(last->time))) /
         deltaT;
  command->toX = (((double)(last->x)) + (frac * deltaX)) / 1000.0;
  command->toY = (((double)(last->y)) + (frac * deltaY)) / 1000.0;
  command->toZ = (((double)(last->z)) + (frac * deltaZ)) / 1000.0;

  command->present = (int)(last->present);
  return 0;
}

int uampIsMore(struct uampClient *client, int agentID) {
  struct uampUpdate *current;

  ASSERT(agentID >= 0 && agentID < client->numAgents, "Invalid agent ID");

  current = getCurrentUpdate(client, agentID);
  if (current->time < client->timeLimit)
    return 1;
  else
    return 0;
}

int uampAdvance(struct uampClient *client, int agentID) {
  struct uampUpdate *update;
  uint32_t i, limit;
  int ret;
  int wasErr = 0;

  /*
   * Check that this call is legal.  We save the reference to the current
   * update, as it will shortly become the previous update.
   */
  ASSERT(agentID >= 0 && agentID < client->numAgents, "Invalid agent ID");
  update = getCurrentUpdate(client, agentID);
  if (update->time == client->timeLimit)
    ERROR(isErr, wasErr, ERROR_NO_MORE_DATA);

  /* Advance the underlying buffer to the next update */
  ret = advanceAgent(client, agentID);
  ERROR_CHECK(isErr, wasErr, ret);

  /*
   * Check if we need to update our client-wide cached times.  Note that update
   * now refers to the agent's previous update.
   */
  if (update->time > client->largestLastTime)
    client->largestLastTime = update->time;
  if (update->time == client->smallestCurrentTime) {
    limit = UINT32_MAX;
    for (i = 0; i < client->numAgents; i++) {
      update = getCurrentUpdate(client, i);
      if (update->time < limit)
        limit = update->time;
    }
    client->smallestCurrentTime = limit;
  }

isErr:
  return wasErr;
}

int uampIsAnyMore(struct uampClient *client) {
  if (client->smallestCurrentTime < client->timeLimit)
    return 1;
  else
    return 0;
}

int uampAdvanceOldest(struct uampClient *client) {
  int ret;
  uint32_t i;
  uint32_t oldest = client->smallestCurrentTime;
  struct uampUpdate *current;
  int wasErr = 0;

  if (oldest == client->timeLimit)
    ERROR(isErr, wasErr, ERROR_NO_MORE_DATA);

  for (i = 0; i < client->numAgents; i++) {
    current = getCurrentUpdate(client, i);
    if (current->time == oldest) {
      ret = uampAdvance(client, (int)i);
      ERROR_CHECK(isErr, wasErr, ret);
    }
  }

isErr:
  return wasErr;
}

int uampChangeState(struct uampClient *client, int agentID, double atTime,
                    int newState) {
  uint32_t sendTime;
  int ret;
  int wasErr = 0;

  /* UAMP clients ignore this function */
  if (client->numStates == 0)
    return 0;

  /* Convert the time to milliseconds */
  if (atTime < 0.0 || atTime > UAMP_MAX_TIME)
    ERROR(isErr, wasErr, ERROR_INVALID_CHANGE_TIME);
  sendTime = (uint32_t)llround(atTime * 1000.0);

  /* Verify parameter sanity */
  ASSERT(agentID >= 0 && agentID < client->numAgents, "Invalid agent ID");
  if (sendTime > client->timeLimit)
    ERROR(isErr, wasErr, ERROR_INVALID_CHANGE_TIME);
  if (newState < 0 || newState >= client->numStates)
    ERROR(isErr, wasErr, ERROR_INVALID_CHANGE_STATE);

  /* Add the state change to the cache of changes to send */
  ret =
      addStateChange(client, (uint32_t)agentID, sendTime, (uint32_t)newState);
  ERROR_CHECK(isErr, wasErr, ret);

isErr:
  return wasErr;
}

const char *uampError(int returnValue) { return returnToString(returnValue); }

static int performHandshake(struct uampClient *client, int isUAMP,
                            uint32_t supportedFeatures) {
  uint8_t id[4];
  uint8_t ver;
  int ret;
  int sendReject = 0;
  int wasErr = 0;

  /* Sanity check here on supportedFeatures */
  if ((~(UAMP_SUPPORTS_3D | UAMP_SUPPORTS_ADD_REMOVE)) & supportedFeatures)
    ERROR(isErr, wasErr, ERROR_INVALID_FEATURES);

  /* Send our identification string */
  beginWrite(&(client->commBuf), 9);
  if (isUAMP)
    ret = socketWriteRaw(&(client->commBuf), client->fd, "UAMP", 4);
  else
    ret = socketWriteRaw(&(client->commBuf), client->fd, "MVIS", 4);
  ERROR_CHECK(isErr, wasErr, ret);

  /*
   * Send what versions and features we support.  Converting the
   * supportedFeatures value to network order makes it into the bit field
   * specified in the UAMP RFC.
   */
  ret = socketWrite8(&(client->commBuf), client->fd, SUPPORTED_VERSION);
  ERROR_CHECK(isErr, wasErr, ret);
  ret = socketWrite32(&(client->commBuf), client->fd, supportedFeatures);
  ERROR_CHECK(isErr, wasErr, ret);

  /* Read the server handshake bytes */
  beginRead(&(client->commBuf), 9);
  ret = socketReadRaw(&(client->commBuf), client->fd, id, 4);
  ERROR_CHECK(isErr, wasErr, ret);
  ret = socketRead8(&(client->commBuf), client->fd, &ver);
  ERROR_CHECK(isErr, wasErr, ret);
  ret =
      socketRead32(&(client->commBuf), client->fd, &(client->serverFeatures));
  ERROR_CHECK(isErr, wasErr, ret);

  /* Verify that the identification string matches (i.e., UAMP vs. MVISP) */
  if (isUAMP) {
    if (memcmp(id, "MVIS", 4) == 0) {
      sendReject = 1;
      ERROR(isErr, wasErr, ERROR_UAMP_CLIENT_MVISP_SERVER);
    } else if (memcmp(id, "UAMP", 4) != 0) {
      sendReject = 1;
      ERROR(isErr, wasErr, ERROR_SERVER_UNKNOWN_HANDSHAKE);
    }
  } else {
    if (memcmp(id, "UAMP", 4) == 0) {
      sendReject = 1;
      ERROR(isErr, wasErr, ERROR_MVISP_CLIENT_UAMP_SERVER);
    } else if (memcmp(id, "MVIS", 4) != 0) {
      sendReject = 1;
      ERROR(isErr, wasErr, ERROR_SERVER_UNKNOWN_HANDSHAKE);
    }
  }

  /* Check that the server supports some common version of UAMP/MVISP to us */
  if (!(ver & SUPPORTED_VERSION)) {
    sendReject = 1;
    ERROR(isErr, wasErr, ERROR_NO_SHARED_VERSION);
  }

  /* Check what additional data the server will send */
  if (((client->serverFeatures) & UAMP_SUPPORTS_3D) &&
      !(supportedFeatures & UAMP_SUPPORTS_3D)) {
    sendReject = 1;
    ERROR(isErr, wasErr, ERROR_2D_CLIENT_3D_SERVER);
  } else if (((client->serverFeatures) & UAMP_SUPPORTS_ADD_REMOVE) &&
             !(supportedFeatures & UAMP_SUPPORTS_ADD_REMOVE)) {
    sendReject = 1;
    ERROR(isErr, wasErr, ERROR_ADD_REMOVE_UNSUPPORTED);
  }

  /*
   * Send the VERSION_CHOICE message.  Since we only support a single version,
   * the version choice message is identical to the versions supported message.
   */
  ver = SUPPORTED_VERSION;
  ret = socketWrite(client->fd, &ver, sizeof(uint8_t));
  ERROR_CHECK(isErr, wasErr, ret);

  /* Receive the VERSION_CHOICE message from the client */
  ret = socketRead(client->fd, &ver, sizeof(uint8_t));
  ERROR_CHECK(isErr, wasErr, ret);
  if (ver == 0)
    ERROR(isErr, wasErr, ERROR_SERVER_REJECTED_HANDSHAKE);
  else if (ver != SUPPORTED_VERSION)
    ERROR(isErr, wasErr, ERROR_SERVER_CLIENT_VERSION_DISAGREE);

isErr:
  if (sendReject) {
    ver = (uint8_t)0x00;
    socketWrite(client->fd, &ver, sizeof(uint8_t));
  }
  return wasErr;
}
