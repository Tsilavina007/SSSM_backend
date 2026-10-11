package sssm.dev.sssm_backend.a03_authentication.role.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
  private String id;
  private String company_id;
  private String name;
  private String description;
}
