package dev.oneonly.voidclient.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.oneonly.voidclient.core.module.Modules;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Co teraz gra (Spotify albo inny odtwarzacz w Windowsie). Dane daje launcher Void Client
 * na http://127.0.0.1:47823 – bez logowania do Spotify i bez Premium.
 */
public final class Spotify {
    private static final String BASE = "http://127.0.0.1:47823";

    public static volatile boolean present;
    public static volatile String title = "", artist = "";
    public static volatile boolean playing;
    private static volatile double pos, dur;
    private static volatile long stamp;
    /** Okładka 32×32 (RGB) albo null. */
    public static volatile int[] cover;
    private static volatile int coverId = -1;
    private static Thread thread;

    private Spotify() {}

    public static synchronized void ensure() {
        if (thread != null) return;
        thread = new Thread(Spotify::loop, "VoidClient-Spotify");
        thread.setDaemon(true);
        thread.start();
    }

    private static void loop() {
        while (true) {
            try {
                if (Modules.SPOTIFY.isEnabled()) poll();
                else present = false;
            } catch (Throwable t) {
                present = false;
            }
            try { Thread.sleep(present ? 1000 : 3000); } catch (InterruptedException e) { return; }
        }
    }

    private static String get(String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(BASE + path).openConnection();
        c.setConnectTimeout(400);
        c.setReadTimeout(1500);
        try (InputStream in = c.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }

    private static void poll() throws Exception {
        JsonObject o = JsonParser.parseString(get("/now")).getAsJsonObject();
        if (o.has("none") || !o.has("title")) { present = false; return; }
        title = str(o, "title");
        artist = str(o, "artist");
        playing = o.has("playing") && o.get("playing").getAsBoolean();
        pos = o.has("pos") ? o.get("pos").getAsDouble() : 0;
        dur = o.has("dur") ? o.get("dur").getAsDouble() : 0;
        stamp = System.currentTimeMillis();
        present = !title.isEmpty();
        int id = o.has("coverId") ? o.get("coverId").getAsInt() : -1;
        if (id != coverId) {
            JsonObject c = JsonParser.parseString(get("/cover")).getAsJsonObject();
            String hex = str(c, "rgb");
            if (hex.length() == 32 * 32 * 6) {
                int[] px = new int[32 * 32];
                for (int i = 0; i < px.length; i++) px[i] = 0xFF000000 | Integer.parseInt(hex.substring(i * 6, i * 6 + 6), 16);
                cover = px;
            } else {
                cover = null;
            }
            coverId = id;
        }
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    /** Pozycja w utworze (sekundy) – liczona płynnie między odczytami. */
    public static double position() {
        double p = playing ? pos + (System.currentTimeMillis() - stamp) / 1000.0 : pos;
        return dur > 0 ? Math.min(dur, p) : p;
    }

    public static double duration() { return dur; }

    public static void command(String cmd) {
        if (cmd.equals("toggle")) playing = !playing; // od razu widać zmianę
        if (cmd.equals("next") || cmd.equals("prev")) { pos = 0; stamp = System.currentTimeMillis(); }
        Thread t = new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(BASE + "/cmd/" + cmd).openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(400);
                c.setReadTimeout(3000);
                c.setDoOutput(true);
                c.getOutputStream().close();
                c.getInputStream().close();
                c.disconnect();
            } catch (Throwable ignored) {
            }
        }, "VoidClient-Spotify-cmd");
        t.setDaemon(true);
        t.start();
    }

    public static String time(double s) {
        int t = (int) Math.max(0, s);
        return t / 60 + ":" + (t % 60 < 10 ? "0" : "") + t % 60;
    }
}
