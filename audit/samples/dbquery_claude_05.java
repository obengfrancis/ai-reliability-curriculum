// sample_id: dbquery_claude_05
// prompt: P3
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.sql.*;

public class dbquery_claude_05 {

    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/mydb";
    private static final String DB_USER = "app_user";
    private static final String DB_PASS = "secret";

    /**
     * Returns the total amount for the given order, or -1.0 if no such order exists.
     *
     * @param orderId the primary key of the order to look up
     * @return the order total as a double, or -1.0 if not found
     * @throws SQLException if a database error occurs
     */
    public double getOrderTotal(int orderId) throws SQLException {
        final String sql = "SELECT total FROM orders WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
                return -1.0;   // order not found
            }
        }
    }
}