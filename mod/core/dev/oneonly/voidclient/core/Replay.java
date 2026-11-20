package dev.oneonly.voidclient.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Sterowanie ReplayModem (instaluje go launcher, Opcje → Replay) przez odbicie – bez zależności przy budowaniu.
 * Robi to samo, co przyciski ReplayModa w menu pauzy: start/stop, pauza, znacznik, lista powtórek.
 */
public final class Replay {
    private Replay() {}

    private static Boolean installed;

    public static boolean installed() {
        if (installed == null) {
            try { Class.forName("com.replaymod.recording.ReplayModRecording"); installed = true; } catch (Throwable t) { installed = false; }
        }
        return installed;
    }

    private static Object field(Object o, String name) throws ReflectiveOperationException {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    private static Object handler() throws ReflectiveOperationException {
        Class<?> c = Class.forName("com.replaymod.recording.ReplayModRecording");
        Object inst = c.getField("instance").get(null);
        return inst == null ? null : c.getMethod("getConnectionEventHandler").invoke(inst);
    }

    private static Object listener() {
        try {
            Object h = handler();
            return h == null ? null : h.getClass().getMethod("getPacketListener").invoke(h);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object controls() {
        try {
            Object h = handler();
            return h == null ? null : field(h, "guiControls");
        } catch (Throwable t) {
            return null;
        }
    }

    /** 0 – nie działa (brak ReplayModa / nie jesteś w świecie), 1 – zatrzymane, 2 – pauza, 3 – nagrywa. */
    public static int state() {
        if (!installed()) return 0;
        Object c = controls();
        if (c == null || listener() == null) return 0;
        try {
            if ((Boolean) c.getClass().getMethod("isStopped").invoke(c)) return 1;
            return (Boolean) c.getClass().getMethod("isPaused").invoke(c) ? 2 : 3;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static long durationMs() {
        Object l = listener();
        try { return l == null ? 0 : (Long) l.getClass().getMethod("getCurrentDuration").invoke(l); } catch (Throwable t) { return 0; }
    }

    private static String marker(String name) throws ReflectiveOperationException {
        return (String) Class.forName("com.replaymod.editor.gui.MarkerProcessor").getField(name).get(null);
    }

    private static void addMarker(Object l, String name, Integer ts) throws ReflectiveOperationException {
        if (ts == null) l.getClass().getMethod("addMarker", String.class).invoke(l, name);
        else l.getClass().getMethod("addMarker", String.class, int.class).invoke(l, name, ts);
    }

    private static void set(Object o, String name, boolean v) throws ReflectiveOperationException {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setBoolean(o, v);
    }

    private static void update(Object c) throws ReflectiveOperationException {
        Method m = c.getClass().getDeclaredMethod("updateState");
        m.setAccessible(true);
        m.invoke(c);
    }

    /** Start / stop nagrywania (jak przycisk w menu pauzy ReplayModa). */
    public static boolean toggleRecording() {
        Object c = controls(), l = listener();
        if (c == null || l == null) return false;
        try {
            boolean stopped = (Boolean) c.getClass().getMethod("isStopped").invoke(c);
            boolean paused = (Boolean) c.getClass().getMethod("isPaused").invoke(c);
            if (stopped) {
                set(c, "paused", false);
                addMarker(l, marker("MARKER_NAME_END_CUT"), null);
            } else {
                int ts = (int) durationMs();
                if (!paused) addMarker(l, marker("MARKER_NAME_START_CUT"), ts);
                addMarker(l, marker("MARKER_NAME_SPLIT"), ts + 1);
            }
            set(c, "stopped", !stopped);
            update(c);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean togglePause() {
        Object c = controls(), l = listener();
        if (c == null || l == null || state() < 2) return false;
        try {
            boolean paused = (Boolean) c.getClass().getMethod("isPaused").invoke(c);
            addMarker(l, marker(paused ? "MARKER_NAME_END_CUT" : "MARKER_NAME_START_CUT"), null);
            set(c, "paused", !paused);
            update(c);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean addMarker() {
        Object l = listener();
        if (l == null) return false;
        try { addMarker(l, null, null); return true; } catch (Throwable t) { return false; }
    }

    /** Lista powtórek ReplayModa. */
    public static boolean openViewer() {
        try {
            Class<?> rep = Class.forName("com.replaymod.replay.ReplayModReplay");
            Object inst = rep.getField("instance").get(null);
            Object gui = Class.forName("com.replaymod.replay.gui.screen.GuiReplayViewer").getConstructor(rep).newInstance(inst);
            gui.getClass().getMethod("display").invoke(gui);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
