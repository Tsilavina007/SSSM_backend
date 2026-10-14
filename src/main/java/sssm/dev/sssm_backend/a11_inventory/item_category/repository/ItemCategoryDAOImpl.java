package sssm.dev.sssm_backend.a11_inventory.item_category.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryUpdateRequest;

@Repository
public class ItemCategoryDAOImpl implements ItemCategoryDAO {

  @Override
  public List<ItemCategoryResponse> getAllItemCategories(int page, int pageSize) {
    String sql = "SELECT * FROM security.item_category ORDER BY id LIMIT ? OFFSET ?";
    List<ItemCategoryResponse> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(
              new ItemCategoryResponse(
                  rs.getString("id"),
                  rs.getString("company_id"),
                  rs.getString("name"),
                  rs.getString("description")));
        }
      }
    } catch (SQLException e) {
      throw new ServerException("Error when getAllItemCategories : " + e);
    }
    return values;
  }

  @Override
  public List<ItemCategoryResponse> searchItemCategoriesByName(
      String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.item_category WHERE name ILIKE ? OR description ILIKE ? ORDER BY id LIMIT ? OFFSET ?";
    List<ItemCategoryResponse> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      String searchPattern = "%" + value + "%";
      stmt.setString(1, searchPattern);
      stmt.setString(2, searchPattern);
      stmt.setInt(3, pageSize);
      stmt.setInt(4, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(mapRowToItemCategoryResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException("Error when searchItemCategoriesByName : " + e);
    }
    return values;
  }

  @Override
  public Optional<ItemCategoryResponse> getItemCategoryById(String id) {
    String sql = "SELECT * FROM security.item_category WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToItemCategoryResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException("Error when getItemCategoryById : " + e);
    }
    return Optional.empty();
  }

  @Override
  public ItemCategoryResponse createItemCategory(ItemCategoryCreateRequest value) {
    String sql =
        "INSERT INTO security.item_category (company_id, name, description) VALUES (?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(
          1, value.getCompany_id() == null ? null : UUID.fromString(value.getCompany_id()));
      stmt.setString(2, value.getName());
      stmt.setString(3, value.getDescription());
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          return mapRowToItemCategoryResponse(rs);
        }
      }
    } catch (SQLException e) {
      throw new ServerException("Error when createItemCategory : " + e);
    }
    return null;
  }

  @Override
  public ItemCategoryResponse updateItemCategory(String id, ItemCategoryUpdateRequest value) {
    StringBuilder sql = new StringBuilder("UPDATE security.item_category SET ");
    List<Object> parameters = new ArrayList<>();

    if (value.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(value.getCompany_id()));
    }
    if (value.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(value.getName());
    }
    if (value.getDescription() != null) {
      sql.append("description = ?, ");
      parameters.add(value.getDescription());
    }

    if (parameters.isEmpty()) {
      return getItemCategoryById(id).get();
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
      throw new ServerException("Error when updateItemCategory : " + e);
    }
    return getItemCategoryById(id).get();
  }

  @Override
  public void deleteItemCategory(String id) {
    String sql = "DELETE FROM security.item_category WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException("Error when deleteItemCategory : " + e);
    }
  }

  private ItemCategoryResponse mapRowToItemCategoryResponse(ResultSet rs) throws SQLException {
    return new ItemCategoryResponse(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("name"),
        rs.getString("description"));
  }
}
