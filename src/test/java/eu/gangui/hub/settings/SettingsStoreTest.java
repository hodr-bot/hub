package eu.gangui.hub.settings;

import java.nio.file.Files;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SettingsStoreTest {

    @TempDir
    java.nio.file.Path dir;

    @Test
    void defaultsWhenNoFile() {
        final var store = new SettingsStore(this.dir);
        assertEquals(HubSettings.defaults(), store.load(UUID.randomUUID()));
    }

    @Test
    void roundTrip() {
        final var store = new SettingsStore(this.dir);
        final var player = UUID.randomUUID();
        final var saved = new HubSettings(
            HubSettings.Language.EN, false, true, HubSettings.Density.REDUCED);
        store.save(player, saved);
        assertTrue(Files.exists(this.dir.resolve(player + ".yml")));
        assertEquals(saved, new SettingsStore(this.dir).load(player));
    }

    @Test
    void corruptFileFallsBackToDefaults() throws Exception {
        final var store = new SettingsStore(this.dir);
        final var player = UUID.randomUUID();
        Files.writeString(this.dir.resolve(player + ".yml"), "language: [unclosed");
        assertEquals(HubSettings.defaults(), store.load(player));
    }
}
