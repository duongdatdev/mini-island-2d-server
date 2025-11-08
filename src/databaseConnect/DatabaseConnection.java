package databaseConnect;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.dbcp2.BasicDataSource;

public class DatabaseConnection {

    private static BasicDataSource dataSource;

    static {
        dataSource = new BasicDataSource();
        //change your database here or use my database
        dataSource.setUrl("jdbc:mysql://localhost:3306/miniisland");
        dataSource.setUsername("root");
        dataSource.setPassword("secret");
        dataSource.setInitialSize(5); // Initial number of connections
        dataSource.setMaxTotal(20);   // Maximum number of connections
    }

    public static Connection getConnection(){
        Connection conn;
        try {
            conn = dataSource.getConnection();
            return conn;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static void CheckConnect() {
        Connection connection = getConnection();
        if (connection != null) {
            System.out.println("Connected to the database!");
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Failed to connect to the database.");
        }
    }
}
