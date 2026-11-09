package dev.oneonly.voidclient.core;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Klawisze Void Client ustawiane w naszym menu (Freelook, Zoom, Waypoint).
 * Kod &gt;= 0 to klawisz klawiatury (GLFW), 1000 + n to przycisk myszy n, -1 – brak.
 * Stan czytamy prosto z GLFW, więc działa tak samo w każdej wersji gry.
 */
public final class Keybinds {
    public static final int NONE = -1;
    public static final int MOUSE = 1000;

    public static int freelook = GLFW.GLFW_KEY_LEFT_ALT;
    public static int zoom = GLFW.GLFW_KEY_C;
    public static int waypoint = GLFW.GLFW_KEY_B;

    private static final Map<String, Boolean> WAS_DOWN = new HashMap<>();

    private Keybinds() {}

    public static boolean isDown(int code) {
        if (code == NONE) return false;
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) return false;
        if (code >= MOUSE) {
            int b = code - MOUSE;
            return b >= 0 && b <= GLFW.GLFW_MOUSE_BUTTON_LAST && GLFW.glfwGetMouseButton(window, b) == GLFW.GLFW_PRESS;
        }
        return code >= 32 && code <= GLFW.GLFW_KEY_LAST && GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
    }

    /** True tylko w ticku, w którym klawisz został wciśnięty. */
    public static boolean pressed(String id, int code) {
        boolean down = isDown(code);
        Boolean before = WAS_DOWN.put(id, down);
        return down && (before == null || !before);
    }

    public static String name(int code) {
        if (code == NONE) return "brak";
        if (code >= MOUSE) {
            int b = code - MOUSE;
            if (b == 0) return "LPM";
            if (b == 1) return "PPM";
            if (b == 2) return "Środkowy myszy";
            return "Mysz " + (b + 1);
        }
        switch (code) {
            case GLFW.GLFW_KEY_LEFT_ALT: return "Lewy Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "Prawy Alt";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "Lewy Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "Prawy Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "Lewy Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "Prawy Ctrl";
            case GLFW.GLFW_KEY_SPACE: return "Spacja";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_UP: return "Strzałka ↑";
            case GLFW.GLFW_KEY_DOWN: return "Strzałka ↓";
            case GLFW.GLFW_KEY_LEFT: return "Strzałka ←";
            case GLFW.GLFW_KEY_RIGHT: return "Strzałka →";
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
            default: break;
        }
        if (code >= 65 && code <= 90 || code >= 48 && code <= 57) return String.valueOf((char) code);
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F25) return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        if (code >= GLFW.GLFW_KEY_KP_0 && code <= GLFW.GLFW_KEY_KP_9) return "Num " + (code - GLFW.GLFW_KEY_KP_0);
        try {
            String n = GLFW.glfwGetKeyName(code, 0);
            if (n != null && !n.isEmpty()) return n.toUpperCase();
        } catch (Throwable ignored) {
            // brak okna – zostaje numer
        }
        return "Klawisz " + code;
    }
}
