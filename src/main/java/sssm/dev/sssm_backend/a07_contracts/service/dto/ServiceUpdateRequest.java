package sssm.dev.sssm_backend.a07_contracts.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceUpdateRequest extends ServiceCreateRequest {
  public ServiceUpdateRequest() {
    super();
  }
}
