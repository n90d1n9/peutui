package tech.kayys.peutui.widgets;

import tech.kayys.peutui.agent.AgentMessage;
import tech.kayys.peutui.agent.MessageRole;
import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.event.KeyEvent;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Size;
import tech.kayys.peutui.core.text.AnsiCodes;
import tech.kayys.peutui.core.text.TextMeasure;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A scrollable, auto-wrapping message list. Word-wraps each message to the
 * allocated width, tags assistant messages with their {@code agentId} when
 * more than one agent is present (multi-agent transcripts), and supports
 * Page Up/Down + arrow scrolling once content exceeds the viewport.
 */
public final class ChatHistoryComponent implements Component {

    private final List<AgentMessage> messages = new CopyOnWriteArrayList<>();
    private int scrollOffset = 0;
    private boolean showAgentTags = false;

    public void append(AgentMessage message) {
        messages.add(message);
        scrollOffset = 0; // stick to bottom on new content
    }

    /**
     * Replaces the most recently appended message, used to render incremental
     * streaming updates in place.
     */
    public void replaceLast(AgentMessage message) {
        if (!messages.isEmpty()) {
            messages.set(messages.size() - 1, message);
        } else {
            messages.add(message);
        }
        scrollOffset = 0;
    }

    public void setShowAgentTags(boolean show) {
        this.showAgentTags = show;
    }

    public void clear() {
        messages.clear();
        scrollOffset = 0;
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
        List<String> wrapped = wrapAll(area.width());
        int totalLines = wrapped.size();
        int viewportHeight = area.height();
        int maxScroll = Math.max(0, totalLines - viewportHeight);
        int effectiveOffset = Math.min(scrollOffset, maxScroll);
        int startLine = Math.max(0, totalLines - viewportHeight - effectiveOffset);

        for (int row = 0; row < viewportHeight; row++) {
            int lineIdx = startLine + row;
            String content = lineIdx < totalLines ? wrapped.get(lineIdx) : "";
            buffer.writeAt(area.x(), area.y() + row, content);
        }
    }

    private List<String> wrapAll(int width) {
        List<String> out = new ArrayList<>();
        for (AgentMessage message : messages) {
            String tag = roleTag(message);
            String body = tag + message.content();
            out.addAll(wrap(body, width));
            out.add(""); // blank separator line between messages
        }
        return out;
    }

    private String roleTag(AgentMessage message) {
        String base = switch (message.role()) {
            case USER -> AnsiCodes.BOLD + "you" + AnsiCodes.RESET;
            case ASSISTANT -> AnsiCodes.BOLD
                    + (showAgentTags && message.agentId() != null ? message.agentId() : "assistant") + AnsiCodes.RESET;
            case SYSTEM -> AnsiCodes.DIM + "system" + AnsiCodes.RESET;
            case TOOL -> AnsiCodes.DIM + "tool" + AnsiCodes.RESET;
        };
        return base + ": ";
    }

    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        if (width <= 0) {
            return lines;
        }
        for (String paragraph : text.split("\n", -1)) {
            if (TextMeasure.visibleWidth(paragraph) <= width) {
                lines.add(paragraph);
                continue;
            }
            StringBuilder current = new StringBuilder();
            int currentWidth = 0;
            for (String word : paragraph.split(" ")) {
                int wordWidth = TextMeasure.visibleWidth(word);
                int extra = current.length() == 0 ? 0 : 1;
                if (currentWidth + extra + wordWidth > width && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder();
                    currentWidth = 0;
                }
                if (current.length() > 0) {
                    current.append(' ');
                    currentWidth++;
                }
                current.append(word);
                currentWidth += wordWidth;
            }
            lines.add(current.toString());
        }
        return lines;
    }

    @Override
    public boolean handleInput(InputEvent event) {
        if (!(event instanceof KeyEvent key)) {
            return false;
        }
        return switch (key.code()) {
            case UP -> {
                scrollOffset++;
                yield true;
            }
            case DOWN -> {
                scrollOffset = Math.max(0, scrollOffset - 1);
                yield true;
            }
            case PAGE_UP -> {
                scrollOffset += 10;
                yield true;
            }
            case PAGE_DOWN -> {
                scrollOffset = Math.max(0, scrollOffset - 10);
                yield true;
            }
            default -> false;
        };
    }
}
