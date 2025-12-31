package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class BoardUpdate implements Serializable {
    private static final long serialVersionUID = 1L;

    private final char[][] board;
    private final char nextTurn;
    private final boolean gameOver;
    private final char winner;

    public BoardUpdate(char[][] board, char nextTurn, boolean gameOver, char winner) {
        this.board = board;
        this.nextTurn = nextTurn;
        this.gameOver = gameOver;
        this.winner = winner;
    }

    public char[][] getBoard() { return board; }
    public char getNextTurn() { return nextTurn; }
    public boolean isGameOver() { return gameOver; }
    public char getWinner() { return winner; }
}
