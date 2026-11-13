package dev.oneonly.voidclient.yarn;

import dev.oneonly.voidclient.core.Game;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.nio.file.Path;

/** Dostęp do gry dla wersji 1.20.1 – 1.21.11 (mapowania Yarn). */
public final class YarnGame implements Game {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

    private MinecraftClient mc() { return MinecraftClient.getInstance(); }

    @Override public int fps() { return mc().getCurrentFps(); }

    @Override
    public int ping() {
        MinecraftClient mc = mc();
        ClientPlayNetworkHandler h = mc.getNetworkHandler();
        if (h == null || mc.player == null) return -1;
        PlayerListEntry e = h.getPlayerListEntry(mc.player.getUuid());
        return e == null ? -1 : e.getLatency();
    }

    @Override public boolean hasPlayer() { return mc().player != null; }

    @Override public int x() { return mc().player.getBlockX(); }

    @Override public int y() { return mc().player.getBlockY(); }

    @Override public int z() { return mc().player.getBlockZ(); }

    @Override
    public String facing() {
        switch (mc().player.getHorizontalFacing()) {
            case NORTH: return "N";
            case SOUTH: return "S";
            case EAST: return "E";
            case WEST: return "W";
            default: return "";
        }
    }

    private KeyBinding binding(Key k) {
        var o = mc().options;
        switch (k) {
            case FORWARD: return o.forwardKey;
            case LEFT: return o.leftKey;
            case BACK: return o.backKey;
            case RIGHT: return o.rightKey;
            case JUMP: return o.jumpKey;
            case ATTACK: return o.attackKey;
            default: return o.useKey;
        }
    }

    @Override public boolean key(Key k) { return binding(k).isPressed(); }

    @Override public boolean leftMouse() { return mc().options.attackKey.isPressed(); }

    @Override public boolean rightMouse() { return mc().options.useKey.isPressed(); }

    @Override
    public Slot slot(int index) {
        ClientPlayerEntity p = mc().player;
        if (p == null) return Slot.EMPTY;
        ItemStack s = p.getEquippedStack(SLOTS[index]);
        if (s.isEmpty()) return Slot.EMPTY;
        return new Slot(s, false, s.isDamageable(), s.getMaxDamage() - s.getDamage(), s.getMaxDamage(), s.getCount());
    }

    @Override
    public boolean canSprint() {
        ClientPlayerEntity p = mc().player;
        return p != null && mc().options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision && !p.isUsingItem();
    }

    @Override public void sprint() { if (mc().player != null) mc().player.setSprinting(true); }

    @Override public int fov() { return mc().options.getFov().getValue(); }

    @Override public void setFov(int value) {
        var opt = mc().options.getFov();
        if (value >= 30 && value <= 110) { opt.setValue(value); return; }
        try {
            forceOption(opt, opt.getValue(), value);
        } catch (Throwable t) {
            opt.setValue(Math.max(30, Math.min(110, value)));
        }
    }

    /** Pole z wartością opcji FOV – gra pozwala ustawić 30–110, a zoom potrzebuje mniej. */
    private static java.lang.reflect.Field fovField;

