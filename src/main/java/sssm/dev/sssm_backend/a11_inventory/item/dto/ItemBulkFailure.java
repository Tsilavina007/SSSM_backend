package sssm.dev.sssm_backend.a11_inventory.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a01_common.errors.ItemError;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemBulkFailure {
  private ItemCreateRequest item;
  private ItemError errors;
}
