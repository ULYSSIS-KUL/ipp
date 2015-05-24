#!/usr/bin/env python3

import redis, argparse

parser = argparse.ArgumentParser()
parser.add_argument('-H', '--host', type=str, help='Redis host', default='localhost')
parser.add_argument('-p', '--port', type=int, help='Redis port', default=6379)
parser.add_argument('-d', '--db', type=int, help='The Redis database to select', default=0)
parser.add_argument('logfile', type=str, help='The log file to import')
parser.add_argument('list', type=str, help='The list name to import to')
args = parser.parse_args()

print('Connecting to Redis at host {}, port {}, db {}'.format(args.host, args.port, args.db))
r = redis.Redis(host=args.host, port=args.port, db=args.db)
p = r.pipeline()

with open(args.logfile, 'r') as f:
    for line in f:
        p.lpush(args.list, line.rstrip())

p.execute()

