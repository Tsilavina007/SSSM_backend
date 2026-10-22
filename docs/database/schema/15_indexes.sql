-- 15. INDEX
-- ============================================================

-- Company
CREATE INDEX idx_company_created_at ON company(created_at DESC);

-- Branch
CREATE INDEX idx_branch_company ON branch(company_id);
CREATE INDEX idx_branch_agency ON branch(agence_id);
CREATE INDEX idx_branch_code ON branch(code);
CREATE INDEX idx_branch_status ON branch(status);

-- Agency
CREATE INDEX idx_agency_company ON agency(company_id);
CREATE INDEX idx_agency_code ON agency(code);
CREATE INDEX idx_agency_active ON agency(active);

-- App User
CREATE INDEX idx_user_company ON app_user(company_id);
CREATE INDEX idx_user_status ON app_user(status);
CREATE INDEX idx_user_email ON app_user(email);
CREATE INDEX idx_user_username ON app_user(username);
CREATE INDEX idx_user_last_login ON app_user(last_login_at DESC);

-- Role & Permissions
CREATE INDEX idx_role_company ON role(company_id);
CREATE INDEX idx_permission_code ON permission(code);
CREATE INDEX idx_role_permission_role ON role_permission(role_id);
CREATE INDEX idx_role_permission_permission ON role_permission(permission_id);
CREATE INDEX idx_user_role_user ON user_role(user_id);
CREATE INDEX idx_user_role_role ON user_role(role_id);

-- Employee
CREATE INDEX idx_employee_company ON employee(company_id);
CREATE INDEX idx_employee_agency ON employee(agency_id);
CREATE INDEX idx_employee_user ON employee(user_id);
CREATE INDEX idx_employee_status ON employee(status);
CREATE INDEX idx_employee_number ON employee(employee_number);
CREATE INDEX idx_employee_email ON employee(email);
CREATE INDEX idx_employee_national_id ON employee(national_id);
CREATE INDEX idx_employee_hire_date ON employee(hire_date);

-- Employee Document
CREATE INDEX idx_employee_doc_employee ON employee_document(employee_id);
CREATE INDEX idx_employee_doc_type ON employee_document(document_type);

-- Client
CREATE INDEX idx_client_company ON client(company_id);
CREATE INDEX idx_client_agency ON client(agency_id);
CREATE INDEX idx_client_code ON client(code);
CREATE INDEX idx_client_active ON client(active);
CREATE INDEX idx_client_email ON client(email);

-- Client Contact
CREATE INDEX idx_client_contact_client ON client_contact(client_id);
CREATE INDEX idx_client_contact_primary ON client_contact(is_primary);

-- Site
CREATE INDEX idx_site_company ON site(company_id);
CREATE INDEX idx_site_client ON site(client_id);
CREATE INDEX idx_site_branch ON site(branch_id);
CREATE INDEX idx_site_code ON site(code);
CREATE INDEX idx_site_city ON site(city);
CREATE INDEX idx_site_status ON site(status);
CREATE INDEX idx_site_location ON site(latitude, longitude);

-- Security Post
CREATE INDEX idx_post_site ON security_post(site_id);
CREATE INDEX idx_post_code ON security_post(code);
CREATE INDEX idx_post_status ON security_post(status);

-- Post Requirement
CREATE INDEX idx_post_requirement_post ON post_requirement(post_id);

-- Service
CREATE INDEX idx_service_company ON service(company_id);
CREATE INDEX idx_service_code ON service(code);
CREATE INDEX idx_service_active ON service(active);

-- Contract
CREATE INDEX idx_contract_company ON contract(company_id);
CREATE INDEX idx_contract_client ON contract(client_id);
CREATE INDEX idx_contract_number ON contract(contract_number);
CREATE INDEX idx_contract_status ON contract(status);
CREATE INDEX idx_contract_dates ON contract(start_date, end_date);

-- Contract Service
CREATE INDEX idx_contract_service_contract ON contract_service(contract_id);
CREATE INDEX idx_contract_service_service ON contract_service(service_id);
CREATE INDEX idx_contract_service_site ON contract_service(site_id);

-- Shift
CREATE INDEX idx_shift_company ON shift(company_id);

-- Schedule
CREATE INDEX idx_schedule_company ON schedule(company_id);
CREATE INDEX idx_schedule_site ON schedule(site_id);
CREATE INDEX idx_schedule_post ON schedule(post_id);
CREATE INDEX idx_schedule_shift ON schedule(shift_id);
CREATE INDEX idx_schedule_date ON schedule(schedule_date);
CREATE INDEX idx_schedule_site_date ON schedule(site_id, schedule_date);
CREATE INDEX idx_schedule_status ON schedule(status);

