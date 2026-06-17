package chess;

public interface MoveProvider {

    int[] pickMove(Board board);

    String displayName();
}
