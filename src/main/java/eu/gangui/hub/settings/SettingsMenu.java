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
public final class SettingsMenu implements NodeLike {

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
        final var language = scope.useState(current.language());
        final var announcements = scope.useState(current.announcements());
        final var autoQueue = scope.useState(current.autoQueue());
        final var density = scope.useState(current.density());
        final var next = new HubSettings(language.get(), announcements.get(), autoQueue.get(), density.get());
        if (!next.equals(current)) {
            this.store.save(player.getUuid(), next);
        }
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
                        .value(language)
                        .build())
                    .build())
                .add(SettingRow.builder()
                    .label("Annonces")
                    .description("Messages d'ambiance du hub")
                    .control(Toggle.of(announcements))
                    .build())
                .add(SettingRow.builder()
                    .label("File auto")
                    .description("Rejoindre la prochaine partie au retour au hub")
                    .control(Toggle.of(autoQueue))
                    .build())
                .add(SettingRow.builder()
                    .label("Particules")
                    .description("Effets cosmetiques du hub")
                    .control(Segmented.<HubSettings.Density>builder()
                        .options(HubSettings.Density.values())
                        .value(density)
                        .build())
                    .build())
                .add(Text.of("Modifie, ca s'enregistre tout seul."))
                .add(Button.of("< Retour", () -> new Canvas(this.menu).open(scope.viewer())))
                .build())
            .build();
    }
}
