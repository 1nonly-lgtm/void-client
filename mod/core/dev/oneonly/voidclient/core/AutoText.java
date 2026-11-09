package dev.oneonly.voidclient.core;

import dev.oneonly.voidclient.core.module.Modules;

/** AutoText: pod wybranym klawiszem wysyła zapisaną wiadomość albo komendę. */
public final class AutoText {
    public static final int SLOTS = 6;
    public static final int MAX_LENGTH = 256;
    private static final long COOLDOWN_MS = 600; // ochrona przed spamem i kickiem z serwera

    public static final String[] TEXTS = new String[SLOTS];
    private static final long[] LAST = new long[SLOTS];

    static {
        for (int i = 0; i < SLOTS; i++) TEXTS[i] = "";
    }

    private AutoText() {}

    public static void trigger(Game game, int slot) {
        if (slot < 0 || slot >= SLOTS || !Modules.AUTOTEXT.isEnabled()) return;
        if (!game.hasPlayer() || game.screenOpen()) return;
        String text = TEXTS[slot] == null ? "" : TEXTS[slot].trim();
        if (text.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - LAST[slot] < COOLDOWN_MS) return;
        LAST[slot] = now;
        game.sendChat(text);
    }
}
