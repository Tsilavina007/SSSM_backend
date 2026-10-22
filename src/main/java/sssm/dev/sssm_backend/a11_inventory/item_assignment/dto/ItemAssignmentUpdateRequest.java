package sssm.dev.sssm_backend.a11_inventory.item_assignment.dto;

import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class ItemAssignmentUpdateRequest {
  // Champs conservés dans le DTO pour produire une erreur explicite : les liens
  // article/employé/poste et la date d'affectation sont immuables.
  @Size(max = 36, message = "36 caractères maximum")
  private String item_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String employee_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String post_id;

  private Timestamp assigned_at;

  // Rétrocompatibilité : un PATCH avec returned_at délègue au même retour que
  // POST /{id}/return. La date et l'acteur sont posés par le serveur.
  private Timestamp returned_at;
  private String condition_on_assignment;
  private String condition_on_return;
  private String notes;
  private Timestamp expected_return_at;
}
