from subprocess import call
import sys
groups = ["g1", "g2", "g3", "g4", "g5","g6"]
ts = [5,25,100,500]
tls = [1000,2500,10000,50000]
dts = [0.004, 0.015]
seed = 1234 # this is likely to be the only change for the tournament
for i in xrange(1000):
	for t,tl in zip(ts, tls):
		for dt in dts:
			calledthing = ["java", "sail.sim.Simulator", "-g", "6"]
			calledthing.extend(groups)
			calledthing.extend(["-t", str(t),"-tl",str(tl),"-dt",str(dt), "-S", str(seed),"--tournament"])
			call(calledthing)
			seed += 1
sys.exit()

