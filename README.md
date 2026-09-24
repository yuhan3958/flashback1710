# Flashback 1710

Flashback1710 records inbound client packets and replays them in an isolated
`ReplayWorld` on Minecraft 1.7.10.

## Commands

- `/flashback record` captures the current client world and starts recording
  packet changes.
- `/flashback stop` finishes the current recording.
- `/flashback play` restores the initial snapshot and starts `latest.fbr` paused.
- `/flashback pause`, `resume`, and `toggle` control the replay clock.
- `/flashback speed <value>` selects 0.25x, 0.5x, 1x, 2x, or 4x playback.
- `/flashback step` advances a paused replay by one 50 ms Minecraft tick.
- `/flashback camera free` detaches the view into a no-clip camera.
- `/flashback camera player` returns the view to the replay player.
- `/flashback camera speed <value>` sets free-camera movement speed.

Replay format version 3 stores a world snapshot before the timestamped packet
stream. The snapshot contains world and weather time, player transform, loaded
view chunks, block metadata, biomes, tile entity NBT, and loaded entity state.
Older replay versions are rejected with an unsupported-version error.
