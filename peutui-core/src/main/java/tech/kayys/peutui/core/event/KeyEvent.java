package tech.kayys.peutui.core.event;

/**
 * A single decoded key press. {@code character} is populated when
 * {@code code == KeyCode.CHARACTER}. Modifier flags reflect what the
 * terminal reported, which is best-effort and terminal-dependent for some
 * combinations (e.g. Alt+Shift on certain emulators).
 */
public record KeyEvent(KeyCode code, String character, boolean ctrl, boolean alt, boolean shift) implements InputEvent {

    public static KeyEvent of(char c) {
        return new KeyEvent(KeyCode.CHARACTER, String.valueOf(c), false, false, false);
    }

    public static KeyEvent control(KeyCode code) {
        return new KeyEvent(code, "", false, false, false);
    }

    public boolean isCharacter() {
        return code == KeyCode.CHARACTER && character != null && !character.isEmpty();
    }
}
