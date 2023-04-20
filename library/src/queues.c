/*
 * Copyright (c) 2010-2023 Ryan Vogt <rvogt@ualberta.ca>
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

#include "queues.h"

#include "errors.h"
#include "ioBuffer.h"
#include "uampClient.h"

#include <stdint.h>
#include <string.h>

/*
 * Requests data from the server to fill all of the empty spaces in the update
 * queues.  Returns 0 on success or a negative value on an error.
 */
static int fillUpdateQueues(struct uampClient *client);

/*
 * A worker function for fillUpdateQueues(), which actually does the work.
 * Because of possible integer overflow issues, fillUpdateQueues() has to loop
 * over the actual work of requesting.  Returns 0 on success or a negative
 * value on error.
 */
static int requestUpdates(struct uampClient *client, int startAgent,
                          uint32_t totalRequests);

/*
 * Returns the number of updates to be requested for the given agent.
 */
static int numToRequest(struct uampClient *client, int agentID);

/*
 * Receives and verifies a location reply from a UAMP or MVISP server.
 * Returns 0 on success or a negative value on error.
 */
static int receiveReply(struct uampClient *client, struct uampAgent *agent);

int initializeQueues(struct uampClient *client) {
  return fillUpdateQueues(client);
}

int advanceAgent(struct uampClient *client, int agentID) {
  /*
   * Advance the pointer, refilling queues if necessary.  Note that the count
   * of how many elements are alive (i.e., could be referenced now or later) in
   * the queue includes the previous update.
   */
  struct uampAgent *agent = (client->agents) + agentID;
  if (agent->updates[agent->currentIndex].time != 0)
    (agent->aliveInQueue)--;
  if (agent->currentIndex == UAMP_UPDATE_QUEUE_SIZE - 1)
    agent->currentIndex = 0;
  else
    (agent->currentIndex)++;
  if (agent->aliveInQueue == 1)
    return fillUpdateQueues(client);
  return 0;
}

struct uampUpdate *getCurrentUpdate(struct uampClient *client, int agentID) {
  struct uampAgent *agent = (client->agents) + agentID;
  return (agent->updates) + (agent->currentIndex);
}

struct uampUpdate *getPreviousUpdate(struct uampClient *client, int agentID) {
  struct uampAgent *agent = (client->agents) + agentID;
  int prevIndex;

  if (agent->updates[agent->currentIndex].time == 0)
    prevIndex = agent->currentIndex;
  else if (agent->currentIndex == 0)
    prevIndex = UAMP_UPDATE_QUEUE_SIZE - 1;
  else
    prevIndex = agent->currentIndex - 1;

  return (agent->updates) + prevIndex;
}

static int fillUpdateQueues(struct uampClient *client) {
  uint32_t totalRequests, requestsForAgent, sum;
  int startAgent, onAgent, ret;
  int wasErr = 0;

  /*
   * The number of agents must fit in a uint32_t, but each agent can require
   * more than one update to fill its buffer.  As such, the number of updates
   * required may (in extreme cases) be larger than a uint32_t.  This may
   * require multiple LOCATION_REQUEST messages be sent to the server.
   */
  startAgent = 0;
  totalRequests = 0;
  for (onAgent = 0; onAgent < client->numAgents; onAgent++) {
    requestsForAgent = numToRequest(client, onAgent);
    sum = totalRequests + requestsForAgent;
    if (sum < totalRequests || sum < requestsForAgent) {
      ret = requestUpdates(client, startAgent, totalRequests);
      ERROR_CHECK(isErr, wasErr, ret);
      startAgent = onAgent;
      totalRequests = requestsForAgent;
    } else
      totalRequests = sum;
  }

  if (totalRequests != 0) {
    ret = requestUpdates(client, startAgent, totalRequests);
    ERROR_CHECK(isErr, wasErr, ret);
  }

isErr:
  return wasErr;
}

