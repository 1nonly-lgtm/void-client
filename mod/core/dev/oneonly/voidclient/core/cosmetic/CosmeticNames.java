package dev.oneonly.voidclient.core.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Nazwy kosmetyków i emotek (te same co w launcherze). */
public final class CosmeticNames {
    private CosmeticNames() {}

    public static final String[] CATS = {"cape", "wings", "hat", "pet", "fx", "emote"};
    public static final String[] LABELS = {"Peleryny", "Skrzydła", "Czapki", "Zwierzaki", "Efekty", "Emotki"};
    private static Map<String, List<String[]>> all;

    public static synchronized List<String[]> list(String cat) {
        if (all == null) {
            all = new HashMap<>();
            try (InputStream in = CosmeticNames.class.getResourceAsStream("/assets/voidclient/cosmetics/names.json")) {
                if (in != null) {
                    JsonObject o = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                    for (String c : o.keySet()) {
                        List<String[]> l = new ArrayList<>();
                        for (var e : o.getAsJsonArray(c)) {
                            JsonArray a = e.getAsJsonArray();
                            l.add(new String[]{a.get(0).getAsString(), a.get(1).getAsString()});
                        }
                        all.put(c, l);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return all.getOrDefault(cat, Collections.emptyList());
    }

    public static String name(String cat, String id) {
        if (id == null) return "—";
        for (String[] e : list(cat)) if (e[0].equals(id)) return e[1];
        return id;
    }
}
