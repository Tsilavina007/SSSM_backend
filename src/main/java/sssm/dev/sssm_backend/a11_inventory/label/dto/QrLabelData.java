package sssm.dev.sssm_backend.a11_inventory.label.dto;

import java.sql.Timestamp;

/** Données d'une étiquette : ce qui est imprimé sous le QR code. */
public class QrLabelData {

  private String id;
  private String qr_code;
  private String description;
  private String type_code;
  private String type_name;
  private String category_name;
  private String warehouse_name;
  private String company_name;
  private Timestamp created_at;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getQr_code() {
    return qr_code;
  }

  public void setQr_code(String qr_code) {
    this.qr_code = qr_code;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType_code() {
    return type_code;
  }

  public void setType_code(String type_code) {
    this.type_code = type_code;
  }

  public String getType_name() {
    return type_name;
  }

  public void setType_name(String type_name) {
    this.type_name = type_name;
  }

  public String getCategory_name() {
    return category_name;
  }

  public void setCategory_name(String category_name) {
    this.category_name = category_name;
  }

  public String getWarehouse_name() {
    return warehouse_name;
  }

  public void setWarehouse_name(String warehouse_name) {
    this.warehouse_name = warehouse_name;
  }

  public String getCompany_name() {
    return company_name;
  }

  public void setCompany_name(String company_name) {
    this.company_name = company_name;
  }

  public Timestamp getCreated_at() {
    return created_at;
  }

  public void setCreated_at(Timestamp created_at) {
    this.created_at = created_at;
  }
}
