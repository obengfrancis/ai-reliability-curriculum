// sample_id: dbquery_chatgpt_04
// prompt: P3
// model: ChatGPT
// model_version: GPT-5.5 Thinking
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class dbquery_chatgpt_04 {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/store";
    private static final String DB_USER = "username";
    private static final String DB_PASSWORD = "password";

    public static double getOrderTotal(int orderId) throws SQLException {
        String sql = "SELECT total_amount FROM orders WHERE order_id = ?";

        try (
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, orderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("total_amount");
                } else {
                    throw new SQLException("No order found with id: " + orderId);
                }
            }
        }
    }
}