package dev.oneonly.voidclient.core;

/** Rysowanie na ekranie – wspólne dla wszystkich wersji gry. Kolory w formacie ARGB. */
public interface Gfx {
    void fill(int x1, int y1, int x2, int y2, int argb);

    void gradient(int x1, int y1, int x2, int y2, int top, int bottom);

    void text(String text, int x, int y, int argb, boolean shadow);

    int width(String text);

    String trim(String text, int maxWidth);

    void item(Object stack, int x, int y);

    void scissor(int x1, int y1, int x2, int y2);

    void noScissor();

    /** Logo Void Client (okrągłe, z przezroczystością) w kwadracie size×size. */
    void logo(int x, int y, int size);

    default void border(int x, int y, int w, int h, int argb) {
        fill(x, y, x + w, y + 1, argb);
        fill(x, y + h - 1, x + w, y + h, argb);
        fill(x, y + 1, x + 1, y + h - 1, argb);
        fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }
}
