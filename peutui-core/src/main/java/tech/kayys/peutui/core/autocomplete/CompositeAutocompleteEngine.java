package tech.kayys.peutui.core.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Aggregates several {@link AutocompleteProvider}s behind a single entry
 * point. On each query, providers are asked in registration order and the
 * first one that returns non-empty suggestions "wins" the query (a provider
 * for {@code '/'} slash commands and one for {@code '@'} file mentions don't
 * usually both want to answer the same keystroke). This is itself a strategy
 * that callers may swap out for e.g. a merge-all-providers behavior.
 */
public final class CompositeAutocompleteEngine {

    private final List<AutocompleteProvider> providers = new ArrayList<>();

    public CompositeAutocompleteEngine register(AutocompleteProvider provider) {
        providers.add(provider);
        return this;
    }

    public List<AutocompleteProvider> providers() {
        return List.copyOf(providers);
    }

    public CompletableFuture<AutocompleteSuggestions> getSuggestions(List<String> lines, int cursorLine, int cursorCol,
            boolean force) {
        return resolveInOrder(providers.iterator(), lines, cursorLine, cursorCol, force);
    }

    private CompletableFuture<AutocompleteSuggestions> resolveInOrder(
            java.util.Iterator<AutocompleteProvider> iterator, List<String> lines, int cursorLine, int cursorCol,
            boolean force) {
        if (!iterator.hasNext()) {
            return CompletableFuture.completedFuture(new AutocompleteSuggestions(List.of(), ""));
        }
        AutocompleteProvider next = iterator.next();
        return next.getSuggestions(lines, cursorLine, cursorCol, force).thenCompose(result -> {
            if (result != null && !result.items().isEmpty()) {
                return CompletableFuture.completedFuture(result);
            }
            return resolveInOrder(iterator, lines, cursorLine, cursorCol, force);
        });
    }
}
