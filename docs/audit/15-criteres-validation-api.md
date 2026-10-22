# 15 — Critères de validation de l'API

**Objectif :** fournir une checklist testable pour décider si l'API peut passer d'un prototype à un pilote, puis à la production. Chaque critère décrit un comportement attendu, pas une opinion.

**Portée :** API backend uniquement. Le frontend n'est pas évalué ici, sauf pour les contrats dont il dépend.

---

## 1. Comment utiliser ce document

### 1.1. Statuts

| Statut | Signification |
|---|---|
| **PASS** | Comportement vérifié par le code et/ou le dernier rapport de tests disponible, sans contradiction connue |
| **PARTIEL** | Implémentation partielle, ou vérifiée seulement statiquement, ou insuffisamment testée |
| **FAIL** | Comportement absent, faux, ou contraire au critère |
| **NON VÉRIFIÉ** | Nécessite une exécution, un environnement ou une donnée non disponible pendant l'audit |

**Important :** la suite complète a été exécutée après le Lot 4 : **150 tests, 150 succès, 0 échec, 28 suites**. Un `PASS` signifie « cohérent avec le code et les tests disponibles » ; les critères de tenancy A/B et de concurrence restent non vérifiés. Les statuts ci-dessous intègrent les corrections des Lots 2 à 4 vérifiées par les tests ; le détail historique reste disponible dans les rapports 11 et 16.

### 1.2. Règle de décision

- Un seul **FAIL** sur un critère P0 bloque la production.
- Un **PARTIEL** sur un critère P0 bloque aussi, sauf dérogation écrite et compensée.
- Les critères P1 peuvent être partiels pour un pilote mono-entreprise encadré, avec liste d'écarts acceptés.
- Les critères P2 sont planifiables après stabilisation.

### 1.3. Preuves attendues

Chaque critère doit être prouvé par :

- un test automatisé sur base isolée ;
- ou une vérification manuelle reproductible ;
- ou une contrainte de base démontrée ;
- avec la trace dans le rapport de recette.

Une capture d'écran ne remplace pas un test de contrat.

---

## 2. Portes Go/No-Go

| Porte | Critère | Statut actuel | Preuve / écart |
|---|---|---|---|
| **G0-1** | Un utilisateur de l'entreprise A ne peut lire aucune donnée de B | **FAIL** | Requêtes globales ; aucun filtre `company_id` |
| **G0-2** | Un utilisateur de A ne peut écrire aucune donnée de B | **FAIL** | `company_id` contrôlé par le client ; pas de FK composite |
| **G0-3** | Un compte sans entreprise ou sans rôle ne peut pas accéder à l'inventaire | **PARTIEL** | Un compte sans rôle est bloqué ; un admin plateforme sans entreprise reste autorisé par conception. |
| **G0-4** | Les écritures multi-lignes sont atomiques | **PASS** | Opérations `receipt`/`issue`/`transfer` avec lignes et mouvements atomiques ; rollback testé après échec sur la deuxième ligne (`P1-06`). |
| **G0-5** | Le stock a une source de vérité unique et testée | **PASS** | `stock_balance` alimenté par les mouvements, opérations, articles et clôtures de comptage ; `GET /stock-balances` et dashboard low-stock testés (`P0-05`). Les ajustements de clôture ne sont pas encore journalisés. |
| **G0-6** | `ASSIGNED` implique exactement une affectation active | **PASS** | Création, `OUT`, retour, void et suppression passent par le même service transactionnel ; index unique actif et tests dédiés (`P0-06`, Lot 4). Réserve : une affectation active préexistante de la base de démonstration pointe vers un article `AVAILABLE` ; elle n'est pas créée par le code corrigé. |
| **G0-7** | Aucune erreur de saisie ne produit 500 avec SQLSTATE | **PASS** | Doublons 409, FK/CHECK 422, NOT NULL/format 400 ; aucun SQLSTATE exposé. |
| **G0-8** | Un rôle révoqué ne peut pas être rafraîchi | **PASS** | `/refresh` recharge les rôles et permissions en base ; rotation testée. |

**Verdict :** 5 portes sur 8 en `PASS` (`G0-4`, `G0-5`, `G0-6`, `G0-7`, `G0-8`), 1 `PARTIEL` (`G0-3`), 2 `FAIL` (`G0-1`, `G0-2`). La production reste bloquée par la tenancy ; les affectations sont couvertes par le code mais une donnée de démonstration antérieure reste à réconcilier.

---

