package tech.kayys.peutui.core.component;

import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Size;

/**
 * The base unit of the render tree. Analogous to a widget in Ink/React or a
 * View in a native UI toolkit, but intentionally minimal: two-phase
 * measure-then-paint, plus optional input handling for interactive/focused
 * components.
 *
 * <p>
 * Implementations should be stateless where possible and instead read
 * from injected state holders, so the tree can be rebuilt cheaply on every
 * frame - the {@link tech.kayys.peutui.core.buffer.ScreenBuffer} diff takes
 * care of only writing what actually changed to the real terminal.
 */
public interface Component {

    /**
     * Computes the size this component wants to occupy, given the available
     * constraints.
     */
    Size measure(BoxConstraints constraints);

    /**
     * Paints this component's content into {@code context.buffer()} within
     * {@code context.area()}.
     */
    void render(RenderContext context);

    /**
     * Handles an input event while this component holds focus.
     *
     * @return true if the event was consumed and should not propagate further
     */
    default boolean handleInput(InputEvent event) {
        return false;
    }

    /**
     * Whether this component can receive focus (and therefore input events) at all.
     */
    default boolean isFocusable() {
        return false;
    }
}
