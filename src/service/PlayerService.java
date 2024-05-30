package service;

import dao.PlayerDAOImp;
import leaderBoard.LeaderBoardDAO;

public class PlayerService implements Service{
    private PlayerDAOImp playerDAOImp;
    private LeaderBoardDAO leaderBoardDAO;

    public PlayerService() {
        this.playerDAOImp = new PlayerDAOImp();
        this.leaderBoardDAO = new LeaderBoardDAO();
    }

    /**
     * Registers a player with the given username, email, and password.
     *
     * @param username username of the player
     * @param email    email of the player
     * @param password password of the player
     * @return a message indicating the result of the registration
     */
    public String register(String username, String email, String password) {
        String msg = playerDAOImp.registerPlayer(username, email, password);
        String status;
        if (msg.equals("User registered successfully")) {
            status = "Success|";
        } else {
            status = "Failed|";
        }
        return status + msg;
    }

    /**
     * Logs in a player with the given username and password.
     *
     * @param username username of the player
     * @param password password of the player
     * @return a message indicating the result of the login
     */
    public String login(String username, String password) {
        String msg = playerDAOImp.loginPlayer(username, password);
        String status;
        if (msg.equals("Login successful")) {
            status = "Success|";
        } else {
            status = "Failed|";
        }
        return status + msg;
    }

    /**
     * Gets the top 20 players from the database.
     *
     * @return a string containing the top 20 players
     */
    public String leaderBoard() {
        System.out.println("Getting top 20");
        return leaderBoardDAO.getTop20();
    }

    /**
     * Updates the points of a player in the database.
     *
     * @param username the username of the player
     * @param points   the new points of the player
     * @return a message indicating the result of the update
     */
    public String updatePoint(String username, int points) {
        return playerDAOImp.updatePoint(username, points);
    }
}