-- Assignment
CREATE INDEX idx_assignment_schedule ON assignment(schedule_id);
CREATE INDEX idx_assignment_employee ON assignment(employee_id);
CREATE INDEX idx_assignment_status ON assignment(status);

-- Attendance
CREATE INDEX idx_attendance_employee ON attendance(employee_id);
CREATE INDEX idx_attendance_site ON attendance(site_id);
CREATE INDEX idx_attendance_assignment ON attendance(assignment_id);
CREATE INDEX idx_attendance_employee_date ON attendance(employee_id, attendance_date);
CREATE INDEX idx_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_attendance_status ON attendance(status);

-- Patrol Route
CREATE INDEX idx_patrol_route_site ON patrol_route(site_id);
CREATE INDEX idx_patrol_route_active ON patrol_route(active);

-- Checkpoint
CREATE INDEX idx_checkpoint_route ON checkpoint(patrol_route_id);
CREATE INDEX idx_checkpoint_code ON checkpoint(code);
CREATE INDEX idx_checkpoint_qr ON checkpoint(qr_code);
CREATE INDEX idx_checkpoint_nfc ON checkpoint(nfc_code);

-- Patrol
CREATE INDEX idx_patrol_route ON patrol(patrol_route_id);
CREATE INDEX idx_patrol_employee ON patrol(employee_id);
CREATE INDEX idx_patrol_site ON patrol(site_id);
CREATE INDEX idx_patrol_status ON patrol(status);
CREATE INDEX idx_patrol_scheduled_at ON patrol(scheduled_at);
CREATE INDEX idx_patrol_started_at ON patrol(started_at DESC);

-- Patrol Checkpoint
CREATE INDEX idx_patrol_checkpoint_patrol ON patrol_checkpoint(patrol_id);
CREATE INDEX idx_patrol_checkpoint_checkpoint ON patrol_checkpoint(checkpoint_id);
CREATE INDEX idx_patrol_checkpoint_scanned_at ON patrol_checkpoint(scanned_at);

-- GPS Tracking
CREATE INDEX idx_gps_employee_time ON gps_tracking(employee_id, recorded_at DESC);
CREATE INDEX idx_gps_patrol_time ON gps_tracking(patrol_id, recorded_at DESC);
CREATE INDEX idx_gps_recorded_at ON gps_tracking(recorded_at DESC);

-- Incident Type
CREATE INDEX idx_incident_type_company ON incident_type(company_id);

-- Incident
CREATE INDEX idx_incident_company ON incident(company_id);
CREATE INDEX idx_incident_site ON incident(site_id);
CREATE INDEX idx_incident_post ON incident(post_id);
CREATE INDEX idx_incident_type ON incident(incident_type_id);
CREATE INDEX idx_incident_reported_by ON incident(reported_by_employee_id);
CREATE INDEX idx_incident_assigned_to ON incident(assigned_to_employee_id);
CREATE INDEX idx_incident_status ON incident(status);
CREATE INDEX idx_incident_severity ON incident(severity);
CREATE INDEX idx_incident_reported_at ON incident(reported_at DESC);
CREATE INDEX idx_incident_occurred_at ON incident(occurred_at DESC);

-- Incident Attachment
CREATE INDEX idx_incident_attachment_incident ON incident_attachment(incident_id);

-- Incident Action
CREATE INDEX idx_incident_action_incident ON incident_action(incident_id);
CREATE INDEX idx_incident_action_employee ON incident_action(employee_id);

-- Item Category
CREATE INDEX idx_item_category_company ON item_category(company_id);

-- Item Type
CREATE INDEX idx_item_type_company_status ON item_type(company_id, status) WHERE archived_at IS NULL;
CREATE INDEX idx_item_type_category ON item_type(category_id);

-- Item
CREATE INDEX idx_item_company_status ON item(company_id, status) WHERE archived_at IS NULL;
CREATE INDEX idx_item_type ON item(item_type_id);
CREATE INDEX idx_item_category ON item(category_id);
CREATE INDEX idx_item_current_warehouse ON item(current_warehouse_id);
CREATE INDEX idx_item_qr_code ON item(qr_code);

-- Warehouse
CREATE INDEX idx_warehouse_company ON warehouse(company_id);
CREATE INDEX idx_warehouse_branch ON warehouse(branch_id);
CREATE INDEX idx_warehouse_company_active ON warehouse(company_id, active) WHERE archived_at IS NULL;

