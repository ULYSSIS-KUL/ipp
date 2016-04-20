#!/usr/bin/env python

import json
import sys

f = open(sys.argv[1], 'r')

tag_to_team = { }

team_names = {
     1: 'Apolloon',
     2: 'Ekonomika',
     3: 'LBK',
     4: 'VTK',
     5: 'Wina',
     7: 'Psychokring',
     9: 'Atmosphere',
    10: '4 Speed',
    11: 'Industria',
    12: 'Oker',
    13: 'VRG',
    14: 'Politika',
    15: 'Run for Specials',
    16: 'Medica',
    17: 'Pedal & Hesteria',
    18: 'Lerkeveld'
}

reader_team_to_passes = {}
reader_team_to_pass_times = {}

for i in xrange(0,3):
    for j in xrange(1,19):
        reader_team_to_passes[(i,j)] = []
        reader_team_to_pass_times[(i,j)] = []

started = False

last_pass = {}

for line in f:
    line_obj = json.loads(line)
    if line_obj['type'] == 'Start':
        started = True
    elif line_obj['type'] == 'AddTag':
        tag_to_team[line_obj['tag']] = line_obj['teamNb']
    elif line_obj['type'] == 'RemoveTag':
        del tag_to_team[line_obj['tag']]
    elif line_obj['type'] == 'TagSeen':
        if started and line_obj['tag'] in tag_to_team:
            team_nb = tag_to_team[line_obj['tag']]
            tpl = (line_obj['readerId'], team_nb)
            if tpl not in last_pass:
                last_pass[tpl] = line_obj['time']
            else:
                diff = line_obj['time'] - last_pass[tpl]
                if diff > 30: # Filter out everthing below 30 seconds
                    last_pass[tpl] = line_obj['time']
                    reader_team_to_passes[tpl].append(line_obj['time'])
                    reader_team_to_pass_times[tpl].append(diff)

import matplotlib.pyplot as plt
import numpy as np

for i in xrange(0,3):
    for j in xrange(1,19):
        if j in (6,8):
            continue
        plt.figure(figsize=(20, 8))
        threshold = min(reader_team_to_pass_times[(i,j)]) * 2
        q1 = np.percentile(reader_team_to_pass_times[(i,j)], 25)
        q3 = np.percentile(reader_team_to_pass_times[(i,j)], 75)
        dq = q3 - q1
        maxlim = q3 + 1.5 * dq
        minlim = q1 - 1.5 * dq
        plt.plot(reader_team_to_passes[(i,j)], reader_team_to_pass_times[(i,j)])
        plt.axhline(y=threshold, color='r')
        plt.axhline(y=maxlim, color='c')
        plt.axhline(y=minlim, color='c')
        plt.ylim(ymin=0)
        plt.savefig('chart_{team:02d}_{team_name}_{reader}'.format(reader=i, team=j, team_name=team_names[j]))
        plt.close()

