# Flashback 1710

Flashback 1710 is an experimental client-side replay system for Minecraft 1.7.10.

It records clientbound network traffic, selected local-player actions, and periodic world checkpoints, then reconstructs the session inside an isolated replay world. The project is currently aimed at proving that a modern replay workflow can be made practical on the GTNH 1.7.10 stack.

The long-term goal is not only to provide a standalone replay mod, but to reach a quality level where a proposal such as "should GTNH ship or officially support this?" can be discussed on technical merits.

## Current Status

Implemented:

- Clientbound packet recording.
- Selected serverbound player-action recording.
- Initial world snapshot.
- Configurable delta checkpoints.
- Configurable full checkpoint anchors.
- Timeline seeking.
- Reverse playback through in-memory tick history, segment reconstruction, and interpolated entity restoration.
- Playback speeds from -4x to 4x.
- Tick stepping while paused.
- Free camera.
- Player camera.
- ModularUI2 playback controls.
- Explicit Stop control.
- Replay boundary pause: reaching the beginning or end pauses playback instead of returning to the live world.
- Replay format v7.
- Backward loading support for replay format v6.

The project is still experimental. Compatibility with arbitrary modded packets, tile entities, world state, and long-running GTNH sessions has not yet been proven.

## Why This Exists

Replay systems are unusually difficult on Minecraft 1.7.10 because the client is not a deterministic simulation that can simply be rewound.

A packet can mutate:

- chunks,
- entities,
- tile entities,
- inventories,
- weather,
- world time,
- mod-specific client state,
- GUI state,
- custom FML channels.

Many of those operations have no inverse.

For that reason Flashback 1710 does not attempt to reverse packets. Instead, it reconstructs an earlier state from checkpoints and then replays packets forward to the requested timestamp.

That architecture is the central idea of the project.

## High-Level Workflow

### Recording

```text
Live server connection
        |
        | clientbound packets
        v
MixinNetworkManager
        |
        v
ReplayRecorder
        |
        +--------------------------+
        |                          |
        | packet stream            | ClientTick END
        v                          v
ReplayWriter              SnapshotCapture
                                   |
                                   v
                         checkpoint comparison
                                   |
                         +---------+---------+
                         |                   |
                         v                   v
                    delta checkpoint    full anchor
```

Recording begins by capturing a full initial snapshot.

Packets are then written with:

- replay timestamp,
- packet direction,
- packet class,
- optional FML channel,
- serialized payload.

Checkpoint capture is performed at client tick END rather than inside the inbound packet hook. This matters because the inbound packet hook executes before the packet has necessarily finished mutating the client world. Capturing at tick END gives the checkpoint a substantially clearer meaning: "client state after this tick's packet processing."

### Replay File Layout

Replay format v7 is a mixed record stream.

```text
magic
format version
initial full snapshot

packet
packet
packet
delta checkpoint
packet
packet
full checkpoint
packet
...
```

Record types are currently:

```text
0 = packet
1 = full checkpoint
2 = delta checkpoint
```

The default checkpoint configuration is:

```text
delta checkpoint: 30 seconds
full anchor:       300 seconds
```

Both intervals are configurable.

### Playback

```text
ReplayReader
    |
    +--> initial snapshot
    +--> packet list
    +--> checkpoint list
              |
              v
        ReplayPlayer
              |
              v
        ReplaySession
              |
       +------+------+
       |             |
       v             v
 ReplayWorld   recorded player
       |
       v
ReplayNetHandler
```

Playback does not run inside the original live world.

`ReplaySession` creates:

- an isolated `ReplayWorld`,
- a replay network handler,
- the recorded player entity,
- a spectator entity for the local client,
- a replay camera controller.

The live world and live player are retained so that Stop can restore the original session.

## Seeking

Seeking is segment based. Checkpoints are resolved into replay segments when the replay is loaded.

For a target such as 08:47:

```text
target = 08:47
      |
      v
find segment containing 08:47
      |
      v
restore resolved segment snapshot
      |
      v
replay segment bootstrap packet bundle
      |
      v
replay packet tail up to 08:47
```

With the default settings, a file may look like:

```text
05:00 FULL
05:30 delta
06:00 delta
06:30 delta
...
08:30 delta
```

Checkpoint chains are resolved once at load time into a binary-searchable `ReplaySegmentIndex`. Seeking to 08:47 finds the 08:30 segment in O(log n), restores its resolved snapshot, replays its bootstrap packet bundle for supported mod/FML side effects, and then replays only the remaining packet tail.

A segment stores the packet index associated with its start state, so playback can resume from the correct position in the packet stream.

## Reverse Playback

Reverse playback is state reconstruction, not packet inversion.

Forward playback:

```text
clock advances
    ->
packets are processed
    ->
ReplayWorld simulates forward
    ->
a reverse frame is captured at replay-tick boundaries
```

Reverse playback:

```text
clock moves backward
    ->
find the surrounding reverse frames
    ->
restore immutable player/entity/world state
    ->
interpolate transforms between the two replay ticks
```

