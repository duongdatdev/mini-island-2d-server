package testShop.server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShopServer {
    private static final int PORT = 12345;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/game_shop";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Shop server is running...");

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                     DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

                    String request = in.readUTF();
                    if ("GET_ITEMS".equals(request)) {
                        List<String> items = fetchItemsFromDatabase();
                        for (String item : items) {
                            out.writeUTF(item);
                        }
                        out.writeUTF(""); // End of items
                    } else if (request.startsWith("BUY_ITEM")) {
                        int userId = in.readInt();
                        int itemId = in.readInt();
                        String result = buyItem(userId, itemId);
                        out.writeUTF(result);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> fetchItemsFromDatabase() {
        List<String> items = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
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

    private static String buyItem(int userId, int itemId) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
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
}