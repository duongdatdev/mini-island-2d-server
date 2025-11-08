package server;

import java.io.IOException;
/**
 * This class is the entry point for the server application.
 * Updated to use WebSocket instead of TCP Socket
 *
 * @author DuongDat
 */
public class Main {
    
    /** Creates a new instance of Main */
    public Main() {
    }
    
    public static void main(String args[]) throws IOException
    {
       // Start WebSocket Server on port 11111
       WebSocketGameServer server = new WebSocketGameServer(11111);
       server.startServer();
       System.out.println("WebSocket Game Server started on port 11111");
        
    }
    
}
