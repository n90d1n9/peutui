package tech.kayys.peutui.core.layout;

/** An integer terminal-cell size (columns x rows). */
public record Size(int width, int height) {
    public static final Size ZERO = new Size(0, 0);
}
