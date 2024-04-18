package dao;

import databaseConnect.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerDAOImp implements PlayerDAO {
    private Connection conn;

    public PlayerDAOImp() {
        this.conn = DatabaseConnection.getConnection();
    }

    @Override
    public String registerPlayer(String username, String email, String password) {
        if (playerExists(username)) {
            return "Username already exists";
        }
        if (username.equals("Username")) {
            return "Invalid username";
        } else {
            this.conn = DatabaseConnection.getConnection();

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            String query = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)";
            try (PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setString(1, username);
                statement.setString(2, hashedPassword);
                statement.setString(3, email);
                int rowsAffected = statement.executeUpdate();
                statement.close();
                if (rowsAffected > 0) {
                    return "User registered successfully";
                } else {
                    return "Failed to register user";
                }

            } catch (SQLException e) {
                e.printStackTrace();
                return "Failed to register user";
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String loginPlayer(String username, String password) {
        this.conn = DatabaseConnection.getConnection();
        String query = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String hashedPassword = resultSet.getString("password_hash");
                if (BCrypt.checkpw(password, hashedPassword)) {
                    statement.close();
                    resultSet.close();
                    return "Login successful";
                } else {
                    statement.close();
                    resultSet.close();
                    return "Invalid password";
                }
            } else {
                statement.close();
                resultSet.close();
                return "Invalid username";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Failed to login";
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean playerExists(String username) {
        this.conn = DatabaseConnection.getConnection();
        String query = "SELECT * FROM Users WHERE username = ?";
        try (PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                statement.close();
                resultSet.close();
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return false;
    }
}
