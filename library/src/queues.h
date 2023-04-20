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

#ifndef __QUEUES_H__
#define __QUEUES_H__

#include "uampClient.h"

/*
 * Fills the update queues by requesting the initial position update for each
 * agent, plus subsequent updates to completely fill each queue.  Returns 0 on
 * success or a negative value on error.
 */
int initializeQueues(struct uampClient *client);

/*
 * Advances the given agent to the next update, updating the agent's queue and
 * requesting additional data from the server as necessary.  Returns 0 on
 * success or a negative value on error.
 */
int advanceAgent(struct uampClient *client, int agentID);

/*
 * Returns a pointer to the current uampUpdate for the given agent.
 */
struct uampUpdate *getCurrentUpdate(struct uampClient *client, int agentID);

/*
 * Returns a pointer to the previous uampUpdate for the given agent.
 */
struct uampUpdate *getPreviousUpdate(struct uampClient *client, int agentID);

#endif
