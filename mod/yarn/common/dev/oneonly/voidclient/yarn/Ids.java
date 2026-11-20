package dev.oneonly.voidclient.yarn;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.UUID;

/** UUID gracza po numerze encji (stan renderu od 1.21.2 zna tylko numer). */
public final class Ids {
    private Ids() {}

    public static UUID uuid(int id) {
        var w = MinecraftClient.getInstance().world;
        if (w == null) return null;
        Entity e = w.getEntityById(id);
        return e == null ? null : e.getUuid();
    }
}
