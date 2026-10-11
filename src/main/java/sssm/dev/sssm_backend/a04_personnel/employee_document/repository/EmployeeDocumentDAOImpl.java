package sssm.dev.sssm_backend.a04_personnel.employee_document.repository;

import java.sql.*;
import java.util.*;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a04_personnel.employee_document.entity.EmployeeDocument;

public class EmployeeDocumentDAOImpl implements EmployeeDocumentDAO {
    public List<EmployeeDocument> findByEmployeeId(String employeeId) {
        List<EmployeeDocument> result=new ArrayList<>();
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT * FROM security.employee_document WHERE employee_id=?")){
            s.setObject(1,uuid(employeeId));try(ResultSet r=s.executeQuery()){while(r.next())result.add(map(r));}
        }catch(SQLException e){e.printStackTrace();}return result;
    }
    public Optional<EmployeeDocument> findById(String id){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT * FROM security.employee_document WHERE id=?")){
            s.setObject(1,uuid(id));try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}
        }catch(SQLException e){e.printStackTrace();return Optional.empty();}
    }
    public EmployeeDocument create(EmployeeDocument d){
        String q="INSERT INTO security.employee_document(employee_id,document_type,document_number,issue_date,expiry_date,file_url,notes) VALUES(?,?,?,?,?,?,?)";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(q,Statement.RETURN_GENERATED_KEYS)){
            setValues(s,d);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(r.next())d.setId(r.getString(1));}
        }catch(SQLException e){e.printStackTrace();}return d;
    }
    public EmployeeDocument update(EmployeeDocument d){
        String q="UPDATE security.employee_document SET document_type=?,document_number=?,issue_date=?,expiry_date=?,file_url=?,notes=? WHERE id=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(q)){
            s.setString(1,d.getDocument_type());s.setString(2,d.getDocument_number());s.setDate(3,d.getIssue_date());s.setDate(4,d.getExpiry_date());s.setString(5,d.getFile_url());s.setString(6,d.getNotes());s.setObject(7,uuid(d.getId()));s.executeUpdate();
        }catch(SQLException e){e.printStackTrace();}return d;
    }
    private void setValues(PreparedStatement s,EmployeeDocument d)throws SQLException{s.setObject(1,uuid(d.getEmployee_id()));s.setString(2,d.getDocument_type());s.setString(3,d.getDocument_number());s.setDate(4,d.getIssue_date());s.setDate(5,d.getExpiry_date());s.setString(6,d.getFile_url());s.setString(7,d.getNotes());}
    public void delete(String id){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("DELETE FROM security.employee_document WHERE id=?")){s.setObject(1,uuid(id));s.executeUpdate();}catch(SQLException e){e.printStackTrace();}}
    private UUID uuid(String value){return UUID.fromString(value);}
    private EmployeeDocument map(ResultSet r)throws SQLException{return new EmployeeDocument(r.getString("id"),r.getString("employee_id"),r.getString("document_type"),r.getString("document_number"),r.getDate("issue_date"),r.getDate("expiry_date"),r.getString("file_url"),r.getString("notes"),r.getTimestamp("created_at"));}
}
