package tech.kayys.peutui.core.fuzzy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Lightweight subsequence fuzzy matcher used for filtering autocomplete
 * candidates, command palettes, and similar lists. Not a full Smith-Waterman
 * style matcher - optimized for interactive typing latency over exhaustive
 * ranking quality.
 */
public final class FuzzyMatcher {

    private FuzzyMatcher() {
    }

    public record Match<T>(T item, int score) {
    }

    /**
     * Filters and ranks {@code candidates} by fuzzy subsequence match of
     * {@code query} against {@code text}.
     */
    public static <T> List<Match<T>> filter(String query, List<T> candidates, Function<T, String> textOf) {
        if (query == null || query.isBlank()) {
            List<Match<T>> all = new ArrayList<>(candidates.size());
            for (T c : candidates) {
                all.add(new Match<>(c, 0));
            }
            return all;
        }
        String q = query.toLowerCase(Locale.ROOT);
        List<Match<T>> matches = new ArrayList<>();
        for (T candidate : candidates) {
            String text = textOf.apply(candidate);
            int score = score(q, text.toLowerCase(Locale.ROOT));
            if (score >= 0) {
                matches.add(new Match<>(candidate, score));
            }
        }
        matches.sort(Comparator.comparingInt((Match<T> m) -> -m.score()));
        return matches;
    }

    /**
     * Returns a match score (higher is better) if {@code query} is a
     * subsequence of {@code text}, otherwise -1. Consecutive-character and
     * prefix matches are rewarded to bias toward more intuitive results.
     */
    private static int score(String query, String text) {
        int qi = 0;
        int score = 0;
        int consecutiveRun = 0;
        for (int ti = 0; ti < text.length() && qi < query.length(); ti++) {
            if (text.charAt(ti) == query.charAt(qi)) {
                consecutiveRun++;
                score += 1 + consecutiveRun;
                if (ti == qi) {
                    score += 2; // prefix bonus
                }
                qi++;
            } else {
                consecutiveRun = 0;
            }
        }
        if (qi < query.length()) {
            return -1; // not all query chars matched, in order
        }
        if (text.startsWith(query)) {
            score += 10;
        }
        return score;
    }
}
