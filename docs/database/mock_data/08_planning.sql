-- 8. PLANNING : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Équipes, plannings, affectations et présences.
--
-- Variables gset exposées aux fichiers suivants :
--   :shift_id             Équipe jour
--   :shift_morning_id, :shift_afternoon_id, :shift_night_id
--   :schedule_id          planning du 15 juin (poste principal)
--   :assignment_id        affectation du 15 juin
-- ============================================================

SET search_path TO security, public;

INSERT INTO shift (
    company_id,
    name,
    start_time,
    end_time,
    duration_hours,
    description
) VALUES (
    :'company_id',
    'Équipe jour',
    '06:00',
    '18:00',
    12,
    'Équipe de jour'
) RETURNING id \gset shift_

INSERT INTO schedule (
    company_id,
    site_id,
    post_id,
    shift_id,
    schedule_date,
    status,
    notes
) VALUES (
    :'company_id',
    :'site_id',
    :'post_id',
    :'shift_id',
    '2024-06-15',
    'CONFIRMED',
    'Planning de démonstration'
) RETURNING id \gset schedule_

INSERT INTO assignment (
    schedule_id,
    employee_id,
    status,
    check_in_at,
    check_out_at,
    notes
) VALUES (
    :'schedule_id',
    :'employee_primary_id',
    'COMPLETED',
    '2024-06-15 06:00:00+03',
    '2024-06-15 17:55:00+03',
    'Affectation principale du premier jour'
) RETURNING id \gset assignment_

INSERT INTO attendance (
    employee_id,
    site_id,
    assignment_id,
    attendance_date,
    check_in_at,
    check_out_at,
    status
) VALUES (
    :'employee_primary_id',
    :'site_id',
    :'assignment_id',
    '2024-06-15',
    '2024-06-15 05:55:00+03',
    '2024-06-15 18:05:00+03',
    'PRESENT'
);

INSERT INTO shift (company_id, name, start_time, end_time, duration_hours, description) VALUES
    (:'company_id', 'Équipe matin',      '06:00', '14:00', 8, 'Équipe du matin'),
    (:'company_id', 'Équipe après-midi', '14:00', '22:00', 8, 'Équipe de l''après-midi'),
    (:'company_id', 'Équipe nuit',       '22:00', '06:00', 8, 'Équipe de nuit');

SELECT
    (SELECT id FROM shift WHERE name = 'Équipe matin'      AND company_id = :'company_id') AS shift_morning_id,
    (SELECT id FROM shift WHERE name = 'Équipe après-midi' AND company_id = :'company_id') AS shift_afternoon_id,
    (SELECT id FROM shift WHERE name = 'Équipe nuit'       AND company_id = :'company_id') AS shift_night_id
\gset

INSERT INTO schedule (company_id, site_id, post_id, shift_id, schedule_date, status, notes) VALUES
    (:'company_id', :'site_id',  :'post_id',  :'shift_id',           '2024-06-16', 'CONFIRMED', 'Planning poste principal'),
    (:'company_id', :'site_id',  :'post_id',  :'shift_id',           '2024-06-17', 'CONFIRMED', 'Planning poste principal'),
    (:'company_id', :'site2_id', :'post2_id', :'shift_id',           '2024-06-16', 'CONFIRMED', 'Planning accueil banque'),
    (:'company_id', :'site2_id', :'post2_id', :'shift_id',           '2024-06-17', 'CONFIRMED', 'Planning accueil banque'),
    (:'company_id', :'site3_id', :'post4_id', :'shift_morning_id',   '2024-06-16', 'CONFIRMED', 'Entrée hôtel - matin'),
    (:'company_id', :'site3_id', :'post4_id', :'shift_morning_id',   '2024-06-17', 'CONFIRMED', 'Entrée hôtel - matin'),
    (:'company_id', :'site3_id', :'post5_id', :'shift_afternoon_id', '2024-06-16', 'CONFIRMED', 'Jardin hôtel - après-midi'),
    (:'company_id', :'site3_id', :'post5_id', :'shift_afternoon_id', '2024-06-17', 'CONFIRMED', 'Jardin hôtel - après-midi'),
    (:'company_id', :'site4_id', :'post6_id', :'shift_id',           '2024-06-16', 'CONFIRMED', 'Planning Mada Market'),
    (:'company_id', :'site4_id', :'post6_id', :'shift_id',           '2024-06-17', 'CONFIRMED', 'Planning Mada Market'),
    (:'company_id', :'site_id',  :'post_id',  :'shift_id',           '2024-06-18', 'CONFIRMED', 'Planning poste principal'),
    (:'company_id', :'site2_id', :'post3_id', :'shift_id',           '2024-06-18', 'CONFIRMED', 'Planning distributeurs'),
    (:'company_id', :'site5_id', :'post7_id', :'shift_id',           '2024-06-18', 'CONFIRMED', 'Gardien entrepôt'),
    (:'company_id', :'site4_id', :'post6_id', :'shift_morning_id',   '2024-06-18', 'CONFIRMED', 'Caisses - équipe du matin'),
    (:'company_id', :'site3_id', :'post4_id', :'shift_morning_id',   '2024-06-18', 'CANCELLED', 'Annulé - absentéisme non planifié'),
    (:'company_id', :'site3_id', :'post5_id', :'shift_night_id',     '2024-06-18', 'PLANNED',   'Ronde de nuit à confirmer');

