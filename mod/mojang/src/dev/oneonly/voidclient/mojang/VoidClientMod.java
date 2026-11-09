package dev.oneonly.voidclient.mojang;

import com.mojang.blaze3d.platform.InputConstants;
import dev.oneonly.voidclient.core.AutoText;
import dev.oneonly.voidclient.core.VoidCore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class VoidClientMod implements ClientModInitializer {
    public static KeyMapping guiKey;
    public static KeyMapping zoomKey;
    public static KeyMapping freelookKey;
    public static final KeyMapping[] autoKeys = new KeyMapping[AutoText.SLOTS];
    private static final MojangGfx HUD_GFX = new MojangGfx();

    @Override
    public void onInitializeClient() {
        VoidCore.init(new MojangGame());

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("voidclient", "main"));
        guiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.voidclient.gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        zoomKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.voidclient.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, category));
        freelookKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.voidclient.freelook", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, category));
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
            VoidCore.tick(zoomKey.isDown(), freelookKey.isDown());
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> VoidCore.shutdown());

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("voidclient", "hud"), (graphics, delta) -> {
            Minecraft c = Minecraft.getInstance();
            VoidCore.renderHud(HUD_GFX.set(graphics, c.font), c.getWindow().getGuiScaledWidth(), c.getWindow().getGuiScaledHeight());
        });
    }
}
