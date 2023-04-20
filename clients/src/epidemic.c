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
#include <stdint.h>
#include <stdio.h>
#include <uampClient.h>

/*
 * The time in seconds for which an infection incubates before the agent
 * becomes contagious, and the range in metres at which a contagious agent can
 * infect an uninfected agent.
 */
static double INCUBATION_TIME = 60.0;
static double INFECTION_RANGE = 1.0;

/*
 * The total number of agents in the simulation; the number of agents in the
 * contagious state at time zero; and, the number of agents immune.
 */
static int NUM_AGENTS = 100;
static int INITIAL_AGENTS = 1;
static int IMMUNE_AGENTS = 0;

/*
 * The type of client we will be running: UAMP or MVISP.
 */
#define CLIENT_TYPE_UAMP (0)
#define CLIENT_TYPE_MVISP (1)
static int CLIENT_TYPE = CLIENT_TYPE_UAMP;

/*
 * The maximal duration of the simulation in seconds, and the seed to send to
 * the server (if applicable).
 */
static double TIME_LIMIT = UAMP_MAX_TIME;
static long SEED = 0;

/*
 * The file to append with the infection times of each host.
 */
static FILE *RESULT_FILE = NULL;

/*
 * The states sent to the MVISP server.
 */
static const char *STATE_NAMES[] = {"Uninfected", "Incubating", "Contagious",
                                    "Immune"};
#define STATE_UNINFECTED (0)
#define STATE_INCUBATING (1)
#define STATE_CONTAGIOUS (2)
#define STATE_IMMUNE (3)

/*
 * Keeps track of when each agent is infected with the disease.
 */
struct agent {
  double infectedTime;   /* INVALID_TIME if not yet infected */
  double contagiousTime; /* INVALID_TIME if not yet infected */
};
#define INVALID_TIME (UAMP_MAX_TIME + 1.0)

/*
 * Run the UAMP or MVISP client, connecting to the server on the given host and
 * port and simulating a disease spreading according to the above parameters.
 * Returns 0 on success, -1 on error (and prints an error message).
 */
static int runClient(const char *hostname, unsigned short port);

/*
 * Verifies that the MVISP server is simulating enough agents to account for
 * the number of initial infections and immune hosts we want.  Returns zero
 * to indicate acceptance.
 */
static int verifyAgents(int numAgents, double seconds);

/*
 * Process the movements of the NUM_AGENTS agents simultaneously performing
 * the given commands.  Update the state of the agents as necessary and update
 * the contents of the infectedAgents value.
 */
static void processMovements(struct agent *agents,
                             struct uampCommand *commands,
                             int *infectedAgents);

/*
 * Report the state changes to the MVISP server and write infection times to
 * the results file.  Returns 0 on success, or returns -1 and print an error
 * message on error.
 */
static int finalizeStates(struct uampClient *client, struct agent *agents);

/*
 * Considers two agents that are performing commands cmdA and cmdB from the
 * same uampCommand startTime to the same uampCommand endTime.  If during that
 * time period, the two agents are ever within minDist metres of each other,
 * return 0 and set fromTime and toTime to the beginning and end of the time
 * period that the agents are within that distance.  If the two agents do not
 * come within that distance of each other, return -1.
 */
static int timeTogether(struct uampCommand *cmdA, struct uampCommand *cmdB,
                        double minDist, double *fromTime, double *toTime);

/*
 * Solves ax^2 + b^x + c <= 0, where a >= 0.  If the inequality does not
 * hold for any real x, return -1.  Otherwise, return 0 and set low and high
 * such that the inequality holds for all low <= x <= high.  The value of low
 * may be set to -HUGE_VAL, and the value of high may be set to HUGE_VAL.
 */
static int quadraticLT(double a, double b, double c, double *low,
                       double *high);

/*
 * Adds value to the end of the array and increments currentSize, unless value
 * is already in the array (in which case, do nothing).
 */
static void addUnique(int *array, int *currentSize, int value);

/*
 * Parses command line options, filling in the hostname and port variables.
 * Also sets the global variables above.
 * Returns 0 on success, or prints an error message and returns -1 on error.
 */
static int parseCommandLine(int argc, char **argv, char **hostname,
                            unsigned short *port);

/*
 * The usage string to print either if the user requests it, or if there is an
 * error parsing the command line options.
 */
static const char *usageString = "\n    [-i initialInfections]"
                                 "\n    [-r infectionRangeMetres]"
                                 "\n    [-t incubationTimeSeconds]"
                                 "\n    [-n immuneAgents]"
                                 "\n    [(-u numAgents [-s seed]) | (-m)]"
                                 "\n    [--epidemicFile fileToAppend]"
                                 "\n    hostname port";

