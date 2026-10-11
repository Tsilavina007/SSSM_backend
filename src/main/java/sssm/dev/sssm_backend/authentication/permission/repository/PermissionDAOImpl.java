package sssm.dev.sssm_backend.authentication.permission.repository;

import java.sql.*;
import java.util.*;
import sssm.dev.sssm_backend.authentication.permission.entity.Permission;
import sssm.dev.sssm_backend.infrastructure.database.DatabaseConnection;

public class PermissionDAOImpl implements PermissionDAO {
    public List<Permission> findAll(int page,int size){List<Permission> out=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT * FROM security.permission LIMIT ? OFFSET ?")){s.setInt(1,size);s.setInt(2,(page-1)*size);try(ResultSet r=s.executeQuery()){while(r.next())out.add(map(r));}}catch(SQLException e){e.printStackTrace();}return out;}
    public Optional<Permission> findById(String id){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT * FROM security.permission WHERE id=?")){s.setObject(1,UUID.fromString(id));try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}catch(SQLException e){e.printStackTrace();return Optional.empty();}}
    public Permission create(Permission p){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("INSERT INTO security.permission(code,name,description) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){s.setString(1,p.getCode());s.setString(2,p.getName());s.setString(3,p.getDescription());s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(r.next())p.setId(r.getString(1));}}catch(SQLException e){e.printStackTrace();}return p;}
    public Permission update(Permission p){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("UPDATE security.permission SET code=?,name=?,description=? WHERE id=?")){s.setString(1,p.getCode());s.setString(2,p.getName());s.setString(3,p.getDescription());s.setObject(4,UUID.fromString(p.getId()));s.executeUpdate();}catch(SQLException e){e.printStackTrace();}return p;}
    public void delete(String id){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("DELETE FROM security.permission WHERE id=?")){s.setObject(1,UUID.fromString(id));s.executeUpdate();}catch(SQLException e){e.printStackTrace();}}
    private Permission map(ResultSet r)throws SQLException{return new Permission(r.getString("id"),r.getString("code"),r.getString("name"),r.getString("description"));}
}
