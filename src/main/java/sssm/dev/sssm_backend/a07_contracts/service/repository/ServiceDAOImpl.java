package sssm.dev.sssm_backend.a07_contracts.service.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a07_contracts.service.entity.Service;

public class ServiceDAOImpl implements ServiceDAO {

  @Override
  public List<Service> findAll(int page, int pageSize) {
    String sql = "SELECT * FROM security.service ORDER BY name LIMIT ? OFFSET ?";
    List<Service> services = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Service service = mapRowToService(rs);
          services.add(service);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return services;
  }

  @Override
  public List<Service> findByCompanyId(String companyId) {
    String sql = "SELECT * FROM security.service WHERE company_id = ? ORDER BY name";
    List<Service> services = new ArrayList<>();
    UUID id = UUID.fromString(companyId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Service service = mapRowToService(rs);
          services.add(service);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return services;
  }

  @Override
  public Optional<Service> findById(String serviceId) {
    String sql = "SELECT * FROM security.service WHERE id = ?";
    UUID id = UUID.fromString(serviceId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToService(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Service create(Service service) {
    String sql =
        "INSERT INTO security.service (company_id, code, name, description, unit, unit_price, vat_rate, active) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(service.getCompany_id()));
      stmt.setString(2, service.getCode());
      stmt.setString(3, service.getName());
      stmt.setString(4, service.getDescription());
      stmt.setString(5, service.getUnit() == null ? "month" : service.getUnit());
      stmt.setBigDecimal(
          6, service.getUnit_price() == null ? java.math.BigDecimal.ZERO : service.getUnit_price());
      stmt.setBigDecimal(
          7, service.getVat_rate() == null ? java.math.BigDecimal.ZERO : service.getVat_rate());
      stmt.setBoolean(8, service.getActive() == null || service.getActive());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          service.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return service;
  }

  @Override
  public Service update(Service service) {
    StringBuilder sql = new StringBuilder("UPDATE security.service SET ");
    List<Object> parameters = new ArrayList<>();

    if (service.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(service.getCompany_id()));
    }
    if (service.getCode() != null) {
      sql.append("code = ?, ");
      parameters.add(service.getCode());
    }
    if (service.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(service.getName());
    }
    if (service.getDescription() != null) {
      sql.append("description = ?, ");
      parameters.add(service.getDescription());
    }
    if (service.getUnit() != null) {
      sql.append("unit = ?, ");
      parameters.add(service.getUnit());
    }
    if (service.getUnit_price() != null) {
      sql.append("unit_price = ?, ");
      parameters.add(service.getUnit_price());
    }
    if (service.getVat_rate() != null) {
      sql.append("vat_rate = ?, ");
      parameters.add(service.getVat_rate());
    }
    if (service.getActive() != null) {
      sql.append("active = ?, ");
      parameters.add(service.getActive());
    }

    if (parameters.isEmpty()) {
      return service;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(service.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return service;
  }

  @Override
  public void delete(String serviceId) {
    String sql = "DELETE FROM security.service WHERE id = ?";
    UUID id = UUID.fromString(serviceId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private Service mapRowToService(ResultSet rs) throws SQLException {
    return new Service(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("code"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getString("unit"),
        rs.getBigDecimal("unit_price"),
        rs.getBigDecimal("vat_rate"),
        rs.getBoolean("active"));
  }
}
