package com.example.checkers;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.Map;

public class CheckersServer extends WebSocketServer {

    private static final int PORT = 3000;
    private Queue<WebSocket> playerQueue = new ConcurrentLinkedQueue<>();
    private Map<WebSocket, CheckersGame> games = new HashMap<>();

    public CheckersServer() {
        super(new InetSocketAddress(PORT));
    }


    @Override
    public void onStart() {
        System.out.println("Server started successfully!");
    }
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
        playerQueue.offer(conn);
        matchPlayers();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + conn.getRemoteSocketAddress());
        playerQueue.remove(conn);
        CheckersGame game = games.remove(conn);
        if (game != null) {
            WebSocket opponent = game.getOpponent(conn);
            if (opponent != null) {
                opponent.send("Opponent disconnected, game over.");
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        CheckersGame game = games.get(conn);
        if (game != null) {
            game.handleMove(conn, message);
            if (game.isGameOver()) {
                games.remove(game.getPlayer1());
                games.remove(game.getPlayer2());
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    private void matchPlayers() {
        while (playerQueue.size() >= 2) {
            WebSocket player1 = playerQueue.poll();
            WebSocket player2 = playerQueue.poll();

            if (player1 != null && player2 != null) {
                startGame(player1, player2);
            }
        }
    }

    private void startGame(WebSocket player1, WebSocket player2) {
        System.out.println("Starting a new game between " + player1.getRemoteSocketAddress() + " and " + player2.getRemoteSocketAddress());
        player1.send("Match found! You are player 1.");
        player2.send("Match found! You are player 2.");

        CheckersGame game = new CheckersGame(player1, player2);
        games.put(player1, game);
        games.put(player2, game);
    }

    public static void main(String[] args) {
        CheckersServer server = new CheckersServer();
        server.start();
        System.out.println("Checkers server started on port: " + PORT);
    }
}