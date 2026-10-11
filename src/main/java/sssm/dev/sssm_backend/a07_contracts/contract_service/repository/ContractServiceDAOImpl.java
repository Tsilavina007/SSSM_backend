package sssm.dev.sssm_backend.a07_contracts.contract_service.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a07_contracts.contract_service.entity.ContractServiceItem;

public class ContractServiceDAOImpl implements ContractServiceDAO {

  @Override
  public List<ContractServiceItem> findByContractId(String contractId) {
    String sql = "SELECT * FROM security.contract_service WHERE contract_id = ? ORDER BY id";
    List<ContractServiceItem> items = new ArrayList<>();
    UUID id = UUID.fromString(contractId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          ContractServiceItem item = mapRowToContractServiceItem(rs);
          items.add(item);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return items;
  }

  @Override
  public Optional<ContractServiceItem> findById(String itemId) {
    String sql = "SELECT * FROM security.contract_service WHERE id = ?";
    UUID id = UUID.fromString(itemId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToContractServiceItem(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public ContractServiceItem create(ContractServiceItem item) {
    String sql =
        "INSERT INTO security.contract_service (contract_id, service_id, site_id, quantity, unit_price, "
            + "vat_rate, start_date, end_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(item.getContract_id()));
      stmt.setObject(2, UUID.fromString(item.getService_id()));
      stmt.setObject(3, UUID.fromString(item.getSite_id()));
      stmt.setBigDecimal(
          4, item.getQuantity() == null ? java.math.BigDecimal.ONE : item.getQuantity());
      stmt.setBigDecimal(
          5, item.getUnit_price() == null ? java.math.BigDecimal.ZERO : item.getUnit_price());
      stmt.setBigDecimal(
          6, item.getVat_rate() == null ? java.math.BigDecimal.ZERO : item.getVat_rate());
      stmt.setDate(7, item.getStart_date());
      stmt.setDate(8, item.getEnd_date());
      stmt.setString(9, item.getNotes());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          item.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return item;
  }

  @Override
  public ContractServiceItem update(ContractServiceItem item) {
    StringBuilder sql = new StringBuilder("UPDATE security.contract_service SET ");
    List<Object> parameters = new ArrayList<>();

    if (item.getContract_id() != null) {
      sql.append("contract_id = ?, ");
      parameters.add(UUID.fromString(item.getContract_id()));
    }
    if (item.getService_id() != null) {
      sql.append("service_id = ?, ");
      parameters.add(UUID.fromString(item.getService_id()));
    }
    if (item.getSite_id() != null) {
      sql.append("site_id = ?, ");
      parameters.add(UUID.fromString(item.getSite_id()));
    }
    if (item.getQuantity() != null) {
      sql.append("quantity = ?, ");
      parameters.add(item.getQuantity());
    }
    if (item.getUnit_price() != null) {
      sql.append("unit_price = ?, ");
      parameters.add(item.getUnit_price());
    }
    if (item.getVat_rate() != null) {
      sql.append("vat_rate = ?, ");
      parameters.add(item.getVat_rate());
    }
    if (item.getStart_date() != null) {
      sql.append("start_date = ?, ");
      parameters.add(item.getStart_date());
    }
    if (item.getEnd_date() != null) {
      sql.append("end_date = ?, ");
      parameters.add(item.getEnd_date());
    }
    if (item.getNotes() != null) {
      sql.append("notes = ?, ");
      parameters.add(item.getNotes());
    }

    if (parameters.isEmpty()) {
      return item;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(item.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return item;
  }

  @Override
  public void delete(String itemId) {
    String sql = "DELETE FROM security.contract_service WHERE id = ?";
    UUID id = UUID.fromString(itemId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private ContractServiceItem mapRowToContractServiceItem(ResultSet rs) throws SQLException {
    return new ContractServiceItem(
        rs.getString("id"),
        rs.getString("contract_id"),
        rs.getString("service_id"),
        rs.getString("site_id"),
        rs.getBigDecimal("quantity"),
        rs.getBigDecimal("unit_price"),
        rs.getBigDecimal("vat_rate"),
        rs.getDate("start_date"),
        rs.getDate("end_date"),
        rs.getString("notes"));
  }
}
