package auth.service;
//
//import server.database.DatabaseConnection;
//import server.database.UserDAO;
//import server.users.User;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//
//public class UserService implements Service {
//    private UserDAO userDAO;
//    public static UserService instance = null;
//
//    public static UserService getInstance() {
//        if (instance == null) {
//            instance = new UserService();
//        }
//        return instance;
//    }
//
//    private UserService() {
//        userDAO = new UserDAO();
//    }
//
//    public List<String> getTransactionHistory(int userId, Timestamp startTime, Timestamp endTime) {
//    List<String> transactions = new ArrayList<>();
//    try {
//        Connection conn = DatabaseConnection.getConnection();
//        String sql = "SELECT transaction_type, amount, timestamp FROM transactions WHERE user_id = ? AND timestamp BETWEEN  ? AND ? ORDER BY timestamp DESC";
//        Statement stmt = conn.createStatement();
//        PreparedStatement pstmt = conn.prepareStatement(sql);
//        pstmt.setInt(1, userId);
//        pstmt.setTimestamp(2, startTime);
//        pstmt.setTimestamp(3, endTime);
//        ResultSet rs = pstmt.executeQuery();
//
//        while (rs.next()) {
//            String transaction = rs.getString("transaction_type") + " " + rs.getInt("amount") + " at " + rs.getTimestamp("timestamp");
//            System.out.println(transaction);
//            transactions.add(transaction);
//        }
//    } catch (Exception e) {
//        e.printStackTrace();
//    }
//    finally {
//        try {
//            DatabaseConnection.getConnection().close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//    return transactions;
//}
//
//    public int getUserId(String username) {
//        return userDAO.getUserId(username);
//    }
//    public String registerUser(String username, String password) {
//        String result = userDAO.registerUser(username, password);
//        return result;
//    }
//
//    public String loginUser(String username, String password) {
//        String result = userDAO.loginUser(username, password);
//        if (result.equals("User logged in")) {
//            int money = userDAO.getUserMoney(username);
//            User user = new User(username, password, money);
//        }
//        return result;
//    }
//
//    public int getUserMoney(String username) {
//        return userDAO.getUserMoney(username);
//    }
//
//    public void updateUserMoney(String username, int money) {
//        userDAO.updateUserMoney(username, money);
//    }
//
//    public void transferMoney(String fromUsername, String toUsername, int amount) {
//        userDAO.transferMoney(fromUsername, toUsername, amount);
//        userDAO.addTransaction(fromUsername, "TRANSFER to " + toUsername, -amount);
//        userDAO.addTransaction(toUsername, "RECEIVE from " + fromUsername, amount);
//    }
//
//    public void addTransaction(String username, String withdraw, int i) {
//        userDAO.addTransaction(username, withdraw, i);
//    }
//}
