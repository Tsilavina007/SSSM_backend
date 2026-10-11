package sssm.dev.sssm_backend.a05_clients.client_contact.dto;

import lombok.Data;

@Data
public class ClientContactUpdateRequest {
    private String first_name;
    private String last_name;
    private String function;
    private String email;
    private String phone;
    private Boolean is_primary;
}
