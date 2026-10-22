-- 13. DOCUMENTS / COMMUNICATIONS : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Documents joints, notifications (in-app, e-mail, SMS, push) et
-- journal des e-mails.
--
-- Variables utilisées : celles des fichiers précédents (clients,
-- employés, contrats, factures, inventaire).
-- ============================================================

SET search_path TO security, public;

INSERT INTO document (
    company_id,
    client_id,
    employee_id,
    contract_id,
    invoice_id,
    document_type,
    title,
    file_name,
    file_url,
    mime_type
) VALUES (
    :'company_id',
    :'client_id',
    :'employee_primary_id',
    :'contract_id',
    :'invoice_id',
    'CONTRACT',
    'Contrat signé démo',
    'contrat-2024-001.pdf',
    'https://example.test/documents/contrat-2024-001.pdf',
    'application/pdf'
);

INSERT INTO document (
    company_id, client_id, employee_id, contract_id, invoice_id, document_type, title, file_name, file_url, mime_type
) VALUES
    (:'company_id', :'client2_id', NULL,       :'contract2_id', NULL,            'CONTRACT', 'Contrat Banque Océan',
     'contrat-2024-002.pdf', 'https://example.test/documents/contrat-2024-002.pdf', 'application/pdf'),
    (:'company_id', :'client3_id', NULL,       :'contract3_id', NULL,            'CONTRACT', 'Contrat Hôtel Le Baobab',
     'contrat-2024-003.pdf', 'https://example.test/documents/contrat-2024-003.pdf', 'application/pdf'),
    (:'company_id', :'client4_id', NULL,       :'contract4_id', NULL,            'CONTRACT', 'Contrat Mada Market',
     'contrat-2024-004.pdf', 'https://example.test/documents/contrat-2024-004.pdf', 'application/pdf'),
    (:'company_id', :'client2_id', NULL,       :'contract2_id', :'invoice3_id',  'INVOICE',  'Facture FAC-2024-003',
     'fac-2024-003.pdf',     'https://example.test/documents/fac-2024-003.pdf',     'application/pdf'),
    (:'company_id', :'client3_id', NULL,       :'contract3_id', :'invoice4_id',  'INVOICE',  'Facture FAC-2024-004',
     'fac-2024-004.pdf',     'https://example.test/documents/fac-2024-004.pdf',     'application/pdf'),
    (:'company_id', NULL,          :'emp4_id', NULL,            NULL,            'CONTRACT', 'Contrat de travail Tahina Razafindrabe',
     'cdi-emp-0004.pdf',     'https://example.test/documents/cdi-emp-0004.pdf',     'application/pdf');

-- Documents supplémentaires (rapport, reçu, certificat, autre).
INSERT INTO document (
    company_id, client_id, employee_id, contract_id, invoice_id, document_type, title, file_name, file_url, mime_type
) VALUES
    (:'company_id', :'client2_id', NULL,        :'contract2_id', :'invoice3_id',  'RECEIPT',     'Reçu d''acompte FAC-2024-003',
     'recu-fac-2024-003.pdf',   'https://example.test/documents/recu-fac-2024-003.pdf',   'application/pdf'),
    (:'company_id', :'client2_id', NULL,        NULL,            NULL,            'REPORT',      'Rapport mensuel de surveillance - juin 2024',
     'rapport-juin-2024.pdf',   'https://example.test/documents/rapport-juin-2024.pdf',   'application/pdf'),
    (:'company_id', NULL,          :'emp8_id',  NULL,            NULL,            'CERTIFICATE', 'Attestation de formation aux gestes de secours',
     'formation-emp-0008.pdf',  'https://example.test/documents/formation-emp-0008.pdf',  'application/pdf'),
    (:'company_id', NULL,          :'employee_primary_id', NULL, NULL,            'REPORT',      'Rapport d''incident - tentative d''accès',
     'rapport-inc-001.pdf',     'https://example.test/documents/rapport-inc-001.pdf',     'application/pdf');

INSERT INTO notification (
    user_id,
    employee_id,
    channel,
    status,
    subject,
    message,
    scheduled_at,
    sent_at
) VALUES (
    :'user_id',
    :'employee_primary_id',
    'IN_APP',
    'SENT',
    'Nouvelle affectation',
    'Une nouvelle affectation vous a été attribuée.',
    '2024-06-15 05:30:00+03',
    '2024-06-15 05:30:05+03'
);

