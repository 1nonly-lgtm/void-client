package dev.oneonly.voidclient.core;

import dev.oneonly.voidclient.core.module.Modules;

import java.time.LocalTime;

/** Czat: godzina przy wiadomości, łączenie powtórzeń w „×N” i dłuższa historia. */
public final class ChatTweaks {
    private static String last = null;
    private static int repeat = 0;

    private ChatTweaks() {}

    public static boolean timestamps() { return Modules.CHAT_TIME.isEnabled(); }

    public static String timestamp() {
        LocalTime t = LocalTime.now();
        return String.format("[%02d:%02d] ", t.getHour(), t.getMinute());
    }

    /**
     * Wywoływane dla każdej nowej wiadomości. Zwraca, który raz z rzędu przyszła ta sama treść
     * (1 = nowa). Gdy &gt; 1, warstwa gry usuwa poprzednią linijkę i dopisuje „×N”.
     */
    public static int onMessage(String plain) {
        if (!Modules.CHAT_STACK.isEnabled()) { last = null; return 1; }
        if (plain.equals(last)) return ++repeat;
        last = plain;
        repeat = 1;
        return 1;
    }

    /** Ile wiadomości pamięta czat (w grze 100). */
    public static int historyLimit(int vanilla) {
        return Modules.CHAT_HISTORY.isEnabled() ? Math.max(vanilla, 1000) : vanilla;
    }
}
