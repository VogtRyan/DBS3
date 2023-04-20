/*
 * Copyright (c) 2007-2023 Ryan Vogt <rvogt@ualberta.ca>
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

#include <errno.h>
#include <limits.h>
#include <math.h>
#include <netdb.h>
#include <stdlib.h>
#include <string.h>

/* Maximum length of string representation of IPv4 address */
#define MAX_IP_STR_LEN 15

int helpRequested(int argc, char **argv, const char *usageString) {
  if (argc == 2 && strncmp(argv[1], "--help", 7) == 0) {
    printf("Usage: %s%s\n", argv[0], usageString);
    return 1;
  }
  return 0;
}

int processPortArg(const char *theArg, unsigned short *result) {
  long lval;
  char *ep;
  int wasErr = 0;

  errno = 0;
  lval = strtol(theArg, &ep, 10);
  if (theArg[0] == '\0' || *ep != '\0')
    ERROR(isErr, wasErr, "Invalid port number: %s", theArg);
  else if ((errno == ERANGE && (lval == LONG_MAX || lval == LONG_MIN)) ||
           (lval > (long)USHRT_MAX || lval < 0))
    ERROR(isErr, wasErr, "Argument out of range: %s", theArg);

  *result = (unsigned short)lval;

isErr:
  return wasErr ? -1 : 0;
}

int processIntArg(const char *theArg, int *result) {
  long lval;
  char *ep;
  int wasErr = 0;

  errno = 0;
  lval = strtol(theArg, &ep, 10);
  if (theArg[0] == '\0' || *ep != '\0')
    ERROR(isErr, wasErr, "Invalid argument: \'%s\'", theArg);
  else if ((errno == ERANGE && (lval == LONG_MAX || lval == LONG_MIN)) ||
           (lval > INT_MAX || lval < INT_MIN))
    ERROR(isErr, wasErr, "Argument out of range: \'%s\'\n", theArg);

  *result = (int)lval;
isErr:
  return wasErr ? -1 : 0;
}

int processLongArg(const char *theArg, long *result) {
  long lval;
  char *ep;
  int wasErr = 0;

  errno = 0;
  lval = strtol(theArg, &ep, 10);
  if (theArg[0] == '\0' || *ep != '\0')
    ERROR(isErr, wasErr, "Invalid argument: \'%s\'", theArg);
  else if (errno == ERANGE && (lval == LONG_MAX || lval == LONG_MIN))
    ERROR(isErr, wasErr, "Argument out of range: \'%s\'\n", theArg);

  *result = lval;
isErr:
  return wasErr ? -1 : 0;
}

int processDoubleArg(const char *theArg, double *result) {
  double dval;
  char *ep;
  int wasErr = 0;

  errno = 0;
  dval = strtod(theArg, &ep);
  if (theArg[0] == '\0' || *ep != '\0')
    ERROR(isErr, wasErr, "Invalid argument: \'%s\'", theArg);
  else if (dval == 0 && ep == theArg)
    ERROR(isErr, wasErr, "Could not convert argument to double: \'%s\'",
          theArg);
  else if ((dval == HUGE_VAL || dval == -HUGE_VAL || dval == 0) &&
           errno == ERANGE)
    ERROR(isErr, wasErr, "Argument out of range: \'%s\'", theArg);

  *result = dval;
isErr:
  return wasErr ? -1 : 0;
}

int processFileArg(const char *theArg, FILE **result, int append) {
  int wasErr = 0;

  if (*result != NULL)
    return -1;
  if (append)
    *result = fopen(theArg, "a");
  else
    *result = fopen(theArg, "w");
  if (*result == NULL)
    ERROR(isErr, wasErr, "Cannot open file \'%s\' for writing", theArg);

isErr:
  return wasErr ? -1 : 0;
}

int connectMessage(const char *hostname, unsigned short port,
                   const char *description) {
  char ip[MAX_IP_STR_LEN + 1];
  struct hostent *hp;
  int wasErr = 0;

  /* Get the hostname information for the target host */
  hp = gethostbyname(hostname);
  if (hp == NULL)
    ERROR(isErr, wasErr, "Could not get information for hostname %s",
          hostname);

  /* Convert the IP to a string */
  snprintf(ip, MAX_IP_STR_LEN + 1, "%hhu.%hhu.%hhu.%hhu",
           (unsigned char)hp->h_addr[0], (unsigned char)hp->h_addr[1],
           (unsigned char)hp->h_addr[2], (unsigned char)hp->h_addr[3]);

  printf("Connecting to %s at %s:%hu (%s:%hu)\n", description, ip, port,
         hp->h_name, port);

isErr:
  return wasErr ? -1 : 0;
}
