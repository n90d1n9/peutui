package tech.kayys.peutui.core.buffer;

/**
 * A single terminal grid cell: the grapheme cluster occupying it plus any
 * SGR styling prefix that should be emitted before it. Wide (2-column)
 * graphemes occupy their leading cell with {@code width=2} and are followed
 * by a {@link #continuation()} placeholder cell so buffer diffing stays a
 * simple 1:1 grid.
 */
public record Cell(String grapheme, String styling, int width) {

    public static final Cell EMPTY = new Cell(" ", "", 1);

    public static Cell continuation() {
        return new Cell("", "", 0);
    }

    public boolean isContinuation() {
        return width == 0;
    }
}
