package eu.gangui.hub.binding;

import java.util.ArrayList;
import java.util.List;
import me.chubbyduck.canvas.core.dialog.RenderedDialog;
import me.chubbyduck.canvas.paint.DialogInput;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogActionButton;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.dialog.DialogBody;
import net.minestom.server.dialog.DialogMetadata;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Converts Canvas rendered dialogs to Minestom dialogs and parses the
 * click payload echo. Buttons use dynamic custom actions so the client
 * merges native input values into the payload compound.
 */
@NullMarked
public final class MinestomDialogs {

    private MinestomDialogs() {
    }

    static Dialog convert(final RenderedDialog dialog) {
        final var meta = new DialogMetadata(
            dialog.title(),
            dialog.title(),
            dialog.closeOnEscape(),
            false,
            DialogAfterAction.NONE,
            dialog.body().stream()
                .map(body -> (net.minestom.server.dialog.DialogBody) new DialogBody.PlainMessage(body.text(), body.width()))
                .toList(),
            dialog.inputs().stream()
                .map(input -> convertInput(input.key(), input.input()))
                .flatMap(List::stream)
                .toList());
        final List<DialogActionButton> buttons = new ArrayList<>();
        for (final RenderedDialog.Button button : dialog.buttons()) {
            buttons.add(new DialogActionButton(
                button.label(), button.tooltip(), button.width(), actionOf(button.payload())));
        }
        final DialogActionButton exit;
        if (dialog.exit() == null) {
            exit = null;
        } else {
            exit = new DialogActionButton(
                dialog.exit().label(), dialog.exit().tooltip(), dialog.exit().width(),
                actionOf(dialog.exit().payload()));
        }
        if (exit != null && buttons.isEmpty()) {
            return new Dialog.Notice(meta, exit);
        }
        return new Dialog.MultiAction(meta, buttons, exit, dialog.columns());
    }

    private static DialogAction actionOf(final String payload) {
        return new DialogAction.DynamicCustom(RenderedDialog.ACTION, parse(payload));
    }

    static CompoundBinaryTag parse(final String payload) {
        // RenderedDialog.payload emits {s:"<session>",v:<nonce>,a:"<action>"}.
        try {
            final String s = stringField(payload, "s:\"");
            final String a = stringField(payload, "a:\"");
            final int vStart = payload.indexOf("v:") + 2;
            final int vEnd = payload.indexOf(',', vStart) < 0
                ? payload.indexOf('}', vStart) : payload.indexOf(',', vStart);
            final int v = Integer.parseInt(payload.substring(vStart, vEnd).trim());
            if (s == null || a == null) {
                return CompoundBinaryTag.empty();
            }
            return CompoundBinaryTag.builder()
                .putString("s", s)
                .putInt("v", v)
                .putString("a", a)
                .build();
        } catch (final RuntimeException e) {
            return CompoundBinaryTag.empty();
        }
    }

    private static @Nullable String stringField(final String payload, final String marker) {
        final int start = payload.indexOf(marker);
        if (start < 0) {
            return null;
        }
        final int end = payload.indexOf('"', start + marker.length());
        if (end < 0) {
            return null;
        }
        return payload.substring(start + marker.length(), end);
    }

    private static List<net.minestom.server.dialog.DialogInput> convertInput(
        final String key, final DialogInput input) {
        return switch (input) {
            case DialogInput.Bool bool ->
                List.of(new net.minestom.server.dialog.DialogInput.Boolean(
                    key, bool.label(), bool.initial(), "true", "false"));
            case DialogInput.Range range ->
                List.of(new net.minestom.server.dialog.DialogInput.NumberRange(
                    key, range.width(), range.label(), range.labelFormat(),
                    range.start(), range.end(), range.step(), range.initial()));
            case DialogInput.Options options -> {
                final var mine = new ArrayList<net.minestom.server.dialog.DialogInput.SingleOption.Option>();
                for (final DialogInput.Options.Option option : options.options()) {
                    mine.add(new net.minestom.server.dialog.DialogInput.SingleOption.Option(
                        option.id(), option.display(), option.id().equals(options.initialId())));
                }
                yield List.of(new net.minestom.server.dialog.DialogInput.SingleOption(
                    key, options.width(), mine, options.label(), options.labelVisible()));
            }
            case DialogInput.Text text ->
                List.of(new net.minestom.server.dialog.DialogInput.Text(
                    key, text.width(), text.label(), text.labelVisible(),
                    text.initial(), text.maxLength(), null));
        };
    }

    static @Nullable String readText(final CompoundBinaryTag payload, final String key) {
        final var tag = payload.get(key);
        if (tag instanceof net.kyori.adventure.nbt.StringBinaryTag s) {
            return s.value();
        }
        return null;
    }

    static @Nullable Boolean readBool(final CompoundBinaryTag payload, final String key) {
        final var tag = payload.get(key);
        if (tag instanceof net.kyori.adventure.nbt.ByteBinaryTag b) {
            return b.value() != 0;
        }
        return null;
    }

    static @Nullable Float readNumber(final CompoundBinaryTag payload, final String key) {
        final var tag = payload.get(key);
        if (tag instanceof net.kyori.adventure.nbt.NumberBinaryTag n) {
            return n.floatValue();
        }
        return null;
    }

    static Key actionKey() {
        return RenderedDialog.ACTION;
    }
}
