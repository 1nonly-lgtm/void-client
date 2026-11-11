package dev.oneonly.voidclient.mojang;

import com.mojang.blaze3d.platform.InputConstants;
import dev.oneonly.voidclient.core.AutoText;
import dev.oneonly.voidclient.core.LoadingArt;
import dev.oneonly.voidclient.core.VoidCore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class VoidClientMod implements ClientModInitializer {
    public static KeyMapping guiKey;
    public static final KeyMapping[] autoKeys = new KeyMapping[AutoText.SLOTS];
    private static final MojangGfx HUD_GFX = new MojangGfx();
    private static final MojangGfx LOAD_GFX = new MojangGfx();

    @Override
    public void onInitializeClient() {
        VoidCore.init(new MojangGame());

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("voidclient", "main"));
        guiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.voidclient.gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        for (int i = 0; i < AutoText.SLOTS; i++) {
            autoKeys[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.voidclient.autotext" + (i + 1), InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.consumeClick()) {
                if (client.screen == null) client.setScreen(new VoidScreen());
            }
            for (int i = 0; i < autoKeys.length; i++) {
                while (autoKeys[i].consumeClick()) VoidCore.autoText(i);
            }
            VoidCore.tick(); // Zoom, Freelook i Waypoint – klawisze ustawiane w menu Void Client
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> VoidCore.shutdown());

        // Łączenie z serwerem i wczytywanie świata – tło Void Client, przyciski rysujemy na nowo na wierzchu
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen instanceof TitleScreen) {
                // menu główne: unoszące się iskry, spadające gwiazdy i poświata przycisku pod kursorem
                ScreenEvents.afterExtract(screen).register((s, graphics, mx, my, delta) -> {
                    try {
                        MojangGfx gx = LOAD_GFX.set(graphics, client.font);
                        LoadingArt.titleFx(gx, s.width, s.height);
                        for (AbstractWidget b : Screens.getWidgets(s)) {
                            if (b.visible && b.active && b.isHovered()) LoadingArt.buttonGlow(gx, b.getX(), b.getY(), b.getWidth(), b.getHeight());
                        }
                    } catch (Throwable ignored) {
                        // bez animacji
                    }
                });
                return;
            }
            if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen) {
                // podgląd zawartości shulkera pod kursorem
                ScreenEvents.afterExtract(screen).register((s, graphics, mx, my, delta) -> {
                    try {
                        Object slot = hoveredSlot(s);
                        if (!(slot instanceof net.minecraft.world.inventory.Slot sl) || !sl.hasItem()) return;
                        dev.oneonly.voidclient.core.ShulkerPreview.draw(LOAD_GFX.set(graphics, client.font), VoidCore.game(),
                                sl.getItem(), mx, my, s.width, s.height);
                    } catch (Throwable ignored) {
                        // bez podglądu
                    }
                });
                return;
            }
            if (!isLoading(screen)) return;
            ScreenEvents.afterExtract(screen).register((s, graphics, mx, my, delta) -> {
                try {
                    LoadingArt.screen(LOAD_GFX.set(graphics, client.font), s.width, s.height, s.getTitle().getString());
                    for (AbstractWidget b : Screens.getWidgets(s)) b.extractRenderState(graphics, mx, my, delta);
                } catch (Throwable ignored) {
                    // zostaje zwykły ekran gry
                }
            });
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("voidclient", "hud"), (graphics, delta) -> {
            Minecraft c = Minecraft.getInstance();
            VoidCore.renderHud(HUD_GFX.set(graphics, c.font), c.getWindow().getGuiScaledWidth(), c.getWindow().getGuiScaledHeight());
        });
    }

    private static java.lang.reflect.Field hovered;

    /** 26.1 nie jest zaciemnione, więc pole „hoveredSlot” czytamy po nazwie. */
    private static Object hoveredSlot(Screen s) throws ReflectiveOperationException {
        if (hovered == null) {
            hovered = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
            hovered.setAccessible(true);
        }
        return hovered.get(s);
    }

    private static boolean isLoading(Screen s) {
        return s instanceof ConnectScreen || s instanceof LevelLoadingScreen || s instanceof GenericMessageScreen || s instanceof ProgressScreen;
    }
}
