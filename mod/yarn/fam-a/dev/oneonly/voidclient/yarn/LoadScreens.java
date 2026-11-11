package dev.oneonly.voidclient.yarn;

import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.LevelLoadingScreen;

/** Ekrany łączenia i wczytywania, na których rysujemy tło Void Client. */
final class LoadScreens {
    private LoadScreens() {}

    static boolean is(Screen s) {
        return s instanceof ConnectScreen || s instanceof DownloadingTerrainScreen || s instanceof LevelLoadingScreen
            || s instanceof MessageScreen || s instanceof ProgressScreen;
    }
}
