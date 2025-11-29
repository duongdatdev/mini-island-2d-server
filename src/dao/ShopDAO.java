package dao;

import databaseConnect.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO để quản lý Shop Skins
 */
public class ShopDAO {
    
    public ShopDAO() {
        initializeTables();
    }
    
    /**
     * Khởi tạo bảng skins
     */
    public void initializeTables() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;
        
        try {
            // Tạo bảng skins
            String createSkinsTable = 
                "CREATE TABLE IF NOT EXISTS skins (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(100) NOT NULL," +
                "  description VARCHAR(255)," +
                "  price INT NOT NULL DEFAULT 0," +
                "  skin_folder VARCHAR(50) NOT NULL," +
                "  is_default BOOLEAN DEFAULT FALSE," +
                "  is_active BOOLEAN DEFAULT TRUE" +
                ")";
            PreparedStatement stmt = conn.prepareStatement(createSkinsTable);
            stmt.executeUpdate();
            stmt.close();
            
            // Tạo bảng player_skins
            String createPlayerSkinsTable = 
                "CREATE TABLE IF NOT EXISTS player_skins (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  username VARCHAR(50) NOT NULL," +
                "  skin_id INT NOT NULL," +
                "  is_equipped BOOLEAN DEFAULT FALSE," +
                "  purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  UNIQUE KEY unique_player_skin (username, skin_id)" +
                ")";
            stmt = conn.prepareStatement(createPlayerSkinsTable);
            stmt.executeUpdate();
            stmt.close();
            
            // Thêm cột coins vào users nếu chưa có
            try {
                String addCoinsColumn = "ALTER TABLE users ADD COLUMN coins INT DEFAULT 100";
                stmt = conn.prepareStatement(addCoinsColumn);
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                // Column exists
            }
            
            insertDefaultSkins(conn);
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    
    private void insertDefaultSkins(Connection conn) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM skins";
        PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
        ResultSet rs = checkStmt.executeQuery();
        rs.next();
        if (rs.getInt(1) > 0) {
            rs.close();
            checkStmt.close();
            return;
        }
        rs.close();
        checkStmt.close();
        
        String insertQuery = "INSERT INTO skins (name, description, price, skin_folder, is_default) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(insertQuery);
        
        // Skins có sẵn
        addSkin(stmt, "Default Hero", "The classic adventurer", 0, "1", true);
        addSkin(stmt, "Blue Warrior", "A brave warrior in blue", 100, "2", false);
        
        // Skins coming soon
        addSkin(stmt, "Red Knight", "A fierce red knight", 200, "3", false);
        addSkin(stmt, "Gold Champion", "The legendary champion", 500, "4", false);
        addSkin(stmt, "Shadow Ninja", "Master of shadows", 300, "5", false);
        addSkin(stmt, "Ice Mage", "Wielder of frost magic", 400, "6", false);
        
        stmt.close();
        System.out.println("Default skins inserted!");
    }
    
    private void addSkin(PreparedStatement stmt, String name, String desc, int price, String folder, boolean isDefault) throws SQLException {
        stmt.setString(1, name);
        stmt.setString(2, desc);
        stmt.setInt(3, price);
        stmt.setString(4, folder);
        stmt.setBoolean(5, isDefault);
        stmt.executeUpdate();
    }
    
