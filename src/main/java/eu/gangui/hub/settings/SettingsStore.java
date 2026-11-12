package eu.gangui.hub.settings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * File-backed per-player settings. One YAML file ({uuid}.yml) per player,
 * cached in memory. Missing or corrupt files fall back to defaults.
 */
@NullMarked
public final class SettingsStore {

    private final Path dir;
    private final Yaml yaml;
    private final Map<UUID, HubSettings> cache = new ConcurrentHashMap<>();

    public SettingsStore(final Path dir) {
        this.dir = dir;
        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);
    }

    public HubSettings load(final UUID player) {
        return this.cache.computeIfAbsent(player, this::read);
    }

    public void save(final UUID player, final HubSettings settings) {
        this.cache.put(player, settings);
        try {
            Files.createDirectories(this.dir);
            try (Writer out = Files.newBufferedWriter(this.dir.resolve(player + ".yml"))) {
                this.yaml.dump(Map.of(
                    "language", settings.language().name(),
                    "announcements", settings.announcements(),
                    "autoQueue", settings.autoQueue(),
                    "density", settings.density().name()), out);
            }
        } catch (final IOException e) {
            // Best effort: the in-memory value still applies for this session.
        }
    }

    private HubSettings read(final UUID player) {
        final Path file = this.dir.resolve(player + ".yml");
        if (!Files.isRegularFile(file)) {
            return HubSettings.defaults();
        }
        try (Reader in = Files.newBufferedReader(file)) {
            final Map<String, Object> map = this.yaml.load(in);
            if (map == null) {
                return HubSettings.defaults();
            }
            return new HubSettings(
                parse(HubSettings.Language.class, map.get("language"), HubSettings.Language.FR),
                bool(map.get("announcements"), true),
                bool(map.get("autoQueue"), false),
                parse(HubSettings.Density.class, map.get("density"), HubSettings.Density.FULL));
        } catch (final IOException | RuntimeException e) {
            return HubSettings.defaults();
        }
    }

    private static <T extends Enum<T>> T parse(final Class<T> type, final Object raw, final T fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, String.valueOf(raw));
        } catch (final IllegalArgumentException e) {
            return fallback;
        }
    }

    private static boolean bool(final Object raw, final boolean fallback) {
        return raw instanceof Boolean b ? b : fallback;
    }
}
