package dev.oneonly.voidclient.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.oneonly.voidclient.core.module.HudModule;
import dev.oneonly.voidclient.core.module.Module;
import dev.oneonly.voidclient.core.module.Modules;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/** Zapis i odczyt ustawień do config/voidclient.json. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file;

    private static final class Data {
        int accent = 0;
        boolean hudBackground = true;
        boolean textShadow = true;
        boolean slimSkin = false;
        String[] autoText;
        Map<String, Entry> modules = new HashMap<>();
    }

    private static final class Entry {
        boolean enabled;
        Integer x;
        Integer y;
    }

    private Config() {}

    public static void load(Path configDir) {
        file = configDir.resolve("voidclient.json");
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d == null) return;
            Theme.accentIndex = d.accent;
            Theme.hudBackground = d.hudBackground;
            Theme.textShadow = d.textShadow;
            SkinUploader.slim = d.slimSkin;
            if (d.autoText != null) {
                for (int i = 0; i < AutoText.SLOTS && i < d.autoText.length; i++) {
                    AutoText.TEXTS[i] = d.autoText[i] == null ? "" : d.autoText[i];
                }
            }
            if (d.modules == null) return;
            for (Module m : Modules.all()) {
                Entry e = d.modules.get(m.id);
                if (e == null) continue;
                m.setEnabled(e.enabled);
                if (m instanceof HudModule) {
                    HudModule h = (HudModule) m;
                    if (e.x != null) h.x = e.x;
                    if (e.y != null) h.y = e.y;
                }
            }
        } catch (Exception ex) {
            VoidCore.LOG.warn("Nie udało się wczytać configu Void Client", ex);
        }
    }

    public static void save() {
        if (file == null) return;
        Data d = new Data();
        d.accent = Theme.accentIndex;
        d.hudBackground = Theme.hudBackground;
        d.textShadow = Theme.textShadow;
        d.slimSkin = SkinUploader.slim;
        d.autoText = AutoText.TEXTS.clone();
        for (Module m : Modules.all()) {
            Entry e = new Entry();
            e.enabled = m.isEnabled();
            if (m instanceof HudModule) {
                e.x = ((HudModule) m).x;
                e.y = ((HudModule) m).y;
            }
            d.modules.put(m.id, e);
        }
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("voidclient.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(d, w);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            VoidCore.LOG.warn("Nie udało się zapisać configu Void Client", ex);
        }
    }
}
