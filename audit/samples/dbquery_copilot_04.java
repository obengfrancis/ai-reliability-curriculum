// sample_id: dbquery_copilot_04
// prompt: P3
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-3
// compiles: yes
// edits_to_compile: none

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

public class dbquery_copilot_04 {
    private DataSource dataSource;

    public dbquery_copilot_04(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Retrieves the total amount for the given order id from the orders table.
     *
     * @param orderId the order id to look up
     * @return the total amount for the order
     * @throws SQLException if a database error occurs
     * @throws IllegalArgumentException if the order is not found
     */
    public double getOrderTotal(int orderId) throws SQLException {
        String query = "SELECT total FROM orders WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            statement.setInt(1, orderId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("total");
                } else {
                    throw new IllegalArgumentException("Order not found with id: " + orderId);
                }
            }
        }
    }
}