package sssm.dev.sssm_backend.a00_infrastructure.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

  private static final String URL =
      getRequiredEnv("DB_URL", "jdbc:postgresql://localhost:5432/sssm_db");
  private static final String USER = getRequiredEnv("DB_USER", "postgres");
  private static final String PASSWORD = getRequiredEnv("DB_PASSWORD", "postgres");

  private static String getRequiredEnv(String name, String defaultValue) {
    String value = System.getenv().getOrDefault(name, defaultValue);

    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing environment variable: " + name);
    }

    return value;
  }

  public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(URL, USER, PASSWORD);
  }
}
