package tech.kayys.peutui.core.text;

import java.text.BreakIterator;
import java.util.Locale;

/**
 * Word-boundary cursor navigation (Ctrl+Left / Ctrl+Right style movement)
 * over plain text. Pure functions - never mutate any state, just compute a
 * new cursor offset (in UTF-16 char index) from an old one.
 */
public final class WordNavigation {

    private WordNavigation() {
    }

    private static boolean isWhitespace(String s) {
        return !s.isEmpty() && s.chars().allMatch(Character::isWhitespace);
    }

    /** Moves the cursor one word backward, skipping trailing whitespace first. */
    public static int findWordBackward(String text, int cursor) {
        if (cursor <= 0) {
            return 0;
        }
        String before = text.substring(0, cursor);
        BreakIterator wordIterator = BreakIterator.getWordInstance(Locale.ROOT);
        wordIterator.setText(before);

        int boundary = wordIterator.last();
        int prevBoundary = wordIterator.previous();
        // Skip trailing whitespace-only tokens.
        while (prevBoundary != BreakIterator.DONE && isWhitespace(before.substring(prevBoundary, boundary))) {
            boundary = prevBoundary;
            prevBoundary = wordIterator.previous();
        }
        if (prevBoundary == BreakIterator.DONE) {
            return 0;
        }
        return prevBoundary;
    }

    /** Moves the cursor one word forward, skipping leading whitespace first. */
    public static int findWordForward(String text, int cursor) {
        if (cursor >= text.length()) {
            return text.length();
        }
        BreakIterator wordIterator = BreakIterator.getWordInstance(Locale.ROOT);
        wordIterator.setText(text);

        int start = cursor;
        int end = wordIterator.following(start);
        // Skip leading whitespace-only tokens.
        while (end != BreakIterator.DONE && isWhitespace(text.substring(start, end))) {
            start = end;
            end = wordIterator.next();
        }
        return end == BreakIterator.DONE ? text.length() : end;
    }
}
