package eu.gangui.hub;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.timer.TaskSchedule;
import org.jspecify.annotations.NullMarked;

/**
 * Hub welcome pack: title, chat message, tab list, boss bar, join sound,
 * join/quit feed, overhead nametag and sidebar scoreboard.
 * Text follows the network language files (hub.* keys, FR-first).
 */
@NullMarked
public final class HubWelcome {

	private static final MiniMessage MM = MiniMessage.miniMessage();

	private static final String MEMBER = "<#85a2d5>";
	private static final String ACCENT = "<#fc8b4f>";

	private static final Map<UUID, Sidebar> BOARDS = new ConcurrentHashMap<>();

	private HubWelcome() {
	}

	/** Registers hub chat format and quit feed. */
	public static void install(final GlobalEventHandler events) {
		events.addListener(PlayerChatEvent.class, event -> {
			Component head = MM.deserialize(
				MEMBER + event.getPlayer().getUsername() + " <dark_gray>• ");
			event.setFormattedMessage(head.append(Component.text(event.getRawMessage())));
		});
		events.addListener(PlayerDisconnectEvent.class, event -> {
			Sidebar board = BOARDS.remove(event.getPlayer().getUuid());
			if (board != null) {
				board.removeViewer(event.getPlayer());
			}
			Component quit = MM.deserialize(
				"<red>" + event.getPlayer().getUsername() + " a quitté le hub.");
			broadcast(quit);
		});
	}

	/** Full welcome sequence for a player arriving in the hub. */
	// setCustomName has no replacement in this Minestom pin (overhead nametag).
	@SuppressWarnings("deprecation")
	public static void greet(final Player player) {
		String name = player.getUsername();
		player.showTitle(Title.title(
			MM.deserialize(ACCENT + "<b>ᴡᴇʟᴄᴏᴍᴇ"),
			MM.deserialize("<white>mc.gangui.eu")));
		player.sendActionBar(MM.deserialize("<yellow>ᴍᴄ.ɢᴀɴɢᴜɪ.ᴇᴜ"));
		player.sendMessage(MM.deserialize(
			"<gray>Ton avis sur les bugs ou suggestions compte : partage-le sur notre "
				+ "<blue>ᴅɪѕᴄᴏʀᴅ <dark_gray>• <white>https://discord.gg/7Kj8vsGdSr<gray>. "
				+ "Fais <blue>/menu<gray> pour jouer, <blue>/help<gray> pour les commandes."));
		player.sendPlayerListHeaderAndFooter(
			MM.deserialize(ACCENT + "<b>ɢᴀɴɢᴜɪ ɴᴇᴛᴡᴏʀᴋ"),
			MM.deserialize("<newline><yellow>ɢᴀɴɢᴜɪ.ᴇᴜ"));
		player.setDisplayName(MM.deserialize(MEMBER + name));
		player.setCustomName(MM.deserialize("<white>" + MEMBER + name));
		player.setCustomNameVisible(true);
		BossBar bar = BossBar.bossBar(
			MM.deserialize("<white>Rejoins notre <blue>/discord !"),
			1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
		player.showBossBar(bar);
		MinecraftServer.getSchedulerManager().buildTask(() -> player.hideBossBar(bar))
			.delay(TaskSchedule.seconds(15)).schedule();
		player.playSound(Sound.sound(
			Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 0.4f, 1.0f));
		Sidebar board = new Sidebar(MM.deserialize(ACCENT + "<b>HUB"));
		board.createLine(new Sidebar.ScoreboardLine("name",
			MM.deserialize("<white>Pseudo <dark_gray>• " + ACCENT + name), 3));
		board.createLine(new Sidebar.ScoreboardLine("rank",
			MM.deserialize("<white>Rang <dark_gray>• <white>Joueur"), 2));
		board.createLine(new Sidebar.ScoreboardLine("server",
			MM.deserialize("<yellow>mc.gangui.eu"), 1));
		BOARDS.put(player.getUuid(), board);
		board.addViewer(player);
		broadcast(MM.deserialize("<green>" + name + " a rejoint le hub."));
	}

	/** Sends a message to every online player. */
	private static void broadcast(final Component message) {
		for (Player online : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
			online.sendMessage(message);
		}
	}
}
