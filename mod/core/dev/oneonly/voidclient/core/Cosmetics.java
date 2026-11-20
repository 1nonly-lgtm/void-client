package dev.oneonly.voidclient.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Kosmetyki Void Client w grze. Swoje bierzemy z config/void-cosmetics.json (zapisuje je launcher przy starcie),
 * innych graczy – z serwera Void (VoidNet, void_cosmetics_get). Na razie w grze: animowane peleryny.
 */
public final class Cosmetics {
    private static final Pattern ID = Pattern.compile("[a-z]{2,24}");
    private static final Map<UUID, String> CAPES = new ConcurrentHashMap<>();
    /** Kategorie 3D w kolejności tablic worn(): skrzydła, czapka, zwierzak, efekt. */
    public static final String[] CATS3D = {"wings", "hat", "pet", "fx"};
    private static final Map<UUID, String[]> WORN = new ConcurrentHashMap<>();
    private static volatile String ownCape;
    private static volatile String[] ownWorn = new String[4];
    /** Twoje pola koła emotek (24 = 3 strony po 8). */
    public static volatile String[] slots = defaultSlots();
    private static volatile boolean ownLoaded;
    public static final int FRAMES = 12;

    private Cosmetics() {}

    public static void loadOwn(Game g) {
        ownLoaded = true;
        try {
            Path f = g.gameDir().resolve("config").resolve("void-cosmetics.json");
            if (!Files.exists(f)) return;
            JsonObject o = JsonParser.parseString(new String(Files.readAllBytes(f), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject c = o.has("cosmetics") && o.get("cosmetics").isJsonObject() ? o.getAsJsonObject("cosmetics") : null;
            ownCape = c == null ? null : valid(c.get("cape"));
            String[] w = new String[4];
            for (int i = 0; i < 4; i++) w[i] = c == null ? null : valid(c.get(CATS3D[i]));
            ownWorn = w;
            if (o.has("emoteSlots") && o.get("emoteSlots").isJsonArray() && o.getAsJsonArray("emoteSlots").size() == 24) {
                String[] sl = new String[24];
                for (int i = 0; i < 24; i++) sl[i] = valid(o.getAsJsonArray("emoteSlots").get(i));
                slots = sl;
            }
        } catch (Throwable t) {
            ownCape = null;
        }
    }

    private static String valid(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        String s = e.getAsString();
        return ID.matcher(s).matches() ? s : null;
    }

    /** Dane z serwera: {cape, wings, hat, pet, fx, emoteSlots}. */
    public static void setRemote(UUID id, JsonObject data) {
        String cape = data == null ? null : valid(data.get("cape"));
        if (cape == null) CAPES.remove(id); else CAPES.put(id, cape);
        String[] w = new String[4];
        boolean any = false;
        for (int i = 0; i < 4; i++) { w[i] = data == null ? null : valid(data.get(CATS3D[i])); any |= w[i] != null; }
        if (any) WORN.put(id, w); else WORN.remove(id);
    }

    /** Założone kosmetyki 3D gracza (CATS3D) albo null. */
    public static String[] worn(UUID id) {
        if (id == null) return null;
        try {
            if (id.equals(VoidCore.game().selfUuid())) {
                if (!ownLoaded) loadOwn(VoidCore.game());
                return ownWorn;
            }
        } catch (Throwable ignored) {
        }
        return WORN.get(id);
    }

    public static String ownCape() { return ownCape; }

    public static String ownWorn(int i) { return ownWorn[i]; }

    /** Zmiana w grze (menu → Wygląd → Kosmetyki): zapis do pliku i na konto. */
    public static void setOwn(String cat, String id) {
        if (!ownLoaded) loadOwn(VoidCore.game());
        if ("cape".equals(cat)) ownCape = id;
        else for (int i = 0; i < 4; i++) if (CATS3D[i].equals(cat)) { String[] w = ownWorn.clone(); w[i] = id; ownWorn = w; }
        saveOwn();
    }

    /** Pole koła emotek (0–23) – zapis do pliku i na konto. */
    public static void setSlot(int i, String id) {
        if (!ownLoaded) loadOwn(VoidCore.game());
        String[] sl = slots.clone();
        sl[i] = id;
        slots = sl;
        saveOwn();
    }

    private static String[] defaultSlots() {
        String[] d = {"machanie", "taniec", "dab", "salto", "klaskanie", "facepalm", "gitara", "uklon",
                "robot", "krecio", "pajacyki", "medytacja", "salut", "pompki", "zombie", "hura",
                "boks", "karate", "moonwalk", "headbang", "serce", "samolot", "zwyciestwo", "hulajnoga"};
        return d;
    }

    static JsonObject ownJson() {
        JsonObject c = new JsonObject();
        c.addProperty("cape", ownCape);
        for (int i = 0; i < 4; i++) c.addProperty(CATS3D[i], ownWorn[i]);
        return c;
    }

    private static void saveOwn() {
        Game g = VoidCore.game();
        try {
            JsonObject o = new JsonObject();
            o.add("cosmetics", ownJson());
            com.google.gson.JsonArray sl = new com.google.gson.JsonArray();
            for (String s : slots) sl.add(s);
            o.add("emoteSlots", sl);
            o.addProperty("changedInGame", System.currentTimeMillis());
            Path dir = g.gameDir().resolve("config");
            Files.createDirectories(dir);
            Files.write(dir.resolve("void-cosmetics.json"), o.toString().getBytes(StandardCharsets.UTF_8));
            Files.write(dir.resolve("void-cosmetics-game.json"), o.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
        VoidNet.saveCosmetics(g, ownJson(), slots);
    }

    public static String cape(UUID id) {
        if (id == null) return null;
        Game g = VoidCore.game();
        try {
            if (id.equals(g.selfUuid())) {
                if (!ownLoaded) loadOwn(g);
                return ownCape;
            }
        } catch (Throwable ignored) {
        }
        return CAPES.get(id);
    }

    /** Nazwa klatki animowanej peleryny (np. "zorza_7") albo null, gdy gracz nie ma peleryny Void. */
    public static String capeFrame(UUID id) {
        String c = cape(id);
        if (c == null || !ID.matcher(c).matches()) return null;
        return c + "_" + (int) ((System.currentTimeMillis() / 250) % FRAMES);
    }
}
