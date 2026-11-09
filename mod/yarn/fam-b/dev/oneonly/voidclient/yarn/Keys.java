package dev.oneonly.voidclient.yarn;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/** Klawisze – kategoria jako tekst (do 1.21.8). */
final class Keys {
    private Keys() {}

    static KeyBinding create(String name, int key) {
        return new KeyBinding(name, InputUtil.Type.KEYSYM, key, "category.voidclient");
    }
}
