# GTNH Integration Development Plan

## Purpose

This document defines a path from the current Flashback 1710 prototype to a state where it is reasonable to approach the GTNH team with a concrete integration or official-support proposal.

The goal is not to convince the team with feature count.

The goal is to present evidence that the replay system is:

- useful,
- technically sound,
- compatible with the GTNH environment,
- performant enough for normal play,
- maintainable,
- testable,
- recoverable when something goes wrong.

The proposal should eventually be able to answer:

> What problem does this solve for GTNH, what does it cost, how reliable is it, and who will maintain it?

## Candidate GTNH Use Cases

A GTNH-facing proposal becomes much stronger if it is tied to real workflows rather than only cinematic replay.

Candidate use cases:

- reproducing client-visible bugs,
- documenting desyncs,
- reviewing multiplayer incidents,
- support diagnostics,
- cinematic recording,
- tutorial production,
- machine or automation demonstrations,
- progression documentation,
- server-event recording,
- testing visual regressions,
- inspecting complicated mod interactions after the fact.

The project should collect evidence for which of these are actually valuable to GTNH users and maintainers.

## Current Architecture

The current architecture is:

```text
recording
    |
    +-- initial full snapshot
    |
    +-- timestamped packet stream
    |
    +-- periodic delta checkpoints
    |
    +-- periodic full anchors
             |
             v
          .fbr file
             |
             v
        ReplayReader
             |
             v
        ReplayPlayer
             |
      +------+------+
      |             |
      v             v
checkpoint      packet tail
resolution      replay
      |             |
      +------+------+
             |
             v
        ReplaySession
             |
             v
         ReplayWorld
```

Reverse playback uses two layers:

- an in-memory tick-state history for interactive reverse playback,
- replay-segment reconstruction when the requested time falls outside the cached history window.

Packets themselves are never inverted. Entity state is reconstructed from immutable reverse frames and interpolated between replay ticks. Segment reconstruction remains the fallback for older history.

The long-term correctness target is not merely smooth reverse movement. Forward playback, reverse playback, and direct seek must converge on the same replay-visible state at the same timestamp.

# Proposal Readiness Gates

The project should not be presented as a GTNH integration candidate until the following gates are met.

## Gate 1: Deterministic Core Behavior

Required:

- forward playback does not unexpectedly diverge during normal vanilla interactions,
- seeking to the same timestamp repeatedly produces equivalent visible state,
- reverse playback and forward playback can cross the same region repeatedly without entity loss, frozen transforms, or state drift,
- reaching either replay boundary does not corrupt the session,
- Stop reliably restores the original live world,
- free camera does not mutate recorded-player state,
- replay time remains stable across pause/resume and speed changes.

Acceptance target:

Create a deterministic test scenario with known block, entity, inventory, movement, weather, and time changes. Record it, then verify selected timestamps against expected state.

For every sampled timestamp, compare three paths:

```text
forward from start -> t
reverse from later time -> t
direct seek -> t
```

All three paths should produce equivalent replay-visible state.

## Gate 2: GTNH Packet Compatibility

Build a packet compatibility matrix.

For each major mod or subsystem:

- identify custom channels,
- identify important clientbound packet families,
- record whether playback succeeds,
- record whether state survives seeking,
- record whether state survives reverse playback,
- identify packets that depend on external static/global state.

Initial target areas should include high-visibility systems such as:

- GregTech machines and covers,
- NEI-related state where relevant,
- ModularUI / ModularUI2 interactions,
- Applied Energistics 2,
- Thaumcraft,
- Forestry,
- Railcraft,
- Galacticraft or dimension-heavy systems used by the pack,
- major GTNH custom integrations.

The exact list should be based on current GTNH usage rather than historical assumptions.

Deliverable:

`docs/COMPATIBILITY.md`

with explicit statuses such as:

```text
PASS
PARTIAL
BROKEN
NOT TESTED
```

Do not hide failures.

## Gate 3: Long-Session Reliability

Test recording durations of at least:

- 10 minutes,
- 30 minutes,
- 2 hours,
- 6 hours.

Collect:

- replay file size,
- average bytes per minute,
- maximum observed checkpoint size,
- heap usage,
- recording CPU overhead,
- recording frame-time impact,
- replay load time,
- random seek latency,
- reverse playback cost.

Test both quiet bases and highly active machine areas.

Deliverable:

`docs/BENCHMARKS.md`

with hardware, JVM, Java version, render distance, modpack version, and test conditions.

## Gate 4: Checkpoint Performance

