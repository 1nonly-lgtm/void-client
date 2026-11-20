package dev.oneonly.voidclient.yarn;

import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;

/**
 * Podmiana peleryny w danych skina gracza. Rekord ze skinem zmienia się między wersjami
 * (Identifier → opis tekstury), więc składamy go na nowo po typach pól: peleryna to drugie pole
 * tego samego typu co skin.
 */
public final class CapeSwap {
    private CapeSwap() {}

    private static final java.util.Map<Object, Object[]> CACHE = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** Z pamięcią: ten sam skin i ta sama klatka = ten sam obiekt (gra pyta o skin wiele razy na klatkę). */
    public static Object swap(Object skin, String frame) {
        if (skin == null || frame == null) return null;
        Object[] c = CACHE.get(skin);
        if (c != null && frame.equals(c[0])) return c[1];
        Object r = build(skin, frame);
        if (r != null) CACHE.put(skin, new Object[]{frame, r});
        return r;
    }

    private static Object build(Object skin, String frame) {
        try {
            Class<?> c = skin.getClass();
            if (!c.isRecord()) return null;
            RecordComponent[] rc = c.getRecordComponents();
            Class<?> t0 = rc[0].getType();
            int cape = -1;
            for (int i = 1; i < rc.length; i++) if (rc[i].getType() == t0) { cape = i; break; }
            if (cape < 0) return null;
            Object[] vals = new Object[rc.length];
            Class<?>[] types = new Class<?>[rc.length];
            for (int i = 0; i < rc.length; i++) {
                rc[i].getAccessor().setAccessible(true);
                vals[i] = rc[i].getAccessor().invoke(skin);
                types[i] = rc[i].getType();
            }
            vals[cape] = t0 == Identifier.class
                    ? Identifier.tryParse("voidclient:textures/cape/" + frame + ".png")
                    : asset(t0, Identifier.tryParse("voidclient:cape/" + frame));
            if (vals[cape] == null) return null;
            Constructor<?> k = c.getDeclaredConstructor(types);
            k.setAccessible(true);
            return k.newInstance(vals);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Opis tekstury (1.21.9+): klasa implementująca typ pola, z konstruktorem (Identifier). */
    static Object asset(Class<?> type, Identifier id) throws ReflectiveOperationException {
        java.util.List<Class<?>> cands = new java.util.ArrayList<>();
        if (!type.isInterface() && !java.lang.reflect.Modifier.isAbstract(type.getModifiers())) cands.add(type);
        try {
            Class<?>[] p = type.getPermittedSubclasses();
            if (p != null) cands.addAll(java.util.Arrays.asList(p));
        } catch (Throwable ignored) {
        }
        cands.addAll(java.util.Arrays.asList(type.getDeclaredClasses()));
        if (type.getEnclosingClass() != null) cands.addAll(java.util.Arrays.asList(type.getEnclosingClass().getDeclaredClasses()));
        for (Class<?> c : cands) {
            if (!type.isAssignableFrom(c) || c.isInterface() || java.lang.reflect.Modifier.isAbstract(c.getModifiers())) continue;
            for (Constructor<?> k : c.getDeclaredConstructors()) {
                if (k.getParameterCount() == 1 && k.getParameterTypes()[0] == Identifier.class) {
                    k.setAccessible(true);
                    return k.newInstance(id);
                }
            }
        }
        return null;
    }
}
