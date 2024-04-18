package dao;

public interface PlayerDAO {
    public String registerPlayer(String username, String email, String password);
    public String loginPlayer(String username, String password);
    public boolean playerExists(String username);
}
