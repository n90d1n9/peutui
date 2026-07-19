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
 * An enhanced chat input component with modern TUI features:
 * <ul>
 * <li>Multi-line support with visual line wrapping</li>
 * <li>Cursor visibility with blinking control</li>
 * <li>Enhanced autocomplete popup with descriptions</li>
 * <li>Character count and limit display</li>
 * <li>History navigation (up/down for previous inputs)</li>
 * <li>Visual cursor block/underline styling</li>
 * <li>Placeholder text with dim styling</li>
 * <li>Configurable prompt prefix</li>
 * </ul>
 */
public final class ChatInputComponent implements Component {

    private final List<StringBuilder> lines = new ArrayList<>(List.of(new StringBuilder()));
    private int cursorLine = 0;
    private int cursorCol = 0;

    // Input history for up/down navigation
    private final List<String> inputHistory = new ArrayList<>();
    private int historyIndex = -1;
    private String currentDraft = "";

    private CompositeAutocompleteEngine autocomplete;
    private AutocompleteSuggestions activeSuggestions = new AutocompleteSuggestions(List.of(), "");
    private int suggestionIndex = 0;

    private Consumer<String> onSubmit = text -> {
    };
    private String placeholder = "";
    private String promptPrefix = "╰─❯ ";

    // Configuration
    private int maxLines = 10;
    private int maxChars = 4000;
    private boolean showCharCount = true;
    private boolean showLineNumbers = false;
    private boolean cursorVisible = true;
    private boolean blinkCursor = true;
    private long lastBlinkTime = System.currentTimeMillis();
    private static final long BLINK_INTERVAL_MS = 500;

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

    public void setMaxLines(int max) {
        this.maxLines = Math.max(1, max);
    }

    public void setMaxChars(int max) {
        this.maxChars = Math.max(1, max);
    }

    public void setShowCharCount(boolean show) {
        this.showCharCount = show;
    }

    public void setShowLineNumbers(boolean show) {
        this.showLineNumbers = show;
    }

    public void setCursorVisible(boolean visible) {
        this.cursorVisible = visible;
    }

    public void setBlinkCursor(boolean blink) {
        this.blinkCursor = blink;
    }

    /** Returns the full text content. */
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

    /** Returns character count. */
    public int charCount() {
        return lines.stream().mapToInt(StringBuilder::length).sum();
    }

    /** Clears the input and resets history position. */
    public void clear() {
        lines.clear();
        lines.add(new StringBuilder());
        cursorLine = 0;
        cursorCol = 0;
        activeSuggestions = new AutocompleteSuggestions(List.of(), "");
        historyIndex = -1;
        currentDraft = "";
    }

    /** Inserts a newline at cursor position. */
    public void insertNewline() {
        if (lines.size() >= maxLines) {
            return;
        }
        String currentLine = lines.get(cursorLine).toString();
        String before = currentLine.substring(0, cursorCol);
        String after = currentLine.substring(cursorCol);
        lines.set(cursorLine, new StringBuilder(before));
        lines.add(cursorLine + 1, new StringBuilder(after));
        cursorLine++;
        cursorCol = 0;
    }

