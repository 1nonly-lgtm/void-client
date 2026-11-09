package dev.oneonly.voidclient.core;

import dev.oneonly.voidclient.core.module.Modules;

/**
 * Freelook: trzymasz klawisz (domyślnie lewy Alt) i obracasz kamerę myszką wokół postaci,
 * a postać patrzy dalej w tę samą stronę.
 */
public final class Freelook {
    private static boolean active;
    private static float yaw;
    private static float pitch;
    private static int savedPerspective;

    private Freelook() {}

    public static boolean active() { return active; }

    public static float yaw() { return yaw; }

    public static float pitch() { return pitch; }

    public static void update(Game game, boolean held) {
        boolean want = held && Modules.FREELOOK.isEnabled() && game.hasPlayer() && !game.screenOpen();
        if (want && !active) {
            active = true;
            yaw = game.playerYaw();
            pitch = game.playerPitch();
            savedPerspective = game.perspective();
            if (savedPerspective == 0) game.setPerspective(1); // widok zza pleców
        } else if (!want && active) {
            active = false;
            game.setPerspective(savedPerspective);
        }
    }

    /** Ruch myszy podczas Freelooka – obraca kamerę zamiast gracza. */
    public static void turn(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
    }
}
