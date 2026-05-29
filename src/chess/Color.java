package chess;

public enum Color {
    WHITE, BLACK;

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    public String label() {
        return this == WHITE ? "white" : "black";
    }

    public static Color fromLabel(String s) {
        return "white".equals(s) ? WHITE : BLACK;
    }
}
