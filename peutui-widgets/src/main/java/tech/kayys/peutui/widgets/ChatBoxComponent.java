package tech.kayys.peutui.widgets;

import tech.kayys.peutui.agent.AgentMessage;
import tech.kayys.peutui.agent.MessageRole;
import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.event.KeyCode;
import tech.kayys.peutui.core.event.KeyEvent;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Size;
import tech.kayys.peutui.core.text.AnsiCodes;
import tech.kayys.peutui.core.text.TextMeasure;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An enhanced scrollable chat component with modern TUI features:
 * <ul>
 * <li>Smooth scrolling with home/end support</li>
 * <li>Timestamp display for each message</li>
 * <li>Avatar/indicator styling per role</li>
 * <li>Configurable message separators</li>
 * <li>Scroll position indicator</li>
 * <li>Copy-to-clipboard hint (visual only)</li>
 * <li>Auto-scroll to bottom on new messages (configurable)</li>
 * </ul>
 */
public final class ChatBoxComponent implements Component {

    private final List<ChatMessage> messages = new CopyOnWriteArrayList<>();
    private int scrollOffset = 0;
    private boolean autoScroll = true;
    private boolean showTimestamps = true;
    private boolean showSeparators = true;
    private boolean showScrollIndicator = true;
    private String emptyMessage = "No messages yet. Start the conversation!";

    // Role-based styling configuration
    private final Map<MessageRole, RoleStyle> roleStyles = new ConcurrentHashMap<>();

    public record ChatMessage(AgentMessage agentMessage, Instant timestamp) {
        public ChatMessage(AgentMessage agentMessage) {
            this(agentMessage, Instant.now());
        }
    }

    public record RoleStyle(String avatar, String nameColor, String messageBg, String borderColor) {
        public static RoleStyle user() {
            return new RoleStyle("●", AnsiCodes.BOLD + AnsiCodes.BLUE, "", AnsiCodes.BLUE);
        }

        public static RoleStyle assistant() {
            return new RoleStyle("○", AnsiCodes.BOLD + AnsiCodes.GREEN, "", AnsiCodes.GREEN);
        }

        public static RoleStyle system() {
            return new RoleStyle("⚙", AnsiCodes.DIM + AnsiCodes.YELLOW, AnsiCodes.DIM, AnsiCodes.YELLOW);
        }

        public static RoleStyle tool() {
            return new RoleStyle("⚡", AnsiCodes.DIM + AnsiCodes.CYAN, "", AnsiCodes.CYAN);
        }
    }

    public ChatBoxComponent() {
        // Initialize default role styles
        roleStyles.put(MessageRole.USER, RoleStyle.user());
        roleStyles.put(MessageRole.ASSISTANT, RoleStyle.assistant());
        roleStyles.put(MessageRole.SYSTEM, RoleStyle.system());
        roleStyles.put(MessageRole.TOOL, RoleStyle.tool());
    }

    /** Appends a message and optionally scrolls to bottom. */
    public void append(AgentMessage message) {
        messages.add(new ChatMessage(message));
        if (autoScroll) {
            scrollOffset = 0;
        }
    }

    /** Replaces the most recent message (for streaming updates). */
    public void replaceLast(AgentMessage message) {
        if (!messages.isEmpty()) {
            int idx = messages.size() - 1;
            ChatMessage existing = messages.get(idx);
            messages.set(idx, new ChatMessage(message, existing.timestamp()));
        } else {
            append(message);
        }
        if (autoScroll) {
            scrollOffset = 0;
        }
    }

    /** Clears all messages. */
    public void clear() {
        messages.clear();
        scrollOffset = 0;
    }

    /** Enables/disables auto-scroll to bottom on new messages. */
    public void setAutoScroll(boolean enabled) {
        this.autoScroll = enabled;
    }

    /** Shows/hides timestamps next to messages. */
    public void setShowTimestamps(boolean show) {
        this.showTimestamps = show;
    }

    /** Shows/hides separator lines between messages. */
    public void setShowSeparators(boolean show) {
        this.showSeparators = show;
    }

    /** Shows/hides the scroll position indicator. */
    public void setShowScrollIndicator(boolean show) {
        this.showScrollIndicator = show;
    }

    /** Sets the message shown when chat is empty. */
    public void setEmptyMessage(String message) {
        this.emptyMessage = message == null ? "" : message;
    }