int main(int argc, char **argv) {
  char *hostname = NULL;
  unsigned short port;
  int wasErr = 0;

  /* Parse the command line and output a summary to stdout */
  if (helpRequested(argc, argv, usageString)) {
    return -1;
  }
  if (parseCommandLine(argc, argv, &hostname, &port))
    ERROR_QUIET(isErr, wasErr);
  if (connectMessage(
          hostname, port,
          (CLIENT_TYPE == CLIENT_TYPE_UAMP ? "UAMP server" : "MVISP server")))
    ERROR_QUIET(isErr, wasErr);
  if (CLIENT_TYPE == CLIENT_TYPE_UAMP) {
    printf("Total agents:       %d\n", NUM_AGENTS);
    printf("Random seed:        %ld\n", SEED);
  }
  printf("Initial infections: %d\n", INITIAL_AGENTS);
  printf("Immune agents:      %d\n", IMMUNE_AGENTS);
  printf("Infection range:    %.3lf metres\n", INFECTION_RANGE);
  printf("Incubation period:  %.3lf seconds\n", INCUBATION_TIME);

  /* Run the client */
  if (runClient(hostname, port))
    ERROR_QUIET(isErr, wasErr);

isErr:
  if (RESULT_FILE != NULL)
    fclose(RESULT_FILE);
  return wasErr ? -1 : 0;
}

static int runClient(const char *hostname, unsigned short port) {
  struct uampClient client;
  struct agent *agents = NULL;
  struct uampCommand *commands = NULL;
  int ret, i, infectedAgents;
  int wasErr = 0;

  /* Connect to the UAMP/MVISP server and allocate memory */
  if (CLIENT_TYPE == CLIENT_TYPE_UAMP)
    ret = uampConnect(&client, hostname, port, NUM_AGENTS, TIME_LIMIT, SEED,
                      UAMP_SUPPORTS_3D | UAMP_SUPPORTS_ADD_REMOVE);
  else
    ret = mvispConnect(&client, hostname, port, &NUM_AGENTS, &TIME_LIMIT,
                       STATE_NAMES, sizeof(STATE_NAMES) / sizeof(char *),
                       &verifyAgents,
                       UAMP_SUPPORTS_3D | UAMP_SUPPORTS_ADD_REMOVE);
  ERROR_CHECK_UAMP(isErr, wasErr, ret);
  agents =
      (struct agent *)calloc(NUM_AGENTS - IMMUNE_AGENTS, sizeof(struct agent));
  commands = (struct uampCommand *)calloc(NUM_AGENTS - IMMUNE_AGENTS,
                                          sizeof(struct uampCommand));
  if (agents == NULL || commands == NULL)
    ERROR(isErr, wasErr, "Out of memory");

  /*
   * Consider agents [0, INITIAL_AGENTS-1] to be the initially infected agents,
   * and agents [NUM_AGENTS-IMMUNE_AGENTS, NUM_AGENTS-1] to be the immune
   * agents.
   */
  for (i = 0; i < INITIAL_AGENTS; i++)
    agents[i].infectedTime = agents[i].contagiousTime = 0.0;
  for (i = INITIAL_AGENTS; i < NUM_AGENTS - IMMUNE_AGENTS; i++)
    agents[i].infectedTime = agents[i].contagiousTime = INVALID_TIME;
  infectedAgents = INITIAL_AGENTS;

  /*
   * Request movement data until everyone is infected (or until no movement
   * data remains from the server).
   */
  while (infectedAgents + IMMUNE_AGENTS < NUM_AGENTS) {
    for (i = 0; i < NUM_AGENTS - IMMUNE_AGENTS; i++) {
      ret = uampIntersectCommand(&client, i, commands + i);
      ERROR_CHECK_UAMP(isErr, wasErr, ret);
    }
    processMovements(agents, commands, &infectedAgents);
    if (uampIsAnyMore(&client) == 0)
      break;
    ret = uampAdvanceOldest(&client);
    ERROR_CHECK_UAMP(isErr, wasErr, ret);
  }

  /* Send the state change times to the server and the result file */
  if (finalizeStates(&client, agents))
    ERROR_QUIET(isErr, wasErr);

isErr:
  uampTerminate(&client);
  if (agents != NULL)
    free(agents);
  if (commands != NULL)
    free(commands);
  return wasErr ? -1 : 0;
}

