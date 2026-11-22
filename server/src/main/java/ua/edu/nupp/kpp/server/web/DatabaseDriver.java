package ua.edu.nupp.kpp.server.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseDriver implements ServletContextListener {
    public void contextInitialized(ServletContextEvent event) {
        // Драйвер лежить у нашому WAR, тому завантажуємо його класом цього застосунку.
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new IllegalStateException("У WAR бракує MySQL Connector/J", e); }
    }

    public void contextDestroyed(ServletContextEvent event) {
        // Tomcat може перезапустити WAR в тій самій JVM. Завершуємо потік нашого драйвера.
        com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        DriverManager.drivers().filter(d -> d.getClass().getClassLoader() == getClass().getClassLoader())
                .forEach(d -> {
                    try { DriverManager.deregisterDriver(d); }
                    catch (SQLException e) { event.getServletContext().log("Не вдалося вивантажити JDBC-драйвер", e); }
                });
    }
}
