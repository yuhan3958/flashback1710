# Implementation Roadmap

This document turns the goals in `GTNH_INTEGRATION_PLAN.md` into an implementation order.

The project is now past the stage where the highest-value work is adding more UI polish. The next phase should harden the replay engine into something measurable, recoverable, and maintainable under real GTNH workloads.

## 1. Establish a Correctness Baseline

Add automated tests before expanding the feature surface.

Priority targets:

- `ReplayClock`
- `SnapshotDelta`
- `ReplayCheckpointResolver`
- replay boundary handling
- repeated seek behavior
- live-session restoration

Create deterministic replay fixtures containing known changes to:

- blocks,
- entities,
- entity spawn and removal,
- player inventory,
- world time,
- weather,
- movement.

For each sampled timestamp `t`, compare:

```text
forward playback from start -> t
reverse playback from a later timestamp -> t
direct seek -> t
```

All three paths should converge on equivalent replay-visible state.

The acceptance criterion is state equivalence, not merely visually plausible playback.

## 2. Harden the Replay File Format

**Status: implemented for format v8; continue extending tests as the format evolves.**

Treat replay files as durable user data rather than development artifacts.

Extend the replay metadata header with:

- creation timestamp,
- Minecraft version,
- Flashback version,
- GTNH version when available,
- mod-list or compatibility fingerprint.

Add defensive record framing:

- bounded record lengths,
- checksums where useful,
- explicit clean-close marker,
- corruption detection,
- truncated-file recovery,
- rejection of unreasonable allocations or length fields.

`ReplayReader` should recover up to the last valid record where possible instead of failing the entire file.

The current format is documented in:

`docs/FORMAT.md`

The format version must remain independent from the mod version. Readers may support a bounded set of older formats, and incompatible changes should increment the format version explicitly.

## 3. Expand Reverse Playback into a Mutation Journal

Entity reverse frames are useful, but they are not sufficient for exact world reversal.

Introduce a tick-ordered mutation journal with replay-visible before/after state.

A possible structure is:

```text
ReplayMutationJournal
 ├─ BlockMutation
 ├─ TileEntityMutation
 ├─ EntityMutation
 ├─ InventoryMutation
 ├─ ChunkLifecycleMutation
 ├─ WorldMetadataMutation
 └─ CompatMutation
```

Each mutation type should contain enough information to apply both forward and backward.

The intended split is:

- mutation journal: continuous interactive reverse playback,
- checkpoint + packet-tail reconstruction: random access, recovery, and fallback outside retained journal history.

The reverse engine should eventually cover:

- entity creation and removal,
- entity state and transforms,
- player inventory and held items,
- block ID and metadata,
- tile entity creation, removal, and NBT,
- chunk load and unload state,
- world time and weather,
- mod-owned state where adapters exist.

## 4. Strengthen Core/UI Boundaries

Keep replay reconstruction independent from the UI layer.

Target package boundaries:

```text
recording/
io/
snapshot/
replay/
camera/
ui/
compat/
```

UI code should only call stable replay-facing operations such as:

- `seek()`
- `setSpeed()`
- `togglePause()`
- camera mode controls.

Do not put mod-specific compatibility logic directly into `ReplayPlayer`.

Only introduce compatibility abstractions after real incompatibilities justify them. Likely interfaces include:

```text
ReplayStateProvider
ReplayPacketAdapter
ReplayLifecycleListener
```

## 5. Build the GTNH Compatibility Matrix

Test representative GTNH systems rather than assuming packet compatibility.

Initial targets should include:

- GregTech machines and covers,
- ModularUI / ModularUI2,
- Applied Energistics 2,
- Thaumcraft,
- Forestry,
- Railcraft,
- dimension-heavy systems,
- major GTNH-specific integrations.

For each subsystem, test:

```text
record
playback
seek
reverse playback
```

Record results in:

`docs/COMPATIBILITY.md`

Use explicit statuses:

```text
PASS
PARTIAL
BROKEN
NOT TESTED
```

Failures should remain visible. The matrix is also the evidence needed to decide whether a compatibility API is actually required.

## 6. Measure and Optimize Checkpoint Cost

Instrument the existing checkpoint path before optimizing it.

Measure:

- `SnapshotCapture` time,
- delta comparison time,
- full-anchor serialization time,
- delta serialization time,
- restore time,
- packet-tail replay time.

If checkpoint work creates visible stalls, optimize in this order:

