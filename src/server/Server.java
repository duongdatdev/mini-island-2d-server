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
import java.util.Objects;
import java.util.concurrent.ExecutorService;

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

    //maze
    private MazeGen mazeGen = new MazeGen(10,20);
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
        try {
            reader = new DataInputStream(clientSocket.getInputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String sentence = "";

        try {
            sentence = reader.readUTF();
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

            int x = 1000;
            int y = 1000;

            BroadCastMessage(protocol.NewClientPacket(username, x, y, -1, playerOnline.size() + 1,"lobby"));

            System.out.println(protocol.leaderBoardPacket(playerService.leaderBoard()));
            sendToClient(protocol.leaderBoardPacket(playerService.leaderBoard()));

            sendAllClientsInMap(writer, "lobby");

            playerOnline.add(new ClientInfo(writer, username, x, y, -1,"lobby"));

        } else if (sentence.startsWith("Update")) {

            String[] parts = sentence.split(",");

            String username = parts[1];
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            int dir = Integer.parseInt(parts[4]);

            //Update location player
            for (ClientInfo player : playerOnline) {
                if (player != null && player.getUsername().equals(username)) {
                    player.setPosX(x);
                    player.setPosY(y);
                    player.setDirection(dir);
                    break;
                }
            }
            BroadCastMessage(sentence);
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

//            if(map.equals("Maze")){
//                MazeGen mazeGen = new MazeGen(10,20);
//                mazeGen.solve();
////                mazeGen.draw();
//
//                sendToClient(protocol.mazeMapPacket(mazeGen.toString()));
//            }

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

            for (ClientInfo player : playerOnline) {
                if (player != null && player.getUsername().equals(username)) {
                    player.setMap("Loading");
                    break;
                }
            }
            if(winMaze){
                mazeGen = new MazeGen(10,20);
                mazeGen.solve();
                winMaze = false;
            }

            sendToClient(protocol.mazeMapPacket(mazeGen.toString()));

            BroadCastMessage(sentence);

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

            BroadCastMessage(sentence);

            for (ClientInfo player : playerOnline) {
                if (player != null && player.getUsername().equals(username)) {
                    playerOnline.remove(player);
                    break;
                }
            }

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

    public void sendAllClientsInMap(DataOutputStream writer, String map) {
        for (int i = 0; i < playerOnline.size(); i++) {
            if (playerOnline.get(i) != null && playerOnline.get(i).getMap().equals(map)) {
                String username = playerOnline.get(i).getUsername();
                int x = playerOnline.get(i).getX();
                int y = playerOnline.get(i).getY();
                int dir = playerOnline.get(i).getDir();
                try {
                    writer.writeUTF(protocol.NewClientPacket(username, x, y,                                                                                                                                                                                                                                                                    dir, i + 1, map));
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
