package server;

import map.MazeGen;
import service.PlayerService;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket-based game server for Mini Island 2D
 * This server handles client connections via WebSocket instead of TCP Socket
 */
public class WebSocketGameServer extends WebSocketServer {

    private ArrayList<ClientInfo> playerOnline;
    private Map<WebSocket, String> connectionAuthMap; // Track authentication per connection
    private Protocol protocol;
    private PlayerService playerService;

    //Maze gen
    private MazeGen mazeGen = new MazeGen(10, 20);
    private boolean winMaze = true;

    public WebSocketGameServer(int port) {
        super(new InetSocketAddress(port));
        playerOnline = new ArrayList<ClientInfo>();
        connectionAuthMap = new HashMap<>();
        protocol = new Protocol();
        playerService = new PlayerService();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection from " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + conn.getRemoteSocketAddress());
        
        // Find and remove player associated with this connection
        String username = connectionAuthMap.get(conn);
        if (username != null) {
            for (ClientInfo player : playerOnline) {
                if (player != null && player.getUsername().equals(username)) {
                    playerOnline.remove(player);
                    broadcastMessage("Exit" + username);
                    break;
                }
            }
            connectionAuthMap.remove(conn);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            handleMessage(conn, message);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error handling message: " + message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket Game Server started successfully!");
    }

    private void handleMessage(WebSocket conn, String sentence) {
        int defaultX = 1645;
        int defaultY = 754;

        if (sentence.startsWith("Login")) {
            handleLogin(conn, sentence);
        } else if (sentence.startsWith("Register")) {
            handleRegister(conn, sentence);
        } else if (sentence.startsWith("Hello")) {
            handleHello(conn, sentence, defaultX, defaultY);
        } else if (sentence.startsWith("Update")) {
            handleUpdate(sentence);
        } else if (sentence.startsWith("TeleportToMap")) {
            handleTeleportToMap(sentence);
        } else if (sentence.startsWith("EnterMaze")) {
            handleEnterMaze(sentence);
        } else if (sentence.startsWith("WinMaze")) {
            handleWinMaze(sentence);
        } else if (sentence.startsWith("BulletCollision")) {
            handleBulletCollision(sentence);
        } else if (sentence.startsWith("Respawn")) {
            handleRespawn(sentence);
        } else if (sentence.startsWith("Chat")) {
            broadcastMessage(sentence);
        } else if (sentence.startsWith("Shot")) {
            broadcastMessage(sentence);
        } else if (sentence.startsWith("Remove")) {
            handleRemove(sentence);
        } else if (sentence.startsWith("Exit")) {
            handleExit(sentence);
        } else if (sentence.startsWith("Exit Auth")) {
            handleExitAuth(conn);
        } else if (sentence.startsWith("GET_ITEMS")) {
            handleGetItems(conn);
        } else if (sentence.startsWith("BUY_ITEM")) {
            handleBuyItem(conn, sentence);
        }
    }

    private void handleLogin(WebSocket conn, String sentence) {
        String[] parts = sentence.split(",");
        String username = parts[1];
        String password = parts[2];

        String result = playerService.login(username, password);
        String msg = result.substring(result.indexOf('|') + 1, result.length());

        // Check if user is already logged in
        for (ClientInfo clientInfo : playerOnline) {
            if (clientInfo != null && clientInfo.getUsername().equals(username)) {
                sendToClient(conn, protocol.LoginPacket("Failed", "User already logged in"));
                return;
            }
        }

        if (result.startsWith("Success")) {
            System.out.println("Login Success: " + username);
            sendToClient(conn, protocol.LoginPacket("Success", msg));
        } else {
            System.out.println("Login Failed: " + username);
            sendToClient(conn, protocol.LoginPacket("Failed", msg));
        }
    }

    private void handleRegister(WebSocket conn, String sentence) {
        String[] parts = sentence.split(",");
        String username = parts[1];
        String password = parts[2];
        String email = parts[3];

        String result = playerService.register(username, password, email);
        int posResult = result.indexOf('|');
        String msg = result.substring(posResult + 1, result.length());

        if (result.startsWith("Success")) {
            sendToClient(conn, protocol.registerPacket("Success", msg));
            System.out.println("Register Success: " + username);
        } else {
            sendToClient(conn, protocol.registerPacket("Failed", msg));
            System.out.println("Register Failed: " + username);
        }
    }

