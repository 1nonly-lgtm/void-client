package dev.oneonly.voidclient.yarn;

import dev.oneonly.voidclient.core.gui.GuiController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

/** Ekran menu – przekazuje rysowanie i wejście do wspólnego GuiController. (1.21.9 – 1.21.11) */
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
    public boolean mouseClicked(Click click, boolean doubled) {
        return gui.mouseClicked(click.x(), click.y(), click.button()) || super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        return gui.mouseDragged(click.x(), click.y()) || super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        return gui.mouseReleased() || super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        return gui.mouseScrolled(vertical) || super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (gui.keyPressed(input.getKeycode(), input.modifiers())) return true;
        if (input.getKeycode() == GLFW.GLFW_KEY_ESCAPE && gui.escape()) return true;
        if (VoidClientMod.guiKey.matchesKey(input)) {
            gui.requestClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        return gui.charTyped(input.codepoint()) || super.charTyped(input);
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
