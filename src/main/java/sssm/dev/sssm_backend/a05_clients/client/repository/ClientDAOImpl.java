package sssm.dev.sssm_backend.a05_clients.client.repository;

import java.sql.*;
import java.util.*;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a05_clients.client.entity.Client;

public class ClientDAOImpl implements ClientDAO {
    public List<Client> findAll(int page,int size){return query("SELECT * FROM security.client LIMIT ? OFFSET ?",null,page,size);}
    public List<Client> search(String value,int page,int size){return query("SELECT * FROM security.client WHERE name ILIKE ? OR code ILIKE ? OR legal_name ILIKE ? LIMIT ? OFFSET ?",value,page,size);}
    private List<Client> query(String sql,String value,int page,int size){
        List<Client> out=new ArrayList<>(); int i=1;
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){
            if(value!=null){String p="%"+value+"%";s.setString(i++,p);s.setString(i++,p);s.setString(i++,p);}
            s.setInt(i++,size);s.setInt(i,(page-1)*size);
            try(ResultSet r=s.executeQuery()){while(r.next())out.add(map(r));}
        }catch(SQLException e){e.printStackTrace();}return out;
    }
    public Optional<Client> findById(String id){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT * FROM security.client WHERE id=?")){s.setObject(1,uuid(id));try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}catch(SQLException e){e.printStackTrace();return Optional.empty();}}
    public Client create(Client x){String q="INSERT INTO security.client(company_id,agency_id,code,name,legal_name,client_type,registration_number,tax_number,email,phone,address,city,country,payment_terms_days,active) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(q,Statement.RETURN_GENERATED_KEYS)){values(s,x);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(r.next())x.setId(r.getString(1));}}catch(SQLException e){e.printStackTrace();}return x;}
    public Client update(Client x){String q="UPDATE security.client SET company_id=?,agency_id=?,code=?,name=?,legal_name=?,client_type=?,registration_number=?,tax_number=?,email=?,phone=?,address=?,city=?,country=?,payment_terms_days=?,active=?,updated_at=now() WHERE id=?";try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(q)){values(s,x);s.setObject(16,uuid(x.getId()));s.executeUpdate();}catch(SQLException e){e.printStackTrace();}return x;}
    private void values(PreparedStatement s,Client x)throws SQLException{s.setObject(1,uuid(x.getCompany_id()));s.setObject(2,uuid(x.getAgency_id()));s.setString(3,x.getCode());s.setString(4,x.getName());s.setString(5,x.getLegal_name());s.setString(6,x.getClient_type());s.setString(7,x.getRegistration_number());s.setString(8,x.getTax_number());s.setString(9,x.getEmail());s.setString(10,x.getPhone());s.setString(11,x.getAddress());s.setString(12,x.getCity());s.setString(13,x.getCountry());s.setObject(14,x.getPayment_terms_days());s.setObject(15,x.getActive());}
    public void delete(String id){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("DELETE FROM security.client WHERE id=?")){s.setObject(1,uuid(id));s.executeUpdate();}catch(SQLException e){e.printStackTrace();}}
    private UUID uuid(String value){return value==null?null:UUID.fromString(value);}
    private Client map(ResultSet r)throws SQLException{return new Client(r.getString("id"),r.getString("company_id"),r.getString("agency_id"),r.getString("code"),r.getString("name"),r.getString("legal_name"),r.getString("client_type"),r.getString("registration_number"),r.getString("tax_number"),r.getString("email"),r.getString("phone"),r.getString("address"),r.getString("city"),r.getString("country"),r.getInt("payment_terms_days"),r.getBoolean("active"),r.getTimestamp("created_at"),r.getTimestamp("updated_at"));}
}
