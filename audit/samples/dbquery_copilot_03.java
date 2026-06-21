// sample_id: dbquery_copilot_03
// prompt: P3
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-3
// compiles: yes
// edits_to_compile: none

import java.sql.*;

public class dbquery_copilot_03 {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/your_database";
    private static final String DB_USER = "your_username";
    private static final String DB_PASSWORD = "your_password";
    
    /**
     * Retrieves the total amount for a given order ID from the database.
     * 
     * @param orderId the order ID to look up
     * @return the total amount for the order, or -1 if not found
     * @throws SQLException if a database access error occurs
     */
    public static double getOrderTotal(int orderId) throws SQLException {
        String query = "SELECT total FROM orders WHERE id = ?";
        double total = -1;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, orderId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    total = rs.getDouble("total");
                }
            }
        }
        
        return total;
    }
    
    public static void main(String[] args) {
        try {
            double orderTotal = getOrderTotal(123);
            System.out.println("Order total: $" + orderTotal);
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}