    private static void forceOption(Object opt, Object current, int value) throws Exception {
        if (fovField == null) {
            for (Class<?> c = opt.getClass(); c != null && fovField == null; c = c.getSuperclass()) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType() != Object.class) continue;
                    f.setAccessible(true);
                    if (f.get(opt) == current) { fovField = f; break; }
                }
            }
        }
        if (fovField == null) throw new IllegalStateException("brak pola FOV");
        fovField.set(opt, Integer.valueOf(value));
    }

    @Override public boolean smoothCamera() { return mc().options.smoothCameraEnabled; }

    @Override public void setSmoothCamera(boolean value) { mc().options.smoothCameraEnabled = value; }

    @Override public Object gammaOption() { return mc().options.getGamma(); }

    @Override public double gamma() { return mc().options.getGamma().getValue(); }

    @Override public void setGamma(double value) { mc().options.getGamma().setValue(value); }

    @Override public boolean screenOpen() { return mc().currentScreen != null; }

    @Override public boolean hudHidden() { return mc().options.hudHidden; }

    @Override public Path configDir() { return FabricLoader.getInstance().getConfigDir(); }

    @Override public String accessToken() { return mc().getSession().getAccessToken(); }

    @Override
    public void sendChat(String text) {
        ClientPlayerEntity p = mc().player;
        if (p == null || p.networkHandler == null) return;
        if (text.startsWith("/")) p.networkHandler.sendChatCommand(text.substring(1));
        else p.networkHandler.sendChatMessage(text);
    }

    @Override
    public String autoTextKey(int slot) {
        KeyBinding k = VoidClientMod.autoKeys[slot];
        return k == null || k.isUnbound() ? "-" : k.getBoundKeyLocalizedText().getString();
    }

    @Override public String clipboard() { return mc().keyboard.getClipboard(); }

    @Override
    public void applyFpsPreset() {
        var o = mc().options;
        // Enumy bez importów – nazwy klas różnią się między wersjami, kolejność wartości nie.
        var particles = o.getParticles();
        var pModes = particles.getValue().getDeclaringClass().getEnumConstants();
        particles.setValue(pModes[pModes.length - 1]);          // MINIMAL
        var clouds = o.getCloudRenderMode();
        var cModes = clouds.getValue().getDeclaringClass().getEnumConstants();
        clouds.setValue(cModes[0]);                             // OFF
        o.getEntityShadows().setValue(false);
        o.getBiomeBlendRadius().setValue(0);
        o.getEnableVsync().setValue(false);
        o.getMaxFps().setValue(260);                             // bez limitu
        o.getEntityDistanceScaling().setValue(0.75);
        o.write();
    }

    @Override public float playerYaw() { return mc().player.getYaw(); }

    @Override public float playerPitch() { return mc().player.getPitch(); }

    @Override public int perspective() { return mc().options.getPerspective().ordinal(); }

    @Override public void setPerspective(int index) { mc().options.setPerspective(Perspective.values()[index]); }

    @Override
    public String server() {
        MinecraftClient mc = mc();
        if (mc.world == null) return "";
        if (mc.isIntegratedServerRunning()) return "sp";
        net.minecraft.client.network.ServerInfo info = mc.getCurrentServerEntry();
        return info == null ? "mp" : info.address;
    }
    @Override
    public double[] eye() {
        var p = mc().player;
        return p == null ? new double[3] : new double[]{p.getX(), p.getEyeY(), p.getZ()};
    }

    @Override
    public String dimension() {
        return mc().world == null ? "" : String.valueOf(mc().world.getRegistryKey());
    }

    // ───────────── nowe: HUD, niebo, czat, shulker ─────────────

    @Override
    public java.util.List<Effect> effects() {
        var p = mc().player;
        java.util.List<Effect> out = new java.util.ArrayList<>();
        if (p == null) return out;
        for (net.minecraft.entity.effect.StatusEffectInstance e : p.getStatusEffects()) {
            String name = net.minecraft.text.Text.translatable(e.getTranslationKey()).getString();
            out.add(new Effect(name, e.getAmplifier(), e.getDuration(), e.isInfinite()));
        }
        out.sort(java.util.Comparator.comparingInt(Effect::ticks));
        return out;
    }

    @Override
    public Slot countItems(java.util.function.Predicate<String> match, boolean blocks) {
        var p = mc().player;
        if (p == null) return Slot.EMPTY;
        var inv = p.getInventory();
        ItemStack first = null;
        int total = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            boolean ok;
            if (blocks) ok = s.getItem() instanceof net.minecraft.item.BlockItem;
            else ok = match.test(net.minecraft.registry.Registries.ITEM.getId(s.getItem()) + "|" + s.getName().getString());
            if (!ok) continue;
            total += s.getCount();
            if (first == null) first = s;
        }
        return first == null ? Slot.EMPTY : new Slot(first, false, false, 0, 0, total);
    }

    @Override
    public String biome() {
        var m = mc();
        if (m.world == null || m.player == null) return "";
        return m.world.getBiome(m.player.getBlockPos()).getKey()
                .map(k -> net.minecraft.text.Text.translatable("biome." + k.getValue().getNamespace() + "." + k.getValue().getPath()).getString())
                .orElse("");
    }

    @Override public Path gameDir() { return FabricLoader.getInstance().getGameDir(); }

    @Override public boolean modLoaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }

    @Override
    public void reloadPack(String packId, boolean enabled) {
        MinecraftClient m = mc();
        var mgr = m.getResourcePackManager();
        mgr.scanPacks();
        boolean changed = enabled ? mgr.enable(packId) : mgr.disable(packId);
        if (changed) m.options.refreshResourcePacks(mgr);
        else m.reloadResources();
    }

    @Override
    public void chatInfo(String text) {
        MinecraftClient m = mc();
        if (m.inGameHud != null) m.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal(text));
    }

    @Override
    public java.util.List<Object> containerItems(Object stack) {
        java.util.List<Object> out = new java.util.ArrayList<>();
        for (ItemStack s : Contents.of((ItemStack) stack)) out.add(s.isEmpty() ? null : s);
        return out;
    }

    @Override
    public boolean outlineSupported() {
        // 1.20 – 1.21.8: kolor podajemy przy rysowaniu ramki; od 1.21.9 ramka idzie innym torem
        String v = FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
        return !(v.startsWith("1.21.9") || v.startsWith("1.21.10") || v.startsWith("1.21.11"));
    }

    @Override
    public java.util.List<java.util.UUID> tabUuids() {
        ClientPlayNetworkHandler h = mc().getNetworkHandler();
        return h == null ? java.util.List.of() : new java.util.ArrayList<>(h.getPlayerUuids());
    }

    @Override
    public int armorPoints() {
        ClientPlayerEntity p = mc().player;
        return p == null ? 0 : p.getArmor();
    }

    @Override
    public float attackCooldown() {
        ClientPlayerEntity p = mc().player;
        return p == null ? 0f : p.getAttackCooldownProgress(0.5f);
    }

    @Override
    public boolean critReady() {
        ClientPlayerEntity p = mc().player;
        return p != null && !p.isOnGround() && p.getVelocity().y < 0 && !p.isClimbing()
                && !p.isTouchingWater() && !p.hasVehicle() && !p.isSprinting();
    }

    @Override public boolean scoreboardMovable() { return true; }

    @Override
    public Object invStack(int index) {
        var p = mc().player;
        if (p == null) return null;
        ItemStack s = p.getInventory().getStack(index);
        return s.isEmpty() ? null : s;
    }
}
