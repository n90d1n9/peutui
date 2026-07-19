package tech.kayys.peutui.core.autocomplete;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A single registrable slash command, e.g. {@code /clear} or {@code /model}.
 */
public interface SlashCommand {

    String name();

    default String description() {
        return "";
    }

    default String argumentHint() {
        return "";
    }

    /**
     * Returns argument completions for this command, or an empty list if none are
     * available.
     */
    default CompletableFuture<List<AutocompleteItem>> getArgumentCompletions(String argumentPrefix) {
        return CompletableFuture.completedFuture(List.of());
    }
}
