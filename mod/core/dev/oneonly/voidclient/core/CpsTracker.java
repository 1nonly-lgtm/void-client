package dev.oneonly.voidclient.core;

/**
 * Liczy kliknięcia z ostatniej sekundy. Stan przycisków jest sprawdzany co klatkę,
 * więc działa bez ingerencji w kod gry (to samo na każdej wersji).
 */
public final class CpsTracker {
    private static final int SIZE = 64;
    private static final long[] LEFT = new long[SIZE];
    private static final long[] RIGHT = new long[SIZE];
    private static int leftIdx, rightIdx;
    private static boolean wasLeft, wasRight;

    private CpsTracker() {}

    public static void poll(boolean left, boolean right) {
        long now = System.currentTimeMillis();
        if (left && !wasLeft) { LEFT[leftIdx] = now; leftIdx = (leftIdx + 1) % SIZE; }
        if (right && !wasRight) { RIGHT[rightIdx] = now; rightIdx = (rightIdx + 1) % SIZE; }
        wasLeft = left;
        wasRight = right;
    }

    public static int left() { return count(LEFT); }

    public static int right() { return count(RIGHT); }

    private static int count(long[] arr) {
        long limit = System.currentTimeMillis() - 1000L;
        int c = 0;
        for (long t : arr) if (t > limit) c++;
        return c;
    }
}
