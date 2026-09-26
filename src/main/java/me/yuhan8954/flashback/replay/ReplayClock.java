package me.yuhan8954.flashback.replay;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

public final class ReplayClock {

    public static final long MINECRAFT_TICK_NANOS = 50_000_000L;
    public static final double DEFAULT_SPEED = 1.0D;

    public static final List<Double> SUPPORTED_SPEEDS = Collections
        .unmodifiableList(Arrays.asList(-4.0D, -2.0D, -1.0D, -0.5D, -0.25D, 0.25D, 0.5D, 1.0D, 2.0D, 4.0D));

    private final LongSupplier timeSource;

    private long currentTimeNanos;
    private double speedMultiplier = DEFAULT_SPEED;
    private boolean paused = true;
    private long lastUpdateNanos;

    public ReplayClock() {
        this(System::nanoTime);
    }

    public ReplayClock(LongSupplier timeSource) {
        this.timeSource = timeSource;
        this.lastUpdateNanos = timeSource.getAsLong();
    }

    public long getCurrentTimeNanos() {
        return currentTimeNanos;
    }

    public double getSpeed() {
        return speedMultiplier;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean getPaused() {
        return paused;
    }

    public void reset() {
        currentTimeNanos = 0L;
        speedMultiplier = DEFAULT_SPEED;
        paused = true;
        lastUpdateNanos = timeSource.getAsLong();
    }

    public void update() {
        long now = timeSource.getAsLong();
        long wallTimeDelta = Math.max(now - lastUpdateNanos, 0L);

        if (!paused) {
            currentTimeNanos = Math.max(currentTimeNanos + (long) (wallTimeDelta * speedMultiplier), 0L);
        }

        lastUpdateNanos = now;
    }

    public void pause() {
        if (paused) {
            return;
        }

        update();
        paused = true;
    }

    public void resume() {
        if (!paused) {
            return;
        }

        lastUpdateNanos = timeSource.getAsLong();
        paused = false;
    }

    public void togglePause() {
        if (paused) {
            resume();
        } else {
            pause();
        }
    }

    public void setSpeed(double speed) {
        if (!isSupportedSpeed(speed)) {
            throw new IllegalArgumentException("Unsupported replay speed: " + speed);
        }

        update();
        speedMultiplier = speed;
    }

    public void seek(long timeNanos) {
        currentTimeNanos = Math.max(timeNanos, 0L);
        lastUpdateNanos = timeSource.getAsLong();
    }

    public void step() {
        if (!paused) {
            throw new IllegalStateException("Replay must be paused before stepping");
        }

        currentTimeNanos += MINECRAFT_TICK_NANOS;
        lastUpdateNanos = timeSource.getAsLong();
    }

    public void stop() {
        currentTimeNanos = 0L;
        speedMultiplier = DEFAULT_SPEED;
        paused = true;
        lastUpdateNanos = timeSource.getAsLong();
    }

    public static boolean isSupportedSpeed(double speed) {
        return SUPPORTED_SPEEDS.contains(speed);
    }
}
