package dev.oneonly.voidclient.core;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** Zmiana skina z poziomu gry – przez oficjalne API Minecrafta, na Twoim koncie. */
public final class SkinUploader {
    private static final String URL = "https://api.minecraftservices.com/minecraft/profile/skins";

    public static boolean slim = false;
    public static volatile String status = "";
    private static volatile boolean busy;

    private SkinUploader() {}

    public static boolean busy() { return busy; }

    /** Otwiera okno wyboru pliku i wysyła skin w tle. */
    public static void pickAndUpload(Game game) {
        if (busy) return;
        String chosen;
        try {
            chosen = TinyFileDialogs.tinyfd_openFileDialog("Wybierz skin (PNG 64×64)", "", null, "Skin PNG", false);
        } catch (Throwable t) {
            status = "Okno wyboru pliku niedostępne";
            return;
        }
        if (chosen == null || chosen.isEmpty()) return;
        String token = game.accessToken();
        boolean model = slim;
        busy = true;
        status = "Wysyłanie skina…";
        Thread t = new Thread(() -> {
            try {
                status = upload(Path.of(chosen), token, model);
            } catch (Exception e) {
                status = "Błąd: " + e.getMessage();
            } finally {
                busy = false;
            }
        }, "VoidClient-Skin");
        t.setDaemon(true);
        t.start();
    }

    private static String upload(Path file, String token, boolean slimModel) throws Exception {
        byte[] png = Files.readAllBytes(file);
        if (png.length < 24 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') return "To nie jest plik PNG";
        int w = readInt(png, 16);
        int h = readInt(png, 20);
        if (w != 64 || (h != 64 && h != 32)) return "Skin musi mieć 64×64 (lub 64×32) piksele";
        if (token == null || token.isEmpty() || token.length() < 20) return "Brak sesji – uruchom grę z launchera";

        String boundary = "----VoidClient" + System.nanoTime();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"variant\"\r\n\r\n"
                + (slimModel ? "slim" : "classic") + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n";
        body.write(head.getBytes(StandardCharsets.UTF_8));
        body.write(png);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        int code = res.statusCode();
        if (code >= 200 && code < 300) return "Skin zmieniony! Wejdź ponownie na serwer, żeby go zobaczyć";
        if (code == 401) return "Sesja wygasła – uruchom grę ponownie z launchera";
        if (code == 429) return "Za dużo prób – spróbuj za minutę";
        return "Serwer odrzucił skin (kod " + code + ")";
    }

    private static int readInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16) | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }
}
