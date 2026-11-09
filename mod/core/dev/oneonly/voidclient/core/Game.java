package dev.oneonly.voidclient.core;

import java.nio.file.Path;

/**
 * Dostęp do gry. Każda wersja Minecrafta ma własną implementację,
 * dzięki czemu reszta klienta (menu, moduły, zapis) jest wspólna.
 */
public interface Game {
    enum Key { FORWARD, LEFT, BACK, RIGHT, JUMP, ATTACK, USE }

    /** Przedmiot w slocie: ikonę rysuje Gfx#item, reszta to liczby. */
    record Slot(Object stack, boolean empty, boolean damageable, int left, int max, int count) {
        public static final Slot EMPTY = new Slot(null, true, false, 0, 0, 0);
    }

    int fps();

    /** Ping w ms albo -1, gdy brak danych. */
    int ping();

    boolean hasPlayer();

    int x();

    int y();

    int z();

    /** N, S, E, W */
    String facing();

    boolean key(Key key);

    boolean leftMouse();

    boolean rightMouse();

    /** 0 hełm, 1 napierśnik, 2 spodnie, 3 buty, 4 ręka. */
    Slot slot(int index);

    /** Gracz idzie do przodu i może biec. */
    boolean canSprint();

    void sprint();

    int fov();

    void setFov(int value);

    boolean smoothCamera();

    void setSmoothCamera(boolean value);

    /** Obiekt opcji jasności (SimpleOption / OptionInstance). */
    Object gammaOption();

    double gamma();

    /** Ustawia jasność w normalnym zakresie 0–1. */
    void setGamma(double value);

    boolean screenOpen();

    boolean hudHidden();

    Path configDir();

    /** Token sesji gracza (do zmiany skina przez oficjalne API). */
    String accessToken();

    /** Wysyła wiadomość na czat albo komendę (gdy zaczyna się od /). */
    void sendChat(String text);

    /** Nazwa klawisza przypisanego do slotu AutoText (0–5). */
    String autoTextKey(int slot);

    /** Tekst ze schowka (Ctrl+V w polach tekstowych). */
    String clipboard();

    /** Ustawienia grafiki pod maksymalne FPS (cząsteczki, chmury, cienie, vsync…). */
    void applyFpsPreset();

    float playerYaw();

    float playerPitch();

    /** 0 pierwsza osoba, 1 zza pleców, 2 od przodu. */
    int perspective();

    void setPerspective(int index);
}
