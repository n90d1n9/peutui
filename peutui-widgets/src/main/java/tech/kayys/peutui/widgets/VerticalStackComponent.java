package tech.kayys.peutui.widgets;

import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Rect;
import tech.kayys.peutui.core.layout.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * The simplest possible layout container: stacks children top-to-bottom,
 * giving each a fixed row count except for one designated "flexible" child
 * (typically a scrollable history view) which absorbs whatever vertical
 * space remains. Sufficient for a chat-style CLI without pulling in a full
 * flex-layout engine.
 */
public final class VerticalStackComponent implements Component {

    private record Entry(Component component, int fixedHeight, boolean flexible) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private Component focusTarget;

    public VerticalStackComponent addFixed(Component component, int height) {
        entries.add(new Entry(component, height, false));
        return this;
    }

    public VerticalStackComponent addFlexible(Component component) {
        entries.add(new Entry(component, 0, true));
        return this;
    }

    /**
     * Marks which child receives forwarded input events (App itself still owns
     * overall focus, this is a convenience passthrough).
     */
    public void setFocusTarget(Component component) {
        this.focusTarget = component;
    }

    @Override
    public Size measure(BoxConstraints constraints) {
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    @Override
    public void render(RenderContext context) {
        var area = context.area();
        int fixedTotal = entries.stream().filter(e -> !e.flexible()).mapToInt(Entry::fixedHeight).sum();
        int flexibleCount = (int) entries.stream().filter(Entry::flexible).count();
        int flexibleHeight = flexibleCount == 0 ? 0 : Math.max(0, area.height() - fixedTotal) / flexibleCount;

        int cursorY = area.y();
        for (Entry entry : entries) {
            int height = entry.flexible() ? flexibleHeight : entry.fixedHeight();
            if (height <= 0) {
                continue;
            }
            Rect childArea = new Rect(area.x(), cursorY, area.width(), height);
            entry.component().measure(BoxConstraints.tight(new Size(childArea.width(), childArea.height())));
            entry.component().render(context.withArea(childArea));
            cursorY += height;
        }
    }

    @Override
    public boolean handleInput(InputEvent event) {
        return focusTarget != null && focusTarget.handleInput(event);
    }

    @Override
    public boolean isFocusable() {
        return true;
    }
}
