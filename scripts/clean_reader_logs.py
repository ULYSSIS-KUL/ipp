import json
import re

path = input("Path to reader log: ")
start_time = float(input("Timestamp of event start: "))
end_time = float(input("Timestamp of event end: "))
pattern = re.compile(input("Tag regex pattern: ")) # e.g. ^0024202200\d\d$

filtered_lines = []

with open(path, "r") as f:
    for line in f:
        read = json.loads(line)
        if read["updateTime"] < start_time or read["updateTime"] > end_time:
            continue
        
        if not pattern.match(read["tag"]):
            continue
        
        filtered_lines.append(line)

with open(path, "w") as f:
    f.writelines(filtered_lines)

