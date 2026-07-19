package tech.kayys.peutui.widgets.palette;

import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a command in the command palette.
 */
public record CommandItem(
        String id,
        String name,
        String description,
        String shortcut,
        String category,
        boolean enabled) {
    public CommandItem(String id, String name, String description) {
        this(id, name, description, "", "", true);
    }

    public CommandItem(String id, String name, String description, String shortcut) {
        this(id, name, description, shortcut, "", true);
    }
}
