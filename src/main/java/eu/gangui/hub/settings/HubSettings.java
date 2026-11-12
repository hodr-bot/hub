package eu.gangui.hub.settings;

import org.jspecify.annotations.NullMarked;

/**
 * Per-player hub preferences. Persisted by SettingsStore, edited through the
 * settings menu. Every field backs a visible, working behaviour.
 */
@NullMarked
public record HubSettings(Language language, boolean announcements, boolean autoQueue, Density density) {

    public enum Language {
        FR, EN
    }

    public enum Density {
        FULL, REDUCED, OFF
    }

    public static HubSettings defaults() {
        return new HubSettings(Language.FR, true, false, Density.FULL);
    }
}
