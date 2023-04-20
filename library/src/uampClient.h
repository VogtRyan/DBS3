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

#ifndef __UAMP_CLIENT_H__
#define __UAMP_CLIENT_H__
#ifdef __cplusplus
extern "C" {
#endif
#if 0
}
#endif

#include <stdint.h>

/*
 * The uampCommand structure represents a command for a single agent to move to
 * a given location, arriving at a given time.
 */
struct uampCommand {
  int agentID; /* The agent for which this command is intended */

  double fromX;    /* The starting X coordinate, in metres */
  double fromY;    /* The starting Y coordinate, in metres */
  double fromZ;    /* The starting Z coordinate, in metres */
  double fromTime; /* The starting time, in seconds */

  double toX;    /* The target X coordinate, in metres */
  double toY;    /* The target Y coordinate, in metres */
  double toZ;    /* The target Z coordinate, in metres */
  double toTime; /* The time at which the agent should arrive, in seconds */

  int present; /*
                * Whether the agent is present in the environment during
                * this time period.  If 0, the coordinates may be ignored.
                */
};

/*
 * The uampUpdate structure is an internal data structure representing a
 * mobility data update from a UAMP or MVISP server.
 */
struct uampUpdate {
  uint32_t time;
  uint32_t x;
  uint32_t y;
  uint32_t z;
  uint8_t present;
};

/*
 * The uampAgent structure is an internal data structure that keeps a buffer of
 * uampUpdates that have been received from the server.  The updates are stored
 * in a circular queue.  The queue size must be at least 2, since both the
 * current update and the previous update must be maintained.
 */
#define UAMP_UPDATE_QUEUE_SIZE (6)
struct uampAgent {
  struct uampUpdate updates[UAMP_UPDATE_QUEUE_SIZE];
  int currentIndex;
  int aliveInQueue;
  int recvIndex;
  int receivedFinal;
};

/*
 * The uampState structure is an internal data structure representing a state
 * change message that needs to be sent to an MVISP server.
 */
#define UAMP_STATE_BUFFER_SIZE (128)
struct uampState {
  uint32_t agentID;
  uint32_t time;
  uint32_t newState;
};

/*
 * The uampIOBuffer structure is an internal data structure used to buffer data
 * input and output on the communication socket.
 */
#define UAMP_IO_BUFFER_SIZE (2048)
struct uampIOBuffer {
  uint64_t total;
  uint64_t passed;
  int inBuffer;
  unsigned char buffer[UAMP_IO_BUFFER_SIZE];
};

/*
 * The uampClient structure contains all of the metadata required for
 * connecting to a UAMP or MVISP server.  This structure should not be modified
 * by the user; the functions below should be used instead.
 */
struct uampClient {
  int fd;
  struct uampIOBuffer commBuf;
  uint32_t serverFeatures;

  uint32_t numAgents;
  uint32_t timeLimit;
  uint32_t numStates;

  struct uampAgent *agents;
  uint32_t largestLastTime;
  uint32_t smallestCurrentTime;

  struct uampState changes[UAMP_STATE_BUFFER_SIZE];
  int numChanges;
};

/*
 * The maximum possible time limit that can be given to a UAMP server.
 */
#define UAMP_MAX_TIME (4294967.295)

/*
 * Values that can be bitwise ORed together to declare any additional UAMP
 * features that are supported by the client.  These values are given to the
 * uampConnect and mvsipConnect functions.
 *
 * If the client does not support 3D data, the fromZ and toZ values in all
 * uampCommands are guaranteed to be 0.  If the client does not support
 * addition and removal data, the present flag is guaranteed to be set in all
 * uampCommands.
 */
#define UAMP_NO_EXTRAS ((uint32_t)(0x00000000))
#define UAMP_SUPPORTS_3D ((uint32_t)(0x80000000))
#define UAMP_SUPPORTS_ADD_REMOVE ((uint32_t)(0x40000000))

/*
 * An MVISP callback function determines whether to accept the simulation
 * specification sent by an MVISP server to a connecting MVISP client.
 * It takes two parameters: the number of agents as the first, and the duration
 * in seconds as the second.  Returning zero indicates that the client accepts,
 * whereas returning non-zero indicates a reject.
 */
typedef int (*mvispCallback)(int, double);

/*
 * Initializes the given UAMP client structure.  It is not necessary to call
 * this function before calling uampConnect or mvispConnect, nor is it
 * necessary to call uampTerminate after calling uampInitialize.  However, at
 * any point after this function is called, it is safe to call uampTerminate on
 * the given structure.
 */