    private void handleHello(WebSocket conn, String sentence, int defaultX, int defaultY) {
        String username = sentence.substring(5, sentence.length());
        
        // Store the authenticated username for this connection
        connectionAuthMap.put(conn, username);

        sendToClient(conn, protocol.IDPacket(playerOnline.size() + 1, username));
        broadcastMessage(protocol.NewClientPacket(username, defaultX, defaultY, -1, playerOnline.size() + 1, "lobby"));

        System.out.println(protocol.leaderBoardPacket(playerService.leaderBoard()));
        sendToClient(conn, protocol.leaderBoardPacket(playerService.leaderBoard()));

        sendAllClientsInMap(conn, "lobby");

        playerOnline.add(new ClientInfo(conn, username, defaultX, defaultY, -1, "lobby"));
    }

    private void handleUpdate(String sentence) {
        String[] parts = sentence.split(",");
        String username = parts[1];
        int x = Integer.parseInt(parts[2]);
        int y = Integer.parseInt(parts[3]);
        int dir = Integer.parseInt(parts[4]);

        playerOnline.stream()
                .filter(player -> player.getUsername().equals(username))
                .findFirst()
                .ifPresent(player -> {
                    player.setPosX(x);
                    player.setPosY(y);
                    player.setDirection(dir);
                });

        playerOnline.stream()
                .filter(player -> !player.getUsername().equals(username))
                .forEach(player -> sendToClient(player.getWebSocket(), sentence));
    }

    private void handleTeleportToMap(String sentence) {
        String[] parts = sentence.split(",");
        String username = parts[1];
        String map = parts[2];
        int x = Integer.parseInt(parts[3]);
        int y = Integer.parseInt(parts[4]);

        ClientInfo p = null;
        for (ClientInfo player : playerOnline) {
            if (player != null && player.getUsername().equals(username)) {
                player.setPosX(x);
                player.setPosY(y);
                player.setMap(map);
                p = player;
                break;
            }
        }

        broadcastMessage(protocol.NewClientPacket(username, x, y, -1, playerOnline.size() + 1, p.getMap()));
        sendAllClientsInMap(p.getWebSocket(), map);
        broadcastMessage(sentence);
    }

    private void handleEnterMaze(String sentence) {
        String username = sentence.substring(9);

        ClientInfo p = null;
        for (ClientInfo player : playerOnline) {
            if (player != null && player.getUsername().equals(username)) {
                p = player;
                player.setMap("Loading");
                break;
            }
        }
        if (winMaze) {
            mazeGen = new MazeGen(10, 20);
            mazeGen.solve();
            winMaze = false;
        }

        assert p != null;
        sendToClient(p.getWebSocket(), protocol.mazeMapPacket(mazeGen.toString()));
    }

    private void handleWinMaze(String sentence) {
        String username = sentence.substring(7);

        ClientInfo p = null;
        for (ClientInfo player : playerOnline) {
            if (player != null && player.getUsername().equals(username)) {
                p = player;
                player.setMap("lobby");
                break;
            }
        }

        assert p != null;

        broadcastMessage(protocol.NewClientPacket(username, 1645, 754, -1, playerOnline.size() + 1, p.getMap()));
        playerService.updatePoint(username, 50);
        sendLeaderBoardToAllClient();
        sendAllClientsInMap(p.getWebSocket(), "lobby");
        teleportAllPlayerInMapToMap("maze", "lobby");
        winMaze = true;
    }

    private void handleBulletCollision(String sentence) {
        String[] parts = sentence.split(",");
        String playerShot = parts[1];
        String playerHit = parts[2];

        for (ClientInfo player : playerOnline) {
            if (player != null && player.getUsername().equals(playerHit) && player.isAlive) {
                playerService.updatePoint(playerShot, 10);
                playerService.updatePoint(playerHit, -10);
                sendLeaderBoardToAllClient();
                broadcastMessage(sentence);
                break;
            }
        }
        for (ClientInfo player : playerOnline) {
            if (player != null && player.getUsername().equals(playerHit)) {
                player.isAlive = false;
                break;
            }
        }
    }

    private void handleRespawn(String sentence) {
        String username = sentence.substring(7);

        for (ClientInfo player : playerOnline) {
            if (player != null && player.getUsername().equals(username)) {
                player.isAlive = true;
                break;
            }
        }
    }

    private void handleRemove(String sentence) {
        int id = Integer.parseInt(sentence.substring(6));
        broadcastMessage(sentence);
        playerOnline.remove(id);
    }

    private void handleExit(String sentence) {
        String username = sentence.substring(4);
        System.out.println("Exit: " + username);

        for (ClientInfo player : playerOnline) {
            if (player != null && player.getUsername().equals(username)) {
                playerOnline.remove(player);
                connectionAuthMap.remove(player.getWebSocket());
                break;
            }
        }
        broadcastMessage(sentence);
    }

