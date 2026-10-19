package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanItemResponse;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanItemRest;

@Component
@RequiredArgsConstructor
public class QrScanItemRestMapper {

  public QrScanItemRest toRest(QrScanItemResponse rs) {
    return (new QrScanItemRest(
        rs.getId(),
        rs.getCompany_id(),
        rs.getCategory_id(),
        rs.getItem_type_id(),
        rs.getQr_code(),
        rs.getDescription(),
        rs.getStatus(),
        rs.getCurrent_warehouse_id(),
        rs.getCreated_at(),
        rs.getUpdated_at(),
        rs.getArchived_at(),
        rs.getArchived_by(),
        rs.getLast_inventory_at()));
  }
}
