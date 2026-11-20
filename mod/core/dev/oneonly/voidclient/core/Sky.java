package dev.oneonly.voidclient.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Własne nieba Void Client. Rysuje je mod Nuit (launcher instaluje go sam), a my podajemy mu
 * małą paczkę zasobów „VoidClient-Niebo” z jednym plikiem nieba – tym wybranym w menu.
 * Tekstury (arkusze 3×2) są w jarze Void Client: assets/voidclient/textures/sky/.
 */
public final class Sky {
    public static final class Def {
        public final String id, name, desc;
        public final int top, bottom;

        Def(String id, String name, String desc, int top, int bottom) {
            this.id = id; this.name = name; this.desc = desc; this.top = top; this.bottom = bottom;
        }
    }

    public static final Def[] ALL = {
            new Def("pustka", "Pustka", "Czarna dziura z fioletowym dyskiem", 0xFF0E0620, 0xFF2A0F4A),
            new Def("noc", "Gwiaździsta noc", "Droga Mleczna i tysiące gwiazd", 0xFF040814, 0xFF12203E),
            new Def("zachod", "Różowy zachód", "Ciepłe słońce nad chmurami", 0xFF2A1A52, 0xFFFF9670),
            new Def("zorza", "Zorza polarna", "Zielono-fioletowe kurtyny światła", 0xFF04101C, 0xFF1C6E5A),
            new Def("end", "Niebo Endu", "Mroczne smugi i fiolet", 0xFF0A040E, 0xFF3A1650),
            new Def("mglawica", "Mgławica", "Niebiesko-różowe obłoki gazu", 0xFF080818, 0xFF4A2A7A),
            new Def("planeta", "Planeta", "Gazowy olbrzym z pierścieniami", 0xFF03020A, 0xFF1A1030),
            new Def("synthwave", "Synthwave", "Słońce w paski i neonowa siatka", 0xFF14042A, 0xFFFF3CAA),
            new Def("krwawy", "Krwawy księżyc", "Czerwona noc z wielkim księżycem", 0xFF0E0204, 0xFF6E0E12),
            new Def("galaktyka", "Galaktyka", "Spiralna galaktyka nad głową", 0xFF020208, 0xFF2A2A5A),
            new Def("swit", "Złoty świt", "Słońce nad morzem chmur", 0xFF283C78, 0xFFFFBE6E),
            new Def("burza", "Burza", "Ciężkie chmury i błyskawica", 0xFF0E0E16, 0xFF2E2C3E),
            new Def("meteory", "Deszcz meteorytów", "Spadające gwiazdy z ogonami", 0xFF04040C, 0xFF140E22),
            new Def("pierscien", "Pierścień planety", "Świecący pierścień przez całe niebo", 0xFF03030A, 0xFF120E28),
            new Def("dwa_slonca", "Dwa słońca", "Zachód nad pustynią obcej planety", 0xFF28205A, 0xFFFFAA5A),
            new Def("podwodne", "Pod wodą", "Promienie światła i bąbelki", 0xFF0A4E78, 0xFF04203C),
            new Def("cukrowa", "Wata cukrowa", "Różowe chmury na pastelowym niebie", 0xFF7AAAFA, 0xFFFFBEDC),
            new Def("noir", "Noir", "Czarno-biała noc z księżycem", 0xFF060608, 0xFF28282A),
    };

    public static final String PACK = "VoidClient-Niebo";
    /** Id wybranego nieba albo "" (zwykłe niebo / niebo z Twojej paczki tekstur). */
    public static String selected = "";

    private Sky() {}

    public static Def find(String id) {
        for (Def d : ALL) if (d.id.equals(id)) return d;
        return null;
    }

    /** Czy jest mod, który narysuje niebo (Nuit, stary FabricSkyBoxes albo port Horizon). */
    public static boolean rendererPresent(Game g) {
        try {
            return g.modLoaded("nuit") || g.modLoaded("fabricskyboxes") || g.modLoaded("horizon");
        } catch (Throwable t) {
            return true;
        }
    }

    /** Wybór z menu: zapisuje paczkę i przeładowuje zasoby gry. */
    public static void select(Game g, String id) {
        if (id.equals(selected)) return;
        selected = id;
        Config.save();
        write(g);
        try {
            g.reloadPack("file/" + PACK, !selected.isEmpty());
        } catch (Throwable t) {
            VoidCore.LOG.warn("Nie udało się przeładować nieba", t);
        }
    }

    /** Przy starcie gry – paczka na dysku ma odpowiadać wybranemu niebu (przed wczytaniem zasobów). */
    public static void init(Game g) {
        write(g);
    }

    private static void write(Game g) {
        try {
            Path dir = g.gameDir().resolve("resourcepacks").resolve(PACK);
            Path assets = dir.resolve("assets");
            if (Files.exists(assets)) {
                try (Stream<Path> s = Files.walk(assets)) {
                    s.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
                }
            }
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("pack.mcmeta"),
                    "{\"pack\":{\"description\":\"Nieba Void Client – wybierasz je w menu (prawy Shift)\","
                            + "\"pack_format\":15,\"supported_formats\":[15,999],\"min_format\":15,\"max_format\":999}}",
                    StandardCharsets.UTF_8);
            Def d = find(selected);
            if (d == null) return;
            String tex = "voidclient:textures/sky/" + d.id + ".png";
            boolean old = g.modLoaded("fabricskyboxes");
            Path json;
            String body;
            if (old) {
                // Nuit 0.7 / FabricSkyBoxes (1.20.x): obrót o 360° na dobę wokół osi pionowej
                json = dir.resolve("assets/voidclient/sky/void_" + d.id + ".json");
                body = "{\"schemaVersion\":2,\"type\":\"single-sprite-square-textured\","
                        + "\"properties\":{\"priority\":10,\"fade\":{\"alwaysOn\":true},\"rotation\":{\"rotationSpeedY\":1.0}},"
                        + "\"decorations\":{\"showSun\":false,\"showMoon\":false,\"showStars\":false},"
                        + "\"texture\":\"" + tex + "\"}";
            } else {
                // Nuit 1.0+ (i Horizon): ten sam arkusz 3×2, obrót wokół osi pionowej
                json = dir.resolve("assets/nuit/sky/voidclient_" + d.id + ".json");
                body = "{\"schemaVersion\":1,\"type\":\"square-textured\","
                        + "\"properties\":{\"layer\":10,\"fade\":{\"alwaysOn\":true},"
                        + "\"rotation\":{\"speed\":1.0,\"axis\":{\"0\":[0,0,0]}}},"
                        + "\"texture\":\"" + tex + "\"}";
            }
            Files.createDirectories(json.getParent());
            Files.writeString(json, body, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            VoidCore.LOG.warn("Nie udało się przygotować paczki nieba", t);
        }
    }
}
