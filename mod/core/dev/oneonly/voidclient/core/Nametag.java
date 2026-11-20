package dev.oneonly.voidclient.core;

import dev.oneonly.voidclient.core.module.Modules;

import java.util.UUID;

/** Własny nick nad głową (F5) z gradientem. */
public final class Nametag {
    private Nametag() {}

    /** Czy pokazać nick nad własną głową (gra domyślnie go chowa). */
    public static boolean showSelf(UUID id) {
        if (id == null || !Modules.NAMETAG.isEnabled()) return false;
        Game g = VoidCore.game();
        try { return id.equals(g.selfUuid()) && g.perspective() != 0 && !g.hudHidden(); } catch (Throwable t) { return false; }
    }

    public static boolean gradientFor(UUID id) {
        if (id == null || !Modules.NAMETAG.isEnabled()) return false;
        try { return id.equals(VoidCore.game().selfUuid()); } catch (Throwable t) { return false; }
    }

    /** Kolor litery i z n: od akcentu do różu i z powrotem (przesuwa się w czasie). */
    public static int color(int i, int n) {
        double t = (i / (double) Math.max(1, n - 1)) + System.currentTimeMillis() / 3000.0;
        double k = 0.5 + 0.5 * Math.sin(t * Math.PI * 2);
        int a = Theme.accent(), b = 0xFFF0ABFC;
        int r = (int) ((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * k);
        int gg = (int) ((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * k);
        int bb = (int) ((a & 255) + ((b & 255) - (a & 255)) * k);
        return r << 16 | gg << 8 | bb;
    }
}
