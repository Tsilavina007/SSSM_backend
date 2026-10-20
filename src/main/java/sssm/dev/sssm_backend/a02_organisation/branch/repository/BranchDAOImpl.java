package sssm.dev.sssm_backend.a02_organisation.branch.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.enums.SiteStatus;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchCreateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchResponse;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchUpdateRequest;

@Repository
public class BranchDAOImpl implements BranchDAO {

  @Override
  public List<BranchResponse> searchBranchesByName(String searchName, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.branch WHERE name ILIKE ? OR code ILIKE ? LIMIT ? OFFSET ?";
    List<BranchResponse> branches = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      String searchPattern = "%" + searchName + "%";
      stmt.setString(1, searchPattern);
      stmt.setString(2, searchPattern);
      stmt.setInt(3, pageSize);
      stmt.setInt(4, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          BranchResponse branch = mapRowToBranch(rs);
          branches.add(branch);
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return branches;
  }

  @Override
  public List<BranchResponse> getAllBranches(int page, int pageSize) {
    String sql = "SELECT * FROM security.branch LIMIT ? OFFSET ?";
    List<BranchResponse> branches = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          BranchResponse branch = mapRowToBranch(rs);
          branches.add(branch);
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return branches;
  }

  @Override
  public Optional<BranchResponse> getBranchById(String branchId) {
    String sql = "SELECT * FROM security.branch WHERE id = ?";
    UUID id = UUID.fromString(branchId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToBranch(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return Optional.empty();
  }

  @Override
  public BranchResponse createBranch(BranchCreateRequest branch) {
    String sql =
        "INSERT INTO security.branch (company_id, agence_id, name, code, address, city, phone, "
            + "email, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS security.site_status))";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(branch.getCompany_id()));
      stmt.setObject(2, UUID.fromString(branch.getAgence_id()));
      stmt.setString(3, branch.getName());
      stmt.setString(4, branch.getCode());
      stmt.setString(5, branch.getAddress());
      stmt.setString(6, branch.getCity());
      stmt.setString(7, branch.getPhone());
      stmt.setString(8, branch.getEmail());
      stmt.setString(9, branch.getStatus() == null ? null : branch.getStatus().name());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          return mapRowToBranch(rs);
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return null;
  }

  @Override
  public BranchResponse updateBranch(String id, BranchUpdateRequest branch) {
    StringBuilder sql = new StringBuilder("UPDATE security.branch SET ");
    List<Object> parameters = new ArrayList<>();

    if (branch.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(branch.getCompany_id()));
    }
    if (branch.getAgence_id() != null) {
      sql.append("agence_id = ?, ");
      parameters.add(UUID.fromString(branch.getAgence_id()));
    }
    if (branch.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(branch.getName());
    }
    if (branch.getCode() != null) {
      sql.append("code = ?, ");
      parameters.add(branch.getCode());
    }
    if (branch.getAddress() != null) {
      sql.append("address = ?, ");
      parameters.add(branch.getAddress());
    }
    if (branch.getCity() != null) {
      sql.append("city = ?, ");
      parameters.add(branch.getCity());
    }
    if (branch.getPhone() != null) {
      sql.append("phone = ?, ");
      parameters.add(branch.getPhone());
    }
    if (branch.getEmail() != null) {
      sql.append("email = ?, ");
      parameters.add(branch.getEmail());
    }
    if (branch.getStatus() != null) {
      sql.append("status = CAST(? AS security.site_status), ");
      parameters.add(branch.getStatus().name());
    }

    if (parameters.isEmpty()) {
      return getBranchById(id).get();
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
    return getBranchById(id).get();
  }

  @Override
  public void deleteBranch(String branchId) {
    String sql = "DELETE FROM security.branch WHERE id = ?";
    UUID id = UUID.fromString(branchId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private BranchResponse mapRowToBranch(ResultSet rs) throws SQLException {
    return new BranchResponse(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("agence_id"),
        rs.getString("name"),
        rs.getString("code"),
        rs.getString("address"),
        rs.getString("city"),
        rs.getString("phone"),
        rs.getString("email"),
        SiteStatus.valueOf(rs.getString("status")),
        rs.getDate("created_at"),
        rs.getDate("updated_at"));
  }
}
