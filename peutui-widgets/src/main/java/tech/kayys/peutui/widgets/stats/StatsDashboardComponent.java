package tech.kayys.peutui.widgets.stats;

import tech.kayys.peutui.core.buffer.ScreenBuffer;
import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.event.KeyCode;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Size;
import tech.kayys.peutui.core.text.AnsiCodes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * TUI component that displays a dashboard of chat statistics and performance
 * metrics.
 * Shows daily/monthly token usage, average performance metrics, and recent
 * activity.
 * 
 * This component is designed to be embedded in a modal dialog, side panel, or
 * dedicated dashboard view. Developers can customize the display by extending
 * this class or creating their own implementations.
 */
public class StatsDashboardComponent implements Component {

    private final ChatStatsTracker statsTracker;
    private boolean showDetails = false;
    private int scrollOffset = 0;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Colors
    private static final String HEADER_COLOR = AnsiCodes.CYAN + AnsiCodes.BRIGHT_CYAN;
    private static final String LABEL_COLOR = AnsiCodes.YELLOW;
    private static final String VALUE_COLOR = AnsiCodes.GREEN + AnsiCodes.BRIGHT_GREEN;
    private static final String SUBTLE_COLOR = AnsiCodes.BRIGHT_WHITE;
    private static final String WARNING_COLOR = AnsiCodes.RED + AnsiCodes.BRIGHT_RED;
    private static final String RESET = AnsiCodes.RESET;

    public StatsDashboardComponent(ChatStatsTracker statsTracker) {
        this.statsTracker = statsTracker;
    }

    @Override
    public Size measure(BoxConstraints constraints) {
        // Dashboard wants to use all available space, minimum 40x15
        int width = Math.max(40, constraints.maxWidth());
        int height = Math.max(15, constraints.maxHeight());
        return new Size(width, height);
    }

    /**
     * Toggle detailed view mode.
     */
    public void toggleDetails() {
        showDetails = !showDetails;
        scrollOffset = 0;
    }

