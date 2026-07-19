package tech.kayys.peutui.core.text;

import java.util.List;
import java.util.regex.Matcher;

/**
 * ANSI- and grapheme-aware text measurement and slicing utilities for a
 * monospace terminal grid. All operations work in terms of visible terminal
 * columns, not char/codepoint counts, and preserve/re-emit SGR styling
 * around any slice boundary they introduce.
 */
public final class TextMeasure {

    private TextMeasure() {
    }

    /**
     * Result of slicing a line: the resulting (possibly re-styled) text and its
     * true visible width.
     */
    public record Slice(String text, int width) {
    }

    /**
     * Result of splitting a line into a "before" and "after" region around a gap.
     */
    public record Segments(String before, int beforeWidth, String after, int afterWidth) {
    }

    /** Visible width of {@code text}, ignoring ANSI escape sequences. */
    public static int visibleWidth(String text) {
        return GraphemeWidth.stringWidth(AnsiCodes.strip(text));
    }

    /**
     * Extracts the visible columns {@code [startCol, startCol+length)} from
     * {@code line}.
     */
    public static String sliceByColumn(String line, int startCol, int length) {
        return sliceByColumn(line, startCol, length, false);
    }

    /**
     * Extracts a range of visible columns from a line, preserving ANSI styling
     * that was active at the start of the range.
     *
     * @param strict if true, a wide (2-column) grapheme that would straddle the
     *               end boundary is dropped rather than allowed to overflow it
     */
    public static String sliceByColumn(String line, int startCol, int length, boolean strict) {
        return sliceWithWidth(line, startCol, length, strict).text();
    }

    /**
     * Like {@link #sliceByColumn} but also returns the actual visible width
     * extracted.
     */
    public static Slice sliceWithWidth(String line, int startCol, int length, boolean strict) {
        if (length <= 0) {
            return new Slice("", 0);
        }
        int endCol = startCol + length;
        StringBuilder result = new StringBuilder();
        StringBuilder pendingAnsi = new StringBuilder();
        int currentCol = 0;
        int resultWidth = 0;
        int i = 0;
        Matcher csi = AnsiCodes.CSI_SEQUENCE.matcher(line);

        while (i < line.length()) {
            if (csi.find(i) && csi.start() == i) {
                String code = line.substring(csi.start(), csi.end());
                if (currentCol >= startCol && currentCol < endCol) {
                    result.append(code);
                } else if (currentCol < startCol) {
                    pendingAnsi.append(code);
                }
                i = csi.end();
                continue;
            }
            int textEnd = nextEscapeOrEnd(line, i, csi);
            for (String segment : GraphemeWidth.segments(line.substring(i, textEnd))) {
                int w = GraphemeWidth.width(segment);
                boolean inRange = currentCol >= startCol && currentCol < endCol;
                boolean fits = !strict || currentCol + w <= endCol;
                if (inRange && fits) {
                    if (pendingAnsi.length() > 0) {
                        result.append(pendingAnsi);
                        pendingAnsi.setLength(0);
                    }
                    result.append(segment);
                    resultWidth += w;
                }
                currentCol += w;
                if (currentCol >= endCol) {
                    break;
                }
            }
            i = textEnd;
            if (currentCol >= endCol) {
                break;
            }
        }
        return new Slice(result.toString(), resultWidth);
    }

