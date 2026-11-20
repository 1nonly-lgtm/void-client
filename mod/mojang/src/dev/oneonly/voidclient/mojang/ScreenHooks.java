package dev.oneonly.voidclient.mojang;

import dev.oneonly.voidclient.core.VoidCore;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.Screen;

/** Klik myszką na otwartym ekranie (czat, ekwipunek) trafia najpierw do HUD Void (przyciski Spotify). */
final class ScreenHooks {
    private ScreenHooks() {}

    static void clicks(Screen screen) {
        if (screen instanceof VoidScreen) return;
        ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !VoidCore.hudClick(event.x(), event.y(), event.button()));
    }
}
