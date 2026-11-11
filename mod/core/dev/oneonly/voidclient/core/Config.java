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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        Integer freelookKey;
        Integer zoomKey;
        Integer waypointKey;
        String sky;
        Integer crossShape, crossColor, crossSize, outlineColor;
        Boolean crossDot, crossOutline;
        Float vmX, vmY, vmZ, vmScale;
        Integer vmRotX, vmRotY, vmRotZ, aspect;
        List<Waypoints.Point> waypoints = new ArrayList<>();
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
            if (d.freelookKey != null) Keybinds.freelook = d.freelookKey;
            if (d.zoomKey != null) Keybinds.zoom = d.zoomKey;
            if (d.waypointKey != null) Keybinds.waypoint = d.waypointKey;
            if (d.sky != null && (d.sky.isEmpty() || Sky.find(d.sky) != null)) Sky.selected = d.sky;
            if (d.crossShape != null) Look.shape = Math.floorMod(d.crossShape, Look.SHAPES.length);
            if (d.crossColor != null) Look.color = Math.floorMod(d.crossColor, Look.COLOR_NAMES.length);
            if (d.crossSize != null) Look.size = Math.max(2, Math.min(10, d.crossSize));
            if (d.outlineColor != null) Look.outlineColor = Math.floorMod(d.outlineColor, Look.OUTLINE_NAMES.length);
            if (d.crossDot != null) Look.dot = d.crossDot;
            if (d.crossOutline != null) Look.outline = d.crossOutline;
            if (d.vmX != null) View.x = d.vmX;
            if (d.vmY != null) View.y = d.vmY;
            if (d.vmZ != null) View.z = d.vmZ;
            if (d.vmScale != null) View.scale = Math.max(0.5f, Math.min(1.5f, d.vmScale));
            if (d.vmRotX != null) View.rotX = d.vmRotX;
            if (d.vmRotY != null) View.rotY = d.vmRotY;
            if (d.vmRotZ != null) View.rotZ = d.vmRotZ;
            if (d.aspect != null) View.aspect = Math.floorMod(d.aspect, View.ASPECTS.length);
            if (d.waypoints != null) {
                Waypoints.ALL.clear();
                for (Waypoints.Point p : d.waypoints) if (p != null && p.world != null) Waypoints.ALL.add(p);
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
        d.freelookKey = Keybinds.freelook;
        d.zoomKey = Keybinds.zoom;
        d.waypointKey = Keybinds.waypoint;
        d.sky = Sky.selected;
        d.crossShape = Look.shape;
        d.crossColor = Look.color;
        d.crossSize = Look.size;
        d.crossDot = Look.dot;
        d.crossOutline = Look.outline;
        d.outlineColor = Look.outlineColor;
        d.vmX = View.x; d.vmY = View.y; d.vmZ = View.z; d.vmScale = View.scale;
        d.vmRotX = View.rotX; d.vmRotY = View.rotY; d.vmRotZ = View.rotZ;
        d.aspect = View.aspect;
        d.waypoints = new ArrayList<>(Waypoints.ALL);
        for (Module m : Modules.all()) {
            Entry e = new Entry();
            e.enabled = m.savedEnabled();
            if (m instanceof HudModule) {
                HudModule hm = (HudModule) m;
                e.x = hm.atAuto() ? HudModule.AUTO : hm.x;
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
