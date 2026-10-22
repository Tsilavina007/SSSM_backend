package sssm.dev.sssm_backend.schema;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Utilitaires partagés par les tests de conformité entre les scripts SQL de {@code docs/database} et
 * l'application.
 *
 * <p>Ces tests n'utilisent pas Spring : ils ouvrent une connexion JDBC avec la même configuration
 * que l'application ({@code DB_URL}, {@code DB_USER}, {@code DB_PASSWORD}) afin de rester rapides et
 * de contrôler l'état réel de la base.
 */
final class SchemaTestSupport {

  private SchemaTestSupport() {}

  /** Ouvre une connexion avec la configuration de l'application. */
  static Connection openDatabase() throws SQLException {
    String url = environment("DB_URL", "jdbc:postgresql://localhost:5432/sssm_db");
    String user = environment("DB_USER", "postgres");
    String password = environment("DB_PASSWORD", "postgres");
    return DriverManager.getConnection(url, user, password);
  }

  private static String environment(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }

  /** Racine du dépôt, repérée par la présence des scripts SQL et des sources Java. */
  static Path projectRoot() {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
      if (Files.isDirectory(current.resolve("docs/database/schema"))
          && Files.isDirectory(current.resolve("src/main/java"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException(
        "Racine du projet introuvable depuis " + System.getProperty("user.dir"));
  }

  /** Colonnes d'une table du schéma {@code security}. */
  static Set<String> columns(Connection connection, String table) throws SQLException {
    Set<String> columns = new LinkedHashSet<>();
    String sql =
        "SELECT column_name FROM information_schema.columns "
            + "WHERE table_schema = 'security' AND table_name = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          columns.add(resultSet.getString(1));
        }
      }
    }
    return columns;
  }

  /** Type SQL et nullabilité d'une colonne. */
  static String columnDefinition(Connection connection, String table, String column)
      throws SQLException {
    String sql =
        "SELECT data_type, is_nullable FROM information_schema.columns "
            + "WHERE table_schema = 'security' AND table_name = ? AND column_name = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, table);
      statement.setString(2, column);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return null;
        }
        return resultSet.getString(1) + (resultSet.getString(2).equals("NO") ? " NOT NULL" : "");
      }
    }
  }

  /** Tables du schéma {@code security}. */
  static Set<String> tables(Connection connection) throws SQLException {
    Set<String> tables = new LinkedHashSet<>();
    String sql =
        "SELECT table_name FROM information_schema.tables "
            + "WHERE table_schema = 'security' AND table_type = 'BASE TABLE'";
    try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        tables.add(resultSet.getString(1));
      }
    }
    return tables;
  }

  /** Types énumérés du schéma {@code security} (tables et types PostgreSQL). */
  static Set<String> schemaObjects(Connection connection) throws SQLException {
    Set<String> objects = new LinkedHashSet<>();
    String sql =
        "SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
            + "WHERE n.nspname = 'security' AND c.relkind IN ('r', 'v', 'm', 'S') "
            + "UNION SELECT t.typname FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace "
            + "WHERE n.nspname = 'security' AND t.typtype = 'e'";
    try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        objects.add(resultSet.getString(1));
      }
    }
    return objects;
  }

  /** Valeurs d'un type énuméré de PostgreSQL. */
  static Set<String> enumLabels(Connection connection, String typeName) throws SQLException {
    Set<String> labels = new LinkedHashSet<>();
    String sql =
        "SELECT e.enumlabel FROM pg_enum e "
            + "JOIN pg_type t ON t.oid = e.enumtypid "
            + "JOIN pg_namespace n ON n.oid = t.typnamespace "
            + "WHERE n.nspname = 'security' AND t.typname = ? ORDER BY e.enumsortorder";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, typeName);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          labels.add(resultSet.getString(1));
        }
      }
    }
    return labels;
  }

  /** Tables référencées par la couche DAO, via le préfixe {@code security.} du SQL. */
  static Set<String> tablesReferencedByDaos() {
    Path repositories = projectRoot().resolve("src/main/java");
    Set<String> referenced = new LinkedHashSet<>();
    Pattern reference = Pattern.compile("security\\.([a-z_][a-z0-9_]*)");
    try (Stream<Path> files = Files.walk(repositories)) {
      List<Path> daoFiles =
          files.filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".java"))
              .filter(path -> path.getParent().getFileName().toString().equals("repository"))
              .toList();
      for (Path file : daoFiles) {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        Matcher matcher = reference.matcher(content);
        while (matcher.find()) {
          referenced.add(matcher.group(1));
        }
      }
    } catch (IOException exception) {
      throw new UncheckedIOException("Lecture des sources Java impossible", exception);
    }
    return referenced;
  }

  /** Fichiers SQL d'un dossier, triés par nom. */
  static List<Path> sqlFiles(Path directory) {
    try (Stream<Path> files = Files.list(directory)) {
      List<Path> found = new ArrayList<>();
      files.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".sql"))
          .forEach(found::add);
      found.sort(Comparator.comparing(path -> path.getFileName().toString()));
      return found;
    } catch (IOException exception) {
      throw new UncheckedIOException("Lecture du dossier " + directory + " impossible", exception);
    }
  }

  /**
   * Tables créées par les scripts de schéma. Le nom peut être préfixé par le schéma
   * ({@code security.stock_balance}) : seul le dernier segment est retenu.
   */
  static Set<String> createdTables(Path schemaDirectory) {
    Pattern create =
        Pattern.compile(
            "CREATE\\s+TABLE\\s+(IF\\s+NOT\\s+EXISTS\\s+)?([a-z_][a-z0-9_.]*)", Pattern.CASE_INSENSITIVE);
    Set<String> tables = new LinkedHashSet<>();
    for (Path file : sqlFiles(schemaDirectory)) {
      Matcher matcher = create.matcher(read(file));
      while (matcher.find()) {
        String name = matcher.group(2).toLowerCase(Locale.ROOT);
        tables.add(name.substring(name.lastIndexOf('.') + 1));
      }
    }
    return tables;
  }

  /** Contenu texte d'un fichier. */
  static String read(Path file) {
    try {
      return Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new UncheckedIOException("Lecture de " + file + " impossible", exception);
    }
  }
}