## 3. Tenancy

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| T-01 | `GET /items` ne retourne que les articles de l'entreprise du jeton | **FAIL** | `ItemDAOImpl.getAllItems` sans filtre |
| T-02 | `GET /items/{id}` d'une autre entreprise renvoie 404, pas 200 | **FAIL** | Lecture par ID global |
| T-03 | `PATCH /items/{id}` d'une autre entreprise renvoie 404 | **FAIL** | Aucun contrôle de tenant |
| T-04 | `DELETE /items/{id}` d'une autre entreprise renvoie 404 | **FAIL** | Aucun contrôle de tenant |
| T-05 | `POST /items` ignore un `company_id` différent du jeton et utilise celui du jeton | **FAIL** | `company_id` vient du corps |
| T-06 | Un article ne peut pas référencer un type/catégorie/entrepôt d'une autre entreprise | **FAIL** | Aucune FK composite, aucun contrôle |
| T-07 | Une affectation ne peut pas lier un article, un employé et un poste de trois entreprises différentes | **FAIL** | Aucun contrôle |
| T-08 | Un mouvement ne peut pas référencer un entrepôt ou un employé d'une autre entreprise | **FAIL** | FK simple, pas de tenant |
| T-09 | Un scan QR ne renvoie jamais un article d'une autre entreprise | **FAIL** | `QrScanDAOImpl` global |
| T-10 | Un comptage ne peut scanner que des articles de son entreprise et de son périmètre | **PARTIEL** | Le scan vérifie l'entreprise de l'article et refuse un comptage inconnu ; un article d'un autre entrepôt reste scannable et produit un écart `WRONG_LOCATION`/`MISSING`, pas un refus. |
| T-11 | Les listes de types, catégories, entrepôts, mouvements, affectations, comptages et rapports sont tenant-scoped | **FAIL** | Toutes les listes sont globales |
| T-12 | Un administrateur d'entreprise ne peut pas administrer une autre entreprise | **PASS** | Scope `company_id` sur `/api/users` et `/api/roles` ; admin plateforme non filtré. |
| T-13 | La base possède des contraintes composites ou une RLS empêchant le cross-tenant | **FAIL** | Schéma à FK simples |
| T-14 | Un utilisateur anonyme ne peut pas appeler l'inventaire | **PASS** | `SecurityConfig` : `authenticated()` |
| T-15 | Un utilisateur sans entreprise ne peut pas appeler l'inventaire | **PARTIEL** | Un admin plateforme sans entreprise y accède par conception ; les autres comptes sans entreprise n'ont pas de rôle métier. |

### Scénario d'acceptation T-A

```text
Étant donné l'entreprise A et l'entreprise B, chacune avec un article
Et un utilisateur de A authentifié
Quand il appelle GET /api/v1/items
Alors la réponse ne contient que les articles de A
Et GET /api/v1/items/{id_de_B} renvoie 404
Et PATCH /api/v1/items/{id_de_B} renvoie 404
Et POST /api/v1/items avec company_id=B crée l'article dans A ou renvoie 422
```

---

## 4. Authentification et autorisation

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| A-01 | Un login valide renvoie un access token et le profil | **PASS** | Login réel utilisé par `RefreshTokenApiTests` et `AuthHardeningApiTests`. |
| A-02 | Un login invalide renvoie 401, pas 400 | **FAIL** | `AuthService` lève toujours `ClientException` → 400. |
| A-03 | Un compte inactif renvoie 403 | **FAIL** | 400. |
| A-04 | Un compte verrouillé ne peut pas se connecter | **FAIL** | `LOCKED` non appliqué automatiquement. |
| A-05 | L'inscription ne crée pas de compte sans entreprise | **PASS** | Entreprise déduite du jeton ; admin plateforme doit passer par `/api/users`. |
| A-06 | L'inscription exige une invitation ou une validation | **PASS** | Route réservée à `ROLE_ADMIN` ou `users.manage`. |
| A-07 | Un utilisateur sans rôle ne peut pas accéder à l'inventaire | **PASS** | `ROLE_ADMIN`, `ROLE_INVENTORY` ou `inventory.manage` requis. |
| A-08 | Un `USER` ne peut pas appeler `/api/users`, `/roles`, `/permissions` | **PASS** | `users.manage`/`roles.manage` requis ; `/permissions` réservé à `ROLE_ADMIN` ; test admin d'entreprise. |
| A-09 | Un `INVENTORY` peut lire et écrire l'inventaire selon ses permissions | **PARTIEL** | `ROLE_INVENTORY` ou `inventory.manage` autorise l'ensemble de l'inventaire ; pas de permission par action. |
| A-10 | Un `ADMIN` d'entreprise ne peut pas gérer une autre entreprise | **PASS** | Scope `company_id` sur les utilisateurs et rôles. |
| A-11 | Le refresh recharge les rôles depuis la base | **PASS** | `AuthService.refresh` recharge rôles et permissions. |
| A-12 | Un rôle révoqué ne peut pas être conservé par refresh | **PASS** | Le nouveau jeton reprend les rôles rechargés ; pas de test dédié de révocation de rôle. |
| A-13 | Le refresh token est distinct, tourne et est révocable | **PASS** | Table `refresh_token`, consommation atomique de l'ancien token, tests. |
| A-14 | Le logout révoque l'access et le refresh | **PASS** | Test `logoutRevokesRefreshAndAccessTokens`. |
| A-15 | Un jeton expiré renvoie 401 | **PARTIEL** | Filtre présent ; pas de test d'expiration réelle. |
| A-16 | Un jeton signé avec une mauvaise clé renvoie 401 | **PARTIEL** | Filtre présent ; pas de test de mauvaise clé. |
| A-17 | Le JWT contient l'entreprise et un `jti` | **PASS** | Claims `company_id` et `jti` émis et testés. |
| A-18 | Le login et le refresh sont limités en débit | **FAIL** | Absent. |
| A-19 | Le mot de passe est hashé et jamais renvoyé | **PASS** | BCrypt ; DTO sans hash. |
| A-20 | Le CORS n'accepte que les origines nécessaires | **PASS** | `CORS_ALLOWED_ORIGINS` explicite ; en-têtes/méthodes encore larges. |
| A-21 | Les erreurs SQL ne sont pas exposées en production | **PASS** | Mapping stable 400/409/422/503 ; aucun SQLSTATE ni message PostgreSQL. |
| A-22 | Un compte supprimé ne peut plus rafraîchir | **PASS** | 401 sur refresh d'un compte supprimé/inconnu. |