The in-memory reverse history currently covers the most recent 30 seconds. When playback crosses the beginning of that history window, Flashback reconstructs the previous window from the nearest replay segment and then continues from newly generated reverse frames.

Reverse frames do not retain live entity object references. Entity state is captured as immutable NBT plus replay-relevant transforms and identity data, so entities can be recreated after a replay-world reset.

Entity position, rotation, motion, server position, head rotation, and recorded-player transforms are interpolated between replay ticks. The replay clock remains at the requested target timestamp instead of snapping to the previous stored frame.

Flashback still does not attempt to execute network packets backwards. Packet inversion is not generally well-defined for entity destruction, block replacement, inventory mutation, chunk loading, tile entities, or arbitrary mod packets.

Supported playback speeds are:

```text
-4x
-2x
-1x
-0.5x
-0.25x
0.25x
0.5x
1x
2x
4x
```

At timestamp 0, reverse playback pauses.

At the replay duration, forward playback pauses.

The replay session remains open at both boundaries until the user explicitly presses Stop or runs the stop command.

## Snapshot Model

A full snapshot currently includes:

- dimension ID,
- world seed,
- world time,
- total world time,
- rain and thunder state,
- replay-player transform,
- replay-player motion,
- replay-player inventory,
- loaded view chunks,
- block IDs,
- block metadata,
- biome arrays,
- tile entity NBT,
- loaded entity state.

Delta checkpoints compare two snapshot states and retain changed state plus removals.

The current delta model is designed for implementation simplicity and correctness before aggressive storage optimization. Further optimization can move toward finer-grained dirty-state tracking after behavior is validated.

## Configuration

Flashback 1710 uses the Forge configuration file.

Current recording options:

```text
checkpointIntervalSeconds = 30
checkpointAnchorIntervalSeconds = 300
```

`checkpointIntervalSeconds` controls delta checkpoints.

`checkpointAnchorIntervalSeconds` controls full checkpoint anchors.

A value of `0` disables that checkpoint type.

## Commands

```text
/flashback record
/flashback stop
/flashback play
/flashback pause
/flashback resume
/flashback toggle
/flashback speed <value>
/flashback step
/flashback camera free
/flashback camera player
/flashback camera speed <value>
/flashback ui
```

Current development recording is written to:

```text
.minecraft/replays/latest.fbr
```

### Playback Speed

Example:

```text
/flashback speed -1
/flashback speed 0.5
/flashback speed 4
```

Negative values play backward through repeated checkpoint-based reconstruction.

### Camera

```text
/flashback camera free
```

enables the no-clip replay camera.

```text
/flashback camera player
```

returns the view to the recorded player.

Free-camera movement remains usable while the replay UI is open. Hold the right mouse button and drag to look around.

## Replay UI

Playback opens a ModularUI2 editing workspace modeled after desktop video editors.

The screen follows the 2:1 reference split from the UI mockup: roughly two thirds of the height is the workspace, one third is the timeline; the upper workspace is split roughly two thirds VIEW to one third SETTINGS.

The screen is divided into three regions:

```text
+---------------------------+-------------+
|                           |             |
|           VIEW            |  SETTINGS   |
|                           |             |
+---------------------------+-------------+
|                                         |
|                TIMELINE                 |
|                                         |
+-----------------------------------------+
```

The upper-left **VIEW** region intentionally leaves the replay world visible instead of painting an opaque panel over it. The upper-right **SETTINGS** region currently exposes replay-camera controls and is reserved for future camera/keyframe settings. The bottom **TIMELINE** region spans the full screen width and owns transport controls plus the editor-style timeline viewport. The three regions are edge-aligned without decorative outer gutters so the separators match the reference wireframe directly.

The timeline is no longer scaled permanently to the full replay duration. It maintains a horizontal time viewport:

- mouse wheel: zoom around the cursor position (one-hand scale control),
- Shift + mouse wheel: horizontal pan,
- middle-mouse drag: horizontal pan,
- left click / drag: seek,
- major ruler marks: real replay-time labels,
- minor ruler marks: adaptive subdivisions,
- playback: automatically keeps the playhead inside the visible viewport.

Ruler spacing uses human-friendly time steps rather than fixed Minecraft-tick spacing, so zooming changes the visible scale without changing replay time.

Transport controls include current time, total duration, playback speed, skip backward/forward, speed changes, play/pause, and Stop.

Stop is intentionally distinct from Pause.

Pause freezes replay time while keeping the replay world active.

Stop destroys the replay session and restores the original live world and player.

## Main Components

### Recording

`ReplayRecorder`

Coordinates packet recording and checkpoint scheduling.

`SnapshotCapture`

Captures the current client-visible world state.

`SnapshotDelta`

Builds and applies checkpoint deltas.

`ReplayWriter`

Serializes the replay stream.

### Loading

`ReplayReader`

Loads the initial snapshot, packets, and checkpoints.

