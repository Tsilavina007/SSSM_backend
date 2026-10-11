package sssm.dev.sssm_backend.authentication.user.repository;

import java.sql.*;
import java.util.*;
import sssm.dev.sssm_backend.authentication.user.entity.AppUser;
import sssm.dev.sssm_backend.infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.common.enums.UserStatus;

public class AppUserDAOImpl implements AppUserDAO {
    public List<AppUser> findAll(int page, int size) {
        List<AppUser> result = new ArrayList<>();
        String sql = "SELECT * FROM security.app_user LIMIT ? OFFSET ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, size); s.setInt(2, (page - 1) * size);
            try (ResultSet r = s.executeQuery()) { while (r.next()) result.add(map(r)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }
    public Optional<AppUser> findById(String id) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT * FROM security.app_user WHERE id=?")) {
            s.setObject(1, UUID.fromString(id));
            try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(map(r)) : Optional.empty(); }
        } catch (SQLException e) { e.printStackTrace(); return Optional.empty(); }
    }
    public AppUser create(AppUser u) {
        String sql = "INSERT INTO security.app_user (company_id,username,email,password_hash,first_name,last_name,phone,status) VALUES (?,?,?,?,?,?,?,CAST(? AS user_status))";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setObject(1, uuid(u.getCompany_id())); s.setString(2,u.getUsername()); s.setString(3,u.getEmail());
            s.setString(4,u.getPassword_hash()); s.setString(5,u.getFirst_name()); s.setString(6,u.getLast_name());
            s.setString(7,u.getPhone()); s.setString(8,u.getStatus() == null ? null : u.getStatus().name()); s.executeUpdate();
            try (ResultSet r=s.getGeneratedKeys()) { if(r.next()) u.setId(r.getString(1)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return u;
    }
    public AppUser update(AppUser u) {
        String sql = "UPDATE security.app_user SET email=?,password_hash=?,first_name=?,last_name=?,phone=?,status=CAST(? AS user_status),updated_at=now() WHERE id=?";
        try (Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setString(1,u.getEmail()); s.setString(2,u.getPassword_hash()); s.setString(3,u.getFirst_name());
            s.setString(4,u.getLast_name()); s.setString(5,u.getPhone()); s.setString(6,u.getStatus() == null ? null : u.getStatus().name()); s.setObject(7,uuid(u.getId())); s.executeUpdate();
        } catch(SQLException e){e.printStackTrace();} return u;
    }
    public void delete(String id) { execute("DELETE FROM security.app_user WHERE id=?", id); }
    public void assignRole(String userId,String roleId) { executePair("INSERT INTO security.user_role(user_id,role_id) VALUES(?,?) ON CONFLICT DO NOTHING",userId,roleId); }
    public void removeRole(String userId,String roleId) { executePair("DELETE FROM security.user_role WHERE user_id=? AND role_id=?",userId,roleId); }
    private void execute(String sql,String id){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setObject(1,uuid(id));s.executeUpdate();}catch(SQLException e){e.printStackTrace();}}
    private void executePair(String sql,String a,String b){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setObject(1,uuid(a));s.setObject(2,uuid(b));s.executeUpdate();}catch(SQLException e){e.printStackTrace();}}
    private UUID uuid(String value){return value == null ? null : UUID.fromString(value);}
    private AppUser map(ResultSet r)throws SQLException{return new AppUser(r.getString("id"),r.getString("company_id"),r.getString("username"),r.getString("email"),r.getString("password_hash"),r.getString("first_name"),r.getString("last_name"),r.getString("phone"),UserStatus.valueOf(r.getString("status")),r.getTimestamp("last_login_at"),r.getTimestamp("created_at"),r.getTimestamp("updated_at"));}
}
