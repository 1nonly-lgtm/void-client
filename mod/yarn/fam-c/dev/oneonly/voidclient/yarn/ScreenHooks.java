package dev.oneonly.voidclient.yarn;

import dev.oneonly.voidclient.core.VoidCore;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screen.Screen;

/** Klik myszką na otwartym ekranie (czat, ekwipunek) trafia najpierw do HUD Void (przyciski Spotify). */
final class ScreenHooks {
    private ScreenHooks() {}

    static void clicks(Screen screen) {
        if (screen instanceof VoidScreen) return;
        ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> !VoidCore.hudClick(click.x(), click.y(), click.button()));
    }
}
