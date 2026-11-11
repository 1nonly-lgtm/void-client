package dev.oneonly.voidclient.core;

import dev.oneonly.voidclient.core.module.Modules;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Szybkie zrzuty ekranu: po F2 kopiuje zdjęcie do schowka (Windows) i pisze na czacie,
 * a przycisk w menu otwiera folder ze zrzutami.
 */
public final class Screenshots {
    private static long lastSeen = -1;
    private static int ticks;

    private Screenshots() {}

    public static Path folder(Game g) { return g.gameDir().resolve("screenshots"); }

    public static void tick(Game g) {
        if (++ticks < 10) return;
        ticks = 0;
        Path newest = newest(g);
        long t = newest == null ? 0 : newest.toFile().lastModified();
        if (lastSeen < 0) { lastSeen = t; return; } // stare zrzuty sprzed uruchomienia pomijamy
        if (t <= lastSeen) return;
        lastSeen = t;
        if (!Modules.SCREENSHOTS.isEnabled()) return;
        boolean copied = copy(newest.toFile());
        g.chatInfo(copied ? "§dVoid Client §7» §fZrzut skopiowany do schowka §8(Ctrl+V)" : "§dVoid Client §7» §fZrzut zapisany: §7" + newest.getFileName());
    }

    private static Path newest(Game g) {
        Path dir = folder(g);
        if (!Files.isDirectory(dir)) return null;
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".png"))
                    .max((a, b) -> Long.compare(a.toFile().lastModified(), b.toFile().lastModified())).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }

    /** Gra działa w trybie bez okien AWT, więc kopiujemy przez PowerShell (Windows). */
    private static boolean copy(File f) {
        if (!isWindows()) return false;
        try {
            String path = f.getAbsolutePath().replace("'", "''");
            String cmd = "Add-Type -AssemblyName System.Windows.Forms,System.Drawing;"
                    + "$i=[System.Drawing.Image]::FromFile('" + path + "');"
                    + "[System.Windows.Forms.Clipboard]::SetImage($i);$i.Dispose()";
            new ProcessBuilder("powershell", "-NoProfile", "-STA", "-WindowStyle", "Hidden", "-Command", cmd)
                    .redirectErrorStream(true).start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void openFolder(Game g) {
        try {
            File dir = folder(g).toFile();
            dir.mkdirs();
            if (isWindows()) new ProcessBuilder("explorer", dir.getAbsolutePath()).start();
            else new ProcessBuilder("xdg-open", dir.getAbsolutePath()).start();
        } catch (Exception e) {
            VoidCore.LOG.warn("Nie udało się otworzyć folderu zrzutów", e);
        }
    }
}