    /** Adds a string to input history. */
    public void addToHistory(String text) {
        if (text != null && !text.isBlank()) {
            inputHistory.add(text);
            if (inputHistory.size() > 100) {
                inputHistory.remove(0);
            }
            historyIndex = -1;
        }
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public Size measure(BoxConstraints constraints) {
        int height = Math.min(Math.max(lines.size(), 1), maxLines);
        return new Size(constraints.maxWidth(), Math.max(height + (showCharCount ? 1 : 0), constraints.minHeight()));
    }

    @Override
    public void render(RenderContext context) {
        var area = context.area();
        var buffer = context.buffer();
        boolean showPlaceholder = lines.size() == 1 && lines.get(0).isEmpty() && !placeholder.isEmpty();

        // Update cursor blink state
        boolean cursorOn = !blinkCursor || cursorVisible;
        if (blinkCursor) {
            long now = System.currentTimeMillis();
            if (now - lastBlinkTime >= BLINK_INTERVAL_MS) {
                cursorVisible = !cursorVisible;
                lastBlinkTime = now;
                cursorOn = cursorVisible;
            }
        }

        int renderHeight = showCharCount ? area.height() - 1 : area.height();

        for (int row = 0; row < renderHeight; row++) {
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

            // Add line number if enabled
            if (showLineNumbers) {
                String lineNum = String.format("%2d ", lineIdx + 1);
                prefix = AnsiCodes.DIM + lineNum + AnsiCodes.RESET + prefix;
            }

            // Render cursor on current line
            if (lineIdx == cursorLine && cursorOn) {
                String before = content.substring(0, Math.min(cursorCol, content.length()));
                String after = content.length() > cursorCol ? content.substring(cursorCol) : "";
                char cursorChar = cursorCol < content.length() ? content.charAt(cursorCol) : ' ';
                content = before + AnsiCodes.INVERSE + cursorChar + AnsiCodes.RESET + after;
            }

            buffer.writeAt(area.x(), area.y() + row, prefix + content);
        }

        // Render character count
        if (showCharCount && renderHeight < area.height()) {
            int countRow = area.y() + renderHeight;
            int count = charCount();
            String countText = count + "/" + maxChars;
            if (count > maxChars * 0.9) {
                countText = AnsiCodes.BOLD + AnsiCodes.RED + countText + AnsiCodes.RESET;
            } else {
                countText = AnsiCodes.DIM + countText + AnsiCodes.RESET;
            }
            buffer.writeAt(area.x() + area.width() - countText.length() - 2, countRow, countText);
        }

        renderSuggestionPopup(context);
    }

    private void renderSuggestionPopup(RenderContext context) {
        if (activeSuggestions.items().isEmpty()) {
            return;
        }
        var area = context.area();
        var buffer = context.buffer();
        int popupRow = area.y() + Math.min(lines.size(), area.height() - 1);
        int maxItems = Math.min(activeSuggestions.items().size(), 6);

        for (int i = 0; i < maxItems; i++) {
            AutocompleteItem item = activeSuggestions.items().get(i);
            boolean selected = i == suggestionIndex;
            String style = selected ? AnsiCodes.INVERSE : AnsiCodes.DIM;
            String label = " " + item.label();
            String description = item.description() != null
                    ? "  " + AnsiCodes.DIM + item.description() + AnsiCodes.RESET
                    : "";
            buffer.writeAt(area.x(), popupRow + i, style + label + AnsiCodes.RESET + description);
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
                if (handleHistoryUp()) {
                    yield true;
                }
                if (cursorLine > 0) {
                    cursorLine--;
                    cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                }
                yield true;
            }
            case DOWN -> {
                if (handleHistoryDown()) {
                    yield true;
                }
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
            case PAGE_UP -> {
                cursorLine = 0;
                cursorCol = 0;
                yield true;
            }
            case PAGE_DOWN -> {
                cursorLine = lines.size() - 1;
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

    private boolean handleHistoryUp() {
        if (inputHistory.isEmpty() || historyIndex == 0) {
            return false;
        }

        if (historyIndex == -1) {
            currentDraft = text();
        }

        historyIndex = Math.min(historyIndex + 1, inputHistory.size() - 1);
        loadHistoryEntry(inputHistory.get(inputHistory.size() - 1 - historyIndex));
        return true;
    }

    private boolean handleHistoryDown() {
        if (inputHistory.isEmpty() || historyIndex == -1) {
            return false;
        }

        historyIndex--;
        if (historyIndex == -1) {
            loadHistoryEntry(currentDraft);
        } else {
            loadHistoryEntry(inputHistory.get(inputHistory.size() - 1 - historyIndex));
        }
        return true;
    }

    private void loadHistoryEntry(String text) {
        lines.clear();
        for (String line : text.split("\n", -1)) {
            lines.add(new StringBuilder(line));
        }
        if (lines.isEmpty()) {
            lines.add(new StringBuilder());
        }
        cursorLine = lines.size() - 1;
        cursorCol = lines.get(cursorLine).length();
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
        if ("u".equals(character)) {
            deleteLineBeforeCursor();
            return true;
        }
        if ("k".equals(character)) {
            deleteLineAfterCursor();
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
            if (charCount() >= maxChars) {
                break;
            }
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

    private void deleteLineBeforeCursor() {
        lines.get(cursorLine).delete(0, cursorCol);
        cursorCol = 0;
    }

    private void deleteLineAfterCursor() {
        lines.get(cursorLine).delete(cursorCol, lines.get(cursorLine).length());
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
        addToHistory(value);
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
