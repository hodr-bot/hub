package eu.gangui.hub.menu;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import eu.gangui.hub.binding.MinestomCanvas;
import eu.gangui.hub.config.HubConfig;
import eu.gangui.hub.settings.SettingsMenu;
import eu.gangui.hub.settings.SettingsStore;
import me.chubbyduck.canvas.Canvas;
import me.chubbyduck.canvas.elements.control.Button;
import me.chubbyduck.canvas.elements.display.Panel;
import me.chubbyduck.canvas.elements.display.SettingRow;
import me.chubbyduck.canvas.elements.display.Text;
import me.chubbyduck.canvas.elements.layout.Column;
import me.chubbyduck.canvas.elements.layout.Row;
import me.chubbyduck.canvas.layout.Align;
import me.chubbyduck.canvas.node.Node;
import me.chubbyduck.canvas.node.NodeLike;
import me.chubbyduck.canvas.state.Scope;
import net.minestom.server.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Hub game selector: one button per configured game plus a settings entry.
 * Join behaviour is injected per game id, so the network project reuses this
 * screen with its own transfer logic.
 */
@NullMarked
public final class GameMenu extends me.chubbyduck.canvas.node.Node {

    /**
     * Join handler for one game. Implementations teleport, transfer, or queue
     * the player; the menu does not care which.
     */
    public interface GameConnector extends Consumer<Player> {
    }

    private final HubConfig config;
    private final Map<String, GameConnector> connectors;
    private final SettingsStore store;

    public GameMenu(final HubConfig config, final Map<String, GameConnector> connectors, final SettingsStore store) {
        this.config = config;
        this.connectors = Map.copyOf(connectors);
        this.store = store;
    }

    @Override
    public Node getNode(final Scope scope) {
        final var player = MinestomCanvas.playerOf(scope.viewer());
        final var buttons = Row.builder().gap(4);
        final List<HubConfig.GameEntry> games = this.config.games();
        for (final HubConfig.GameEntry game : games) {
            final GameConnector join = this.connectors.get(game.id());
            buttons.add(Button.builder()
                .label(game.label())
                .tooltip(game.description())
                .minWidth(90)
                .onClick(join == null ? () -> {
                } : () -> join.accept(player))
                .build());
        }
        return Panel.builder()
            .title(this.config.menuTitle())
            .add(Column.builder()
                .gap(6)
                .align(Align.STRETCH)
                .add(Text.of("Choisis ton jeu :"))
                .add(buttons.build())
                .add(SettingRow.builder()
                    .label("Reglages")
                    .description("Langue, annonces, file auto, particules")
                    .control(Button.of("Ouvrir",
                        () -> new Canvas(new SettingsMenu(this, this.store)).open(scope.viewer())))
                    .build())
                .build())
            .build();
    }
}
