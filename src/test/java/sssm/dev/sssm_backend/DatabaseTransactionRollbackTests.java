package sssm.dev.sssm_backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_category.repository.ItemCategoryDAO;

/** Prouve qu'une écriture de DAO participe à la transaction Spring et est annulée en cas d'échec. */
@SpringBootTest
class DatabaseTransactionRollbackTests {

  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private ItemCategoryDAO itemCategoryDAO;
  @Autowired private DataSource dataSource;

  private String companyId;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("SELECT id FROM security.company ORDER BY id LIMIT 1");
        ResultSet rs = stmt.executeQuery()) {
      companyId = rs.next() ? rs.getString(1) : null;
    }
    Assumptions.assumeTrue(companyId != null, "entreprise de démonstration requise");
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void connectionComesFromSpringHikariPool() throws Exception {
    assertTrue(
        dataSource instanceof HikariDataSource,
        "le DataSource Spring doit être le pool HikariCP configuré");
    try (Connection conn = DatabaseConnection.getConnection()) {
      assertNotNull(conn, "la connexion applicative doit être disponible");
      assertFalse(conn.isClosed(), "la connexion applicative doit être ouverte");
    }
  }

  @Test
  void rollbackAfterFirstWriteLeavesNoPartialRow() throws Exception {
    String marker = "phase2-tx-" + System.nanoTime();
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

    assertThrows(
        IllegalStateException.class,
        () ->
            transactionTemplate.executeWithoutResult(
                status -> {
                  ItemCategoryCreateRequest request = new ItemCategoryCreateRequest();
                  request.setCompany_id(companyId);
                  request.setName(marker);
                  itemCategoryDAO.createItemCategory(request);
                  throw new IllegalStateException("échec forcé après la première écriture");
                }));

    assertFalse(categoryExists(marker), "la première écriture doit être annulée par le rollback");
  }

  private static boolean categoryExists(String name) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("SELECT 1 FROM security.item_category WHERE name = ?")) {
      stmt.setString(1, name);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    }
  }

  private static void purge() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "DELETE FROM security.item_category WHERE name LIKE 'phase2-tx-%'")) {
      stmt.executeUpdate();
    }
  }
}
