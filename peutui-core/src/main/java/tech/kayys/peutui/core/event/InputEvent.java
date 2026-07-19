package tech.kayys.peutui.core.event;

/**
 * Marker interface for all events flowing through the render loop's input
 * pipeline.
 */
public sealed interface InputEvent permits KeyEvent, ResizeEvent, PasteEvent {
}