    private void handleExitAuth(WebSocket conn) {
        connectionAuthMap.remove(conn);
        conn.close();
    }

    private void handleGetItems(WebSocket conn) {
        List<String> items = fetchItemsFromDatabase();
        for (String item : items) {
            sendToClient(conn, item);
        }
        sendToClient(conn, ""); // End of items
    }

    private void handleBuyItem(WebSocket conn, String sentence) {
        String[] parts = sentence.split(",");
        int userId = Integer.parseInt(parts[1]);
        int itemId = Integer.parseInt(parts[2]);
        String result = buyItem(userId, itemId);
        sendToClient(conn, result);
    }

    private List<String> fetchItemsFromDatabase() {
        List<String> items = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/game_shop", "root", "password");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT id, name, price FROM items")) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                int price = resultSet.getInt("price");
                items.add(id + ": " + name + " - " + price + " coins");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    private String buyItem(int userId, int itemId) {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/game_shop", "root", "password")) {
            PreparedStatement checkBalanceStmt = connection.prepareStatement("SELECT balance FROM users WHERE id = ?");
            checkBalanceStmt.setInt(1, userId);
            ResultSet balanceResult = checkBalanceStmt.executeQuery();
            if (balanceResult.next()) {
                int balance = balanceResult.getInt("balance");

                PreparedStatement getItemPriceStmt = connection.prepareStatement("SELECT price FROM items WHERE id = ?");
                getItemPriceStmt.setInt(1, itemId);
                ResultSet priceResult = getItemPriceStmt.executeQuery();
                if (priceResult.next()) {
                    int price = priceResult.getInt("price");

                    if (balance >= price) {
                        PreparedStatement updateBalanceStmt = connection.prepareStatement("UPDATE users SET balance = balance - ? WHERE id = ?");
                        updateBalanceStmt.setInt(1, price);
                        updateBalanceStmt.setInt(2, userId);
                        updateBalanceStmt.executeUpdate();

                        PreparedStatement addItemStmt = connection.prepareStatement("INSERT INTO userItems (user_id, item_id) VALUES (?, ?)");
                        addItemStmt.setInt(1, userId);
                        addItemStmt.setInt(2, itemId);
                        addItemStmt.executeUpdate();

                        return "Purchase successful!";
                    } else {
                        return "Insufficient balance!";
                    }
                } else {
                    return "Item not found!";
                }
            } else {
                return "User not found!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Purchase failed!";
        }
    }

    public void teleportAllPlayerInMapToMap(String map, String map2) {
        for (ClientInfo player : playerOnline) {
            if (player.getMap().equals(map)) {
                player.setMap(map2);
                sendToClient(player.getWebSocket(), protocol.teleportPacket(player.getUsername(), map2, 1645, 754));
                broadcastMessage(protocol.NewClientPacket(player.getUsername(), 1645, 754, -1, playerOnline.size() + 1, player.getMap()));
                sendAllClientsInMap(player.getWebSocket(), map2);
            }
        }
    }

    public void broadcastMessage(String message) {
        for (ClientInfo clientInfo : playerOnline) {
            if (clientInfo != null && clientInfo.getWebSocket() != null && clientInfo.getWebSocket().isOpen()) {
                sendToClient(clientInfo.getWebSocket(), message);
            }
        }
    }

    public void sendToClient(WebSocket conn, String message) {
        if (conn != null && conn.isOpen()) {
            conn.send(message);
        }
    }

    public void sendLeaderBoardToAllClient() {
        for (ClientInfo clientInfo : playerOnline) {
            if (clientInfo != null && clientInfo.getWebSocket() != null) {
                sendLeaderBoardToClient(clientInfo.getWebSocket());
            }
        }
    }

    public void sendLeaderBoardToClient(WebSocket conn) {
        sendToClient(conn, protocol.leaderBoardPacket(playerService.leaderBoard()));
    }

    public void sendAllClientsInMap(WebSocket conn, String map) {
        for (int i = 0; i < playerOnline.size(); i++) {
            if (playerOnline.get(i) != null && playerOnline.get(i).getMap().equals(map)) {
                String username = playerOnline.get(i).getUsername();
                int x = playerOnline.get(i).getX();
                int y = playerOnline.get(i).getY();
                int dir = playerOnline.get(i).getDir();
                sendToClient(conn, protocol.NewClientPacket(username, x, y, dir, i + 1, map));
            }
        }
    }

    public ArrayList<ClientInfo> getPlayerOnline() {
        return playerOnline;
    }

    public void startServer() {
        start();
    }

    public void stopServer() throws IOException, InterruptedException {
        stop();
    }
}
