package sssm.dev.sssm_backend.a11_inventory.dashboard.dto.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryRest {
  private Long items;
  private Long warehouses;
  private Long operations;
}
