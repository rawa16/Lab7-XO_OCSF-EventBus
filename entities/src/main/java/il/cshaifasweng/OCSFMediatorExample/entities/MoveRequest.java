package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class MoveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row;
    private final int col;

    public MoveRequest(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    @Override
    public String toString() {
        return "MoveRequest{row=" + row + ", col=" + col + "}";
    }
}
