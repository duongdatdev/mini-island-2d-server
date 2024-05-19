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

    public String leaderBoard() {
        System.out.println("Getting top 20");
        return leaderBoardDAO.getTop20();
    }
}
