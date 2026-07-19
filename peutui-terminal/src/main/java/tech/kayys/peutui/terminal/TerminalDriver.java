package tech.kayys.peutui.terminal;

import tech.kayys.peutui.core.event.InputEvent;

import java.util.function.Consumer;

/**
 * Strategy interface for terminal I/O. Concrete implementations own raw-mode
 * lifecycle, translate raw input into {@link InputEvent}s, and write frames
 * produced by the render loop. Swappable so the same {@code peutui-widgets}
 * render loop can run against a real TTY (JLine), a headless test double, or
 * a remote PTY bridge.
 */
public interface TerminalDriver extends AutoCloseable {

    /**
     * Puts the terminal into raw mode (no line buffering/echo) and enables the
     * alternate screen.
     */
    void start();

    /**
     * Restores the terminal to its original mode and leaves the alternate screen.
     */
    void stop();

    /** Current terminal size. */
    TerminalSize size();

    /**
     * Registers a listener invoked for every decoded input event (key press,
     * resize, paste).
     */
    void onInput(Consumer<InputEvent> listener);

    /**
     * Writes a raw pre-rendered frame (ANSI included) to the terminal, without an
     * implicit trailing newline.
     */
    void write(String rawFrame);

    /** Forces an immediate flush of any buffered output. */
    void flush();

    /** Moves the terminal cursor to the given zero-based column/row. */
    void moveCursor(int column, int row);

    void showCursor(boolean visible);

    @Override
    default void close() {
        stop();
    }
}
