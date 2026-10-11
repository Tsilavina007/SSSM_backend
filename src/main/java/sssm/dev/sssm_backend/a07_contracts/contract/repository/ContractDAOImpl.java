package sssm.dev.sssm_backend.a07_contracts.contract.repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a01_common.enums.ContractStatus;
import sssm.dev.sssm_backend.a07_contracts.contract.entity.Contract;

public class ContractDAOImpl implements ContractDAO {
  public List<Contract> findAll(int page, int pageSize) {
    return query(
        "SELECT * FROM security.contract ORDER BY start_date DESC LIMIT ? OFFSET ?",
        null,
        page,
        pageSize);
  }

  public List<Contract> findByCompanyId(String companyId) {
    return queryByUuid(
        "SELECT * FROM security.contract WHERE company_id = ? ORDER BY start_date DESC", companyId);
  }

  public List<Contract> findByClientId(String clientId) {
    return queryByUuid(
        "SELECT * FROM security.contract WHERE client_id = ? ORDER BY start_date DESC", clientId);
  }

  private List<Contract> query(String sql, String ignored, int page, int pageSize) {
    List<Contract> result = new ArrayList<>();
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

  private List<Contract> queryByUuid(String sql, String value) {
    List<Contract> result = new ArrayList<>();
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, uuid(value));
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) result.add(map(rows));
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return result;
  }

  public Optional<Contract> findById(String id) {
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT * FROM security.contract WHERE id = ?")) {
      statement.setObject(1, uuid(id));
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(map(rows)) : Optional.empty();
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
      return Optional.empty();
    }
  }

  public Contract create(Contract contract) {
    String sql =
        "INSERT INTO security.contract(company_id, client_id, contract_number, title, start_date, end_date, status, billing_frequency, currency_code, payment_terms_days, total_amount, notes) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS contract_status), ?, ?, ?, ?, ?)";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      values(statement, contract);
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) contract.setId(keys.getString(1));
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return contract;
  }

  public Contract update(Contract contract) {
    String sql =
        "UPDATE security.contract SET company_id = ?, client_id = ?, contract_number = ?, title = ?, start_date = ?, end_date = ?, status = COALESCE(CAST(? AS contract_status), status), billing_frequency = ?, currency_code = ?, payment_terms_days = ?, total_amount = ?, notes = ? WHERE id = ?";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      values(statement, contract);
      statement.setObject(14, uuid(contract.getId()));
      statement.executeUpdate();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return contract;
  }

  private void values(PreparedStatement statement, Contract contract) throws SQLException {
    statement.setObject(1, uuid(contract.getCompany_id()));
    statement.setObject(2, uuid(contract.getClient_id()));
    statement.setString(3, contract.getContract_number());
    statement.setString(4, contract.getTitle());
    statement.setDate(5, contract.getStart_date());
    statement.setDate(6, contract.getEnd_date());
    statement.setString(
        7,
        contract.getStatus() == null ? ContractStatus.DRAFT.name() : contract.getStatus().name());
    statement.setString(
        8, contract.getBilling_frequency() == null ? "MONTHLY" : contract.getBilling_frequency());
    statement.setString(
        9, contract.getCurrency_code() == null ? "MGA" : contract.getCurrency_code());
    statement.setInt(
        10, contract.getPayment_terms_days() == null ? 30 : contract.getPayment_terms_days());
    statement.setBigDecimal(
        11, contract.getTotal_amount() == null ? BigDecimal.ZERO : contract.getTotal_amount());
    statement.setString(12, contract.getNotes());
  }

  public void delete(String id) {
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("DELETE FROM security.contract WHERE id = ?")) {
      statement.setObject(1, uuid(id));
      statement.executeUpdate();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
  }

  private UUID uuid(String value) {
    return value == null ? null : UUID.fromString(value);
  }

  private Contract map(ResultSet rows) throws SQLException {
    return new Contract(
        rows.getString("id"),
        rows.getString("company_id"),
        rows.getString("client_id"),
        rows.getString("contract_number"),
        rows.getString("title"),
        rows.getDate("start_date"),
        rows.getDate("end_date"),
        ContractStatus.valueOf(rows.getString("status")),
        rows.getString("billing_frequency"),
        rows.getString("currency_code"),
        rows.getInt("payment_terms_days"),
        rows.getBigDecimal("total_amount"),
        rows.getString("notes"),
        rows.getTimestamp("created_at"),
        rows.getTimestamp("updated_at"));
  }
}
