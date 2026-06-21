// sample_id: dbquery_claude_04
// prompt: P3
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

public class dbquery_claude_04 {

    private final DataSource dataSource;

    public dbquery_claude_04(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Looks up the total amount for the given order ID in the orders table.
     *
     * @param orderId the ID of the order to look up
     * @return the total amount as a double
     * @throws SQLException             if a database access error occurs
     * @throws IllegalArgumentException if no order with the given ID exists
     */
    public double getOrderTotal(int orderId) throws SQLException {
        final String sql = "SELECT total FROM orders WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "No order found with id: " + orderId);
                }
                return rs.getDouble("total");
            }
        }
    }
}