package dev.oneonly.voidclient.yarn;

import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;

/** Ekrany łączenia i wczytywania, na których rysujemy tło Void Client (1.21.9+). */
final class LoadScreens {
    private LoadScreens() {}

    static boolean is(Screen s) {
        return s instanceof ConnectScreen || s instanceof LevelLoadingScreen || s instanceof MessageScreen || s instanceof ProgressScreen;
    }
}
