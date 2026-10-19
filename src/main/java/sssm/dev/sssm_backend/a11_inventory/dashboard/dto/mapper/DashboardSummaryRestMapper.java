package sssm.dev.sssm_backend.a11_inventory.dashboard.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.dashboard.dto.DashboardSummaryResponse;
import sssm.dev.sssm_backend.a11_inventory.dashboard.dto.rest.DashboardSummaryRest;

@Component
@RequiredArgsConstructor
public class DashboardSummaryRestMapper {

  public DashboardSummaryRest toRest(DashboardSummaryResponse rs) {
    return (new DashboardSummaryRest(rs.getItems(), rs.getWarehouses(), rs.getOperations()));
  }
}
