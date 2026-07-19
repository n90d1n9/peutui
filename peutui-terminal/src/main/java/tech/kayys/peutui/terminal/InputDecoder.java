package tech.kayys.peutui.terminal;

import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.event.KeyCode;
import tech.kayys.peutui.core.event.KeyEvent;
import tech.kayys.peutui.core.event.PasteEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateful decoder translating a stream of raw terminal input characters
 * into {@link InputEvent}s: single keys, common escape sequences (arrows,
 * Home/End, Page Up/Down, function keys), Ctrl-combinations, and
 * bracketed-paste blocks ({@code ESC[200~ ... ESC[201~}).
 *
 * <p>
 * Feed characters one at a time via {@link #feed(int)}; fully decoded
 * events are returned immediately, partially-received escape sequences are
 * buffered internally until they resolve or time out (callers should treat
 * a lone ESC not followed by further input within ~50ms as the Escape key -
 * that timing policy lives in the {@link TerminalDriver} implementation,
 * not here).
 */
public final class InputDecoder {

    private static final String PASTE_START = "\u001b[200~";
    private static final String PASTE_END = "\u001b[201~";

    private final StringBuilder pending = new StringBuilder();
    private boolean inPaste = false;
    private final StringBuilder pasteBuffer = new StringBuilder();

    /**
     * Feeds one decoded character (codepoint) into the decoder; returns zero or
     * more resolved events.
     */
    public List<InputEvent> feed(int codepoint) {
        List<InputEvent> out = new ArrayList<>();
        char c = (char) codepoint;

        if (inPaste) {
            pasteBuffer.append(c);
            if (endsWith(pasteBuffer, PASTE_END)) {
                pasteBuffer.setLength(pasteBuffer.length() - PASTE_END.length());
                out.add(new PasteEvent(pasteBuffer.toString()));
                pasteBuffer.setLength(0);
                inPaste = false;
            }
            return out;
        }

        pending.append(c);
        String buf = pending.toString();

        if (buf.equals("\u001b") || isPrefixOfKnownSequence(buf)) {
            if (buf.equals(PASTE_START)) {
                pending.setLength(0);
                inPaste = true;
                return out;
            }
            KeyEvent resolved = resolveSequence(buf);
            if (resolved != null) {
                pending.setLength(0);
                out.add(resolved);
            }
            // else: still an incomplete/ambiguous sequence, keep buffering.
            return out;
        }

        // Not an escape sequence at all: flush whatever was pending as-is, then handle
        // this char.
        pending.setLength(0);
        out.add(decodePlainChar(c));
        return out;
    }

    private static boolean endsWith(CharSequence buf, String suffix) {
        int bl = buf.length();
        int sl = suffix.length();
        if (bl < sl) {
            return false;
        }
        for (int i = 0; i < sl; i++) {
            if (buf.charAt(bl - sl + i) != suffix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPrefixOfKnownSequence(String buf) {
        for (String known : KNOWN_SEQUENCES.keySet()) {
            if (known.startsWith(buf)) {
                return true;
            }
        }
        return PASTE_START.startsWith(buf) || buf.startsWith("\u001b[") || buf.startsWith("\u001bO")
                || buf.equals("\u001b");
    }

    private static KeyEvent resolveSequence(String buf) {
        KeyCode direct = KNOWN_SEQUENCES.get(buf);
        if (direct != null) {
            return KeyEvent.control(direct);
        }
        if (buf.length() == 1 && buf.charAt(0) == '\u001b') {
            return null; // wait to see if more bytes follow (real Escape vs sequence start)
        }
        return null;
    }

    private static KeyEvent decodePlainChar(char c) {
        return switch (c) {
            case '\r', '\n' -> KeyEvent.control(KeyCode.ENTER);
            case '\t' -> KeyEvent.control(KeyCode.TAB);
            case (char) 127, (char) 8 -> KeyEvent.control(KeyCode.BACKSPACE);
            case (char) 27 -> KeyEvent.control(KeyCode.ESCAPE);
            default -> {
                if (c < 32) {
                    // Ctrl+<letter> range (Ctrl+A=1 .. Ctrl+Z=26).
                    char letter = (char) (c + 96);
                    yield new KeyEvent(KeyCode.CHARACTER, String.valueOf(letter), true, false, false);
                }
                yield KeyEvent.of(c);
            }
        };
    }

    private static final java.util.Map<String, KeyCode> KNOWN_SEQUENCES = java.util.Map.ofEntries(
            java.util.Map.entry("\u001b[A", KeyCode.UP),
            java.util.Map.entry("\u001b[B", KeyCode.DOWN),
            java.util.Map.entry("\u001b[C", KeyCode.RIGHT),
            java.util.Map.entry("\u001b[D", KeyCode.LEFT),
            java.util.Map.entry("\u001bOA", KeyCode.UP),
            java.util.Map.entry("\u001bOB", KeyCode.DOWN),
            java.util.Map.entry("\u001bOC", KeyCode.RIGHT),
            java.util.Map.entry("\u001bOD", KeyCode.LEFT),
            java.util.Map.entry("\u001b[H", KeyCode.HOME),
            java.util.Map.entry("\u001b[F", KeyCode.END),
            java.util.Map.entry("\u001b[1~", KeyCode.HOME),
            java.util.Map.entry("\u001b[4~", KeyCode.END),
            java.util.Map.entry("\u001b[3~", KeyCode.DELETE),
            java.util.Map.entry("\u001b[5~", KeyCode.PAGE_UP),
            java.util.Map.entry("\u001b[6~", KeyCode.PAGE_DOWN),
            java.util.Map.entry("\u001bOP", KeyCode.F1),
            java.util.Map.entry("\u001bOQ", KeyCode.F2),
            java.util.Map.entry("\u001bOR", KeyCode.F3),
            java.util.Map.entry("\u001bOS", KeyCode.F4));

    /**
     * True while a bracketed-paste block is being accumulated and hasn't resolved
     * to a {@link PasteEvent} yet.
     */
    public boolean isInPaste() {
        return inPaste;
    }

    /**
     * If a bare ESC has been buffered with nothing following, flush it out as the
     * Escape key.
     */
    public List<InputEvent> flushPendingEscape() {
        if (pending.length() == 1 && pending.charAt(0) == '\u001b') {
            pending.setLength(0);
            return List.of(KeyEvent.control(KeyCode.ESCAPE));
        }
        return List.of();
    }
}
