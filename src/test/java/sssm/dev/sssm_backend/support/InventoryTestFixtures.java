package sssm.dev.sssm_backend.support;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

/**
 * Jeu de données autonome d'un test d'intégration de l'inventaire.
 *
 * <p>Chaque instance crée sa propre entreprise, son utilisateur, son entrepôt, son type d'article,
 * son employé et son poste, tous marqués par un préfixe unique. Les tests ne dépendent donc plus des
 * données de démonstration et peuvent tourner sur une base dédiée. {@link #close()} supprime
 * exactement les lignes créées, dans l'ordre imposé par les clés étrangères.
 */
public final class InventoryTestFixtures implements AutoCloseable {

  private final DataSource dataSource;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  private final String marker;
  private final String companyId;
  private final String userId;
  private final String warehouseId;
  private final String itemTypeId;
  private final String itemCategoryId;
  private final String employeeId;
  private final String siteId;
  private final String postId;

  private InventoryTestFixtures(
      DataSource dataSource,
      JwtService jwtService,
      PasswordEncoder passwordEncoder,
      String marker,
      String companyId,
      String userId,
      String warehouseId,
      String itemTypeId,
      String itemCategoryId,
      String employeeId,
      String siteId,
      String postId) {
    this.dataSource = dataSource;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
    this.marker = marker;
    this.companyId = companyId;
    this.userId = userId;
    this.warehouseId = warehouseId;
    this.itemTypeId = itemTypeId;
    this.itemCategoryId = itemCategoryId;
    this.employeeId = employeeId;
    this.siteId = siteId;
    this.postId = postId;
  }

  /** Crée les fixtures d'un test ; {@code module} sert de préfixe lisible aux marqueurs. */
  public static InventoryTestFixtures create(
      DataSource dataSource, JwtService jwtService, PasswordEncoder passwordEncoder, String module)
      throws SQLException {
    String marker = module + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    String companyId = newId();
    String userId = newId();
    String warehouseId = newId();
    String itemTypeId = newId();
    String itemCategoryId = newId();
    String employeeId = newId();
    String siteId = newId();
    String postId = newId();

    InventoryTestFixtures fixtures =
        new InventoryTestFixtures(
            dataSource,
            jwtService,
            passwordEncoder,
            marker,
            companyId,
            userId,
            warehouseId,
            itemTypeId,
            itemCategoryId,
            employeeId,
            siteId,
            postId);
    fixtures.provision();
    return fixtures;
  }

  private void provision() throws SQLException {
    update(
        "INSERT INTO security.company (id, name, currency_code) VALUES (?, ?, 'MGA')",
        companyId,
        "Société " + marker);
    update(
        "INSERT INTO security.app_user "
            + "(id, company_id, username, email, password_hash, first_name, last_name, status) "
            + "VALUES (?, ?, ?, ?, ?, 'Test', 'Lot8', 'ACTIVE')",
        userId,
        companyId,
        "user-" + marker.toLowerCase(),
        "user-" + marker.toLowerCase() + "@example.test",
        passwordEncoder.encode("Lot8-Test-Password!"));
    update(
        "INSERT INTO security.item_category (id, company_id, name) VALUES (?, ?, ?)",
        itemCategoryId,
        companyId,
        "Catégorie " + marker);
    update(
        "INSERT INTO security.item_type "
            + "(id, company_id, category_id, code, name, unit, minimum_stock) "
            + "VALUES (?, ?, ?, ?, ?, 'piece', 1)",
        itemTypeId,
        companyId,
        itemCategoryId,
        "TYPE-" + marker,
        "Type " + marker);
    update(
        "INSERT INTO security.warehouse (id, company_id, name, active) VALUES (?, ?, ?, true)",
        warehouseId,
        companyId,
        "Entrepôt " + marker);
    update(
        "INSERT INTO security.employee "
            + "(id, company_id, user_id, employee_number, first_name, last_name, status) "
            + "VALUES (?, ?, ?, ?, 'Agent', 'Lot8', 'ACTIVE')",
        employeeId,
        companyId,
        userId,
        "EMP-" + marker);
    update(
        "INSERT INTO security.site (id, company_id, name, code, status) "
            + "VALUES (?, ?, ?, ?, 'ACTIVE')",
        siteId,
        companyId,
        "Site " + marker,
        "SITE-" + marker);
    update(
        "INSERT INTO security.security_post (id, site_id, name, code, status) "
            + "VALUES (?, ?, ?, ?, 'ACTIVE')",
        postId,
        siteId,
        "Poste " + marker,
        "POST-" + marker);
  }

