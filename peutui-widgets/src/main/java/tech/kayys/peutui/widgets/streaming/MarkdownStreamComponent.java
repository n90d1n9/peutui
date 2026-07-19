package tech.kayys.peutui.widgets.streaming;

import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Size;
import tech.kayys.peutui.core.text.AnsiCodes;

import java.util.List;

/**
 * A component that renders streaming Markdown content with proper formatting.
 * Integrates with the MarkdownStreamingRenderer for incremental updates.
 */
public class MarkdownStreamComponent implements Component {

    private final MarkdownStreamingRenderer renderer;
    private String title = "";
    private boolean showTitle = true;
    private int scrollOffset = 0;

    public MarkdownStreamComponent() {
        this.renderer = new MarkdownStreamingRenderer();
        setupDefaultCodeBlockHandler();
    }

    public MarkdownStreamComponent(MarkdownStreamingRenderer renderer) {
        this.renderer = renderer;
        setupDefaultCodeBlockHandler();
    }

    private void setupDefaultCodeBlockHandler() {
        renderer.setCodeBlockHandler(new MarkdownStreamingRenderer.CodeBlockHandler() {
            @Override
            public void onCodeBlockStart(String language) {
                // Could trigger syntax highlighting initialization
            }

            @Override
            public void onCodeLine(String line) {
                // Could accumulate for syntax highlighting
            }

            @Override
            public void onCodeBlockEnd() {
                // Could finalize syntax highlighting
            }
        });
    }

    /**
     * Feeds a streaming token to the renderer.
     */
    public void feed(StreamingToken token) {
        renderer.onData(token);
    }

    /**
     * Marks the stream as complete.
     */
    public void complete() {
        renderer.onComplete();
    }

    /**
     * Resets the component for a new stream.
     */
    public void reset() {
        renderer.reset();
        scrollOffset = 0;
    }

    /**
     * Sets an optional title to display above the content.
     */
    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    /**
     * Enables/disables title display.
     */
    public void setShowTitle(boolean show) {
        this.showTitle = show;
    }

    /**
     * Gets the underlying renderer for advanced configuration.
     */
    public MarkdownStreamingRenderer getRenderer() {
        return renderer;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public Size measure(BoxConstraints constraints) {
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    @Override
    public void render(RenderContext context) {
        var area = context.area();
        var buffer = context.buffer();
        int width = area.width();
        int height = area.height();

        List<String> lines = renderer.getRenderedLines();
        int totalLines = lines.size();

        // Calculate available height (reserve 1 line for title if shown)
        int contentHeight = showTitle && !title.isEmpty() ? height - 1 : height;
        int maxScroll = Math.max(0, totalLines - contentHeight);
        int effectiveOffset = Math.min(scrollOffset, maxScroll);

        // Render title if enabled
        if (showTitle && !title.isEmpty()) {
            String titleText = " " + title + " ";
            int titleWidth = Math.min(width - 2, titleText.length());
            String paddedTitle = "─".repeat((width - titleWidth) / 2) +
                    AnsiCodes.BOLD + AnsiCodes.CYAN + titleText + AnsiCodes.RESET +
                    "─".repeat(width - (width - titleWidth) / 2 - titleWidth);
            buffer.writeAt(area.x(), area.y(), paddedTitle);
        }

        // Calculate visible range
        int startLine = effectiveOffset;
        int endLine = Math.min(totalLines, startLine + contentHeight);

        // Render visible lines
        int startY = showTitle && !title.isEmpty() ? area.y() + 1 : area.y();
        for (int row = 0; row < contentHeight; row++) {
            int lineIdx = startLine + row;
            int bufferY = startY + row;

            if (lineIdx >= 0 && lineIdx < lines.size()) {
                String line = lines.get(lineIdx);
                // Pad line to fill width
                int visibleWidth = tech.kayys.peutui.core.text.TextMeasure.visibleWidth(line);
                int padding = width - visibleWidth;
                if (padding > 0) {
                    line += " ".repeat(padding);
                }
                buffer.writeAt(area.x(), bufferY, line);
            } else {
                // Empty line
                buffer.writeAt(area.x(), bufferY, " ".repeat(width));
            }
        }

        // Draw scroll indicator if content overflows
        if (totalLines > contentHeight) {
            renderScrollIndicator(buffer, area, totalLines, startLine, endLine, showTitle && !title.isEmpty());
        }
    }

    private void renderScrollIndicator(tech.kayys.peutui.core.buffer.ScreenBuffer buffer,
            tech.kayys.peutui.core.layout.Rect area,
            int totalLines, int startLine, int endLine,
            boolean hasTitle) {
        int contentHeight = hasTitle ? area.height() - 1 : area.height();
        float progress = (float) startLine / Math.max(1, totalLines - contentHeight);
        int indicatorHeight = Math.max(2, (int) ((contentHeight * contentHeight) / (float) totalLines));
        int indicatorPos = (int) (progress * (contentHeight - indicatorHeight));

        for (int i = 0; i < contentHeight; i++) {
            char ch;
            if (i >= indicatorPos && i < indicatorPos + indicatorHeight) {
                ch = '█';
            } else {
                ch = '░';
            }
            int yPos = hasTitle ? area.y() + 1 + i : area.y() + i;
            buffer.writeAt(area.x() + area.width() - 1, yPos, AnsiCodes.DIM + ch + AnsiCodes.RESET);
        }
    }

    @Override
    public boolean handleInput(InputEvent event) {
        if (!(event instanceof tech.kayys.peutui.core.event.KeyEvent key)) {
            return false;
        }

        return switch (key.code()) {
            case UP -> {
                scrollOffset = Math.max(0, scrollOffset - 1);
                yield true;
            }
            case DOWN -> {
                scrollOffset++;
                yield true;
            }
            case PAGE_UP -> {
                scrollOffset = Math.max(0, scrollOffset - 10);
                yield true;
            }
            case PAGE_DOWN -> {
                scrollOffset += 10;
                yield true;
            }
            case HOME -> {
                scrollOffset = 0;
                yield true;
            }
            case END -> {
                scrollOffset = Integer.MAX_VALUE;
                yield true;
            }
            default -> false;
        };
    }
}
