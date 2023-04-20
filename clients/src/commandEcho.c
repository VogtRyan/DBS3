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

#include "global.h"

#include <getopt.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <uampClient.h>

/*
 * Default parameters: the number of agents, the time limit in seconds, and the
 * seed to request from the server.
 */
#define DEFAULT_NUM_AGENTS (10)
#define DEFAULT_TIME_LIMIT (100.0)
#define DEFAULT_SEED (0L)

/*
 * Run the UAMP client, connecting to the UAMP server on the given host and
 * port and requesting a simulation with the given number of agents, time
 * limit, and seed.  Returns 0 on success, -1 on error (and prints an error
 * message).
 */
static int runClient(const char *hostname, unsigned short port, int numAgents,
                     double timeLimit, long seed);

/*
 * Parses the command line and fills in the hostname, port number, number of
 * agents, time limit, and seed provided on the command line.
 * Returns 0 on success, -1 on error (and prints an error message).
 */
static int parseCommandLine(int argc, char **argv, char **hostname,
                            unsigned short *port, int *numAgents,
                            double *timeLimit, long *seed);

/*
 * The usage string to print either if the user requests it, or if there is an
 * error parsing the command line options.
 */
static const char *usageString = "\n    [-n numAgents]"
                                 "\n    [-t durationSeconds]"
                                 "\n    [-s randomSeed]"
                                 "\n    hostname port";

int main(int argc, char **argv) {
  char *hostname = NULL;
  unsigned short port;
  int numAgents;
  double timeLimit;
  long seed;
  int wasErr = 0;

  /* Parse the command line and output a summary to stdout */
  if (helpRequested(argc, argv, usageString)) {
    return -1;
  }
  if (parseCommandLine(argc, argv, &hostname, &port, &numAgents, &timeLimit,
                       &seed))
    ERROR_QUIET(isErr, wasErr);
  if (connectMessage(hostname, port, "UAMP server"))
    ERROR_QUIET(isErr, wasErr);
  printf("Agents:      %u\n", (unsigned int)numAgents);
  printf("Duration:    %.3lf seconds\n", timeLimit);
  printf("Random seed: %u\n", (unsigned int)seed);

  /* Run the client */
  if (runClient(hostname, port, numAgents, timeLimit, seed))
    ERROR_QUIET(isErr, wasErr);

isErr:
  return wasErr ? -1 : 0;
}

static int runClient(const char *hostname, unsigned short port, int numAgents,
                     double timeLimit, long seed) {
  struct uampClient client;
  struct uampCommand command;
  int ret, onAgent;
  int wasErr = 0;

  /* Connect to the UAMP server */
  ret = uampConnect(&client, hostname, port, numAgents, timeLimit, seed,
                    UAMP_SUPPORTS_3D);
  ERROR_CHECK_UAMP(isErr, wasErr, ret);

  /* Get all the commands for each agent */
  for (onAgent = 0; onAgent < numAgents; onAgent++) {
    printf("\nAgent %d\n", onAgent);
    while (1) {
      uampCurrentCommand(&client, onAgent, &command);
      printf("Time %.3lf: location %.3lf, %.3lf, %.3lf\n", command.toTime,
             command.toX, command.toY, command.toZ);
      if (uampIsMore(&client, onAgent) == 0)
        break;
      ret = uampAdvance(&client, onAgent);
      ERROR_CHECK_UAMP(isErr, wasErr, ret);
    }
  }

isErr:
  uampTerminate(&client);
  return wasErr ? -1 : 0;
}

static int parseCommandLine(int argc, char **argv, char **hostname,
                            unsigned short *port, int *numAgents,
                            double *timeLimit, long *seed) {
  int ch, i;
  int procN, procT, procS;
  int wasErr = 0;

  struct option longopts[] = {{"numAgents", required_argument, NULL, 'n'},
                              {"time", required_argument, NULL, 't'},
                              {"seed", required_argument, NULL, 's'},
                              {NULL, 0, NULL, 0}};
  static const char *optstring = "n:t:s:";

  /* Set default options */
  *numAgents = DEFAULT_NUM_AGENTS;
  *timeLimit = DEFAULT_TIME_LIMIT;
  *seed = DEFAULT_SEED;

  /* Process input options */
  i = 0;
  procN = procT = procS = 0;
  while ((ch = getopt_long(argc, argv, optstring, longopts, NULL)) != -1) {
    switch (ch) {
    case 'n':
      i = (procN ? -1 : processIntArg(optarg, numAgents));
      procN = 1;
      break;
    case 't':
      i = (procT ? -1 : processDoubleArg(optarg, timeLimit));
      procT = 1;
      break;
    case 's':
      i = (procS ? -1 : processLongArg(optarg, seed));
      procS = 1;
      break;
    default:
      i = -1;
      break;
    }
    if (i == -1)
      break;
  }

  /* There should be two options remaining.  Parse the host and port */
  if (i != -1) {
    if (argc - optind == 2) {
      *hostname = argv[argc - 2];
      i = processPortArg(argv[argc - 1], port);
    } else
      i = -1;
  }

  /* Ensure value sanity */
  if (*numAgents <= 0 || *timeLimit < 0.0 || *timeLimit > UAMP_MAX_TIME)
    i = -1;

  /* If there was any error, print the usage message */
  if (i == -1)
    ERROR(isErr, wasErr, "Usage: %s%s", argv[0], usageString);

isErr:
  return wasErr ? -1 : 0;
}
