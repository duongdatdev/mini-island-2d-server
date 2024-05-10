package service;

import dao.PlayerDAOImp;

public class PlayerService implements Service{
    private PlayerDAOImp playerDAOImp;
    public PlayerService() {
        this.playerDAOImp = new PlayerDAOImp();
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
}
