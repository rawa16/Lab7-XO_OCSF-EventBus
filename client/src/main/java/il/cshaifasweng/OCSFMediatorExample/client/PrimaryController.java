package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.BoardUpdate;
import il.cshaifasweng.OCSFMediatorExample.entities.GameStart;
import il.cshaifasweng.OCSFMediatorExample.entities.MoveRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Window;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

public class PrimaryController {

    @FXML private Button b00; @FXML private Button b01; @FXML private Button b02;
    @FXML private Button b10; @FXML private Button b11; @FXML private Button b12;
    @FXML private Button b20; @FXML private Button b21; @FXML private Button b22;

    @FXML private Label statusLabel;

    private char mySymbol = '\0';   // 'X'/'O'
    private boolean myTurn = false;

    @FXML
    void initialize() {

        // Wire buttons to send MoveRequest
        wire(b00, 0, 0); wire(b01, 0, 1); wire(b02, 0, 2);
        wire(b10, 1, 0); wire(b11, 1, 1); wire(b12, 1, 2);
        wire(b20, 2, 0); wire(b21, 2, 1); wire(b22, 2, 2);

        setStatus("Waiting for game to start...");
        setBoardDisabled(true); // until GameStart arrives

        // Register this controller as an EventBus subscriber
        EventBus.getDefault().register(this);

        // Unregister automatically when the window is closed
        Platform.runLater(() -> {
            if (statusLabel != null) {
                Window w = statusLabel.getScene() != null ? statusLabel.getScene().getWindow() : null;
                if (w != null) {
                    w.setOnHidden(e -> safeUnregister());
                }
            }
        });
    }

    private void safeUnregister() {
        try {
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this);
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    /**
     * EventBus handler: called when server sends GameStart
     */
    @Subscribe
    public void onGameStart(GameStartEvent event) {
        GameStart gs = event.getGameStart();
        applyGameStart(gs.getSymbol(), gs.isYourTurn());
    }

    /**
     * EventBus handler: called when server sends BoardUpdate
     */
    @Subscribe
    public void onBoardUpdate(BoardUpdateEvent event) {
        applyBoardUpdate(event.getBoardUpdate());
    }

    private void wire(Button btn, int r, int c) {
        btn.setOnAction(e -> {
            if (!myTurn) return;                    // not my turn
            if (!btn.getText().isEmpty()) return;   // already filled

            try {
                SimpleClient cClient = SimpleClient.getClient();

                // If your AbstractClient has isConnected(), this is good.
                // If it doesn't exist in your version, tell me and I'll adapt it.
                if (cClient == null || !cClient.isConnected()) {
                    setStatus("Not connected to server");
                    return;
                }

                cClient.sendToServer(new MoveRequest(r, c));
            } catch (IOException ex) {
                ex.printStackTrace();
                setStatus("Failed to send move");
            }
        });
    }

    private void setStatus(String s) {
        if (statusLabel != null) statusLabel.setText(s);
    }

    private void setBoardDisabled(boolean disabled) {
        b00.setDisable(disabled); b01.setDisable(disabled); b02.setDisable(disabled);
        b10.setDisable(disabled); b11.setDisable(disabled); b12.setDisable(disabled);
        b20.setDisable(disabled); b21.setDisable(disabled); b22.setDisable(disabled);
    }

    private void setButtonsFromBoard(char[][] board) {
        b00.setText(cell(board[0][0])); b01.setText(cell(board[0][1])); b02.setText(cell(board[0][2]));
        b10.setText(cell(board[1][0])); b11.setText(cell(board[1][1])); b12.setText(cell(board[1][2]));
        b20.setText(cell(board[2][0])); b21.setText(cell(board[2][1])); b22.setText(cell(board[2][2]));
    }

    private void disableFilledCells(char[][] board) {
        // Disable only cells that are already filled
        b00.setDisable(board[0][0] != '\0'); b01.setDisable(board[0][1] != '\0'); b02.setDisable(board[0][2] != '\0');
        b10.setDisable(board[1][0] != '\0'); b11.setDisable(board[1][1] != '\0'); b12.setDisable(board[1][2] != '\0');
        b20.setDisable(board[2][0] != '\0'); b21.setDisable(board[2][1] != '\0'); b22.setDisable(board[2][2] != '\0');
    }

    private void showGameOver(char winner) {
        String msg;
        if (winner == 'T') {
            msg = "Draw!";
        } else if (winner == 'X' || winner == 'O') {
            msg = "Winner: " + winner;
        } else {
            msg = "Game Over";
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText("Game Over");
        alert.show();
    }

    private static String cell(char ch) {
        return ch == '\0' ? "" : String.valueOf(ch);
    }



    private void applyGameStart(char symbol, boolean yourTurn) {
        Platform.runLater(() -> {
            this.mySymbol = symbol;
            this.myTurn = yourTurn;

            setStatus("You are " + symbol + (yourTurn ? " — Your turn" : " — Waiting..."));

            if (yourTurn) {
                setBoardDisabled(false); // enable all, then will be limited by BoardUpdate
            } else {
                setBoardDisabled(true);
            }
        });
    }

    private void applyBoardUpdate(BoardUpdate upd) {
        Platform.runLater(() -> {
            char[][] board = upd.getBoard();
            setButtonsFromBoard(board);

            // If game is over
            if (upd.isGameOver()) {
                myTurn = false;
                setBoardDisabled(true);
                showGameOver(upd.getWinner());
                return;
            }

            // If we still didn't receive GameStart
            if (mySymbol == '\0') {
                setStatus("Waiting for game start...");
                setBoardDisabled(true);
                return;
            }


            myTurn = (upd.getNextTurn() == mySymbol);

            setStatus(myTurn
                    ? "Your turn (" + mySymbol + ")"
                    : "Opponent's turn (next: " + upd.getNextTurn() + ")");

            // Enable/disable buttons
            if (myTurn) {
                setBoardDisabled(false);
                disableFilledCells(board);
            } else {
                setBoardDisabled(true);
            }
        });
    }
}
