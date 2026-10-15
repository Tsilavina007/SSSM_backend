package sssm.dev.sssm_backend.a11_inventory.item_category.dto.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a02_organisation.company.dto.rest.CompanyRest;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCategoryRest {
  private String id;
  private CompanyRest company;
  private String name;
  private String description;
}
