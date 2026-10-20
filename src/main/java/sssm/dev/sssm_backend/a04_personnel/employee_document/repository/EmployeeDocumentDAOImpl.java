package sssm.dev.sssm_backend.a04_personnel.employee_document.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a04_personnel.employee_document.entity.EmployeeDocument;

@Repository
public class EmployeeDocumentDAOImpl implements EmployeeDocumentDAO {

  @Override
  public List<EmployeeDocument> findByEmployeeId(String employeeId) {
    String sql = "SELECT * FROM security.employee_document WHERE employee_id = ?";
    List<EmployeeDocument> documents = new ArrayList<>();
    UUID id = UUID.fromString(employeeId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          EmployeeDocument document = mapRowToEmployeeDocument(rs);
          documents.add(document);
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return documents;
  }

  @Override
  public Optional<EmployeeDocument> findById(String documentId) {
    String sql = "SELECT * FROM security.employee_document WHERE id = ?";
    UUID id = UUID.fromString(documentId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToEmployeeDocument(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return Optional.empty();
  }

  @Override
  public EmployeeDocument create(EmployeeDocument document) {
    String sql =
        "INSERT INTO security.employee_document (employee_id, document_type, document_number, "
            + "issue_date, expiry_date, file_url, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(document.getEmployee_id()));
      stmt.setString(2, document.getDocument_type());
      stmt.setString(3, document.getDocument_number());
      stmt.setDate(4, document.getIssue_date());
      stmt.setDate(5, document.getExpiry_date());
      stmt.setString(6, document.getFile_url());
      stmt.setString(7, document.getNotes());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          document.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return document;
  }

  @Override
  public EmployeeDocument update(EmployeeDocument document) {
    StringBuilder sql = new StringBuilder("UPDATE security.employee_document SET ");
    List<Object> parameters = new ArrayList<>();

    if (document.getEmployee_id() != null) {
      sql.append("employee_id = ?, ");
      parameters.add(UUID.fromString(document.getEmployee_id()));
    }
    if (document.getDocument_type() != null) {
      sql.append("document_type = ?, ");
      parameters.add(document.getDocument_type());
    }
    if (document.getDocument_number() != null) {
      sql.append("document_number = ?, ");
      parameters.add(document.getDocument_number());
    }
    if (document.getIssue_date() != null) {
      sql.append("issue_date = ?, ");
      parameters.add(document.getIssue_date());
    }
    if (document.getExpiry_date() != null) {
      sql.append("expiry_date = ?, ");
      parameters.add(document.getExpiry_date());
    }
    if (document.getFile_url() != null) {
      sql.append("file_url = ?, ");
      parameters.add(document.getFile_url());
    }
    if (document.getNotes() != null) {
      sql.append("notes = ?, ");
      parameters.add(document.getNotes());
    }

    if (parameters.isEmpty()) {
      return document;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(document.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return document;
  }

  @Override
  public void delete(String documentId) {
    String sql = "DELETE FROM security.employee_document WHERE id = ?";
    UUID id = UUID.fromString(documentId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private EmployeeDocument mapRowToEmployeeDocument(ResultSet rs) throws SQLException {
    return new EmployeeDocument(
        rs.getString("id"),
        rs.getString("employee_id"),
        rs.getString("document_type"),
        rs.getString("document_number"),
        rs.getDate("issue_date"),
        rs.getDate("expiry_date"),
        rs.getString("file_url"),
        rs.getString("notes"),
        rs.getTimestamp("created_at"));
  }
}
