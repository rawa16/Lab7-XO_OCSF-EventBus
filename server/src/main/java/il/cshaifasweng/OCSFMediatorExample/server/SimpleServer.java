package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.BoardUpdate;
import il.cshaifasweng.OCSFMediatorExample.entities.GameStart;
import il.cshaifasweng.OCSFMediatorExample.entities.MoveRequest;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * SimpleServer implements a Tic-Tac-Toe (XO) game server.
 * It supports exactly two players at a time and communicates
 * with clients using OCSF and EventBus.
 */
public class SimpleServer extends AbstractServer {

    /**
     * List of subscribed clients (used for EventBus demo, not game logic)
     */
    private static final ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();

    /**
     * References to the two players connected to the game
     */
    private ConnectionToClient player1 = null;
    private ConnectionToClient player2 = null;

    /**
     * Game board: '\0' = empty, 'X' or 'O'
     */
    private final char[][] board = new char[3][3];

    /**
     * Which player number ('1' or '2') is assigned symbol 'X'
     */
    private char xPlayer = '1';

    /**
     * Whose turn it is now: 'X' or 'O'
     */
    private char currentTurn = 'X';

    public SimpleServer(int port) {
        super(port);
    }

    /**
     * Called automatically by OCSF when a client connects
     */
    @Override
    protected void clientConnected(ConnectionToClient client) {
        super.clientConnected(client);

        // First connected client becomes Player 1
        if (player1 == null) {
            player1 = client;
            try {
                client.sendToClient("Connected as Player 1. Waiting for Player 2...");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Second connected client becomes Player 2
        if (player2 == null) {
            player2 = client;
            try {
                client.sendToClient("Connected as Player 2. Starting game...");
            } catch (IOException e) {
                e.printStackTrace();
            }
            startGame();
            return;
        }

        // If already two players are connected, reject additional clients
        try {
            client.sendToClient("Game already has 2 players. Try later.");
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called automatically by OCSF when a client disconnects
     */
    @Override
    protected void clientDisconnected(ConnectionToClient client) {
        super.clientDisconnected(client);

        // Remove from subscribers list if exists
        SubscribersList.removeIf(sc -> client.equals(sc.getClient()));

        boolean wasPlayer = false;

        if (client.equals(player1)) {
            player1 = null;
            wasPlayer = true;
        } else if (client.equals(player2)) {
            player2 = null;
            wasPlayer = true;
        }

        // If a player disconnected, notify the remaining player
        if (wasPlayer) {
            ConnectionToClient other = (player1 != null) ? player1 : player2;
            if (other != null) {
                try {
                    other.sendToClient("Opponent disconnected. Waiting for a new player...");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Reset game state
            resetBoard();
            currentTurn = 'X';
            xPlayer = '1';
        }
    }

    /**
     * Clears the game board
     */
    private void resetBoard() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                board[r][c] = '\0';
            }
        }
    }

    /**
     * Initializes a new game between two connected players
     */
    private void startGame() {
        resetBoard();
        Random rnd = new Random();

        // Randomly decide who gets X
        boolean player1IsX = rnd.nextBoolean();
        xPlayer = player1IsX ? '1' : '2';

        // Randomly decide who starts
        currentTurn = rnd.nextBoolean() ? 'X' : 'O';

        char p1Symbol = player1IsX ? 'X' : 'O';
        char p2Symbol = player1IsX ? 'O' : 'X';

        boolean p1Turn = (p1Symbol == currentTurn);
        boolean p2Turn = (p2Symbol == currentTurn);

        try {
            player1.sendToClient(new GameStart(p1Symbol, p1Turn));
            player2.sendToClient(new GameStart(p2Symbol, p2Turn));

            // Send initial empty board
            broadcastBoardUpdate(currentTurn, false, '\0');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles messages sent from clients
     */
    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {

        // Handle string-based commands
        if (msg instanceof String) {
            String s = (String) msg;

            // Demo of sending Warning object (EventBus example)
            if (s.startsWith("#warning")) {
                Warning warning = new Warning("Warning from server!");
                try {
                    client.sendToClient(warning);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            // Subscribe client (EventBus demo)
            if (s.startsWith("add client")) {
                SubscribersList.add(new SubscribedClient(client));
                try {
                    client.sendToClient("client added successfully");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            // Unsubscribe client
            if (s.startsWith("remove client")) {
                SubscribersList.removeIf(sc -> client.equals(sc.getClient()));
                return;
            }

            return;
        }

        // Handle a move request
        if (msg instanceof MoveRequest) {
            handleMove((MoveRequest) msg, client);
            return;
        }

        // Unknown message type
        System.out.println("Unknown message type: " + msg);
    }

    /**
     * Processes a single move in the game
     */
    private void handleMove(MoveRequest move, ConnectionToClient client) {

        // Game requires exactly two players
        if (player1 == null || player2 == null) return;

        int r = move.getRow();
        int c = move.getCol();

        // Bounds check
        if (r < 0 || r > 2 || c < 0 || c > 2) return;

        // Determine client's symbol
        char clientSymbol = symbolFor(client);
        if (clientSymbol == '\0') return;

        // Check turn
        if (clientSymbol != currentTurn) {
            try {
                client.sendToClient("Not your turn");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Cell must be empty
        if (board[r][c] != '\0') return;

        // Apply move
        board[r][c] = clientSymbol;

        // Check for winner
        char winner = checkWinner();
        if (winner != '\0') {
            broadcastBoardUpdate(currentTurn, true, winner);
            return;
        }

        // Check for draw
        if (isDraw()) {
            broadcastBoardUpdate(currentTurn, true, 'T');
            return;
        }

        // Continue game
        currentTurn = (currentTurn == 'X') ? 'O' : 'X';
        broadcastBoardUpdate(currentTurn, false, '\0');
    }

    /**
     * Returns the symbol ('X' or 'O') of a given client
     */
    private char symbolFor(ConnectionToClient client) {
        boolean isP1 = client.equals(player1);
        boolean isP2 = client.equals(player2);
        if (!isP1 && !isP2) return '\0';

        char playerNum = isP1 ? '1' : '2';
        return (playerNum == xPlayer) ? 'X' : 'O';
    }

    /**
     * Checks if there is a winner on the board
     */
    private char checkWinner() {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != '\0' &&
                    board[i][0] == board[i][1] &&
                    board[i][1] == board[i][2]) {
                return board[i][0];
            }

            if (board[0][i] != '\0' &&
                    board[0][i] == board[1][i] &&
                    board[1][i] == board[2][i]) {
                return board[0][i];
            }
        }

        if (board[0][0] != '\0' &&
                board[0][0] == board[1][1] &&
                board[1][1] == board[2][2]) {
            return board[0][0];
        }

        if (board[0][2] != '\0' &&
                board[0][2] == board[1][1] &&
                board[1][1] == board[2][0]) {
            return board[0][2];
        }

        return '\0';
    }

    /**
     * Checks if the game ended in a draw
     */
    private boolean isDraw() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c] == '\0') return false;
            }
        }
        return checkWinner() == '\0';
    }

    /**
     * Sends a BoardUpdate object to both players
     */
    private void broadcastBoardUpdate(char nextTurn, boolean gameOver, char winner) {
        char[][] copy = new char[3][3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, 3);
        }

        BoardUpdate upd = new BoardUpdate(copy, nextTurn, gameOver, winner);

        try {
            if (player1 != null) player1.sendToClient(upd);
            if (player2 != null) player2.sendToClient(upd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
