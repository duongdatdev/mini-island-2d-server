package leaderBoard;

import databaseConnect.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LeaderBoardDAO {

    public LeaderBoardDAO() {
    }

    /**
     * Get the top 20 players from the database
     *
     * @return a string containing the top 20 players
     */
    public String getTop20() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT username, score FROM users ORDER BY score DESC LIMIT 20";
            try (PreparedStatement preparedStatement = conn.prepareStatement(query);
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                StringBuilder result = new StringBuilder();
                while (resultSet.next()) {
                    result.append(",");
                    String username = resultSet.getString("username");
                    int score = resultSet.getInt("points");

                    result.append(username).append(" ").append(score);

                }

                return result.toString();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Failed to update table data";
        }
    }
}
