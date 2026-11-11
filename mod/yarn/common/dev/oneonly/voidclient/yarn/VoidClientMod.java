package dev.oneonly.voidclient.yarn;

import dev.oneonly.voidclient.core.AutoText;
import dev.oneonly.voidclient.core.LoadingArt;
import dev.oneonly.voidclient.core.VoidCore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class VoidClientMod implements ClientModInitializer {
    public static KeyBinding guiKey;
    public static final KeyBinding[] autoKeys = new KeyBinding[AutoText.SLOTS];
    private static final YarnGfx HUD_GFX = new YarnGfx();
    private static final YarnGfx LOAD_GFX = new YarnGfx();

    @Override
    public void onInitializeClient() {
        VoidCore.init(new YarnGame());

        guiKey = KeyBindingHelper.registerKeyBinding(Keys.create("key.voidclient.gui", GLFW.GLFW_KEY_RIGHT_SHIFT));
        for (int i = 0; i < AutoText.SLOTS; i++) {
            autoKeys[i] = KeyBindingHelper.registerKeyBinding(Keys.create("key.voidclient.autotext" + (i + 1), GLFW.GLFW_KEY_UNKNOWN));
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new VoidScreen());
            }
            for (int i = 0; i < autoKeys.length; i++) {
                while (autoKeys[i].wasPressed()) VoidCore.autoText(i);
            }
            VoidCore.tick(); // Zoom, Freelook i Waypoint – klawisze ustawiane w menu Void Client
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> VoidCore.shutdown());

        // Łączenie z serwerem i wczytywanie świata – tło Void Client, przyciski (np. Anuluj) rysujemy na nowo na wierzchu
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen instanceof TitleScreen) {
                // menu główne: unoszące się iskry, spadające gwiazdy i poświata przycisku pod kursorem
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                    try {
                        YarnGfx gx = LOAD_GFX.set(ctx, client.textRenderer);
                        LoadingArt.titleFx(gx, s.width, s.height);
                        for (ClickableWidget b : Screens.getButtons(s)) {
                            if (b.visible && b.active && b.isHovered()) LoadingArt.buttonGlow(gx, b.getX(), b.getY(), b.getWidth(), b.getHeight());
                        }
                    } catch (Throwable ignored) {
                        // bez animacji
                    }
                });
                return;
            }
            if (screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen) {
                // podgląd zawartości shulkera pod kursorem
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                    try {
                        var slot = ((dev.oneonly.voidclient.yarn.mixin.HandledScreenAccessor) s).voidclient$focusedSlot();
                        if (slot == null || !slot.hasStack()) return;
                        dev.oneonly.voidclient.core.ShulkerPreview.draw(LOAD_GFX.set(ctx, client.textRenderer), VoidCore.game(),
                                slot.getStack(), mx, my, s.width, s.height);
                    } catch (Throwable ignored) {
                        // bez podglądu
                    }
                });
                return;
            }
            if (!LoadScreens.is(screen)) return;
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                try {
                    LoadingArt.screen(LOAD_GFX.set(ctx, client.textRenderer), s.width, s.height, s.getTitle().getString());
                    for (ClickableWidget b : Screens.getButtons(s)) b.render(ctx, mx, my, delta);
                } catch (Throwable ignored) {
                    // zostaje zwykły ekran gry
                }
            });
        });

        // Parametry różnią się między wersjami (float / RenderTickCounter) – lambda pasuje do obu.
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            MinecraftClient c = MinecraftClient.getInstance();
            VoidCore.renderHud(HUD_GFX.set(ctx, c.textRenderer), c.getWindow().getScaledWidth(), c.getWindow().getScaledHeight());
        });
    }
}
