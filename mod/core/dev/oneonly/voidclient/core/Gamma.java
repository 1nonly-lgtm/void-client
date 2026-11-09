package dev.oneonly.voidclient.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Ustawia jasność ponad 100% (Fullbright). Suwak gry na to nie pozwala, więc wartość
 * wpisujemy bezpośrednio do pola opcji. Pole szukamy po typie, nie po nazwie,
 * dzięki czemu działa na każdej wersji i każdych mapowaniach.
 */
public final class Gamma {
    private static Field valueField;
    private static boolean failed;

    private Gamma() {}

    public static boolean forceSet(Object option, double value) {
        if (option == null || failed) return false;
        try {
            if (valueField == null) {
                for (Field f : option.getClass().getDeclaredFields()) {
                    if (f.getType() == Object.class && !Modifier.isFinal(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        valueField = f;
                        break;
                    }
                }
                if (valueField == null) { failed = true; return false; }
            }
            valueField.set(option, value);
            return true;
        } catch (Throwable t) {
            failed = true;
            VoidCore.LOG.warn("Fullbright niedostępny na tej wersji", t);
            return false;
        }
    }
}
