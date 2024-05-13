package server;

public class Protocol {

    private String message = "";

    /**
     * Creates a new instance of Protocol
     */
    public Protocol() {
    }

    public String IDPacket(int id) {
        message = "ID" + id;
        return message;
    }

    public String IDPacket(int id, String username) {
        message = "ID" + id + "," + username;
        return message;
    }

    public String NewClientPacket(int x, int y, int dir, int id) {
        message = "NewClient" + x + "," + y + "-" + dir + "|" + id;
        return message;
    }

    public String NewClientPacket(String username, int x, int y, int dir, int id) {
        message = "NewClient" + username + "," + x + "-" + y + "|" + dir + "!" + id;
        return message;
    }

    public String LoginPacket(String status,String msg) {
        message = "Login," + status + "," + msg;
        return message;
    }

    public String registerPacket(String status, String msg) {
        message = "Register," + status + "," + msg;
        return message;
    }
}
