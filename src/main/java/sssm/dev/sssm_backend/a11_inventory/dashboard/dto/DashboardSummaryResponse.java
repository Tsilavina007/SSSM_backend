package sssm.dev.sssm_backend.a11_inventory.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
  private Long items;
  private Long warehouses;
  private Long operations;
}
