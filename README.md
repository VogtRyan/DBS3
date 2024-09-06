# DBS3
The Destination-Based Space Syntax Simulator

**Table of Contents:**
1. [Copyright and License](#copyright-and-license)
2. [Quick Start](#quick-start)
3. [About DBS3](#about-dbs3)
4. [Building and Installing DBS3](#building-and-installing-dbs3)
5. [Running the DBS3 UAMP Server](#running-the-dbs3-uamp-server)
6. [Large-Scale Experiments with DBS3](#large-scale-experiments-with-dbs3)
7. [Creating a UAMP or MVISP Client](#creating-a-uamp-or-mvisp-client)
8. [Using the GUI](#using-the-gui)
9. [Pathfinding Algorithms](#pathfinding-algorithms)
10. [Destination Selection Algorithms](#destination-selection-algorithms)
11. [Map Files](#map-files)

## Copyright and License

Copyright (c) 2007-2024 Ryan Vogt <rvogt@ualberta.ca>

This software is released under the ISC license. See the `LICENSE` file for
more details.

The map images included in this project are from Google Maps, circa 2009, and
the images themselves have been stamped to indicate their origin.

## Quick Start

DBS3 is a tool for generating large amounts of realistic outdoor urban
pedestrian mobility data, which can be consumed by other applications (e.g.,
for the purposes of running simulations, experiments, visualizations, etc.).
But, if you would like to start by seeing DBS3 run a single, simple simulation
before you read a long, detailed document about it, this section is for you.

#### Prerequisites
- A Java 9 or higher JDK, and Apache Ant to build the code
- A C99 compiler with GNU make

#### Build the Core of DBS3
The following builds the main component of DBS3, the Java-based mobility
simulation server:
```
% cd java/
% ant
```

#### Build the UAMP C library
The following builds the C library that the client code will link:
```
% cd ../library/src/
% make
```

#### Install the C library locally
The following will install the C library in the same directory as the
repository, just for now:
```
% unset UAMP_PREFIX
% make install
```

#### Build the Epidemic Simulation Client
The following builds a client that consumes mobility data in order to simulate
an epidemic, and produces results that are easy to see and interpret:
```
% cd ../../clients/src/
% make
```

#### Start the DBS3 GUI
Launch the GUI, to see the urban centre in which the epidemic will spread:
```
% cd ../../bin/
% ./gui &
```

#### Start a New Simulation
Begin a new simulation that will receive visualizable data from the client:
- From the "Simulation" menu, choose "New simulation..."
- At the bottom of the dialog, enable "MVISP server" (on port 40000)
- Click "Start Simulation"
- At this point, the GUI should be awaiting a connection from an MVISP client

#### Run the Epidemic Client
Run the epidemic simulation client from the `bin/` directory:
```
% ./epidemic -m localhost 40000
```

#### Watch the Result
Watch an infection spread from one individual throughout an urban centre:
- Use the buttons and slider below the map to control the simulation
- Or, use shortcut keys like space to play and pause the simulation, +/- to
  speed it up or slow it down, and the left/right arrows (holding shift,
  control, combinations thereof, etc.) to fast-forward or rewind

#### Getting Help

For any of the command-line programs in the `bin/` directory, use a single
`--help` argument to see more options:
```
% ./epidemic --help
```

Included with the GUI are two urban environments that you can open from the
"File" menu. You will find many other visualization options in the menus as
well.

## About DBS3

The Destination-Based Space Syntax Simulator (DBS3) is a human mobility
simulator for outdoor, urban environments, based on the principle of space syntax.
Using DBS3, you can send
mobility data to clients. One practical example of a client would be a wireless
network simulator, capable of simulating network communication but requiring
data for where each person with a wireless device is at any given point in
time. Another example of a client would be a flu epidemic simulator, capable
of modelling the spread of the disease from person to person, but requiring
mobility data for where each person moves.

DBS3 uses maps to constrain the movement of each agent, allowing agents to walk
on streets but nowhere else. Two sample maps, downtown Edmonton, Canada and
central Fira, Greece, are included in the `maps/` directory; it is easy to
create additional maps if you wish.

There are two different but similar protocols used by DBS3: the UAMP protocol
and the MVISP protocol. Both are described in detail in the `rfc/` directory.

UAMP clients send a simulation request to DBS3, including the number of agents
they want simulated, the duration of movement data they need, and a random seed
for the server to use (different seeds result in different movement data). The
UAMP server then sends the requested mobility data to the client. UAMP clients
can prematurely terminate a simulation if they no longer require any more
movement data, so clients that do not know a priori how much movement data they
will require should request the maximum possible duration from DBS3.
Practically speaking, a UAMP client should be used when you want to run
experiments that require mobility data. For example, a flu simulator could run
100 trials of an experiment by sending 100 different seeds to the UAMP server,
thus receiving 100 different sets of movement data.

MVISP clients, on the other hand, receive a simulation specification from DBS3,
including the number of agents that DBS3 will be simulating and the duration of
the simulation. The MVISP client receives this movement data, and sends
notifications back to the MVISP server when agents undergo state changes. For
example, an MVISP flu-simulation client might send notifications to DBS3 when
agents (i.e., people) change from the uninfected state to the incubating state.
Practically speaking, an MVISP client should be used when you want to visualize
the results of a single trial of an experiment. DBS3 includes a GUI with a
built-in MVISP server, allowing you to watch state changes unfold in a single
run of a mobility simulation.

DBS3 is described extensively in: R. Vogt, I. Nikolaidis, and P. Gburzynski.
A realistic outdoor urban pedestrian mobility model.
*Simulation Modelling Practice and Theory*, 26:113&ndash;134, 2012.

If you use DBS3 in any academic work, we kindly ask that you cite this paper.
The preprint of this paper is included in the `paper/` directory.

Also, we really do love hearing about what people are doing with DBS3. If you
would like, please feel free to send us an email and let us know how it's
working for you.

## Building and Installing DBS3

The Java components, including the UAMP server and the GUI with the MVISP
server, are written in Java 9. To build the Java components, run:
```
% cd java/
% ant
```

If you wish to build the accompanying Javadoc documentation, e.g., for the
purposes of editing the DBS3 codebase, also run:
```
% ant javadoc
```

Included with DBS3 is a C library for rapid development of your own UAMP and/or
MVISP clients. This library provides meaningful functions for interacting with
UAMP and MVISP servers, as well as error checking (so your clients can be
certain the data they are receiving from the UAMP or MVISP server is consistent
and sane). To build the library, run:
```
% cd ../library/src/
% make
```

You can install the library in a system-wide location by modifying the
`UAMP_PREFIX` environment variable. If `UAMP_PREFIX` is defined, the library
will be installed in `${UAMP_PREFIX}/lib/`, and the header file in
`${UAMP_PREFIX}/include/`:
```
% export UAMP_PREFIX=/usr/local
% sudo make install
```

If `UAMP_PREFIX` is not set, the library will be installed by default in this
directory, i.e., in the `library/local/include` and `library/local/lib`
directories inside the repository:
```
% unset UAMP_PREFIX
% make install
```

Note that on systems where GNU make is not the default make, it is necessary to
use `gmake` instead of `make`.

Also included with DBS3 are two sample clients, written in C. To build the
clients, run:
```
% cd ../../clients/src/
% make
```

The client makefile expects to find the UAMP library in
`../../library/local/lib` and `../../library/local/include` by default, unless
`UAMP_PREFIX` is set.

This library has also been ported to Java, for rapid development of Java UAMP
and/or MVISP clients. To compile the library, run:
```
% cd ../../java/
% ant library
```

The library will be compiled as `java/libout/dbs3-<VERSION>.jar`. Include that
file in your `CLASSPATH` to use the library.

## Running the DBS3 UAMP Server

After the Java portion of DBS3 has been built, you can launch DBS3's UAMP
server by running `bin/uampServer`. You can see the options that the UAMP
server takes by running
```
% bin/uampServer --help
```
In particular, you can specify the map file on which the server will perform
its simulations, the minimum and maximum speeds at which agents can possibly
walk, and the pathfinding and destination-selection algorithms.

Once running, DBS3's UAMP server can feed mobility data to any number of programs
or experiments that require paths and locations for moving agents.

The `-daemonize` option of `uampServer` will detach the server from the shell
once its socket is ready to accept connections. The PID of the server will then
be printed to standard output. This makes it easy to launch and terminate
DBS3's UAMP server from inside shell scripts that perform extensive mobility
experiments. For example, the following shell script will start an instance of
the UAMP server, perform a large number of experiments (i.e., make many client
connections to the UAMP server), then terminate the UAMP server:

```
#!/bin/sh
killServer=0
trap "killServer=1" 2 3 15
cd bin/
pid=$(./uampServer -daemonize)
trap "kill ${pid} >/dev/null 2>&1 ; exit" 0 2 3 15
if [ ${killServer} -eq 1 ] ; then
    kill ${pid} >/dev/null 2>&1
    exit
fi

# Insert experiments here

# End of script
# No need to kill ${pid} here; the trap 0 will do it
```

## Large-Scale Experiments with DBS3

One of the included DBS3 clients, `epidemic`, is intended to be a simple
demonstration of what a full-featured UAMP and MVISP client, used for
large-scale simulations, would look like. It receives movement data from a
UAMP or MVISP server, and simulates an airborne virus outbreak among the moving
agents. The actual rules for viral propagation are simplistic &mdash; the goal
of this program is to demonstrate DBS3's API and role as a mobility simulation
tool in large-scale experiments, not to be a realistic epidemiology
representation. Any uninfected, vulnerable person that comes within $R$ metres
of a contagious person catches the disease. Anyone who has caught the disease
becomes contagious after $T$ seconds. The parameters $R$ and $T$ are chosen on
the command line; run
```
% ./epidemic --help
```
to see what other options are available.

As is demonstrated in the [Quick Start](#quick-start), you can use the `-m`
flag to run `epidemic` as an MVISP client, to watch a single outbreak with the
DBS3 GUI.

But more importantly from a large-scale simulation perspective, `epidemic` can
run as a UAMP client, executing one complete simulated outbreak per execution
of `epidemic`. It can append the results of each simulation to a running
epidemic result file, using a different random seed (specified by `-s`) for
each execution:
```
% ./epidemic --epidemicFile results.txt -s 1000 localhost 40000 >/dev/null
% ./epidemic --epidemicFile results.txt -s 1001 localhost 40000 >/dev/null
% ./epidemic --epidemicFile results.txt -s 1002 localhost 40000 >/dev/null
% [...]
```
The results in the running file `results.txt` can be analyzed by other
statistical scripts or programs after all of the replicate simulations with
different random seeds are complete. In this example, the format and contents
of `results.txt` are determined entirely by the `epidemic` client &mdash;
clients can do whatever is desired with the mobility data they receive.

There are two important notes about random seeds sent to DBS3's UAMP server,
which are critical for large-scale experiments requiring mobility data:
- Seed values sent to DBS3 are run through a cryptographic hash function by the
  DBS3 server itself, meaning there will be no bias introduced by using related
  but non-identical seed values (e.g., 1000, 1001, 1002, etc.) in different
  trials of an experiment.
- Adding or removing agents to a simulation with a fixed seed value does not
  alter the behaviour of the other agents in the simulation. That is, a
  simulation using seed 1000 with 501 agents will be the same as a simulation
  using the seed 1000 with 500 agents, aside from the one additional agent.
  Put another way, the behaviour of the original 500 agents is not affected by
  the addition of the 501st agent.

As such, scripts running large-scale experiments are free to use simple
incrementing, non-repeating seed values. As an example, 30 trials of an
outbreak in Edmonton could be run using seeds 1000 through 1029; then, 30
trials of an outbreak in Fira could be run using seeds 1030 through 1059. All
of the simulations would be independent.

Alternately, scripts are free to add or remove agents from a fixed-seed
simulation. The same environment in Fira could have 10 more agents added to it
without altering the movement of the other agents:
```
% ./epidemic --epidemicFile results.txt -u 500 -s 1000 localhost 40000 >/dev/null
% ./epidemic --epidemicFile results.txt -u 510 -s 1000 localhost 40000 >/dev/null
```

## Creating a UAMP or MVISP Client

The included `epidemic` client is included as a demonstration of DBS3's
capabilities, and as an example of using DBS3's API. Most users of DBS3 will
want to create their own clients to consume the mobility data produced by DBS3.

DBS3 includes a library for rapid development of UAMP and/or MVISP clients in
C, and another library for development in Java. This section describes the
usage of the C library. The Java library is almost identical, so this
documentation in fact describes the programmatic usage of both libraries.
Javadoc-style documentation of the function call names and parameters of the
Java version can be read in the `java/javadoc/` directory; see, in particular,
the `UAMPClient` and `MVISPClient` classes.

After you [install the UAMP library](#building-and-installing-dbs3) for C
development, your client should
```
#include <uampClient.h>
```
You may wish to install the library in, e.g., `/usr/local/`. You will have to
link your program against `libuamp.a` by including the `-luamp` flag during
linking.

The `uampClient.h` header provides the `struct uampClient`. You should not
modify the contents of this structure directly; instead, let the functions in
`uampClient.h` modify the structure for you.

Use either the `uampConnect` or `mvispConnect` function to initialize the
`struct uampClient`, connecting to either a UAMP or MVISP server respectively.
The structure is cleaned up and the client disconnected when the
`uampTerminate` function is called.

Once a connection is established, commands for each agent are buffered by the
`struct uampClient`. A command is an instruction for an agent to move in a
straight line at a constant speed, starting at a given location and point in
time and arriving at a given location at a given point in time (pause times are
represented as zero-speed commands). The initial command buffered by the
`uampClient` for each agent is the "initial position" command, in which the
start and finish times of the command are both 0.

The `uampCurrentCommand` function is used to retrieve the currently buffered
command for an agent. To request the next command for an agent from the UAMP
or MVISP server, representing the next straight-line constant-speed movement by
the agent, use the `uampAdvance` function. The `uampIsMore` function should be
called before calling `uampAdvance`, to determine if there is more movement
data to request for a given agent. The library buffers commands ahead of time,
so each call to `uampAdvance` does not necessarily result in an exchange
between the client and server applications.

The `struct uampClient` is also capable of presenting a synchronous view of
agent movement. In this view, the current commands for each agent have the
same start and end times, and represent periods of time in which all agents
move in a straight line and at a constant speed (that is, the next period of
time begins when any agent changes speed or direction). The
`uampIntersectCommand` function returns the current synchronous command for a
given agent, and the `uampAdvanceOldest` command advances time for all of the
agents. The `uampIsAnyMore` function should be used to determine whether there
is any more movement data to request from the server.

Finally, use the `uampChangeState` function to send state changes back to an
MVISP server (if a UAMP client calls this function, it does nothing).

Two example clients are provided in the `clients/` directory. The first,
`commandEcho`, is a UAMP-only client that prints the movement data it receives
from a UAMP server to standard output. You can compare the output of
`commandEcho` with the output of `bin/uampSimulation`, which makes the Java
DBS3 code print the movement data of a single movement simulation directly to
standard out. The other client is `epidemic`, which is described in detail
in the context of
[large-scale experiments with DBS3](#large-scale-experiments-with-dbs3).

## Using the GUI

To launch the DBS3 GUI, used for visualizing movement simulations, run
`bin/gui`. Most of its functionality should be straightforward and intuitive.

When you start a new simulation, you will be asked to specify what pathfinding
and destination-selection algorithms DBS3 should use to generate its mobility
data. Both the [pathfinding](#pathfinding-algorithms) and
[destination-selection](#destination-selection-algorithms) algorithms are
described in their own sections of this document.

When creating a new movement simulation to view, you also have the option to
start an MVISP server. If you choose this option, the GUI will wait until it
receives a connection from an MVISP client. The server will then send the
simulation data to the MVISP client and receive back the state changes that the
agents undergo during the course of their movement. The GUI will then display
the agent states at any given time by the colour of each agent.

The GUI will remember what colours you choose for various states, as well as
what size circle you want to draw for each agent, in a configuration file. You
can change which configuration file the DBS3 GUI uses by passing it the
`-config` option, or create a new configuration file with the `-newConfig`
option.

If the performance of the GUI is poor (i.e., laggy), you can disable
anti-aliasing by exporting a non-empty value to the environmental variable
`DBS3_NOAA`. For example, in `bash`:
```
% export DBS3_NOAA=1
```
That said, this option is likely of limited usefulness for modern computers.
But, it remains as a relic of getting DBS3 to run on very old computers in a
lab environment.

## Pathfinding Algorithms

There are three pathfinding algorithms in DBS3 &mdash; that is, algorithms that
determine how agents will move from their current location to their next chosen
destination. All three algorithms share the same basic premise, though their
actual implementations are significantly optimized from how it is described
here. Each time the pathfinder is executed, it generates a random point in
each intersection on the map; then, for each intersection, that intersection's
random point is mirrored into all four quadrants of that intersection. The end
result is four random points in every intersection (that are different for each
execution of the pathfinding algorithm). An agent on a given street is free to
move in a straight line to any of the four points in any intersection on that
street, or in a straight line directly to the destination if the destination is
on that street.

The algorithms differ in which of the potential paths returned by this search
is chosen as the actual path for the agent to take to the destination. The
algorithm that minimizes Euclidean distance chooses the path with the smallest
amount of Euclidean distance travelled. The algorithm that minimizes the number
of turns chooses the path with the fewest changes in direction. Finally, the
algorithm that minimizes angle change chooses the path with the smallest sum
over all the angles of the changes in direction. For both of the latter two
algorithms, ties are broken by minimizing Euclidean distance travelled.

The efficient implementations of these algorithms, using the multi-expansion
A* search (MEA* search) and the StreetCut extension to MEA*, are described in
the paper found in the `paper/` directory.

## Destination Selection Algorithms

The integrated destination selection algorithm in DBS3 is responsible for
choosing a new destination for an agent after it reaches its previous
destination. The algorithm is controlled by three constants: a floating point
value $\alpha \ge 0.0$, a floating point value $\delta \ge 0.0$, and an integer
value $\rho \ge 0$ ($\rho$ is also called the "radius"). The exponent $\alpha$
controls how much influence centrality has on destination selection, and the
radius $\rho$ controls how wide an area (measured in turns) is considered when
computing the centrality of a location. A higher value of $\alpha$ leads to
greater centrality bias, meaning that agents will tend to prefer destinations
that are fewer turns away from lots of other destinations (more central). The
other exponent, $\delta$, controls how much influence distance decay has on
destination selection. A higher value of $\delta$ leads to greater distance
decay, meaning that agents will tend to prefer destinations that are fewer
turns away from their current location.

Formally, let $\textnormal{L}(s)$ be the length of street $s$, and let
$\textnormal{D}(s,d)$ be the distance between a source street $s$ and a
potential destination street $d$. We define
$\textnormal{D}(s,d)$ as $\textnormal{T}(s,d)+1$, where $\textnormal{T}(s,d)$
is the minimum number of turns an agent would have to make to get from $s$ to
$d$. Note that $\textnormal{D}(s,d) \ge 1$.

The destination-based integration value of a potential destination street $d$
measures how central, or how well-connected, street $d$ is within its
neighbourhood. Formally, the destination-based integration value of street $d$,
within a fixed radius, is
```math
\textnormal{I}(d) =
\frac{\sum_{i~|~\textnormal{T}(i, d) \le \rho}
{\left( \textnormal{L}(i) \cdot \textnormal{D}(i,d) \right)}}
{\sum_{i~|~\textnormal{T}(i, d) \le \rho}{\textnormal{L}(i)}}\;.
```
Note that $1 \le \textnormal{I}(d) \le \rho+1$, with a large value of
$\textnormal{I}(d)$ representing that street $d$ is poorly connected
(non-central) within its neighbourhood.

The probability of an agent choosing a destination on street $d$ as a
destination, given that it is starting on street $s$, is proportional to
```math
\textnormal{P}(s, d) =
\frac{\textnormal{L}(d)}{\textnormal{I}(d)^\alpha \cdot
\textnormal{D}(s, d)^\delta}\;.
```
If the destination street is chosen to be street $d$, all locations on street
$d$ are equally probably to be chosen as the destination.

Note that if either $\alpha = 0$ or $\rho = 0$, centrality (i.e., the
integration value) has no effect on destination selection. If $\delta = 0$,
distance decay has no effect on destination selection. If both of these
conditions are met, the selection algorithm degenerates into a uniform
destination selection algorithm (all destinations are equally likely to be
chosen as the destination).

## Map Files

Map files are plain-text documents, describing (in any units of measure you
wish) where streets are located and what width they are. The file provides a
conversion from the units you used to metres. Maps can also include images
(such as a Google maps screenshot), along with a conversion scale from metres
to pixels on the image. These images are used when you visualize the movement
of agents with the DBS3 GUI.

The `bin/` directory includes two command-line tools for analyzing map files.

The first tool, `describeMap`, prints a description of all of the streets and
intersections in a map file to standard out. While that use of `describeMap`
was largely obsoleted with the introduction of the DBS3 GUI, `describeMap`
still has the ability to compute and output statistics about a given map file.

The second tool, `steadyState`, runs a DBS3 mobility simulation on the given
map file in which a single agent travels to a random sequence of destinations
at a constant speed with no pause time at the destinations, according to the
destination selection and pathfinding algorithms specified on the command line.
The tool then outputs the percentage of time spent on each street (as a value
between 0 and 100). Data can also be output with segment granularity. This tool
is used to determine the effect of the pathfinding and destination selection
algorithms on which thoroughfares are favoured by agents.
