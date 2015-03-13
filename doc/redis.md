# Redis lists, pubsub channels and vars

## READER

### LISTS

#### updates

Contains all updates of tags. Most recent updates are pushed on the right.

### CHANNELS

#### status

The reader publishes status updates to this channel, to be monitored by the processors and/or GUI.

#### update:N

The reader publishes the number of the latest update on this channel. The processors monitor this channel for updates. N is the *reader*'s id. This is so that you can run multiple readers on one system with a single Redis instance, because pubsub is cross-database for some reason.

## PROCESSOR

### VARS

#### start\_time

Contains the start time in milliseconds since UNIX epoch, in UTC.

### CHANNELS

#### control

Used to send control messages to the processor, to start it, for example.
