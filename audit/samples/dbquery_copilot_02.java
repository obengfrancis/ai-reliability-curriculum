// sample_id: dbquery_copilot_02
// prompt: P3
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-2
// compiles: yes
// edits_to_compile: none

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class dbquery_copilot_02 {
    
    // Database connection parameters
    private static final String DB_URL = "jdbc:mysql://localhost:3306/orders_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";
    
    /**
     * Retrieves the total amount for a given order ID from the database.
     * 
     * @param orderId the ID of the order to look up
     * @return the total amount for the order as a double
     * @throws SQLException if a database error occurs
     * @throws IllegalArgumentException if orderId is invalid
     */
    public static double getOrderTotal(int orderId) throws SQLException {
        if (orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be a positive integer");
        }
        
        String query = "SELECT total FROM orders WHERE id = ?";
        
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            
            // Set the order ID parameter
            preparedStatement.setInt(1, orderId);
            
            // Execute the query
            ResultSet resultSet = preparedStatement.executeQuery();
            
            // Check if a result was found
            if (resultSet.next()) {
                return resultSet.getDouble("total");
            } else {
                throw new SQLException("Order with ID " + orderId + " not found");
            }
        }
    }
}