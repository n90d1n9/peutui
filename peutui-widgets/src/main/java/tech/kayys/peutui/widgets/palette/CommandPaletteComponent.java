package tech.kayys.peutui.widgets.palette;

import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.event.KeyCode;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Size;
import tech.kayys.peutui.core.text.AnsiCodes;
import tech.kayys.peutui.core.text.TextMeasure;

import java.util.List;
import java.util.function.Consumer;

/**
 * A TUI component that displays a command palette with filtering and selection.
 * Similar to VS Code's Ctrl+P or Command Palette pattern.
 */
public class CommandPaletteComponent extends CommandPalette implements Component {

    private boolean visible = false;
    private String placeholder = "Type to search commands...";
    private int maxVisibleItems = 10;
    private OnCloseCallback onCloseCallback;

    public interface OnCloseCallback {
        void onClose();
    }

    public CommandPaletteComponent() {
        super();
    }

    /**
     * Sets the placeholder text shown when filter is empty.
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
    }

    /**
     * Sets the maximum number of visible items in the dropdown.
     */
    public void setMaxVisibleItems(int max) {
        this.maxVisibleItems = Math.max(3, max);
    }

    /**
     * Sets the callback invoked when the palette is closed.
     */
    public void setOnCloseCallback(OnCloseCallback callback) {
        this.onCloseCallback = callback;
    }

    /**
     * Shows the palette.
     */
    public void show() {
        visible = true;
        reset();
    }

    /**
     * Hides the palette.
     */
    public void hide() {
        visible = false;
        if (onCloseCallback != null) {
            onCloseCallback.onClose();
        }
    }

    /**
     * Toggles visibility.
     */
    public void toggle() {
        if (visible) {
            hide();
        } else {
            show();
        }
    }

    /**
     * Checks if the palette is currently visible.
     */
    public boolean isVisible() {
        return visible;
    }

    @Override
    protected void handleExecution(CommandItem command) {
        // Default: hide after execution
        hide();
    }

    @Override
    public boolean isFocusable() {
        return visible;
    }

