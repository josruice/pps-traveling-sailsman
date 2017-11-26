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
    # (20,  0.015, 1000, 100),
    (100, 0.015, 10000, 100),
    # (100, 0.015, 20000, 10)
]
players = ['g1', 'g2', 'g3', 'g4', 'g5', 'g6']

for num_targets, time_step, time_limit, repetition in env_vars:

    # Generating same seeds for each run
    random.seed(primal_seed)
    seeds = [];
    for i in range(repetition):
        seeds.append(random.randrange(2147483647))
    print(seeds)

    results = {}
    for p in players:
        results[p] = { 'final_scores': [] }

    not_best_score_rounds = []
    last_score_rounds = []

    for run in tqdm(range(repetition)):
        p = open("tmp.log", "w")
        err = open("err.log", "w")
        subprocess.run(["java", "sail.sim.Simulator",
            "--seed", str(seeds[run]),
            "-t", str(num_targets),
            "-dt", str(time_step),
            "-tl", str(time_limit),
            "-g", str(len(players))] + players, stdout = p, stderr = err)
        p.close()
        err.close()

        with open("tmp.log", "r") as log:
            t = log.readlines()[-len(players):]
            our_final_score = None
            round_final_scores = []
            for i in range(len(players)):
                parsed_log = [save_float(s) for s in t[i].split()]
                final_score = parsed_log[-1]
                round_final_scores.append(final_score)
                if players[i] == 'g5':
                    our_final_score = final_score
                results[players[i]]['final_scores'].append(final_score)

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

        print("%d,%d,%d,%d,%d,%s,%.2f,%.2f,%d,%d,%.2f" % (
            repetition,
            primal_seed,
            num_targets,
            time_step,
            time_limit,
            player,

            statistics.mean(final_scores),
            statistics.median(final_scores),
            int(min(final_scores)),
            int(max(final_scores)),
            statistics.pstdev(final_scores)
        ))
    print()
    if len(last_score_rounds) > 0:
        print('We have been LAST for the following runs: \n - ', end='')
        print('\n - '.join(last_score_rounds))
    print()
    if len(not_best_score_rounds) > 0:
        print('We have been NOT FIRST for the following runs: \n - ', end='')
        print('\n - '.join(not_best_score_rounds))
    print('\n')
