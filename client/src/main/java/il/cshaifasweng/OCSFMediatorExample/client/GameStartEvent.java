package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.GameStart;

public class GameStartEvent {
    private final GameStart gameStart;

    public GameStartEvent(GameStart gameStart) {
        this.gameStart = gameStart;
    }

    public GameStart getGameStart() {
        return gameStart;
    }
}
