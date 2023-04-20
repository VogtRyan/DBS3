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

#ifndef __STATES_H__
#define __STATES_H__

#include "uampClient.h"

#include <stdint.h>

/*
 * Verifies that the number of states is legal, the length of each state is
 * legal, and that there are no duplicate state names.  If all of the names are
 * legal, an array of the lengths will be allocated and saved to *nameLengths.
 * The caller is responsible for freeing that memory.  Returns 0 if everything
 * is legal, or a negative number otherwise.  If there is an error, NULL will
 * be saved to *nameLengths.
 */
int verifyStates(const char **stateNames, int numStates,
                 uint32_t **nameLengths);

/*
 * Write the number of states onto the socket, followed by each state's length
 * then each state's ASCII name.  Does not perform any verification; see the
 * verifyStates function.  Returns 0 on successful write, or a negative number
 * otherwise.
 */
int writeStates(struct uampClient *client, const char **stateNames,
                int numStates, uint32_t *nameLengths);

/*
 * Adds the given state change to the queue of state changes to be sent to the
 * MVISP server, flushing all of the state changes to the server if the queue
 * becomes full.  Returns 0 on success or a negative value on error.
 */
int addStateChange(struct uampClient *client, uint32_t agentID, uint32_t time,
                   uint32_t newState);

/*
 * Flushes all of the buffered state changes to the MVISP server.  Returns 0
 * on success or a negative value on error.
 */
int flushStateChanges(struct uampClient *client);

#endif
