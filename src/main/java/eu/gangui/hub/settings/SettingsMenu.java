package eu.gangui.hub.settings;

import eu.gangui.hub.binding.MinestomCanvas;
import eu.gangui.hub.menu.GameMenu;
import me.chubbyduck.canvas.Canvas;
import me.chubbyduck.canvas.elements.control.Button;
import me.chubbyduck.canvas.elements.control.Segmented;
import me.chubbyduck.canvas.elements.control.Toggle;
import me.chubbyduck.canvas.elements.display.Panel;
import me.chubbyduck.canvas.elements.display.SettingRow;
import me.chubbyduck.canvas.elements.display.Text;
import me.chubbyduck.canvas.elements.layout.Column;
import me.chubbyduck.canvas.layout.Align;
import me.chubbyduck.canvas.node.Node;
import me.chubbyduck.canvas.node.NodeLike;
import me.chubbyduck.canvas.state.Scope;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/**
 * Player settings screen. Every control persists on change; values apply on
 * next hub interaction (language, announcements gate, auto-queue, particles).
 */
@NullMarked
public final class SettingsMenu extends me.chubbyduck.canvas.node.Node {

    private final GameMenu menu;
    private final SettingsStore store;

    public SettingsMenu(final GameMenu menu, final SettingsStore store) {
        this.menu = menu;
        this.store = store;
    }

    @Override
    public Node getNode(final Scope scope) {
        final var player = MinestomCanvas.playerOf(scope.viewer());
        final var current = this.store.load(player.getUuid());
        return Panel.builder()
            .title(Component.text("Reglages"))
            .add(Column.builder()
                .gap(4)
                .align(Align.STRETCH)
                .add(SettingRow.builder()
                    .label("Langue")
                    .description("Langue des menus du hub")
                    .control(Segmented.<HubSettings.Language>builder()
                        .options(HubSettings.Language.values())
                        .value(current.language(), value -> this.store.save(player.getUuid(), new HubSettings(
                            value, current.announcements(), current.autoQueue(), current.density())))
                        .build())
                    .build())
                .add(SettingRow.builder()
                    .label("Annonces")
                    .description("Messages d'ambiance du hub")
                    .control(Toggle.of(current.announcements(), value -> this.store.save(player.getUuid(), new HubSettings(
                        current.language(), value, current.autoQueue(), current.density()))))
                    .build())
                .add(SettingRow.builder()
                    .label("File auto")
                    .description("Rejoindre la prochaine partie au retour au hub")
                    .control(Toggle.of(current.autoQueue(), value -> this.store.save(player.getUuid(), new HubSettings(
                        current.language(), current.announcements(), value, current.density()))))
                    .build())
                .add(SettingRow.builder()
                    .label("Particules")
                    .description("Effets cosmetiques du hub")
                    .control(Segmented.<HubSettings.Density>builder()
                        .options(HubSettings.Density.values())
                        .value(current.density(), value -> this.store.save(player.getUuid(), new HubSettings(
                            current.language(), current.announcements(), current.autoQueue(), value)))
                        .build())
                    .build())
                .add(Text.of("Modifie, ca s'enregistre tout seul."))
                .add(Button.of("< Retour", () -> new Canvas(this.menu).open(scope.viewer())))
                .build())
            .build();
    }
}
