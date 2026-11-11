package dev.oneonly.voidclient.yarn;

import net.minecraft.client.gui.hud.ChatHudLine;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Usuwa najnowszą wiadomość z czatu (przy łączeniu powtórzeń w ×N).
 * Listy szukamy po typie elementów, bo nazwy pól w grze są zaciemnione.
 */
public final class ChatLines {
    private static Field all, visible;
    private static boolean looked;

    private ChatLines() {}

    @SuppressWarnings("unchecked")
    public static boolean removeNewest(Object chatHud) {
        try {
            if (!find(chatHud)) return false;
            List<Object> msgs = (List<Object>) all.get(chatHud);
            List<Object> vis = (List<Object>) visible.get(chatHud);
            if (msgs.isEmpty() || vis.isEmpty()) return false;
            msgs.remove(0);
            vis.remove(0); // ostatnia linijka najnowszej wiadomości
            while (!vis.isEmpty() && !((ChatHudLine.Visible) vis.get(0)).endOfEntry()) vis.remove(0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean find(Object hud) throws IllegalAccessException {
        if (all != null && visible != null) return true;
        for (Field f : hud.getClass().getDeclaredFields()) {
            if (!List.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            List<?> l = (List<?>) f.get(hud);
            if (l == null || l.isEmpty()) continue;
            Object e = l.get(0);
            if (e instanceof ChatHudLine.Visible) visible = f;
            else if (e instanceof ChatHudLine) all = f;
        }
        return all != null && visible != null;
    }
}