    @Override
    public Size measure(BoxConstraints constraints) {
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    @Override
    public void render(RenderContext context) {
        if (!visible) {
            return;
        }

        var area = context.area();
        var buffer = context.buffer();
        int width = area.width();
        int height = area.height();

        // Calculate palette dimensions
        int paletteWidth = Math.min(width - 4, 60);
        int paletteHeight = maxVisibleItems + 3; // header + input + items + border
        int paletteX = area.x() + (width - paletteWidth) / 2;
        int paletteY = area.y() + Math.max(0, (height - paletteHeight) / 2);

        // Draw border
        drawBorder(buffer, paletteX, paletteY, paletteWidth, paletteHeight);

        // Draw header
        String header = " Command Palette ";
        int headerPadding = (paletteWidth - 2 - header.length()) / 2;
        String headerLine = " ".repeat(headerPadding) +
                AnsiCodes.BOLD + AnsiCodes.CYAN + header + AnsiCodes.RESET +
                " ".repeat(paletteWidth - 2 - headerPadding - header.length());
        buffer.writeAt(paletteX + 1, paletteY + 1, headerLine);

        // Draw input field
        String inputPrompt = "> ";
        String inputContent = filterText;
        String cursor = AnsiCodes.INVERSE + " " + AnsiCodes.RESET;
        String fullInput = inputPrompt + inputContent + cursor;

        // Truncate if too long
        int inputMaxWidth = paletteWidth - 4;
        if (TextMeasure.visibleWidth(fullInput) > inputMaxWidth) {
            int overflow = TextMeasure.visibleWidth(fullInput) - inputMaxWidth;
            inputContent = inputContent.substring(Math.min(overflow, inputContent.length()));
            fullInput = inputPrompt + inputContent + cursor;
        }

        buffer.writeAt(paletteX + 2, paletteY + 2, fullInput);

        // Draw filtered commands
        List<CommandItem> filtered = getFilteredCommands();
        int scrollOffset = calculateScrollOffset(filtered.size(), maxVisibleItems);

        for (int i = 0; i < Math.min(maxVisibleItems, filtered.size()); i++) {
            int itemIndex = i + scrollOffset;
            if (itemIndex >= filtered.size())
                break;

            CommandItem item = filtered.get(itemIndex);
            boolean isSelected = itemIndex == selectedIndex;

            int row = paletteY + 3 + i;
            if (row >= paletteY + paletteHeight - 1)
                break;

            StringBuilder line = new StringBuilder();
            line.append(" ");

            if (isSelected) {
                line.append(AnsiCodes.BG_BLUE + AnsiCodes.WHITE);
            }

            // Name
            String name = TextMeasure.truncateToWidth(item.name(), 25, "…", false);
            line.append(name);

            // Padding
            int namePad = 26 - TextMeasure.visibleWidth(name);
            line.append(" ".repeat(Math.max(0, namePad)));

            // Description (dimmed)
            if (!isSelected) {
                line.append(AnsiCodes.DIM);
            }
            String desc = TextMeasure.truncateToWidth(
                    item.description(),
                    paletteWidth - 32,
                    "…",
                    false);
            line.append(desc);

            if (isSelected) {
                line.append(AnsiCodes.RESET);
            }

            // Pad to fill
            int lineWidth = TextMeasure.visibleWidth(line.toString());
            int remaining = paletteWidth - 2 - lineWidth;
            if (remaining > 0) {
                line.append(" ".repeat(remaining));
            }

            buffer.writeAt(paletteX + 1, row, line.toString());
        }

        // Draw hint
        String hint = AnsiCodes.DIM + "↑↓ to navigate, Enter to execute, Esc to close" + AnsiCodes.RESET;
        int hintX = paletteX + (paletteWidth - TextMeasure.visibleWidth(hint)) / 2;
        buffer.writeAt(hintX, paletteY + paletteHeight - 2, hint);
    }

    private void drawBorder(tech.kayys.peutui.core.buffer.ScreenBuffer buffer,
            int x, int y, int width, int height) {
        // Corners
        buffer.writeAt(x, y, "╭" + "─".repeat(width - 2) + "╮");
        buffer.writeAt(x, y + height - 1, "╰" + "─".repeat(width - 2) + "╯");

        // Sides
        for (int i = 1; i < height - 1; i++) {
            buffer.writeAt(x, y + i, "│");
            buffer.writeAt(x + width - 1, y + i, "│");
        }
    }

    private int calculateScrollOffset(int totalItems, int visibleItems) {
        if (totalItems <= visibleItems) {
            return 0;
        }
        if (selectedIndex < visibleItems / 2) {
            return 0;
        }
        if (selectedIndex >= totalItems - visibleItems + visibleItems / 2) {
            return totalItems - visibleItems;
        }
        return selectedIndex - visibleItems / 2;
    }

    @Override
    public boolean handleInput(InputEvent event) {
        if (!visible) {
            return false;
        }

        if (!(event instanceof tech.kayys.peutui.core.event.KeyEvent key)) {
            return false;
        }

        if (key.code() == KeyCode.ESCAPE) {
            hide();
            return true;
        }

        if (key.code() == KeyCode.ENTER) {
            executeSelected();
            return true;
        }

        if (key.code() == KeyCode.UP) {
            selectUp();
            return true;
        }

        if (key.code() == KeyCode.DOWN) {
            selectDown();
            return true;
        }

        // Handle text input for filtering
        if (key.character() != null && key.character().length() == 1) {
            char ch = key.character().charAt(0);
            if (ch >= 32 && ch != 127) {
                setFilter(filterText + ch);
                return true;
            }
        }

        // Backspace
        if (key.code() == KeyCode.BACKSPACE && !filterText.isEmpty()) {
            setFilter(filterText.substring(0, filterText.length() - 1));
            return true;
        }

        return false;
    }
}
