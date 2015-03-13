#!/usr/bin/env ruby
#
require 'json'

if ARGV.size < 3
  puts "Usage: ./to_csv.rb input_file team_nb reader_id"
end

inFile = File.new(ARGV[0], 'r')

tagToTeam = {
  '002420140001' => 1,
  '002420140002' => 2,
  '002420140003' => 3,
  '002420140004' => 4,
  '002420140005' => 5,
  '002420140006' => 6,
  '002420140007' => 7,
  '002420140008' => 8,
  '002420140009' => 9,
  '002420140010' => 10,
  '002420140011' => 11,
  '002420140012' => 12,
  '002420140013' => 13,
  '002420140014' => 14,
  '002420140015' => 15,
  '002420140016' => 16,
  '002420140017' => 17,
  '002420140020' => 18
}

lastTime = nil

started = false

inFile.each do |line|
  line.chomp!
  json = JSON.parse(line)
  if json['type'] == 'Start'
    started = true
  elsif json['type'] == 'TagSeen'
    if started
      team = tagToTeam[json['tag']]
      if team != nil and team == ARGV[1].to_i(10) and json['readerId'] == ARGV[2].to_i(10)
        if lastTime != nil
          diff = json['time'] - lastTime
          if diff > 30
            puts "#{json['time']},#{diff}"
          end
        end
        lastTime = json['time']
      end
    end
  elsif json['type'] == 'AddTag'
    tagToTeam[json['tag']] = json['teamNb']
  elsif json['type'] == 'RemoveTag'
    tagToTeam.delete(json['tag'])
  elsif json['type']  == 'End'
    started = false
  end
end
