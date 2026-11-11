package dev.oneonly.voidclient.core;

import dev.oneonly.voidclient.core.module.Modules;

/** Viewmodel (położenie, rozmiar i obrót przedmiotu w ręce) oraz rozciągnięte proporcje obrazu. */
public final class View {
    public static float x = 0f, y = 0f, z = 0f;      // przesunięcie w blokach (-0.5 … 0.5)
    public static float scale = 1f;                   // 0.5 … 1.5
    public static int rotX = 0, rotY = 0, rotZ = 0;   // stopnie (-45 … 45)

    public static final String[] ASPECTS = {"Wyłączone", "4:3", "5:4", "3:2", "16:10"};
    private static final float[] RATIO = {0f, 4f / 3f, 5f / 4f, 3f / 2f, 16f / 10f};
    public static int aspect = 0;
    /** Ile razy gra dała nam zmienić projekcję – 0 w świecie znaczy, że ta wersja gry tego nie obsługuje. */
    public static volatile int aspectHits;

    private View() {}

    public static boolean viewmodel() { return Modules.VIEWMODEL.isEnabled(); }

    /**
     * Mnożnik skali X macierzy projekcji: obraz jak na rozciągniętej rozdzielczości (np. 4:3 na 16:9).
     * 1 = bez zmian.
     */
    public static float stretch(float windowAspect) {
        aspectHits++;
        if (aspect <= 0 || aspect >= RATIO.length || windowAspect <= 0) return 1f;
        return windowAspect / RATIO[aspect];
    }

    public static void reset() {
        x = y = z = 0f;
        scale = 1f;
        rotX = rotY = rotZ = 0;
    }

    public static String fmt(float v) {
        int t = Math.round(v * 10);
        return (t > 0 ? "+" : "") + (t / 10) + "." + Math.abs(t % 10);
    }

    /** Następna wartość w kółko: od min do max co krok. */
    public static float next(float v, float min, float max, float step) {
        float n = Math.round((v + step) / step) * step;
        return n > max + 0.0001f ? min : n;
    }
}
