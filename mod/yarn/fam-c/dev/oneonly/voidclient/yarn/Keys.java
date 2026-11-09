package dev.oneonly.voidclient.yarn;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

/** Klawisze – kategoria jako obiekt (1.21.9+). */
final class Keys {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("voidclient", "main"));

    private Keys() {}

    static KeyBinding create(String name, int key) {
        return new KeyBinding(name, InputUtil.Type.KEYSYM, key, CATEGORY);
    }
}
