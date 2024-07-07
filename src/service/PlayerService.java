package service;

import dao.PlayerDAOImp;
import dao.LeaderBoardDAO;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerService implements Service {
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
    public String register(String username,String password, String email) {
        if (!isValidUsername(username)) {
            return "Failed|Invalid username!";
        }
        if (!isValidEmail(email)) {
            return "Failed|Invalid email!";
        }
        String passwordValidation = validatePassword(password);
        if (passwordValidation != null) {
            return "Failed|" + passwordValidation;
        }
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

    private boolean isValidEmail(String email) {
        Pattern VALID_EMAIL_ADDRESS_REGEX =
                Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(email);
        return matcher.matches();
    }

    private boolean isValidUsername(String username) {
        String usernameRegex = "^[a-zA-Z0-9]*$";
        Pattern pattern = Pattern.compile(usernameRegex);
        Matcher matcher = pattern.matcher(username);
        return matcher.matches();
    }

    private String validatePassword(String password) {
        if (password.length() < 6) return "Password has to be at least 6 characters long!";
        if (!password.matches(".*[A-Z].*")) return "Password has to have at least one uppercase letter!";
        if (!password.matches(".*[a-z].*")) return "Password has to have at least one lowercase letter!";
        if (!password.matches(".*[0-9].*")) return "Password has to have at least one digit!";
        if (!password.matches(".*[!@#$%^&*].*")) return "Password has to have at least one special character!";
        return null;
    }
}
