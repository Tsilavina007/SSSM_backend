# 08 — Audit des affectations et des employés

**Objectif :** vérifier la cohérence des affectations de matériel aux employés, la restitution, les acteurs et la suppression, ainsi que les liens avec les employés et les postes. Le stock et les mouvements sont traités dans les rapports 05 et 07.

**Mise à jour du 22 octobre 2026 (Lot 2) :** la création d'employé sans `status` applique désormais `DEFAULT 'ACTIVE'`, les FK/`CHECK` renvoient 422 et les doublons 409, sans SQLSTATE. Les mentions de « 500 » dans ce rapport décrivent l'état initial ; les incohérences métier d'affectation et de retour restent ouvertes.

---

## 1. Deux modules pour une même table

Le backend expose **deux API distinctes** qui manipulent la même table `security.item_assignment` :

| API | Base | Contrôleur | Service | DAO |
|---|---|---|---|---|
| Affectations typées | `/api/item-assignments` | `ItemAssignmentController` | `ItemAssignmentService` | `ItemAssignmentDAOImpl` |
| Affectations génériques | `/api/inventory/assignments` | `AssignmentController` | `AssignmentService` | `AssignmentDAOImpl` |

Cette duplication est une dette d'architecture majeure. Les deux API n'ont ni les mêmes champs, ni les mêmes validations, ni les mêmes codes HTTP, ni les mêmes effets.

---

## 2. Comparaison des contrats

| Sujet | API typée | API générique |
|---|---|---|
| Création | DTO `ItemAssignmentCreateRequest` | JSON libre |
| Validation | `@NotBlank` sur `item_id`, `employee_id`, `post_id` | aucune |
| Vérification de disponibilité | oui (`AVAILABLE`) | non |
| Création avec `returned_at` | possible, mais l'article est quand même `ASSIGNED` | possible, même effet |
| Champs d'audit | absents du DTO | `assigned_by`, `returned_by` acceptés |
| `expected_return_at` | absent | accepté |
| Retour | `PATCH` avec `returned_at` | `POST /{id}/return` ou `PATCH` |
| Retour sans `returned_at` | aucun effet | aucun effet, 200 |
| Suppression | `DELETE` exposé | service présent, **route absente** |
| Erreur « inconnu » | 404 | 200 avec `errors` |
| Création | 201 | 200 |
| Liste | toutes les affectations | actives uniquement |

Un frontend doit choisir un module. Aucun des deux n'est complet, et ils ne sont pas interchangeables.

---

## 3. Modèle de données

`item_assignment` contient :

- `id` ;
- `item_id` ;
- `employee_id` ;
- `post_id` ;
- `assigned_at` ;
- `returned_at` ;
- `condition_on_assignment` ;
- `condition_on_return` ;
- `notes` ;
- `created_at`, `updated_at` ;
- `assigned_by`, `returned_by` ;
- `expected_return_at`.

Contraintes :

- FK `item_id` → `item(id)` RESTRICT ;
- FK `employee_id` → `employee(id)` RESTRICT ;
- FK `post_id` → `security_post(id)` RESTRICT ;
- `returned_at IS NULL OR returned_at >= assigned_at` ;
- index partiel unique `uq_item_assignment_active(item_id) WHERE returned_at IS NULL`.

### 3.1. Ce que l'index garantit

Un article ne peut avoir qu'une affectation active à la fois, au sens de `returned_at IS NULL`.

### 3.2. Ce que l'index ne garantit pas

- que l'article est `ASSIGNED` ;
- que l'employé est actif ;
- que l'article, l'employé et le poste appartiennent à la même entreprise ;
- que le poste correspond à l'employé ;
- que `assigned_by` ou `returned_by` sont renseignés ;
- que `returned_at` est cohérent avec le statut de l'article.

La table n'a pas de `company_id`. La cohérence de tenant ne peut être vérifiée qu'en joignant `item`, `employee` et `security_post`.

---

## 4. Création typée

### 4.1. Flux

`ItemAssignmentService.createItemAssignment` :

1. charge l'article ;
2. vérifie qu'il existe ;
3. vérifie `status == AVAILABLE` ;
4. insère l'affectation ;
5. passe l'article à `ASSIGNED` ;
6. vide `current_warehouse_id`.

### 4.2. Ce qui est contrôlé

- existence de l'article ;
- statut `AVAILABLE`.

### 4.3. Ce qui n'est pas contrôlé

