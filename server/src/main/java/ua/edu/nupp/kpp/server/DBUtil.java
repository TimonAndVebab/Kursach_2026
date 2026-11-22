package ua.edu.nupp.kpp.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBUtil {
    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/kpp25_approval_demo");
        String user = System.getenv("DB_USER");
        if (user == null || user.isBlank()) {
            throw new SQLException("Задайте ім’я користувача MySQL у DB_USER.");
        }
        // Пароль тягнемо з оточення
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");

        return DriverManager.getConnection(url, user, password);
    }
}