  public String marker() {
    return marker;
  }

  public String companyId() {
    return companyId;
  }

  public String userId() {
    return userId;
  }

  public String warehouseId() {
    return warehouseId;
  }

  public String itemTypeId() {
    return itemTypeId;
  }

  public String employeeId() {
    return employeeId;
  }

  public String postId() {
    return postId;
  }

  /** Jeton synthétique d'administration, rattaché à l'entreprise des fixtures. */
  public String adminToken() {
    return jwtService.createAccessToken(
        userId, "user-" + marker.toLowerCase(), companyId, List.of("ADMIN"), List.of(), false);
  }

  /** Insère un article unitaire et renvoie son identifiant. */
  public String insertItem(String suffix, ItemStatus status, String currentWarehouseId)
      throws SQLException {
    String id = newId();
    update(
        "INSERT INTO security.item "
            + "(id, company_id, category_id, item_type_id, qr_code, description, status, "
            + "current_warehouse_id) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS security.item_status), ?)",
        id,
        companyId,
        itemCategoryId,
        itemTypeId,
        marker + "-" + suffix,
        "Article " + marker + "-" + suffix,
        status.name(),
        currentWarehouseId);
    return id;
  }

  /** Insère un article disponible dans l'entrepôt des fixtures. */
  public String insertAvailableItem(String suffix) throws SQLException {
    return insertItem(suffix, ItemStatus.AVAILABLE, warehouseId);
  }

  /** Fige le solde de l'entrepôt des fixtures pour le type d'article des fixtures. */
  public void setStock(String quantity) throws SQLException {
    update(
        "INSERT INTO security.stock_balance "
            + "(company_id, warehouse_id, item_type_id, quantity, version) VALUES (?, ?, ?, ?, 0) "
            + "ON CONFLICT (company_id, warehouse_id, item_type_id) DO UPDATE "
            + "SET quantity = EXCLUDED.quantity, version = security.stock_balance.version + 1",
        companyId,
        warehouseId,
        itemTypeId,
        new BigDecimal(quantity));
  }

  /** Solde courant de l'entrepôt des fixtures pour le type d'article des fixtures. */
  public BigDecimal stock() throws SQLException {
    BigDecimal value =
        decimal(
            "SELECT quantity FROM security.stock_balance "
                + "WHERE company_id = ? AND warehouse_id = ? AND item_type_id = ?",
            companyId,
            warehouseId,
            itemTypeId);
    return value == null ? BigDecimal.ZERO : value;
  }

  /** Statut courant d'un article. */
  public String itemStatus(String itemId) throws SQLException {
    return scalar("SELECT status FROM security.item WHERE id = ?", itemId);
  }

  /** Entrepôt courant d'un article, {@code null} s'il n'est affecté à aucun entrepôt. */
  public String itemWarehouse(String itemId) throws SQLException {
    return scalar("SELECT current_warehouse_id FROM security.item WHERE id = ?", itemId);
  }

  /** Nombre d'affectations actives d'un article. */
  public long activeAssignments(String itemId) throws SQLException {
    return count(
        "SELECT count(*) FROM security.item_assignment "
            + "WHERE item_id = ? AND returned_at IS NULL AND voided_at IS NULL",
        itemId);
  }

  /** Nombre de mouvements d'un article. */
  public long movementCount(String itemId) throws SQLException {
    return count("SELECT count(*) FROM security.stock_movement WHERE item_id = ?", itemId);
  }

  /** Premier identifiant d'affectation active d'un article. */
  public String activeAssignmentId(String itemId) throws SQLException {
    return scalar(
        "SELECT id FROM security.item_assignment "
            + "WHERE item_id = ? AND returned_at IS NULL AND voided_at IS NULL",
        itemId);
  }