-- Inventory Operation
CREATE UNIQUE INDEX uq_inventory_operation_idempotency ON inventory_operation(company_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_inventory_operation_company_date ON inventory_operation(company_id, created_at DESC);
CREATE INDEX idx_inventory_operation_status ON inventory_operation(company_id, status);
CREATE INDEX idx_inventory_operation_reference ON inventory_operation(company_id, reference);

-- Stock Movement
CREATE INDEX idx_stock_movement_item_date ON stock_movement(item_id, movement_date DESC);
CREATE INDEX idx_stock_movement_warehouse_date ON stock_movement(warehouse_id, movement_date DESC);
CREATE INDEX idx_stock_movement_employee_date ON stock_movement(employee_id, movement_date DESC);
CREATE INDEX idx_stock_movement_operation ON stock_movement(operation_id);
CREATE INDEX idx_stock_movement_reference ON stock_movement(reference);

-- Inventory Operation Item
CREATE INDEX idx_operation_item_item ON inventory_operation_item(item_id);
CREATE INDEX idx_operation_item_operation ON inventory_operation_item(operation_id);

-- Item Assignment
CREATE UNIQUE INDEX uq_item_assignment_active ON item_assignment(item_id) WHERE returned_at IS NULL;
CREATE INDEX idx_item_assignment_employee_active ON item_assignment(employee_id, assigned_at DESC) WHERE returned_at IS NULL;
CREATE INDEX idx_item_assignment_post_active ON item_assignment(post_id) WHERE returned_at IS NULL;
CREATE INDEX idx_item_assignment_item_history ON item_assignment(item_id, assigned_at DESC);

-- Warehouse Stock Config
CREATE INDEX idx_warehouse_stock_config_type ON warehouse_stock_config(item_type_id);

-- Inventory Count
CREATE INDEX idx_inventory_count_warehouse_status ON inventory_count(warehouse_id, status);
CREATE INDEX idx_inventory_count_company_date ON inventory_count(company_id, started_at DESC);

-- Inventory Count Scan
CREATE INDEX idx_inventory_count_scan_count_item ON inventory_count_scan(inventory_count_id, item_id);
CREATE INDEX idx_inventory_count_scan_item ON inventory_count_scan(item_id);
CREATE INDEX idx_inventory_count_scan_inventory_count ON inventory_count_scan(inventory_count_id);

-- Tax Rate
CREATE INDEX idx_tax_rate_company ON tax_rate(company_id);
CREATE INDEX idx_tax_rate_active ON tax_rate(active);

-- Invoice
CREATE INDEX idx_invoice_company ON invoice(company_id);
CREATE INDEX idx_invoice_client ON invoice(client_id);
CREATE INDEX idx_invoice_contract ON invoice(contract_id);
CREATE INDEX idx_invoice_number ON invoice(invoice_number);
CREATE INDEX idx_invoice_status ON invoice(status);
CREATE INDEX idx_invoice_issue_date ON invoice(issue_date DESC);
CREATE INDEX idx_invoice_due_date ON invoice(due_date);

-- Invoice Line
CREATE INDEX idx_invoice_line_invoice ON invoice_line(invoice_id);
CREATE INDEX idx_invoice_line_service ON invoice_line(service_id);

-- Payment
CREATE INDEX idx_payment_invoice ON payment(invoice_id);
CREATE INDEX idx_payment_date ON payment(payment_date DESC);
CREATE INDEX idx_payment_status ON payment(status);
CREATE INDEX idx_payment_method ON payment(method);
CREATE INDEX idx_payment_reference ON payment(payment_reference);

-- Payment Allocation
CREATE INDEX idx_payment_alloc_payment ON payment_allocation(payment_id);
CREATE INDEX idx_payment_alloc_invoice ON payment_allocation(invoice_id);

-- Document
CREATE INDEX idx_document_company ON document(company_id);
CREATE INDEX idx_document_client ON document(client_id);
CREATE INDEX idx_document_employee ON document(employee_id);
CREATE INDEX idx_document_contract ON document(contract_id);
CREATE INDEX idx_document_invoice ON document(invoice_id);
CREATE INDEX idx_document_type ON document(document_type);

-- Notification
CREATE INDEX idx_notification_user ON notification(user_id);
CREATE INDEX idx_notification_employee ON notification(employee_id);
CREATE INDEX idx_notification_status ON notification(status);
CREATE INDEX idx_notification_channel ON notification(channel);
CREATE INDEX idx_notification_created_at ON notification(created_at DESC);

-- Email Log
CREATE INDEX idx_email_log_recipient ON email_log(recipient_email);
CREATE INDEX idx_email_log_status ON email_log(status);
CREATE INDEX idx_email_log_sent_at ON email_log(sent_at DESC);

-- Audit Log
CREATE INDEX idx_audit_user ON audit_log(user_id);
CREATE INDEX idx_audit_entity ON audit_log(entity_name, entity_id);
CREATE INDEX idx_audit_created_at ON audit_log(created_at DESC);

-- ============================================================
