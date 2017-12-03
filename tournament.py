import os
import sys
import pdb
import subprocess
import statistics
import random
from tqdm import tqdm

def save_float(x):
	try:
		return float(x)
	except ValueError:
		return None

primal_seed = 20171126
env_vars = [
    (5,   0.015, 1000,  10),
    (25,  0.015, 2500,  10),
    (100, 0.015, 10000, 10),
    (500, 0.015, 50000, 10),

    (5,   0.004, 1000,  10),
    (25,  0.004, 2500,  10),
    (100, 0.004, 10000, 10),
    (500, 0.004, 50000, 10)
]
players = ['g1', 'g2', 'g3', 'g4', 'g5', 'g6']

for num_targets, time_step, time_limit, repetition in env_vars:

	# Generating same seeds for each run
	random.seed(primal_seed)
	seeds = []
	for i in range(repetition):
		seeds.append(random.randrange(2147483647))
	print(seeds)

	results = {}
	for p in players:
		results[p] = {
            'final_scores': [],
            'time_remaining': []
        }

	not_best_score_rounds = []
	last_score_rounds = []

	for run in tqdm(range(repetition)):
		p = open("tmp.log", "w")
		err = open("err.log", "w")
		subprocess.run(["java", "sail.sim.Simulator",
            "--tournament",
            "--seed", str(seeds[run]),
            "-t", str(num_targets),
            "-dt", str(time_step),
            "-tl", str(time_limit),
            "-g", str(len(players))] + players, stdout = p, stderr = err)
		p.close()
		err.close()

		with open("tmp.log", "r") as log:
			t = log.readlines()[-1]
			parsed_log = [save_float(s) for s in t.split(', ')][:-1]

			our_final_score = None
			round_final_scores = []
			for i, player in enumerate(players):
				player_base_index = (2*i) - len(players)*2
				final_score = parsed_log[player_base_index]
				time_remaining = parsed_log[player_base_index+1]
				round_final_scores.append(final_score)
				if player == 'g5':
					our_final_score = final_score
				results[players[i]]['final_scores'].append(final_score)
				results[players[i]]['time_remaining'].append(time_remaining)

# Use this checks to find runs where our strategy might not work.
			if (our_final_score == min(round_final_scores)):
				last_score_rounds.append(
					"Ours: %d, best: %d (--seed %d -t %d -dt %f -tl %d)" % (
						our_final_score, max(round_final_scores),
						seeds[run], num_targets, time_step, time_limit
					)
				)
			elif (our_final_score < max(round_final_scores)):
				not_best_score_rounds.append(
					"Ours: %d, best: %d (--seed %d -t %d -dt %f -tl %d)" % (
						our_final_score, max(round_final_scores),
						seeds[run], num_targets, time_step, time_limit
					)
				)

			log.close()

	print()
	for player, scores in results.items():
		final_scores = scores['final_scores']
		time_remaining = scores['time_remaining']

		print("%d,%d,%d,%d,%d,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f" % (
            repetition,
            primal_seed,
            num_targets,
            time_step,
            time_limit,
            player,

            statistics.mean(final_scores),
            statistics.median(final_scores),
            min(final_scores),
            max(final_scores),
            statistics.pstdev(final_scores),

            statistics.mean(time_remaining),
            statistics.median(time_remaining),
            min(time_remaining),
            max(time_remaining),
            statistics.pstdev(time_remaining)
        ))
	print()
	if len(last_score_rounds) > 0:
		print('We have been LAST for the following runs: \n - ')
		print('\n - '.join(last_score_rounds))
# print()
# if len(not_best_score_rounds) > 0:
#     print('We have been NOT FIRST for the following runs: \n - ', end='')
#     print('\n - '.join(not_best_score_rounds))
    print('\n')