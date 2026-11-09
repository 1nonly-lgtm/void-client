package dev.oneonly.voidclient.mojang;

import dev.oneonly.voidclient.core.Game;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;

/** Dostęp do gry dla 26.1+ (oficjalne nazwy Mojang). */
public final class MojangGame implements Game {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

    private Minecraft mc() { return Minecraft.getInstance(); }

    @Override public int fps() { return mc().getFps(); }

    @Override
    public int ping() {
        Minecraft mc = mc();
        ClientPacketListener c = mc.getConnection();
        if (c == null || mc.player == null) return -1;
        PlayerInfo info = c.getPlayerInfo(mc.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    @Override public boolean hasPlayer() { return mc().player != null; }

    @Override public int x() { return mc().player.blockPosition().getX(); }

    @Override public int y() { return mc().player.blockPosition().getY(); }

    @Override public int z() { return mc().player.blockPosition().getZ(); }

    @Override
    public String facing() {
        switch (mc().player.getDirection()) {
            case NORTH: return "N";
            case SOUTH: return "S";
            case EAST: return "E";
            case WEST: return "W";
            default: return "";
        }
    }

    private KeyMapping mapping(Key k) {
        var o = mc().options;
        switch (k) {
            case FORWARD: return o.keyUp;
            case LEFT: return o.keyLeft;
            case BACK: return o.keyDown;
            case RIGHT: return o.keyRight;
            case JUMP: return o.keyJump;
            case ATTACK: return o.keyAttack;
            default: return o.keyUse;
        }
    }

    @Override public boolean key(Key k) { return mapping(k).isDown(); }

    @Override public boolean leftMouse() { return mc().options.keyAttack.isDown(); }

    @Override public boolean rightMouse() { return mc().options.keyUse.isDown(); }

    @Override
    public Slot slot(int index) {
        LocalPlayer p = mc().player;
        if (p == null) return Slot.EMPTY;
        ItemStack s = p.getItemBySlot(SLOTS[index]);
        if (s.isEmpty()) return Slot.EMPTY;
        return new Slot(s, false, s.isDamageableItem(), s.getMaxDamage() - s.getDamageValue(), s.getMaxDamage(), s.getCount());
    }

    @Override
    public boolean canSprint() {
        LocalPlayer p = mc().player;
        return p != null && mc().options.keyUp.isDown() && !p.isShiftKeyDown() && !p.horizontalCollision && !p.isUsingItem();
    }

    @Override public void sprint() { if (mc().player != null) mc().player.setSprinting(true); }

    @Override public int fov() { return mc().options.fov().get(); }

    @Override public void setFov(int value) { mc().options.fov().set(value); }

    @Override public boolean smoothCamera() { return mc().options.smoothCamera; }

    @Override public void setSmoothCamera(boolean value) { mc().options.smoothCamera = value; }

    @Override public Object gammaOption() { return mc().options.gamma(); }

    @Override public double gamma() { return mc().options.gamma().get(); }

    @Override public void setGamma(double value) { mc().options.gamma().set(value); }

    @Override public boolean screenOpen() { return mc().screen != null; }

    @Override public boolean hudHidden() { return mc().options.hideGui; }

    @Override public Path configDir() { return FabricLoader.getInstance().getConfigDir(); }

    @Override public String accessToken() { return mc().getUser().getAccessToken(); }

    @Override
    public void sendChat(String text) {
        LocalPlayer p = mc().player;
        if (p == null || p.connection == null) return;
        if (text.startsWith("/")) p.connection.sendCommand(text.substring(1));
        else p.connection.sendChat(text);
    }

    @Override
    public String autoTextKey(int slot) {
        KeyMapping k = VoidClientMod.autoKeys[slot];
        return k == null || k.isUnbound() ? "-" : k.getTranslatedKeyMessage().getString();
    }

    @Override public String clipboard() { return mc().keyboardHandler.getClipboard(); }

    @Override
    public void applyFpsPreset() {
        var o = mc().options;
        // Enumy bez importów – nazwy klas różnią się między wersjami, kolejność wartości nie.
        var particles = o.particles();
        var pModes = particles.get().getDeclaringClass().getEnumConstants();
        particles.set(pModes[pModes.length - 1]);          // MINIMAL
        var clouds = o.cloudStatus();
        var cModes = clouds.get().getDeclaringClass().getEnumConstants();
        clouds.set(cModes[0]);                             // OFF
        o.entityShadows().set(false);
        o.biomeBlendRadius().set(0);
        o.enableVsync().set(false);
        o.framerateLimit().set(260);                             // bez limitu
        o.entityDistanceScaling().set(0.75);
        o.save();
    }

    @Override public float playerYaw() { return mc().player.getYRot(); }

    @Override public float playerPitch() { return mc().player.getXRot(); }

    @Override public int perspective() { return mc().options.getCameraType().ordinal(); }

    @Override public void setPerspective(int index) { mc().options.setCameraType(CameraType.values()[index]); }
}
