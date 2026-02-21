package org.blueobsidian.worldTimer.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([smhd])$", Pattern.CASE_INSENSITIVE);

    private TimeUtil() {
    }

    /**
     * Parses a time string like "30m", "2h", "3600s", "1d" into seconds.
     * Returns -1 for "unlimited".
     * Returns -2 if the format is invalid.
     */
    public static long parseTimeToSeconds(String input) {
        if (input == null || input.isEmpty()) {
            return -2;
        }
        if (input.equalsIgnoreCase("unlimited")) {
            return -1;
        }
        Matcher matcher = TIME_PATTERN.matcher(input);
        if (!matcher.matches()) {
            return -2;
        }
        long value = Long.parseLong(matcher.group(1));
        char unit = matcher.group(2).toLowerCase().charAt(0);
        return switch (unit) {
            case 's' -> value;
            case 'm' -> value * 60;
            case 'h' -> value * 3600;
            case 'd' -> value * 86400;
            default -> -2;
        };
    }

    /**
     * Formats seconds into a human-readable string like "5m 30s", "2h 10m".
     */
    public static String formatTime(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s";
        }
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
