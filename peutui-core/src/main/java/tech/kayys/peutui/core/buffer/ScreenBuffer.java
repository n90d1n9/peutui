package tech.kayys.peutui.core.buffer;

import tech.kayys.peutui.core.layout.Size;
import tech.kayys.peutui.core.text.AnsiCodes;
import tech.kayys.peutui.core.text.GraphemeWidth;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * A fixed-size grid of {@link Cell}s representing one full terminal frame.
 * Supports writing styled text at a coordinate and diffing against a
 * previous frame to produce a minimal set of terminal writes, which is what
 * makes the render loop flicker-free and cheap on large screens.
 */
public final class ScreenBuffer {

    private final int width;
    private final int height;
    private final Cell[][] cells;

    public ScreenBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        this.cells = new Cell[height][width];
        clear();
    }

    public Size size() {
        return new Size(width, height);
    }

    public void clear() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells[y][x] = Cell.EMPTY;
            }
        }
    }

    /**
     * Writes {@code text} (which may contain ANSI SGR codes) starting at
     * {@code (x, y)}, clipped to bounds.
     */
    public void writeAt(int x, int y, String text) {
        if (y < 0 || y >= height || text == null || text.isEmpty()) {
            return;
        }
        int col = x;
        String activeStyle = "";
        Matcher csi = AnsiCodes.CSI_SEQUENCE.matcher(text);
        int i = 0;
        while (i < text.length()) {
            if (csi.find(i) && csi.start() == i) {
                String code = text.substring(csi.start(), csi.end());
                activeStyle = code.equals(AnsiCodes.RESET) ? "" : activeStyle + code;
                i = csi.end();
                continue;
            }
            int nextEscape = csi.find(i) ? csi.start() : text.length();
            for (String g : GraphemeWidth.segments(text.substring(i, nextEscape))) {
                int w = GraphemeWidth.width(g);
                if (col >= 0 && col < width) {
                    cells[y][col] = new Cell(g, activeStyle, Math.max(w, 1));
                    if (w == 2 && col + 1 < width) {
                        cells[y][col + 1] = Cell.continuation();
                    }
                }
                col += Math.max(w, 1);
                if (col >= width) {
                    break;
                }
            }
            i = nextEscape;
            if (col >= width) {
                break;
            }
        }
    }

    public Cell get(int x, int y) {
        return cells[y][x];
    }

    /**
     * Computes the minimal list of row-writes needed to transform
     * {@code previous} into {@code this}. A {@code null} previous buffer (or
     * a size mismatch) forces a full-frame diff.
     */
    public List<RowDiff> diff(ScreenBuffer previous) {
        List<RowDiff> diffs = new ArrayList<>();
        boolean fullRepaint = previous == null || previous.width != width || previous.height != height;
        for (int y = 0; y < height; y++) {
            if (fullRepaint || !rowEquals(previous, y)) {
                diffs.add(new RowDiff(y, renderRow(y)));
            }
        }
        return diffs;
    }

    private boolean rowEquals(ScreenBuffer other, int y) {
        for (int x = 0; x < width; x++) {
            Cell a = cells[y][x];
            Cell b = other.cells[y][x];
            if (!a.grapheme().equals(b.grapheme()) || !a.styling().equals(b.styling())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Renders one row to a single ANSI-styled string, coalescing consecutive cells
     * with identical styling.
     */
    public String renderRow(int y) {
        StringBuilder sb = new StringBuilder();
        String lastStyle = null;
        for (int x = 0; x < width; x++) {
            Cell cell = cells[y][x];
            if (cell.isContinuation()) {
                continue;
            }
            if (!cell.styling().equals(lastStyle == null ? "" : lastStyle)) {
                if (lastStyle != null) {
                    sb.append(AnsiCodes.RESET);
                }
                sb.append(cell.styling());
                lastStyle = cell.styling();
            }
            sb.append(cell.grapheme());
        }
        if (lastStyle != null && !lastStyle.isEmpty()) {
            sb.append(AnsiCodes.RESET);
        }
        return sb.toString();
    }

    /**
     * A single row that changed between two frames, with row index and the fully
     * rendered replacement content.
     */
    public record RowDiff(int row, String content) {
    }
}
