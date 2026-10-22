package sssm.dev.sssm_backend.a11_inventory.item_assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class ItemAssignmentCreateRequest {
  @NotBlank(message = "L'identifiant de l'article est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String item_id;

  @NotBlank(message = "L'identifiant de l'employé est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String employee_id;

  @NotBlank(message = "L'identifiant du poste est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String post_id;

  private Timestamp assigned_at;
  private Timestamp returned_at;
  private String condition_on_assignment;
  private String condition_on_return;
  private String notes;
  private Timestamp expected_return_at;
}
