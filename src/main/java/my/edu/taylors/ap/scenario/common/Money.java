package my.edu.taylors.ap.scenario.common;

/**
 * Money is always held as whole sen in a {@code long}/{@code int}. Never use double for money.
 */
public final class Money {

    private Money() { }

    /** 1234 -> "RM12.34" */
    public static String rm(long sen) {
        String sign = sen < 0 ? "-" : "";
        long abs = Math.abs(sen);
        return String.format("%sRM%d.%02d", sign, abs / 100, abs % 100);
    }

    /** value x percent / 100, rounded half-up to the sen. */
    public static long percentOf(long sen, int percent) {
        return Math.floorDiv(sen * percent + 50, 100);
    }

    /**
     * Malaysian rounding mechanism: round to the nearest 5 sen.
     * Endings 1-2 round down to 0, 3-4 round up to 5, 6-7 round down to 5, 8-9 round up to 10.
     */
    public static long roundToNearest5Sen(long sen) {
        long r = Math.floorMod(sen, 5);
        return r <= 2 ? sen - r : sen + (5 - r);
    }
}
