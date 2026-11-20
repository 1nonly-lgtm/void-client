package dev.oneonly.voidclient.yarn;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Gładka czcionka menu (Inter z assets/voidclient/font/ui.json). Sposób ustawiania czcionki w Style
 * zmienia się między wersjami (Identifier → opis czcionki), więc metodę znajdujemy po typach.
 */
final class UiFont {
    private static Style regular, bold;
    private static final Map<String, Text> REG = new HashMap<>(), BOLD = new HashMap<>();

    private UiFont() {}

    static Text text(String s, boolean b) {
        Map<String, Text> cache = b ? BOLD : REG;
        Text t = cache.get(s);
        if (t == null) {
            if (cache.size() > 4000) cache.clear();
            t = Text.literal(s).setStyle(style(b));
            cache.put(s, t);
        }
        return t;
    }

    private static Style style(boolean b) {
        if (regular == null) {
            regular = make("voidclient:ui");
            bold = make("voidclient:ui_bold");
        }
        return b ? bold : regular;
    }

    static Style make(String id) {
        Identifier ident = Identifier.tryParse(id);
        try {
            // 1.20 – 1.21.8: withFont(Identifier)
            for (Method m : Style.class.getMethods()) {
                if (Modifier.isStatic(m.getModifiers()) || m.getReturnType() != Style.class || m.getParameterCount() != 1) continue;
                if (m.getParameterTypes()[0] == Identifier.class) return (Style) m.invoke(Style.EMPTY, ident);
            }
            // 1.21.9+: withFont(opis czcionki) – opis to rekord z jednym polem Identifier
            for (Method m : Style.class.getMethods()) {
                if (Modifier.isStatic(m.getModifiers()) || m.getReturnType() != Style.class || m.getParameterCount() != 1) continue;
                Class<?> p = m.getParameterTypes()[0];
                if (p.isPrimitive() || p == String.class || p == Style.class || p.isEnum()) continue;
                Object desc = descriptor(p, ident);
                if (desc != null) return (Style) m.invoke(Style.EMPTY, desc);
            }
        } catch (Throwable ignored) {
        }
        return Style.EMPTY;
    }

    private static Object descriptor(Class<?> type, Identifier ident) throws ReflectiveOperationException {
        java.util.List<Class<?>> cands = new java.util.ArrayList<>();
        try {
            Class<?>[] permitted = type.getPermittedSubclasses();
            if (permitted != null) cands.addAll(java.util.Arrays.asList(permitted));
        } catch (Throwable ignored) {
        }
        cands.addAll(java.util.Arrays.asList(type.getDeclaredClasses()));
        for (Class<?> c : cands) {
            if (!type.isAssignableFrom(c) || Modifier.isAbstract(c.getModifiers())) continue;
            for (Constructor<?> k : c.getDeclaredConstructors()) {
                if (k.getParameterCount() == 1 && k.getParameterTypes()[0] == Identifier.class) {
                    k.setAccessible(true);
                    return k.newInstance(ident);
                }
            }
        }
        return null;
    }
}
