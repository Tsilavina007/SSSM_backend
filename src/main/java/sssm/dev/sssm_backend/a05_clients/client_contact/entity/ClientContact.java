package sssm.dev.sssm_backend.a05_clients.client_contact.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientContact {
  private String id;
  private String client_id;
  private String first_name;
  private String last_name;
  private String function;
  private String email;
  private String phone;
  private Boolean is_primary;
}