### Scénario d'acceptation A-A

```text
Étant donné un utilisateur ADMIN actif
Quand un administrateur lui retire le rôle ADMIN
Et qu'il appelle POST /api/v1/auth/refresh avec son refresh token
Alors la réponse ne contient plus ADMIN
Et l'accès à /api/v1/users renvoie 403
```

---

## 5. Catalogue

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| CAT-01 | CRUD nominal article, type, catégorie, entrepôt | **PARTIEL** | Code + dernier rapport, non rejoué |
| CAT-02 | Créer un type sans `unit`, `quantity_total`, `minimum_stock` applique les défauts | **PASS** | Test `ItemTypeApiTests.createWithoutOptionalValuesAppliesDatabaseDefaults`. |
| CAT-03 | Créer un employé sans `status` applique `ACTIVE` | **PASS** | Test `EmployeeDefaultsTests.createWithoutStatusAppliesDatabaseDefault`. |
| CAT-04 | Un doublon de QR/code/nom renvoie 409 | **PASS** | Mapping `23505` → 409 ; test doublon de catégorie. |
| CAT-05 | Un article ne peut pas référencer une relation d'une autre entreprise | **FAIL** | Aucun contrôle |
| CAT-06 | `GET /items` retourne `total`, `page`, `pageSize`, `totalPages` | **FAIL** | Tableau brut |
| CAT-07 | `page=0` renvoie 400, pas 500 | **FAIL** | `OFFSET` négatif possible |
| CAT-08 | `pageSize` est plafonné | **FAIL** | Aucun plafond typé |
| CAT-09 | Les transitions de statut d'article sont contrôlées | **FAIL** | `PATCH` libre |
| CAT-10 | On peut retirer une relation nullable par `PATCH` | **FAIL** | Convention « non nul seulement » |
| CAT-11 | Une suppression référencée renvoie 409, pas 500 | **PARTIEL** | `23503` → 422 (plus de 500), mais le critère attend 409. |
| CAT-12 | Une suppression inconnue renvoie 404, pas 204 | **PASS** | `DELETE` d'un article, type, catégorie ou entrepôt inconnu renvoie 404 ; tests dédiés Lot 4. |
| CAT-13 | Les éléments archivés ne figurent pas dans les listes | **PASS** | Filtres `archived_at IS NULL` sur articles, catégories, types et entrepôts ; tests de suppression/restauration. |
| CAT-14 | `GET /items/{id}/history` renvoie 404 pour un article inconnu | **FAIL** | Toujours 200 + listes vides ; l'historique a seulement été filtré par article. |
| CAT-15 | L'historique est filtré et paginé côté SQL | **PASS** | Lectures `by item` des mouvements et affectations, plafonnées à 500 lignes chacune ; pas encore de pagination complète. |
| CAT-16 | Le bulk est atomique ou retourne des échecs indexés | **FAIL** | Partiel, sans index, sans `@Valid` |
| CAT-17 | Les filtres article couvrent type, catégorie, entrepôt, statut, dates | **FAIL** | Filtres limités |
| CAT-18 | Un article supprimé conserve son historique | **PASS** | Archivage logique ; la ligne, les affectations et les mouvements sont conservés. |

---

