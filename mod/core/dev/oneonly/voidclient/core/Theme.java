package dev.oneonly.voidclient.core;

public final class Theme {
    public static final String[] ACCENT_NAMES = {"Fiolet", "Róż", "Błękit", "Zieleń", "Biały"};
    public static final int[] ACCENTS = {0xFFC084FC, 0xFFE070F5, 0xFF5CC8FF, 0xFF5EF08F, 0xFFFFFFFF};

    public static int accentIndex = 0;
    public static boolean hudBackground = true;
    public static boolean textShadow = true;

    private Theme() {}

    public static int accent() {
        return ACCENTS[Math.floorMod(accentIndex, ACCENTS.length)];
    }

    public static String accentName() {
        return ACCENT_NAMES[Math.floorMod(accentIndex, ACCENT_NAMES.length)];
    }

    /** Kolor akcentu z przezroczystością 0–255. */
    public static int accent(int alpha) {
        return (accent() & 0x00FFFFFF) | (alpha << 24);
    }
}
