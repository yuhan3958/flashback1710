# Flashback 1710

A client-side replay recorder and editor for Minecraft 1.7.10 on the GTNH stack. Record a session, reopen it from the main menu, and inspect it in an isolated replay world.

> **Project relationship:** Flashback 1710 is an independent reimplementation inspired by Moulberry's Flashback. It is not a source port or source-level backport of the original mod, and its replay, editor, snapshot, checkpoint, and reverse-playback systems are independently implemented for the Minecraft 1.7.10 / Forge / GTNH environment. This project is not affiliated with or endorsed by Moulberry.

> Flashback 1710 is experimental. Replay accuracy and performance across the full GTNH mod set are still being validated.

## Start here

1. Join a world or server and run `/flashback record`.
2. Play normally, then run `/flashback stop` to finish the recording.
3. Return to the main menu and choose **Replays**. Pick a recording and press **Play**. You can also use `/flashback library` from a world.
4. Use the replay editor to seek, change speed, move the camera, and edit keyframes. The **Exit** button returns to your previous world or the main menu.

Recordings are stored in the Minecraft instance's `replays/` directory as `.fbr` files. Camera, Orbit, FOV, Speed, and Time of Day keyframes, markers, and playback ranges are saved beside them as `.fbr.fbe` files. Keep both files if you want to move an edited replay to another instance.

## Replay editor

The editor has a game view, toolbar, inspector, transport strip, track list, and six timeline rows: Camera, Orbit, FOV, Speed, Time of Day, and Markers. Playback opens paused. The free camera is enabled initially; **Player** follows the recorded player. While the editor is open, hold the right mouse button to look around with the free camera. Buttons use your selected Minecraft language; English and Korean translations are included.

| Action | Control |
| --- | --- |
| Play or pause | Transport button or Alt+Space |
| Seek | Left click or drag on the timeline |
| Zoom around cursor | Mouse wheel over the timeline |
| Pan the visible timeline | Shift + mouse wheel or middle mouse drag |
| Add a keyframe | Select Camera, Orbit, FOV, Speed, or Time of Day in the track list, seek, then press **Add** or Alt+K |
| Add an Orbit key | Aim the free camera at a subject, select Orbit, and press **Add**. The first center is eight blocks along the look direction; later Orbit keys retain the current center. |
| Edit an Orbit key | Select it to edit Center X/Y/Z, Distance, Yaw, and Pitch in the inspector. **Use Current Camera** recalculates distance, yaw, and pitch relative to its center. |
| Select or move a keyframe | Click or drag its mark in its timeline row |
| Change a FOV or Speed value | Select its key, click **Edit value** in the inspector, type a number, and press Enter |
| Update a camera keyframe's pose | Move the free camera, then press **Use camera pose** in the inspector |
| Edit a camera coordinate or angle | Select a camera key, click its X/Y/Z/Yaw/Pitch value in the inspector, type a number, and press Enter |
| Edit a Time of Day keyframe | Select its key, then enter a tick from `0` to `23999` or choose a dawn/noon/dusk/midnight preset in the inspector |
| Delete a selected keyframe | **Delete key** in the inspector or Alt+Delete |
| Set or clear a playback range | Inspector **In**, **Out**, and **Clear**, or Alt+I and Alt+O |
| Add a marker | Select Markers and press **Add**, or press Alt+M |
| Seek by one tick or one second | Alt+Left/Right or Alt+Shift+Left/Right |
| Hide or show the editor with Minecraft's HUD | Alt+F1 |
| Leave replay | **Exit** in the toolbar or **X** in the timeline transport |

Camera and FOV keyframes interpolate their values. Speed automation interpolates between multipliers from -8 to 8. Time of Day interpolates around the 24,000-tick day and sets the replay world's displayed clock to that absolute time of day; it does not alter the saved recording. The inspector's HUD switch controls in-game overlays during replay. The effective playback speed is the manual transport multiplier times the Speed track value; an empty Speed track means 1.0. A zero value freezes replay time while the editor stays interactive. Seeking always targets the requested replay time directly. Moving a keyframe changes its time while keeping its value. Adding another keyframe on the same track and time replaces the previous one.

## Commands

| Command | Purpose |
| --- | --- |
| `/flashback record` | Start recording |
| `/flashback stop` | Stop recording or replay |
| `/flashback library` | Open the replay library |
| `/flashback play [filename.fbr]` | Open the library or play a named file |
| `/flashback pause`, `/flashback resume`, `/flashback toggle` | Control playback |
| `/flashback speed <value>` | Set playback speed, including reverse speeds |
| `/flashback step` | Advance one tick while paused |
| `/flashback camera free`, `/flashback camera player` | Change the camera mode |
| `/flashback camera speed <value>` | Set free-camera movement speed |
| `/flashback ui` | Reopen the editor |

Supported speeds are `-4`, `-2`, `-1`, `-0.5`, `-0.25`, `0.25`, `0.5`, `1`, `2`, and `4`. Playback pauses at the start and end of a replay instead of closing it.

## How it works

Flashback records clientbound packets, selected player actions, an initial world snapshot, and periodic checkpoints. Replay runs in an isolated `ReplayWorld`; it does not seek by changing the live world. Seeking restores a checkpoint and replays the necessary packet tail. Reverse playback uses recent in-memory mutation history and reconstructs earlier windows as needed. It does not attempt to invert arbitrary packets.

The default delta checkpoint interval is 30 seconds, and the default full checkpoint anchor interval is 300 seconds. Both are configurable through Forge configuration; `0` disables an interval.

The current replay format is v8, with framed records, CRC32 checks, and recovery of valid records before a damaged trailing record. Older v6 and v7 recordings can be loaded. See [the replay format](docs/FORMAT.md) for details.

## Current limits

- Modded custom packets, tile entities, and other client-owned state need broader GTNH compatibility testing.
- Long recordings still need storage, memory, and seek-latency measurements.
- Reverse restoration is not exact for every block, tile entity, particle, sound, or mod-owned state change.
- Editor edits use a separate sidecar file. Exporting rendered video is not implemented.
- The library opens recordings from the current instance's `replays/` directory.

The [GTNH integration plan](docs/GTNH_INTEGRATION_PLAN.md) tracks the work needed before considering wider use.

## Development

The project targets Minecraft 1.7.10, Forge 10.13.4.1614, and Java 8. It uses GTNH Gradle and requires ModularUI2. Open the project as a Gradle project in IntelliJ IDEA. The development client task is `runClient`.

Flashback 1710 is a prototype. Please include the replay file, relevant log, and steps to reproduce when reporting a replay mismatch.
