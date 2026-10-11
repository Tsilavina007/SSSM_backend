package sssm.dev.sssm_backend.organisation.branch.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sssm.dev.sssm_backend.organisation.branch.entity.Branch;
import sssm.dev.sssm_backend.infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.common.enums.SiteStatus;

public class BranchDAOImpl implements BranchDAO {
    @Override public List<Branch> getAllBranches(int page, int pageSize) {
        return query("SELECT * FROM security.branch LIMIT ? OFFSET ?", null, page, pageSize);
    }
    @Override public List<Branch> searchBranchesByName(String name, int page, int pageSize) {
        return query("SELECT * FROM security.branch WHERE name ILIKE ? OR code ILIKE ? LIMIT ? OFFSET ?", name, page, pageSize);
    }
    private List<Branch> query(String sql, String name, int page, int pageSize) {
        List<Branch> result = new ArrayList<>();
        int i = 1;
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            if (name != null) { s.setString(i++, "%" + name + "%"); s.setString(i++, "%" + name + "%"); }
            s.setInt(i++, pageSize); s.setInt(i, (page - 1) * pageSize);
            try (ResultSet rs = s.executeQuery()) { while (rs.next()) result.add(mapRow(rs)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }
    @Override public Optional<Branch> getBranchById(String id) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT * FROM security.branch WHERE id=?")) {
            s.setObject(1, UUID.fromString(id));
            try (ResultSet rs = s.executeQuery()) { return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty(); }
        } catch (SQLException e) { e.printStackTrace(); return Optional.empty(); }
    }
    @Override public Branch createBranch(Branch b) {
        String sql = "INSERT INTO security.branch (company_id, agence_id, name, code, address, city, phone, email, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS site_status), ?, ?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setValues(s, b); s.executeUpdate();
            try (ResultSet rs = s.getGeneratedKeys()) { if (rs.next()) b.setId(rs.getString(1)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return b;
    }
    private void setValues(PreparedStatement s, Branch b) throws SQLException {
        s.setObject(1, UUID.fromString(b.getCompany_id())); s.setObject(2, UUID.fromString(b.getAgence_id()));
        s.setString(3, b.getName()); s.setString(4, b.getCode()); s.setString(5, b.getAddress());
        s.setString(6, b.getCity()); s.setString(7, b.getPhone()); s.setString(8, b.getEmail());
        s.setString(9, b.getStatus() == null ? null : b.getStatus().name()); s.setDate(10, b.getCreated_at()); s.setDate(11, b.getUpdated_at());
    }
    @Override public Branch updateBranch(Branch b) {
        String sql = "UPDATE security.branch SET company_id=?, agence_id=?, name=?, code=?, address=?, city=?, phone=?, email=?, status=CAST(? AS site_status), created_at=?, updated_at=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            setValues(s, b); s.setObject(12, UUID.fromString(b.getId())); s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        return b;
    }
    @Override public void deleteBranch(String id) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("DELETE FROM security.branch WHERE id=?")) {
            s.setObject(1, UUID.fromString(id)); s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    private Branch mapRow(ResultSet rs) throws SQLException {
        return new Branch(rs.getString("id"), rs.getString("company_id"), rs.getString("agence_id"),
                rs.getString("name"), rs.getString("code"), rs.getString("address"), rs.getString("city"),
                rs.getString("phone"), rs.getString("email"), SiteStatus.valueOf(rs.getString("status")),
                rs.getDate("created_at"), rs.getDate("updated_at"));
    }
}
