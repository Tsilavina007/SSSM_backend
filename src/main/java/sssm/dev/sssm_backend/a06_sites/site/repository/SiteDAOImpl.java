package sssm.dev.sssm_backend.a06_sites.site.repository;

import java.sql.*;
import java.util.*;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a01_common.enums.SiteStatus;
import sssm.dev.sssm_backend.a06_sites.site.entity.Site;

public class SiteDAOImpl implements SiteDAO {
    public List<Site> findAll(int page, int size) {
        return query("SELECT * FROM security.site ORDER BY name LIMIT ? OFFSET ?", null, page, size);
    }

    public List<Site> search(String value, int page, int size) {
        return query("SELECT * FROM security.site WHERE name ILIKE ? OR code ILIKE ? OR city ILIKE ? ORDER BY name LIMIT ? OFFSET ?", value, page, size);
    }

    private List<Site> query(String sql, String value, int page, int size) {
        List<Site> result = new ArrayList<>();
        int index = 1;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (value != null) {
                String pattern = "%" + value + "%";
                statement.setString(index++, pattern);
                statement.setString(index++, pattern);
                statement.setString(index++, pattern);
            }
            statement.setInt(index++, size);
            statement.setInt(index, (page - 1) * size);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(map(rows));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return result;
    }

    public Optional<Site> findById(String id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM security.site WHERE id = ?")) {
            statement.setObject(1, uuid(id));
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }
    }

    public Site create(Site site) {
        String sql = "INSERT INTO security.site(company_id, client_id, branch_id, name, code, address, city, latitude, longitude, status, access_instructions, risk_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS site_status), ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            values(statement, site);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    site.setId(keys.getString(1));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return site;
    }

    public Site update(Site site) {
        String sql = "UPDATE security.site SET company_id = ?, client_id = ?, branch_id = ?, name = ?, code = ?, address = ?, city = ?, latitude = ?, longitude = ?, status = CAST(? AS site_status), access_instructions = ?, risk_level = ?, updated_at = now() WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            values(statement, site);
            statement.setObject(13, uuid(site.getId()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return site;
    }

    private void values(PreparedStatement statement, Site site) throws SQLException {
        statement.setObject(1, uuid(site.getCompany_id()));
        statement.setObject(2, uuid(site.getClient_id()));
        statement.setObject(3, uuid(site.getBranch_id()));
        statement.setString(4, site.getName());
        statement.setString(5, site.getCode());
        statement.setString(6, site.getAddress());
        statement.setString(7, site.getCity());
        statement.setBigDecimal(8, site.getLatitude());
        statement.setBigDecimal(9, site.getLongitude());
        statement.setString(10, site.getStatus() == null ? SiteStatus.ACTIVE.name() : site.getStatus().name());
        statement.setString(11, site.getAccess_instructions());
        statement.setString(12, site.getRisk_level());
    }

    public void delete(String id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM security.site WHERE id = ?")) {
            statement.setObject(1, uuid(id));
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private Site map(ResultSet rows) throws SQLException {
        return new Site(rows.getString("id"), rows.getString("company_id"), rows.getString("client_id"),
                rows.getString("branch_id"), rows.getString("name"), rows.getString("code"),
                rows.getString("address"), rows.getString("city"), rows.getBigDecimal("latitude"),
                rows.getBigDecimal("longitude"), SiteStatus.valueOf(rows.getString("status")),
                rows.getString("access_instructions"), rows.getString("risk_level"),
                rows.getTimestamp("created_at"), rows.getTimestamp("updated_at"));
    }
}
