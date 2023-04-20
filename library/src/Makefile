# Copyright (c) 2009-2023 Ryan Vogt <rvogt@ualberta.ca>
#
# Permission to use, copy, modify, and/or distribute this software for any
# purpose with or without fee is hereby granted, provided that the above
# copyright notice and this permission notice appear in all copies.
#
# THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
# WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
# SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
# WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION
# OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
# CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

# GNU Makefile

CC=cc
CFLAGS=-Wall -O2
AR=ar

OBJDIR=../obj

ifndef UAMP_PREFIX
UAMP_PREFIX=../local
endif
INSTALL_HEADER=${UAMP_PREFIX}/include
INSTALL_LIB=${UAMP_PREFIX}/lib

library_OBJS=errors.o ioBuffer.o queues.o socketWrapper.o states.o uampClient.o

.PHONY:
.PHONY: clean
.SUFFIXES:
.SUFFIXES: .c .o
${OBJDIR}/%.o : %.c
	${CC} ${CFLAGS} -c -o $@ $<

all: ${OBJDIR}/libuamp.a

install: ${OBJDIR}/libuamp.a
	mkdir -p ${INSTALL_HEADER}
	mkdir -p ${INSTALL_LIB}
	cp uampClient.h ${INSTALL_HEADER}/uampClient.h
	cp ${OBJDIR}/libuamp.a ${INSTALL_LIB}/libuamp.a

${OBJDIR}/libuamp.a: $(addprefix ${OBJDIR}/, ${library_OBJS})
	${AR} -rcs $@ $^

clean:
	@rm -f *~ \
	$(addprefix ${OBJDIR}/, ${library_OBJS}) \
	${OBJDIR}/libuamp.a

${OBJDIR}/errors.o: errors.c errors.h
${OBJDIR}/ioBuffer.o: ioBuffer.c errors.h ioBuffer.h uampClient.h \
  socketWrapper.h
${OBJDIR}/queues.o: queues.c errors.h ioBuffer.h uampClient.h queues.h
${OBJDIR}/socketWrapper.o: socketWrapper.c errors.h socketWrapper.h
${OBJDIR}/states.o: states.c errors.h ioBuffer.h uampClient.h states.h
${OBJDIR}/uampClient.o: uampClient.c errors.h ioBuffer.h uampClient.h \
  queues.h socketWrapper.h states.h
