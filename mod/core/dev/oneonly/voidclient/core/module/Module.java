package dev.oneonly.voidclient.core.module;

import dev.oneonly.voidclient.core.Game;

public abstract class Module {
    public enum Category {
        HUD("HUD"),
        GAME("Gra");

        public final String label;

        Category(String label) { this.label = label; }
    }

    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    private boolean enabled;

    protected Module(String id, String name, String description, Category category, boolean defaultEnabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = defaultEnabled;
    }

    public boolean isEnabled() { return enabled && available(); }

    /** Czy działa na tej wersji gry. Niedostępny moduł jest wyszarzony w menu. */
    public boolean available() { return true; }

    /** Krótki powód, gdy moduł jest niedostępny. */
    public String unavailableReason() { return "Niedostępne w tej wersji gry"; }

    /** Zapisany stan (także dla modułów niedostępnych w tej wersji). */
    public boolean savedEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
    }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() {}

    protected void onDisable() {}

    /** 20 razy na sekundę, tylko gdy moduł jest włączony i gracz jest w świecie. */
    public void onTick(Game game) {}
}
