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

#ifndef __ERRORS_H__
#define __ERRORS_H__

#include <stdio.h>
#include <stdlib.h>

/*
 * Sets the wasErr variable to the given error code, then jumps to the goto
 * target.
 */
#define ERROR(target, wasErr, errorCode)                                      \
  do {                                                                        \
    wasErr = (errorCode);                                                     \
    goto target;                                                              \
  } while (0)

/*
 * If the given error code is negative, set the wasErr variable to the given
 * error code, then jump to the goto target.
 */
#define ERROR_CHECK(target, wasErr, errorCode)                                \
  do {                                                                        \
    if ((errorCode) < 0) {                                                    \
      wasErr = (errorCode);                                                   \
      goto target;                                                            \
    }                                                                         \
  } while (0)

/*
 * Check if the condition passes.  If not, print that the assertion failed and
 * kill the program.
 */
#define ASSERTOUTPUT stderr
#define ASSERT(condition, args...)                                            \
  do {                                                                        \
    if ((condition) == 0) {                                                   \
      fprintf(ASSERTOUTPUT, "Assertion failed: ");                            \
      fprintf(ASSERTOUTPUT, ##args);                                          \
      fprintf(ASSERTOUTPUT, "\n");                                            \
      fflush(ASSERTOUTPUT);                                                   \
      abort();                                                                \
    }                                                                         \
  } while (0)

/*
 * Converts the given function return value into a constant string describing
 * the error.  Returns NULL if given an invalid return value.
 */
const char *returnToString(int retValue);

/*
 * The different error codes that can be returned from the various library
 * functions.
 */
#define ERROR_INVALID_PORT (-1)
#define ERROR_HOSTNAME_INFORMATION (-2)
#define ERROR_CREATE_SOCKET (-3)
#define ERROR_CONNECT_SOCKET (-4)
#define ERROR_SOCKET_DRY (-5)
#define ERROR_SOCKET_READ (-6)
#define ERROR_SOCKET_WRITE (-7)
#define ERROR_OUT_OF_MEMORY (-8)
#define ERROR_INVALID_NUMBER_STATES (-9)
#define ERROR_ZERO_STATE_LENGTH (-10)
#define ERROR_STATE_LENGTH_LONG (-11)
#define ERROR_DUPLICATE_STATE (-12)
#define ERROR_INVALID_NUM_AGENTS (-13)
#define ERROR_INVALID_TIME_LIMIT (-14)
#define ERROR_UAMP_CLIENT_MVISP_SERVER (-15)
#define ERROR_MVISP_CLIENT_UAMP_SERVER (-16)
#define ERROR_SERVER_UNKNOWN_HANDSHAKE (-17)
#define ERROR_SIMULATION_DENIED (-18)
#define ERROR_SIMULATION_RESPONSE_BAD (-19)
#define ERROR_NO_MORE_DATA (-20)
#define ERROR_INVALID_CHANGE_TIME (-21)
#define ERROR_INVALID_CHANGE_STATE (-22)
#define ERROR_NO_INTERSECTION (-23)
#define ERROR_NO_SHARED_VERSION (-24)
#define ERROR_2D_CLIENT_3D_SERVER (-25)
#define ERROR_ADD_REMOVE_UNSUPPORTED (-26)
#define ERROR_INVALID_FEATURES (-27)
#define ERROR_SERVER_REJECTED_HANDSHAKE (-28)
#define ERROR_SERVER_CLIENT_VERSION_DISAGREE (-29)
#define ERROR_MVISP_NO_AGENTS (-30)
#define ERROR_FIRST_UPDATE_TIME (-31)
#define ERROR_NON_EQUAL_FINAL_UPDATES (-32)
#define ERROR_TIMESTAMP_TOO_LARGE (-33)
#define ERROR_TIMESTAMP_NOT_INCREMENTED (-34)
#define ERROR_INVALID_PRESENT_FLAG (-35)

#endif
