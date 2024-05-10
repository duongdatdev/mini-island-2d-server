package server;

import service.PlayerService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
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
            System.out.println(sentence);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (sentence.startsWith("Login")) {

            String[] parts = sentence.split(",");
            String username = parts[1];
            String password = parts[2];

            System.out.println(username + " " + password);

//            String result = playerService.login(username, password);
            try {
                writer = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (username.equals("admin") && password.equals("admin")) {

                System.out.println("Login Success");

                sendToClient(protocol.LoginPacket("Success"));
            } else {

                System.out.println("Login Failed");

            }

        } else if (sentence.startsWith("Hello")) {

            String username = sentence.substring(5,sentence.length());

            System.out.println("size = " + playerOnline.size());

            sendToClient(protocol.IDPacket(playerOnline.size() + 1, username));

            int x = 1000;
            int y = 1000;

            try {
                BroadCastMessage(protocol.NewClientPacket(username, x, y, -1, playerOnline.size() + 1));
                sendAllClients(writer);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            playerOnline.add(new ClientInfo(writer, username, x, y, -1));
        } else if (sentence.startsWith("Register")) {
            int pos1 = sentence.indexOf(',');
            int pos2 = sentence.indexOf('-');
            int pos3 = sentence.indexOf('|');

            String username = sentence.substring(8, pos1);
            String password = sentence.substring(pos1 + 1, pos2);
            String email = sentence.substring(pos2 + 1, pos3);

            String result = playerService.register(username, password, email);

            int posResult = result.indexOf('|');

            String status = result.substring(0, posResult);
            String msg = result.substring(posResult + 1, result.length());

            if (status.equals("Success")) {
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

        } else if (sentence.startsWith("Update")) {

            String[] parts = sentence.split(",");

            String username = parts[1];
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            int dir = Integer.parseInt(parts[4]);
            int id = Integer.parseInt(parts[5]);

            if (id > 0 && id <= playerOnline.size() && playerOnline.get(id - 1) != null) {

                playerOnline.get(id - 1).setPosX(x);
                playerOnline.get(id - 1).setPosY(y);
                playerOnline.get(id - 1).setDirection(dir);

                try {
                    BroadCastMessage(sentence);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            }
        } else if (sentence.startsWith("Chat")) {

            try {
                BroadCastMessage(sentence);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        } else if (sentence.startsWith("Shot")) {

            try {
                BroadCastMessage(sentence);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        } else if (sentence.startsWith("Remove")) {

            int id = Integer.parseInt(sentence.substring(6));

            try {
                BroadCastMessage(sentence);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            playerOnline.set(id - 1, null);

        } else if (sentence.startsWith("Exit")) {

            int id = Integer.parseInt(sentence.substring(4));

            try {
                BroadCastMessage(sentence);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            try {
                if (playerOnline.get(id - 1) != null && id > 0 && id <= playerOnline.size())
                    playerOnline.set(id - 1, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else if (sentence.startsWith("Exit Auth")) {
        }
    }

    public void stopServer() throws IOException {
        running = false;
    }

    public void BroadCastMessage(String mess) throws IOException {
        for (int i = 0; i < playerOnline.size(); i++) {
            if (playerOnline.get(i) != null) {
                playerOnline.get(i).getWriterStream().writeUTF(mess);
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

    public void sendAllClients(DataOutputStream writer) {
        String username;
        int x, y, dir;
        for (int i = 0; i < playerOnline.size(); i++) {
            if (playerOnline.get(i) != null) {
                username = playerOnline.get(i).getUsername();
                x = playerOnline.get(i).getX();
                y = playerOnline.get(i).getY();
                dir = playerOnline.get(i).getDir();
                try {
                    writer.writeUTF(protocol.NewClientPacket(username, x, y, dir, i + 1));
                    writer.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


}