The current checkpoint system is intentionally correctness-first.

Measure:

- `SnapshotCapture` time,
- delta comparison time,
- full anchor write time,
- delta write time,
- restore time,
- packet-tail replay time.

Target behavior:

Checkpoint work should not create noticeable periodic client freezes under normal settings.

If it does, move toward:

- dirty chunk tracking,
- dirty tile entity tracking,
- dirty entity tracking,
- incremental serialization,
- background compression where thread safety permits,
- bounded work per tick.

Do not move world mutation itself off the Minecraft client thread without proving thread safety.

## Gate 5: Replay File Robustness

Format v8 now provides metadata, framed records, CRC32 validation, bounded record lengths, a clean-close marker, and prefix recovery for trailing truncation/corruption. Remaining work is broader integration and adversarial testing.

A production-quality format needs explicit failure behavior.

Add:

- replay metadata header,
- creation timestamp,
- Minecraft version,
- mod version,
- GTNH version when available,
- mod list or compatibility fingerprint,
- clean-close marker,
- record checksums or framing,
- corruption detection,
- truncated-file handling,
- maximum record sizes,
- defensive validation.

A partially written replay should fail gracefully or recover up to the last valid record.

The reader must never trust arbitrary length fields without bounds.

## Gate 6: Format Evolution Policy

Define a stable policy.

Recommended:

- replay format version remains independent from mod version,
- readers may support a bounded number of previous versions,
- incompatible changes increment the format version,
- migration is optional unless there is a real user need,
- format documentation lives in `docs/FORMAT.md`.

The file format should be documented well enough that another maintainer can implement a reader without reverse engineering the source.

## Gate 7: Replay Library UX

Replace `latest.fbr` as the primary user workflow.

Required:

- unique replay filenames,
- replay metadata,
- replay list,
- duration display,
- recording date,
- rename,
- delete,
- open,
- clear failure messages.

Nice to have:

- server name,
- dimension summary,
- thumbnail,
- tags,
- notes.

A GTNH proposal should not require users to manually manage one hard-coded development file.

## Gate 8: Playback UX

The current playback UI uses the reference three-region editor workspace, with an approximately 2/3-height workspace above a 1/3-height full-width timeline. The upper workspace is split approximately 2/3 VIEW and 1/3 SETTINGS:

- replay view in the upper-left,
- settings/camera panel in the upper-right,
- full-width timeline editor across the bottom.

The timeline is a horizontal viewport rather than a fixed full-duration progress bar. It supports cursor-anchored wheel zoom with the mouse alone, Shift+wheel horizontal scrolling, middle-mouse drag panning, adaptive ruler spacing, and real replay-time labels on major marks. The bottom timeline spans the full window width to preserve the editing-surface feel of desktop NLEs.

Minimum proposal-ready controls:

- play/pause,
- Stop,
- timeline seek,
- horizontal timeline pan,
- timeline zoom,
- labeled time ruler,
- reverse playback,
- speed control,
- frame/tick stepping,
- player camera,
- free camera,
- current time / total duration.

Additional useful controls:

- jump backward/forward,
- previous/next checkpoint,
- configurable seek step,
- hotkeys,
- camera path keyframes,
- hide replay UI for capture,
- resizable or user-configurable workspace splits.

The UI should clearly distinguish:

- Pause: keep replay world open,
- Stop: return to the live session.

## Gate 9: Automated Tests

Add tests for components that can be isolated from Minecraft runtime.

Priority unit tests:

- replay record framing,
- v6 reader compatibility,
- v7 reader/writer round trip,
- checkpoint resolver,
- snapshot delta create/apply,
- boundary timestamps,
- negative replay clock behavior,
- malformed length rejection,
- truncated stream behavior.

Integration tests should verify replay state where possible.

A useful invariant:

```text
apply(base, create(base, target)) == target
```

for snapshot state represented by the delta model.

Another useful invariant:

```text
resolve(targetTime)
```

must never return a checkpoint state whose packet index represents events after `targetTime`.

## Gate 10: Crash and Session Safety

Test:

- disconnect while recording,
- server kick,
- world unload,
- dimension transition,
- client crash,
- replay Stop during seek,
- closing the UI,
- opening another GUI,
- pausing at the first frame,
- pausing at the last frame,
- loading a malformed replay,
- loading a replay from another mod configuration.

The live session must not be left in a broken state after replay failure.

## Gate 11: Mod Interaction Boundaries

Some state will not be reconstructible from packets and world snapshots alone.

