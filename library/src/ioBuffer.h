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

#ifndef __IO_BUFFER_H__
#define __IO_BUFFER_H__

#include "uampClient.h"

#include <stdint.h>

/*
 * Begins a new read operation that will return the given amount of data in
 * total.
 */
void beginRead(struct uampIOBuffer *buf, uint64_t total);

/*
 * Fetches an 8- or 32-bit unsigned integer (converted from network order) or
 * the given amount of raw data from the buffer.  The buffer will refill as
 * necessary from the given file descriptor, up to the number of bytes dictated
 * to the beginRead function.  Return 0 on success or a negative value on
 * error.
 */
int socketRead8(struct uampIOBuffer *buf, int fd, uint8_t *input);
int socketRead32(struct uampIOBuffer *buf, int fd, uint32_t *input);
int socketReadRaw(struct uampIOBuffer *buf, int fd, void *data,
                  uint64_t length);

/*
 * Begins a new write operation that will write the given amount of data in
 * total.
 */
void beginWrite(struct uampIOBuffer *buf, uint64_t total);

/*
 * Writes an 8- or 32-bit unsigned integer (converted to network order) or the
 * given amount of raw data to the buffer.  The buffer will be flushed to the
 * given file descriptor as necessary: either when the buffer runs out of
 * space, or when the last bytes (as dictated to the beginWrite function) are
 * written to the buffer.  Returns 0 on success or a negative value on error.
 */
int socketWrite8(struct uampIOBuffer *buf, int fd, uint8_t output);
int socketWrite32(struct uampIOBuffer *buf, int fd, uint32_t output);
int socketWriteRaw(struct uampIOBuffer *buf, int fd, const void *data,
                   uint64_t length);

#endif
