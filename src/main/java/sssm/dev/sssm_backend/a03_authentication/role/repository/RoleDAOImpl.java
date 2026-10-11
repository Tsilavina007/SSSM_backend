package sssm.dev.sssm_backend.a03_authentication.role.repository;

import java.sql.*;
import java.util.*;
import sssm.dev.sssm_backend.a03_authentication.role.entity.Role;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;

public class RoleDAOImpl implements RoleDAO {
    public List<Role> findAll(int page,int size){List<Role> out=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT * FROM security.role LIMIT ? OFFSET ?")){s.setInt(1,size);s.setInt(2,(page-1)*size);try(ResultSet r=s.executeQuery()){while(r.next())out.add(map(r));}}catch(SQLException e){e.printStackTrace();}return out;}
    public Optional<Role> findById(String id){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT * FROM security.role WHERE id=?")){s.setObject(1,uuid(id));try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}catch(SQLException e){e.printStackTrace();return Optional.empty();}}
    public Role create(Role role){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("INSERT INTO security.role(company_id,name,description) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){s.setObject(1,uuid(role.getCompany_id()));s.setString(2,role.getName());s.setString(3,role.getDescription());s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(r.next())role.setId(r.getString(1));}}catch(SQLException e){e.printStackTrace();}return role;}
    public Role update(Role role){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("UPDATE security.role SET company_id=?,name=?,description=? WHERE id=?")){s.setObject(1,uuid(role.getCompany_id()));s.setString(2,role.getName());s.setString(3,role.getDescription());s.setObject(4,uuid(role.getId()));s.executeUpdate();}catch(SQLException e){e.printStackTrace();}return role;}
    public void delete(String id){execute("DELETE FROM security.role WHERE id=?",id);}
    public void assignPermission(String roleId,String permissionId){pair("INSERT INTO security.role_permission(role_id,permission_id) VALUES(?,?) ON CONFLICT DO NOTHING",roleId,permissionId);}
    public void removePermission(String roleId,String permissionId){pair("DELETE FROM security.role_permission WHERE role_id=? AND permission_id=?",roleId,permissionId);}
    private void execute(String q,String id){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(q)){s.setObject(1,uuid(id));s.executeUpdate();}catch(SQLException e){e.printStackTrace();}}
    private void pair(String q,String a,String b){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(q)){s.setObject(1,uuid(a));s.setObject(2,uuid(b));s.executeUpdate();}catch(SQLException e){e.printStackTrace();}}
    private UUID uuid(String v){return v==null?null:UUID.fromString(v);}
    private Role map(ResultSet r)throws SQLException{return new Role(r.getString("id"),r.getString("company_id"),r.getString("name"),r.getString("description"));}
}
