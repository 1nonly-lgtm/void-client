package dev.oneonly.voidclient.yarn;

import dev.oneonly.voidclient.core.gui.GuiController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Ekran menu – przekazuje rysowanie i wejście do wspólnego GuiController. (1.20.2 – 1.21.8) */
public class VoidScreen extends Screen {
    private final GuiController gui = new GuiController();
    private final YarnGfx gfx = new YarnGfx();

    public VoidScreen() {
        super(Text.literal("Void Client"));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        gui.render(gfx.set(ctx, textRenderer), mouseX, mouseY, width, height);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return gui.mouseClicked(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return gui.mouseDragged(mx, my) || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return gui.mouseReleased() || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        return gui.mouseScrolled(vertical) || super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (gui.keyPressed(keyCode, modifiers)) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && gui.escape()) return true;
        if (VoidClientMod.guiKey.matchesKey(keyCode, scanCode)) {
            gui.requestClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return gui.charTyped(chr) || super.charTyped(chr, modifiers);
    }

    @Override
    public void tick() {
        if (gui.closeFinished()) close(); // po animacji zamykania
    }

    @Override
    public void removed() {
        gui.closed();
        super.removed();
    }
}
