package server;

import map.MazeGen;
import service.PlayerService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is responsible for handling client connections and managing the game state.
 * It listens for incoming connections from clients and creates a new thread to handle each client.
 * It also updates the game state based on the messages received from the clients.
 * It is the main class for the server application.
 */
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Thread {

    private ArrayList<ClientInfo> playerOnline;
    private ServerSocket serverSocket;
    private int serverPort = 11111;

    private DataInputStream reader;
    private DataOutputStream writer;

    private Protocol protocol;
    private boolean running = true;

    private PlayerService playerService;

    private ExecutorService executorService;

    //Maze gen
    private MazeGen mazeGen = new MazeGen(10, 20);
    private boolean winMaze = true;

    public Server() throws SocketException, IOException {
        playerOnline = new ArrayList<ClientInfo>();
        protocol = new Protocol();
        executorService = Executors.newCachedThreadPool();
        try {
            serverSocket = new ServerSocket(serverPort);
        } catch (IOException ex) {
            System.out.println("Error: Can't setup server on this port number.");
        }

        playerService = new PlayerService();
    }

    public void run() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(() -> handleClient(clientSocket));
            } catch (IOException ex) {
                System.out.println("Error: Can't setup server on this port number.");
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        int defaultX = 1645;
        int defaultY = 754;
        try {
            reader = new DataInputStream(clientSocket.getInputStream());
        } catch (IOException ex) {
            System.out.println("Error: Can't get input stream.");
        }
        String sentence = "";

        try {
            sentence = reader.readUTF();
        } catch (IOException ex) {
            System.out.println("Error: Can't read from client.");
        }

        if (sentence.startsWith("Login")) {
            handleLogin(clientSocket, sentence);
        } else if (sentence.startsWith("Register")) {
            handleRegister(clientSocket, sentence);
        } else if (sentence.startsWith("Hello")) {
            handleHello(clientSocket, sentence, defaultX, defaultY);
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
            BroadCastMessage(sentence);
        } else if (sentence.startsWith("Shot")) {
            BroadCastMessage(sentence);
        } else if (sentence.startsWith("Remove")) {
            handleRemove(sentence);
        } else if (sentence.startsWith("Exit")) {
            handleExit(sentence);
        } else if (sentence.startsWith("Exit Auth")) {
            handleExitAuth(clientSocket);
        } else if (sentence.startsWith("GET_ITEMS")) {
            handleGetItems(clientSocket);
        } else if (sentence.startsWith("BUY_ITEM")) {
            handleBuyItem(clientSocket);
        }
    }

    private void handleLogin(Socket clientSocket, String sentence) {
        String[] parts = sentence.split(",");
        String username = parts[1];
        String password = parts[2];

        String result = playerService.login(username, password);

        String msg = result.substring(result.indexOf('|') + 1, result.length());

        try {
            writer = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException ex) {
            System.out.println("Error: Can't get output stream.");
        }

        for (ClientInfo clientInfo : playerOnline) {
            if (clientInfo != null && clientInfo.getUsername().equals(username)) {
                try {
                    writer.writeUTF(protocol.LoginPacket("Failed", "User already logged in"));
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                return;
            }
        }

        if (result.startsWith("Success")) {
            System.out.println("Login Success");
            try {
                writer.writeUTF(protocol.LoginPacket("Success", msg));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Login Failed");
            try {
                writer.writeUTF(protocol.LoginPacket("Failed", msg));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleRegister(Socket clientSocket, String sentence) {
        String[] parts = sentence.split(",");

        String username = parts[1];
        String password = parts[2];
        String email = parts[3];

        String result = playerService.register(username, password, email);

        int posResult = result.indexOf('|');

        String msg = result.substring(posResult + 1, result.length());

        try {
            writer = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (result.startsWith("Success")) {
            try {
                writer.writeUTF(protocol.registerPacket("Success", msg));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.out.println("Register Success");
        } else {
            try {
                writer.writeUTF(protocol.registerPacket("Failed", msg));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.out.println("Register Failed");
        }
    }

    private void handleHello(Socket clientSocket, String sentence, int defaultX, int defaultY) {
        try {
            writer = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String username = sentence.substring(5, sentence.length());

        sendToClient(protocol.IDPacket(playerOnline.size() + 1, username));

        BroadCastMessage(protocol.NewClientPacket(username, defaultX, defaultY, -1, playerOnline.size() + 1, "lobby"));

        System.out.println(protocol.leaderBoardPacket(playerService.leaderBoard()));
        sendToClient(protocol.leaderBoardPacket(playerService.leaderBoard()));

        sendAllClientsInMap(writer, "lobby");

        playerOnline.add(new ClientInfo(writer, username, defaultX, defaultY, -1, "lobby"));
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

        String finalSentence = sentence;

        playerOnline.stream()
                .filter(player -> !player.getUsername().equals(username))
                .forEach(player -> sendToClient(player.getWriterStream(), finalSentence));
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

        BroadCastMessage(protocol.NewClientPacket(username, x, y, -1, playerOnline.size() + 1, p.getMap()));

        sendAllClientsInMap(p.getWriterStream(), map);

        BroadCastMessage(sentence);
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

        sendToClient(p.getWriterStream(), protocol.mazeMapPacket(mazeGen.toString()));
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

        //Send to all player in lobby that player win maze
        BroadCastMessage(protocol.NewClientPacket(username, 1645, 754, -1, playerOnline.size() + 1, p.getMap()));

        //Update score
        playerService.updatePoint(username, 50);

        //Send leader board to all player
        sendLeaderBoardToAllClient();

        //send to win player all player in lobby
        sendAllClientsInMap(p.getWriterStream(), "lobby");

        //teleport all player in maze to lobby
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
                BroadCastMessage(sentence);
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
        BroadCastMessage(sentence);
        playerOnline.remove(id);
    }

    private void handleExit(String sentence) {
        String username = sentence.substring(4);
        System.out.println("Exit" + username);

        for (ClientInfo player : playerOnline) {
            if (player != null && player.getUsername().equals(username)) {
                playerOnline.remove(player);
                break;
            }
        }
        try {
            BroadCastMessage(sentence);
        } catch (Exception e) {
            System.out.println("Error: Can't close client socket.");
        }
    }

    private void handleExitAuth(Socket clientSocket) {
        try {
            clientSocket.close();
            if (reader != null) reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleGetItems(Socket clientSocket) {
        try {
            writer = new DataOutputStream(clientSocket.getOutputStream());
            List<String> items = fetchItemsFromDatabase();
            for (String item : items) {
                writer.writeUTF(item);
            }
            writer.writeUTF(""); // End of items
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void handleBuyItem(Socket clientSocket) {
        try {
            int userId = reader.readInt();
            int itemId = reader.readInt();
            String result = buyItem(userId, itemId);
            writer = new DataOutputStream(clientSocket.getOutputStream());
            writer.writeUTF(result);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
            // Check user's balance
            PreparedStatement checkBalanceStmt = connection.prepareStatement("SELECT balance FROM users WHERE id = ?");
            checkBalanceStmt.setInt(1, userId);
            ResultSet balanceResult = checkBalanceStmt.executeQuery();
            if (balanceResult.next()) {
                int balance = balanceResult.getInt("balance");

                // Get item price
                PreparedStatement getItemPriceStmt = connection.prepareStatement("SELECT price FROM items WHERE id = ?");
                getItemPriceStmt.setInt(1, itemId);
                ResultSet priceResult = getItemPriceStmt.executeQuery();
                if (priceResult.next()) {
                    int price = priceResult.getInt("price");

                    if (balance >= price) {
                        // Deduct price from user's balance
                        PreparedStatement updateBalanceStmt = connection.prepareStatement("UPDATE users SET balance = balance - ? WHERE id = ?");
                        updateBalanceStmt.setInt(1, price);
                        updateBalanceStmt.setInt(2, userId);
                        updateBalanceStmt.executeUpdate();

                        // Add item to user's inventory
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

                sendToClient(player.getWriterStream(), protocol.teleportPacket(player.getUsername(), map2, 1645, 754));

                BroadCastMessage(protocol.NewClientPacket(player.getUsername(), 1645, 754, -1, playerOnline.size() + 1, player.getMap()));

                sendAllClientsInMap(player.getWriterStream(), map2);
            }
        }
    }

    private void handlePlayerExit(String player) {
        BroadCastMessage("Exit" + player);
    }

    public void stopServer() throws IOException {
        running = false;
    }

    public void BroadCastMessage(String mess) {
        for (ClientInfo clientInfo : playerOnline) {
            if (clientInfo != null && clientInfo.getWriterStream() != null) {
                try {
                    clientInfo.getWriterStream().writeUTF(mess);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void sendToClient(String message) {
        if (message.equals("exit")) System.exit(0);
        else {
            try {
                writer.writeUTF(message);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendLeaderBoardToAllClient() {
        for (ClientInfo clientInfo : playerOnline) {
            if (clientInfo != null && clientInfo.getWriterStream() != null) {
                sendLeaderBoardToClient(clientInfo.getWriterStream());
            }
        }
    }

    public void sendLeaderBoardToClient(DataOutputStream writer) {
        try {
            writer.writeUTF(protocol.leaderBoardPacket(playerService.leaderBoard()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendToClient(DataOutputStream writer, String message) {
        if (message.equals("exit")) System.exit(0);
        else {
            try {
                writer.writeUTF(message);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendAllClientsInMap(DataOutputStream writer, String map) {
        for (int i = 0; i < playerOnline.size(); i++) {
            if (playerOnline.get(i) != null && playerOnline.get(i).getMap().equals(map)) {
                String username = playerOnline.get(i).getUsername();
                int x = playerOnline.get(i).getX();
                int y = playerOnline.get(i).getY();
                int dir = playerOnline.get(i).getDir();
                try {
                    writer.writeUTF(protocol.NewClientPacket(username, x, y, dir, i + 1, map));
                    writer.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public ArrayList<ClientInfo> getPlayerOnline() {
        return playerOnline;
    }

    public void setPlayerOnline(ArrayList<ClientInfo> playerOnline) {
        this.playerOnline = playerOnline;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public DataInputStream getReader() {
        return reader;
    }

    public void setReader(DataInputStream reader) {
        this.reader = reader;
    }

    public DataOutputStream getWriter() {
        return writer;
    }

    public void setWriter(DataOutputStream writer) {
        this.writer = writer;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public PlayerService getPlayerService() {
        return playerService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
