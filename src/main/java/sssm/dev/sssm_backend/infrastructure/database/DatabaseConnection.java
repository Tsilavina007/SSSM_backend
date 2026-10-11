package sssm.dev.sssm_backend.infrastructure.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/sssm_db";
    // private static final String USER = System.getenv("MY_PSQL_USER");
    // private static final String PASSWORD = System.getenv("MY_PSQL_PASSWD");
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}