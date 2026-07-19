package tech.kayys.peutui.widgets.streaming;

import java.util.function.Consumer;

/**
 * Abstract base class for streaming content renderers.
 * Provides a framework for incrementally rendering streaming content like AI
 * responses.
 * 
 * @param <T> The type of streaming data (e.g., StreamingToken, MarkdownBlock)
 */
public abstract class StreamingRenderer<T> {

    protected final StringBuilder buffer = new StringBuilder();
    protected boolean isComplete = false;
    protected Consumer<String> renderCallback;

    /**
     * Called when a new chunk of data arrives.
     * Implementations should update internal state and trigger re-render.
     */
    public abstract void onData(T data);

    /**
     * Called when the stream is complete.
     */
    public void onComplete() {
        this.isComplete = true;
        onStreamComplete();
    }

    /**
     * Hook for subclasses to handle stream completion.
     */
    protected void onStreamComplete() {
        // Default no-op
    }

    /**
     * Resets the renderer state for a new stream.
     */
    public void reset() {
        buffer.setLength(0);
        isComplete = false;
        onReset();
    }

    /**
     * Hook for subclasses to handle reset.
     */
    protected void onReset() {
        // Default no-op
    }

    /**
     * Gets the current accumulated content.
     */
    public String getContent() {
        return buffer.toString();
    }

    /**
     * Sets the callback for triggering renders.
     */
    public void setRenderCallback(Consumer<String> callback) {
        this.renderCallback = callback;
    }

    /**
     * Triggers a render update.
     */
    protected void triggerRender() {
        if (renderCallback != null) {
            renderCallback.accept(getContent());
        }
    }

    /**
     * Checks if the stream is complete.
     */
    public boolean isComplete() {
        return isComplete;
    }
}
