package sssm.dev.sssm_backend.a11_inventory.item.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemRest;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemBulkCreateResult {
  private List<ItemRest> createdItems;
  private List<ItemBulkFailure> failedItems;
}