    /**
     * Splits a line into "before" ({@code [0, beforeEnd)}) and "after"
     * ({@code [afterStart, afterStart+afterLen)}) regions in one pass, with
     * the "after" region prefixed by whatever SGR styling was active
     * immediately before it (so overlay compositing doesn't lose color/bold
     * state that started earlier in the line).
     */
    public static Segments extractSegments(String line, int beforeEnd, int afterStart, int afterLen,
            boolean strictAfter) {
        StringBuilder before = new StringBuilder();
        StringBuilder after = new StringBuilder();
        StringBuilder pendingAnsiBefore = new StringBuilder();
        int beforeWidth = 0;
        int afterWidth = 0;
        int currentCol = 0;
        int i = 0;
        boolean afterStarted = false;
        int afterEnd = afterStart + afterLen;
        AnsiCodeTracker tracker = new AnsiCodeTracker();
        Matcher csi = AnsiCodes.CSI_SEQUENCE.matcher(line);

        while (i < line.length()) {
            if (csi.find(i) && csi.start() == i) {
                String code = line.substring(csi.start(), csi.end());
                tracker.process(code);
                if (currentCol < beforeEnd) {
                    pendingAnsiBefore.append(code);
                } else if (currentCol >= afterStart && currentCol < afterEnd && afterStarted) {
                    after.append(code);
                }
                i = csi.end();
                continue;
            }
            int textEnd = nextEscapeOrEnd(line, i, csi);
            for (String segment : GraphemeWidth.segments(line.substring(i, textEnd))) {
                int w = GraphemeWidth.width(segment);
                if (currentCol < beforeEnd && currentCol + w <= beforeEnd) {
                    if (pendingAnsiBefore.length() > 0) {
                        before.append(pendingAnsiBefore);
                        pendingAnsiBefore.setLength(0);
                    }
                    before.append(segment);
                    beforeWidth += w;
                } else if (currentCol >= afterStart && currentCol < afterEnd) {
                    boolean fits = !strictAfter || currentCol + w <= afterEnd;
                    if (fits) {
                        if (!afterStarted) {
                            after.append(tracker.getActiveCodes());
                            afterStarted = true;
                        }
                        after.append(segment);
                        afterWidth += w;
                    }
                }
                currentCol += w;
                boolean done = afterLen <= 0 ? currentCol >= beforeEnd : currentCol >= afterEnd;
                if (done) {
                    break;
                }
            }
            i = textEnd;
            boolean done = afterLen <= 0 ? currentCol >= beforeEnd : currentCol >= afterEnd;
            if (done) {
                break;
            }
        }
        return new Segments(before.toString(), beforeWidth, after.toString(), afterWidth);
    }

    /**
     * Truncates {@code text} to at most {@code maxWidth} visible columns, appending
     * {@code ellipsis} if cut.
     */
    public static String truncateToWidth(String text, int maxWidth, String ellipsis, boolean pad) {
        if (maxWidth <= 0) {
            return "";
        }
        int ellipsisWidth = GraphemeWidth.stringWidth(ellipsis);
        List<String> graphemes = GraphemeWidth.segments(AnsiCodes.strip(text));
        // Re-slice with styling preserved using the width-aware slicer once we know the
        // cut point.
        int visibleSoFar = 0;
        boolean overflowed = false;
        for (String g : graphemes) {
            int w = GraphemeWidth.width(g);
            if (visibleSoFar + w > maxWidth) {
                overflowed = true;
                break;
            }
            visibleSoFar += w;
        }
        if (!overflowed) {
            String plain = AnsiCodes.strip(text);
            if (pad && visibleSoFar < maxWidth) {
                return withStyling(text, plain) + " ".repeat(maxWidth - visibleSoFar);
            }
            return withStyling(text, plain);
        }
        int keepWidth = Math.max(0, maxWidth - ellipsisWidth);
        Slice kept = sliceWithWidth(text, 0, keepWidth, true);
        String result = kept.text() + ellipsis;
        int totalWidth = kept.width() + ellipsisWidth;
        if (pad && totalWidth < maxWidth) {
            result += " ".repeat(maxWidth - totalWidth);
        }
        return result;
    }

    private static String withStyling(String original, String plainFallback) {
        // If the original had no ANSI codes, plainFallback is equivalent and cheaper to
        // return.
        return AnsiCodes.CSI_SEQUENCE.matcher(original).find() ? original : plainFallback;
    }

    private static int nextEscapeOrEnd(String line, int from, Matcher csi) {
        if (csi.find(from)) {
            return csi.start();
        }
        return line.length();
    }
}
