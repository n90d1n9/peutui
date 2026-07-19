package tech.kayys.peutui.widgets;

import tech.kayys.peutui.core.autocomplete.AutocompleteItem;
import tech.kayys.peutui.core.autocomplete.AutocompleteSuggestions;
import tech.kayys.peutui.core.autocomplete.CompositeAutocompleteEngine;
import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.event.KeyCode;
import tech.kayys.peutui.core.event.KeyEvent;
import tech.kayys.peutui.core.event.PasteEvent;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Size;
import tech.kayys.peutui.core.text.AnsiCodes;
import tech.kayys.peutui.core.text.TextMeasure;
import tech.kayys.peutui.core.text.WordNavigation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A multi-line text input with cursor movement, word navigation (Ctrl+Left /
 * Ctrl+Right), bracketed-paste support, and an optional
 * {@link CompositeAutocompleteEngine} driving a suggestion popup. Enter
 * submits by default (via {@link #onSubmit}); Shift+Enter (reported as a
 * plain Enter with no alt/ctrl by most terminals, so hosts may instead bind
 * a distinct key) can be wired to insert a newline via
 * {@link #insertNewline()}.
 */
public final class TextInputComponent implements Component {

    private final List<StringBuilder> lines = new ArrayList<>(List.of(new StringBuilder()));
    private int cursorLine = 0;
    private int cursorCol = 0;

    private CompositeAutocompleteEngine autocomplete;
    private AutocompleteSuggestions activeSuggestions = new AutocompleteSuggestions(List.of(), "");
    private int suggestionIndex = 0;

    private Consumer<String> onSubmit = text -> {
    };
    private String placeholder = "";
    private String promptPrefix = "> ";

    public void setAutocomplete(CompositeAutocompleteEngine engine) {
        this.autocomplete = engine;
    }

    public void setOnSubmit(Consumer<String> handler) {
        this.onSubmit = handler != null ? handler : text -> {
        };
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
    }

    public void setPromptPrefix(String prefix) {
        this.promptPrefix = prefix == null ? "" : prefix;
    }

    public String text() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    public void clear() {
        lines.clear();
        lines.add(new StringBuilder());
        cursorLine = 0;
        cursorCol = 0;
        activeSuggestions = new AutocompleteSuggestions(List.of(), "");
    }

    public void insertNewline() {
        String currentLine = lines.get(cursorLine).toString();
        String before = currentLine.substring(0, cursorCol);
        String after = currentLine.substring(cursorCol);
        lines.set(cursorLine, new StringBuilder(before));
        lines.add(cursorLine + 1, new StringBuilder(after));
        cursorLine++;
        cursorCol = 0;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public Size measure(BoxConstraints constraints) {
        int height = Math.min(Math.max(lines.size(), 1), constraints.maxHeight());
        return new Size(constraints.maxWidth(), Math.max(height, constraints.minHeight()));
    }

    @Override
    public void render(RenderContext context) {
        var area = context.area();
        var buffer = context.buffer();
        boolean showPlaceholder = lines.size() == 1 && lines.get(0).isEmpty() && !placeholder.isEmpty();

        for (int row = 0; row < area.height(); row++) {
            int lineIdx = row;
            String prefix = row == 0 ? promptPrefix : " ".repeat(TextMeasure.visibleWidth(promptPrefix));
            String content;
            if (showPlaceholder && row == 0) {
                content = AnsiCodes.DIM + placeholder + AnsiCodes.RESET;
            } else if (lineIdx < lines.size()) {
                content = lines.get(lineIdx).toString();
            } else {
                content = "";
            }
            buffer.writeAt(area.x(), area.y() + row, prefix + content);
        }
        renderSuggestionPopup(context);
    }

    private void renderSuggestionPopup(RenderContext context) {
        if (activeSuggestions.items().isEmpty()) {
            return;
        }
        var area = context.area();
        var buffer = context.buffer();
        int popupRow = area.y() + Math.min(lines.size(), area.height());
        int maxItems = Math.min(activeSuggestions.items().size(), 6);
        for (int i = 0; i < maxItems; i++) {
            AutocompleteItem item = activeSuggestions.items().get(i);
            boolean selected = i == suggestionIndex;
            String style = selected ? AnsiCodes.INVERSE : "";
            String label = " " + item.label()
                    + (item.description() != null ? "  " + AnsiCodes.DIM + item.description() + AnsiCodes.RESET : "");
            buffer.writeAt(area.x(), popupRow + i, style + label + AnsiCodes.RESET);
        }
    }

    @Override
    public boolean handleInput(InputEvent event) {
        if (event instanceof PasteEvent paste) {
            insertText(paste.text());
            return true;
        }
        if (!(event instanceof KeyEvent key)) {
            return false;
        }
        if (!activeSuggestions.items().isEmpty() && handleSuggestionNavigation(key)) {
            return true;
        }
        return switch (key.code()) {
            case CHARACTER -> {
                if (key.ctrl()) {
                    yield handleControlChar(key.character());
                }
                insertText(key.character());
                yield true;
            }
            case BACKSPACE -> {
                backspace();
                yield true;
            }
            case DELETE -> {
                deleteForward();
                yield true;
            }
            case ENTER -> {
                submit();
                yield true;
            }
            case TAB -> requestCompletion(true);
            case LEFT -> {
                moveLeft(key.ctrl());
                yield true;
            }
            case RIGHT -> {
                moveRight(key.ctrl());
                yield true;
            }
            case UP -> {
                if (cursorLine > 0) {
                    cursorLine--;
                    cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                }
                yield true;
            }
            case DOWN -> {
                if (cursorLine < lines.size() - 1) {
                    cursorLine++;
                    cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                }
                yield true;
            }
            case HOME -> {
                cursorCol = 0;
                yield true;
            }
            case END -> {
                cursorCol = lines.get(cursorLine).length();
                yield true;
            }
            case ESCAPE -> {
                activeSuggestions = new AutocompleteSuggestions(List.of(), "");
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleControlChar(String character) {
        if ("a".equals(character)) {
            cursorCol = 0;
            return true;
        }
        if ("e".equals(character)) {
            cursorCol = lines.get(cursorLine).length();
            return true;
        }
        if ("w".equals(character)) {
            deleteWordBackward();
            return true;
        }
        return false;
    }

    private boolean handleSuggestionNavigation(KeyEvent key) {
        return switch (key.code()) {
            case UP -> {
                suggestionIndex = Math.floorMod(suggestionIndex - 1, activeSuggestions.items().size());
                yield true;
            }
            case DOWN -> {
                suggestionIndex = Math.floorMod(suggestionIndex + 1, activeSuggestions.items().size());
                yield true;
            }
            case TAB, ENTER -> {
                applySelectedSuggestion();
                yield true;
            }
            case ESCAPE -> {
                activeSuggestions = new AutocompleteSuggestions(List.of(), "");
                yield true;
            }
            default -> false;
        };
    }

    private void applySelectedSuggestion() {
        if (autocomplete == null || activeSuggestions.items().isEmpty()) {
            return;
        }
        AutocompleteItem item = activeSuggestions.items().get(suggestionIndex);
        List<String> asStrings = linesAsStrings();
        for (var provider : autocomplete.providers()) {
            var edit = provider.applyCompletion(asStrings, cursorLine, cursorCol, item, activeSuggestions.prefix());
            if (edit != null) {
                lines.clear();
                for (String l : edit.lines()) {
                    lines.add(new StringBuilder(l));
                }
                cursorLine = edit.cursorLine();
                cursorCol = edit.cursorCol();
                break;
            }
        }
        activeSuggestions = new AutocompleteSuggestions(List.of(), "");
        suggestionIndex = 0;
    }

    private boolean requestCompletion(boolean force) {
        if (autocomplete == null) {
            return false;
        }
        autocomplete.getSuggestions(linesAsStrings(), cursorLine, cursorCol, force).thenAccept(result -> {
            activeSuggestions = result;
            suggestionIndex = 0;
        });
        return true;
    }

    private void insertText(String text) {
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                insertNewline();
            } else {
                lines.get(cursorLine).insert(cursorCol, c);
                cursorCol++;
            }
        }
        maybeTriggerAutocomplete();
    }

    private void maybeTriggerAutocomplete() {
        if (autocomplete == null) {
            return;
        }
        String currentLine = lines.get(cursorLine).toString();
        if (cursorCol > 0) {
            char last = currentLine.charAt(cursorCol - 1);
            if (autocomplete.providers().stream().anyMatch(p -> p.triggerCharacters().contains(last))) {
                requestCompletion(false);
                return;
            }
        }
        if (!activeSuggestions.items().isEmpty()) {
            requestCompletion(false);
        }
    }

    private void backspace() {
        if (cursorCol > 0) {
            lines.get(cursorLine).deleteCharAt(cursorCol - 1);
            cursorCol--;
        } else if (cursorLine > 0) {
            int prevLen = lines.get(cursorLine - 1).length();
            lines.get(cursorLine - 1).append(lines.get(cursorLine));
            lines.remove(cursorLine);
            cursorLine--;
            cursorCol = prevLen;
        }
        maybeTriggerAutocomplete();
    }

    private void deleteForward() {
        StringBuilder line = lines.get(cursorLine);
        if (cursorCol < line.length()) {
            line.deleteCharAt(cursorCol);
        } else if (cursorLine < lines.size() - 1) {
            line.append(lines.get(cursorLine + 1));
            lines.remove(cursorLine + 1);
        }
    }

    private void deleteWordBackward() {
        String line = lines.get(cursorLine).toString();
        int newCol = WordNavigation.findWordBackward(line, cursorCol);
        lines.set(cursorLine, new StringBuilder(line.substring(0, newCol) + line.substring(cursorCol)));
        cursorCol = newCol;
    }

    private void moveLeft(boolean wordwise) {
        if (wordwise) {
            cursorCol = WordNavigation.findWordBackward(lines.get(cursorLine).toString(), cursorCol);
        } else if (cursorCol > 0) {
            cursorCol--;
        } else if (cursorLine > 0) {
            cursorLine--;
            cursorCol = lines.get(cursorLine).length();
        }
    }

    private void moveRight(boolean wordwise) {
        if (wordwise) {
            cursorCol = WordNavigation.findWordForward(lines.get(cursorLine).toString(), cursorCol);
        } else if (cursorCol < lines.get(cursorLine).length()) {
            cursorCol++;
        } else if (cursorLine < lines.size() - 1) {
            cursorLine++;
            cursorCol = 0;
        }
    }

    private void submit() {
        String value = text();
        clear();
        onSubmit.accept(value);
    }

    private List<String> linesAsStrings() {
        List<String> result = new ArrayList<>(lines.size());
        for (StringBuilder l : lines) {
            result.add(l.toString());
        }
        return result;
    }
}