  public String scalar(String sql, Object... parameters) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, parameters);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? resultSet.getString(1) : null;
      }
    }
  }

  public BigDecimal decimal(String sql, Object... parameters) throws SQLException {
    String value = scalar(sql, parameters);
    return value == null ? null : new BigDecimal(value);
  }

  public long count(String sql, Object... parameters) throws SQLException {
    String value = scalar(sql, parameters);
    return value == null ? 0L : Long.parseLong(value);
  }

  public int update(String sql, Object... parameters) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, parameters);
      return statement.executeUpdate();
    }
  }

  /** Insère un lot d'articles disponibles en une seule transaction locale. */
  public void insertAvailableItems(String suffix, int quantity) throws SQLException {
    String sql =
        "INSERT INTO security.item "
            + "(id, company_id, category_id, item_type_id, qr_code, description, status, "
            + "current_warehouse_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, 'AVAILABLE'::security.item_status, ?)";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      boolean autoCommit = connection.getAutoCommit();
      connection.setAutoCommit(false);
      try {
        for (int index = 0; index < quantity; index++) {
          statement.setObject(1, UUID.fromString(newId()));
          statement.setObject(2, UUID.fromString(companyId));
          statement.setObject(3, UUID.fromString(itemCategoryId));
          statement.setObject(4, UUID.fromString(itemTypeId));
          statement.setString(5, marker + "-" + suffix + "-" + index);
          statement.setString(6, "Article " + marker + "-" + suffix + "-" + index);
          statement.setObject(7, UUID.fromString(warehouseId));
          statement.addBatch();
        }
        statement.executeBatch();
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      } finally {
        connection.setAutoCommit(autoCommit);
      }
    }
  }

  /** Supprime toutes les lignes créées ; l'ordre respecte les clés étrangères. */
  @Override
  public void close() throws SQLException {
    // Le journal des mouvements ne renseigne pas company_id : le rattachement se fait par l'article.
    update(
        "DELETE FROM security.stock_movement WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE company_id = ?)",
        companyId);
    update(
        "DELETE FROM security.item_assignment WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE company_id = ?)",
        companyId);
    update("DELETE FROM security.stock_balance WHERE company_id = ?", companyId);
    update(
        "DELETE FROM security.inventory_count_scan WHERE inventory_count_id IN "
            + "(SELECT id FROM security.inventory_count WHERE company_id = ?)",
        companyId);
    update(
        "DELETE FROM security.inventory_count_expected WHERE inventory_count_id IN "
            + "(SELECT id FROM security.inventory_count WHERE company_id = ?)",
        companyId);
    update("DELETE FROM security.inventory_count WHERE company_id = ?", companyId);
    update("DELETE FROM security.item WHERE company_id = ?", companyId);
    update("DELETE FROM security.employee WHERE company_id = ?", companyId);
    update("DELETE FROM security.security_post WHERE site_id = ?", siteId);
    update("DELETE FROM security.site WHERE company_id = ?", companyId);
    update("DELETE FROM security.warehouse_stock_config WHERE warehouse_id = ?", warehouseId);
    update("DELETE FROM security.warehouse WHERE company_id = ?", companyId);
    update("DELETE FROM security.item_type WHERE company_id = ?", companyId);
    update("DELETE FROM security.item_category WHERE company_id = ?", companyId);
    update("DELETE FROM security.refresh_token WHERE user_id = ?", userId);
    update("DELETE FROM security.revoked_access_token WHERE user_id = ?", userId);
    update("DELETE FROM security.audit_log WHERE user_id = ?", userId);
    update("DELETE FROM security.app_user WHERE company_id = ?", companyId);
    update("DELETE FROM security.company WHERE id = ?", companyId);
  }

  /** Horodatage UTC utilisé pour les corps de requête. */
  public static Timestamp now() {
    return Timestamp.from(Instant.now());
  }

  private static String newId() {
    return UUID.randomUUID().toString();
  }

  private static void bind(PreparedStatement statement, Object... parameters) throws SQLException {
    for (int index = 0; index < parameters.length; index++) {
      Object value = parameters[index];
      if (value instanceof String text && isUuid(text)) {
        statement.setObject(index + 1, UUID.fromString(text));
      } else {
        statement.setObject(index + 1, value);
      }
    }
  }

  private static boolean isUuid(String value) {
    return value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
  }
}
