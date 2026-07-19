package tech.kayys.peutui.widgets;

import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Size;
import tech.kayys.peutui.core.text.AnsiCodes;
import tech.kayys.peutui.core.text.TextMeasure;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single-row status line showing left-aligned segments (e.g. current
 * project, session, agent, provider) and right-aligned segments (e.g. token
 * usage, connection state). Segments are set by key so callers can update
 * one without rebuilding the whole bar.
 */
public final class StatusBarComponent implements Component {

    private final Map<String, String> leftSegments = new LinkedHashMap<>();
    private final Map<String, String> rightSegments = new LinkedHashMap<>();

    public void setLeft(String key, String value) {
        leftSegments.put(key, value);
    }

    public void setRight(String key, String value) {
        rightSegments.put(key, value);
    }

    public void remove(String key) {
        leftSegments.remove(key);
        rightSegments.remove(key);
    }

    @Override
    public Size measure(BoxConstraints constraints) {
        return new Size(constraints.maxWidth(), 1);
    }

    @Override
    public void render(RenderContext context) {
        var area = context.area();
        var buffer = context.buffer();
        String left = String.join("  ", leftSegments.values());
        String right = String.join("  ", rightSegments.values());
        int leftWidth = TextMeasure.visibleWidth(left);
        int rightWidth = TextMeasure.visibleWidth(right);
        int gap = Math.max(1, area.width() - leftWidth - rightWidth);
        String row = AnsiCodes.INVERSE + left + " ".repeat(gap) + right + AnsiCodes.RESET;
        buffer.writeAt(area.x(), area.y(), TextMeasure.truncateToWidth(row, area.width(), "", true));
    }
}
