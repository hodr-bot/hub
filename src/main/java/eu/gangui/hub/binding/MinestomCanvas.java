package eu.gangui.hub.binding;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import me.chubbyduck.canvas.Canvas;
import me.chubbyduck.canvas.core.dialog.GlyphCompiler;
import me.chubbyduck.canvas.core.dialog.InputBinding;
import me.chubbyduck.canvas.core.dialog.PlainCompiler;
import me.chubbyduck.canvas.core.render.Renderer;
import me.chubbyduck.canvas.core.session.SessionManager;
import me.chubbyduck.canvas.core.session.SessionPlatform;
import me.chubbyduck.canvas.elements.Nodes;
import me.chubbyduck.canvas.elements.Themes;
import me.chubbyduck.canvas.node.Node;
import me.chubbyduck.canvas.pack.CanvasPack;
import me.chubbyduck.canvas.pack.CanvasPackBuilder;
import me.chubbyduck.canvas.session.Session;
import me.chubbyduck.canvas.spi.CanvasPlatform;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.pointer.Pointer;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerResourcePackStatusEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Minestom runtime for Canvas dialogs. Owns the SessionManager, converts
 * rendered dialogs, and routes client click echoes back into sessions.
 * Sends the Canvas glyph pack on first spawn (opt-in, like the HG era
 * pack); players who accept it get the styled rendering, the rest keep
 * the plain fallback.
 */
@NullMarked
public final class MinestomCanvas {

    private static final Logger LOG = Logger.getLogger("hub");
    private static final String PACK_URL =
        "https://github.com/hodr-bot/hub/releases/download/network-1/canvas-pack.zip";
    private static final UUID PACK_ID = UUID.nameUUIDFromBytes(
        "gangui-canvas".getBytes(StandardCharsets.UTF_8));
    private static final Set<UUID> PACK_LOADED = ConcurrentHashMap.newKeySet();
    private static SessionManager manager;
    private static volatile CanvasPack pack;

    private MinestomCanvas() {
    }

    public static synchronized void install(final EventNode<Event> events) {
        if (manager != null) {
            return;
        }
        final var packBuilder = new CanvasPackBuilder().registerTheme(Themes.ORE);
        for (final Class<? extends Node> type : Nodes.defaults()) {
            packBuilder.register(type);
        }
        final var pack = packBuilder.build();
        MinestomCanvas.pack = pack;
        LOG.info("CANVAS_PACK sha1=" + pack.sha1() + " bytes=" + pack.zip().length);
        final var platform = new SessionPlatform() {
            @Override
            public void show(
                final Audience viewer, final me.chubbyduck.canvas.core.dialog.RenderedDialog dialog) {
                playerOf(viewer).showDialog(MinestomDialogs.convert(dialog));
            }

            @Override
            public void close(final Audience viewer) {
                playerOf(viewer).closeDialog();
            }

            @Override
            public void schedule(final Audience viewer, final Runnable task) {
                net.minestom.server.MinecraftServer.getSchedulerManager().scheduleNextTick(task);
            }

            @Override
            public boolean supportsGlyphs(final Audience viewer) {
                return viewer.get(Identity.UUID).map(PACK_LOADED::contains).orElse(false);
            }
        };
        manager = new SessionManager(
            new Renderer(pack.atlas(), pack.registered()::contains),
            new GlyphCompiler(pack.atlas()),
            new PlainCompiler(),
            platform,
            theme -> theme == null ? Themes.ORE : theme,
            LOG);
        CanvasPlatform.install(manager);
        events.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                sendPack(event.getPlayer());
            }
        });
        events.addListener(PlayerResourcePackStatusEvent.class, event -> {
            if (!PACK_ID.equals(event.getPackUuid())) {
                return;
            }
            final var player = event.getPlayer();
            if (event.getStatus() == ResourcePackStatus.SUCCESSFULLY_LOADED) {
                PACK_LOADED.add(player.getUuid());
                manager.getCurrentSession(viewerOf(player)).ifPresent(Session::updateCanvas);
            } else if (!event.getStatus().intermediate()) {
                PACK_LOADED.remove(player.getUuid());
                LOG.warning("CANVAS_PACK status=" + event.getStatus()
                    + " player=" + player.getUsername());
            }
        });
        events.addListener(PlayerCustomClickEvent.class, event -> {
            if (!event.getKey().equals(MinestomDialogs.actionKey())) {
                return;
            }
            if (!(event.getPayload() instanceof CompoundBinaryTag payload)) {
                return;
            }
            final String session = MinestomDialogs.readText(payload, "s");
            final Float nonce = MinestomDialogs.readNumber(payload, "v");
            final String action = MinestomDialogs.readText(payload, "a");
            if (session == null || nonce == null || action == null) {
                return;
            }
            manager.handleClick(event.getPlayer().getUuid(), session, nonce.intValue(), action,
                MinestomCanvas.reader(payload));
        });
    }

    static InputBinding.InputReader reader(final CompoundBinaryTag payload) {
        return new InputBinding.InputReader() {
            @Override
            public @Nullable String text(final String key) {
                return MinestomDialogs.readText(payload, key);
            }

            @Override
            public @Nullable Boolean bool(final String key) {
                return MinestomDialogs.readBool(payload, key);
            }

            @Override
            public @Nullable Float number(final String key) {
                return MinestomDialogs.readNumber(payload, key);
            }
        };
    }

    /** Offers the Canvas glyph pack, opt-in like the HG era pack. */
    public static void sendPack(final Player player) {
        final var built = pack;
        if (built == null) {
            return;
        }
        try {
            final var info = ResourcePackInfo.resourcePackInfo()
                .id(PACK_ID)
                .uri(new URI(PACK_URL))
                .hash(built.sha1())
                .build();
            player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .required(false)
                .prompt(Component.text("Pack visuel Gangui — menus stylés (recommandé)"))
                .build());
        } catch (final Exception e) {
            LOG.warning("CANVAS_PACK send failed: " + e);
        }
    }

    /** Opens a Canvas screen for a player. */
    public static void open(final Canvas canvas, final Player player) {
        canvas.open(viewerOf(player));
    }

    public static Player playerOf(final Audience viewer) {
        final var found = viewer.get(Identity.UUID);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Canvas viewer has no UUID");
        }
        final var online = net.minestom.server.MinecraftServer.getConnectionManager()
            .getOnlinePlayerByUuid(found.get());
        if (online == null) {
            throw new IllegalArgumentException("Canvas viewer is offline");
        }
        return online;
    }

    static Audience viewerOf(final Player player) {
        final Audience backing = player;
        return new ForwardingAudience() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<T> get(final Pointer<T> id) {
                if (id == Identity.UUID) {
                    return Optional.of((T) player.getUuid());
                }
                return backing.get(id);
            }

            @Override
            public Iterable<? extends Audience> audiences() {
                return List.of(backing);
            }
        };
    }
}
