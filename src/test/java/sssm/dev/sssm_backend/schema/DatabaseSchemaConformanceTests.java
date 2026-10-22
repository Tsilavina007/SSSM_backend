package sssm.dev.sssm_backend.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static sssm.dev.sssm_backend.schema.SchemaTestSupport.columnDefinition;
import static sssm.dev.sssm_backend.schema.SchemaTestSupport.columns;
import static sssm.dev.sssm_backend.schema.SchemaTestSupport.enumLabels;
import static sssm.dev.sssm_backend.schema.SchemaTestSupport.openDatabase;
import static sssm.dev.sssm_backend.schema.SchemaTestSupport.projectRoot;
import static sssm.dev.sssm_backend.schema.SchemaTestSupport.schemaObjects;
import static sssm.dev.sssm_backend.schema.SchemaTestSupport.tables;
import static sssm.dev.sssm_backend.schema.SchemaTestSupport.tablesReferencedByDaos;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sssm.dev.sssm_backend.a01_common.enums.ContractStatus;
import sssm.dev.sssm_backend.a01_common.enums.EmployeeStatus;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a01_common.enums.MovementType;
import sssm.dev.sssm_backend.a01_common.enums.PostStatus;
import sssm.dev.sssm_backend.a01_common.enums.SiteStatus;
import sssm.dev.sssm_backend.a01_common.enums.UserStatus;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.rest.ItemAssignmentRest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.rest.ItemCategoryRest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.rest.ItemTypeRest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.rest.StockMovementRest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.rest.WarehouseRest;

/**
 * Contraste le schéma réellement appliqué en base avec le contrat de l'application.
 *
 * <p>Ces tests ne vérifient pas le comportement métier : ils vérifient que la base ressemble à ce
 * que le code attend (tables, colonnes, types, clés étrangères, index, énumérations), afin qu'une
 * migration divergente soit détectée immédiatement.
 */
class DatabaseSchemaConformanceTests {

  /** Objets énumérés du schéma comparés aux enums Java. */
  private static final Map<String, Class<? extends Enum<?>>> ENUMS =
      Map.of(
          "item_status", ItemStatus.class,
          "movement_type", MovementType.class,
          "employee_status", EmployeeStatus.class,
          "user_status", UserStatus.class,
          "post_status", PostStatus.class,
          "site_status", SiteStatus.class,
          "contract_status", ContractStatus.class);

  /** DTO exposés par l'API et table qui les porte. */
  private static final Map<Class<?>, String> DTO_TABLES =
      Map.of(
          ItemAssignmentRest.class, "item_assignment",
          StockMovementRest.class, "stock_movement",
          ItemCategoryRest.class, "item_category",
          ItemTypeRest.class, "item_type",
          WarehouseRest.class, "warehouse");

  /** Champs des DTO agrégés : ils portent des objets, pas des colonnes. */
  private static final Set<String> NESTED_FIELDS = Set.of("company", "branch", "category");

  @Test
  @DisplayName("Toutes les tables lues par les DAO existent dans le schéma security")
  void tablesUsedByTheDaosExist() throws Exception {
    Set<String> referenced = tablesReferencedByDaos();
    assertFalse(referenced.isEmpty(), "aucune table DAO n'a été détectée");

    try (Connection connection = openDatabase()) {
      Set<String> existing = schemaObjects(connection);
      List<String> missing =
          referenced.stream().filter(name -> !existing.contains(name)).sorted().toList();
      assertTrue(
          missing.isEmpty(),
          () -> "Objets référencés par les DAO mais absents du schéma security : " + missing);
    }
  }

  @Test
  @DisplayName("Les colonnes exposées par l'API existent dans les tables correspondantes")
  void columnsExposedByTheApiExist() throws Exception {
    try (Connection connection = openDatabase()) {
      for (Map.Entry<Class<?>, String> entry : DTO_TABLES.entrySet()) {
        Class<?> dto = entry.getKey();
        String table = entry.getValue();
        Set<String> available = columns(connection, table);
        assertFalse(available.isEmpty(), () -> "table absente : " + table);

        List<String> missing = new ArrayList<>();
        for (Field field : dto.getDeclaredFields()) {
          if (NESTED_FIELDS.contains(field.getName())) {
            continue;
          }
          if (!available.contains(field.getName())) {
            missing.add(table + "." + field.getName());
          }
        }
        assertTrue(
            missing.isEmpty(),
            () -> "Colonnes exposées par " + dto.getSimpleName() + " mais absentes : " + missing);
      }
    }
  }