## 6. Stock et mouvements

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| STK-01 | Un mouvement porte une quantité > 0 | **PASS** | `quantity` exposée, valeur par défaut 1, quantité nulle ou négative refusée. |
| STK-02 | Un mouvement porte source, destination, opération et acteur | **PARTIEL** | Champs exposés et alimentés ; l'acteur peut rester nul si le JWT et la requête n'apportent pas d'UUID. Les lignes historiques restent incomplètes. |
| STK-03 | Le stock disponible est calculé par entrepôt et type | **PASS** | `stock_balance` et `GET /stock-balances`. |
| STK-04 | Le stock ne peut pas devenir négatif | **PASS** | `409 INSUFFICIENT_STOCK`, état inchangé, test dédié. |
| STK-05 | Un mouvement validé est immuable | **PASS** | `PATCH`/`DELETE` renvoient `409 IMMUTABLE_RESOURCE`. |
| STK-06 | Une correction crée un mouvement inverse | **PARTIEL** | Un mouvement inverse manuel est possible ; aucune compensation automatique. |
| STK-07 | `IN` incrémente le stock | **PASS** | Solde incrémenté et article remis `AVAILABLE`. |
| STK-08 | `OUT` décrémente le stock et exige une affectation | **PASS** | `employee_id` et `post_id` obligatoires, affectation créée dans la transaction, solde décrémenté ; test dédié (`P0-06`). |
| STK-09 | `TRANSFER` déplace une quantité d'un entrepôt à l'autre | **PASS** | Solde source/destination et emplacement de l'article mis à jour. |
| STK-10 | `RETURN` clôt l'affectation active | **PASS** | Le retour clôt l'affectation active et restaure l'article ; test dédié (`P0-06`). |
| STK-11 | `DESTRUCTION` interdit un article affecté | **PASS** | `IN`, `TRANSFER` et `DESTRUCTION` refusent une affectation active (`409 ITEM_ASSIGNED`) ; test dédié. |
| STK-12 | `ADJUSTMENT` modifie une quantité | **PASS** | Solde ajusté selon la source/destination, sans changer l'état physique. |
| STK-13 | La réponse de création renvoie le type et la date réellement stockés | **PASS** | Création relue depuis la base (Lot 2). |
| STK-14 | Un mouvement inconnu renvoie 404 | **PASS** | Code + test. |
| STK-15 | Un mouvement référencé ne peut pas être supprimé sans compensation | **PASS** | `DELETE` interdit par le journal append-only. |
| STK-16 | Le rapport mouvements filtre par date, type, acteur et entreprise | **FAIL** | Filtres item/entrepôt seulement. |
| STK-17 | Le dashboard utilise le stock réel | **PARTIEL** | Le low-stock lit `stock_balance` ; les compteurs de synthèse restent globaux. |
| STK-18 | Les alertes low-stock utilisent un seuil par entrepôt | **PASS** | Seuil effectif `warehouse_stock_config.minimum_stock`, sinon `item_type.minimum_stock`. |

### Scénario d'acceptation STK-A

```text
Étant donné un article disponible dans l'entrepôt A
Et un stock initial de 5
Quand un mouvement OUT de quantité 2 est validé
Alors le stock de l'entrepôt A devient 3
Et l'article est ASSIGNED avec une affectation active
Et le mouvement contient performed_by, quantity=2, source_warehouse_id=A
Et un second OUT de 4 renvoie 409 sans modifier le stock
```

---

## 7. Affectations

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| ASG-01 | Un seul contrat d'affectation est exposé | **PARTIEL** | `/api/item-assignments` est canonique ; `/api/inventory/assignments` reste une façade dépréciée déléguant au même service. Deux préfixes subsistent jusqu'à D-07. |
| ASG-02 | Affecter un article non disponible renvoie 409 | **PASS** | `409 ITEM_NOT_AVAILABLE` ; test dédié. |
| ASG-03 | Affecter à un employé inactif renvoie 422 | **FAIL** | Aucun contrôle |
| ASG-04 | Affecter à un employé d'une autre entreprise renvoie 404/422 | **FAIL** | Aucun contrôle (`P0-01`) |
| ASG-05 | Affecter à un poste d'une autre entreprise renvoie 404/422 | **FAIL** | Aucun contrôle (`P0-01`) |
| ASG-06 | L'affectation et le passage à `ASSIGNED` sont atomiques | **PASS** | Service `@Transactional`, DAO sur connexion Spring, solde synchronisé ; tests de création et rollback. |
| ASG-07 | Un retour exige une date de retour | **PASS** | `POST /{id}/return` pose `returned_at` côté serveur ; date bornée à `assigned_at` si nécessaire. |
| ASG-08 | Un retour restaure l'entrepôt | **PASS** | `source_warehouse_id` ajouté et restauré ; test dédié. |
| ASG-09 | Un retour renseigne `returned_by` | **PASS** | Acteur JWT UUID ; test `returned_by`. Un acteur non-UUID reste nul. |
| ASG-10 | Un retour est idempotent | **PARTIEL** | Un second retour renvoie 409 `ASSIGNMENT_ALREADY_RETURNED` de façon déterministe, mais aucune clé d'idempotence (`P1-14`). |
| ASG-11 | Un retour avec seulement une condition ne renvoie pas 200 | **PASS** | `PATCH` avec `condition_on_return` seul renvoie 400 `RETURN_ENDPOINT_REQUIRED`. |
| ASG-12 | Une affectation active ne peut pas être supprimée | **PASS** | `DELETE` renvoie 409 `ACTIVE_ASSIGNMENT` ; test dédié. |
| ASG-13 | Une annulation est auditée et remet l'article disponible | **PASS** | `void` motivé, `voided_at`/`voided_by`/`void_reason`, article et stock restaurés. |
| ASG-14 | `ASSIGNED` implique exactement une affectation active | **PASS** | Tous les chemins passent par le service ; index unique actif et tests. |
| ASG-15 | `AVAILABLE` implique aucune affectation active | **PARTIEL** | Le code corrigé garantit la règle ; une affectation active préexistante de la base de démonstration pointe encore vers un article `AVAILABLE`. |
| ASG-16 | Deux affectations concurrentes produisent un 409 | **PARTIEL** | Index unique + `23505` → 409 ; pas de test de course dédié. |
| ASG-17 | Les affectations en retard sont identifiables | **FAIL** | `expected_return_at` est modifiable mais aucune route ni requête de retard n'existe. |
| ASG-18 | L'historique des acteurs est conservé | **PASS** | `assigned_by`, `returned_by` et `voided_by` alimentés depuis le JWT ; les lignes historiques restent nulles. |

