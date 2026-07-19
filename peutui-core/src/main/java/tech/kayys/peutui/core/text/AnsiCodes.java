package tech.kayys.peutui.core.text;

import java.util.regex.Pattern;

/**
 * Constants and small helpers for building/recognizing ANSI SGR (Select
 * Graphic Rendition) escape sequences. Kept dependency-free so it can be
 * reused by any renderer or terminal backend.
 */
public final class AnsiCodes {

    /** Matches a single CSI escape sequence, e.g. {@code \u001b[38;5;12m}. */
    public static final Pattern CSI_SEQUENCE = Pattern.compile("\u001b\\[[0-9;]*[a-zA-Z]");

    public static final String RESET = "\u001b[0m";
    public static final String BOLD = "\u001b[1m";
    public static final String DIM = "\u001b[2m";
    public static final String ITALIC = "\u001b[3m";
    public static final String UNDERLINE = "\u001b[4m";
    public static final String INVERSE = "\u001b[7m";
    public static final String STRIKETHROUGH = "\u001b[9m";

    // Standard foreground colors (bright variants available via +60)
    public static final String BLACK = "\u001b[30m";
    public static final String RED = "\u001b[31m";
    public static final String GREEN = "\u001b[32m";
    public static final String YELLOW = "\u001b[33m";
    public static final String BLUE = "\u001b[34m";
    public static final String MAGENTA = "\u001b[35m";
    public static final String CYAN = "\u001b[36m";
    public static final String WHITE = "\u001b[37m";

    // Bright foreground colors
    public static final String BRIGHT_BLACK = "\u001b[90m";
    public static final String BRIGHT_RED = "\u001b[91m";
    public static final String BRIGHT_GREEN = "\u001b[92m";
    public static final String BRIGHT_YELLOW = "\u001b[93m";
    public static final String BRIGHT_BLUE = "\u001b[94m";
    public static final String BRIGHT_MAGENTA = "\u001b[95m";
    public static final String BRIGHT_CYAN = "\u001b[96m";
    public static final String BRIGHT_WHITE = "\u001b[97m";

    // Background colors
    public static final String BG_BLACK = "\u001b[40m";
    public static final String BG_RED = "\u001b[41m";
    public static final String BG_GREEN = "\u001b[42m";
    public static final String BG_YELLOW = "\u001b[43m";
    public static final String BG_BLUE = "\u001b[44m";
    public static final String BG_MAGENTA = "\u001b[45m";
    public static final String BG_CYAN = "\u001b[46m";
    public static final String BG_WHITE = "\u001b[47m";

    // Dark background colors (bright variants)
    public static final String BG_DARK_GRAY = "\u001b[100m";
    public static final String BG_BRIGHT_RED = "\u001b[101m";
    public static final String BG_BRIGHT_GREEN = "\u001b[102m";
    public static final String BG_BRIGHT_YELLOW = "\u001b[103m";
    public static final String BG_BRIGHT_BLUE = "\u001b[104m";
    public static final String BG_BRIGHT_MAGENTA = "\u001b[105m";
    public static final String BG_BRIGHT_CYAN = "\u001b[106m";
    public static final String BG_BRIGHT_WHITE = "\u001b[107m";

    private AnsiCodes() {
    }

    public static String fg256(int colorIndex) {
        return "\u001b[38;5;" + colorIndex + "m";
    }

    public static String bg256(int colorIndex) {
        return "\u001b[48;5;" + colorIndex + "m";
    }

    public static String fgRgb(int r, int g, int b) {
        return "\u001b[38;2;" + r + ";" + g + ";" + b + "m";
    }

    public static String bgRgb(int r, int g, int b) {
        return "\u001b[48;2;" + r + ";" + g + ";" + b + "m";
    }

    /** Strips all CSI/SGR escape sequences from the given text. */
    public static String strip(String text) {
        return CSI_SEQUENCE.matcher(text).replaceAll("");
    }

    /**
     * If {@code text} starts with a CSI sequence at {@code index}, returns its
     * length in chars; otherwise returns 0.
     */
    public static int matchLength(CharSequence text, int index) {
        var matcher = CSI_SEQUENCE.matcher(text);
        if (matcher.find(index) && matcher.start() == index) {
            return matcher.end() - matcher.start();
        }
        return 0;
    }
}
