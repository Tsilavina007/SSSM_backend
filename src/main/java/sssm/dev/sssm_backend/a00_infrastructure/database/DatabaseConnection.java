package sssm.dev.sssm_backend.a00_infrastructure.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Point d'accès JDBC unique de l'application.
 *
 * <p>Les DAO existants conservent leur appel à {@link #getConnection()}, mais la connexion provient
 * désormais du {@link DataSource} Spring (HikariCP) et non plus de {@code DriverManager}. La
 * connexion renvoyée participe à la transaction Spring courante et sa fermeture passe par {@link
 * DataSourceUtils#releaseConnection(Connection, DataSource)} : un {@code @Transactional} de service
 * couvre donc réellement les écritures des DAO.
 *
 * <p>Le {@link DataSource} est mémorisé dans un champ statique : plusieurs contextes Spring
 * (par exemple un contexte de test Annoté de {@code @MockitoBean}) peuvent coexister dans la même
 * JVM, et le dernier contexte instancié écrase alors ce champ. Pour que l'écriture d'un DAO
 * participe toujours à la transaction en cours, la connexion de la transaction est recherchée dans
 * les ressources synchronisées de Spring, et pas seulement pour le {@link DataSource} mémorisé.
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

    DataSource owner = currentDataSource;
    Connection target = null;

    ConnectionHolder holder = connectionHolder(currentDataSource);
    if (holder != null) {
      target = holderConnection(holder);
    }
    if (target == null) {
      // La transaction courante a pu être ouverte sur une autre instance de DataSource
      // (autre contexte Spring) : on réutilise sa connexion pour que l'écriture du DAO
      // soit réellement annulée par le rollback.
      Map.Entry<DataSource, ConnectionHolder> transactional = transactionalConnection();
      if (transactional != null) {
        owner = transactional.getKey();
        target = holderConnection(transactional.getValue());
      }
    }
    if (target == null) {
      target = DataSourceUtils.getConnection(currentDataSource);
      owner = currentDataSource;
    }

    final Connection connection = target;
    final DataSource connectionOwner = owner;
    boolean[] released = {false};
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
              if ("close".equals(method.getName()) && method.getParameterCount() == 0) {
                if (!released[0]) {
                  released[0] = true;
                  DataSourceUtils.releaseConnection(connection, connectionOwner);
                }
                return null;
              }
              try {
                return method.invoke(connection, args);
              } catch (InvocationTargetException exception) {
                throw exception.getCause();
              }
            });
  }

  /** Porteur de connexion associé au {@link DataSource} courant, s'il existe. */
  private static ConnectionHolder connectionHolder(DataSource candidate) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return null;
    }
    Object resource = TransactionSynchronizationManager.getResource(candidate);
    return resource instanceof ConnectionHolder holder ? holder : null;
  }

  /** Connexion liée à la transaction en cours, quel que soit le {@link DataSource} qui l'a ouverte. */
  private static Map.Entry<DataSource, ConnectionHolder> transactionalConnection() {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return null;
    }
    Map<Object, Object> resources = TransactionSynchronizationManager.getResourceMap();
    if (resources == null) {
      return null;
    }
    for (Map.Entry<Object, Object> resource : resources.entrySet()) {
      if (resource.getKey() instanceof DataSource candidate
          && resource.getValue() instanceof ConnectionHolder holder
          && holderConnection(holder) != null) {
        return Map.entry(candidate, holder);
      }
    }
    return null;
  }

  /** Connexion physique portée par le holder, ou {@code null} si elle a déjà été libérée. */
  private static Connection holderConnection(ConnectionHolder holder) {
    if (holder == null) {
      return null;
    }
    ConnectionHandle handle = holder.getConnectionHandle();
    return handle == null ? null : handle.getConnection();
  }
}
