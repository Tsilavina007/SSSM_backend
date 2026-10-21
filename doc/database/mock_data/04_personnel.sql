-- 4. PERSONNEL : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Employés (statuts variés) et documents administratifs.
--
-- Variables gset exposées aux fichiers suivants :
--   :employee_primary_id    EMP-0001 (chef de site, lié à demo.admin)
--   :employee_secondary_id  EMP-0002
--   :emp3_id ... :emp14_id  EMP-0003 à EMP-0014
-- ============================================================

SET search_path TO security, public;

INSERT INTO employee (
    company_id,
    agency_id,
    user_id,
    employee_number,
    first_name,
    last_name,
    gender,
    date_of_birth,
    phone,
    email,
    address,
    national_id,
    hire_date,
    position,
    status,
    emergency_contact_name,
    emergency_contact_phone,
    notes
) VALUES (
    :'company_id',
    :'agency_id',
    :'user_id',
    'EMP-0001',
    'Jean',
    'Rakoto',
    'M',
    '1990-05-15',
    '+261 34 00 000 61',
    'jean.rakoto@sssm.test',
    'Ambohimanarina',
    'NID-0001',
    '2024-01-15',
    'Chef de site',
    'ACTIVE',
    'Marie Rakoto',
    '+261 34 00 000 62',
    'Agent principal de démonstration'
) RETURNING id \gset employee_primary_

INSERT INTO employee (
    company_id,
    agency_id,
    employee_number,
    first_name,
    last_name,
    gender,
    date_of_birth,
    phone,
    email,
    address,
    national_id,
    hire_date,
    position,
    status,
    emergency_contact_name,
    emergency_contact_phone
) VALUES (
    :'company_id',
    :'agency_id',
    'EMP-0002',
    'Hery',
    'Rasoanaivo',
    'M',
    '1995-08-20',
    '+261 34 00 000 63',
    'hery.rasoanaivo@sssm.test',
    'Behoririka',
    'NID-0002',
    '2024-02-01',
    'Agent de sécurité',
    'ACTIVE',
    'Soa Rasoanaivo',
    '+261 34 00 000 64'
) RETURNING id \gset employee_secondary_

INSERT INTO employee (
    company_id, agency_id, user_id, employee_number, first_name, last_name, gender, date_of_birth,
    phone, email, address, national_id, hire_date, position, status,
    emergency_contact_name, emergency_contact_phone
) VALUES
    (:'company_id', :'agency_id',     :'user_sup_id',    'EMP-0003', 'Lalao',    'Randriamanana', 'F', '1988-03-12',
     '+261 34 00 000 65', 'lalao.randriamanana@sssm.test', 'Ankadifotsy', 'NID-0003', '2023-09-01',
     'Superviseur', 'ACTIVE', 'Rado Randriamanana', '+261 34 00 000 66'),
    (:'company_id', :'agency_id',     :'user_agent1_id', 'EMP-0004', 'Tahina',   'Razafindrabe',  'M', '1993-11-02',
     '+261 34 00 000 67', 'tahina.razafindrabe@sssm.test', 'Ankorondrano', 'NID-0004', '2024-02-15',
     'Agent de sécurité', 'ACTIVE', 'Naina Razafindrabe', '+261 34 00 000 68'),
    (:'company_id', :'agency_id',     :'user_compta_id', 'EMP-0005', 'Voahangy', 'Rakotomalala',  'F', '1991-07-25',
     '+261 34 00 000 69', 'voahangy.rakotomalala@sssm.test', 'Isotry', 'NID-0005', '2023-06-01',
     'Comptable', 'ACTIVE', 'Hanta Rakotomalala', '+261 34 00 000 70'),
    (:'company_id', :'agency_toa_id', NULL,              'EMP-0006', 'Mamy',     'Andrianarivo',  'M', '1994-01-18',
     '+261 34 00 000 71', 'mamy.andrianarivo@sssm.test', 'Ambodimanga', 'NID-0006', '2024-03-01',
     'Agent de sécurité', 'ACTIVE', 'Lanto Andrianarivo', '+261 34 00 000 72'),
    (:'company_id', :'agency_toa_id', NULL,              'EMP-0007', 'Njaka',    'Rabemananjara', 'M', '1996-09-09',
     '+261 34 00 000 73', 'njaka.rabemananjara@sssm.test', 'Tanambao II', 'NID-0007', '2024-03-01',
     'Agent de sécurité', 'ACTIVE', 'Vola Rabemananjara', '+261 34 00 000 74'),
    (:'company_id', :'agency_id',     :'user_agent2_id', 'EMP-0008', 'Tsiky',    'Ravelomanana',  'F', '1997-04-30',
     '+261 34 00 000 75', 'tsiky.ravelomanana@sssm.test', 'Andohalo', 'NID-0008', '2024-02-20',
     'Agent de sécurité', 'ACTIVE', 'Zo Ravelomanana', '+261 34 00 000 76'),
    (:'company_id', :'agency_ant_id', NULL,              'EMP-0009', 'Fidy',     'Randrianasolo', 'M', '1989-12-05',
     '+261 34 00 000 77', 'fidy.randrianasolo@sssm.test', 'Ambohimena', 'NID-0009', '2023-11-01',
     'Chef d''équipe', 'ACTIVE', 'Tiana Randrianasolo', '+261 34 00 000 78'),
    (:'company_id', :'agency_ant_id', NULL,              'EMP-0010', 'Miora',    'Rasolofo',      'F', '1998-06-14',
     '+261 34 00 000 79', 'miora.rasolofo@sssm.test', 'Mahazoarivo', 'NID-0010', '2024-04-01',
     'Agent de sécurité', 'ACTIVE', 'Haja Rasolofo', '+261 34 00 000 80');

