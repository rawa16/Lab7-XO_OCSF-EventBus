package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class GameStart implements Serializable {

    private static final long serialVersionUID = 1L;

    private char symbol;
    private boolean yourTurn;

    public GameStart(char symbol, boolean yourTurn) {
        this.symbol = symbol;
        this.yourTurn = yourTurn;
    }

    public char getSymbol() {
        return symbol;
    }

    public boolean isYourTurn() {
        return yourTurn;
    }
}