    /**
     * Lấy tất cả skins
     */
    public List<SkinItem> getAllSkins() {
        List<SkinItem> skins = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return skins;
        
        try {
            String query = "SELECT * FROM skins WHERE is_active = TRUE ORDER BY price";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                SkinItem skin = new SkinItem();
                skin.id = rs.getInt("id");
                skin.name = rs.getString("name");
                skin.description = rs.getString("description");
                skin.price = rs.getInt("price");
                skin.skinFolder = rs.getString("skin_folder");
                skin.isDefault = rs.getBoolean("is_default");
                skins.add(skin);
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        
        return skins;
    }
    
    /**
     * Lấy coins của người chơi
     */
    public int getPlayerCoins(String username) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return 0;
        
        try {
            String query = "SELECT coins FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int coins = rs.getInt("coins");
                rs.close();
                stmt.close();
                return coins;
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        
        return 0;
    }
    
    /**
     * Mua skin
     */
    public String buySkin(String username, int skinId) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return "Error|Database error";
        
        try {
            // Lấy thông tin skin
            String skinQuery = "SELECT * FROM skins WHERE id = ? AND is_active = TRUE";
            PreparedStatement skinStmt = conn.prepareStatement(skinQuery);
            skinStmt.setInt(1, skinId);
            ResultSet skinRs = skinStmt.executeQuery();
            
            if (!skinRs.next()) {
                skinRs.close();
                skinStmt.close();
                return "Error|Skin not found";
            }
            
            int price = skinRs.getInt("price");
            String skinName = skinRs.getString("name");
            String skinFolder = skinRs.getString("skin_folder");
            skinRs.close();
            skinStmt.close();
            
            // Check skin có sẵn không (folder 1, 2)
            if (!skinFolder.equals("1") && !skinFolder.equals("2")) {
                return "Error|Coming soon!";
            }
            
            // Check đã có chưa
            String ownQuery = "SELECT * FROM player_skins WHERE username = ? AND skin_id = ?";
            PreparedStatement ownStmt = conn.prepareStatement(ownQuery);
            ownStmt.setString(1, username);
            ownStmt.setInt(2, skinId);
            ResultSet ownRs = ownStmt.executeQuery();
            
            if (ownRs.next()) {
                ownRs.close();
                ownStmt.close();
                return "Error|Already owned";
            }
            ownRs.close();
            ownStmt.close();
            
            // Check coins
            int coins = getPlayerCoins(username);
            if (coins < price) {
                return "Error|Not enough coins";
            }
            
            // Trừ coins
            String deductQuery = "UPDATE users SET coins = coins - ? WHERE username = ?";
            PreparedStatement deductStmt = conn.prepareStatement(deductQuery);
            deductStmt.setInt(1, price);
            deductStmt.setString(2, username);
            deductStmt.executeUpdate();
            deductStmt.close();
            
            // Thêm skin
            String insertQuery = "INSERT INTO player_skins (username, skin_id) VALUES (?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setInt(2, skinId);
            insertStmt.executeUpdate();
            insertStmt.close();
            
            return "Success|" + skinName;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error|Purchase failed";
        } finally {
            try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    
    /**
     * Lấy skins của người chơi
     */
    public List<PlayerSkin> getPlayerSkins(String username) {
        List<PlayerSkin> skins = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return skins;
        
        try {
            String query = "SELECT ps.*, s.name, s.description, s.skin_folder " +
                "FROM player_skins ps JOIN skins s ON ps.skin_id = s.id " +
                "WHERE ps.username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PlayerSkin skin = new PlayerSkin();
                skin.id = rs.getInt("skin_id");
                skin.name = rs.getString("name");
                skin.description = rs.getString("description");
                skin.skinFolder = rs.getString("skin_folder");
                skin.isEquipped = rs.getBoolean("is_equipped");
                skins.add(skin);
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        
        return skins;
    }
    
    /**
     * Equip skin - trả về skin folder
     */
    public String equipSkin(String username, int skinId) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return "Error|Database error";
        
        try {
            // Check có skin không
            String checkQuery = "SELECT s.skin_folder FROM player_skins ps " +
                "JOIN skins s ON ps.skin_id = s.id " +
                "WHERE ps.username = ? AND ps.skin_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            checkStmt.setInt(2, skinId);
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (!checkRs.next()) {
                checkRs.close();
                checkStmt.close();
                return "Error|Don't own this skin";
            }
            String skinFolder = checkRs.getString("skin_folder");
            checkRs.close();
            checkStmt.close();
            
            // Unequip all
            String unequipQuery = "UPDATE player_skins SET is_equipped = FALSE WHERE username = ?";
            PreparedStatement unequipStmt = conn.prepareStatement(unequipQuery);
            unequipStmt.setString(1, username);
            unequipStmt.executeUpdate();
            unequipStmt.close();
            
            // Equip này
            String equipQuery = "UPDATE player_skins SET is_equipped = TRUE WHERE username = ? AND skin_id = ?";
            PreparedStatement equipStmt = conn.prepareStatement(equipQuery);
            equipStmt.setString(1, username);
            equipStmt.setInt(2, skinId);
            equipStmt.executeUpdate();
            equipStmt.close();
            
            return "Success|" + skinFolder;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error|Equip failed";
        } finally {
            try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    
    /**
     * Lấy skin đang equip
     */
    public String getEquippedSkin(String username) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return "1";
        
        try {
            String query = "SELECT s.skin_folder FROM player_skins ps " +
                "JOIN skins s ON ps.skin_id = s.id " +
                "WHERE ps.username = ? AND ps.is_equipped = TRUE";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String folder = rs.getString("skin_folder");
                rs.close();
                stmt.close();
                return folder;
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        
        return "1";
    }
    
    /**
     * Tặng default skin cho user mới
     */
    public void giveDefaultSkin(String username) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;
        
        try {
            String query = "SELECT id FROM skins WHERE is_default = TRUE LIMIT 1";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int skinId = rs.getInt("id");
                rs.close();
                stmt.close();
                
                String insert = "INSERT IGNORE INTO player_skins (username, skin_id, is_equipped) VALUES (?, ?, TRUE)";
                PreparedStatement insertStmt = conn.prepareStatement(insert);
                insertStmt.setString(1, username);
                insertStmt.setInt(2, skinId);
                insertStmt.executeUpdate();
                insertStmt.close();
            } else {
                rs.close();
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    
    // ============ Inner Classes ============
    
    public static class SkinItem {
        public int id;
        public String name;
        public String description;
        public int price;
        public String skinFolder;
        public boolean isDefault;
        
        public String toProtocolString() {
            return id + "|" + name + "|" + description + "|" + price + "|" + skinFolder + "|" + (isDefault ? "1" : "0");
        }
    }
    
    public static class PlayerSkin {
        public int id;
        public String name;
        public String description;
        public String skinFolder;
        public boolean isEquipped;
        
        public String toProtocolString() {
            return id + "|" + name + "|" + description + "|" + skinFolder + "|" + (isEquipped ? "1" : "0");
        }
    }
}
