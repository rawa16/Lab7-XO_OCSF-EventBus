package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.BoardUpdate;

public class BoardUpdateEvent {
    private final BoardUpdate boardUpdate;

    public BoardUpdateEvent(BoardUpdate boardUpdate) {
        this.boardUpdate = boardUpdate;
    }

    public BoardUpdate getBoardUpdate() {
        return boardUpdate;
    }
}
