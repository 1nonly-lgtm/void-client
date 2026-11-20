package dev.oneonly.voidclient.yarn;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Warstwy renderu kosmetyków (1.21.10). */
final class Layers {
    private static final Map<String, RenderLayer> CACHE = new HashMap<>();

    static RenderLayer of(String tex, int mode) {
        return CACHE.computeIfAbsent(tex + mode, k -> {
            Identifier id = Identifier.tryParse(tex);
            return mode == 2 ? RenderLayer.getEyes(id) : mode == 1 ? RenderLayer.getEntityTranslucentEmissive(id) : RenderLayer.getEntityTranslucent(id);
        });
    }
}
