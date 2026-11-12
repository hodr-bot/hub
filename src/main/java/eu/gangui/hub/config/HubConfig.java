package eu.gangui.hub.config;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/**
 * Static hub configuration. Games are display entries; how a player actually
 * joins one is provided separately as connectors (see HubModule), so the
 * network project can reuse this module with its own transfer logic.
 */
@NullMarked
public record HubConfig(Component menuTitle, List<GameEntry> games) {

    public HubConfig {
        games = List.copyOf(games);
    }

    public record GameEntry(String id, Component label, Component description) {
    }
}
