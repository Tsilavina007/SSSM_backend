package sssm.dev.sssm_backend.a00_infrastructure.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

/**
 * Point d'accès JDBC unique de l'application.
 *
 * <p>Les DAO existants conservent leur appel à {@link #getConnection()}, mais la connexion provient
 * désormais du {@link DataSource} Spring (HikariCP) et non plus de {@code DriverManager}. La
 * connexion renvoyée participe à la transaction Spring courante et sa fermeture passe par {@link
 * DataSourceUtils#releaseConnection(Connection, DataSource)} : un {@code @Transactional} de service
 * couvre donc réellement les écritures des DAO.
 */
@Component
public class DatabaseConnection {

  private static volatile DataSource dataSource;

  public DatabaseConnection(DataSource dataSource) {
    DatabaseConnection.dataSource = dataSource;
  }

  /**
   * Retourne une connexion transactionnelle. La fermeture de la connexion par un bloc
   * try-with-resources libère la connexion auprès de Spring sans fermer la connexion physique
   * lorsqu'une transaction est active.
   */
  public static Connection getConnection() throws SQLException {
    DataSource currentDataSource = dataSource;
    if (currentDataSource == null) {
      throw new SQLException("DataSource Spring non initialisé");
    }

    Connection target = DataSourceUtils.getConnection(currentDataSource);
    boolean[] released = {false};
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
              if ("close".equals(method.getName()) && method.getParameterCount() == 0) {
                if (!released[0]) {
                  released[0] = true;
                  DataSourceUtils.releaseConnection(target, currentDataSource);
                }
                return null;
              }
              try {
                return method.invoke(target, args);
              } catch (InvocationTargetException exception) {
                throw exception.getCause();
              }
            });
  }
}
