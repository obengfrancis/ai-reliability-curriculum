// sample_id: dbquery_claude_03
// prompt: P3
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.sql.*;

public class dbquery_claude_03 {

    private final String url;
    private final String user;
    private final String password;

    public dbquery_claude_03(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public double getOrderTotal(int orderId) throws SQLException {
        String sql = "SELECT total FROM orders WHERE order_id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
                throw new SQLException("No order found with id: " + orderId);
            }
        }
    }
}