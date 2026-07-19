package tech.kayys.peutui.core.autocomplete;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Strategy interface for a single source of autocomplete suggestions (file
 * paths, slash commands, @-mentions, agent names, etc). Multiple providers
 * are aggregated by {@link CompositeAutocompleteEngine}; each is free to
 * decide, per keystroke, whether it has anything to offer.
 */
public interface AutocompleteProvider {

    /**
     * Characters that should naturally trigger this provider at a token boundary
     * (e.g. {@code '/'}, {@code '@'}).
     */
    default List<Character> triggerCharacters() {
        return List.of();
    }

    /**
     * Computes suggestions for the current multi-line buffer and cursor
     * position, or an empty result if this provider has nothing to offer
     * here. {@code force} indicates an explicit Tab press rather than a
     * trigger-character-driven lookup.
     */
    CompletableFuture<AutocompleteSuggestions> getSuggestions(List<String> lines, int cursorLine, int cursorCol,
            boolean force);

    /** Applies a chosen suggestion, producing the new buffer state. */
    CompletionEdit applyCompletion(List<String> lines, int cursorLine, int cursorCol, AutocompleteItem item,
            String prefix);

    /**
     * Whether explicit Tab-completion should be attempted by this provider at the
     * current position.
     */
    default boolean shouldTriggerFileCompletion(List<String> lines, int cursorLine, int cursorCol) {
        return false;
    }
}
