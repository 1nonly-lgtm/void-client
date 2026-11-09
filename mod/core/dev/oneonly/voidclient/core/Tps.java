package dev.oneonly.voidclient.core;

/**
 * TPS serwera liczone po stronie klienta: serwer wysyła czas świata co 20 swoich ticków,
 * więc przy 20 TPS pakiety przychodzą co ~1 s. Im wolniej przychodzą, tym niższe TPS.
 */
public final class Tps {
    private static final int SAMPLES = 6;
    private static final double[] values = new double[SAMPLES];
    private static int count;
    private static int next;
    private static long last;

    private Tps() {}

    /** Wywoływane przy każdym pakiecie czasu świata (na wątku gry). */
    public static void onTimePacket() {
        long now = System.nanoTime();
        if (last != 0) {
            double seconds = (now - last) / 1_000_000_000.0;
            if (seconds > 0.05 && seconds < 15) {
                values[next] = Math.min(20.0, 20.0 / seconds);
                next = (next + 1) % SAMPLES;
                if (count < SAMPLES) count++;
            }
        }
        last = now;
    }

    /** Średnie TPS albo -1, gdy brak danych (menu, świeże wejście, zerwane połączenie). */
    public static double value() {
        if (count == 0 || last == 0) return -1;
        if (System.nanoTime() - last > 10_000_000_000L) return -1;
        double sum = 0;
        for (int i = 0; i < count; i++) sum += values[i];
        return sum / count;
    }

    public static void reset() {
        count = 0;
        next = 0;
        last = 0;
    }
}
