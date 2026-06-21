// sample_id: dbquery_copilot_05
// prompt: P3
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-3
// compiles: yes
// edits_to_compile: none

import java.sql.*;

public class dbquery_copilot_05 {
    
    /**
     * Retrieves the total amount for a given order ID from the database.
     * 
     * @param orderId the ID of the order to look up
     * @return the total amount for the order as a double
     * @throws SQLException if a database error occurs
     */
    public double getOrderTotal(int orderId) throws SQLException {
        String query = "SELECT total FROM orders WHERE id = ?";
        double total = 0.0;
        
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            statement.setInt(1, orderId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    total = resultSet.getDouble("total");
                } else {
                    throw new SQLException("Order not found with ID: " + orderId);
                }
            }
        }
        
        return total;
    }
    
    /**
     * Gets a database connection.
     * In a real application, this would typically use a connection pool.
     * 
     * @return a Connection object
     * @throws SQLException if connection fails
     */
    private Connection getConnection() throws SQLException {
        // Example using DriverManager (for production, use a connection pool like HikariCP)
        String url = "jdbc:mysql://localhost:3306/your_database";
        String user = "your_username";
        String password = "your_password";
        
        return DriverManager.getConnection(url, user, password);
    }
}