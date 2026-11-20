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
    private static volatile String ownCape;
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
        if (c == null) return null;
        return c + "_" + (int) ((System.currentTimeMillis() / 250) % FRAMES);
    }
}
