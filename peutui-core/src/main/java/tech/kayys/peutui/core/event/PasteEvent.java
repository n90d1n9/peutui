package tech.kayys.peutui.core.event;

/**
 * Fired when the terminal reports a bracketed-paste block, delivered as a
 * single atomic event.
 */
public record PasteEvent(String text) implements InputEvent {
}
