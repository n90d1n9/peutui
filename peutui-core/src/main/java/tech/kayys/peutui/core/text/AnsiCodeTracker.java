package tech.kayys.peutui.core.text;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks the currently "active" SGR codes as a stream of ANSI escape
 * sequences is processed, so that a later fragment of text can be prefixed
 * with the styling that was in effect at a given point (used when splitting
 * a styled line into "before" / "after" segments for overlay compositing).
 *
 * <p>
 * This is intentionally a simple last-write-wins model keyed by SGR
 * "category" (color, bold, underline, ...): a reset code ({@code ESC[0m})
 * clears all tracked state.
 */
public final class AnsiCodeTracker {

    private final Map<String, String> activeByCategory = new LinkedHashMap<>();

    public void clear() {
        activeByCategory.clear();
    }

    /**
     * Feed a single CSI/SGR escape sequence (e.g. {@code "\u001b[1m"}) into the
     * tracker.
     */
    public void process(String code) {
        if (code == null || code.isEmpty()) {
            return;
        }
        if (code.equals(AnsiCodes.RESET) || code.equals("\u001b[m")) {
            clear();
            return;
        }
        String category = categoryOf(code);
        activeByCategory.put(category, code);
    }

    /**
     * Returns the concatenation of all currently active SGR codes, in insertion
     * order.
     */
    public String getActiveCodes() {
        if (activeByCategory.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String code : activeByCategory.values()) {
            sb.append(code);
        }
        return sb.toString();
    }

    /**
     * Buckets an SGR code into a coarse category so that, e.g., a new
     * foreground color overwrites a previous foreground color but does not
     * clear bold/underline state.
     */
    private static String categoryOf(String code) {
        String body = code;
        if (body.startsWith("\u001b[")) {
            body = body.substring(2);
        }
        if (body.endsWith("m")) {
            body = body.substring(0, body.length() - 1);
        }
        if (body.startsWith("38;") || body.equals("39")) {
            return "fg";
        }
        if (body.startsWith("48;") || body.equals("49")) {
            return "bg";
        }
        if (body.equals("1") || body.equals("22")) {
            return "bold";
        }
        if (body.equals("2")) {
            return "dim";
        }
        if (body.equals("3") || body.equals("23")) {
            return "italic";
        }
        if (body.equals("4") || body.equals("24")) {
            return "underline";
        }
        if (body.equals("7") || body.equals("27")) {
            return "inverse";
        }
        if (body.equals("9") || body.equals("29")) {
            return "strikethrough";
        }
        // Basic 30-37 / 90-97 foreground and 40-47 / 100-107 background codes.
        try {
            int n = Integer.parseInt(body);
            if ((n >= 30 && n <= 37) || (n >= 90 && n <= 97)) {
                return "fg";
            }
            if ((n >= 40 && n <= 47) || (n >= 100 && n <= 107)) {
                return "bg";
            }
        } catch (NumberFormatException ignored) {
            // fall through to generic bucket
        }
        return "other:" + body;
    }
}