void uampInitialize(struct uampClient *client);

/*
 * Connects as a UAMP client to the server at hostname:port, sending a
 * simulation request for the given number of agents and the given time in
 * seconds (see the UAMP_MAX_TIME constant) with the given random seed.
 * Returns 0 on success or a negative value if an error occurs.
 */
int uampConnect(struct uampClient *client, const char *hostname,
                unsigned short port, int numAgents, double timeLimit,
                long seed, uint32_t supportedFeatures);

/*
 * Connects as an MVISP client to the server at hostname:port, saving the
 * number of agents and duration in seconds to numAgents and timeLimit if
 * they are non-NULL.  If acceptFunc is NULL or if acceptFunc returns zero, the
 * client will accept the simulation specification.  The client then sends the
 * given state names to the server.
 * Returns 0 on success or a negative value if an error occurs.
 */
int mvispConnect(struct uampClient *client, const char *hostname,
                 unsigned short port, int *numAgents, double *timeLimit,
                 const char **stateNames, int numStates,
                 mvispCallback acceptFunc, uint32_t supportedFeatures);

/*
 * Terminates the UAMP or MVISP protocol and disconnects from the server,
 * freeing all resources allocated by uampConnect or mvispConnect.  This
 * function can be safely called after uampInitialize is called or after one of
 * the connect functions is called.  It is not necessary to call this function
 * if the connect function fails, but there is no harm in doing so.
 * Returns 0 on success or a negative value if an error occurs.
 */
int uampTerminate(struct uampClient *client);

/*
 * Fills in the current command for the given agent.  The first command
 * available to each agent is its "initial location" command, which will have
 * fromTime = toTime = 0.0, and fromX = toX and fromY = toY.  All subsequent
 * commands (see the uampAdvance function) will have toTime > fromTime, with
 * the starting time and location of each command guaranteed to be the ending
 * time and location of the previous command.
 */
void uampCurrentCommand(struct uampClient *client, int agentID,
                        struct uampCommand *command);

/*
 * Each agent has a current command, which runs from time fromTime to time
 * toTime.  The intersection time is defined as the time period
 * [lateFrom, earlyTo], where lateFrom is the latest fromTime of any agent, and
 * earlyTo is the earliest toTime of any agent.  This function returns an
 * interpolated command for the given agent ID, covering the period
 * [lateFrom, earlyTo].  This function can be used in conjunction with the
 * uampAdvanceOldest function to present a synchronous view of all the agents'
 * movements.
 */
int uampIntersectCommand(struct uampClient *client, int agentID,
                         struct uampCommand *command);

/*
 * Returns a non-zero value if there is more mobility data to request for the
 * given agent ID, or returns 0 if the given agent ID has reached the end of
 * the simulation.
 */
int uampIsMore(struct uampClient *client, int agentID);

/*
 * Fetches the next command from the UAMP or MVISP server for the given agent
 * ID.  Returns 0 on success, or a negative value if an error occurs (including
 * if there is no more mobility data for the given agent ID; see the uampIsMore
 * function).
 */
int uampAdvance(struct uampClient *client, int agentID);

/*
 * Returns a non-zero value if there is more mobility data to request for any
 * agent ID, or returns 0 if all agent IDs have reach the end of their
 * simulation.
 */
int uampIsAnyMore(struct uampClient *client);

/*
 * Each agent has a current command, which runs from time fromTime to time
 * toTime.  This function calls uampAdvance on the agent(s) whose toTime is the
 * smallest out of all toTimes.  This function can be used in conjunction with
 * the uampIntersectCommand function to present a synchronous view of all the
 * agents' movements.  Returns 0 on success, or a negative value if an error
 * occurs (including if there is no more mobility data; see the uampIsAnyMore
 * function).
 */
int uampAdvanceOldest(struct uampClient *client);

/*
 * Sends a notification of state change to an MVISP server, changing the given
 * agent at the given time in seconds to the given state.  If connected to a
 * UAMP server, this function does nothing and returns 0.  Note that the state
 * change message may not be sent right away; it can be buffered arbitrarily by
 * this library, up until the connection to the server is closed.
 * Returns 0 on success or a negative value if an error occurs.
 */
int uampChangeState(struct uampClient *client, int agentID, double atTime,
                    int newState);

/*
 * Converts the negative return value from a function in uampClient.h into a
 * string representation of the error that occurred.  Returns NULL on invalid
 * error codes.
 */
const char *uampError(int returnValue);

#if 0
{
#endif
#ifdef __cplusplus
}
#endif
#endif
