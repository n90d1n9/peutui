package tech.kayys.peutui.core.layout;

/**
 * Min/max width and height a {@link tech.kayys.peutui.core.component.Component}
 * may occupy when measuring itself during layout, mirroring the constraint
 * model used by most box-layout systems (Flutter's BoxConstraints, CSS
 * min/max-width, etc.).
 */
public record BoxConstraints(int minWidth, int maxWidth, int minHeight, int maxHeight) {

    public static BoxConstraints tight(Size size) {
        return new BoxConstraints(size.width(), size.width(), size.height(), size.height());
    }

    public static BoxConstraints tight(int width, int height) {
        return new BoxConstraints(width, width, height, height);
    }

    public static BoxConstraints loose(int maxWidth, int maxHeight) {
        return new BoxConstraints(0, maxWidth, 0, maxHeight);
    }

    public Size clamp(Size proposed) {
        int w = Math.min(Math.max(proposed.width(), minWidth), maxWidth);
        int h = Math.min(Math.max(proposed.height(), minHeight), maxHeight);
        return new Size(w, h);
    }
}
