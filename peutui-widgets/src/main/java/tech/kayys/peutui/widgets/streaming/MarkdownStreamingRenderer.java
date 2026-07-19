package tech.kayys.peutui.widgets.streaming;

import tech.kayys.peutui.core.text.AnsiCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders Markdown content with streaming support.
 * Handles incremental parsing and formatting of Markdown elements.
 */
public class MarkdownStreamingRenderer extends StreamingRenderer<StreamingToken> {

    private static final Pattern CODE_BLOCK_START = Pattern.compile("^```(\\w*)");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern BOLD = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("\\*([^*]+)\\*");
    private static final Pattern HEADER = Pattern.compile("^(#{1,6})\\s+(.+)$");

    private boolean inCodeBlock = false;
    private String codeBlockLanguage = "";
    private final List<String> renderedLines = new ArrayList<>();
    private CodeBlockHandler codeBlockHandler;

    public interface CodeBlockHandler {
        void onCodeBlockStart(String language);

        void onCodeLine(String line);

        void onCodeBlockEnd();
    }

    @Override
    public void onData(StreamingToken token) {
        buffer.append(token.content());
        parseAndRender();
        triggerRender();

        if (token.isComplete()) {
            onComplete();
        }
    }

    private void parseAndRender() {
        renderedLines.clear();
        String[] lines = buffer.toString().split("\n", -1);

        for (String line : lines) {
            if (inCodeBlock) {
                handleCodeLine(line);
            } else {
                handleNormalLine(line);
            }
        }
    }

    private void handleCodeLine(String line) {
        if (line.trim().equals("```")) {
            inCodeBlock = false;
            if (codeBlockHandler != null) {
                codeBlockHandler.onCodeBlockEnd();
            }
            renderedLines.add(AnsiCodes.DIM + "└" + "─".repeat(40) + AnsiCodes.RESET);
        } else {
            renderedLines.add(AnsiCodes.DIM + "│ " + AnsiCodes.RESET + line);
            if (codeBlockHandler != null) {
                codeBlockHandler.onCodeLine(line);
            }
        }
    }

    private void handleNormalLine(String line) {
        Matcher codeBlockMatcher = CODE_BLOCK_START.matcher(line);
        if (codeBlockMatcher.find()) {
            inCodeBlock = true;
            codeBlockLanguage = codeBlockMatcher.group(1);
            renderedLines.add(AnsiCodes.BOLD + AnsiCodes.CYAN + "┌─ [" +
                    (codeBlockLanguage.isEmpty() ? "code" : codeBlockLanguage) + "] " +
                    "─".repeat(Math.max(0, 35 - codeBlockLanguage.length())) + AnsiCodes.RESET);
            if (codeBlockHandler != null) {
                codeBlockHandler.onCodeBlockStart(codeBlockLanguage);
            }
            return;
        }

        // Handle headers
        Matcher headerMatcher = HEADER.matcher(line);
        if (headerMatcher.find()) {
            int level = headerMatcher.group(1).length();
            String text = headerMatcher.group(2);
            String style = switch (level) {
                case 1 -> AnsiCodes.BOLD + AnsiCodes.UNDERLINE + AnsiCodes.CYAN;
                case 2 -> AnsiCodes.BOLD + AnsiCodes.YELLOW;
                case 3 -> AnsiCodes.BOLD + AnsiCodes.GREEN;
                default -> AnsiCodes.BOLD;
            };
            renderedLines.add(style + text + AnsiCodes.RESET);
            return;
        }

        // Apply inline formatting
        String formatted = line;
        formatted = applyInlineFormatting(formatted);
        renderedLines.add(formatted);
    }

    private String applyInlineFormatting(String text) {
        // Bold
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*",
                AnsiCodes.BOLD + "$1" + AnsiCodes.RESET);
        // Italic
        text = text.replaceAll("\\*([^*]+)\\*",
                AnsiCodes.ITALIC + "$1" + AnsiCodes.RESET);
        // Inline code
        text = text.replaceAll("`([^`]+)`",
                AnsiCodes.DIM + AnsiCodes.BG_DARK_GRAY + "$1" + AnsiCodes.RESET);

        return text;
    }

    public void setCodeBlockHandler(CodeBlockHandler handler) {
        this.codeBlockHandler = handler;
    }

    public List<String> getRenderedLines() {
        return new ArrayList<>(renderedLines);
    }

    @Override
    protected void onReset() {
        inCodeBlock = false;
        codeBlockLanguage = "";
        renderedLines.clear();
    }
}