static int verifyAgents(int numAgents, double seconds) {
  int totalRequired = INITIAL_AGENTS + IMMUNE_AGENTS;
  if (totalRequired < INITIAL_AGENTS || totalRequired < IMMUNE_AGENTS)
    return -1;
  else if (totalRequired > numAgents)
    return -1;
  return 0;
}

static void processMovements(struct agent *agents,
                             struct uampCommand *commands,
                             int *infectedAgents) {
  int infectors[NUM_AGENTS - IMMUNE_AGENTS];
  int victims[NUM_AGENTS - IMMUNE_AGENTS];
  int numInfectors = 0, numVictims = 0;
  int i, theInfector, theVictim;
  double startTime, endTime, startInRange, endInRange, earliestPossible,
      affectTime;

  /*
   * Infectors are anyone with a contagious time <= endTime, and victims are
   * anyone who is not immune with an infection time > startTime.
   */
  startTime = commands[0].fromTime;
  endTime = commands[0].toTime;
  for (i = 0; i < NUM_AGENTS - IMMUNE_AGENTS; i++) {
    if ((agents[i].contagiousTime <= endTime) && commands[i].present)
      infectors[numInfectors++] = i;
    if ((agents[i].infectedTime > startTime) && commands[i].present)
      victims[numVictims++] = i;
  }

  while (numInfectors > 0) {
    /*
     * For each infector, determine the earliest possible time they could
     * infect another agent.
     */
    theInfector = infectors[--numInfectors];
    earliestPossible = startTime > (agents[theInfector].contagiousTime)
                           ? startTime
                           : (agents[theInfector].contagiousTime);

    /* Test the infector against each possible victim */
    for (i = 0; i < numVictims; i++) {
      theVictim = victims[i];
      if (theInfector == theVictim)
        continue;

      /* Can the infector can actually change the victim's infected time? */
      if (earliestPossible >= agents[theVictim].infectedTime)
        continue;
      if (timeTogether(commands + theInfector, commands + theVictim,
                       INFECTION_RANGE, &startInRange, &endInRange))
        continue;

      /*
       * We know that the infector and the possible victim come into range.
       * But, what is the first time at which the infector is both contagious
       * and in range?
       */
      if (earliestPossible > endInRange)
        continue;
      affectTime =
          startInRange > earliestPossible ? startInRange : earliestPossible;
      if (affectTime >= agents[theVictim].infectedTime)
        continue;

      /*
       * Update the victim to the new, earlier infected time.  If the victim's
       * new (earlier) contagious time falls within the current time period of
       * [startTime, endTime], we will have to reconsider this victim as an
       * infector.
       */
      if (agents[theVictim].infectedTime == INVALID_TIME)
        (*infectedAgents)++;
      agents[theVictim].infectedTime = affectTime;
      agents[theVictim].contagiousTime = affectTime + INCUBATION_TIME;
      if (agents[theVictim].contagiousTime <= endTime)
        addUnique(infectors, &numInfectors, theVictim);
    }
  }
}

static int finalizeStates(struct uampClient *client, struct agent *agents) {
  const char *sep = "";
  int onAgent, ret;
  int wasErr = 0;

  /* Send state change messages to the MVISP server */
  for (onAgent = 0; onAgent < NUM_AGENTS - IMMUNE_AGENTS; onAgent++) {
    if (agents[onAgent].infectedTime <= TIME_LIMIT &&
        agents[onAgent].contagiousTime != agents[onAgent].infectedTime) {
      ret = uampChangeState(client, onAgent, agents[onAgent].infectedTime,
                            STATE_INCUBATING);
      ERROR_CHECK_UAMP(isErr, wasErr, ret);
    }
    if (agents[onAgent].contagiousTime <= TIME_LIMIT) {
      ret = uampChangeState(client, onAgent, agents[onAgent].contagiousTime,
                            STATE_CONTAGIOUS);
      ERROR_CHECK_UAMP(isErr, wasErr, ret);
    }
  }
  for (onAgent = NUM_AGENTS - IMMUNE_AGENTS; onAgent < NUM_AGENTS; onAgent++) {
    ret = uampChangeState(client, onAgent, 0.0, STATE_IMMUNE);
    ERROR_CHECK_UAMP(isErr, wasErr, ret);
  }

  /* Write out infection times to the result file */
  if (RESULT_FILE != NULL) {
    for (onAgent = 0; onAgent < NUM_AGENTS - IMMUNE_AGENTS; onAgent++) {
      if (agents[onAgent].infectedTime == INVALID_TIME)
        fprintf(RESULT_FILE, "%s-1.000", sep);
      else
        fprintf(RESULT_FILE, "%s%.3lf", sep, agents[onAgent].infectedTime);
      sep = " ";
    }
    fprintf(RESULT_FILE, "\n");
  }

isErr:
  return wasErr ? -1 : 0;
}