### Scénario d'acceptation ASG-A

```text
Étant donné un article ASSIGNED avec une affectation active
Quand un opérateur appelle POST /api/v1/item-assignments/{id}/return
Alors returned_at et returned_by sont renseignés
Et l'affectation est clôturée
Et l'article redevient AVAILABLE dans son entrepôt de retour
Et un second appel renvoie 409 ou la même réponse idempotente
Et il n'existe plus aucune affectation active pour cet article
```

---

## 8. QR Codes et mobile

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| QR-01 | Le code est stable après impression | **FAIL** | `PATCH qr_code` libre |
| QR-02 | L'unicité est garantie par entreprise | **PASS** | `UNIQUE(company_id, qr_code)` |
| QR-03 | Un doublon de code renvoie 409 | **PASS** | Mapping `23505` → 409. |
| QR-04 | Un scan est strictement tenant-scoped | **FAIL** | Requête globale |
| QR-05 | Un scan inconnu renvoie 404 | **FAIL** | 200 `[]` |
| QR-06 | Un scan renvoie type, catégorie, entrepôt, affectation et détenteur | **FAIL** | Ligne brute |
| QR-07 | Un scan ne renvoie pas de champs internes inutiles | **FAIL** | `archived_by`, UUID, etc. |
| QR-08 | Chaque scan est journalisé | **FAIL** | Aucune table |
| QR-09 | Le scan est limité en débit | **FAIL** | Absent |
| QR-10 | L'image PNG est générée correctement | **PASS** | Code + test existant |
| QR-11 | L'image est mise en cache | **FAIL** | Aucun header |
| QR-12 | Une étiquette PDF est disponible | **FAIL** | PNG seul |
| QR-13 | Une planche d'étiquettes est disponible | **FAIL** | Absent |
| QR-14 | Un code peut être révoqué avec grâce | **FAIL** | Rotation absente |
| QR-15 | Deux entreprises peuvent avoir le même code sans ambiguïté | **FAIL** | Scan global |

### Scénario d'acceptation QR-A

```text
Étant donné deux entreprises A et B avec le même code QR
Et un utilisateur de A
Quand il scanne le code
Alors seul l'article de A est retourné
Et la réponse contient le type, l'entrepôt et l'affectation active
Et le scan est journalisé avec l'utilisateur et l'appareil
```

---

## 9. Comptages et opérations

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| CNT-01 | Un comptage fige la quantité attendue à l'ouverture | **PASS** | `expected_quantity` calculée au scan depuis l'état de l'article et l'entrepôt (`Lot3StockModelTests`). |
| CNT-02 | Un scan de comptage calcule l'écart côté serveur | **PASS** | `difference` et `discrepancy_type` calculés par le serveur ; le client ne peut pas les imposer. |
| CNT-03 | Un article hors périmètre est refusé | **PARTIEL** | Article d'une autre entreprise refusé en 404 ; article d'un autre entrepôt accepté comme écart `WRONG_LOCATION`/`MISSING`. |
| CNT-04 | Un doublon de scan est traité explicitement | **PASS** | Contrainte unique et erreur métier `Article déjà scanné`. |
| CNT-05 | Un comptage vide ne peut pas être clôturé comme complet | **FAIL** | Clôture sans corps toujours acceptée (hors périmètre du Lot 3). |
| CNT-06 | La clôture renseigne `completed_at` | **PASS** | Horodatage serveur écrit dans la clôture transactionnelle. |
| CNT-07 | La clôture réconcilie le stock | **PARTIEL** | Statut, emplacement et `stock_balance` réconciliés ; aucun mouvement compensatoire n'est journalisé. |
| CNT-08 | La clôture met à jour `last_inventory_at` | **PASS** | Écrit pour chaque ligne scannée ; rollback testé. |
| CNT-09 | `GET /inventory-counts/{id}` existe et renvoie 404 si inconnu | **PASS** | Route ajoutée et testée. |
| CNT-10 | Les lignes sont paginées et filtrables | **FAIL** | Résultats non paginés. |
| CNT-11 | Un comptage peut être annulé | **FAIL** | Absent. |
| CNT-12 | Un comptage est tenant-scoped | **PARTIEL** | Le scan contrôle l'entreprise de l'article ; les listes et lectures par ID ne filtrent pas encore par entreprise (`P0-01`). |
| OPS-01 | `POST /operations/receipt` impose `RECEIPT` | **PASS** | Type imposé par la route et testé. |
| OPS-02 | `POST /operations/issue` impose `ISSUE` | **PASS** | Type imposé par la route et testé. |
| OPS-03 | Une opération contient des lignes d'articles | **PASS** | `inventory_operation_item` créé pour chaque ligne. |
| OPS-04 | Une opération crée des mouvements atomiques | **PASS** | Mouvement, ligne et en-tête dans une transaction ; rollback testé. |
| OPS-05 | Un type manquant renvoie 400/422 | **PASS** | `23502` → 400 ; test `OperationApiTests.missingOperationTypeReturns400Envelope`. |
| OPS-06 | Une opération inconnue renvoie 404 | **FAIL** | Soft 200 |
| OPS-07 | Une opération est idempotente | **FAIL** | Absent |

