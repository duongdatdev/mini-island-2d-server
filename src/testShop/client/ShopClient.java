package testShop.client;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ShopClient {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public ShopClient(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public List<String> fetchItems() throws IOException {
        out.writeUTF("GET_ITEMS");
        List<String> items = new ArrayList<>();
        String item;
        while (!(item = in.readUTF()).isEmpty()) {
            items.add(item);
        }
        return items;
    }

    public String buyItem(int userId, int itemId) throws IOException {
        out.writeUTF("BUY_ITEM");
        out.writeInt(userId);
        out.writeInt(itemId);
        return in.readUTF();
    }

    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }

    public static void main(String[] args) {
        try {
            int userId = 1; // Example user ID
            ShopClient client = new ShopClient("localhost", 12345);
            List<String> items = client.fetchItems();
            client.close();

            // Create the shop panel with items fetched from the server
            ShopPanel shopPanel = new ShopPanel(items, userId);

            // Create a frame to display the panel
            JFrame frame = new JFrame("Game Shop");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(shopPanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}