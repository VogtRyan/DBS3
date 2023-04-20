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

#include "states.h"

#include "errors.h"
#include "ioBuffer.h"

#include <string.h>

/*
 * The maximum length of a state name, not including the terminating NULL
 * character.  An arbitrary value, but included for sanity purposes and to
 * ensure that we don't overflow the uint64_t used by the write buffer.
 */
#define MAX_NAME_LEN (1024)

/*
 * Saves the length of the given string into len.  Returns 0 on success or a
 * negative value if the length of the string is 0 or if it is greater than
 * MAX_NAME_LEN.
 */
static int stateNameLength(const char *s, uint32_t *len);

int verifyStates(const char **stateNames, int numStates,
                 uint32_t **nameLengths) {
  int onState, prev, ret;
  uint32_t *lens = NULL;
  int wasErr = 0;

  /* Allocate memory to store the length of each state */
  if (numStates <= 0 || numStates > UINT32_MAX)
    ERROR(isErr, wasErr, ERROR_INVALID_NUMBER_STATES);
  lens = (uint32_t *)calloc(numStates, sizeof(uint32_t));
  if (lens == NULL)
    ERROR(isErr, wasErr, ERROR_OUT_OF_MEMORY);

  for (onState = 0; onState < numStates; onState++) {
    /* Verify valid length for each state */
    ret = stateNameLength(stateNames[onState], lens + onState);
    ERROR_CHECK(isErr, wasErr, ret);

    /* Verify it is not a duplicate */
    for (prev = 0; prev < onState; prev++) {
      if (lens[onState] != lens[prev])
        continue;
      if (memcmp(stateNames[onState], stateNames[prev], lens[onState]) == 0)
        ERROR(isErr, wasErr, ERROR_DUPLICATE_STATE);
    }
  }

isErr:
  if (wasErr && lens != NULL) {
    free(lens);
    lens = NULL;
  }
  *nameLengths = lens;
  return wasErr;
}

int writeStates(struct uampClient *client, const char **stateNames,
                int numStates, uint32_t *nameLengths) {
  int onState, ret;
  uint64_t totalLen;
  int wasErr = 0;

  /*
   * We write the number of states, followed by the length of each state,
   * followed by the characters that make up the names of each state. Because
   * of the MAX_NAME_LEN limit on the length of each state, and because the
   * number of states must fit within a uint32_t, the total amount of data to
   * be written is guaranteed to fit in a uint64_t.
   */
  totalLen = ((uint64_t)sizeof(uint32_t)) +
             ((uint64_t)sizeof(uint32_t)) * ((uint64_t)numStates);
  for (onState = 0; onState < numStates; onState++)
    totalLen += (uint64_t)(nameLengths[onState]);
  beginWrite(&(client->commBuf), totalLen);

  /* Write the number of states */
  ret = socketWrite32(&(client->commBuf), client->fd, (uint32_t)numStates);
  ERROR_CHECK(isErr, wasErr, ret);

  /* Write the lengths of the states */
  for (onState = 0; onState < numStates; onState++) {
    ret = socketWrite32(&(client->commBuf), client->fd,
                        (uint32_t)(nameLengths[onState]));
    ERROR_CHECK(isErr, wasErr, ret);
  }

  /* Write the characters */
  for (onState = 0; onState < numStates; onState++) {
    ret = socketWriteRaw(&(client->commBuf), client->fd, stateNames[onState],
                         nameLengths[onState]);
    ERROR_CHECK(isErr, wasErr, ret);
  }

isErr:
  return wasErr;
}

int addStateChange(struct uampClient *client, uint32_t agentID, uint32_t time,
                   uint32_t newState) {
  int ret;
  int wasErr = 0;

  /* Add the state change to the buffer */
  client->changes[client->numChanges].agentID = agentID;
  client->changes[client->numChanges].time = time;
  client->changes[client->numChanges].newState = newState;
  (client->numChanges)++;

  /* If the buffer is full, flush it */
  if (client->numChanges == UAMP_STATE_BUFFER_SIZE) {
    ret = flushStateChanges(client);
    ERROR_CHECK(isErr, wasErr, ret);
  }

isErr:
  return wasErr;
}

int flushStateChanges(struct uampClient *client) {
  int ret = 0;
  int onChange;
  uint64_t totalLen = 0;
  int wasErr = 0;

  /*
   * The total amount of data to be written: a single byte signalling the
   * start of a CHANGE_STATE message + a 32-bit integer denoting the number
   * of state changes + 3 32-bit integers per state change.  That is, 5 fixed
   * bytes plus 12 bytes per state change.
   *
   * Assuming there is any reasonable bound on the size of the state change
   * buffer (see uampClient.h), this amount of data is guaranteed to fit in
   * the uint64_t range.
   */
  totalLen = ((uint64_t)5) + ((uint64_t)12) * ((uint64_t)(client->numChanges));
  beginWrite(&(client->commBuf), totalLen);

  /* Write the fixed header */
  ret = socketWrite8(&(client->commBuf), client->fd, (uint8_t)0x02);
  ERROR_CHECK(isErr, wasErr, ret);
  ret = socketWrite32(&(client->commBuf), client->fd,
                      (uint32_t)(client->numChanges));
  ERROR_CHECK(isErr, wasErr, ret);

  /* Write all of the changes */
  for (onChange = 0; onChange < client->numChanges; onChange++) {
    ret = socketWrite32(&(client->commBuf), client->fd,
                        client->changes[onChange].agentID);
    ERROR_CHECK(isErr, wasErr, ret);
    ret = socketWrite32(&(client->commBuf), client->fd,
                        client->changes[onChange].time);
    ERROR_CHECK(isErr, wasErr, ret);
    ret = socketWrite32(&(client->commBuf), client->fd,
                        client->changes[onChange].newState);
    ERROR_CHECK(isErr, wasErr, ret);
  }

  /* The state change buffer is now flushed */
  client->numChanges = 0;

isErr:
  return wasErr;
}

static int stateNameLength(const char *s, uint32_t *len) {
  uint32_t l = 0;

  /* Determine the length l, up to MAX_NAME_LEN+1 */
  while (l <= MAX_NAME_LEN) {
    if (*s == '\0')
      break;
    s++;
    l++;
  }

  /* Verify correctness of length */
  if (l == 0)
    return ERROR_ZERO_STATE_LENGTH;
  if (l > MAX_NAME_LEN)
    return ERROR_STATE_LENGTH_LONG;

  *len = l;
  return 0;
}
