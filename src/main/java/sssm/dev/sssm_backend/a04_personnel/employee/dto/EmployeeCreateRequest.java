package sssm.dev.sssm_backend.a04_personnel.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.sql.Date;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.EmployeeStatus;

@Data
public class EmployeeCreateRequest {
  @NotBlank(message = "L'identifiant de l'entreprise est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String company_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String agency_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String user_id;

  @NotBlank(message = "Le matricule de l'employé est obligatoire")
  @Size(max = 80, message = "80 caractères maximum")
  private String employee_number;

  @NotBlank(message = "Le prénom est obligatoire")
  @Size(max = 100, message = "100 caractères maximum")
  private String first_name;

  @NotBlank(message = "Le nom est obligatoire")
  @Size(max = 100, message = "100 caractères maximum")
  private String last_name;

  @Size(max = 30, message = "30 caractères maximum")
  private String gender;

  private Date date_of_birth;

  @Size(max = 50, message = "50 caractères maximum")
  private String phone;

  @Email(message = "L'adresse email est invalide")
  @Size(max = 180, message = "180 caractères maximum")
  private String email;

  private String address;

  @Size(max = 100, message = "100 caractères maximum")
  private String national_id;

  private Date hire_date;
  private Date end_date;

  @Size(max = 100, message = "100 caractères maximum")
  private String position;

  private EmployeeStatus status;
  private String photo_url;

  @Size(max = 180, message = "180 caractères maximum")
  private String emergency_contact_name;

  @Size(max = 50, message = "50 caractères maximum")
  private String emergency_contact_phone;

  private String notes;
}
