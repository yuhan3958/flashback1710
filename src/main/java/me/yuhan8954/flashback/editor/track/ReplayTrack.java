package me.yuhan8954.flashback.editor.track;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ReplayTrack<T> {

    private final String id;
    private final ReplayTrackType<T> type;
    private final List<ReplayKeyframe<T>> entries = new ArrayList<>();

    public ReplayTrack(String id, ReplayTrackType<T> type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public ReplayTrackType<T> getType() {
        return type;
    }

    public int getSize() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    public List<ReplayKeyframe<T>> keyframes() {
        return new ArrayList<>(entries);
    }

    public ReplayKeyframe<T> keyframeAt(long timestampNanos) {
        int index = findIndex(timestampNanos);
        return index >= 0 ? entries.get(index) : null;
    }

    public void put(ReplayKeyframe<T> keyframe) {
        type.validate(keyframe.getValue());
        long timestamp = Math.max(keyframe.getTimestampNanos(), 0L);
        ReplayKeyframe<T> normalized = keyframe.copy(timestamp, keyframe.getValue(), keyframe.getInterpolation());
        int index = findIndex(timestamp);
        if (index >= 0) {
            entries.set(index, normalized);
        } else {
            entries.add(-index - 1, normalized);
        }
    }

    public boolean move(long fromTimestampNanos, long toTimestampNanos) {
        ReplayKeyframe<T> keyframe = keyframeAt(fromTimestampNanos);
        if (keyframe == null) {
            return false;
        }
        delete(fromTimestampNanos);
        put(keyframe.copy(toTimestampNanos, keyframe.getValue(), keyframe.getInterpolation()));
        return true;
    }

    public boolean delete(long timestampNanos) {
        int index = findIndex(timestampNanos);
        if (index < 0) {
            return false;
        }
        entries.remove(index);
        return true;
    }

    public boolean updateInterpolation(long timestampNanos, ReplayInterpolation interpolation) {
        int index = findIndex(timestampNanos);
        if (index < 0) {
            return false;
        }
        ReplayKeyframe<T> keyframe = entries.get(index);
        entries.set(index, keyframe.copy(timestampNanos, keyframe.getValue(), interpolation));
        return true;
    }

    public T evaluate(long timestampNanos) {
        if (entries.isEmpty()) {
            return null;
        }
        int index = findIndex(timestampNanos);
        int beforeIndex = index >= 0 ? index : Math.max(0, -index - 2);
        return type.evaluate(entries, beforeIndex, timestampNanos);
    }

    public void writeKeyframes(DataOutput output) throws IOException {
        output.writeInt(entries.size());
        for (ReplayKeyframe<T> keyframe : entries) {
            output.writeLong(keyframe.getTimestampNanos());
            output.writeUTF(
                keyframe.getInterpolation()
                    .getSerializedId());
            type.writeValue(output, keyframe.getValue());
        }
    }

    public void readKeyframes(DataInput input, int maximumCount) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > maximumCount) {
            throw new IllegalArgumentException("Invalid keyframe count");
        }
        for (int i = 0; i < count; i++) {
            long timestamp = input.readLong();
            ReplayInterpolation interpolation = ReplayInterpolation.Companion.fromSerializedId(input.readUTF());
            put(new ReplayKeyframe<>(timestamp, type.readValue(input), interpolation));
        }
    }

    private int findIndex(long timestampNanos) {
        int low = 0;
        int high = entries.size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            long middleTimestamp = entries.get(middle)
                .getTimestampNanos();
            if (middleTimestamp < timestampNanos) {
                low = middle + 1;
            } else if (middleTimestamp > timestampNanos) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return -(low + 1);
    }
}
