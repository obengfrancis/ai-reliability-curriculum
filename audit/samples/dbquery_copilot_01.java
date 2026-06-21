// sample_id: dbequery_copilot_01
// prompt: P3
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class dbquery_copilot_01 {
    /**
     * Connects to a relational database and retrieves the total amount
     * for the given order id from the orders table.
     * 
     * @param orderId the order ID to look up
     * @return the total amount for the order as a double
     * @throws SQLException if a database access error occurs
     */
    public static double getOrderTotal(int orderId) throws SQLException {
        // Database connection parameters
        String url = "jdbc:mysql://localhost:3306/orders_db";
        String username = "db_user";
        String password = "db_password";
        
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        double total = 0.0;
        
        try {
            // Establish database connection
            connection = DriverManager.getConnection(url, username, password);
            
            // Prepare SQL query to retrieve order total
            String query = "SELECT total FROM orders WHERE order_id = ?";
            statement = connection.prepareStatement(query);
            statement.setInt(1, orderId);
            
            // Execute query
            resultSet = statement.executeQuery();
            
            // Extract total from result set
            if (resultSet.next()) {
                total = resultSet.getDouble("total");
            } else {
                throw new SQLException("Order not found with ID: " + orderId);
            }
            
            return total;
            
        } finally {
            // Close resources
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }
    
    /**
     * Example usage of getOrderTotal method
     */
    public static void main(String[] args) {
        try {
            double orderTotal = getOrderTotal(1001);
            System.out.println("Order total: $" + orderTotal);
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
