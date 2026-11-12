package eu.gangui.hub;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import eu.gangui.hub.binding.MinestomCanvas;
import eu.gangui.hub.config.HubConfig;
import eu.gangui.hub.menu.GameMenu;
import eu.gangui.hub.settings.SettingsMenu;
import eu.gangui.hub.settings.SettingsStore;
import me.chubbyduck.canvas.Canvas;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.jspecify.annotations.NullMarked;

/**
 * Entry point. Installs the Canvas runtime, the /menu and /settings commands,
 * and keeps game join logic injected so the network project can reuse this
 * module with its own transfer implementation.
 */
@NullMarked
public final class HubModule {

    private HubModule() {
    }

    public static GameMenu install(
        final CommandManager commands,
        final EventNode<Event> events,
        final HubConfig config,
        final Map<String, GameMenu.GameConnector> connectors,
        final Path dataDir
    ) {
        MinestomCanvas.install(events);
        final var store = new SettingsStore(dataDir.resolve("settings"));
        final var menu = new GameMenu(config, connectors, store);

        final var menuCommand = new Command("menu");
        menuCommand.setDefaultExecutor((sender, context) -> {
            if (sender instanceof Player player) {
                MinestomCanvas.open(new Canvas(menu), player);
            }
        });
        final var settingsCommand = new Command("settings");
        settingsCommand.setDefaultExecutor((sender, context) -> {
            if (sender instanceof Player player) {
                MinestomCanvas.open(new Canvas(new SettingsMenu(menu, store)), player);
            }
        });
        commands.register(menuCommand, settingsCommand);
        return menu;
    }

    /** Ready-made hub config: Rush + Hunger Games, French labels. */
    public static HubConfig ganguiConfig() {
        return new HubConfig(
            Component.text("Gangui"),
            List.of(
                new HubConfig.GameEntry("rush", Component.text("Rush"),
                    Component.text("Detruis le lit adverse !")),
                new HubConfig.GameEntry("hg", Component.text("Hunger Games"),
                    Component.text("Survis dans l'arene !"))));
    }
}
