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

OBJDIR=../obj
BINDIR=../bin

ifndef UAMP_PREFIX
UAMP_PREFIX=../../library/local
endif
UAMP_HEADER=$(realpath ${UAMP_PREFIX}/include)
UAMP_LIB=$(realpath ${UAMP_PREFIX}/lib)
ifeq "${UAMP_HEADER}" ""
$(error Invalid UAMP header directory. Check UAMP_PREFIX environment variable)
endif
ifeq "${UAMP_LIB}" ""
$(error Invalid UAMP library directory. Check UAMP_PREFIX environment variable)
endif

commandEcho_OBJS=commandEcho.o global.o
epidemic_OBJS=epidemic.o global.o
bin_LIBS=-luamp -lm

.PHONY:
.PHONY: clean
.SUFFIXES:
.SUFFIXES: .c .o
${OBJDIR}/%.o : %.c
	${CC} ${CFLAGS} -I${UAMP_HEADER} -c -o $@ $<

all: ${BINDIR}/commandEcho ${BINDIR}/epidemic

debug: CFLAGS=-Wall -DDEBUGGING -g
debug: all

profile: CFLAGS=-Wall -pg
profile: all

${BINDIR}/commandEcho: $(addprefix ${OBJDIR}/, ${commandEcho_OBJS})
	${CC} ${CFLAGS} -o $@ $^ -L${UAMP_LIB} ${bin_LIBS}
${BINDIR}/epidemic: $(addprefix ${OBJDIR}/, ${epidemic_OBJS})
	${CC} ${CFLAGS} -o $@ $^ -L${UAMP_LIB} ${bin_LIBS}

clean:
	@rm -f *~ \
	$(addprefix ${OBJDIR}/, ${commandEcho_OBJS}) \
	$(addprefix ${OBJDIR}/, ${epidemic_OBJS})

${OBJDIR}/commandEcho.o: commandEcho.c global.h
${OBJDIR}/epidemic.o: epidemic.c global.h
${OBJDIR}/global.o: global.c global.h