static int timeTogether(struct uampCommand *cmdA, struct uampCommand *cmdB,
                        double minDist, double *fromTime, double *toTime) {
  double a, b, c, f, g, h, i, j, k, lowT, highT;
  double dx, dy, dz, dist;

  /* Ensure the commands have the same start and end time */
  ASSERT((cmdA->fromTime == cmdB->fromTime) && (cmdA->toTime == cmdB->toTime),
         "Commands from different times");

  /* For initial positions, we need only check distance at endTime */
  if (cmdA->toTime == 0.0) {
    dx = (cmdA->toX) - (cmdB->toX);
    dy = (cmdA->toY) - (cmdB->toY);
    dz = (cmdA->toZ) - (cmdB->toZ);
    dist = sqrt(dx * dx + dy * dy + dz * dz);
    if (dist <= minDist) {
      *fromTime = *toTime = 0.0;
      return 0;
    }
    return -1;
  }

  /*
   * Both agents are moving during the period [startTime, endTime].
   * Let T = (t - startTime) / (endTime - startTime); i.e., T in [0, 1] is a
   * measure of time within the range [startTime, endTime].
   *
   * Let d(T)^2 be the square of the distance between the two agents at time
   * 0 <= T <= 1.
   * d(T)^2 = (f^2 + g^2 + h^2) * T^2 +
   *          (2fi + 2gj + 2hk) * T +
   *          (i^2 + j^2 + k^2),
   * where f, g, h, i, j, and k are as defined in the code below.
   *
   * To derive the above formula, run the following in Maple:
   * > xaT := cmdAFromX + (cmdAToX - cmdAFromX)*T:
   * > xbT := cmdBFromX + (cmdBToX - cmdBFromX)*T:
   * > yaT := cmdAFromY + (cmdAToY - cmdAFromY)*T:
   * > ybT := cmdBFromY + (cmdBToY - cmdBFromY)*T:
   * > zaT := cmdAFromZ + (cmdAToZ - cmdAFromZ)*T:
   * > zbT := cmdBFromZ + (cmdBToZ - cmdBFromZ)*T:
   * > dTsq := (xaT - xbT)^2 + (yaT - ybT)^2 + (zaT - zbT)^2:
   * > collect(dTsq, T);
   */
  f = (cmdA->toX) - (cmdA->fromX) - (cmdB->toX) + (cmdB->fromX);
  g = (cmdA->toY) - (cmdA->fromY) - (cmdB->toY) + (cmdB->fromY);
  h = (cmdA->toZ) - (cmdA->fromZ) - (cmdB->toZ) + (cmdB->fromZ);
  i = (cmdA->fromX) - (cmdB->fromX);
  j = (cmdA->fromY) - (cmdB->fromY);
  k = (cmdA->fromZ) - (cmdB->fromZ);

  /*
   * What values of T yield d(T) <= minDist?  Since d(T) and minDist are both
   * non-negative, d(T) <= minDist iff d(T)^2 <= minDist^2 iff
   * d(T)^2 - minDist^2 <= 0.
   * If there are no real values of T for which d(T)^2 - minDist^2 <= 0, the
   * agents are not within range at any point T in [0, 1].
   */
  a = f * f + g * g + h * h;
  b = 2 * f * i + 2 * g * j + 2 * h * k;
  c = i * i + j * j + k * k - minDist * minDist;
  if (quadraticLT(a, b, c, &lowT, &highT))
    return -1;

  /* Check if there are values of T in [0, 1] where the agents are in range */
  if (lowT > 1.0 || highT < 0.0)
    return -1;
  if (lowT < 0.0)
    lowT = 0.0;
  if (highT > 1.0)
    highT = 1.0;
  *fromTime = cmdA->fromTime + (lowT * ((cmdA->toTime) - (cmdA->fromTime)));
  *toTime = cmdA->fromTime + (highT * ((cmdA->toTime) - (cmdA->fromTime)));
  return 0;
}