`ReplayCheckpointResolver`

Resolves the best checkpoint state for a requested timestamp.

### Playback

`ReplayPlayer`

Owns replay time, packet position, seeking, playback direction, and playback state.

`ReplayClock`

Tracks replay time and signed playback speed.

`ReplaySession`

Owns replay-world lifetime and restores the live Minecraft session on Stop.

`ReplayWorld`

Runs replay-time-aware world simulation.

`ReplayNetHandler`

Applies recorded clientbound packets to the replay world.

### Camera and UI

`ReplayCameraController`

Controls free camera and player camera modes.

`ReplayControlBar`

Provides primary playback controls.

`ReplayTimelineWidget`

Displays replay progress and performs timeline seeking.

## Design Principles

### Do not mutate the live world during playback

Replay state belongs in an isolated world.

### Do not invent reverse packets

Reverse playback should reconstruct known state rather than guess an inverse network operation.

### Prefer explicit replay time

Replay state should depend on the replay clock rather than wall-clock time wherever possible.

### Preserve modded packet data

Unknown packet behavior should be retained rather than translated into a lossy custom event format unless there is a clear reason to do so.

### Keep the recorded player separate from the spectator

The replay subject and the local camera/controller are different entities.

### Optimize after correctness

Checkpoint compression and dirty-state tracking matter, but a smaller incorrect replay is not useful.

## Known Limitations

The largest unresolved questions are compatibility and determinism.

Current limitations include:

- Replay files are still managed as a single development file, `latest.fbr`.
- There is no replay browser or metadata index.
- Checkpoint capture currently snapshots client-visible state and can become expensive on large view distances.
- Delta creation currently compares snapshots rather than consuming a complete dirty-state event stream.
- Reverse playback is substantially cheaper inside the 30-second in-memory reverse-history window, but rebuilding an older history window still requires segment reconstruction.
- Arbitrary modded custom packets have not been tested across the GTNH mod set.
- Client-side state that is not represented by packets, snapshots, or reverse-frame state may diverge.
- Tick-level reverse restoration for blocks, tile entities, chunk membership, particles, sounds, and arbitrary mod-owned client state is not yet exact.
- Perfect reverse playback requires a world-mutation journal or equivalent reversible state stream for every replay-relevant mutation, not only entity transforms.
- Dimension transitions need dedicated stress testing.
- Long-session memory, file-size, and seek-latency characteristics have not yet been benchmarked.
- Crash recovery and partially written replay handling are not yet production-grade.
- Replay format compatibility policy is not yet formally specified.
- Automated replay correctness tests are not yet sufficient for a GTNH-scale integration proposal.

## Development Direction

The project should not be proposed for GTNH integration merely because basic playback works.

Before such a proposal, it should demonstrate:

1. replay correctness across representative GTNH gameplay,
2. acceptable recording overhead,
3. bounded replay file growth,
4. predictable seek latency,
5. stable handling of modded packets,
6. recovery from malformed or interrupted replay files,
7. a maintainable compatibility strategy,
8. automated regression coverage,
9. usable replay file management,
10. clear ownership and maintenance expectations.

The detailed plan is in:

[GTNH Integration Plan](docs/GTNH_INTEGRATION_PLAN.md)

## Proposed End State

A proposal-ready version should be able to make a concrete claim:

> A normal GTNH client can record gameplay for an extended session with acceptable overhead, reopen it later, seek and reverse through it reliably, and reproduce the client-visible state closely enough for debugging, review, cinematics, and support workflows.

Only after that claim is backed by tests and measurements does it make sense to ask whether the feature belongs in GTNH itself, an officially recommended companion mod, or a separate maintained project.

## Build

The project uses the GTNH convention Gradle plugin and targets Minecraft 1.7.10 / Forge 10.13.4.1614.

ModularUI2 is currently a required dependency.

```text
com.github.GTNewHorizons:ModularUI2:2.3.89-1.7.10:dev
```

Kotlin targets JVM 8 bytecode.

## Project Stage

Flashback 1710 should currently be treated as a prototype moving toward an engineering validation phase.

The next milestone is not "more features."

The next milestone is proving that the existing architecture remains correct, fast, and maintainable under real GTNH workloads.


## Perfect Replay Target

The architectural target is stronger than "visually plausible reverse playback."

Flashback 1710 should eventually satisfy the following invariant:

```text
state_at(t)
==
state produced by forward playback to t
==
state produced by reverse playback back to t
==
state produced by seek directly to t
```

for every replay-relevant client-visible state component.

That includes:

- player state,
- all entities,
- entity lifecycle,
- blocks,
- chunk load state,
- tile entities,
- inventories,
- world metadata,
- weather and time,
- mod-owned state,
- transient visual state where practical.

The current reverse entity path now reconstructs immutable entity state and interpolates transforms correctly. Full perfect reverse playback still requires reversible world mutation tracking for blocks, tile entities, chunks, and mod-specific state.