-- Employés aux statuts représentatifs (suspension, inactivité, départ).
INSERT INTO employee (
    company_id, agency_id, user_id, employee_number, first_name, last_name, gender, date_of_birth,
    phone, email, address, national_id, hire_date, end_date, position, status,
    emergency_contact_name, emergency_contact_phone, notes
) VALUES
    (:'company_id', :'agency_id',     NULL,              'EMP-0011', 'Rado',   'Andriamampianina', 'M', '1992-02-08',
     '+261 34 00 000 81', 'rado.andriamampianina@sssm.test', 'Anjanahary', 'NID-0011', '2023-10-02', NULL,
     'Agent de sécurité', 'SUSPENDED', 'Mamy Andriamampianina', '+261 34 00 000 82',
     'Suspension de 15 jours pour abandons de poste'),
    (:'company_id', :'agency_ant_id', NULL,              'EMP-0012', 'Hanitra','Ranaivoson',       'F', '1990-11-21',
     '+261 34 00 000 83', 'hanitra.ranaivoson@sssm.test', 'Madiorano', 'NID-0012', '2023-05-15', NULL,
     'Agent de sécurité', 'INACTIVE', 'Rivo Ranaivoson', '+261 34 00 000 84',
     'Congé sans solde de longue durée'),
    (:'company_id', :'agency_id',     NULL,              'EMP-0013', 'Faniry', 'Ratsimamanga',     'M', '1987-07-09',
     '+261 34 00 000 85', 'faniry.ratsimamanga@sssm.test', 'Vontovorona', 'NID-0013', '2023-01-10', '2024-05-31',
     'Agent de sécurité', 'LEFT', 'Tojo Ratsimamanga', '+261 34 00 000 86',
     'Démissionnaire - fin de contrat'), 
    (:'company_id', :'agency_id',     :'user_agent3_id', 'EMP-0014', 'Soan',   'Rajaonarivelo',    'F', '1996-12-03',
     '+261 34 00 000 87', 'soan.rajaonarivelo@sssm.test', 'Andravoahangy', 'NID-0014', '2024-05-06', NULL,
     'Agent de sécurité', 'ACTIVE', 'Nirina Rajaonarivelo', '+261 34 00 000 88', NULL);

SELECT
    (SELECT id FROM employee WHERE employee_number = 'EMP-0003') AS emp3_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0004') AS emp4_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0005') AS emp5_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0006') AS emp6_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0007') AS emp7_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0008') AS emp8_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0009') AS emp9_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0010') AS emp10_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0011') AS emp11_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0012') AS emp12_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0013') AS emp13_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0014') AS emp14_id
\gset

INSERT INTO employee_document (employee_id, document_type, document_number, issue_date, expiry_date, file_url, notes) VALUES
    (:'employee_primary_id', 'Carte professionnelle', 'DOC-EMP-0001', '2024-01-10', '2027-01-10',
     'https://example.test/documents/emp-0001.pdf', 'Document de démonstration'),
    (:'employee_secondary_id', 'Carte professionnelle', 'DOC-EMP-0002', '2024-01-25', '2027-01-25',
     'https://example.test/documents/emp-0002.pdf', 'Carte professionnelle agent'),
    (:'emp3_id',  'Carte professionnelle', 'DOC-EMP-0003', '2023-08-20', '2026-08-20',
     'https://example.test/documents/emp-0003.pdf', 'Carte superviseur'),
    (:'emp4_id',  'Carte professionnelle', 'DOC-EMP-0004', '2024-02-10', '2027-02-10',
     'https://example.test/documents/emp-0004.pdf', NULL),
    (:'emp4_id',  'Certificat médical',    'MED-EMP-0004', '2024-02-05', '2025-02-05',
     'https://example.test/documents/med-emp-0004.pdf', 'Aptitude physique validée'),
    (:'emp6_id',  'Carte professionnelle', 'DOC-EMP-0006', '2024-02-25', '2027-02-25',
     'https://example.test/documents/emp-0006.pdf', NULL),
    (:'emp8_id',  'Permis de conduire',    'PERM-EMP-0008', '2022-05-10', '2032-05-10',
     'https://example.test/documents/perm-emp-0008.pdf', 'Catégorie B'),
    (:'emp9_id',  'Carte professionnelle', 'DOC-EMP-0009', '2023-10-20', '2026-10-20',
     'https://example.test/documents/emp-0009.pdf', 'Expire bientôt'),
    (:'emp10_id', 'Certificat médical',    'MED-EMP-0010', '2024-03-20', '2025-03-20',
     'https://example.test/documents/med-emp-0010.pdf', NULL),
    (:'emp11_id', 'Carte professionnelle', 'DOC-EMP-0011', '2023-10-15', '2026-10-15',
     'https://example.test/documents/emp-0011.pdf', 'Carte suspendue avec l''agent'),
    (:'emp12_id', 'Certificat médical',    'MED-EMP-0012', '2023-05-10', '2024-05-10',
     'https://example.test/documents/med-emp-0012.pdf', 'Expiré pendant le congé sans solde'),
    (:'emp13_id', 'Contrat de travail',    'CTR-EMP-0013', '2023-01-10', NULL,
     'https://example.test/documents/ctr-emp-0013.pdf', 'Archivé lors du départ en mai 2024');