static int quadraticLT(double a, double b, double c, double *low,
                       double *high) {
  double xInt, disc, t, rootOne, rootTwo;

  /* This function only considers a >= 0 */
  ASSERT(a >= 0, "Negative value for a");

  /* If a == 0, we degenerate to a linear equation */
  if (a == 0) {
    /*
     * If b == 0, the inequality looks like f(x) = c <= 0, so the inequality
     * either holds for all x or for no x.
     */
    if (b == 0) {
      if (c <= 0) {
        *low = -HUGE_VAL;
        *high = HUGE_VAL;
        return 0;
      } else
        return -1;
    }

    /* If b != 0, we have a linear equation */
    else {
      xInt = (-c) / b;
      if (b > 0) {
        *low = -HUGE_VAL;
        *high = xInt;
      } else {
        *low = xInt;
        *high = HUGE_VAL;
      }
      return 0;
    }
  }

  /*
   * If we made it this far, we're actually dealing with a quadratic equation,
   * i.e., a != 0.  First, check if there are any real roots to f(x) = 0.
   * If there are none, the parabola never crosses the x-axis.  But, since
   * a > 0, this means that the parabola is never below the x-axis, so the
   * inequality never holds.
   */
  disc = (b * b) - (4 * a * c);
  if (disc < 0)
    return -1;

  /*
   * If the discriminant is equal to zero, then there is a single root.
   * Recall, we know a != 0.
   */
  if (disc == 0) {
    *low = *high = (-b) / (2 * a);
    return 0;
  }

  /*
   * The discriminant is > 0, so there are two unique roots.  Since a > 0,
   * the inequality holds between those two roots (as opposed to on either
   * side of those two roots, if it were the case that a < 0).
   * Note that since disc > 0, we are guaranteed that t will never be equal
   * to zero in the calculations below.
   */
  if (b < 0)
    t = (-0.5) * (b - sqrt(disc));
  else
    t = (-0.5) * (b + sqrt(disc));
  rootOne = t / a;
  rootTwo = c / t;

  if (rootOne < rootTwo) {
    *low = rootOne;
    *high = rootTwo;
  } else {
    *low = rootTwo;
    *high = rootOne;
  }
  return 0;
}

static void addUnique(int *array, int *currentSize, int value) {
  int i, size;

  size = *currentSize;
  for (i = 0; i < size; i++) {
    if (array[i] == value)
      return;
  }
  array[size] = value;
  *currentSize = size + 1;
}

static int parseCommandLine(int argc, char **argv, char **hostname,
                            unsigned short *port) {
  int ch, i;
  int procT, procR, procI, procN, procS, procType;
  int efFlag;
  int wasErr = 0;

  struct option longopts[] = {
      {"incubationTime", required_argument, NULL, 't'},
      {"infectionRange", required_argument, NULL, 'r'},
      {"initialInfections", required_argument, NULL, 'i'},
      {"immuneAgents", required_argument, NULL, 'n'},
      {"uampClient", required_argument, NULL, 'u'},
      {"seed", required_argument, NULL, 's'},
      {"mvispClient", no_argument, NULL, 'm'},
      {"epidemicFile", required_argument, &efFlag, 1},
      {NULL, 0, NULL, 0}};
  static const char *optstring = "t:r:i:n:u:s:m";

  i = procT = procR = procI = procN = procS = procType = efFlag = 0;
  while ((ch = getopt_long(argc, argv, optstring, longopts, NULL)) != -1) {
    switch (ch) {
    case 't':
      i = (procT ? -1 : processDoubleArg(optarg, &INCUBATION_TIME));
      procT = 1;
      break;
    case 'r':
      i = (procR ? -1 : processDoubleArg(optarg, &INFECTION_RANGE));
      procR = 1;
      break;
    case 'i':
      i = (procI ? -1 : processIntArg(optarg, &INITIAL_AGENTS));
      procI = 1;
      break;
    case 'n':
      i = (procN ? -1 : processIntArg(optarg, &IMMUNE_AGENTS));
      procN = 1;
      break;
    case 'u':
      i = (procType ? -1 : processIntArg(optarg, &NUM_AGENTS));
      procType = 1;
      break;
    case 's':
      i = (procS ? -1 : processLongArg(optarg, &SEED));
      procS = 1;
      break;
    case 'm':
      i = (procType ? -1 : 0);
      procType = 1;
      CLIENT_TYPE = CLIENT_TYPE_MVISP;
      break;
    case 0:
      if (efFlag) {
        i = processFileArg(optarg, &RESULT_FILE, 1);
        efFlag = 0;
      }
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
  if (INCUBATION_TIME < 0.0 || INFECTION_RANGE < 0.0 || INITIAL_AGENTS <= 0 ||
      NUM_AGENTS <= 0 || IMMUNE_AGENTS < 0)
    i = -1;
  if (procS && (CLIENT_TYPE != CLIENT_TYPE_UAMP))
    i = -1;

  /* If there was any error, print the usage message */
  if (i == -1)
    ERROR(isErr, wasErr, "Usage: %s%s", argv[0], usageString);

isErr:
  return wasErr ? -1 : 0;
}
