package dao;

public interface PlayerDAO {
    /**
     * Registers a player with the given username, email, and password.
     *
     * @param username username of the player
     * @param email    email of the player
     * @param password password of the player
     * @return a message indicating the result of the registration
     */
    public String registerPlayer(String username, String email, String password);

    /**
     * Logs in a player with the given username and password.
     *
     * @param username username of the player
     * @param password password of the player
     * @return a message indicating the result of the login
     */
    public String loginPlayer(String username, String password);

    /**
     * Checks if a player with the given username exists.
     *
     * @param username username of the player
     * @return true if the player exists, false otherwise
     */
    public boolean playerExists(String username);
}
