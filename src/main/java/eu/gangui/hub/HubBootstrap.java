package eu.gangui.hub;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.timer.TaskSchedule;
import eu.gangui.hub.binding.MinestomCanvas;
import eu.gangui.hub.menu.GameMenu;
import me.chubbyduck.canvas.Canvas;
import org.jspecify.annotations.NullMarked;

/**
 * Standalone hub backend for the Gangui network.
 * Serves the hub map (End sky), opens the game menu on arrival, and moves
 * players to game backends through the proxy ({@code bungeecord:main}
 * {@code Connect} channel). Always runs behind the proxy: boots with modern
 * forwarding and fails fast without {@code VELOCITY_SECRET}.
 * Env: {@code VELOCITY_SECRET} (required), {@code BIND_HOST} (default
 * 127.0.0.1), {@code PORT} (default 25565), {@code HUB_WORLD} (required,
 * world dir with {@code region/}), {@code HUB_SPAWN} (default
 * 26.5,70.0,19.5), {@code DATA_DIR} (default data).
 */
@NullMarked
public final class HubBootstrap {

	static InstanceContainer HUB;
	static Pos SPAWN = new Pos(26.5, 70.0, 19.5);
	static GameMenu MENU;

	private HubBootstrap() {
	}

	public static void main(final String[] args) {
		String secret = System.getenv("VELOCITY_SECRET");
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
				"VELOCITY_SECRET is not set: the hub only runs behind the proxy");
		}
		String spawnRaw = System.getenv("HUB_SPAWN");
		if (spawnRaw != null && !spawnRaw.isBlank()) {
			String[] parts = spawnRaw.split(",");
			SPAWN = new Pos(Double.parseDouble(parts[0].trim()),
				Double.parseDouble(parts[1].trim()), Double.parseDouble(parts[2].trim()));
		}
		MinecraftServer server = MinecraftServer.init(new net.minestom.server.Auth.Velocity(secret));
		openHub();
		var events = MinecraftServer.getGlobalEventHandler();
		MENU = HubModule.install(MinecraftServer.getCommandManager(), events,
			HubModule.ganguiConfig(),
			Map.of("rush", player -> transfer(player, "rush"),
				"hg", player -> transfer(player, "hg")),
			dataDir());
		events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
			event.setSpawningInstance(HUB);
			event.getPlayer().setRespawnPoint(SPAWN);
		});
		HubWelcome.install(events);
		events.addListener(PlayerSpawnEvent.class, event -> {
			if (!event.isFirstSpawn()) {
				return;
			}
			Player player = event.getPlayer();
			player.setGameMode(GameMode.ADVENTURE);
			player.teleport(SPAWN);
			HubWelcome.greet(player);
			MinecraftServer.getSchedulerManager().buildTask(() -> {
				Player online = MinecraftServer.getConnectionManager()
					.getOnlinePlayerByUuid(player.getUuid());
				if (online != null && online.getInstance() == HUB) {
					MinestomCanvas.open(new Canvas(MENU), online);
				}
			}).delay(TaskSchedule.tick(20)).schedule();
		});
		String host = System.getenv("BIND_HOST");
		String portRaw = System.getenv("PORT");
		server.start(host == null || host.isBlank() ? "127.0.0.1" : host,
			portRaw == null || portRaw.isBlank() ? 25565 : Integer.parseInt(portRaw.trim()));
	}

	/** Sends the player to another network backend through the proxy. */
	static void transfer(final Player player, final String server) {
		player.sendPluginMessage("bungeecord:main", NetworkBuffer.makeArray(buffer -> {
			buffer.write(NetworkBuffer.STRING_IO_UTF8, "Connect");
			buffer.write(NetworkBuffer.STRING_IO_UTF8, server);
		}));
		System.out.println("HUB_TRANSFER " + player.getUsername() + " -> " + server);
	}

	/** Loads the hub world (flat single-dimension layout, End sky). */
	@SuppressWarnings("removal")
	static void openHub() {
		String raw = System.getenv("HUB_WORLD");
		if (raw == null || raw.isBlank()) {
			throw new IllegalStateException(
				"HUB_WORLD is not set: point it at the hub world (region/ dir)");
		}
		Path hub = Path.of(raw);
		if (!Files.isDirectory(hub.resolve("region"))) {
			throw new IllegalStateException(
				"HUB_WORLD has no region/ dir: " + hub.toAbsolutePath());
		}
		var endKey = MinecraftServer.getDimensionTypeRegistry()
			.getKey(Key.key("minecraft:the_end"));
		if (endKey == null) {
			throw new IllegalStateException("no minecraft:the_end dimension type registered");
		}
		HUB = MinecraftServer.getInstanceManager().createInstanceContainer(
			endKey, new AnvilLoader(hub));
		HUB.setChunkSupplier(LightingChunk::new);
		HUB.loadChunk(SPAWN.chunkX(), SPAWN.chunkZ()).join();
		var lit = LightingChunk.relight(HUB, HUB.getChunks());
		System.out.println("HUB_WORLD chunks=" + lit.size()
			+ " spawn=" + SPAWN.x() + "," + SPAWN.y() + "," + SPAWN.z());
	}

	static Path dataDir() {
		String raw = System.getenv("DATA_DIR");
		return Path.of(raw == null || raw.isBlank() ? "data" : raw);
	}
}
