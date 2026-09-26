# Replay UI Layout

The replay screen follows the supplied three-region editor reference.

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

- upper workspace: about 2/3 of the screen height,
- timeline: about 1/3 of the screen height,
- VIEW: about 2/3 of the screen width,
- SETTINGS: about 1/3 of the screen width,
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

## Implementation notes

The UI remains built with ModularUI2 and follows the repository's existing GTNH convention-plugin and formatting setup. UI state stays separate from replay reconstruction logic: the timeline asks `ReplayPlayer` to seek, while playback/session ownership remains in the replay layer.
