package tech.kayys.peutui.widgets.palette;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Abstract base for command palette implementations.
 * Provides filtering, selection, and execution infrastructure.
 */
public abstract class CommandPalette {

    protected final List<CommandItem> commands = new ArrayList<>();
    protected int selectedIndex = 0;
    protected String filterText = "";
    protected Consumer<CommandItem> onExecute;

    /**
     * Registers a command with the palette.
     */
    public void register(CommandItem command) {
        commands.add(command);
    }

    /**
     * Registers multiple commands.
     */
    public void registerAll(List<CommandItem> items) {
        commands.addAll(items);
    }

    /**
     * Removes a command by ID.
     */
    public boolean unregister(String id) {
        return commands.removeIf(cmd -> cmd.id().equals(id));
    }

    /**
     * Gets filtered commands based on current filter text.
     */
    public List<CommandItem> getFilteredCommands() {
        if (filterText.isEmpty()) {
            return commands.stream().filter(CommandItem::enabled).toList();
        }

        String lowerFilter = filterText.toLowerCase();
        return commands.stream()
                .filter(CommandItem::enabled)
                .filter(cmd -> matchesFilter(cmd, lowerFilter))
                .toList();
    }

    private boolean matchesFilter(CommandItem cmd, String filter) {
        return cmd.name().toLowerCase().contains(filter) ||
                cmd.description().toLowerCase().contains(filter) ||
                cmd.category().toLowerCase().contains(filter) ||
                cmd.shortcut().toLowerCase().contains(filter);
    }

    /**
     * Updates the filter text and resets selection.
     */
    public void setFilter(String filter) {
        this.filterText = filter != null ? filter : "";
        this.selectedIndex = 0;
    }

    /**
     * Moves selection up.
     */
    public void selectUp() {
        var filtered = getFilteredCommands();
        if (!filtered.isEmpty()) {
            selectedIndex = Math.max(0, selectedIndex - 1);
        }
    }

    /**
     * Moves selection down.
     */
    public void selectDown() {
        var filtered = getFilteredCommands();
        if (!filtered.isEmpty()) {
            selectedIndex = Math.min(filtered.size() - 1, selectedIndex + 1);
        }
    }

    /**
     * Executes the currently selected command.
     */
    public boolean executeSelected() {
        var filtered = getFilteredCommands();
        if (selectedIndex >= 0 && selectedIndex < filtered.size()) {
            CommandItem selected = filtered.get(selectedIndex);
            if (onExecute != null) {
                onExecute.accept(selected);
            }
            handleExecution(selected);
            return true;
        }
        return false;
    }

    /**
     * Hook for subclasses to handle command execution.
     */
    protected abstract void handleExecution(CommandItem command);

    /**
     * Sets the execute callback.
     */
    public void setOnExecute(Consumer<CommandItem> callback) {
        this.onExecute = callback;
    }

    /**
     * Gets the currently selected index.
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Gets the current filter text.
     */
    public String getFilterText() {
        return filterText;
    }

    /**
     * Resets the palette state.
     */
    public void reset() {
        filterText = "";
        selectedIndex = 0;
    }
}
