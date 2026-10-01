# Replay UI Layout

The replay screen has a game view, inspector, and full-width timeline below a toolbar.

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

## Geometry

The target proportions are intentionally simple:

- upper workspace: about 72% of the space below the toolbar,
- timeline: about 28% of that space, capped at 210 scaled pixels,
- VIEW: about 70% of the screen width,
- SETTINGS: about 30% of the screen width, capped at 220 scaled pixels,
- TIMELINE: full screen width.

The regions are edge-aligned so the two main separators read like the reference wireframe instead of three floating cards.

## Timeline interaction

`ReplayTimelineWidget` behaves like an editing viewport rather than a fixed progress bar.

- Wheel zooms the time scale around the mouse cursor.
- Shift + wheel pans horizontally.
- Middle-mouse drag pans horizontally.
- Left click and left drag seek.
- Major ruler ticks show formatted replay time.
- Minor ticks adapt to the current scale.
- During playback the viewport follows the playhead without forcing the whole replay to fit on screen.

Zoom changes only the viewport scale; replay time itself is unchanged.

## Tracks and inspector

The timeline has Camera, FOV, Speed, Time of Day, and Markers rows. Select a row before adding a keyframe. The Time of Day track changes the rendered sky on a 24,000-tick clock without editing recorded world time. Its inspector accepts integer ticks from `0` through `23999` and offers dawn, noon, dusk, and midnight presets. The inspector also has a replay-only HUD overlay switch. Track edits, markers, and playback range are saved in the `.fbr.fbe` sidecar file.

Replay editor keyboard actions require Alt: Alt+Space toggles playback, Alt+Left/Right seeks one tick, Alt+Shift+Left/Right seeks one second, Alt+K adds a keyframe to the selected row, Alt+Delete removes the selected keyframe, Alt+I/O sets the playback range, Alt+M adds a marker, and Alt+F1 toggles Minecraft's HUD visibility. Enter, Backspace, and Escape still operate normally while typing an inspector value.

UI text is loaded from `assets/flashback1710/lang/en_US.lang` and `ko_KR.lang` through `ReplayLang`.

## Implementation notes

The UI remains built with ModularUI2 and follows the repository's existing GTNH convention-plugin and formatting setup. UI state stays separate from replay reconstruction logic: the timeline asks `ReplayPlayer` to seek, while playback/session ownership remains in the replay layer.
