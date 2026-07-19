package tech.kayys.peutui.core.event;

import tech.kayys.peutui.core.layout.Size;

/** Fired when the underlying terminal window is resized. */
public record ResizeEvent(Size newSize) implements InputEvent {
}
