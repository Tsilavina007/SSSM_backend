package sssm.dev.sssm_backend.a11_inventory.label.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a11_inventory.label.dto.QrLabelData;

/** Accès aux données nécessaire à l'impression des étiquettes. */
@Repository
public class LabelDAO {

  private static final String LABEL_SQL =
      "SELECT i.id, i.qr_code, i.description, i.created_at, "
          + "t.code AS type_code, t.name AS type_name, "
          + "c.name AS category_name, w.name AS warehouse_name, co.name AS company_name "
          + "FROM security.item i "
          + "LEFT JOIN security.item_type t ON t.id = i.item_type_id "
          + "LEFT JOIN security.item_category c ON c.id = i.category_id "
          + "LEFT JOIN security.warehouse w ON w.id = i.current_warehouse_id "
          + "LEFT JOIN security.company co ON co.id = i.company_id "
          + "WHERE i.id = ? AND i.company_id = ?";

  /** Données d'étiquette d'un article de l'entreprise donnée. */
  public Optional<QrLabelData> findLabel(String itemId, String companyId) {
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(LABEL_SQL)) {
      statement.setObject(1, UUID.fromString(itemId));
      statement.setObject(2, UUID.fromString(companyId));
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
      }
    } catch (SQLException exception) {
      throw new ServerException(exception);
    }
  }

  /** Données d'étiquette de plusieurs articles, dans l'ordre demandé. */
  public List<QrLabelData> findLabels(List<String> itemIds, String companyId) {
    List<QrLabelData> labels = new ArrayList<>();
    for (String itemId : itemIds) {
      findLabel(itemId, companyId).ifPresent(labels::add);
    }
    return labels;
  }

  private static QrLabelData mapRow(ResultSet resultSet) throws SQLException {
    QrLabelData data = new QrLabelData();
    data.setId(resultSet.getString("id"));
    data.setQr_code(resultSet.getString("qr_code"));
    data.setDescription(resultSet.getString("description"));
    data.setCreated_at(resultSet.getTimestamp("created_at"));
    data.setType_code(resultSet.getString("type_code"));
    data.setType_name(resultSet.getString("type_name"));
    data.setCategory_name(resultSet.getString("category_name"));
    data.setWarehouse_name(resultSet.getString("warehouse_name"));
    data.setCompany_name(resultSet.getString("company_name"));
    return data;
  }
}
