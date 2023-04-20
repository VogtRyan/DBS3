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

#include "errors.h"

#include <stdlib.h>

const char *returnToString(int retValue) {
  switch (retValue) {
  case 0:
    return "Success";
  case ERROR_INVALID_PORT:
    return "Invalid port number specified";
  case ERROR_HOSTNAME_INFORMATION:
    return "Could not get information for given hostname";
  case ERROR_CREATE_SOCKET:
    return "Could not create socket";
  case ERROR_CONNECT_SOCKET:
    return "Could not connect socket";
  case ERROR_SOCKET_DRY:
    return "Socket dried up";
  case ERROR_SOCKET_READ:
    return "Could not read from socket";
  case ERROR_SOCKET_WRITE:
    return "Could not write to socket";
  case ERROR_OUT_OF_MEMORY:
    return "Out of memory";
  case ERROR_INVALID_NUMBER_STATES:
    return "Invalid number of states";
  case ERROR_ZERO_STATE_LENGTH:
    return "Zero-length state name";
  case ERROR_STATE_LENGTH_LONG:
    return "State name length longer than supported";
  case ERROR_DUPLICATE_STATE:
    return "Duplicate state name";
  case ERROR_INVALID_NUM_AGENTS:
    return "Invalid number of agents";
  case ERROR_INVALID_TIME_LIMIT:
    return "Invalid time limit for simulation";
  case ERROR_UAMP_CLIENT_MVISP_SERVER:
    return "UAMP client attempting to contact MVISP server";
  case ERROR_MVISP_CLIENT_UAMP_SERVER:
    return "MVISP client attempting to contact UAMP server";
  case ERROR_SERVER_UNKNOWN_HANDSHAKE:
    return "Unknown handshake data from server";
  case ERROR_SIMULATION_DENIED:
    return "Simulation specification denied";
  case ERROR_SIMULATION_RESPONSE_BAD:
    return "Simulation specification response malformed";
  case ERROR_NO_MORE_DATA:
    return "No more movement data to request";
  case ERROR_INVALID_CHANGE_TIME:
    return "Invalid time given for state change";
  case ERROR_INVALID_CHANGE_STATE:
    return "Invalid state into which to transition";
  case ERROR_NO_INTERSECTION:
    return "Current command times form no intersection";
  case ERROR_NO_SHARED_VERSION:
    return "Client and server do not support a common UAMP/MVISP version";
  case ERROR_2D_CLIENT_3D_SERVER:
    return "Server sends 3D data, which client does not support";
  case ERROR_ADD_REMOVE_UNSUPPORTED:
    return "Server sends add/remove data, which client does not support";
  case ERROR_INVALID_FEATURES:
    return "Invalid features given to connect function";
  case ERROR_SERVER_REJECTED_HANDSHAKE:
    return "Server rejected handshake for unknown reason";
  case ERROR_SERVER_CLIENT_VERSION_DISAGREE:
    return "Server and client do not agree on protocol version to run";
  case ERROR_MVISP_NO_AGENTS:
    return "MVISP server specified zero agents";
  case ERROR_FIRST_UPDATE_TIME:
    return "First location update did not have zero time";
  case ERROR_NON_EQUAL_FINAL_UPDATES:
    return "Server sent non-matching final updates";
  case ERROR_TIMESTAMP_TOO_LARGE:
    return "Server sent update with timestamp past simulation duration";
  case ERROR_TIMESTAMP_NOT_INCREMENTED:
    return "Server sent update with timestamp that did not increase";
  case ERROR_INVALID_PRESENT_FLAG:
    return "Server sent malformed present flag";
  default:
    return NULL;
  }
}