### Scénario d'acceptation CNT-A

```text
Étant donné un comptage ouvert avec 3 lignes attendues
Quand un opérateur scanne 2 lignes avec des quantités
Et clôture avec applyAdjustments=true
Alors chaque ligne a expected_quantity, counted_quantity et difference
Et les ajustements sont créés atomiquement
Et completed_at et item.last_inventory_at sont renseignés
Et un comptage déjà clôturé renvoie 409
```

---

## 10. Contrat HTTP

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| API-01 | Les routes métier sont versionnées `/api/v1` | **FAIL** | `/api` |
| API-02 | Les listes renvoient un `PageResponse` avec total | **FAIL** | Tableaux bruts |
| API-03 | Aucun endpoint ne renvoie 200 avec `errors` | **FAIL** | Plusieurs modules |
| API-04 | Une erreur de saisie ne renvoie pas 500 | **PASS** | Doublons 409, FK/CHECK 422, NOT NULL/format 400, base indisponible 503. |
| API-05 | Aucun SQLSTATE ni message PostgreSQL en production | **PASS** | `ErrorMapper` et `SqlErrorMappingTests`. |
| API-06 | Une création renvoie 201 et `Location` | **PARTIEL** | 201 sans `Location` ; génériques en 200 |
| API-07 | Une suppression réussie renvoie 204 | **PARTIEL** | Souvent 204, parfois 200 |
| API-08 | Une réponse de création ne contient pas de date nulle due au défaut | **PASS** | Relecture persistée ; tests timestamp mouvement/affectation. |
| API-09 | Les dates sont ISO-8601 UTC | **NON VÉRIFIÉ** | Format non contractuel |
| API-10 | Les créations sensibles acceptent `Idempotency-Key` | **FAIL** | Absent |
| API-11 | Les `PATCH` sensibles utilisent `If-Match`/ETag | **FAIL** | Absent |
| API-12 | Les erreurs ont un `code` stable et documenté | **PASS** | Codes applicatifs (`CONFLICT`, `BAD_REQUEST`, `UNPROCESSABLE_ENTITY`, etc.) documentés dans `ItemError`. |
| API-13 | L'OpenAPI décrit tous les endpoints, erreurs et exemples | **PARTIEL** | 26 fichiers ; lint Redocly rejoué au Lot 4 : 179 avertissements, 0 erreur (183 à l'audit initial). Les exemples et les `license` manquent encore. |
| API-14 | Le CORS est restreint par environnement | **FAIL** | `*` |
| API-15 | Les filtres couvrent les besoins des écrans | **FAIL** | Voir rapports 02 et 10 |
| API-16 | Chaque réponse d'erreur contient un `traceId` | **FAIL** | Absent |
| API-17 | Les enums sont documentés avec leurs libellés | **FAIL** | Absent |
| API-18 | Un UUID invalide renvoie 400, pas 500 | **PARTIEL** | `IllegalArgumentException` → 400, `id()` générique non vérifié |

---

