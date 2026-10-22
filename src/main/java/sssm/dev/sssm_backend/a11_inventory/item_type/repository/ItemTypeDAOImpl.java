package sssm.dev.sssm_backend.a11_inventory.item_type.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeResponse;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeUpdateRequest;

@Repository
public class ItemTypeDAOImpl implements ItemTypeDAO {

  @Override
  public List<ItemTypeResponse> getAllItemTypes(int page, int pageSize) {
    String sql = "SELECT * FROM security.item_type ORDER BY id LIMIT ? OFFSET ?";
    List<ItemTypeResponse> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(
              new ItemTypeResponse(
                  rs.getString("id"),
                  rs.getString("company_id"),
                  rs.getString("category_id"),
                  rs.getString("code"),
                  rs.getString("name"),
                  rs.getString("description"),
                  rs.getString("unit"),
                  rs.getBigDecimal("quantity_total"),
                  rs.getBigDecimal("minimum_stock"),
                  ItemStatus.valueOf(rs.getString("status")),
                  rs.getTimestamp("created_at"),
                  rs.getTimestamp("updated_at")));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return values;
  }

  @Override
  public List<ItemTypeResponse> getAllItemTypesByItemCategoryId(
      String itemCategoryId, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.item_type WHERE category_id = ? ORDER BY id LIMIT ? OFFSET ?";
    List<ItemTypeResponse> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;
    UUID uuid = UUID.fromString(itemCategoryId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);
      stmt.setInt(2, pageSize);
      stmt.setInt(3, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(mapRowToItemTypeResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return values;
  }

  @Override
  public List<ItemTypeResponse> searchItemTypesByName(String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.item_type WHERE code ILIKE ? OR name ILIKE ? OR description ILIKE ? ORDER BY id LIMIT ? OFFSET ?";
    List<ItemTypeResponse> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      String searchPattern = "%" + value + "%";
      stmt.setString(1, searchPattern);
      stmt.setString(2, searchPattern);
      stmt.setString(3, searchPattern);
      stmt.setInt(4, pageSize);
      stmt.setInt(5, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(mapRowToItemTypeResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return values;
  }

  @Override
  public Optional<ItemTypeResponse> getItemTypeById(String id) {
    String sql = "SELECT * FROM security.item_type WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToItemTypeResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return Optional.empty();
  }

  @Override
  public ItemTypeResponse createItemType(ItemTypeCreateRequest value) {
    // Les colonnes nullable ou dotées d'un DEFAULT PostgreSQL ne sont écrites que si une valeur
    // est fournie. Un NULL explicite empêcherait l'application de DEFAULT (unit, quantités,
    // statut).
    StringBuilder columns = new StringBuilder("company_id, code, name");
    StringBuilder placeholders = new StringBuilder("?, ?, ?");
    List<Object> parameters = new ArrayList<>();

    parameters.add(value.getCompany_id() == null ? null : UUID.fromString(value.getCompany_id()));
    parameters.add(value.getCode());
    parameters.add(value.getName());

    if (value.getCategory_id() != null) {
      columns.append(", category_id");
      placeholders.append(", ?");
      parameters.add(UUID.fromString(value.getCategory_id()));
    }
    if (value.getDescription() != null) {
      columns.append(", description");
      placeholders.append(", ?");
      parameters.add(value.getDescription());
    }
    if (value.getUnit() != null) {
      columns.append(", unit");
      placeholders.append(", ?");
      parameters.add(value.getUnit());
    }
    if (value.getQuantity_total() != null) {
      columns.append(", quantity_total");
      placeholders.append(", ?");
      parameters.add(value.getQuantity_total());
    }
    if (value.getMinimum_stock() != null) {
      columns.append(", minimum_stock");
      placeholders.append(", ?");
      parameters.add(value.getMinimum_stock());
    }
    if (value.getStatus() != null) {
      columns.append(", status");
      placeholders.append(", CAST(? AS security.item_status)");
      parameters.add(value.getStatus().name());
    }

    String sql =
        "INSERT INTO security.item_type ("
            + columns
            + ") VALUES ("
            + placeholders
            + ") RETURNING id";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return getItemTypeById(rs.getString("id"))
              .orElseThrow(
                  () ->
                      new ServerException(
                          "Le type d'article créé n'a pas pu être relu depuis la base."));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    throw new ServerException("La base n'a pas renvoyé l'identifiant du type d'article créé.");
  }

  @Override
  public ItemTypeResponse updateItemType(String id, ItemTypeUpdateRequest value) {
    StringBuilder sql = new StringBuilder("UPDATE security.item_type SET ");
    List<Object> parameters = new ArrayList<>();

    if (value.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(value.getCompany_id()));
    }
    if (value.getCategory_id() != null) {
      sql.append("category_id = ?, ");
      parameters.add(UUID.fromString(value.getCategory_id()));
    }
    if (value.getCode() != null) {
      sql.append("code = ?, ");
      parameters.add(value.getCode());
    }
    if (value.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(value.getName());
    }
    if (value.getDescription() != null) {
      sql.append("description = ?, ");
      parameters.add(value.getDescription());
    }
    if (value.getUnit() != null) {
      sql.append("unit = ?, ");
      parameters.add(value.getUnit());
    }
    if (value.getQuantity_total() != null) {
      sql.append("quantity_total = ?, ");
      parameters.add(value.getQuantity_total());
    }
    if (value.getMinimum_stock() != null) {
      sql.append("minimum_stock = ?, ");
      parameters.add(value.getMinimum_stock());
    }
    if (value.getStatus() != null) {
      sql.append("status = CAST(? AS security.item_status), ");
      parameters.add(value.getStatus().name());
    }

    if (parameters.isEmpty()) {
      return getItemTypeById(id)
          .orElseThrow(
              () ->
                  new NotFoundException(
                      "Type d'article introuvable",
                      "Aucun type d'article ne correspond à l'identifiant " + id + ".",
                      "NOT_FOUND"));
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(id));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return getItemTypeById(id)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Type d'article introuvable",
                    "Aucun type d'article ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  @Override
  public void deleteItemType(String id) {
    String sql = "DELETE FROM security.item_type WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private ItemTypeResponse mapRowToItemTypeResponse(ResultSet rs) throws SQLException {
    return new ItemTypeResponse(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("category_id"),
        rs.getString("code"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getString("unit"),
        rs.getBigDecimal("quantity_total"),
        rs.getBigDecimal("minimum_stock"),
        ItemStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at"));
  }
}