  @Test
  @DisplayName("Les types énumérés de la base correspondent aux enums Java")
  void enumsMatchTheJavaEnums() throws Exception {
    try (Connection connection = openDatabase()) {
      for (Map.Entry<String, Class<? extends Enum<?>>> entry : ENUMS.entrySet()) {
        Set<String> expected = new LinkedHashSet<>();
        for (Object constant : entry.getValue().getEnumConstants()) {
          expected.add(((Enum<?>) constant).name());
        }
        Set<String> actual = enumLabels(connection, entry.getKey());
        assertEquals(
            expected,
            actual,
            () -> "type énuméré " + entry.getKey() + " divergent entre Java et PostgreSQL");
      }
    }
  }

  @Test
  @DisplayName("Les colonnes du cycle de vie des affectations ont le type attendu")
  void assignmentLifecycleColumnsMatchTheContract() throws Exception {
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("source_warehouse_id", "uuid");
    expected.put("voided_at", "timestamp with time zone");
    expected.put("voided_by", "uuid");
    expected.put("void_reason", "text");
    expected.put("returned_at", "timestamp with time zone");
    expected.put("assigned_at", "timestamp with time zone NOT NULL");
    expected.put("company_id", "uuid");
    expected.put("expected_return_at", "timestamp with time zone");

    try (Connection connection = openDatabase()) {
      for (Map.Entry<String, String> entry : expected.entrySet()) {
        String actual = columnDefinition(connection, "item_assignment", entry.getKey());
        assertNotNull(actual, () -> "colonne absente : item_assignment." + entry.getKey());
        assertEquals(entry.getValue(), actual, () -> "type divergent pour " + entry.getKey());
      }
    }
  }

  @Test
  @DisplayName("Les référentiels portent les colonnes d'archivage")
  void masterDataExposesArchiveColumns() throws Exception {
    try (Connection connection = openDatabase()) {
      for (String table : List.of("item", "item_category", "item_type", "warehouse")) {
        Set<String> available = columns(connection, table);
        assertTrue(
            available.contains("archived_at"), () -> "archived_at absent de " + table);
      }
      for (String table : List.of("item_category", "item_type", "warehouse")) {
        Set<String> available = columns(connection, table);
        assertTrue(
            available.contains("archived_by"), () -> "archived_by absent de " + table);
      }
    }
  }

  @Test
  @DisplayName("Le stock calculé expose ses colonnes et sa clé unique")
  void stockTablesMatchTheContract() throws Exception {
    try (Connection connection = openDatabase()) {
      Set<String> balances = columns(connection, "stock_balance");
      assertTrue(balances.containsAll(
          List.of("company_id", "warehouse_id", "item_type_id", "quantity", "version")),
          () -> "colonnes manquantes sur stock_balance : " + balances);

      Set<String> movements = columns(connection, "stock_movement");
      assertTrue(movements.containsAll(
          List.of(
              "quantity",
              "source_warehouse_id",
              "destination_warehouse_id",
              "operation_id",
              "performed_by",
              "created_at")),
          () -> "colonnes manquantes sur stock_movement : " + movements);

      String unique = uniqueIndexDefinition(connection, "stock_balance");
      assertNotNull(unique, "aucun index unique sur stock_balance");
      assertTrue(unique.contains("company_id"), () -> "clé unique incomplète : " + unique);
    }
  }

  @Test
  @DisplayName("Les clés étrangères de l'affectation sont déclarées")
  void assignmentForeignKeysAreDeclared() throws Exception {
    try (Connection connection = openDatabase()) {
      Set<String> targets = foreignKeyTargets(connection, "item_assignment");
      assertTrue(
          targets.containsAll(
              List.of("item", "employee", "security_post", "warehouse")),
          () -> "clés étrangères manquantes sur item_assignment : " + targets);
    }
  }

  @Test
  @DisplayName("Un article ne peut avoir qu'une affectation active")
  void activeAssignmentUniquenessIsEnforced() throws Exception {
    try (Connection connection = openDatabase()) {
      String definition = partialIndexDefinition(connection, "uq_item_assignment_active");
      assertNotNull(definition, "index unique partiel uq_item_assignment_active absent");
      assertTrue(definition.contains("item_id"), () -> "index inattendu : " + definition);
      assertTrue(
          definition.toLowerCase().contains("returned_at is null"),
          () -> "l'index doit porter sur returned_at IS NULL : " + definition);
    }
  }

