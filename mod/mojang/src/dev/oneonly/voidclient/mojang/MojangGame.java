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

    @Override
    public String server() {
        Minecraft mc = mc();
        if (mc.level == null) return "";
        if (mc.hasSingleplayerServer()) return "sp";
        net.minecraft.client.multiplayer.ServerData info = mc.getCurrentServer();
        return info == null ? "mp" : info.ip;
    }
    @Override
    public double[] eye() {
        var p = mc().player;
        return p == null ? new double[3] : new double[]{p.getX(), p.getEyeY(), p.getZ()};
    }

    @Override
    public String dimension() {
        return mc().level == null ? "" : String.valueOf(mc().level.dimension());
    }

    // ───────────── nowe: HUD, niebo, czat, shulker ─────────────

    @Override
    public java.util.List<Effect> effects() {
        LocalPlayer p = mc().player;
        java.util.List<Effect> out = new java.util.ArrayList<>();
        if (p == null) return out;
        for (net.minecraft.world.effect.MobEffectInstance e : p.getActiveEffects()) {
            String name = net.minecraft.network.chat.Component.translatable(e.getDescriptionId()).getString();
            out.add(new Effect(name, e.getAmplifier(), e.getDuration(), e.isInfiniteDuration()));
        }
        out.sort(java.util.Comparator.comparingInt(Effect::ticks));
        return out;
    }

    @Override
    public Slot countItems(java.util.function.Predicate<String> match, boolean blocks) {
        LocalPlayer p = mc().player;
        if (p == null) return Slot.EMPTY;
        var inv = p.getInventory();
        ItemStack first = null;
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            boolean ok;
            if (blocks) ok = s.getItem() instanceof net.minecraft.world.item.BlockItem;
            else ok = match.test(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()) + "|" + s.getHoverName().getString());
            if (!ok) continue;
            total += s.getCount();
            if (first == null) first = s;
        }
        return first == null ? Slot.EMPTY : new Slot(first, false, false, 0, 0, total);
    }

    @Override
    public String biome() {
        Minecraft m = mc();
        if (m.level == null || m.player == null) return "";
        // "ResourceKey[minecraft:worldgen/biome / minecraft:plains]" → biome.minecraft.plains
        String k = m.level.getBiome(m.player.blockPosition()).unwrapKey().map(String::valueOf).orElse("");
        int i = k.lastIndexOf(" / ");
        if (i < 0) return "";
        String id = k.substring(i + 3).replace("]", "").trim();
        return net.minecraft.network.chat.Component.translatable("biome." + id.replace(':', '.')).getString();
    }

    @Override public Path gameDir() { return FabricLoader.getInstance().getGameDir(); }

    @Override public boolean modLoaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }

    @Override
    public void reloadPack(String packId, boolean enabled) {
        Minecraft m = mc();
        var repo = m.getResourcePackRepository();
        repo.reload();
        boolean changed = enabled ? repo.addPack(packId) : repo.removePack(packId);
        if (changed) m.options.updateResourcePacks(repo);
        else m.reloadResourcePacks();
    }

    @Override
    public void chatInfo(String text) {
        Minecraft m = mc();
        if (m.gui == null) return;
        // sygnatura addMessage zmienia się między wersjami – szukamy metody, której 1. parametr to Component
        Object chat = m.gui.getChat();
        net.minecraft.network.chat.Component c = net.minecraft.network.chat.Component.literal(text);
        for (java.lang.reflect.Method mt : chat.getClass().getMethods()) {
            Class<?>[] p = mt.getParameterTypes();
            if (!mt.getName().equals("addMessage") || p.length == 0 || p[0] != net.minecraft.network.chat.Component.class) continue;
            boolean prim = false;
            for (int i = 1; i < p.length; i++) prim |= p[i].isPrimitive();
            if (prim) continue;
            Object[] args = new Object[p.length];
            args[0] = c;
            try { mt.invoke(chat, args); } catch (ReflectiveOperationException ignored) {}
            return;
        }
    }

    @Override
    public java.util.List<Object> containerItems(Object stack) {
        java.util.List<Object> out = new java.util.ArrayList<>();
        var c = ((ItemStack) stack).get(net.minecraft.core.component.DataComponents.CONTAINER);
        if (c == null) return out;
        net.minecraft.core.NonNullList<ItemStack> list = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        c.copyInto(list);
        for (ItemStack s : list) out.add(s.isEmpty() ? null : s);
        return out;
    }

    @Override public boolean outlineSupported() { return false; }
}
