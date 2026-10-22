package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.mapper;

import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanResolveResponse;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanAssignmentRest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanCategoryRest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanEmployeeRest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanItemTypeRest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanPostRest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanRest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanWarehouseRest;

/** Convertit une ligne de résolution enrichie en réponse REST. */
@Component
public class QrScanRestMapper {

  public QrScanRest toRest(QrScanResolveResponse response) {
    QrScanRest rest = new QrScanRest();
    rest.setId(response.getId());
    rest.setQr_code(response.getQr_code());
    rest.setDescription(response.getDescription());
    rest.setStatus(status(response.getStatus()));

    if (response.getType_id() != null) {
      rest.setItem_type(
          new QrScanItemTypeRest(
              response.getType_id(), response.getType_code(), response.getType_name()));
    }
    if (response.getCategory_id() != null) {
      rest.setCategory(
          new QrScanCategoryRest(response.getCategory_id(), response.getCategory_name()));
    }
    if (response.getWarehouse_id() != null) {
      rest.setWarehouse(
          new QrScanWarehouseRest(response.getWarehouse_id(), response.getWarehouse_name()));
    }
    if (response.getAssignment_id() != null) {
      QrScanAssignmentRest assignment = new QrScanAssignmentRest();
      assignment.setId(response.getAssignment_id());
      assignment.setAssigned_at(response.getAssigned_at());
      assignment.setExpected_return_at(response.getExpected_return_at());
      if (response.getEmployee_id() != null) {
        assignment.setEmployee(
            new QrScanEmployeeRest(
                response.getEmployee_id(),
                response.getEmployee_number(),
                response.getEmployee_first_name(),
                response.getEmployee_last_name()));
      }
      if (response.getPost_id() != null) {
        assignment.setPost(
            new QrScanPostRest(
                response.getPost_id(), response.getPost_code(), response.getPost_name()));
      }
      rest.setActive_assignment(assignment);
    }
    return rest;
  }

  private static ItemStatus status(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return ItemStatus.valueOf(value);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
