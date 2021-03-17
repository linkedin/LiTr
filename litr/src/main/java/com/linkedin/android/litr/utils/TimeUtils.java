package com.linkedin.android.litr.utils;

class TimeUtils {

    /**
     * Convert the time from Micros(Long) to seconds(float) to keep the fractions of a second
     *
     * @param timeUs time in Microseconds
     * @return the time as float to keep the fractions of a second
     */
    public static float microsToSeconds(long timeUs) {
        return timeUs / 1_000_000F;
    }

    /**
     * Convert the time from Millis(Long) to seconds(float) to keep the fractions of a second
     *
     * @param timeMs time in Milliseconds
     * @return the time as float to keep the fractions of a second
     */
    public static float millisToSeconds(long timeMs) {
        return timeMs / 1_000F;
    }
}