    /** Customizes the style for a specific role. */
    public void setRoleStyle(MessageRole role, RoleStyle style) {
        if (role != null && style != null) {
            roleStyles.put(role, style);
        }
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

        if (messages.isEmpty()) {
            // Render empty state centered
            String emptyText = TextMeasure.truncateToWidth(emptyMessage, width - 4, "…", false);
            String padded = "  " + AnsiCodes.DIM + emptyText + AnsiCodes.RESET;
            int midRow = area.y() + height / 2;
            buffer.writeAt(area.x(), midRow, padded);
            return;
        }

        List<RenderedLine> renderedLines = renderAllMessages(width - 2);
        int totalLines = renderedLines.size();
        int maxScroll = Math.max(0, totalLines - height);
        int effectiveOffset = Math.min(scrollOffset, maxScroll);

        // Calculate visible range
        int startLine = Math.max(0, totalLines - height - effectiveOffset);
        int endLine = Math.min(totalLines, startLine + height);

        // Render visible lines with left padding and optional right border
        for (int row = 0; row < height; row++) {
            int lineIdx = startLine + row;
            int bufferX = area.x() + 1;
            int bufferY = area.y() + row;

            if (lineIdx >= 0 && lineIdx < renderedLines.size()) {
                RenderedLine line = renderedLines.get(lineIdx);
                String content = applyStyle(line.content(), line.style());

                // Pad line to fill width
                int visibleWidth = TextMeasure.visibleWidth(content);
                int padding = width - 2 - visibleWidth;
                if (padding > 0) {
                    content += " ".repeat(padding);
                }

                // Add left margin and right border
                String fullLine = " " + content + " ";
                buffer.writeAt(area.x(), bufferY, fullLine);

                // Draw separator if applicable
                if (showSeparators && line.isSeparatorAfter()) {
                    String sepLine = AnsiCodes.DIM + "─".repeat(width) + AnsiCodes.RESET;
                    if (row + 1 < height) {
                        buffer.writeAt(area.x(), bufferY + 1, sepLine);
                    }
                }
            } else {
                // Empty line
                buffer.writeAt(area.x(), bufferY, " ".repeat(width));
            }
        }

        // Draw left border for all rows
        for (int row = 0; row < height; row++) {
            buffer.writeAt(area.x(), area.y() + row, AnsiCodes.DIM + "│" + AnsiCodes.RESET);
        }

        // Draw scroll indicator if enabled
        if (showScrollIndicator && totalLines > height) {
            renderScrollIndicator(buffer, area, totalLines, startLine, endLine);
        }
    }

    private void renderScrollIndicator(tech.kayys.peutui.core.buffer.ScreenBuffer buffer,
            tech.kayys.peutui.core.layout.Rect area,
            int totalLines, int startLine, int endLine) {
        float progress = (float) startLine / (totalLines - area.height());
        int indicatorHeight = Math.max(2, (int) ((area.height() * area.height()) / (float) totalLines));
        int indicatorPos = (int) (progress * (area.height() - indicatorHeight));

        for (int i = 0; i < area.height(); i++) {
            char ch;
            String style;
            if (i >= indicatorPos && i < indicatorPos + indicatorHeight) {
                ch = '█';
                style = AnsiCodes.DIM;
            } else {
                ch = '░';
                style = AnsiCodes.DIM;
            }
            buffer.writeAt(area.x() + area.width() - 1, area.y() + i, style + ch + AnsiCodes.RESET);
        }
    }

    private String applyStyle(String text, String style) {
        if (style == null || style.isEmpty()) {
            return text;
        }
        return style + text + AnsiCodes.RESET;
    }

    private record RenderedLine(String content, String style, boolean isSeparatorAfter) {
    }

    private List<RenderedLine> renderAllMessages(int width) {
        List<RenderedLine> out = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault());

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage chatMsg = messages.get(i);
            AgentMessage msg = chatMsg.agentMessage();
            RoleStyle style = roleStyles.getOrDefault(msg.role(), RoleStyle.assistant());

            // Build header line: avatar + name + timestamp
            StringBuilder header = new StringBuilder();
            header.append(style.avatar()).append(" ");

            String roleName = switch (msg.role()) {
                case USER -> "You";
                case ASSISTANT -> msg.agentId() != null ? msg.agentId() : "Assistant";
                case SYSTEM -> "System";
                case TOOL -> "Tool";
            };
            header.append(style.nameColor()).append(roleName).append(AnsiCodes.RESET);

            if (showTimestamps) {
                String timestamp = formatter.format(chatMsg.timestamp());
                header.append(AnsiCodes.DIM + "  " + timestamp + AnsiCodes.RESET);
            }

            // Wrap and add header
            out.add(new RenderedLine(header.toString(), "", false));

            // Wrap message content
            String content = msg.content();
            if (content != null && !content.isEmpty()) {
                List<String> wrapped = wrapText(content, width);
                for (String line : wrapped) {
                    out.add(new RenderedLine(line, style.messageBg(), false));
                }
            }

            // Add separator after message if enabled and not last
            boolean isLast = i == messages.size() - 1;
            out.add(new RenderedLine("", "", showSeparators && !isLast));
        }

        return out;
    }

    private List<String> wrapText(String text, int width) {
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

            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }

        return lines;
    }

    @Override
    public boolean handleInput(InputEvent event) {
        if (!(event instanceof KeyEvent key)) {
            return false;
        }

        int viewportHeight = 10; // Will be updated during render

        return switch (key.code()) {
            case UP -> {
                scrollOffset = Math.max(0, scrollOffset - 1);
                yield true;
            }
            case DOWN -> {
                scrollOffset = Math.max(0, scrollOffset + 1);
                yield true;
            }
            case PAGE_UP -> {
                scrollOffset = Math.max(0, scrollOffset - viewportHeight);
                yield true;
            }
            case PAGE_DOWN -> {
                scrollOffset = Math.max(0, scrollOffset + viewportHeight);
                yield true;
            }
            case HOME -> {
                scrollOffset = Integer.MAX_VALUE;
                yield true;
            }
            case END -> {
                scrollOffset = 0;
                yield true;
            }
            default -> false;
        };
    }
}
