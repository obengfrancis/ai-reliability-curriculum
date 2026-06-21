// sample_id: dbquery_claude_01
// prompt: P3
// model: Claude
// model_version: claude-sonnet-4-6
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * P3 — Database access
 *
 * Connects to a relational database, queries the orders table for the
 * total amount of the given order, and returns it as a double.
 */
public class dbquery_claude_01 {

    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/mydb";
    private static final String DB_USER = "dbuser";
    private static final String DB_PASS = "dbpass";

    private static final String QUERY =
            "SELECT total FROM orders WHERE order_id = ?";

    /**
     * Looks up the total amount for the given order in the orders table.
     *
     * @param orderId the ID of the order to look up
     * @return the total amount for the order as a double
     * @throws SQLException             if a database access error occurs
     * @throws IllegalArgumentException if orderId is not positive
     * @throws RuntimeException         if no order is found for the given id
     */
    public static double getOrderTotal(int orderId) throws SQLException {

        if (orderId <= 0) {
            throw new IllegalArgumentException(
                    "orderId must be a positive integer, got: " + orderId);
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(QUERY)) {

            stmt.setInt(1, orderId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException(
                            "No order found for orderId=" + orderId);
                }
                return rs.getDouble("total");
            }
        }
    }

    // ── Quick smoke test ──────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        int orderId = 1001;
        System.out.println("Fetching total for orderId: " + orderId);
        double total = getOrderTotal(orderId);
        System.out.printf("Order total: %.2f%n", total);
    }
}