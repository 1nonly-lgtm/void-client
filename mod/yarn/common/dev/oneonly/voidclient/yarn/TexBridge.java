package dev.oneonly.voidclient.yarn;

import net.minecraft.client.gui.DrawContext;

/** Publiczny dostęp do przesuwania macierzy z Tex (Tex jest pakietowy, a mixin leży w innym pakiecie). */
public final class TexBridge {
    private TexBridge() {}

    public static void push(DrawContext ctx, int dx, int dy) { Tex.push(ctx, dx, dy); }

    public static void pop(DrawContext ctx) { Tex.pop(ctx); }
}
