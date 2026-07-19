package tech.kayys.peutui.widgets;

import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.event.KeyCode;
import tech.kayys.peutui.core.event.KeyEvent;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Size;
import tech.kayys.peutui.core.text.AnsiCodes;
import tech.kayys.peutui.core.text.TextMeasure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A modern header component with title, optional subtitle, and tab navigation.
 * Supports styled tabs with active/inactive states and keyboard navigation.
 */
public final class HeaderComponent implements Component {

    private String title = "";
    private String subtitle = "";
    private final List<Tab> tabs = new ArrayList<>();
    private int activeTabIndex = 0;
    private Consumer<Integer> onTabSelected = index -> {
    };
    private boolean showTabs = true;

    public record Tab(String label, String id, boolean enabled) {
        public Tab(String label, String id) {
            this(label, id, true);
        }
    }

    public HeaderComponent setTitle(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    public HeaderComponent setSubtitle(String subtitle) {
        this.subtitle = subtitle == null ? "" : subtitle;
        return this;
    }

    public HeaderComponent addTab(String label, String id) {
        tabs.add(new Tab(label, id));
        return this;
    }

    public HeaderComponent addTab(Tab tab) {
        tabs.add(tab);
        return this;
    }

    /**
     * Adds a special statistics/dashboard tab that can be used to display
     * token usage and performance metrics.
     * 
     * @param statsTracker the statistics tracker to monitor
     * @return this header for chaining
     */
    public HeaderComponent addStatsTab(tech.kayys.peutui.widgets.stats.ChatStatsTracker statsTracker) {
        tabs.add(new Tab("📊 Stats", "stats", true));
        // Optionally set up listener to update badge or indicator
        if (statsTracker != null) {
            statsTracker.addListener(event -> {
                // Could add visual indicator here (e.g., badge count)
            });
        }
        return this;
    }

    public HeaderComponent clearTabs() {
        tabs.clear();
        return this;
    }

    public HeaderComponent setActiveTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            this.activeTabIndex = index;
        }
        return this;
    }

    public HeaderComponent setActiveTabById(String id) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).id().equals(id)) {
                this.activeTabIndex = i;
                break;
            }
        }
        return this;
    }

    public HeaderComponent setShowTabs(boolean show) {
        this.showTabs = show;
        return this;
    }

    public HeaderComponent setOnTabSelected(Consumer<Integer> handler) {
        this.onTabSelected = handler != null ? handler : index -> {
        };
        return this;
    }

    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    public String getActiveTabId() {
        return tabs.isEmpty() ? "" : tabs.get(activeTabIndex).id();
    }

    @Override
    public boolean isFocusable() {
        return !tabs.isEmpty();
    }

    @Override
    public Size measure(BoxConstraints constraints) {
        return new Size(constraints.maxWidth(), showTabs ? 3 : 2);
    }

    @Override
    public void render(RenderContext context) {
        var area = context.area();
        var buffer = context.buffer();
        int width = area.width();

        // Row 0: Title bar with border top
        String borderTop = AnsiCodes.DIM + "─".repeat(width) + AnsiCodes.RESET;
        buffer.writeAt(area.x(), area.y(), borderTop);

        // Row 1: Title and subtitle
        int titleRow = area.y() + 1;
        String titleText = AnsiCodes.BOLD + AnsiCodes.INVERSE + " " + title + " " + AnsiCodes.RESET;
        int titleWidth = TextMeasure.visibleWidth(titleText);

        if (!subtitle.isEmpty()) {
            String subtitleText = AnsiCodes.DIM + "  " + subtitle + AnsiCodes.RESET;
            int availableWidth = width - titleWidth;
            if (availableWidth > 0) {
                titleText += TextMeasure.truncateToWidth(subtitleText, availableWidth, "", false);
            }
        }

        // Pad or truncate to fill the row
        int paddingNeeded = width - TextMeasure.visibleWidth(titleText);
        if (paddingNeeded > 0) {
            titleText += " ".repeat(paddingNeeded);
        }
        titleText = TextMeasure.truncateToWidth(titleText, width, "", false);
        buffer.writeAt(area.x(), titleRow, titleText);

        // Row 2: Tabs (if enabled)
        if (showTabs && !tabs.isEmpty()) {
            int tabRow = area.y() + 2;
            renderTabs(buffer, area.x(), tabRow, width);
        }
    }

    private void renderTabs(tech.kayys.peutui.core.buffer.ScreenBuffer buffer, int x, int y, int maxWidth) {
        StringBuilder line = new StringBuilder();
        int currentWidth = 0;

        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            boolean isActive = i == activeTabIndex;
            boolean isEnabled = tab.enabled();

            String style;
            if (!isEnabled) {
                style = AnsiCodes.DIM;
            } else if (isActive) {
                style = AnsiCodes.INVERSE + AnsiCodes.BOLD;
            } else {
                style = AnsiCodes.DIM;
            }

            String label = " " + tab.label() + " ";
            String styledLabel = style + label + AnsiCodes.RESET;
            int labelWidth = TextMeasure.visibleWidth(styledLabel);

            if (currentWidth + labelWidth <= maxWidth) {
                line.append(styledLabel);
                currentWidth += labelWidth;
            } else {
                // Not enough space, show ellipsis
                if (currentWidth + 3 <= maxWidth) {
                    line.append(AnsiCodes.DIM + "..." + AnsiCodes.RESET);
                }
                break;
            }
        }

        // Fill remaining space
        int remaining = maxWidth - TextMeasure.visibleWidth(line.toString());
        if (remaining > 0) {
            line.append(" ".repeat(remaining));
        }

        buffer.writeAt(x, y, TextMeasure.truncateToWidth(line.toString(), maxWidth, "", false));
    }

    @Override
    public boolean handleInput(InputEvent event) {
        if (!(event instanceof KeyEvent key) || tabs.isEmpty()) {
            return false;
        }

        return switch (key.code()) {
            case LEFT -> {
                if (activeTabIndex > 0) {
                    activeTabIndex--;
                    onTabSelected.accept(activeTabIndex);
                }
                yield true;
            }
            case RIGHT -> {
                if (activeTabIndex < tabs.size() - 1) {
                    activeTabIndex++;
                    onTabSelected.accept(activeTabIndex);
                }
                yield true;
            }
            case ENTER -> {
                onTabSelected.accept(activeTabIndex);
                yield true;
            }
            case HOME -> {
                activeTabIndex = 0;
                onTabSelected.accept(activeTabIndex);
                yield true;
            }
            case END -> {
                activeTabIndex = tabs.size() - 1;
                onTabSelected.accept(activeTabIndex);
                yield true;
            }
            default -> {
                // Number keys for direct tab selection (1-9)
                if (!key.ctrl() && !key.alt()) {
                    String ch = key.character();
                    if (ch != null && ch.matches("[1-9]")) {
                        int idx = Integer.parseInt(ch) - 1;
                        if (idx >= 0 && idx < tabs.size()) {
                            activeTabIndex = idx;
                            onTabSelected.accept(activeTabIndex);
                            yield true;
                        }
                    }
                }
                yield false;
            }
        };
    }
}
