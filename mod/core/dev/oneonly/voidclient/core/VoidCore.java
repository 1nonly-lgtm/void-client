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
    private static int statusTicks;

    private VoidCore() {}

    public static void init(Game g) {
        game = g;
        Config.load(g.configDir());
        Sky.init(g);
        LOG.info("Void Client załadowany – by: 1nonly $");
    }

    public static Game game() { return game; }

    /** Koniec ticka gry (20/s). Klawisze Void Client czytamy sami – ustawia się je w naszym menu. */
    public static void tick() {
        CpsTracker.poll(game.leftMouse(), game.rightMouse());
        boolean typing = game.screenOpen();
        Modules.ZOOM.update(game, !typing && Keybinds.isDown(Keybinds.zoom));
        Freelook.update(game, !typing && Keybinds.isDown(Keybinds.freelook));
        boolean addWp = Keybinds.pressed("waypoint", Keybinds.waypoint);
        Waypoints.tick(game);
        status();
        try { Screenshots.tick(game); } catch (Throwable ignored) {}
        if (!game.hasPlayer()) {
            Tps.reset();
            return;
        }
        if (addWp && !typing && Modules.WAYPOINTS.isEnabled()) Waypoints.addHere(game);
        for (Module m : Modules.all()) {
            if (m.isEnabled()) m.onTick(game);
        }
    }

    /**
     * Co 5 s wypisuje do logu linię dla launchera (status na Discordzie: serwer i FPS).
     * Launcher czyta wyjście gry i szuka „[VoidStatus]”.
     */
    private static void status() {
        if (++statusTicks < 100) return;
        statusTicks = 0;
        String srv;
        try { srv = game.server(); } catch (Throwable t) { srv = ""; }
        if (srv == null) srv = "";
        String line = "srv=" + srv.replace(' ', '_') + " fps=" + game.fps();
        LOG.info("[VoidStatus] {}", line);
    }

    /** Rysowanie HUD (co klatkę). */
    public static void renderHud(Gfx g, int screenW, int screenH) {
        CpsTracker.poll(game.leftMouse(), game.rightMouse());
        if (game.hudHidden()) return;
        Waypoints.render(g, game, screenW, screenH);
        for (HudModule h : Modules.hud()) {
            if (!h.isEnabled()) continue;
            if (h.draggable()) {
                h.clamp(g, screenW, screenH);
                h.render(g, game);
            }
        }
        if (Modules.WATERMARK.isEnabled() && !game.screenOpen()) Modules.Watermark.draw(g, screenH);
        if (customCrosshair()) Look.drawCrosshair(g, screenW, screenH);
    }

    /** Czy zamiast celownika z gry rysujemy własny (mixin wtedy wyłącza ten z gry). */
    public static boolean customCrosshair() {
        try {
            return game != null && Modules.CROSSHAIR.isEnabled() && game.hasPlayer() && game.perspective() == 0;
        } catch (Throwable t) {
            return false;
        }
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
