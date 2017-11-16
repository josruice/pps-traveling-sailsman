from subprocess import call
import sys
groups = ["g1", "g2", "g3", "g4", "g5","g6"]
landmarks = ["sparse_landmarks", "dense_landmarks"]

# get rid of outputs. write all configs. Print the right stuff
e = [1, 10] #percent of n^2
em = ["random_enemymap", "Quadmap", "Structures", "Diagonal"]
smalln = [11,20]
bign = [100]

smallnt = ["50", "100", "150", "200", "250"]
bignt = ["500", "1250", "2000", "2750"]

smallns = ["1","3","15"]
bigns = ["1","3","16","25","100"]
for g in xrange(5,6):
	for n in bign:
		for s in bigns:
			for lm in landmarks:
				for mapp in em:
					for enems in e:
						for t in bignt:
							calledthing = ["java", "scout.sim.Simulator", "-p", groups[g], 
							"-m", lm ,"-em", mapp, 
							"-n", str(n), "-e", str((enems*n*n)/100), "-s", s, "-t", t, "-S", "20"]
							#print calledthing,
							call(calledthing)
sys.exit()
for g in xrange(6):
	calledthing = ["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g1", 
	"-n", "100", "-e", "100", "-s", "11", "-t", "500", "-S", "20"]
	call(calledthing)

	calledthing = ["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g2", 
	"-n", "50", "-e", "70", "-s", "15", "-t", "400", "-S", "20"]
	call(calledthing)

	calledthing = ["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g3", 
	"-n", "100", "-e", "1200", "-s", "15", "-t", "2000", "-S", "20"]
	call(calledthing)

	calledthing =["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g4", 
	"-n", "100", "-e", "700", "-s", "15", "-t", "2000", "-S", "20"]
	call(calledthing)

	calledthing =["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g5", 
	"-n", "100", "-e", "1000", "-s", "3", "-t", "2000", "-S", "20"]
	call(calledthing)

	calledthing =["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g6", 
	"-n", "100", "-e", "800", "-s", "15", "-t", "1000", "-S", "20"]
	call(calledthing)


for g in xrange(6):
	calledthing = ["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g1", 
	"-n", "100", "-e", "100", "-s", "11", "-t", "500", "-S", "20"]
	call(calledthing)

	calledthing = ["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g2", 
	"-n", "50", "-e", "70", "-s", "15", "-t", "400", "-S", "20"]
	call(calledthing)

	calledthing = ["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g3", 
	"-n", "100", "-e", "1200", "-s", "15", "-t", "2000", "-S", "20"]
	call(calledthing)

	calledthing =["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g4", 
	"-n", "100", "-e", "700", "-s", "15", "-t", "2000", "-S", "20"]
	call(calledthing)

	calledthing =["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g5", 
	"-n", "100", "-e", "1000", "-s", "3", "-t", "2000", "-S", "20"]
	call(calledthing)

	calledthing =["java", "scout.sim.Simulator", "-p", groups[g], "-m", "sparse_landmarks" ,"-em", "g6", 
	"-n", "100", "-e", "800", "-s", "15", "-t", "1000", "-S", "20"]
	call(calledthing)

for g in xrange(6):
	for n in smalln:
		for s in smallns:
			for lm in landmarks:
				for mapp in em:
					for enems in e:
						for t in smallnt:
							calledthing = ["java", "scout.sim.Simulator", "-p", groups[g], 
							"-m", lm ,"-em", mapp, 
							"-n", str(n), "-e", str((enems*n*n)/100), "-s", s, "-t", t, "-S", "20"]
							#print calledthing,
							call(calledthing)

