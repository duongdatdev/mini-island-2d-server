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

/**
 * This class is responsible for handling client connections and managing the game state.
 * It listens for incoming connections from clients and creates a new thread to handle each client.
 * It also updates the game state based on the messages received from the clients.
 * It is the main class for the server application.
 */
public class Server extends Thread {

    /**
     * Creates a new instance of Server
     */

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
        try {
            serverSocket = new ServerSocket(serverPort);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        playerService = new PlayerService();
    }

    public void run() {
        while (running) {
            try {

                Socket clientSocket = serverSocket.accept();

                new Thread(() -> handleClient(clientSocket)).start();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        int defaultX = 1645;
        int defaultY = 754;
        try {
            reader = new DataInputStream(clientSocket.getInputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String sentence = "";

        try {
            sentence = reader.readUTF();
            System.out.println(sentence);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (sentence.startsWith("Login")) {

            String[] parts = sentence.split(",");
            String username = parts[1];
            String password = parts[2];

            System.out.println(username + " " + password);

            String result = playerService.login(username, password);

            String msg = result.substring(result.indexOf('|') + 1, result.length());

            try {
                writer = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            for (ClientInfo clientInfo : playerOnline) {
                if (clientInfo != null && clientInfo.getUsername().equals(username)) {
                    try {
                        writer = new DataOutputStream(clientSocket.getOutputStream());
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

        } else if (sentence.startsWith("Register")) {
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

        } else if (sentence.startsWith("Hello")) {

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

        } else if (sentence.startsWith("Update")) {

            String[] parts = sentence.split(",");

            String username = parts[1];
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            int dir = Integer.parseInt(parts[4]);

            //Update location player
            ClientInfo p = null;
            for (ClientInfo player : playerOnline) {
                if (player != null && player.getUsername().equals(username)) {
                    player.setPosX(x);
                    player.setPosY(y);
                    player.setDirection(dir);

                    p = player;
                    break;
                }
                if (player != null && !player.getUsername().equals(username)) {
                    sendToClient(player.getWriterStream(), sentence);
                }
            }

            for (ClientInfo player : playerOnline) {
                if (player != null && !player.getUsername().equals(username) && player.getMap().equals(p.getMap())) {
                    sendToClient(player.getWriterStream(), sentence);
                }
            }
        } else if (sentence.startsWith("TeleportToMap")) {
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

            BroadCastMessage(protocol.NewClientPacket(username,
                    x,
                    y,
                    -1,
                    playerOnline.size() + 1,
                    p.getMap()));

            sendAllClientsInMap(p.getWriterStream(), map);

            BroadCastMessage(sentence);
        } else if (sentence.startsWith("EnterMaze")) {
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
//                mazeGen = new MazeGen(6, 6);
                mazeGen.solve();
                winMaze = false;
            }

            assert p != null;

            sendToClient(p.getWriterStream(), protocol.mazeMapPacket(mazeGen.toString()));
        } else if (sentence.startsWith("WinMaze")) {
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
            BroadCastMessage(protocol.NewClientPacket(username,
                    defaultX,
                    defaultY,
                    -1,
                    playerOnline.size() + 1,
                    p.getMap()));

            //Update score
            playerService.updatePoint(username, 50);

            //send to win player all player in lobby
            sendAllClientsInMap(p.getWriterStream(), "lobby");

            //teleport all player in maze to lobby
            teleportAllPlayerInMapToMap("maze", "lobby");

            winMaze = true;

        } else if (sentence.startsWith("Chat")) {

            BroadCastMessage(sentence);

        } else if (sentence.startsWith("Shot")) {

            BroadCastMessage(sentence);

        } else if (sentence.startsWith("Remove")) {

            int id = Integer.parseInt(sentence.substring(6));

            BroadCastMessage(sentence);

            playerOnline.remove(id);
        } else if (sentence.startsWith("Exit")) {

            String username = sentence.substring(4);

            System.out.println("Exit" + username);

            for (ClientInfo player : playerOnline) {
                if (player != null && player.getUsername().equals(username)) {
                    playerOnline.remove(player);
                    break;
                }
            }
            BroadCastMessage(sentence);


        } else if (sentence.startsWith("Exit Auth")) {
            try {
                clientSocket.close();
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void teleportAllPlayerInMapToMap(String map, String map2) {
        for (ClientInfo player : playerOnline) {
            if (player.getMap().equals(map)) {
                player.setMap(map2);

                BroadCastMessage(protocol.NewClientPacket(player.getUsername(),
                        1645,
                        754,
                        -1,
                        playerOnline.size() + 1,
                        player.getMap()));

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
        if (message.equals("exit"))
            System.exit(0);
        else {
            try {
                writer.writeUTF(message);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendToClient(DataOutputStream writer, String message) {
        if (message.equals("exit"))
            System.exit(0);
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