INSERT INTO notification (
    user_id, employee_id, channel, status, subject, message, scheduled_at, sent_at
) VALUES
    (:'user_agent1_id', :'emp4_id',  'IN_APP', 'SENT', 'Planning du 17 juin',
     'Vous êtes affecté à l''accueil de la banque le 17 juin à 06h00.',
     '2024-06-16 18:00:00+03', '2024-06-16 18:00:03+03'),
    (:'user_agent2_id', :'emp8_id',  'IN_APP', 'SENT', 'Incident à traiter',
     'Un incident concernant la barrière du parking vous a été assigné.',
     '2024-06-17 09:25:00+03', '2024-06-17 09:25:02+03'),
    (:'user_sup_id',    :'emp3_id',  'IN_APP', 'SENT', 'Incident signalé',
     'Un client agressif a été signalé à l''agence Banque Océan.',
     '2024-06-17 10:36:00+03', '2024-06-17 10:36:01+03'),
    (:'user_compta_id', :'emp5_id',  'EMAIL',  'SENT', 'Factures de juillet',
     'Les factures de juillet 2024 sont prêtes à être validées.',
     '2024-07-01 08:00:00+03', '2024-07-01 08:00:10+03'),
    (:'user_id',        :'employee_primary_id', 'IN_APP', 'SENT', 'Rapport journalier',
     'Le rapport journalier du 16 juin est disponible.',
     '2024-06-16 20:00:00+03', '2024-06-16 20:00:04+03');

-- Notifications aux statuts complémentaires (SMS en attente,
-- push envoyé, e-mail lu, e-mail en échec).
INSERT INTO notification (
    user_id, employee_id, channel, status, subject, message, scheduled_at, sent_at, read_at
) VALUES
    (:'user_id',        :'employee_primary_id', 'EMAIL', 'READ', 'Planning de juin finalisé',
     'Le planning de juin 2024 a été finalisé et publié.',
     '2024-06-01 09:58:00+03', '2024-06-01 09:59:00+03', '2024-06-01 10:00:00+03'),
    (:'user_agent1_id', :'emp4_id',  'SMS',   'PENDING', 'Rappel patrouille de nuit',
     'La ronde de 21h débute dans 30 minutes.',
     '2024-06-17 20:30:00+03', NULL, NULL),
    (:'user_agent2_id', :'emp8_id',  'PUSH',  'SENT', 'Matériel affecté',
     'Une radio vous a été affectée au poste principal.',
     '2024-06-15 06:10:00+03', '2024-06-15 06:10:05+03', NULL),
    (:'user_compta_id', :'emp5_id',  'EMAIL', 'FAILED', 'Relance facture FAC-2024-007',
     'Votre facture présente un solde impayé depuis le 16 juillet.',
     '2024-07-16 09:00:00+03', NULL, NULL);

INSERT INTO email_log (recipient_email, subject, body, sent_at, status) VALUES
    ('demo.admin@sssm.test',           'Bienvenue dans SSSM',        'Votre compte de démonstration est prêt.',                     '2024-01-15 08:00:00+03', 'SENT'),
    ('lalao.randriamanana@sssm.test',  'Bienvenue dans SSSM',        'Votre compte superviseur est prêt.',                          '2023-09-01 08:00:00+03', 'SENT'),
    ('voahangy.rakotomalala@sssm.test','Bienvenue dans SSSM',        'Votre compte comptable est prêt.',                            '2023-06-01 08:00:00+03', 'SENT'),
    ('contact@banque-ocean.test',      'Facture FAC-2024-003',       'Veuillez trouver ci-joint votre facture de juin 2024.',       '2024-06-01 09:00:00+03', 'SENT'),
    ('direction@hotel-baobab.test',    'Facture FAC-2024-004',       'Veuillez trouver ci-joint votre facture de juin 2024.',       '2024-06-01 09:05:00+03', 'SENT'),
    ('achats@mada-market.test',        'Facture FAC-2024-005',       'Veuillez trouver ci-joint votre facture de juin 2024.',       '2024-06-01 09:10:00+03', 'SENT'),
    ('contact@client-demo.test',       'Rapport d''incident',        'Un incident a été constaté sur votre entrepôt le 17 juin.',   '2024-06-17 09:40:00+03', 'SENT');

-- E-mails aux statuts complémentaires (échec, en attente).
INSERT INTO email_log (recipient_email, subject, body, sent_at, status, error_message) VALUES
    ('contact@garage-milasoa.test',    'Facture FAC-2024-007',       'Veuillez régler votre facture en attente.',                   '2024-07-16 09:05:00+03', 'FAILED', 'Adresse non joignable (code SMTP 550)'),
    ('tahina.razafindrabe@sssm.test',  'Planning de juin',           'Votre planning de juin 2024 vient d''être publié.',           NULL,                     'PENDING', NULL);
