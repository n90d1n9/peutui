package tech.kayys.peutui.terminal;

import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.event.ResizeEvent;
import tech.kayys.peutui.core.layout.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Default {@link TerminalDriver} implementation backed by JLine's terminal
 * abstraction. Handles raw mode, the alternate screen buffer, a background
 * input-reading thread feeding an {@link InputDecoder}, and SIGWINCH-driven
 * resize notification.
 */
public final class JLineTerminalDriver implements TerminalDriver {

    private static final String ALT_SCREEN_ON = "\u001b[?1049h";
    private static final String ALT_SCREEN_OFF = "\u001b[?1049l";
    private static final String HIDE_CURSOR = "\u001b[?25l";
    private static final String SHOW_CURSOR = "\u001b[?25h";
    private static final String BRACKETED_PASTE_ON = "\u001b[?2004h";
    private static final String BRACKETED_PASTE_OFF = "\u001b[?2004l";

    private final Terminal terminal;
    private final PrintWriter writer;
    private final InputDecoder decoder = new InputDecoder();
    private final ExecutorService inputExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "peutui-input-reader");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Consumer<InputEvent> listener = event -> {
    };
    private volatile TerminalSize lastKnownSize;

    public JLineTerminalDriver() {
        try {
            this.terminal = TerminalBuilder.builder()
                    .system(true)
                    .jansi(true)
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize terminal", e);
        }
        this.writer = terminal.writer();
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        terminal.enterRawMode();
        writer.print(ALT_SCREEN_ON);
        writer.print(BRACKETED_PASTE_ON);
        writer.print(HIDE_CURSOR);
        writer.flush();
        lastKnownSize = currentSize();
        terminal.handle(Terminal.Signal.WINCH, signal -> onResize());
        inputExecutor.submit(this::readLoop);
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        writer.print(SHOW_CURSOR);
        writer.print(BRACKETED_PASTE_OFF);
        writer.print(ALT_SCREEN_OFF);
        writer.flush();
        inputExecutor.shutdownNow();
        try {
            terminal.close();
        } catch (IOException ignored) {
            // best-effort on shutdown
        }
    }

    private void readLoop() {
        NonBlockingReader reader = terminal.reader();
        while (running.get()) {
            try {
                int c = reader.read(50);
                if (c == NonBlockingReader.READ_EXPIRED) {
                    List<InputEvent> flushed = decoder.flushPendingEscape();
                    flushed.forEach(listener::accept);
                    continue;
                }
                if (c == NonBlockingReader.EOF || c < 0) {
                    break;
                }
                for (InputEvent event : decoder.feed(c)) {
                    listener.accept(event);
                }
            } catch (IOException e) {
                if (running.get()) {
                    break;
                }
            }
        }
    }

    private void onResize() {
        TerminalSize newSize = currentSize();
        if (!newSize.equals(lastKnownSize)) {
            lastKnownSize = newSize;
            listener.accept(new ResizeEvent(new Size(newSize.columns(), newSize.rows())));
        }
    }

    private TerminalSize currentSize() {
        var size = terminal.getSize();
        return new TerminalSize(Math.max(size.getColumns(), 1), Math.max(size.getRows(), 1));
    }

    @Override
    public TerminalSize size() {
        return lastKnownSize != null ? lastKnownSize : currentSize();
    }

    @Override
    public void onInput(Consumer<InputEvent> newListener) {
        this.listener = newListener != null ? newListener : (event -> {
        });
    }

    @Override
    public void write(String rawFrame) {
        writer.print(rawFrame);
    }

    @Override
    public void flush() {
        writer.flush();
    }

    @Override
    public void moveCursor(int column, int row) {
        writer.print("\u001b[" + (row + 1) + ";" + (column + 1) + "H");
    }

    @Override
    public void showCursor(boolean visible) {
        writer.print(visible ? SHOW_CURSOR : HIDE_CURSOR);
    }
}
