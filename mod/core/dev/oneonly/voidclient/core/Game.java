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

    /** "sp" w świecie jednoosobowym, adres serwera w multi, "" w menu. */
    String server();

    /** Pozycja oczu gracza {x, y, z}. */
    double[] eye();

    /** Identyfikator wymiaru (do przypisania waypointów do świata). */
    String dimension();
    /** Efekt mikstury: nazwa (przetłumaczona), poziom od 0, czas w tickach. */
    record Effect(String name, int amplifier, int ticks, boolean infinite) {}

    java.util.List<Effect> effects();

    /**
     * Liczy przedmioty w ekwipunku. match dostaje "id|nazwa" (np. "minecraft:arrow|Strzała"),
     * a blocks = true liczy wszystkie bloki. Zwraca pierwszy pasujący stos (ikona) i sumę sztuk.
     */
    Slot countItems(java.util.function.Predicate<String> match, boolean blocks);

    /** Nazwa biomu pod graczem (przetłumaczona) albo "". */
    String biome();

    /** Folder gry (.minecraft profilu). */
    Path gameDir();

    boolean modLoaded(String id);

    /** Włącza / wyłącza paczkę zasobów o danym id i przeładowuje zasoby. */
    void reloadPack(String packId, boolean enabled);

    /** Wiadomość widoczna tylko dla gracza (na czacie). */
    void chatInfo(String text);

    /** Zawartość przedmiotu-pojemnika (np. shulkera) – 27 slotów (puste = null) albo pusta lista. */
    java.util.List<Object> containerItems(Object stack);

    /** Czy działa zmiana koloru obramowania bloku. */
    boolean outlineSupported();

    /** UUID graczy z listy Tab (do ikonki Void przy nicku). */
    java.util.List<java.util.UUID> tabUuids();

    /** Punkty pancerza (0–20, jak w grze). */
    int armorPoints();

    /** Naładowanie ataku 0–1 (1 = pełne obrażenia). */
    float attackCooldown();

    /** Czy następny cios będzie krytyczny (gracz spada, nie jest w wodzie, na drabinie ani w pojeździe). */
    boolean critReady();

    /** Czy da się przesuwać scoreboard (wersje 1.20–1.21.11). */
    boolean scoreboardMovable();
}
