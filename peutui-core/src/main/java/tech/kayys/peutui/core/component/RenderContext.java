package tech.kayys.peutui.core.component;

import tech.kayys.peutui.core.buffer.ScreenBuffer;
import tech.kayys.peutui.core.layout.Rect;

/**
 * Everything a {@link Component} needs to paint itself: the target buffer
 * and the rectangle it has been allocated within it, plus whether it
 * currently holds input focus (relevant for cursor rendering, highlighting).
 */
public record RenderContext(ScreenBuffer buffer, Rect area, boolean focused) {

    public RenderContext withArea(Rect newArea) {
        return new RenderContext(buffer, newArea, focused);
    }

    public RenderContext withFocused(boolean isFocused) {
        return new RenderContext(buffer, area, isFocused);
    }
}
