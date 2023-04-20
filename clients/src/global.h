/*
 * Copyright (c) 2008-2023 Ryan Vogt <rvogt@ualberta.ca>
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

#ifndef __GLOBAL_H__
#define __GLOBAL_H__

#include <stdio.h>
#include <stdlib.h>

/*
 * Where to send error output.
 */
#ifndef DEBUGOUTPUT
#define DEBUGOUTPUT stderr
#endif

/*
 * Prints the given error message, sets the wasErr flag to 1, and jumps to the
 * given goto target.
 */
#define ERROR(target, wasErr, args...)                                        \
  do {                                                                        \
    fprintf(DEBUGOUTPUT, "Error: ");                                          \
    fprintf(DEBUGOUTPUT, ##args);                                             \
    fprintf(DEBUGOUTPUT, "\n");                                               \
    fflush(DEBUGOUTPUT);                                                      \
    wasErr = 1;                                                               \
    goto target;                                                              \
  } while (0)

/*
 * Sets the wasErr flag to 1 and jumps to the given goto target.
 */
#define ERROR_QUIET(target, wasErr)                                           \
  do {                                                                        \
    wasErr = 1;                                                               \
    goto target;                                                              \
  } while (0)

/*
 * If the given error code is negative, set the wasErr flag to 1, print an
 * error message, then jumps to the goto target.
 */
#define ERROR_CHECK_UAMP(target, wasErr, errorCode)                           \
  do {                                                                        \
    if ((errorCode) < 0) {                                                    \
      fprintf(DEBUGOUTPUT, "Error: %s\n", uampError(errorCode));              \
      fflush(DEBUGOUTPUT);                                                    \
      wasErr = 1;                                                             \
      goto target;                                                            \
    }                                                                         \
  } while (0)

/*
 * Check if the condition passes.  If not, print that the assertion failed and
 * kill the program.
 */
#define ASSERT(condition, args...)                                            \
  do {                                                                        \
    if ((condition) == 0) {                                                   \
      fprintf(DEBUGOUTPUT, "Assertion failed: ");                             \
      fprintf(DEBUGOUTPUT, ##args);                                           \
      fprintf(DEBUGOUTPUT, "\n");                                             \
      fflush(DEBUGOUTPUT);                                                    \
      abort();                                                                \
    }                                                                         \
  } while (0)

/*
 * Checks if the program received only a single argument, "--help". If so,
 * prints a usage message and returns 1. Otherwise, returns 0. The usage string
 * is expected to begin with a newline but not end with one, and contain all of
 * the command-line arguments that the program at argv[0] can receive.
 */
int helpRequested(int argc, char **argv, const char *usageString);

/*
 * Converts the given string argument to an int, long, double, or FILE*.
 * Returns 0 on success, or prints an error message and returns -1 on error.
 */
int processPortArg(const char *theArg, unsigned short *result);
int processIntArg(const char *theArg, int *result);
int processLongArg(const char *theArg, long *result);
int processDoubleArg(const char *theArg, double *result);
int processFileArg(const char *theArg, FILE **result, int append);

/*
 * Prints the message: "Connecting to ___ at IP:port (hostname:port)", where
 * "___" is filled in with the given description.  Returns 0 on success, or
 * prints an error message and returns -1 on error.
 */
int connectMessage(const char *hostname, unsigned short port,
                   const char *description);

#endif
