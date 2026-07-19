package tech.kayys.peutui.core.text;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Terminal-column width calculation for text, grapheme-cluster aware.
 *
 * <p>
 * Mirrors the behavior expected by a monospace terminal renderer: most
 * codepoints occupy one column, "wide" (East Asian Wide/Fullwidth) codepoints
 * occupy two columns, and zero-width codepoints (combining marks, variation
 * selectors, most emoji modifiers) occupy zero columns. A multi-codepoint
 * grapheme cluster (e.g. an emoji + ZWJ sequence, or a base char + combining
 * accent) is measured and sliced as a single unit.
 */
public final class GraphemeWidth {

    private GraphemeWidth() {
    }

    /**
     * Segments {@code text} into grapheme clusters using the default locale's
     * rules.
     */
    public static List<String> segments(String text) {
        List<String> result = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            result.add(text.substring(start, end));
        }
        return result;
    }

    /**
     * Visible column width of a single grapheme cluster (may be multi-codepoint).
     */
    public static int width(String grapheme) {
        if (grapheme.isEmpty()) {
            return 0;
        }
        int cp = grapheme.codePointAt(0);
        if (isZeroWidth(cp)) {
            return 0;
        }
        return isWide(cp) ? 2 : 1;
    }

    /**
     * Total visible column width of the whole string, ignoring ANSI codes (use
     * {@link TextMeasure} for that).
     */
    public static int stringWidth(String text) {
        int total = 0;
        for (String g : segments(text)) {
            total += width(g);
        }
        return total;
    }

    private static boolean isZeroWidth(int cp) {
        if (cp == 0) {
            return true;
        }
        int type = Character.getType(cp);
        if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK
                || type == Character.FORMAT) {
            return true;
        }
        // Variation selectors, zero-width joiner/non-joiner.
        return cp == 0x200D || cp == 0x200C || (cp >= 0xFE00 && cp <= 0xFE0F);
    }

    /**
     * Approximates the Unicode East Asian Width property: codepoints in the
     * CJK, Hangul, and fullwidth-form ranges render as two terminal columns.
     */
    private static boolean isWide(int cp) {
        return (cp >= 0x1100 && cp <= 0x115F) // Hangul Jamo
                || cp == 0x2329 || cp == 0x232A
                || (cp >= 0x2E80 && cp <= 0x303E) // CJK Radicals / Symbols
                || (cp >= 0x3041 && cp <= 0x33FF) // Hiragana .. CJK Compat
                || (cp >= 0x3400 && cp <= 0x4DBF) // CJK Ext A
                || (cp >= 0x4E00 && cp <= 0x9FFF) // CJK Unified Ideographs
                || (cp >= 0xA000 && cp <= 0xA4CF) // Yi
                || (cp >= 0xAC00 && cp <= 0xD7A3) // Hangul Syllables
                || (cp >= 0xF900 && cp <= 0xFAFF) // CJK Compat Ideographs
                || (cp >= 0xFE30 && cp <= 0xFE4F) // CJK Compat Forms
                || (cp >= 0xFF00 && cp <= 0xFF60) // Fullwidth Forms
                || (cp >= 0xFFE0 && cp <= 0xFFE6)
                || (cp >= 0x1F300 && cp <= 0x1FAFF) // Emoji blocks
                || (cp >= 0x20000 && cp <= 0x3FFFD); // CJK Ext B..
    }
}
