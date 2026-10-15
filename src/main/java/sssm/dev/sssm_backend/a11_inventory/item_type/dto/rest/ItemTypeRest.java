package sssm.dev.sssm_backend.a11_inventory.item_type.dto.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a02_organisation.company.dto.rest.CompanyRest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.rest.ItemCategoryRest;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemTypeRest {
  private String id;
  private CompanyRest company;
  private ItemCategoryRest category;
  private String code;
  private String name;
  private String description;
}
