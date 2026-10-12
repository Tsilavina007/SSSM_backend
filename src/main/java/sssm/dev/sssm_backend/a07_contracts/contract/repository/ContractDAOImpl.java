package sssm.dev.sssm_backend.a07_contracts.contract.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a01_common.enums.ContractStatus;
import sssm.dev.sssm_backend.a07_contracts.contract.entity.Contract;

public class ContractDAOImpl implements ContractDAO {

  @Override
  public List<Contract> findAll(int page, int pageSize) {
    String sql = "SELECT * FROM security.contract ORDER BY start_date DESC LIMIT ? OFFSET ?";
    List<Contract> contracts = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Contract contract = mapRowToContract(rs);
          contracts.add(contract);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return contracts;
  }

  @Override
  public List<Contract> findByCompanyId(String companyId) {
    String sql = "SELECT * FROM security.contract WHERE company_id = ? ORDER BY start_date DESC";
    List<Contract> contracts = new ArrayList<>();
    UUID id = UUID.fromString(companyId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Contract contract = mapRowToContract(rs);
          contracts.add(contract);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return contracts;
  }

  @Override
  public List<Contract> findByClientId(String clientId) {
    String sql = "SELECT * FROM security.contract WHERE client_id = ? ORDER BY start_date DESC";
    List<Contract> contracts = new ArrayList<>();
    UUID id = UUID.fromString(clientId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Contract contract = mapRowToContract(rs);
          contracts.add(contract);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return contracts;
  }

  @Override
  public Optional<Contract> findById(String contractId) {
    String sql = "SELECT * FROM security.contract WHERE id = ?";
    UUID id = UUID.fromString(contractId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToContract(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Contract create(Contract contract) {
    String sql =
        "INSERT INTO security.contract (company_id, client_id, contract_number, title, start_date, "
            + "end_date, status, billing_frequency, currency_code, payment_terms_days, total_amount, notes) "
            + "VALUES (?, ?, ?, ?, ?, ?, CAST(? AS security.contract_status), ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(contract.getCompany_id()));
      stmt.setObject(2, UUID.fromString(contract.getClient_id()));
      stmt.setString(3, contract.getContract_number());
      stmt.setString(4, contract.getTitle());
      stmt.setDate(5, contract.getStart_date());
      stmt.setDate(6, contract.getEnd_date());
      stmt.setString(
          7,
          contract.getStatus() == null ? ContractStatus.DRAFT.name() : contract.getStatus().name());
      stmt.setString(
          8, contract.getBilling_frequency() == null ? "MONTHLY" : contract.getBilling_frequency());
      stmt.setString(9, contract.getCurrency_code() == null ? "MGA" : contract.getCurrency_code());
      stmt.setObject(
          10, contract.getPayment_terms_days() == null ? 30 : contract.getPayment_terms_days());
      stmt.setBigDecimal(
          11,
          contract.getTotal_amount() == null
              ? java.math.BigDecimal.ZERO
              : contract.getTotal_amount());
      stmt.setString(12, contract.getNotes());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          contract.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return contract;
  }

  @Override
  public Contract update(Contract contract) {
    StringBuilder sql = new StringBuilder("UPDATE security.contract SET ");
    List<Object> parameters = new ArrayList<>();

    if (contract.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(contract.getCompany_id()));
    }
    if (contract.getClient_id() != null) {
      sql.append("client_id = ?, ");
      parameters.add(UUID.fromString(contract.getClient_id()));
    }
    if (contract.getContract_number() != null) {
      sql.append("contract_number = ?, ");
      parameters.add(contract.getContract_number());
    }
    if (contract.getTitle() != null) {
      sql.append("title = ?, ");
      parameters.add(contract.getTitle());
    }
    if (contract.getStart_date() != null) {
      sql.append("start_date = ?, ");
      parameters.add(contract.getStart_date());
    }
    if (contract.getEnd_date() != null) {
      sql.append("end_date = ?, ");
      parameters.add(contract.getEnd_date());
    }
    if (contract.getStatus() != null) {
      sql.append("status = CAST(? AS security.contract_status), ");
      parameters.add(contract.getStatus().name());
    }
    if (contract.getBilling_frequency() != null) {
      sql.append("billing_frequency = ?, ");
      parameters.add(contract.getBilling_frequency());
    }
    if (contract.getCurrency_code() != null) {
      sql.append("currency_code = ?, ");
      parameters.add(contract.getCurrency_code());
    }
    if (contract.getPayment_terms_days() != null) {
      sql.append("payment_terms_days = ?, ");
      parameters.add(contract.getPayment_terms_days());
    }
    if (contract.getTotal_amount() != null) {
      sql.append("total_amount = ?, ");
      parameters.add(contract.getTotal_amount());
    }
    if (contract.getNotes() != null) {
      sql.append("notes = ?, ");
      parameters.add(contract.getNotes());
    }

    if (parameters.isEmpty()) {
      return contract;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(contract.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return contract;
  }

  @Override
  public void delete(String contractId) {
    String sql = "DELETE FROM security.contract WHERE id = ?";
    UUID id = UUID.fromString(contractId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private Contract mapRowToContract(ResultSet rs) throws SQLException {
    return new Contract(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("client_id"),
        rs.getString("contract_number"),
        rs.getString("title"),
        rs.getDate("start_date"),
        rs.getDate("end_date"),
        ContractStatus.valueOf(rs.getString("status")),
        rs.getString("billing_frequency"),
        rs.getString("currency_code"),
        rs.getInt("payment_terms_days"),
        rs.getBigDecimal("total_amount"),
        rs.getString("notes"),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at"));
  }
}
