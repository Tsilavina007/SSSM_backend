package sssm.dev.sssm_backend.a07_contracts.service.repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a07_contracts.service.entity.Service;

public class ServiceDAOImpl implements ServiceDAO {
  public List<Service> findAll(int page, int pageSize) {
    List<Service> result = new ArrayList<>();
    String sql = "SELECT * FROM security.service ORDER BY name LIMIT ? OFFSET ?";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, pageSize);
      statement.setInt(2, (page - 1) * pageSize);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) result.add(map(rows));
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return result;
  }

  public List<Service> findByCompanyId(String companyId) {
    List<Service> result = new ArrayList<>();
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT * FROM security.service WHERE company_id = ? ORDER BY name")) {
      statement.setObject(1, uuid(companyId));
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) result.add(map(rows));
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return result;
  }

  public Optional<Service> findById(String id) {
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT * FROM security.service WHERE id = ?")) {
      statement.setObject(1, uuid(id));
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(map(rows)) : Optional.empty();
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
      return Optional.empty();
    }
  }

  public Service create(Service service) {
    String sql =
        "INSERT INTO security.service(company_id, code, name, description, unit, unit_price, vat_rate, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      values(statement, service);
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) service.setId(keys.getString(1));
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return service;
  }

  public Service update(Service service) {
    String sql =
        "UPDATE security.service SET company_id = ?, code = ?, name = ?, description = ?, unit = ?, unit_price = ?, vat_rate = ?, active = ? WHERE id = ?";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      values(statement, service);
      statement.setObject(9, uuid(service.getId()));
      statement.executeUpdate();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return service;
  }

  private void values(PreparedStatement statement, Service service) throws SQLException {
    statement.setObject(1, uuid(service.getCompany_id()));
    statement.setString(2, service.getCode());
    statement.setString(3, service.getName());
    statement.setString(4, service.getDescription());
    statement.setString(5, service.getUnit() == null ? "month" : service.getUnit());
    statement.setBigDecimal(
        6, service.getUnit_price() == null ? BigDecimal.ZERO : service.getUnit_price());
    statement.setBigDecimal(
        7, service.getVat_rate() == null ? BigDecimal.ZERO : service.getVat_rate());
    statement.setBoolean(8, service.getActive() == null || service.getActive());
  }

  public void delete(String id) {
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("DELETE FROM security.service WHERE id = ?")) {
      statement.setObject(1, uuid(id));
      statement.executeUpdate();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
  }

  private UUID uuid(String value) {
    return value == null ? null : UUID.fromString(value);
  }

  private Service map(ResultSet rows) throws SQLException {
    return new Service(
        rows.getString("id"),
        rows.getString("company_id"),
        rows.getString("code"),
        rows.getString("name"),
        rows.getString("description"),
        rows.getString("unit"),
        rows.getBigDecimal("unit_price"),
        rows.getBigDecimal("vat_rate"),
        rows.getBoolean("active"));
  }
}
