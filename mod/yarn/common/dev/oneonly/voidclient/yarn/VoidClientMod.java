package dev.oneonly.voidclient.yarn;

import dev.oneonly.voidclient.core.AutoText;
import dev.oneonly.voidclient.core.VoidCore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class VoidClientMod implements ClientModInitializer {
    public static KeyBinding guiKey;
    public static KeyBinding zoomKey;
    public static KeyBinding freelookKey;
    public static final KeyBinding[] autoKeys = new KeyBinding[AutoText.SLOTS];
    private static final YarnGfx HUD_GFX = new YarnGfx();

    @Override
    public void onInitializeClient() {
        VoidCore.init(new YarnGame());

        guiKey = KeyBindingHelper.registerKeyBinding(Keys.create("key.voidclient.gui", GLFW.GLFW_KEY_RIGHT_SHIFT));
        zoomKey = KeyBindingHelper.registerKeyBinding(Keys.create("key.voidclient.zoom", GLFW.GLFW_KEY_C));
        freelookKey = KeyBindingHelper.registerKeyBinding(Keys.create("key.voidclient.freelook", GLFW.GLFW_KEY_LEFT_ALT));
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
            VoidCore.tick(zoomKey.isPressed(), freelookKey.isPressed());
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> VoidCore.shutdown());

        // Parametry różnią się między wersjami (float / RenderTickCounter) – lambda pasuje do obu.
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            MinecraftClient c = MinecraftClient.getInstance();
            VoidCore.renderHud(HUD_GFX.set(ctx, c.textRenderer), c.getWindow().getScaledWidth(), c.getWindow().getScaledHeight());
        });
    }
}