- existence de l'employé (la FK le fera, mais en 500) ;
- statut actif de l'employé ;
- appartenance de l'employé à la même entreprise ;
- existence du poste (FK, 500) ;
- appartenance du poste à la même entreprise ;
- cohérence entre l'employé et le poste ;
- date de retour dans le futur ;
- date d'affectation antérieure à aujourd'hui.

### 4.4. Création avec `returned_at`

Le DTO accepte `returned_at` à la création. Le service ne le regarde pas : il passe **toujours** l'article à `ASSIGNED` après l'insertion.

Une affectation historique (déjà retournée) peut donc laisser l'article `ASSIGNED`, sans affectation active. C'est une incohérence directe entre la ligne et l'état de l'article.

### 4.5. Date d'affectation

Si `assigned_at` est absent :

- la base applique `now()` ;
- la réponse de création renvoie `assigned_at: null`, car le DAO construit la réponse avec la valeur d'entrée.

Le test `ItemAssignmentTimestampTests.createWithoutAssignedAtAppliesDatabaseDefault` encode ce comportement : il vérifie la date en base **et** attend `null` dans la réponse HTTP.

### 4.6. Concurrence

Le contrôle « article `AVAILABLE` » et l'insertion ne sont pas atomiques. Deux requêtes simultanées peuvent lire le même article `AVAILABLE` :

- la première insère et passe l'article à `ASSIGNED` ;
- la seconde a déjà lu `AVAILABLE` et tente d'insérer ;
- l'index unique rejette la seconde avec `23505` ;
- l'API renvoie **500**, pas 409.

Le cas nominal séquentiel est couvert par `ItemAssignmentStateTests.secondActiveAssignmentIsRejected`, mais il est rejeté par le contrôle de statut (400), pas par l'index. La concurrence n'est pas testée.

---

## 5. Restitution typée

### 5.1. `PATCH` avec `returned_at`

Le service :

1. charge l'affectation existante ;
2. met à jour la ligne ;
3. si `returned_at` est non nul, passe l'article **existant** à `AVAILABLE`.

Il ne :

