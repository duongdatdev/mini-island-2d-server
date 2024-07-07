package testShop.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ShopPanel extends JPanel {
    private JList<String> itemList;
    private DefaultListModel<String> listModel;
    private JButton buyButton;
    private int userId;

    public ShopPanel(List<String> items, int userId) {
        this.userId = userId;
        setLayout(new BorderLayout());

        // Create the list model and populate it with items
        listModel = new DefaultListModel<>();
        for (String item : items) {
            listModel.addElement(item);
        }

        // Create the JList and set its model
        itemList = new JList<>(listModel);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setVisibleRowCount(10);

        // Add the list to a scroll pane
        JScrollPane listScrollPane = new JScrollPane(itemList);

        // Add the scroll pane to the panel
        add(listScrollPane, BorderLayout.CENTER);

        // Add a title label
        JLabel titleLabel = new JLabel("Shop Items");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        // Add the buy button
        buyButton = new JButton("Buy");
        buyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = getSelectedItem();
                if (selectedItem != null) {
                    int itemId = Integer.parseInt(selectedItem.split(":")[0]);
                    buyItem(userId, itemId);
                }
            }
        });
        add(buyButton, BorderLayout.SOUTH);
    }

    public String getSelectedItem() {
        return itemList.getSelectedValue();
    }

    private void buyItem(int userId, int itemId) {
        try {
            ShopClient client = new ShopClient("localhost", 12345);
            String result = client.buyItem(userId, itemId);
            client.close();
            JOptionPane.showMessageDialog(this, result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}