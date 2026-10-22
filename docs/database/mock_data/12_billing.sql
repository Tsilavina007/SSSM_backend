-- 12. FACTURATION : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Taux de TVA, factures, lignes, paiements et allocations.
--
-- Variables gset exposées aux fichiers suivants :
--   :tax_rate_id     TVA standard
--   :invoice_id      FAC-2024-001 (ISSUED, acompte)
--   :payment_id      PAY-2024-001
--   :invoice2_id..:invoice6_id, :invoice7_id..:invoice9_id
--   :payment2_id, :payment3_id
-- ============================================================

SET search_path TO security, public;

INSERT INTO tax_rate (
    company_id,
    name,
    rate,
    active
) VALUES (
    :'company_id',
    'TVA standard',
    20,
    TRUE
) RETURNING id \gset tax_rate_

INSERT INTO tax_rate (company_id, name, rate, active) VALUES
    (:'company_id', 'TVA réduite', 5, TRUE),
    (:'company_id', 'Exonéré',     0, TRUE);

INSERT INTO invoice (
    company_id,
    client_id,
    contract_id,
    invoice_number,
    issue_date,
    due_date,
    status,
    currency_code,
    subtotal,
    discount_amount,
    tax_amount,
    total_amount,
    amount_paid,
    notes
) VALUES (
    :'company_id',
    :'client_id',
    :'contract_id',
    'FAC-2024-001',
    '2024-06-01',
    '2024-06-30',
    'ISSUED',
    'MGA',
    2500000,
    0,
    500000,
    3000000,
    1000000,
    'Facture de démonstration'
) RETURNING id \gset invoice_

INSERT INTO invoice_line (
    invoice_id,
    service_id,
    description,
    quantity,
    unit_price,
    discount_rate,
    tax_rate,
    line_subtotal,
    line_tax,
    line_total
) VALUES (
    :'invoice_id',
    :'service_id',
    'Sécurité 24 heures - juin 2024',
    1,
    2500000,
    0,
    20,
    2500000,
    500000,
    3000000
);

INSERT INTO payment (
    invoice_id,
    payment_reference,
    payment_date,
    amount,
    method,
    status,
    transaction_reference,
    notes
) VALUES (
    :'invoice_id',
    'PAY-2024-001',
    '2024-06-10',
    1000000,
    'BANK_TRANSFER',
    'CONFIRMED',
    'TRX-DEMO-001',
    'Acompte de démonstration'
) RETURNING id \gset payment_

INSERT INTO payment_allocation (
    payment_id,
    invoice_id,
    amount
) VALUES (
    :'payment_id',
    :'invoice_id',
    1000000
);

INSERT INTO invoice (
    company_id, client_id, contract_id, invoice_number, issue_date, due_date, status, currency_code,
    subtotal, discount_amount, tax_amount, total_amount, amount_paid, notes
) VALUES
    (:'company_id', :'client_id',  :'contract_id',  'FAC-2024-002', '2024-07-01', '2024-07-31', 'ISSUED', 'MGA',
     2500000, 0, 500000, 3000000, 0,       'Facture de juillet 2024'),
    (:'company_id', :'client2_id', :'contract2_id', 'FAC-2024-003', '2024-06-01', '2024-06-16', 'PAID',   'MGA',
     3200000, 0, 640000, 3840000, 3840000, 'Facture de juin 2024 - Banque Océan'),
    (:'company_id', :'client3_id', :'contract3_id', 'FAC-2024-004', '2024-06-01', '2024-07-01', 'PAID',   'MGA',
     2500000, 0, 500000, 3000000, 3000000, 'Facture de juin 2024 - Hôtel Le Baobab'),
    (:'company_id', :'client4_id', :'contract4_id', 'FAC-2024-005', '2024-06-01', '2024-07-16', 'ISSUED', 'MGA',
     2150000, 0, 430000, 2580000, 0,       'Facture de juin 2024 - Mada Market'),
    (:'company_id', :'client_id',  :'contract5_id', 'FAC-2024-006', '2024-07-01', '2024-07-31', 'ISSUED', 'MGA',
     1400000, 0, 280000, 1680000, 0,       'Facture de juillet 2024 - Entrepôt');

SELECT
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-002') AS invoice2_id,
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-003') AS invoice3_id,
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-004') AS invoice4_id,
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-005') AS invoice5_id,
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-006') AS invoice6_id
\gset

