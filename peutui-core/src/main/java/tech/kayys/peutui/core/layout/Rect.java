package tech.kayys.peutui.core.layout;

/** An axis-aligned rectangle in terminal-cell coordinates, origin top-left. */
public record Rect(int x, int y, int width, int height) {

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(int px, int py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }

    public Rect inset(int amount) {
        return new Rect(x + amount, y + amount, Math.max(0, width - 2 * amount), Math.max(0, height - 2 * amount));
    }
}
