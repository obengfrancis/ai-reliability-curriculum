// sample_id: dbquery_claude_02
// prompt: P3
// model: Claude
// model_version: claude-sonnet-4.6
// date: 2026-06-2
// compiles: yes
// edits_to_compile: none

import java.sql.*;

public class dbquery_claude_02 {

    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/mydb";
    private static final String DB_USER = "user";
    private static final String DB_PASSWORD = "password";

    public double getOrderTotal(int orderId) throws SQLException {
        String sql = "SELECT total_amount FROM orders WHERE order_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_amount");
                }
                throw new SQLException("No order found with id: " + orderId);
            }
        }
    }
}