  @Test
  @DisplayName("Les règles de void sont appliquées par la base")
  void voidRulesAreEnforcedByTheDatabase() throws Exception {
    try (Connection connection = openDatabase()) {
      connection.setAutoCommit(false);
      try {
        String itemId = firstIdWithoutActiveAssignment(connection);
        String employeeId = firstId(connection, "SELECT id FROM security.employee ORDER BY id LIMIT 1");
        String postId =
            firstId(connection, "SELECT id FROM security.security_post ORDER BY id LIMIT 1");
        assertNotNull(itemId, "aucun article disponible pour le test");
        assertNotNull(employeeId, "aucun employé de démonstration");
        assertNotNull(postId, "aucun poste de démonstration");

        String violation = insertViolation(connection, itemId, employeeId, postId, "returned_at");
        assertTrue(
            violation.contains("item_assignment_voided_requires_return"),
            () -> "void sans retour non refusé : " + violation);

        connection.rollback();

        String missingReason = insertViolation(connection, itemId, employeeId, postId, "void_reason");
        assertTrue(
            missingReason.contains("item_assignment_voided_requires_reason"),
            () -> "void sans motif non refusé : " + missingReason);
      } finally {
        connection.rollback();
        connection.setAutoCommit(true);
      }
    }
  }

  @Test
  @DisplayName("La base de démonstration contient les tables attendues")
  void demoDatabaseExposesTheApplicationTables() throws Exception {
    try (Connection connection = openDatabase()) {
      Set<String> existing = tables(connection);
      assertTrue(
          existing.containsAll(
              List.of(
                  "company",
                  "app_user",
                  "role",
                  "permission",
                  "item",
                  "item_type",
                  "item_category",
                  "warehouse",
                  "item_assignment",
                  "stock_movement",
                  "stock_balance",
                  "employee",
                  "security_post")),
          () -> "tables applicatives manquantes : " + existing);
    }
  }

  @Test
  @DisplayName("Le journal des scans existe avec ses colonnes, son type et ses index")
  void qrScanJournalMatchesTheContract() throws Exception {
    try (Connection connection = openDatabase()) {
      Set<String> columns = columns(connection, "qr_scan");
      assertTrue(
          columns.containsAll(
              List.of(
                  "id",
                  "company_id",
                  "item_id",
                  "qr_code",
                  "scanned_by",
                  "scanned_at",
                  "device_id",
                  "result")),
          () -> "colonnes manquantes sur qr_scan : " + columns);

      assertEquals(
          "uuid NOT NULL", columnDefinition(connection, "qr_scan", "id"));
      assertEquals(
          "text NOT NULL", columnDefinition(connection, "qr_scan", "qr_code"));
      assertEquals(
          "timestamp with time zone NOT NULL",
          columnDefinition(connection, "qr_scan", "scanned_at"));
      assertEquals("uuid", columnDefinition(connection, "qr_scan", "company_id"));

      assertEquals(
          Set.of("RESOLVED", "NOT_FOUND"),
          enumLabels(connection, "qr_scan_result"),
          "le type qr_scan_result doit exposer exactement les résultats attendus");

      Set<String> targets = foreignKeyTargets(connection, "qr_scan");
      assertTrue(
          targets.containsAll(List.of("company", "item", "app_user")),
          () -> "clés étrangères manquantes sur qr_scan : " + targets);

      Set<String> indexes = indexNames(connection, "qr_scan");
      assertTrue(
          indexes.containsAll(
              List.of(
                  "idx_qr_scan_company_scanned_at",
                  "idx_qr_scan_item",
                  "idx_qr_scan_code")),
          () -> "index manquants sur qr_scan : " + indexes);
    }
  }

