package service;

import dao.PlayerDAOImp;

public class PlayerService implements Service{
    private PlayerDAOImp playerDAOImp;
    public PlayerService() {
        this.playerDAOImp = new PlayerDAOImp();
    }
    public String registerPlayer(String name, String email, String password) {
        String mgs = playerDAOImp.registerPlayer(name, email, password);
        if (mgs.equals("User registered successfully")) {
            return "success";
        } else {
            return "failed";
        }
    }
    public String loginPlayer(String username, String password) {
        String mgs = playerDAOImp.loginPlayer(username, password);
        if (mgs.equals("Login successful")) {
            return "success";
        } else {
            return "failed";
        }
    }
}
