package sssm.dev.sssm_backend.a11_inventory.item_category.entity;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a02_organisation.company.dto.rest.CompanyRest;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCategory {
  private String id;
  private CompanyRest company;
  private String name;
  private String description;
  private Timestamp created_at;
  private Timestamp updated_at;
  private Timestamp archived_at;
}
