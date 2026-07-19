package tech.kayys.peutui.core.autocomplete;

/** A single candidate offered by an {@link AutocompleteProvider}. */
public record AutocompleteItem(String value, String label, String description) {

    public AutocompleteItem(String value, String label) {
        this(value, label, null);
    }
}
