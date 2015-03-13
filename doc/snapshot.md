# Snapshot

Is this basically the Memento pattern + Command pattern?

One snapshot contains:

* The update that effected the change in state
* Teams state:
    * Tags for teams
    * Rounds

# Updates

* TagUpdate
* AddTag
* RemoveTag
* Correction?

# Effiency

For effiency, can we keep everything that doesn't change?

So, snapshot is:

* Ptr to team/tag state
* Ptr to team/score

An update will often not change the team/tag state.

## Pruning

We can prune the oldest snapshots, to reclaim memory. We can rely
on the GC to know when an old team/tag state can be removed.
