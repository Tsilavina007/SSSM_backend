package sssm.dev.sssm_backend.personnel.employee.entity;

import java.sql.Date;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    private String id;
    private String company_id;
    private String agency_id;
    private String user_id;
    private String employee_number;
    private String first_name;
    private String last_name;
    private String gender;
    private Date date_of_birth;
    private String phone;
    private String email;
    private String address;
    private String national_id;
    private Date hire_date;
    private Date end_date;
    private String position;
    private String status;
    private String photo_url;
    private String emergency_contact_name;
    private String emergency_contact_phone;
    private String notes;
    private Timestamp created_at;
    private Timestamp updated_at;
}
