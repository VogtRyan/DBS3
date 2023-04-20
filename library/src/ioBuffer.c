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

#include "ioBuffer.h"

#include "errors.h"
#include "socketWrapper.h"

#include <sys/types.h>
#include <sys/uio.h>

#include <arpa/inet.h>

#include <stdint.h>
#include <string.h>
#include <unistd.h>

void beginRead(struct uampIOBuffer *buf, uint64_t total) {
  /*
   * We have not yet returned any of the data to the owner of this buffer, and
   * we have not read any data from a file descriptor into the buffer.
   */
  buf->total = total;
  buf->passed = 0;
  buf->inBuffer = 0;
}

int socketRead8(struct uampIOBuffer *buf, int fd, uint8_t *input) {
  return socketReadRaw(buf, fd, (void *)input, sizeof(uint8_t));
}

int socketRead32(struct uampIOBuffer *buf, int fd, uint32_t *input) {
  uint32_t val;
  int ret = socketReadRaw(buf, fd, &val, sizeof(uint32_t));
  if (ret != 0)
    return ret;
  *input = ntohl(val);
  return 0;
}

int socketReadRaw(struct uampIOBuffer *buf, int fd, void *data,
                  uint64_t length) {
  int readRet;
  size_t thisTime;
  unsigned char *dataB = (unsigned char *)data;
  uint64_t remaining;
  int wasErr = 0;

  /*
   * Assert that we are not requesting more data from the buffer than was
   * dictated during the beginRead() call.
   */
  uint64_t totalPassed = buf->passed + length;
  ASSERT(totalPassed >= buf->passed && totalPassed >= (uint64_t)length &&
             totalPassed <= buf->total,
         "Too much data from read buffer");

  /*
   * Loop until we have placed enough data from the buffer into the location
   * provided as an argument to this function.
   */
  while (length > 0) {
    /*
     * If the buffer has been emptied, we have to refill it.  From the file
     * descriptor, we read at most: (a) the size of the buffer, and (b) the
     * amount of data that still needs to be returned, based on the beginRead()
     * call.  If we read less than the total size of the buffer, we read into
     * the high indices of the buffer (rather than starting at index 0 of the
     * buffer).
     */
    if (buf->inBuffer == 0) {
      remaining = buf->total - buf->passed;
      if (remaining < (uint64_t)UAMP_IO_BUFFER_SIZE)
        thisTime = (size_t)remaining;
      else
        thisTime = (size_t)UAMP_IO_BUFFER_SIZE;
      readRet =
          socketRead(fd, (buf->buffer) + (UAMP_IO_BUFFER_SIZE - thisTime),
                     (uint32_t)(thisTime));
      ERROR_CHECK(isErr, wasErr, readRet);
      buf->inBuffer = (int)thisTime;
    }

    /*
     * Copy data out of our buffer into the argument-given location.  We can
     * copy at most the remaining contents in the buffer, but no more than what
     * still remains to be placed in the argument location.
     */
    if (length < (uint64_t)(buf->inBuffer))
      thisTime = (size_t)length;
    else
      thisTime = (size_t)(buf->inBuffer);
    memcpy(dataB, (buf->buffer) + (UAMP_IO_BUFFER_SIZE - buf->inBuffer),
           thisTime);
    length -= (uint64_t)thisTime;
    dataB += thisTime;
    buf->inBuffer -= (int)thisTime;
    buf->passed += (uint64_t)thisTime;
  }

isErr:
  return wasErr;
}

void beginWrite(struct uampIOBuffer *buf, uint64_t total) {
  /*
   * We have not yet been passed any data by the user, so there is no data in
   * the buffer.
   */
  buf->total = total;
  buf->passed = 0;
  buf->inBuffer = 0;
}

int socketWrite8(struct uampIOBuffer *buf, int fd, uint8_t output) {
  return socketWriteRaw(buf, fd, &output, sizeof(uint8_t));
}

int socketWrite32(struct uampIOBuffer *buf, int fd, uint32_t output) {
  output = htonl(output);
  return socketWriteRaw(buf, fd, &output, sizeof(uint32_t));
}

int socketWriteRaw(struct uampIOBuffer *buf, int fd, const void *data,
                   uint64_t length) {
  int writeRet;
  size_t thisTime;
  unsigned char *dataB = (unsigned char *)data;
  int wasErr = 0;

  /*
   * Assert that we are not requesting more data from the buffer than was
   * dictated during the beginRead() call.
   */
  uint64_t totalPassed = buf->passed + (uint64_t)length;
  ASSERT(totalPassed >= buf->passed && totalPassed >= (uint64_t)length &&
             totalPassed <= buf->total,
         "Too much data to write buffer");

  /* Loop until we have placed all the provided data into the buffer */
  while (length > 0) {
    /*
     * We can place at most into the buffer: (a) the amount of space remaining
     * in the buffer, or (b) the amount of data left to put in the buffer.
     */
    if (length < (uint64_t)(UAMP_IO_BUFFER_SIZE - buf->inBuffer))
      thisTime = (size_t)length;
    else
      thisTime = (size_t)(UAMP_IO_BUFFER_SIZE - buf->inBuffer);
    memcpy((buf->buffer) + (buf->inBuffer), dataB, thisTime);
    length -= (uint64_t)thisTime;
    dataB += thisTime;
    buf->inBuffer += (int)thisTime;
    buf->passed += (uint64_t)thisTime;

    /*
     * If the buffer is full or if we've received all the data that we're going
     * to receive, flush to the file descriptor.
     */
    if (buf->inBuffer == UAMP_IO_BUFFER_SIZE || buf->passed == buf->total) {
      writeRet = socketWrite(fd, buf->buffer, (size_t)(buf->inBuffer));
      ERROR_CHECK(isErr, wasErr, writeRet);
      buf->inBuffer = 0;
    }
  }

isErr:
  return wasErr;
}
