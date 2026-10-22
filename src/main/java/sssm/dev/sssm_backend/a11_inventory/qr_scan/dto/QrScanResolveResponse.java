package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto;

import java.sql.Timestamp;

/**
 * Ligne résolue par un scan, avec les jointures utiles à l'opérateur.
 *
 * <p>Le contenu est volontairement limité aux informations affichables : aucun UUID interne
 * d'archivage, aucune date technique hors contexte.
 */
public class QrScanResolveResponse {

  private String id;
  private String company_id;
  private String qr_code;
  private String description;
  private String status;
  private String type_id;
  private String type_code;
  private String type_name;
  private String category_id;
  private String category_name;
  private String warehouse_id;
  private String warehouse_name;
  private String assignment_id;
  private Timestamp assigned_at;
  private Timestamp expected_return_at;
  private String employee_id;
  private String employee_number;
  private String employee_first_name;
  private String employee_last_name;
  private String post_id;
  private String post_code;
  private String post_name;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCompany_id() {
    return company_id;
  }

  public void setCompany_id(String company_id) {
    this.company_id = company_id;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getType_id() {
    return type_id;
  }

  public void setType_id(String type_id) {
    this.type_id = type_id;
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

  public String getCategory_id() {
    return category_id;
  }

  public void setCategory_id(String category_id) {
    this.category_id = category_id;
  }

  public String getCategory_name() {
    return category_name;
  }

  public void setCategory_name(String category_name) {
    this.category_name = category_name;
  }

  public String getWarehouse_id() {
    return warehouse_id;
  }

  public void setWarehouse_id(String warehouse_id) {
    this.warehouse_id = warehouse_id;
  }

  public String getWarehouse_name() {
    return warehouse_name;
  }

  public void setWarehouse_name(String warehouse_name) {
    this.warehouse_name = warehouse_name;
  }

  public String getAssignment_id() {
    return assignment_id;
  }

  public void setAssignment_id(String assignment_id) {
    this.assignment_id = assignment_id;
  }

  public Timestamp getAssigned_at() {
    return assigned_at;
  }

  public void setAssigned_at(Timestamp assigned_at) {
    this.assigned_at = assigned_at;
  }

  public Timestamp getExpected_return_at() {
    return expected_return_at;
  }

  public void setExpected_return_at(Timestamp expected_return_at) {
    this.expected_return_at = expected_return_at;
  }

  public String getEmployee_id() {
    return employee_id;
  }

  public void setEmployee_id(String employee_id) {
    this.employee_id = employee_id;
  }

  public String getEmployee_number() {
    return employee_number;
  }

  public void setEmployee_number(String employee_number) {
    this.employee_number = employee_number;
  }

  public String getEmployee_first_name() {
    return employee_first_name;
  }

  public void setEmployee_first_name(String employee_first_name) {
    this.employee_first_name = employee_first_name;
  }

  public String getEmployee_last_name() {
    return employee_last_name;
  }

  public void setEmployee_last_name(String employee_last_name) {
    this.employee_last_name = employee_last_name;
  }

  public String getPost_id() {
    return post_id;
  }

  public void setPost_id(String post_id) {
    this.post_id = post_id;
  }

  public String getPost_code() {
    return post_code;
  }

  public void setPost_code(String post_code) {
    this.post_code = post_code;
  }

  public String getPost_name() {
    return post_name;
  }

  public void setPost_name(String post_name) {
    this.post_name = post_name;
  }
}