1. dirty chunk tracking,
2. dirty tile entity tracking,
3. dirty entity tracking,
4. incremental serialization,
5. background compression where thread safety permits,
6. bounded checkpoint work per tick if necessary.

World mutation should remain on the Minecraft client thread unless thread safety has been proven.

## 7. Replace `latest.fbr` with Real Replay Management

Move away from a single hard-coded development replay file.

Use unique names such as:

```text
2026-09-26_13-42-18_servername.fbr
```

Introduce structured replay metadata that can be read without fully loading the replay.

Build a replay library with:

- replay list,
- recording date,
- duration,
- server name where available,
- open,
- rename,
- delete,
- clear incompatibility and corruption errors.

Optional later metadata:

- dimension summary,
- thumbnail,
- tags,
- notes.

At this stage, `/flashback play` should stop being the primary user workflow and become a path into normal replay management.

## 8. Keep the Current Editor UI and Fill Functional Gaps

The current three-region layout is sufficient:

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

Keep the current editor-style timeline model:

- horizontal viewport,
- cursor-anchored wheel zoom,
- Shift + wheel horizontal pan,
- middle-mouse drag pan,
- labeled time ruler,
- seek by click and drag.

Do not spend the next phase on cosmetic restructuring.

Functional additions should come later:

- frame/tick stepping,
- previous/next checkpoint,
- configurable seek step,
- camera keyframes,
- optional timeline markers.

Keep timeline drawing based on one consistent `time -> viewport x` transformation so future markers reuse the same coordinate model.

## 9. Make Replay Failures Recoverable

A malformed or incompatible replay must not leave the live session broken.

Introduce explicit recovery behavior.

A possible policy model:

```text
SKIP_NON_CRITICAL
ABORT_REPLAY_AND_RESTORE_LIVE
MARK_INCOMPATIBLE
```

Error information should include, where applicable:

- packet class,
- FML/custom channel,
- replay timestamp,
- replay file offset,
- compatibility adapter involved.

Whenever possible, fatal replay errors should unwind through the normal session-restoration path and return the client to the original live world and player.

## 10. Introduce Structured Logging and Metrics

Replace raw `println` and `System.err` paths with the mod logger.

Useful logging categories:

- recording,
- replay,
- checkpoint,
- format,
- compatibility,
- recovery.

Add optional development metrics for:

- packet count,
- packets per second,
- checkpoint count,
- snapshot capture duration,
- delta size,
- anchor size,
- current packet index,
- seek duration.

These metrics should feed directly into compatibility investigation and benchmark documentation.

## 11. Run Long-Session Benchmarks

Test at least:

- 10 minutes,
- 30 minutes,
- 2 hours,
- 6 hours.

Run both:

- quiet areas,
- active late-game machine areas.

Collect:

- replay file size,
- bytes per minute,
- maximum checkpoint size,
- heap usage,
- recording CPU overhead,
- frame-time impact,
- replay load time,
- random seek latency,
- reverse playback cost.

Publish results in:

`docs/BENCHMARKS.md`

Include:

- hardware,
- JVM,
- Java version,
- GTNH version,
- render distance,
- test conditions.

Use the measured values to define the real performance budget instead of hiding failures behind aspirational targets.

## 12. Prepare the GTNH Proposal Package

Only after correctness, compatibility, recovery, and performance are measured should the project be presented as an integration candidate.

The proposal package should include:

- architecture summary,
- `docs/FORMAT.md`,
- `docs/COMPATIBILITY.md`,
- `docs/BENCHMARKS.md`,
- known limitations,
- privacy notes,
- maintenance plan,
- short demonstration video,
- representative replay files if useful.

Present multiple integration options:

1. remain an independent mod,
2. become an officially recommended optional mod,
3. move into a GTNH-owned repository,
4. extract a reusable replay library,
5. serve primarily as a developer/debugging tool.

The initial maintainer conversation should ask for engineering feedback before asking for inclusion.

# Recommended Implementation Order

The practical near-term order is:

```text
tests
  ->
format hardening
  ->
metadata + unique filenames
  ->
failure recovery
  ->
GTNH compatibility pass
  ->
performance instrumentation
  ->
long-session benchmarks
  ->
replay library
  ->
proposal package
```

The UI should remain comparatively stable during this phase.

The main engineering goal is now:

> Make the existing replay architecture provably correct, recoverable, compatible, and measurable before expanding the control surface.
