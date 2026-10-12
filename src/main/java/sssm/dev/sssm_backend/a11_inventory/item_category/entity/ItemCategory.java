package sssm.dev.sssm_backend.a11_inventory.item_category.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCategory {
  private String id;
  private Company company;
  private String name;
  private String description;
}
