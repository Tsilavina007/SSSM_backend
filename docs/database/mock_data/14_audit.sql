-- 14. AUDIT : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Journal d'audit de démonstration (CREATE / UPDATE / DELETE).
-- Traces volontairement fictives, écrites par le script de démo
-- (le trigger applicatif peut ajouter ses propres entrées).
-- ============================================================

SET search_path TO security, public;

INSERT INTO audit_log (
    user_id,
    action,
    entity_name,
    entity_id,
    old_data,
    new_data,
    ip_address,
    user_agent
) VALUES (
    :'user_id',
    'CREATE',
    'company',
    :'company_id',
    NULL,
    '{"name": "SSSM Madagascar"}'::jsonb,
    '127.0.0.1',
    'SSSM mock data script'
);

INSERT INTO audit_log (user_id, action, entity_name, entity_id, old_data, new_data, ip_address, user_agent) VALUES
    (:'user_id',        'CREATE', 'client',   :'client2_id',   NULL,
     '{"code": "CLI-002", "name": "Banque Océan"}'::jsonb,                        '127.0.0.1', 'SSSM mock data script'),
    (:'user_id',        'CREATE', 'contract', :'contract2_id', NULL,
     '{"contract_number": "CTR-2024-002", "status": "ACTIVE"}'::jsonb,            '127.0.0.1', 'SSSM mock data script'),
    (:'user_compta_id', 'CREATE', 'invoice',  :'invoice3_id',  NULL,
     '{"invoice_number": "FAC-2024-003", "total_amount": 3840000}'::jsonb,        '127.0.0.1', 'SSSM mock data script'),
    (:'user_compta_id', 'UPDATE', 'invoice',  :'invoice3_id',
     '{"status": "ISSUED", "amount_paid": 0}'::jsonb,
     '{"status": "PAID", "amount_paid": 3840000}'::jsonb,                         '127.0.0.1', 'SSSM mock data script'),
    (:'user_sup_id',    'UPDATE', 'incident', :'inc4_id',
     '{"status": "OPEN"}'::jsonb,
     '{"status": "RESOLVED"}'::jsonb,                                             '127.0.0.1', 'SSSM mock data script');

-- Traces supplémentaires : suspension de compte, mise en traitement
-- d'un incident et suppression d'un document expiré (document fictif).
INSERT INTO audit_log (user_id, action, entity_name, entity_id, old_data, new_data, ip_address, user_agent) VALUES
    (:'user_id',     'UPDATE', 'app_user', :'user_locked_id',
     '{"username": "soa.locked", "status": "ACTIVE"}'::jsonb,
     '{"username": "soa.locked", "status": "LOCKED"}'::jsonb,                     '127.0.0.1', 'SSSM mock data script'),
    (:'user_sup_id', 'UPDATE', 'incident', :'inc5_id',
     '{"status": "OPEN"}'::jsonb,
     '{"status": "IN_PROGRESS"}'::jsonb,                                          '127.0.0.1', 'SSSM mock data script'),
    (:'user_id',     'DELETE', 'employee_document', NULL,
     '{"employee_number": "EMP-0001", "document_type": "Carte professionnelle", "document_number": "DOC-EXPIRE-DEMO"}'::jsonb,
     NULL,                                                                        '127.0.0.1', 'SSSM mock data script'),
    (:'user_compta_id', 'UPDATE', 'invoice', :'invoice7_id',
     '{"status": "ISSUED"}'::jsonb,
     '{"status": "OVERDUE"}'::jsonb,                                              '127.0.0.1', 'SSSM mock data script');
