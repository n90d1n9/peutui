package tech.kayys.peutui.core.autocomplete;

import java.util.List;

/**
 * The resulting buffer state after applying a completion: new lines and new
 * cursor position.
 */
public record CompletionEdit(List<String> lines, int cursorLine, int cursorCol) {
}
