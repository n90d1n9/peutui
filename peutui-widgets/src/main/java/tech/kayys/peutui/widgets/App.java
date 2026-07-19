package tech.kayys.peutui.widgets;

import tech.kayys.peutui.core.buffer.ScreenBuffer;
import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.event.InputEvent;
import tech.kayys.peutui.core.event.KeyCode;
import tech.kayys.peutui.core.event.KeyEvent;
import tech.kayys.peutui.core.event.ResizeEvent;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Rect;
import tech.kayys.peutui.terminal.TerminalDriver;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The render loop: owns a {@link TerminalDriver}, a root {@link Component},
 * and the current focus target. Every input event triggers focused-component
 * dispatch followed by a diffed repaint, so the terminal only ever receives
 * the rows that actually changed between frames.
 *
 * <p>
 * Not itself a strategy point - App is deliberately the "glue" layer that
 * every other abstraction in this library (agent orchestration, providers,
 * sessions, settings, storage) is wired into via whatever component tree the
 * host application builds.
 */
public final class App {

    private final TerminalDriver driver;
    private final Object renderLock = new Object();
    private volatile Component root;
    private volatile Component focused;
    private volatile ScreenBuffer previousFrame;
    private volatile Predicate<InputEvent> globalInputInterceptor = event -> false;
    private volatile Consumer<Throwable> errorHandler = Throwable::printStackTrace;
    private final java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(
            false);

    public App(TerminalDriver driver, Component root) {
        this.driver = driver;
        this.root = root;
    }

    public void setRoot(Component newRoot) {
        this.root = newRoot;
        requestRender();
    }

    public void setFocused(Component component) {
        this.focused = component;
    }

    /**
     * Installs a handler invoked before per-component dispatch; returning true
     * consumes the event (e.g. global Ctrl+C quit).
     */
    public void setGlobalInputInterceptor(Predicate<InputEvent> interceptor) {
        this.globalInputInterceptor = interceptor != null ? interceptor : event -> false;
    }

    public void setErrorHandler(Consumer<Throwable> handler) {
        this.errorHandler = handler != null ? handler : Throwable::printStackTrace;
    }

    public void start() {
        driver.start();
        driver.onInput(this::onInput);
        running.set(true);
        requestRender();
    }

    public void stop() {
        running.set(false);
        driver.stop();
        synchronized (running) {
            running.notifyAll();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Blocks the calling thread until {@link #stop()} is invoked (e.g. via Ctrl+C
     * on the input thread).
     */
    public void awaitStop() throws InterruptedException {
        synchronized (running) {
            while (running.get()) {
                running.wait(200);
            }
        }
    }

    private void onInput(InputEvent event) {
        try {
            if (globalInputInterceptor.test(event)) {
                requestRender();
                return;
            }
            if (event instanceof ResizeEvent) {
                requestRender();
                return;
            }
            if (event instanceof KeyEvent key && key.code() == KeyCode.CHARACTER && key.ctrl()
                    && "c".equals(key.character())) {
                stop();
                return;
            }
            Component target = focused;
            if (target != null) {
                target.handleInput(event);
            }
            requestRender();
        } catch (RuntimeException e) {
            errorHandler.accept(e);
        }
    }

    /**
     * Triggers an immediate repaint - call after external state changes too (e.g. a
     * streamed agent token arriving).
     */
    public void requestRender() {
        synchronized (renderLock) {
            var size = driver.size();
            ScreenBuffer frame = new ScreenBuffer(size.columns(), size.rows());
            Component currentRoot = root;
            if (currentRoot != null) {
                currentRoot.measure(BoxConstraints.tight(frame.size()));
                currentRoot.render(new RenderContext(frame, new Rect(0, 0, size.columns(), size.rows()), true));
            }
            for (ScreenBuffer.RowDiff diff : frame.diff(previousFrame)) {
                driver.moveCursor(0, diff.row());
                driver.write("\u001b[2K" + diff.content());
            }
            driver.flush();
            previousFrame = frame;
        }
    }
}
