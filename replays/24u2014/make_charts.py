#!/usr/bin/env python

import json
import sys

f = open(sys.argv[1], 'r')

tag_to_team = {
    '002420140001': 1,
    '002420140002': 2,
    '002420140003': 3,
    '002420140004': 4,
    '002420140005': 5,
    '002420140006': 6,
    '002420140007': 7,
    '002420140008': 8,
    '002420140009': 9,
    '002420140010': 10,
    '002420140011': 11,
    '002420140012': 12,
    '002420140013': 13,
    '002420140014': 14,
    '002420140015': 15,
    '002420140016': 16,
    '002420140017': 17,
    '002420140020': 18
}

team_names = {
     1: 'Apolloon',
     2: 'Ekonomika',
     3: 'LBK',
     4: 'VTK',
     5: 'Wina',
     6: 'Enof',
     7: 'Psychokring',
     8: 'Letteren United',
     9: 'Atmosphere',
    10: '4 Speed',
    11: 'Industria',
    12: 'Oker',
    13: 'VRG',
    14: 'Politika',
    15: 'Run for Specials',
    16: 'Medica',
    17: 'Pedal',
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

