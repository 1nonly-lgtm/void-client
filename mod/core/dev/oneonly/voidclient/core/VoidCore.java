package dev.oneonly.voidclient.core;

import dev.oneonly.voidclient.core.module.HudModule;
import dev.oneonly.voidclient.core.module.Module;
import dev.oneonly.voidclient.core.module.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wspólne serce klienta – wywoływane przez warstwę danej wersji gry. */
public final class VoidCore {
    public static final Logger LOG = LoggerFactory.getLogger("VoidClient");
    private static Game game;

    private VoidCore() {}

    public static void init(Game g) {
        game = g;
        Config.load(g.configDir());
        LOG.info("Void Client załadowany – by: 1nonly $");
    }

    public static Game game() { return game; }

    /** Koniec ticka gry (20/s). */
    public static void tick(boolean zoomPressed, boolean freelookHeld) {
        CpsTracker.poll(game.leftMouse(), game.rightMouse());
        Modules.ZOOM.update(game, zoomPressed);
        Freelook.update(game, freelookHeld);
        if (!game.hasPlayer()) return;
        for (Module m : Modules.all()) {
            if (m.isEnabled()) m.onTick(game);
        }
    }

    /** Rysowanie HUD (co klatkę). */
    public static void renderHud(Gfx g, int screenW, int screenH) {
        CpsTracker.poll(game.leftMouse(), game.rightMouse());
        if (game.hudHidden()) return;
        for (HudModule h : Modules.hud()) {
            if (!h.isEnabled()) continue;
            if (h.draggable()) {
                h.clamp(g, screenW, screenH);
                h.render(g, game);
            }
        }
        if (Modules.WATERMARK.isEnabled() && !game.screenOpen()) Modules.Watermark.draw(g, screenH);
    }

    /** Klawisz AutoText wciśnięty (slot 0–5). */
    public static void autoText(int slot) {
        AutoText.trigger(game, slot);
    }

    public static void shutdown() {
        Modules.ZOOM.update(game, false);
        Freelook.update(game, false);
        Config.save();
    }
}