INSERT INTO assignment (schedule_id, employee_id, status, check_in_at, notes)
SELECT s.id, e.id, 'CONFIRMED', v.check_in::timestamptz, v.notes
FROM (VALUES
    ('POST-001', '2024-06-16', 'EMP-0002', '2024-06-16 06:00:00+03', 'Agent portail'),
    ('POST-001', '2024-06-16', 'EMP-0008', '2024-06-16 06:00:00+03', 'Agent portail'),
    ('POST-001', '2024-06-17', 'EMP-0001', '2024-06-17 06:00:00+03', 'Chef de site'),
    ('POST-001', '2024-06-17', 'EMP-0008', '2024-06-17 06:00:00+03', 'Agent portail'),
    ('POST-002', '2024-06-16', 'EMP-0004', '2024-06-16 06:00:00+03', 'Contrôle hall'),
    ('POST-002', '2024-06-16', 'EMP-0001', '2024-06-16 06:00:00+03', 'Contrôle hall'),
    ('POST-002', '2024-06-17', 'EMP-0004', '2024-06-17 06:00:00+03', 'Contrôle hall'),
    ('POST-002', '2024-06-17', 'EMP-0002', '2024-06-17 06:00:00+03', 'Contrôle hall'),
    ('POST-004', '2024-06-16', 'EMP-0006', '2024-06-16 06:00:00+03', 'Entrée hôtel'),
    ('POST-004', '2024-06-17', 'EMP-0007', '2024-06-17 06:00:00+03', 'Entrée hôtel'),
    ('POST-005', '2024-06-16', 'EMP-0007', '2024-06-16 14:00:00+03', 'Jardin et piscine'),
    ('POST-005', '2024-06-17', 'EMP-0006', '2024-06-17 14:00:00+03', 'Jardin et piscine'),
    ('POST-006', '2024-06-16', 'EMP-0010', '2024-06-16 06:00:00+03', 'Caisses'),
    ('POST-006', '2024-06-16', 'EMP-0009', '2024-06-16 06:00:00+03', 'Chef d''équipe'),
    ('POST-006', '2024-06-17', 'EMP-0010', '2024-06-17 06:00:00+03', 'Caisses'),
    ('POST-006', '2024-06-17', 'EMP-0009', '2024-06-17 06:00:00+03', 'Chef d''équipe'),
    ('POST-001', '2024-06-18', 'EMP-0001', '2024-06-18 06:00:00+03', 'Chef de site'),
    ('POST-001', '2024-06-18', 'EMP-0002', '2024-06-18 06:00:00+03', 'Agent portail'),
    ('POST-003', '2024-06-18', 'EMP-0008', '2024-06-18 06:00:00+03', 'Distributeurs'),
    ('POST-007', '2024-06-18', 'EMP-0014', '2024-06-18 06:00:00+03', 'Gardien entrepôt'),
    ('POST-006', '2024-06-18', 'EMP-0010', '2024-06-18 06:00:00+03', 'Caisses - matin')
) AS v(post_code, sdate, emp, check_in, notes)
JOIN security_post p ON p.code = v.post_code
JOIN schedule s      ON s.post_id = p.id AND s.schedule_date = v.sdate::date
JOIN employee e      ON e.employee_number = v.emp;

-- Affectation annulée : aucun pointage, aucune présence générée.
INSERT INTO assignment (schedule_id, employee_id, status, notes)
SELECT s.id, e.id, 'CANCELLED', 'Affectation annulée - arrêt maladie déclaré le 17 juin'
FROM security_post p
JOIN schedule s ON s.post_id = p.id AND s.schedule_date = '2024-06-18'
JOIN employee e ON e.employee_number = 'EMP-0006'
WHERE p.code = 'POST-004';

-- Présences générées à partir des affectations confirmées
-- (du 16 au 18 juin 2024).
INSERT INTO attendance (employee_id, site_id, assignment_id, attendance_date, check_in_at, check_out_at, status)
SELECT
    a.employee_id,
    s.site_id,
    a.id,
    s.schedule_date,
    a.check_in_at - INTERVAL '5 minutes',
    a.check_in_at + (sh.duration_hours * INTERVAL '1 hour') + INTERVAL '5 minutes',
    'PRESENT'
FROM assignment a
JOIN schedule s ON s.id = a.schedule_id
JOIN shift sh   ON sh.id = s.shift_id
WHERE s.schedule_date >= '2024-06-16'
  AND a.status = 'CONFIRMED';

-- Présences particulières (sans affectation).
INSERT INTO attendance (employee_id, site_id, assignment_id, attendance_date, check_in_at, check_out_at, status, notes) VALUES
    (:'emp6_id', :'site3_id', NULL, '2024-06-18', NULL, NULL, 'ABSENT', 'Absence non justifiée le jour de l''annulation'),
    (:'emp5_id', NULL,        NULL, '2024-06-18', '2024-06-18 06:25:00+03', '2024-06-18 14:05:00+03', 'LATE', 'Arrivée en retard de 25 minutes');
