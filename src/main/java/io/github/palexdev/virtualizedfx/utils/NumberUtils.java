package io.github.palexdev.virtualizedfx.utils;

public class NumberUtils {

    /**
     * Limits the given value to the given min-max range by returning the nearest bound
     * if it exceeds or val if it's in range.
     */
    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }


    /**
     * Limits the given value to the given min-max range by returning the nearest bound
     * if it exceeds or val if it's in range.
     */
    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
