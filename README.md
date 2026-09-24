# Flashback 1710

Flashback1710 records inbound client packets and replays them in an isolated
`ReplayWorld` on Minecraft 1.7.10.

## Commands

- `/flashback record` captures the current client world and starts recording
  packet changes.
- `/flashback stop` finishes the current recording.
- `/flashback play` restores the initial snapshot and replays `latest.fbr`.

Replay format version 3 stores a world snapshot before the timestamped packet
stream. The snapshot contains world and weather time, player transform, loaded
view chunks, block metadata, biomes, tile entity NBT, and loaded entity state.
Older replay versions are rejected with an unsupported-version error.