    /**
     * Scroll up in detailed view.
     */
    public void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
        }
    }

    /**
     * Scroll down in detailed view.
     */
    public void scrollDown(int maxScroll) {
        if (scrollOffset < maxScroll) {
            scrollOffset++;
        }
    }

    @Override
    public void render(RenderContext context) {
        ScreenBuffer buffer = context.buffer();
        var area = context.area();
        int width = area.width();
        int height = area.height();
        int x = area.x();
        int y = area.y();

        if (width < 40 || height < 15) {
            buffer.writeAt(x, y, AnsiCodes.RED + "Dashboard requires minimum 40x15 terminal" + RESET);
            return;
        }

        int row = y;

        // Header
        String title = "╭─ " + HEADER_COLOR + "📊 CHAT STATISTICS DASHBOARD" + RESET + " ─";
        buffer.writeAt(x, row++, title);
        buffer.writeAt(x, row++, "│");

        // Today's Summary
        ChatStatsTracker.DailyUsageSummary today = statsTracker.getTodayUsage();
        buffer.writeAt(x, row++, "│  " + LABEL_COLOR + "Today (" + today.date() + ")" + RESET);
        buffer.writeAt(x, row++, "│    Tokens: " + VALUE_COLOR + formatNumber(today.totalTokens()) + RESET +
                " (In: " + formatNumber(today.inputTokens()) +
                ", Out: " + formatNumber(today.outputTokens()) + ")");
        if (today.totalCost() > 0) {
            buffer.writeAt(x, row++,
                    "│    Cost: " + VALUE_COLOR + "$" + String.format("%.4f", today.totalCost()) + RESET +
                            " | Sessions: " + VALUE_COLOR + today.sessionCount() + RESET);
        }
        buffer.writeAt(x, row++, "│");

        // Monthly Summary
        ChatStatsTracker.MonthlyUsageSummary month = statsTracker.getCurrentMonthUsage();
        buffer.writeAt(x, row++, "│  " + LABEL_COLOR + "This Month (" + month.yearMonth() + ")" + RESET);
        buffer.writeAt(x, row++, "│    Tokens: " + VALUE_COLOR + formatNumber(month.totalTokens()) + RESET +
                " | Avg/Day: " + VALUE_COLOR + formatNumber((long) month.avgTokensPerDay()) + RESET);
        if (month.totalCost() > 0) {
            buffer.writeAt(x, row++,
                    "│    Cost: " + VALUE_COLOR + "$" + String.format("%.4f", month.totalCost()) + RESET);
        }
        buffer.writeAt(x, row++, "│");

        // Performance Metrics
        ChatStatsTracker.AggregatedMetrics metrics = statsTracker.getAverageMetrics();
        buffer.writeAt(x, row++, "│  " + LABEL_COLOR + "Performance (avg)" + RESET);
        if (metrics.sampleCount() > 0) {
            buffer.writeAt(x, row++, "│    Samples: " + VALUE_COLOR + metrics.sampleCount() + RESET);
            buffer.writeAt(x, row++,
                    "│    TTFT: " + VALUE_COLOR + String.format("%.2f ms", metrics.avgTtftMs()) + RESET +
                            " | Speed: " + VALUE_COLOR + String.format("%.2f t/s", metrics.avgTokensPerSecond())
                            + RESET);
            buffer.writeAt(x, row++,
                    "│    Duration: " + VALUE_COLOR + String.format("%.2f ms", metrics.avgDurationMs()) + RESET);
            buffer.writeAt(x, row++,
                    "│    Total Generated: " + VALUE_COLOR + formatNumber(metrics.totalGeneratedTokens()) + RESET +
                            " | Prompt: " + formatNumber(metrics.totalPromptTokens()));
        } else {
            buffer.writeAt(x, row++, "│    " + SUBTLE_COLOR + "No performance data yet" + RESET);
        }
        buffer.writeAt(x, row++, "│");

        // Daily Breakdown (if space allows)
        if (height > 20 && row < y + height - 3) {
            buffer.writeAt(x, row++, "│  " + LABEL_COLOR + "Recent Days" + RESET);
            Map<LocalDate, ChatStatsTracker.DailyUsageSummary> daily = statsTracker.getDailyUsage();
            List<LocalDate> dates = daily.keySet().stream()
                    .sorted((a, b) -> b.compareTo(a)) // Most recent first
                    .limit(5)
                    .toList();

            if (dates.isEmpty()) {
                buffer.writeAt(x, row++, "│    " + SUBTLE_COLOR + "No usage data yet" + RESET);
            } else {
                for (LocalDate date : dates) {
                    if (row >= y + height - 2)
                        break;
                    ChatStatsTracker.DailyUsageSummary summary = daily.get(date);
                    String dateStr = date.equals(LocalDate.now()) ? "Today" : dateFormatter.format(date);
                    buffer.writeAt(x, row++, "│    " + dateStr + ": " + VALUE_COLOR +
                            formatNumber(summary.totalTokens()) + RESET + " tokens");
                }
            }
            buffer.writeAt(x, row++, "│");
        }

        // Footer
        buffer.writeAt(x, row++,
                "│  " + SUBTLE_COLOR + "Press 'd' for details, arrows to scroll, 'q' to close" + RESET);
        buffer.writeAt(x, row, "╰" + "─".repeat(width - 2));
    }

    @Override
    public boolean handleInput(InputEvent event) {
        if (event instanceof tech.kayys.peutui.core.event.KeyEvent keyEvent) {
            char keyChar = keyEvent.isCharacter() && keyEvent.character().length() == 1
                    ? keyEvent.character().charAt(0)
                    : 0;
            int keyCode = keyEvent.code().ordinal();

            switch (keyChar) {
                case 'd':
                case 'D':
                    toggleDetails();
                    return true;
                case 'q':
                case 'Q':
                    return false; // Signal to close
            }

            // Check for ESC
            if (keyEvent.code() == KeyCode.ESC) {
                return false;
            }

            if (keyEvent.code() == KeyCode.UP_ARROW) {
                scrollUp();
                return true;
            } else if (keyEvent.code() == KeyCode.DOWN_ARROW) {
                scrollDown(100); // Adjust based on content
                return true;
            }
        }

        return false;
    }

    /**
     * Formats a number with thousand separators.
     */
    private String formatNumber(long num) {
        if (num >= 1_000_000) {
            return String.format("%.2fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.2fK", num / 1_000.0);
        }
        return String.valueOf(num);
    }

    /**
     * Creates a detailed view component showing per-chat metrics.
     */
    public Component createDetailedView(String sessionId) {
        return new DetailedMetricsComponent(statsTracker, sessionId);
    }

    /**
     * Inner component for displaying detailed per-chat metrics.
     */
    private static class DetailedMetricsComponent implements Component {
        private final ChatStatsTracker statsTracker;
        private final String sessionId;

        DetailedMetricsComponent(ChatStatsTracker statsTracker, String sessionId) {
            this.statsTracker = statsTracker;
            this.sessionId = sessionId;
        }

        @Override
        public Size measure(BoxConstraints constraints) {
            int width = Math.max(50, constraints.maxWidth());
            int height = Math.max(10, constraints.maxHeight());
            return new Size(width, height);
        }

        @Override
        public boolean handleInput(InputEvent event) {
            if (event instanceof tech.kayys.peutui.core.event.KeyEvent keyEvent) {
                char keyChar = keyEvent.isCharacter() && keyEvent.character().length() == 1
                        ? keyEvent.character().charAt(0)
                        : 0;

                if (keyChar == 'q' || keyChar == 'Q') {
                    return false;
                }
                if (keyEvent.code() == KeyCode.ESC) {
                    return false;
                }
            }
            return true; // Consume other events
        }

        @Override
        public void render(RenderContext context) {
            ScreenBuffer buffer = context.buffer();
            var area = context.area();
            int width = area.width();
            int x = area.x();
            int y = area.y();
            int row = y;

            List<ChatPerformanceMetrics> metrics = statsTracker.getSessionMetrics(sessionId);
            List<TokenUsageStats> usage = statsTracker.getSessionUsage(sessionId);

            buffer.writeAt(x, row++, HEADER_COLOR + "╭─ Detailed Metrics for Session: " + sessionId + " ─" + RESET);

            if (metrics.isEmpty() && usage.isEmpty()) {
                buffer.writeAt(x, row++, "│  " + SUBTLE_COLOR + "No data available for this session" + RESET);
                buffer.writeAt(x, row, "╰" + "─".repeat(width - 2));
                return;
            }

            // Show last 5 metrics
            int start = Math.max(0, metrics.size() - 5);
            for (int i = start; i < metrics.size(); i++) {
                ChatPerformanceMetrics m = metrics.get(i);
                buffer.writeAt(x, row++, "│");
                buffer.writeAt(x, row++, "│  " + LABEL_COLOR + "Response #" + (i + 1) + RESET);
                buffer.writeAt(x, row++, "│    Stream updates: " + VALUE_COLOR + m.streamUpdates() + RESET +
                        ", Duration: " + String.format("%.2f s", m.durationMs() / 1000.0));
                buffer.writeAt(x, row++,
                        "│    Speed: " + VALUE_COLOR + String.format("%.2f t/s", m.tokensPerSecond()) + RESET +
                                ", TTFT: " + String.format("%.2f ms", m.ttftMs()));

                if (m.generatedTokens() > 0) {
                    buffer.writeAt(x, row++, "│    Generated: " + VALUE_COLOR + m.generatedTokens() + RESET +
                            " tokens (" + String.format("%.2f ms/tok", m.tokenLatencyMs()) + "/tok)");
                }

                if (!m.bottleneckType().isEmpty()) {
                    buffer.writeAt(x, row++, "│    Bottleneck: " + WARNING_COLOR + m.bottleneckType() + RESET +
                            " (" + String.format("%.2f ms, %.1f%%)",
                                    m.bottleneckValueMs(), m.bottleneckPercentage())
                            + ")");
                }
            }

            // Usage summary
            if (!usage.isEmpty()) {
                long totalTokens = usage.stream().mapToLong(TokenUsageStats::totalTokens).sum();
                long inputTokens = usage.stream().mapToLong(TokenUsageStats::inputTokens).sum();
                long outputTokens = usage.stream().mapToLong(TokenUsageStats::outputTokens).sum();
                double totalCost = usage.stream().mapToDouble(TokenUsageStats::cost).sum();

                buffer.writeAt(x, row++, "│");
                buffer.writeAt(x, row++, "│  " + LABEL_COLOR + "Session Totals" + RESET);
                buffer.writeAt(x, row++, "│    Total Tokens: " + VALUE_COLOR + totalTokens + RESET +
                        " (In: " + inputTokens + ", Out: " + outputTokens + ")");
                if (totalCost > 0) {
                    buffer.writeAt(x, row++, "│    Estimated Cost: " + VALUE_COLOR + "$" +
                            String.format("%.4f", totalCost) + RESET);
                }
            }

            buffer.writeAt(x, row, "╰" + "─".repeat(width - 2));
        }

        @Override
        public boolean handleKey(int keyCode, char keyChar) {
            return keyChar == 'q' || keyChar == 'Q' || keyCode == 27;
        }
    }
}