Identify those cases explicitly.

Possible strategies:

- compatibility hooks,
- replay-aware adapter API,
- mod-specific snapshot providers,
- custom packet handlers,
- exclusion of unsupported systems.

Avoid putting every special case directly into `ReplayPlayer`.

If compatibility hooks become necessary, introduce a small API such as:

```text
ReplayStateProvider
ReplayPacketAdapter
ReplayLifecycleListener
```

only after at least two real compatibility cases prove the abstraction is needed.

## Gate 12: Performance Budget

Before approaching GTNH maintainers, define a performance budget.

Example targets to validate, not assumptions:

- negligible overhead when not recording,
- low single-digit percentage CPU overhead during ordinary recording,
- no recurring multi-frame freeze at default checkpoint settings,
- bounded memory growth,
- predictable replay load time,
- sub-second ordinary seeks on representative replay files.

If the actual measurements cannot meet those values, publish the measured values and adjust the proposal rather than hiding the cost.

# Development Milestones

## Milestone A: Correctness Baseline

Focus:

- automated delta tests,
- replay-clock tests,
- boundary behavior,
- repeated seek tests,
- session restoration tests.

Exit criteria:

The current architecture is reproducible in controlled scenarios.

## Milestone B: Replay Format Hardening

Focus:

- metadata,
- framing,
- corruption detection,
- truncated replay recovery,
- format documentation.

Exit criteria:

Replay files can be treated as durable user data rather than temporary development output.

## Milestone C: GTNH Compatibility Pass

Focus:

- representative GTNH systems,
- modded packet matrix,
- dimension transitions,
- complex tile entities.

Exit criteria:

Major incompatibilities are known and documented.

## Milestone D: Performance Pass

Focus:

- profiling,
- checkpoint cost,
- file size,
- random seek benchmarks,
- long sessions.

Exit criteria:

Default settings have measured and acceptable overhead.

## Milestone E: User Workflow

Focus:

- replay library,
- metadata UI,
- error handling,
- keybinds,
- replay management.

Exit criteria:

A normal user can record and replay without development commands or file manipulation.

## Milestone F: Maintainer Review

Prepare:

- architecture summary,
- compatibility matrix,
- benchmarks,
- known limitations,
- maintenance plan,
- screenshots/video demonstration,
- example replay files if useful,
- concrete integration options.

At this point, ask for technical feedback before asking for inclusion.

# GTNH Proposal Strategy

Do not start with:

> Please add my replay mod to GTNH.

Start with:

> I have been prototyping a 1.7.10 replay system against the GTNH stack. It records packet traffic plus checkpointed client state, supports seeking and reverse playback, and runs playback in an isolated world. I have compatibility and performance results for representative GTNH workloads. Would this be useful enough to justify an official integration, recommended companion mod, or further collaboration?

That framing invites engineering review rather than forcing a packaging decision.

## Include in the Proposal

A strong proposal should contain:

### Problem

What GTNH workflow becomes better?

### Demonstration

Show:

- recording,
- seek,
- reverse playback,
- free camera,
- Stop returning safely to live play.

### Architecture

Explain:

```text
packets + snapshots + delta checkpoints + full anchors
```

and why reverse playback reconstructs state rather than reversing packets.

### Measurements

Include real numbers for:

- recording overhead,
- file size,
- checkpoint cost,
- seek latency,
- long-session behavior.

### Compatibility

List tested GTNH systems and known failures.

### Risk

Be explicit about:

- custom mod state,
- file-format maintenance,
- performance,
- client-only assumptions,
- replay correctness limits.

### Integration Options

Offer multiple possibilities rather than demanding one:

1. keep Flashback 1710 independent,
2. make it an officially recommended optional mod,
3. integrate it into a GTNH-owned repository,
4. extract a reusable replay library,
5. use it primarily as a debugging/developer tool.

Let maintainers choose which ownership model fits the project.

# Architectural Work Before Proposal

## Separate Replay Core from UI

Aim for boundaries similar to:

```text
recording/
io/
snapshot/
replay/
camera/
ui/
compat/
```

The replay engine should not require UI classes to perform core state reconstruction.

## Introduce Structured Logging

Replace raw `println` and `System.err` paths with the mod logger.

Useful categories:

- recording,
- replay,
- checkpoint,
- compatibility,
- format,
- recovery.

## Add Metrics

Optional debug metrics should expose:

- packet count,
- packets/sec,
- checkpoint count,
- snapshot capture duration,
- delta size,
- anchor size,
- current packet index,
- seek duration.

