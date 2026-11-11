package dev.oneonly.voidclient.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.oneonly.voidclient.core.module.Modules;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ikonka Void przy nicku: co 30 s pytamy serwer Void, którzy gracze z listy (Tab) używają Void Client.
 * Co 90 s wysyłamy „gram” (znajomi widzą to w launcherze). Wszystko w tle – gra się nie zacina.
 */
public final class VoidNet {
    private static final String URL = "https://ogapyiwjrplfdztjmbbq.supabase.co/rest/v1/rpc/";
    private static final String KEY = "sb_publishable_auXkcWYA2zmqOGEOw7APrw_LJApnFl6";
    /** Znak z czcionki Void Client (assets/minecraft/font/default.json) – logo Void. */
    public static final String ICON = "";

    private static final Set<UUID> VOID = ConcurrentHashMap.newKeySet();
    private static HttpClient http;
    private static volatile boolean busy;
    private static long nextUsers;
    private static long nextBeat;

    private VoidNet() {}

    public static boolean isVoid(UUID id) {
        return id != null && Modules.VOID_ICON.isEnabled() && VOID.contains(id);
    }

    private static Method profileId;

    /** UUID z GameProfile – getId() w starszych wersjach, id() od 1.21.9 (authlib nie jest zaciemniony). */
    public static UUID idOf(Object profile) {
        if (profile == null) return null;
        try {
            if (profileId == null) {
                try { profileId = profile.getClass().getMethod("getId"); }
                catch (NoSuchMethodException e) { profileId = profile.getClass().getMethod("id"); }
            }
            return (UUID) profileId.invoke(profile);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void tick(Game g) {
        if (busy || !g.hasPlayer()) return;
        long now = System.currentTimeMillis();
        if (now >= nextUsers && Modules.VOID_ICON.isEnabled()) {
            nextUsers = now + 30_000;
            List<UUID> ids;
            try { ids = g.tabUuids(); } catch (Throwable t) { return; }
            run(() -> users(ids));
        } else if (now >= nextBeat) {
            nextBeat = now + 90_000;
            String token;
            try { token = g.accessToken(); } catch (Throwable t) { return; }
            run(() -> post("void_heartbeat", "{\"p_token\":\"" + esc(token) + "\",\"p_playing\":true}"));
        }
    }

    private static void run(Runnable r) {
        busy = true;
        Thread t = new Thread(() -> {
            try { r.run(); } catch (Throwable ignored) { /* brak sieci – spróbujemy później */ }
            finally { busy = false; }
        }, "VoidNet");
        t.setDaemon(true);
        t.start();
    }

    private static void users(List<UUID> ids) {
        if (ids.isEmpty()) return;
        JsonArray arr = new JsonArray();
        for (int i = 0; i < ids.size() && i < 200; i++) arr.add(ids.get(i).toString());
        JsonObject body = new JsonObject();
        body.add("p_uuids", arr);
        String res = post("void_users", body.toString());
        if (res == null) return;
        Set<UUID> found = ConcurrentHashMap.newKeySet();
        for (var e : JsonParser.parseString(res).getAsJsonArray()) {
            try { found.add(UUID.fromString(e.getAsString())); } catch (Exception ignored) {}
        }
        VOID.retainAll(found);
        VOID.addAll(found);
    }

    private static String post(String fn, String json) {
        try {
            if (http == null) http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(URL + fn))
                    .timeout(Duration.ofSeconds(12))
                    .header("apikey", KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            return r.statusCode() == 200 ? r.body() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