- restaure pas `current_warehouse_id` ;
- renseigne pas `returned_by` ;
- vérifie pas que `returned_at >= assigned_at` (la base le fait, mais l'erreur devient 500) ;
- vérifie pas que l'affectation était encore active ;
- vérifie pas que l'article n'a pas changé.

### 5.2. Changement d'article

`ItemAssignmentUpdateRequest` accepte `item_id`. Si un `PATCH` change `item_id` **et** fournit `returned_at`, le service libère l'ancien article (`existing.getItem_id()`), pas le nouveau. Le nouvel article reste dans l'état où il était, et l'ancien devient `AVAILABLE` alors que l'affectation pointe désormais vers un autre article.

### 5.3. Absence de date de retour

Un `PATCH` avec seulement `condition_on_return` ne clôt rien : `returned_at` reste `NULL`, l'article reste `ASSIGNED`.

C'est exactement le comportement du test générique `AssignmentApiTests.returnRecordsCondition`, qui ne vérifie ni la date de retour ni le statut de l'article.

---

## 6. Restitution générique

`POST /api/inventory/assignments/{id}/return` appelle simplement `AssignmentService.save(id, body, true)`.

### 6.1. Avec `returned_at`

Si `returned_at` est fourni :

- la ligne est mise à jour ;
- `AssignmentService.save` voit `returned_at != null` ;
- l'article est repassé `AVAILABLE`.

Il ne restaure pas l'entrepôt, ne renseigne pas `returned_by` automatiquement et ne vérifie pas l'état précédent.

### 6.2. Sans `returned_at`

Si le corps ne contient que `condition_on_return` :

- la ligne est mise à jour ;
- l'article reste `ASSIGNED` ;
- l'API renvoie **200** avec l'affectation modifiée.

C'est un **no-op métier** présenté comme un succès. Le test `AssignmentApiTests.returnRecordsCondition` le consacre.

### 6.3. Corps libre

Le DAO accepte les clés JSON qui respectent `[A-Za-z_][A-Za-z0-9_]*`, sauf `id`. Il peut donc modifier :

- `item_id`, `employee_id`, `post_id` ;
- `assigned_at`, `returned_at` ;
- `condition_on_assignment`, `condition_on_return` ;
- `notes`, `assigned_by`, `returned_by`, `expected_return_at`.

Aucune whitelist de colonnes métier n'est appliquée. Un client peut réécrire l'historique ou usurper un acteur.

---

## 7. Suppression

### 7.1. Typée

`DELETE /api/item-assignments/{id}` :

- supprime physiquement la ligne ;
- ne remet pas l'article `AVAILABLE` ;
- ne vérifie pas si l'affectation est active ;
- ne vérifie pas l'existence (204 sur inconnu).

Un article affecté peut donc rester `ASSIGNED` sans aucune ligne d'affectation. L'index unique ne s'applique plus, mais l'état de l'article est faux.

### 7.2. Générique

`AssignmentService.delete` existe, mais aucune route `DELETE` n'est exposée. La suppression n'est pas accessible par l'API générique.

---

## 8. Liens avec les employés et les postes

### 8.1. Employé inactif

Aucun contrôle de `employee.status`. Un employé `INACTIVE`, `SUSPENDED` ou `LEFT` peut recevoir du matériel.

### 8.2. Employé d'une autre entreprise

Aucun contrôle de `employee.company_id` par rapport à `item.company_id`.

### 8.3. Poste d'une autre entreprise

Aucun contrôle de `security_post` ni de son site. Un article peut être affecté à un poste d'une autre entreprise.

### 8.4. Poste incompatible

Aucun contrôle que l'employé occupe réellement le poste, ni que le poste est actif.

### 8.5. Employés et création

`EmployeeDAOImpl.create` insère explicitement `status` même lorsque la valeur est nulle. Le schéma a un `DEFAULT 'ACTIVE'`, mais le `NULL` explicite viole `NOT NULL` et produit **500 `23502`**. Ce n'est pas un problème d'affectation au sens strict, mais il empêche un formulaire minimal de créer un employé.

---

## 9. Conflit avec les mouvements

Le mouvement `OUT` :

- vérifie que l'article est `AVAILABLE` ;
- passe l'article à `ASSIGNED` ;
- vide l'entrepôt ;
- **ne crée aucune affectation**.

Le mouvement `RETURN` :

- passe l'article à `AVAILABLE` ;
- **ne clôt aucune affectation**.

Il existe donc deux sources de vérité pour le statut `ASSIGNED` :

1. `item_assignment` avec `returned_at IS NULL` ;
2. un mouvement `OUT` sans affectation.

Et deux sources pour `AVAILABLE` :

1. une affectation avec `returned_at` ;
2. un mouvement `RETURN` ou `IN`.

Aucune contrainte ne garantit leur cohérence.

---

## 10. Tests existants

| Classe | Tests | Couverture |
|---|---|---|
| `AssignmentApiTests` | 9 | liste active, création, patch notes, retour condition, soft errors, colonne manquante, listes par employé/poste |
| `ItemAssignmentStateTests` | 4 | affectation → `ASSIGNED`, seconde affectation rejetée, retour → `AVAILABLE`, API générique synchronise l'état |
| `ItemAssignmentTimestampTests` | 3 | date par défaut, date explicite, date invalide |

### 10.1. Ce que les tests figent

- `GET` inconnu → 200 avec `errors`.
- `{}` → 200 avec `errors`.
- création sans ids → 500 `23502`.
- retour avec seulement `condition_on_return` → 200, sans clôture.
- `assigned_at` null dans la réponse de création.

Ces tests documentent des comportements douteux comme des attendus.

### 10.2. Ce qui n'est pas couvert

- suppression typée et statut de l'article ;
- création avec `returned_at` ;
- employé inactif ;
- cross-company ;
- changement d'`item_id` ;
- concurrence ;
- `assigned_by` / `returned_by` ;
- restauration de l'entrepôt ;
- cohérence avec `OUT`/`RETURN`.

---

## 11. Verdict

Les affectations sont **partiellement fonctionnelles** sur le parcours nominal :

- article `AVAILABLE` ;
- création typée ;
- article `ASSIGNED` ;
- restitution avec `returned_at` ;
- article `AVAILABLE`.

Elles ne sont pas **fiables** :

- deux API divergentes ;
- retour sans date = no-op ;
- suppression = article bloqué ;
- création avec date de retour = état faux ;
- acteurs non tracés ;
- employés inactifs et cross-company acceptés ;
- conflit avec les mouvements.

Les priorités sont :

1. choisir une seule API d'affectation ;
2. imposer une machine à états unique ;
3. exiger `returned_at` pour un retour ;
4. ne jamais supprimer une affectation active ;
5. contrôler employé, poste et entreprise ;
6. tracer `assigned_by` et `returned_by` depuis le jeton ;
7. aligner `OUT`/`RETURN` avec les affectations.

Les endpoints proposés sont dans le rapport 13 ; le plan dans le rapport 14.