static int requestUpdates(struct uampClient *client, int startAgent,
                          uint32_t totalRequests) {
  uint64_t totalWrite, totalRead;
  uint32_t onRequest;
  int onAgent, requestsForAgent, ret, i;
  int wasErr = 0;

  /*
   * We write a single byte to request locations, 4 bytes for the number of
   * requests, and an agent ID for each request.  That is, 5 bytes plus four
   * bytes per request.  We read 16 bytes per request (time, x, y, z), or 12
   * if the server is only sending 2D data.  There's an extra byte per request
   * if the server sends addition and removal data.
   */
  totalWrite = ((uint64_t)5) + ((uint64_t)4) * ((uint64_t)totalRequests);
  if ((client->serverFeatures) & UAMP_SUPPORTS_3D)
    totalRead = ((uint64_t)16) * ((uint64_t)totalRequests);
  else
    totalRead = ((uint64_t)12) * ((uint64_t)totalRequests);
  if ((client->serverFeatures) & UAMP_SUPPORTS_ADD_REMOVE)
    totalRead += (uint64_t)totalRequests;

  /* Send the requests */
  beginWrite(&(client->commBuf), totalWrite);
  ret = socketWrite8(&(client->commBuf), client->fd, (uint8_t)0x01);
  ERROR_CHECK(isErr, wasErr, ret);
  ret = socketWrite32(&(client->commBuf), client->fd, totalRequests);
  ERROR_CHECK(isErr, wasErr, ret);
  onAgent = startAgent;
  onRequest = 0;
  while (onRequest < totalRequests) {
    requestsForAgent = numToRequest(client, onAgent);
    for (i = 0; i < requestsForAgent; i++) {
      ret = socketWrite32(&(client->commBuf), client->fd, (uint32_t)onAgent);
      ERROR_CHECK(isErr, wasErr, ret);
    }
    onRequest += requestsForAgent;
    onAgent++;
  }

  /* Read the replies and verify the correctness of each server reply */
  beginRead(&(client->commBuf), totalRead);
  onAgent = startAgent;
  onRequest = 0;
  while (onRequest < totalRequests) {
    requestsForAgent = numToRequest(client, onAgent);
    for (i = 0; i < requestsForAgent; i++) {
      ret = receiveReply(client, client->agents + onAgent);
      ERROR_CHECK(isErr, wasErr, ret);
    }
    onRequest += requestsForAgent;
    onAgent++;
  }

isErr:
  return wasErr;
}

static int numToRequest(struct uampClient *client, int agentID) {
  struct uampAgent *agent = (client->agents) + agentID;
  if (agent->receivedFinal)
    return 0;
  else
    return (UAMP_UPDATE_QUEUE_SIZE - agent->aliveInQueue);
}

static int receiveReply(struct uampClient *client, struct uampAgent *agent) {
  struct uampUpdate *storeReply, *previousStore;
  int ret;
  int wasErr = 0;

  /* Read in the reply from the server */
  storeReply = (agent->updates) + (agent->recvIndex);
  ret = socketRead32(&(client->commBuf), client->fd, &(storeReply->time));
  ERROR_CHECK(isErr, wasErr, ret);
  ret = socketRead32(&(client->commBuf), client->fd, &(storeReply->x));
  ERROR_CHECK(isErr, wasErr, ret);
  ret = socketRead32(&(client->commBuf), client->fd, &(storeReply->y));
  ERROR_CHECK(isErr, wasErr, ret);
  if ((client->serverFeatures) & UAMP_SUPPORTS_3D) {
    ret = socketRead32(&(client->commBuf), client->fd, &(storeReply->z));
    ERROR_CHECK(isErr, wasErr, ret);
  } else
    storeReply->z = (uint32_t)0;
  if ((client->serverFeatures) & UAMP_SUPPORTS_ADD_REMOVE) {
    ret = socketRead8(&(client->commBuf), client->fd, &(storeReply->present));
    ERROR_CHECK(isErr, wasErr, ret);
  } else
    storeReply->present = (uint8_t)0x01;

  /*
   * Correctness verification:
   * - The time of the first update received must be 0
   * - If the final time has not been received, each time must be greater than
   *   the previous time
   * - Once the final time is received, every reply must be identical
   * - No time can be larger than greatest possible time
   */
  if (agent->aliveInQueue == 0) {
    if (storeReply->time != (uint32_t)0)
      ERROR(isErr, wasErr, ERROR_FIRST_UPDATE_TIME);
  } else {
    if (agent->recvIndex == 0)
      previousStore = (agent->updates) + (UAMP_UPDATE_QUEUE_SIZE - 1);
    else
      previousStore = (agent->updates) + (agent->recvIndex - 1);
    if (agent->receivedFinal) {
      if (memcmp(storeReply, previousStore, sizeof(struct uampUpdate)))
        ERROR(isErr, wasErr, ERROR_NON_EQUAL_FINAL_UPDATES);
    } else {
      if (storeReply->time <= previousStore->time)
        ERROR(isErr, wasErr, ERROR_TIMESTAMP_NOT_INCREMENTED);
      else if (storeReply->time > client->timeLimit)
        ERROR(isErr, wasErr, ERROR_TIMESTAMP_TOO_LARGE);
      if (storeReply->time == client->timeLimit)
        agent->receivedFinal = 1;
    }
  }
  if (storeReply->present != (uint8_t)0x00 &&
      storeReply->present != (uint8_t)0x01)
    ERROR(isErr, wasErr, ERROR_INVALID_PRESENT_FLAG);

  (agent->aliveInQueue)++;
  if (agent->recvIndex == UAMP_UPDATE_QUEUE_SIZE - 1)
    agent->recvIndex = 0;
  else
    (agent->recvIndex)++;

isErr:
  return wasErr;
}
