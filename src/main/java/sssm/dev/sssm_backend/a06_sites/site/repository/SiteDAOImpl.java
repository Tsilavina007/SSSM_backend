package sssm.dev.sssm_backend.a06_sites.site.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a01_common.enums.SiteStatus;
import sssm.dev.sssm_backend.a06_sites.site.entity.Site;

public class SiteDAOImpl implements SiteDAO {

  @Override
  public List<Site> search(String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.site WHERE name ILIKE ? OR code ILIKE ? OR city ILIKE ? "
            + "ORDER BY name LIMIT ? OFFSET ?";
    List<Site> sites = new ArrayList<>();
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
          Site site = mapRowToSite(rs);
          sites.add(site);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return sites;
  }

  @Override
  public List<Site> findAll(int page, int pageSize) {
    String sql = "SELECT * FROM security.site ORDER BY name LIMIT ? OFFSET ?";
    List<Site> sites = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Site site = mapRowToSite(rs);
          sites.add(site);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return sites;
  }

  @Override
  public Optional<Site> findById(String siteId) {
    String sql = "SELECT * FROM security.site WHERE id = ?";
    UUID id = UUID.fromString(siteId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToSite(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Site create(Site site) {
    String sql =
        "INSERT INTO security.site (company_id, client_id, branch_id, name, code, address, city, "
            + "latitude, longitude, status, access_instructions, risk_level) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS site_status), ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(site.getCompany_id()));
      stmt.setObject(2, site.getClient_id() == null ? null : UUID.fromString(site.getClient_id()));
      stmt.setObject(3, site.getBranch_id() == null ? null : UUID.fromString(site.getBranch_id()));
      stmt.setString(4, site.getName());
      stmt.setString(5, site.getCode());
      stmt.setString(6, site.getAddress());
      stmt.setString(7, site.getCity());
      stmt.setBigDecimal(8, site.getLatitude());
      stmt.setBigDecimal(9, site.getLongitude());
      stmt.setString(10, site.getStatus() == null ? SiteStatus.ACTIVE.name() : site.getStatus().name());
      stmt.setString(11, site.getAccess_instructions());
      stmt.setString(12, site.getRisk_level());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          site.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return site;
  }

  @Override
  public Site update(Site site) {
    StringBuilder sql = new StringBuilder("UPDATE security.site SET ");
    List<Object> parameters = new ArrayList<>();

    if (site.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(site.getCompany_id()));
    }
    if (site.getClient_id() != null) {
      sql.append("client_id = ?, ");
      parameters.add(UUID.fromString(site.getClient_id()));
    }
    if (site.getBranch_id() != null) {
      sql.append("branch_id = ?, ");
      parameters.add(UUID.fromString(site.getBranch_id()));
    }
    if (site.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(site.getName());
    }
    if (site.getCode() != null) {
      sql.append("code = ?, ");
      parameters.add(site.getCode());
    }
    if (site.getAddress() != null) {
      sql.append("address = ?, ");
      parameters.add(site.getAddress());
    }
    if (site.getCity() != null) {
      sql.append("city = ?, ");
      parameters.add(site.getCity());
    }
    if (site.getLatitude() != null) {
      sql.append("latitude = ?, ");
      parameters.add(site.getLatitude());
    }
    if (site.getLongitude() != null) {
      sql.append("longitude = ?, ");
      parameters.add(site.getLongitude());
    }
    if (site.getStatus() != null) {
      sql.append("status = CAST(? AS site_status), ");
      parameters.add(site.getStatus().name());
    }
    if (site.getAccess_instructions() != null) {
      sql.append("access_instructions = ?, ");
      parameters.add(site.getAccess_instructions());
    }
    if (site.getRisk_level() != null) {
      sql.append("risk_level = ?, ");
      parameters.add(site.getRisk_level());
    }

    if (parameters.isEmpty()) {
      return site;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(site.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return site;
  }

  @Override
  public void delete(String siteId) {
    String sql = "DELETE FROM security.site WHERE id = ?";
    UUID id = UUID.fromString(siteId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private Site mapRowToSite(ResultSet rs) throws SQLException {
    return new Site(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("client_id"),
        rs.getString("branch_id"),
        rs.getString("name"),
        rs.getString("code"),
        rs.getString("address"),
        rs.getString("city"),
        rs.getBigDecimal("latitude"),
        rs.getBigDecimal("longitude"),
        SiteStatus.valueOf(rs.getString("status")),
        rs.getString("access_instructions"),
        rs.getString("risk_level"),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at"));
  }
}