  private static Set<String> indexNames(Connection connection, String table) throws SQLException {
    Set<String> names = new LinkedHashSet<>();
    String sql =
        "SELECT indexname FROM pg_indexes WHERE schemaname = 'security' AND tablename = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          names.add(resultSet.getString(1));
        }
      }
    }
    return names;
  }

  @Test
  @DisplayName("La base contient exactement les tables créées par les scripts")
  void databaseTablesMatchTheSchemaScripts() throws Exception {
    Set<String> created = createdTables();
    try (Connection connection = openDatabase()) {
      Set<String> existing = tables(connection);
      List<String> missing =
          created.stream().filter(name -> !existing.contains(name)).sorted().toList();
      List<String> extra = existing.stream().filter(name -> !created.contains(name)).sorted().toList();
      assertTrue(missing.isEmpty(), () -> "Tables du schéma absentes de la base : " + missing);
      assertTrue(extra.isEmpty(), () -> "Tables de la base absentes du schéma : " + extra);
    }
  }

  /** Tables créées par les scripts de {@code docs/database/schema}. */
  private static Set<String> createdTables() {
    return SchemaTestSupport.createdTables(projectRoot().resolve("docs/database/schema"));
  }

  private static String insertViolation(
      Connection connection, String itemId, String employeeId, String postId, String expected)
      throws SQLException {
    String sql =
        "INSERT INTO security.item_assignment "
            + "(item_id, employee_id, post_id, assigned_at, voided_at, void_reason) "
            + "VALUES (?, ?, ?, now(), now(), ?)";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, java.util.UUID.fromString(itemId));
      statement.setObject(2, java.util.UUID.fromString(employeeId));
      statement.setObject(3, java.util.UUID.fromString(postId));
      statement.setString(4, expected.equals("void_reason") ? null : "motif de test");
      statement.executeUpdate();
      fail("l'insertion invalide aurait dû être refusée");
      return "";
    } catch (SQLException exception) {
      return exception.getMessage() == null ? "" : exception.getMessage();
    }
  }

  private static String firstIdWithoutActiveAssignment(Connection connection) throws SQLException {
    String sql =
        "SELECT i.id FROM security.item i WHERE NOT EXISTS ("
            + "SELECT 1 FROM security.item_assignment a "
            + "WHERE a.item_id = i.id AND a.returned_at IS NULL) ORDER BY i.id LIMIT 1";
    return firstId(connection, sql);
  }

  private static String firstId(Connection connection, String sql) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      return resultSet.next() ? resultSet.getString(1) : null;
    }
  }

  private static Set<String> foreignKeyTargets(Connection connection, String table)
      throws SQLException {
    Set<String> targets = new LinkedHashSet<>();
    String sql =
        "SELECT target.relname FROM pg_constraint con "
            + "JOIN pg_class source ON source.oid = con.conrelid "
            + "JOIN pg_class target ON target.oid = con.confrelid "
            + "JOIN pg_namespace n ON n.oid = source.relnamespace "
            + "WHERE con.contype = 'f' AND n.nspname = 'security' AND source.relname = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          targets.add(resultSet.getString(1));
        }
      }
    }
    return targets;
  }

  private static String uniqueIndexDefinition(Connection connection, String table)
      throws SQLException {
    String sql =
        "SELECT indexdef FROM pg_indexes WHERE schemaname = 'security' AND tablename = ? "
            + "AND indexdef LIKE '%UNIQUE%' AND indexdef LIKE '%company_id%'";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? resultSet.getString(1) : null;
      }
    }
  }

  private static String partialIndexDefinition(Connection connection, String indexName)
      throws SQLException {
    String sql =
        "SELECT indexdef FROM pg_indexes WHERE schemaname = 'security' AND indexname = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, indexName);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? resultSet.getString(1) : null;
      }
    }
  }

  /** Contrôle supplémentaire : les colonnes d'archivage ne sont pas vides par défaut. */
  @Test
  @DisplayName("Les colonnes d'archivage sont neutres par défaut")
  void archiveColumnsDefaultToNull() throws Exception {
    try (Connection connection = openDatabase()) {
      for (String table : List.of("item", "item_category", "item_type", "warehouse")) {
        Set<String> available = columns(connection, table);
        if (available.contains("archived_at")) {
          assertEquals(
              "timestamp with time zone",
              columnDefinition(connection, table, "archived_at"),
              () -> "type inattendu pour " + table + ".archived_at");
        }
      }
    }
  }

  /** Les tables de l'application portent toutes une clé primaire. */
  @Test
  @DisplayName("Les tables applicatives ont une clé primaire")
  void applicationTablesHavePrimaryKey() throws Exception {
    List<String> tables = Arrays.asList(
        "item",
        "item_type",
        "item_category",
        "warehouse",
        "item_assignment",
        "stock_movement",
        "stock_balance",
        "employee",
        "security_post",
        "company");
    try (Connection connection = openDatabase()) {
      for (String table : tables) {
        String sql =
            "SELECT count(*) FROM pg_constraint con "
                + "JOIN pg_class c ON c.oid = con.conrelid "
                + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE con.contype = 'p' AND n.nspname = 'security' AND c.relname = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
          statement.setString(1, table);
          try (ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            assertEquals(1, resultSet.getInt(1), () -> "clé primaire absente sur " + table);
          }
        }
      }
    }
  }
}
