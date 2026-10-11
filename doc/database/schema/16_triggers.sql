-- 16. TRIGGER updated_at
-- ============================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_company_updated_at
BEFORE UPDATE ON company
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_branch_updated_at
BEFORE UPDATE ON branch
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_agency_updated_at
BEFORE UPDATE ON agency
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_updated_at
BEFORE UPDATE ON app_user
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_employee_updated_at
BEFORE UPDATE ON employee
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_client_updated_at
BEFORE UPDATE ON client
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_site_updated_at
BEFORE UPDATE ON site
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_contract_updated_at
BEFORE UPDATE ON contract
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_incident_updated_at
BEFORE UPDATE ON incident
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_item_type_updated_at
BEFORE UPDATE ON item_type
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_item_updated_at
BEFORE UPDATE ON item
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_invoice_updated_at
BEFORE UPDATE ON invoice
FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