## 11. Performance, exploitation et sécurité

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| NFR-01 | Les DAO utilisent un `DataSource` et un pool | **PASS** | `DatabaseConnection` + HikariCP ; test de pool. |
| NFR-02 | Les DAO utilisent le `DataSource` et la configuration de production (pool, timeouts) | **PASS** | `spring.datasource.hikari.*` surchargé par l'environnement. |
| NFR-03 | Une liste de 1 000 articles reste sous un seuil défini | **NON VÉRIFIÉ** | Aucun test de charge |
| NFR-04 | Le N+1 est éliminé ou borné | **FAIL** | Mapper relationnel |
| NFR-05 | Les listes sont plafonnées | **FAIL** | `pageSize` libre |
| NFR-06 | Les images QR sont mises en cache | **FAIL** | Aucun cache |
| NFR-07 | Aucun secret n'apparaît dans une réponse ou un log | **PARTIEL** | Hash absent ; SQLSTATE retiré des réponses, mais logs et secrets non audités |
| NFR-08 | HTTPS est obligatoire en production | **FAIL** | Aucune règle |
| NFR-09 | Les migrations sont sauvegardées et réversibles | **NON VÉRIFIÉ** | Hors audit |
| NFR-10 | Les erreurs 500 sont corrélées par `traceId` | **FAIL** | Absent |
| NFR-11 | Les routes sensibles sont limitées en débit | **FAIL** | Absent |
| NFR-12 | Les conflits concurrents renvoient 409 | **FAIL** | Conflits SQL traduits en 409, mais aucune détection d'écrasement par version |
| NFR-13 | Les journaux d'audit couvrent les actions sensibles | **FAIL** | Absent |

---

## 12. Tests et CI

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| TST-01 | La suite tourne sur une base isolée et reproductible | **PARTIEL** | CI PostgreSQL 16 éphémère ; base locale réelle et pas d'isolation par test |
| TST-02 | Aucun test n'est ignoré silencieusement sur base vide | **FAIL** | `Assumptions` |
| TST-03 | Le login et l'inscription sont testés | **PASS** | `AuthHardeningApiTests` et `RefreshTokenApiTests` couvrent inscription, login, refresh et logout. |
| TST-04 | Le RBAC est testé pour `USER`, `INVENTORY`, `ADMIN`, sans rôle | **PARTIEL** | Tests sans permission, admin d'entreprise et inventaire ; matrice complète des rôles non couverte. |
| TST-05 | La tenancy A/B est testée en lecture et en écriture | **FAIL** | Aucun test à deux entreprises. |
| TST-06 | Le rollback transactionnel est testé | **PASS** | `DatabaseTransactionRollbackTests`, `StockMovementTransactionTests`. |
| TST-07 | La concurrence est testée | **FAIL** | Aucun test |
| TST-08 | Le stock et la réconciliation sont testés | **FAIL** | Aucun test de solde |
| TST-09 | Les contrats d'erreur et de pagination sont testés | **FAIL** | Tests d'erreur ajoutés ; pagination non testée |
| TST-10 | Le code et les tests sont synchronisés | **PARTIEL** | Ancien test 404 retiré ; l'historique reste sans test négatif (`P1-12`). |
| TST-11 | Un pipeline CI bloque les régressions | **PARTIEL** | Workflow GitHub Actions présent ; tests RBAC et transaction locaux, pas de tenancy A/B ni de concurrence |
| TST-12 | Les performances critiques sont mesurées | **FAIL** | Aucun test |
| TST-13 | La suite actuelle est verte sur l'état courant | **PASS** | 140 tests exécutés ; 0 échec, 0 erreur, 0 ignoré, 28 suites. |
| TST-14 | Les tests n'écrivent pas dans la base de démonstration partagée | **FAIL** | Nettoyage SQL sur base réelle |

---

## 13. Adéquation frontend

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| FE-01 | Le catalogue peut être listé, filtré et paginé sans total manquant | **PARTIEL** | Liste et filtres partiels |
| FE-02 | Le détail d'un article peut afficher type, catégorie, entrepôt et statut | **PARTIEL** | Réponses imbriquées, relations nulles |
| FE-03 | La création d'un type ne casse pas si les champs facultatifs sont omis | **PASS** | Défauts PostgreSQL appliqués et relus. |
| FE-04 | Les erreurs peuvent être mappées sur des messages utilisateur | **PARTIEL** | Statuts et codes stables ; certaines routes renvoient encore des erreurs douces en 200. |
| FE-05 | L'historique d'un article est complet et paginé | **FAIL** | 100 globales |
| FE-06 | Un mouvement peut être créé avec quantité et acteur | **FAIL** | Champs absents |
| FE-07 | Une affectation peut être créée, retournée et auditée | **FAIL** | Cycle incohérent |
| FE-08 | Un scan QR fournit assez d'informations pour agir | **FAIL** | Réponse brute |
| FE-09 | Une étiquette PDF peut être imprimée en lot | **FAIL** | Absent |
| FE-10 | Le dashboard affiche un stock réel | **PARTIEL** | Le low-stock utilise `stock_balance` ; les autres compteurs ne sont pas encore un stock par entrepôt. |
| FE-11 | Le dashboard est tenant-scoped | **FAIL** | Global |
| FE-12 | Les écrans peuvent gérer le multi-entreprise | **FAIL** | Pas de tenant |
| FE-13 | Les dates sont affichables sans rechargement après création | **PASS** | Réponses de création relues depuis la base. |
| FE-14 | Les actions sensibles sont idempotentes | **FAIL** | Absent |

---

## 14. Bilan actuel

