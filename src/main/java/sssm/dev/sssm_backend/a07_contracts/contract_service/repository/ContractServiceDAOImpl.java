package sssm.dev.sssm_backend.a07_contracts.contract_service.repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a07_contracts.contract_service.entity.ContractServiceItem;

public class ContractServiceDAOImpl implements ContractServiceDAO {
  public List<ContractServiceItem> findByContractId(String contractId) {
    List<ContractServiceItem> result = new ArrayList<>();
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT * FROM security.contract_service WHERE contract_id = ? ORDER BY id")) {
      statement.setObject(1, uuid(contractId));
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) result.add(map(rows));
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return result;
  }

  public Optional<ContractServiceItem> findById(String id) {
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT * FROM security.contract_service WHERE id = ?")) {
      statement.setObject(1, uuid(id));
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(map(rows)) : Optional.empty();
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
      return Optional.empty();
    }
  }

  public ContractServiceItem create(ContractServiceItem item) {
    String sql =
        "INSERT INTO security.contract_service(contract_id, service_id, site_id, quantity, unit_price, vat_rate, start_date, end_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      values(statement, item);
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) item.setId(keys.getString(1));
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return item;
  }

  public ContractServiceItem update(ContractServiceItem item) {
    String sql =
        "UPDATE security.contract_service SET contract_id = ?, service_id = ?, site_id = ?, quantity = ?, unit_price = ?, vat_rate = ?, start_date = ?, end_date = ?, notes = ? WHERE id = ?";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      values(statement, item);
      statement.setObject(10, uuid(item.getId()));
      statement.executeUpdate();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return item;
  }

  private void values(PreparedStatement statement, ContractServiceItem item) throws SQLException {
    statement.setObject(1, uuid(item.getContract_id()));
    statement.setObject(2, uuid(item.getService_id()));
    statement.setObject(3, uuid(item.getSite_id()));
    statement.setBigDecimal(4, item.getQuantity() == null ? BigDecimal.ONE : item.getQuantity());
    statement.setBigDecimal(
        5, item.getUnit_price() == null ? BigDecimal.ZERO : item.getUnit_price());
    statement.setBigDecimal(6, item.getVat_rate() == null ? BigDecimal.ZERO : item.getVat_rate());
    statement.setDate(7, item.getStart_date());
    statement.setDate(8, item.getEnd_date());
    statement.setString(9, item.getNotes());
  }

  public void delete(String id) {
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("DELETE FROM security.contract_service WHERE id = ?")) {
      statement.setObject(1, uuid(id));
      statement.executeUpdate();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
  }

  private UUID uuid(String value) {
    return value == null ? null : UUID.fromString(value);
  }

  private ContractServiceItem map(ResultSet rows) throws SQLException {
    return new ContractServiceItem(
        rows.getString("id"),
        rows.getString("contract_id"),
        rows.getString("service_id"),
        rows.getString("site_id"),
        rows.getBigDecimal("quantity"),
        rows.getBigDecimal("unit_price"),
        rows.getBigDecimal("vat_rate"),
        rows.getDate("start_date"),
        rows.getDate("end_date"),
        rows.getString("notes"));
  }
}
