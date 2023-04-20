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

#ifndef __SOCKET_WRAPPER_H__
#define __SOCKET_WRAPPER_H__

#include <stddef.h>

/*
 * Make a connection to the socket located at hostname:portnum.  Return the
 * descriptor on success or a negative value on error.
 */
int callSocket(const char *hostname, unsigned short portnum);

/*
 * Read or write numBytes of raw data from/into buffer from/into the given
 * socket.  Return 0 on success, or a negative value on error.
 */
int socketRead(int sock, void *buffer, size_t numBytes);
int socketWrite(int sock, const void *buffer, size_t numBytes);

#endif
