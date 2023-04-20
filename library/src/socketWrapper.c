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

/*
 * The functions in this file are largely based on Jim Frost's
 * "BSD Sockets: A Quick and Dirty Primer" located at:
 * http://www.frostbytes.com/~jimf/papers/sockets/sockets.html
 */

#include "socketWrapper.h"

#include "errors.h"

#include <sys/socket.h>
#include <sys/types.h>
#include <sys/uio.h>

#include <limits.h>
#include <netdb.h>
#include <stddef.h>
#include <string.h>
#include <unistd.h>

int callSocket(const char *hostname, unsigned short portnum) {
  struct sockaddr_in sa;
  struct hostent *hp;
  int conn = -1;
  int wasErr = 0;

  /* We do not support port number zero */
  if (portnum == 0)
    ERROR(isErr, wasErr, ERROR_INVALID_PORT);

  /* Get the host information for my hostname */
  hp = gethostbyname(hostname);
  if (hp == NULL)
    ERROR(isErr, wasErr, ERROR_HOSTNAME_INFORMATION);

  /* Create a reliable, bi-directional UNIX socket */
  conn = socket(AF_INET, SOCK_STREAM, 0);
  if (conn == -1)
    ERROR(isErr, wasErr, ERROR_CREATE_SOCKET);

  /* Create the socket information used for connecting */
  memset(&sa, 0, sizeof(struct sockaddr_in));
  memcpy(&sa.sin_addr, hp->h_addr, hp->h_length);
  sa.sin_family = hp->h_addrtype;
  sa.sin_port = htons(portnum);

  /* Connect the socket to the given hostname:portnum */
  if (connect(conn, (struct sockaddr *)&sa, sizeof(struct sockaddr_in)) == -1)
    ERROR(isErr, wasErr, ERROR_CONNECT_SOCKET);

isErr:
  if (wasErr) {
    if (conn != -1)
      close(conn);
    return wasErr;
  }
  return conn;
}

int socketRead(int sock, void *buf, size_t nBytes) {
  unsigned char *bufB = (unsigned char *)buf;
  size_t thisTime;
  ssize_t res;

  while (nBytes > 0) {
    /* read takes a size_t but has to return an ssize_t */
    thisTime = nBytes < ((size_t)SSIZE_MAX) ? nBytes : ((size_t)SSIZE_MAX);
    res = read(sock, bufB, thisTime);
    if (res == 0)
      return ERROR_SOCKET_DRY;
    if (res < 0)
      return ERROR_SOCKET_READ;
    bufB += res;
    nBytes -= (size_t)res;
  }

  return 0;
}

int socketWrite(int sock, const void *buf, size_t nBytes) {
  const unsigned char *bufB = (const unsigned char *)buf;
  size_t thisTime;
  ssize_t res;

  while (nBytes > 0) {
    /* write takes a size_t but has to return an ssize_t */
    thisTime = nBytes < ((size_t)SSIZE_MAX) ? nBytes : ((size_t)SSIZE_MAX);
    res = write(sock, bufB, thisTime);
    if (res < 0)
      return ERROR_SOCKET_WRITE;
    bufB += res;
    nBytes -= (size_t)res;
  }

  return 0;
}
