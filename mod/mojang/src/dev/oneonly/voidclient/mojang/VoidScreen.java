package dev.oneonly.voidclient.mojang;

import dev.oneonly.voidclient.core.gui.GuiController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Ekran menu dla 26.1+ – przekazuje rysowanie i wejście do wspólnego GuiController. */
public class VoidScreen extends Screen {
    private final GuiController gui = new GuiController();
    private final MojangGfx gfx = new MojangGfx();

    public VoidScreen() {
        super(Component.literal("Void Client"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        gui.render(gfx.set(graphics, font), mouseX, mouseY, width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return gui.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return gui.mouseDragged(event.x(), event.y()) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return gui.mouseReleased() || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        return gui.mouseScrolled(vertical) || super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (gui.keyPressed(event.key(), event.modifiers())) return true;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && gui.escape()) return true;
        if (VoidClientMod.guiKey.matches(event)) {
            gui.requestClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return gui.charTyped(event.codepoint()) || super.charTyped(event);
    }

    @Override
    public void tick() {
        if (gui.closeFinished()) onClose(); // po animacji zamykania
    }

    @Override
    public void removed() {
        gui.closed();
        super.removed();
    }
}