| Domaine | PASS | PARTIEL | FAIL | NON VÉRIFIÉ |
|---|---:|---:|---:|---:|
| Portes Go/No-Go | 4 | 1 | 3 | 0 |
| Tenancy | 2 | 2 | 11 | 0 |
| Authentification | 15 | 3 | 4 | 0 |
| Catalogue | 4 | 2 | 12 | 0 |
| Stock | 11 | 4 | 3 | 0 |
| Affectations | 1 | 2 | 15 | 0 |
| QR | 3 | 0 | 12 | 0 |
| Comptages / opérations | 11 | 3 | 5 | 0 |
| Contrat HTTP | 4 | 4 | 9 | 1 |
| Performance / sécurité | 2 | 1 | 8 | 2 |
| Tests / CI | 3 | 4 | 7 | 0 |
| Frontend | 2 | 4 | 8 | 0 |
| **Total** | **62** | **30** | **97** | **3** |

**Total : 192 critères** (62 `PASS`, 30 `PARTIEL`, 97 `FAIL`, 3 `NON VÉRIFIÉ`) après le Lot 3. La ligne « Comptages / opérations » contient 19 critères : `CNT-01` à `CNT-12` (12) et `OPS-01` à `OPS-07` (7).

**Lecture :** les `PASS` supplémentaires portent sur l'identité, le RBAC inventaire/administration, le scope entreprise des utilisateurs/rôles, le refresh rotatif, le logout, la révocation, le pool, les transactions, les erreurs SQL, les défauts de création, le solde calculé, l'immuabilité du journal, les opérations multi-lignes et la réconciliation des comptages. Les portes de tenancy complète, d'affectations synchronisées et de concurrence restent bloquantes.

---

## 15. Scénarios de recette minimaux

Ces scénarios doivent être exécutés sur une base isolée avant tout Go.

### S-01 — Isolation A/B

```text
Étant donné A et B avec chacune un article, un entrepôt et un utilisateur
Quand l'utilisateur de A liste, lit, modifie et supprime les ressources de B
Alors chaque lecture renvoie 404 et chaque écriture renvoie 404/403
Et aucune ligne de B n'est modifiée
```

### S-02 — Révocation

```text
Étant donné un ADMIN avec un refresh token valide
Quand son rôle ADMIN est retiré
Et qu'il rafraîchit
Alors le nouveau jeton ne contient plus ADMIN
Et /api/v1/users renvoie 403
```

### S-03 — Transaction

```text
Étant donné un mouvement valide
Quand la mise à jour de l'article échoue après l'insertion
Alors aucun mouvement n'est conservé
Et l'article garde son état initial
```

### S-04 — Stock

```text
Étant donné 5 unités dans l'entrepôt A
Quand 2 unités sont sorties
Alors le solde est 3
Et un mouvement de 4 unités est refusé sans modifier le solde
Et le mouvement contient quantité, acteur et entrepôt source
```

### S-05 — Affectation

```text
Étant donné un article disponible
Quand il est affecté à un employé actif de la même entreprise
Alors l'article est ASSIGNED et une affectation active existe
Quand il est retourné
Alors l'article est AVAILABLE, l'affectation est clôturée et l'entrepôt est restauré
```

### S-06 — Comptage

```text
Étant donné un comptage avec 3 lignes attendues
Quand 2 lignes sont scannées et le comptage clôturé avec ajustements
Alors les écarts sont calculés par le serveur
Et les soldes sont ajustés atomiquement
Et last_inventory_at et completed_at sont renseignés
```

### S-07 — QR

```text
Étant donné deux entreprises avec le même code
Quand un utilisateur de A scanne
Alors seul l'article de A est retourné avec son affectation
Et le scan est journalisé
```

### S-08 — Erreurs

```text
Étant donné une création avec un doublon
Quand l'appel est effectué
Alors la réponse est 409 avec un code stable
Et aucun SQLSTATE n'est exposé
Et aucun 200 avec errors n'est retourné
```

---

## 16. Critères de sortie par phase

| Phase | Critères minimum |
|---|---|
| Pilote interne mono-entreprise | T-14, A-19, CAT-01, STK-14, QR-10, API-07 |
| Pilote multi-utilisateurs | T-01 à T-15, A-01 à A-22, ASG-01 à ASG-18 |
| Production mono-entreprise | G0-4, G0-5, G0-7, STK-01 à STK-18, API-03 à API-05 |
| Production multi-entreprise | Tous les P0 et P1, S-01 à S-08 |
| Ouverture frontend | FE-01 à FE-14, API-01 à API-18 |

---

## 17. Conclusion

Le backend ne satisfait pas aujourd'hui les portes Go/No-Go. Il peut servir à un prototype mono-entreprise, avec un utilisateur de confiance, sans enjeu de stock ni de tenancy. Il ne peut pas être déclaré prêt pour la production, ni recevoir un frontend de production, tant que les critères G0-1 à G0-8 ne sont pas `PASS`.

Le plan pour les atteindre est dans le rapport 14. Les anomalies et priorités sont dans le rapport 12.
