package eu.gangui.hub.binding;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinestomDialogsTest {

    @Test
    void payloadRoundTrip() {
        final var parsed = MinestomDialogs.parse(
            me.chubbyduck.canvas.core.dialog.RenderedDialog.payload("abc", 7, "join-rush"));
        assertEquals("abc", MinestomDialogs.readText(parsed, "s"));
        assertEquals(7f, MinestomDialogs.readNumber(parsed, "v"));
        assertEquals("join-rush", MinestomDialogs.readText(parsed, "a"));
    }

    @Test
    void garbagePayloadParsesEmpty() {
        final CompoundBinaryTag parsed = MinestomDialogs.parse("not-snbt{{{");
        assertNull(MinestomDialogs.readText(parsed, "s"));
    }

    @Test
    void convertsPlainDialogWithButtons() {
        final var rendered = new me.chubbyduck.canvas.core.dialog.RenderedDialog(
            Component.text("Gangui"),
            java.util.List.of(new me.chubbyduck.canvas.core.dialog.RenderedDialog.Body(
                Component.text("Choisis ton jeu :"), 300)),
            java.util.List.of(),
            java.util.List.of(new me.chubbyduck.canvas.core.dialog.RenderedDialog.Button(
                Component.text("Rush"), null, 150,
                me.chubbyduck.canvas.core.dialog.RenderedDialog.payload("s1", 1, "join"))),
            2,
            true,
            null);
        final var dialog = MinestomDialogs.convert(rendered);
        assertInstanceOf(net.minestom.server.dialog.Dialog.MultiAction.class, dialog);
        final var multi = (net.minestom.server.dialog.Dialog.MultiAction) dialog;
        assertEquals(1, multi.actions().size());
        assertEquals(Component.text("Gangui"), multi.metadata().title());
    }
}
