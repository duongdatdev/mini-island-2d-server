package server;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * This class provides a GUI for the WebSocket game server.
 * Updated to use WebSocketGameServer instead of old TCP Server.
 *
 * @author DuongDat
 */
public class ServerGUI extends JFrame implements ActionListener {

    private JButton startServerButton;
    private JButton stopServerButton;
    private JLabel statusLabel;

    private WebSocketGameServer server;

    /**
     * Creates a new instance of ServerGUI
     */
    public ServerGUI() {
        setTitle("WebSocket Game Server GUI");
        setBounds(350, 200, 300, 200);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(null);
        startServerButton = new JButton("Start Server");
        startServerButton.setBounds(20, 30, 120, 25);
        startServerButton.addActionListener(this);

        stopServerButton = new JButton("Stop Server");
        stopServerButton.setBounds(150, 30, 120, 25);
        stopServerButton.addActionListener(this);

        statusLabel = new JLabel();
        statusLabel.setBounds(80, 90, 200, 25);

        getContentPane().add(statusLabel);
        getContentPane().add(startServerButton);
        getContentPane().add(stopServerButton);
        
        // Initialize WebSocket server on port 11111
        server = new WebSocketGameServer(11111);

        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startServerButton) {
            // Start server in a new thread to avoid blocking the GUI
            new Thread(() -> {
                server.startServer();
            }).start();
            startServerButton.setEnabled(false);
            statusLabel.setText("WebSocket Server is running on port 11111");
        }

        if (e.getSource() == stopServerButton) {
            try {
                server.stopServer();
                statusLabel.setText("Server is stopping.....");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

}
