package tech.kayys.peutui.widgets;

import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.component.RenderContext;
import tech.kayys.peutui.core.layout.BoxConstraints;
import tech.kayys.peutui.core.layout.Size;
import tech.kayys.peutui.core.text.AnsiCodes;

/**
 * A single-line busy indicator. {@link #tick()} advances the animation frame
 * and should be called from a timer/scheduler driving periodic
 * {@code App.requestRender()} calls while a long-running operation (an
 * agent turn, a provider call) is in flight.
 */
public final class SpinnerComponent implements Component {

    private static final char[] FRAMES = { '\u280B', '\u2819', '\u2839', '\u2838', '\u283C', '\u2834', '\u2826',
            '\u2827', '\u2807', '\u280F' };

    private volatile int frame = 0;
    private volatile String label = "";
    private volatile boolean visible = false;

    public void start(String label) {
        this.label = label == null ? "" : label;
        this.visible = true;
    }

    public void stop() {
        this.visible = false;
    }

    public void tick() {
        frame = (frame + 1) % FRAMES.length;
    }

    @Override
    public Size measure(BoxConstraints constraints) {
        return new Size(constraints.maxWidth(), visible ? 1 : 0);
    }

    @Override
    public void render(RenderContext context) {
        if (!visible) {
            return;
        }
        var area = context.area();
        String text = AnsiCodes.fg256(45) + FRAMES[frame] + AnsiCodes.RESET + " " + label;
        context.buffer().writeAt(area.x(), area.y(), text);
    }
}
