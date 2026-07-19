package tech.kayys.peutui.widgets;

import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Size;
import tech.kayys.peutui.core.text.AnsiCodes;
import tech.kayys.peutui.core.text.TextMeasure;

/**
 * A flexible panel container with configurable borders and title.
 * Supports different border styles (single, double, rounded, bold) and
 * optional title text centered on the top border.
 */
public final class PanelComponent implements Component {

    private Component child;
    private String title = "";
    private BorderStyle borderStyle = BorderStyle.ROUNDED;
    private boolean showBorder = true;
    private int paddingLeft = 1;
    private int paddingRight = 1;
    private int paddingTop = 0;
    private int paddingBottom = 0;

    public enum BorderStyle {
        NONE("", "", "", "", "", ""),
        SINGLE("─", "│", "┌", "┐", "└", "┘"),
        DOUBLE("═", "║", "╔", "╗", "╚", "╝"),
        ROUNDED("─", "│", "╭", "╮", "╰", "╯"),
        BOLD("━", "┃", "┏", "┓", "┗", "┛"),
        ASCII("-", "|", "+", "+", "+", "+");

        public final String horizontal;
        public final String vertical;
        public final String topLeft;
        public final String topRight;
        public final String bottomLeft;
        public final String bottomRight;

        BorderStyle(String h, String v, String tl, String tr, String bl, String br) {
            this.horizontal = h;
            this.vertical = v;
            this.topLeft = tl;
            this.topRight = tr;
            this.bottomLeft = bl;
            this.bottomRight = br;
        }
    }

    public PanelComponent setChild(Component child) {
        this.child = child;
        return this;
    }

    public PanelComponent setTitle(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    public PanelComponent setBorderStyle(BorderStyle style) {
        this.borderStyle = style != null ? style : BorderStyle.ROUNDED;
        return this;
    }

    public PanelComponent setShowBorder(boolean show) {
        this.showBorder = show;
        return this;
    }

    public PanelComponent setPadding(int all) {
        return setPadding(all, all, all, all);
    }

    public PanelComponent setPadding(int left, int right, int top, int bottom) {
        this.paddingLeft = Math.max(0, left);
        this.paddingRight = Math.max(0, right);
        this.paddingTop = Math.max(0, top);
        this.paddingBottom = Math.max(0, bottom);
        return this;
    }

    @Override
    public boolean isFocusable() {
        return child != null && child.isFocusable();
    }

    @Override
    public Size measure(BoxConstraints constraints) {
        if (child == null) {
            return new Size(constraints.maxWidth(), constraints.maxHeight());
        }

        int borderW = showBorder ? 2 : 0;
        int borderH = showBorder ? 2 : 0;
        int innerMaxW = Math.max(1, constraints.maxWidth() - borderW - paddingLeft - paddingRight);
        int innerMaxH = Math.max(1, constraints.maxHeight() - borderH - paddingTop - paddingBottom);

        BoxConstraints inner = BoxConstraints.loose(innerMaxW, innerMaxH);
        Size childSize = child.measure(inner);

        return new Size(
                childSize.width() + borderW + paddingLeft + paddingRight,
                childSize.height() + borderH + paddingTop + paddingBottom);
    }

    @Override
    public void render(RenderContext context) {
        var area = context.area();
        var buffer = context.buffer();
        int width = area.width();
        int height = area.height();

        if (child == null) {
            // Empty panel
            if (showBorder) {
                renderBorder(buffer, area, "");
            }
            return;
        }

        if (showBorder) {
            renderBorder(buffer, area, title);

            // Calculate inner area
            int innerX = area.x() + 1 + paddingLeft;
            int innerY = area.y() + 1 + paddingTop;
            int innerW = Math.max(1, width - 2 - paddingLeft - paddingRight);
            int innerH = Math.max(1, height - 2 - paddingTop - paddingBottom);

            var innerArea = new tech.kayys.peutui.core.layout.Rect(innerX, innerY, innerW, innerH);
            child.measure(BoxConstraints.tight(new Size(innerW, innerH)));
            child.render(context.withArea(innerArea));
        } else {
            // No border, just padding
            int innerX = area.x() + paddingLeft;
            int innerY = area.y() + paddingTop;
            int innerW = Math.max(1, width - paddingLeft - paddingRight);
            int innerH = Math.max(1, height - paddingTop - paddingBottom);

            var innerArea = new tech.kayys.peutui.core.layout.Rect(innerX, innerY, innerW, innerH);
            child.measure(BoxConstraints.tight(new Size(innerW, innerH)));
            child.render(context.withArea(innerArea));
        }
    }

    private void renderBorder(tech.kayys.peutui.core.buffer.ScreenBuffer buffer,
            tech.kayys.peutui.core.layout.Rect area, String titleText) {
        int w = area.width();
        int h = area.height();
        int x = area.x();
        int y = area.y();

        if (w < 2 || h < 2) {
            return;
        }

        String hLine = borderStyle.horizontal;
        String vLine = borderStyle.vertical;
        String tl = borderStyle.topLeft;
        String tr = borderStyle.topRight;
        String bl = borderStyle.bottomLeft;
        String br = borderStyle.bottomRight;

        // Top border with optional title
        StringBuilder top = new StringBuilder();
        top.append(tl);

        if (!titleText.isEmpty()) {
            int titleSpace = w - 4;
            if (titleSpace > 0) {
                int titleLen = Math.min(TextMeasure.visibleWidth(titleText), titleSpace);
                int leftLen = (titleSpace - titleLen) / 2;
                int rightLen = titleSpace - leftLen - titleLen;
                top.append(hLine.repeat(leftLen));
                top.append(" ").append(titleText).append(" ");
                top.append(hLine.repeat(rightLen));
            } else {
                top.append(hLine.repeat(w - 2));
            }
        } else {
            top.append(hLine.repeat(w - 2));
        }
        top.append(tr);

        buffer.writeAt(x, y, TextMeasure.truncateToWidth(top.toString(), w, "", false));

        // Middle rows
        String sideRow = AnsiCodes.DIM + vLine + AnsiCodes.RESET + " ".repeat(w - 2) + AnsiCodes.DIM + vLine
                + AnsiCodes.RESET;
        for (int row = 1; row < h - 1; row++) {
            buffer.writeAt(x, y + row, sideRow);
        }

        // Bottom border
        String bottom = bl + hLine.repeat(w - 2) + br;
        buffer.writeAt(x, y + h - 1, TextMeasure.truncateToWidth(bottom, w, "", false));
    }

    @Override
    public boolean handleInput(tech.kayys.peutui.core.event.InputEvent event) {
        return child != null && child.handleInput(event);
    }
}