These measurements are valuable both for development and for a GTNH proposal.

## Make Replay Failure Recoverable

A bad packet should not automatically destroy the entire client session.

Define policies such as:

- skip non-critical packet and record an error,
- abort replay but restore live world,
- mark replay as incompatible,
- provide the packet class and timestamp in the error.

# Questions the GTNH Team Is Likely to Ask

Be prepared to answer:

- What is the recording overhead in a real late-game base?
- How large is a two-hour replay?
- Does it work with custom GT packets?
- What happens across dimensions?
- What happens if the mod list changes?
- Can a malformed replay crash the client?
- Does recording expose private chat or server packet contents?
- Can replay files contain sensitive data?
- Who maintains compatibility when GTNH mods update?
- Does it work on servers without server-side installation?
- Can it be disabled completely?
- Does it affect normal gameplay when not recording?
- Why should this be integrated instead of remaining optional?
- What is the maintenance cost of the replay file format?

A proposal-ready project should have measured or explicitly scoped answers.

# Privacy and Security Considerations

Replay files may contain serialized network traffic and world state.

Before wider distribution:

- document what is recorded,
- determine whether chat is captured,
- determine whether server-provided identifiers or private data are captured,
- avoid recording authentication secrets,
- validate all replay file input,
- treat replay files as untrusted input when shared between users.

This is especially important if replay files become useful for bug reports.

# Definition of Proposal Ready

Flashback 1710 is proposal ready when all of the following are true:

- core replay behavior has automated regression coverage,
- representative GTNH gameplay has been tested,
- major compatibility failures are documented,
- recording overhead has been benchmarked,
- long replay files have been tested,
- seeking performance is measured,
- replay files recover safely from common corruption cases,
- users can manage more than one replay,
- failures restore the live client safely,
- file format behavior is documented,
- privacy implications are documented,
- there is a clear maintenance plan.

At that point the project can be presented as an engineering result rather than an experiment.

# Recommended Immediate Work Order

1. Extend deterministic replay-state tests beyond isolated core units.
2. Replace `latest.fbr` with metadata-backed unique filenames.
3. Add replay library management and user-visible recovery diagnostics.
4. Expand malformed-file and crash/session recovery integration tests.
5. Build a GTNH compatibility test world.
6. Profile checkpoint capture in a late-game base.
7. Record 30-minute and 2-hour benchmark sessions.
8. Build the compatibility matrix.
9. Add replay library UI.
10. Produce a short demonstration video.
11. Write the GTNH proposal using measured results.

The most important change in mindset is:

> From this point forward, development should prioritize evidence, compatibility, and reliability over adding more controls.


# Perfect Reverse Playback Program

Perfect reverse playback requires a reversible representation of all replay-relevant mutations.

The current entity reverse path is a first step: reverse frames store immutable NBT and transform state rather than live object references, recreate missing entities when necessary, remove entities that did not exist at the target frame, and interpolate transforms between replay ticks.

That is not sufficient for full world correctness.

## Required reversible state domains

The reverse engine must eventually cover:

- entity creation and removal,
- entity NBT and transforms,
- player state and inventory,
- block ID and metadata mutations,
- tile entity creation, removal, and NBT mutation,
- chunk load and unload state,
- world time and weather,
- scoreboard/team state where visible,
- mod-owned client state,
- transient render state where practical.

## Mutation journal

The preferred architecture is a tick-ordered mutation journal.

```text
before tick N
    |
    +-- block before/after
    +-- tile entity before/after
    +-- entity before/after
    +-- chunk lifecycle
    +-- world metadata before/after
    +-- mod adapter state
    |
after tick N
```

Each mutation entry should contain enough information to apply both directions without rebuilding the entire replay world.

Checkpoint/segment reconstruction remains necessary for:

- random access,
- recovery,
- crossing beyond retained reverse history,
- validation against corruption.

The mutation journal is for continuous exact reverse playback.

## Acceptance criteria

A proposal-ready reverse engine should demonstrate:

1. no frozen entities during negative playback,
2. entity spawn/despawn reverses exactly,
3. repeated forward/reverse traversal does not accumulate drift,
4. block changes reverse exactly,
5. tile entity state reverses exactly,
6. inventories and held items reverse exactly,
7. direct seek and reverse traversal agree on target state,
8. reverse playback remains interactive in a representative GTNH base,
9. unsupported mod state is explicitly detected rather than silently diverging.

Until these are met, reverse playback should be described as experimental rather than complete.
