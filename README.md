# PPS 2017 - Traveling Sailsman
Player code authors (group 2):
- Shreyas Mundhra
- Srinidhi Raghvan
- Tahmid Munat
- Jose Vicente Ruiz Cepeda

Simulator author: Kailash Meiyappan.


Arguments:

-g/--groups -> Number of players followed by player packages
E.g: -g 3 g1 g2 g3

-t/--targets -> Number of targets

-fs/--frameskip -> integer value, set to n means (n-1) frames skipped for each shown

-S/--seed -> seed for randomization, defaults to system current time millis

-f/--fps -> frames per second for gui

--gui -> gui enabled

--verbose -> verbose

-tl/--timelimit -> time limit in ms

-dt/--timestep -> timestep in double. Suggested/Default: 0.015

If the simulation gui is slow, crank up the speed by increasing fps and frameskip. You might want this if your player is so fast that the gui speed is limited by the refresh rate. Set frame skip to like ~ 5 made the gui ~5x faster for the random player...

The makefile gives you sample parameters. You can use the makefile for convenience if you want to. Makefile commands:

make compile

-> Compiles simulator

make gui

-> runs the simulator with gui

make run

-> runs without gui

make verbose

-> runs on verbose mode

There are some useful vector functions in Point.java. Simulator.getSpeed is public so you can use that too. The change in coordinates for one move is calculated as (speed) * (direction_vector) * dt

Good luck!
