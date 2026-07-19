package tech.kayys.peutui.core.autocomplete;

import java.util.List;

/**
 * The result of asking a provider for suggestions: candidates plus the text
 * prefix they match against.
 */
public record AutocompleteSuggestions(List<AutocompleteItem> items, String prefix) {
}
