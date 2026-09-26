package me.yuhan8954.flashback.ui;

public final class ReplayTimeFormatter {

    private static final long NANOS_PER_MILLISECOND = 1_000_000L;
    private static final long MILLIS_PER_SECOND = 1_000L;
    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final long MILLIS_PER_HOUR = 3_600_000L;

    private ReplayTimeFormatter() {}

    public static String format(long nanoseconds) {
        long totalMilliseconds = Math.max(nanoseconds, 0L) / NANOS_PER_MILLISECOND;
        long hours = totalMilliseconds / MILLIS_PER_HOUR;
        long minutes = totalMilliseconds % MILLIS_PER_HOUR / MILLIS_PER_MINUTE;
        long seconds = totalMilliseconds % MILLIS_PER_MINUTE / MILLIS_PER_SECOND;
        long milliseconds = totalMilliseconds % MILLIS_PER_SECOND;

        StringBuilder builder = new StringBuilder(hours > 0L ? 12 : 9);
        if (hours > 0L) {
            appendTwoOrMore(builder, hours);
            builder.append(':');
        }
        appendTwo(builder, minutes);
        builder.append(':');
        appendTwo(builder, seconds);
        builder.append('.');
        appendThree(builder, milliseconds);
        return builder.toString();
    }

    public static String formatRuler(long nanoseconds) {
        long totalMilliseconds = Math.max(nanoseconds, 0L) / NANOS_PER_MILLISECOND;
        long hours = totalMilliseconds / MILLIS_PER_HOUR;
        long minutes = totalMilliseconds % MILLIS_PER_HOUR / MILLIS_PER_MINUTE;
        long seconds = totalMilliseconds % MILLIS_PER_MINUTE / MILLIS_PER_SECOND;
        long milliseconds = totalMilliseconds % MILLIS_PER_SECOND;

        if (hours > 0L) {
            StringBuilder builder = new StringBuilder(8);
            builder.append(hours).append(':');
            appendTwo(builder, minutes);
            builder.append(':');
            appendTwo(builder, seconds);
            return builder.toString();
        }

        if (totalMilliseconds < MILLIS_PER_SECOND) {
            StringBuilder builder = new StringBuilder(6);
            builder.append("00.");
            appendThree(builder, milliseconds);
            return builder.toString();
        }

        StringBuilder builder = new StringBuilder(5);
        appendTwoOrMore(builder, minutes);
        builder.append(':');
        appendTwo(builder, seconds);
        return builder.toString();
    }

    public static String formatSpeed(double speed) {
        if (speed == Math.rint(speed)) {
            return Long.toString((long) speed) + 'x';
        }

        long quarters = Math.round(speed * 4.0D);
        if (Math.abs(speed * 4.0D - quarters) < 1.0E-9D) {
            long whole = quarters / 4;
            long remainder = Math.abs(quarters % 4);
            if (remainder == 0L) {
                return Long.toString(whole) + 'x';
            }
            if (remainder == 2L) {
                return (quarters < 0 && whole == 0 ? "-" : Long.toString(whole))
                    + ".5x";
            }
            String prefix = quarters < 0 && whole == 0 ? "-" : Long.toString(whole);
            return prefix + (remainder == 1L ? ".25x" : ".75x");
        }

        return Double.toString(speed) + 'x';
    }

    private static void appendTwo(StringBuilder builder, long value) {
        if (value < 10L) {
            builder.append('0');
        }
        builder.append(value);
    }

    private static void appendTwoOrMore(StringBuilder builder, long value) {
        if (value < 10L) {
            builder.append('0');
        }
        builder.append(value);
    }

    private static void appendThree(StringBuilder builder, long value) {
        if (value < 100L) {
            builder.append('0');
        }
        if (value < 10L) {
            builder.append('0');
        }
        builder.append(value);
    }
}