INSERT INTO invoice_line (
    invoice_id, service_id, description, quantity, unit_price, discount_rate, tax_rate,
    line_subtotal, line_tax, line_total
) VALUES
    (:'invoice2_id', :'service_id',        'Sécurité 24 heures - juillet 2024',            1, 2500000, 0, 20, 2500000, 500000, 3000000),
    (:'invoice3_id', :'service_acces_id',  'Contrôle d''accès - juin 2024',                1, 1800000, 0, 20, 1800000, 360000, 2160000),
    (:'invoice3_id', :'service_12h_id',    'Sécurité 12 heures - juin 2024',               1, 1400000, 0, 20, 1400000, 280000, 1680000),
    (:'invoice4_id', :'service_id',        'Sécurité 24 heures - juin 2024',               1, 2500000, 0, 20, 2500000, 500000, 3000000),
    (:'invoice5_id', :'service_12h_id',    'Sécurité 12 heures - juin 2024',               1, 1400000, 0, 20, 1400000, 280000, 1680000),
    (:'invoice5_id', :'service_ronde_id',  'Rondes de surveillance - juin 2024 (30 rondes)',30,  25000, 0, 20,  750000, 150000,  900000),
    (:'invoice6_id', :'service_12h_id',    'Sécurité 12 heures - juillet 2024',            1, 1400000, 0, 20, 1400000, 280000, 1680000);

INSERT INTO payment (
    invoice_id, payment_reference, payment_date, amount, method, status, transaction_reference, notes
) VALUES
    (:'invoice3_id', 'PAY-2024-002', '2024-06-12', 3840000, 'BANK_TRANSFER', 'CONFIRMED', 'TRX-DEMO-002', 'Paiement intégral Banque Océan'),
    (:'invoice4_id', 'PAY-2024-003', '2024-06-20', 3000000, 'MOBILE_MONEY',  'CONFIRMED', 'TRX-DEMO-003', 'Paiement intégral Hôtel Le Baobab');

SELECT
    (SELECT id FROM payment WHERE payment_reference = 'PAY-2024-002') AS payment2_id,
    (SELECT id FROM payment WHERE payment_reference = 'PAY-2024-003') AS payment3_id
\gset

INSERT INTO payment_allocation (payment_id, invoice_id, amount) VALUES
    (:'payment2_id', :'invoice3_id', 3840000),
    (:'payment3_id', :'invoice4_id', 3000000);

-- Factures aux statuts restants (OVERDUE, DRAFT, PARTIALLY_PAID).
INSERT INTO invoice (
    company_id, client_id, contract_id, invoice_number, issue_date, due_date, status, currency_code,
    subtotal, discount_amount, tax_amount, total_amount, amount_paid, notes
) VALUES
    (:'company_id', :'client4_id', :'contract4_id', 'FAC-2024-007', '2024-07-01', '2024-07-16', 'OVERDUE', 'MGA',
     2150000, 0, 430000, 2580000, 0, 'Facture de juillet en attente de paiement - relance déclenchée'),
    (:'company_id', :'client3_id', :'contract3_id', 'FAC-2024-008', '2024-08-01', '2024-08-15', 'DRAFT', 'MGA',
     2500000, 0, 500000, 3000000, 0, 'Brouillon - à valider avant émission'),
    (:'company_id', :'client3_id', :'contract3_id', 'FAC-2024-009', '2024-07-01', '2024-07-15', 'PARTIALLY_PAID', 'MGA',
     2500000, 0, 500000, 3000000, 1500000, 'Premier acompte reçu, solde attendu');

SELECT
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-007') AS invoice7_id,
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-008') AS invoice8_id,
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-009') AS invoice9_id
\gset

INSERT INTO invoice_line (
    invoice_id, service_id, description, quantity, unit_price, discount_rate, tax_rate,
    line_subtotal, line_tax, line_total
) VALUES
    (:'invoice7_id', :'service_12h_id',   'Sécurité 12 heures - juillet 2024',               1, 1400000, 0, 20, 1400000, 280000, 1680000),
    (:'invoice7_id', :'service_ronde_id', 'Rondes de surveillance - juillet 2024 (30 rondes)',30, 25000,   0, 20,  750000, 150000,  900000),
    (:'invoice8_id', :'service_id',       'Sécurité 24 heures - août 2024',                  1, 2500000, 0, 20, 2500000, 500000, 3000000),
    (:'invoice9_id', :'service_id',       'Sécurité 24 heures - juillet 2024',               1, 2500000, 0, 20, 2500000, 500000, 3000000);

INSERT INTO payment (
    invoice_id, payment_reference, payment_date, amount, method, status, transaction_reference, notes
) VALUES
    (:'invoice9_id', 'PAY-2024-004', '2024-07-08', 1500000, 'CASH',        'CONFIRMED', 'TRX-DEMO-004', 'Acompte en espèces reçu par la comptabilité'),
    (:'invoice9_id', 'PAY-2024-005', '2024-07-20', 1500000, 'MOBILE_MONEY','PENDING',   NULL,           'Second acompte en attente de confirmation mobile money'),
    (:'invoice7_id', 'PAY-2024-006', '2024-07-10', 1200000, 'CHEQUE',      'FAILED',    NULL,           'Chèque sans provision - reçu rejeté par la banque');

SELECT
    (SELECT id FROM payment WHERE payment_reference = 'PAY-2024-004') AS payment4_id
\gset

INSERT INTO payment_allocation (payment_id, invoice_id, amount) VALUES
    (:'payment4_id', :'invoice9_id', 1500000);
