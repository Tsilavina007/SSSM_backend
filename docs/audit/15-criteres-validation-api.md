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

**Important :** la suite complète a été exécutée après le Lot 1 : 120 tests, 119 succès, 1 échec préexistant. Un `PASS` signifie « cohérent avec le code et les tests disponibles » ; les critères de tenancy A/B et de concurrence restent non vérifiés.

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
| **G0-4** | Les écritures multi-lignes sont atomiques | **FAIL** | `DriverManager` ; `@Transactional` inopérant |
| **G0-5** | Le stock a une source de vérité unique et testée | **FAIL** | `quantity_total` manuel ; quantité de mouvement fixe |
| **G0-6** | `ASSIGNED` implique exactement une affectation active | **FAIL** | `OUT`, `RETURN`, `DELETE`, retour no-op |
| **G0-7** | Aucune erreur de saisie ne produit 500 avec SQLSTATE | **FAIL** | Doublons, FK, NOT NULL en 500 |
| **G0-8** | Un rôle révoqué ne peut pas être rafraîchi | **PASS** | `/refresh` recharge les rôles et permissions en base ; rotation testée. |

**Verdict :** 1 porte sur 8 en `PASS` (`G0-8`), 1 `PARTIEL` (`G0-3`), 6 `FAIL`. La production reste bloquée.

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
| T-10 | Un comptage ne peut scanner que des articles de son entreprise et de son périmètre | **FAIL** | Aucun contrôle |
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
| A-21 | Les erreurs SQL ne sont pas exposées en production | **FAIL** | SQLSTATE + message PostgreSQL. |
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
| CAT-02 | Créer un type sans `unit`, `quantity_total`, `minimum_stock` applique les défauts | **FAIL** | `NULL` explicite → 500 |
| CAT-03 | Créer un employé sans `status` applique `ACTIVE` | **FAIL** | `NULL` explicite → 500 |
| CAT-04 | Un doublon de QR/code/nom renvoie 409 | **FAIL** | 500 |
| CAT-05 | Un article ne peut pas référencer une relation d'une autre entreprise | **FAIL** | Aucun contrôle |
| CAT-06 | `GET /items` retourne `total`, `page`, `pageSize`, `totalPages` | **FAIL** | Tableau brut |
| CAT-07 | `page=0` renvoie 400, pas 500 | **FAIL** | `OFFSET` négatif possible |
| CAT-08 | `pageSize` est plafonné | **FAIL** | Aucun plafond typé |
| CAT-09 | Les transitions de statut d'article sont contrôlées | **FAIL** | `PATCH` libre |
| CAT-10 | On peut retirer une relation nullable par `PATCH` | **FAIL** | Convention « non nul seulement » |
| CAT-11 | Une suppression référencée renvoie 409, pas 500 | **FAIL** | `23503` → 500 |
| CAT-12 | Une suppression inconnue renvoie 404, pas 204 | **FAIL** | 204 |
| CAT-13 | Les éléments archivés ne figurent pas dans les listes | **FAIL** | Aucun filtre `archived_at` |
| CAT-14 | `GET /items/{id}/history` renvoie 404 pour un article inconnu | **FAIL** | 200 + listes vides |
| CAT-15 | L'historique est filtré et paginé côté SQL | **FAIL** | 100 lignes globales + filtre mémoire |
| CAT-16 | Le bulk est atomique ou retourne des échecs indexés | **FAIL** | Partiel, sans index, sans `@Valid` |
| CAT-17 | Les filtres article couvrent type, catégorie, entrepôt, statut, dates | **FAIL** | Filtres limités |
| CAT-18 | Un article supprimé conserve son historique | **PASS** | FK RESTRICT |

---

## 6. Stock et mouvements

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| STK-01 | Un mouvement porte une quantité > 0 | **FAIL** | Quantité fixe à 1 |
| STK-02 | Un mouvement porte source, destination, opération et acteur | **FAIL** | Champs absents du DTO |
| STK-03 | Le stock disponible est calculé par entrepôt et type | **FAIL** | Aucun solde |
| STK-04 | Le stock ne peut pas devenir négatif | **FAIL** | Aucune logique de stock |
| STK-05 | Un mouvement validé est immuable | **FAIL** | `PATCH` et `DELETE` exposés |
| STK-06 | Une correction crée un mouvement inverse | **FAIL** | Aucune compensation |
| STK-07 | `IN` incrémente le stock | **FAIL** | Change seulement le statut |
| STK-08 | `OUT` décrémente le stock et exige une affectation | **FAIL** | Statut `ASSIGNED` sans affectation |
| STK-09 | `TRANSFER` déplace une quantité d'un entrepôt à l'autre | **FAIL** | Change seulement `current_warehouse_id` |
| STK-10 | `RETURN` clôt l'affectation active | **FAIL** | Ne la clôt pas |
| STK-11 | `DESTRUCTION` interdit un article affecté | **FAIL** | Aucun contrôle |
| STK-12 | `ADJUSTMENT` modifie une quantité | **FAIL** | Aucun effet |
| STK-13 | La réponse de création renvoie le type et la date réellement stockés | **FAIL** | `null` si absents en entrée |
| STK-14 | Un mouvement inconnu renvoie 404 | **PASS** | Code + test |
| STK-15 | Un mouvement référencé ne peut pas être supprimé sans compensation | **FAIL** | `DELETE` libre |
| STK-16 | Le rapport mouvements filtre par date, type, acteur et entreprise | **FAIL** | Filtres item/entrepôt seulement |
| STK-17 | Le dashboard utilise le stock réel | **FAIL** | Compteurs globaux et champ manuel |
| STK-18 | Les alertes low-stock utilisent un seuil par entrepôt | **FAIL** | `quantity_total` / `minimum_stock` |

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
| ASG-01 | Une seule API d'affectation est exposée | **FAIL** | Deux modules |
| ASG-02 | Affecter un article non disponible renvoie 409 | **PARTIEL** | 400 dans le module typé ; aucun contrôle générique |
| ASG-03 | Affecter à un employé inactif renvoie 422 | **FAIL** | Aucun contrôle |
| ASG-04 | Affecter à un employé d'une autre entreprise renvoie 404/422 | **FAIL** | Aucun contrôle |
| ASG-05 | Affecter à un poste d'une autre entreprise renvoie 404/422 | **FAIL** | Aucun contrôle |
| ASG-06 | L'affectation et le passage à `ASSIGNED` sont atomiques | **FAIL** | Transactions inopérantes |
| ASG-07 | Un retour exige une date de retour | **FAIL** | `returned_at` facultatif |
| ASG-08 | Un retour restaure l'entrepôt | **FAIL** | Non restauré |
| ASG-09 | Un retour renseigne `returned_by` | **FAIL** | Aucun acteur |
| ASG-10 | Un retour est idempotent | **FAIL** | Non testé / non garanti |
| ASG-11 | Un retour avec seulement une condition ne renvoie pas 200 | **FAIL** | No-op accepté |
| ASG-12 | Une affectation active ne peut pas être supprimée | **FAIL** | `DELETE` exposé |
| ASG-13 | Une annulation est auditée et remet l'article disponible | **FAIL** | Pas de `void` |
| ASG-14 | `ASSIGNED` implique exactement une affectation active | **FAIL** | Plusieurs chemins incohérents |
| ASG-15 | `AVAILABLE` implique aucune affectation active | **FAIL** | `RETURN`/`IN` peuvent le faire |
| ASG-16 | Deux affectations concurrentes produisent un 409 | **FAIL** | Index → 500 |
| ASG-17 | Les affectations en retard sont identifiables | **FAIL** | `expected_return_at` non exploité |
| ASG-18 | L'historique des acteurs est conservé | **FAIL** | `assigned_by`/`returned_by` non alimentés |

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
| QR-03 | Un doublon de code renvoie 409 | **FAIL** | 500 |
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
| CNT-01 | Un comptage fige la quantité attendue à l'ouverture | **FAIL** | Colonne absente |
| CNT-02 | Un scan de comptage calcule l'écart côté serveur | **FAIL** | Écart fourni par le client |
| CNT-03 | Un article hors périmètre est refusé | **FAIL** | Aucun contrôle |
| CNT-04 | Un doublon de scan est traité explicitement | **PARTIEL** | Contrainte, mais 500 |
| CNT-05 | Un comptage vide ne peut pas être clôturé comme complet | **FAIL** | Clôture sans corps acceptée |
| CNT-06 | La clôture renseigne `completed_at` | **FAIL** | Non renseigné |
| CNT-07 | La clôture réconcilie le stock | **FAIL** | Aucun ajustement |
| CNT-08 | La clôture met à jour `last_inventory_at` | **FAIL** | Jamais écrit |
| CNT-09 | `GET /inventory-counts/{id}` existe et renvoie 404 si inconnu | **FAIL** | Absent |
| CNT-10 | Les lignes sont paginées et filtrables | **FAIL** | Résultats non structurés |
| CNT-11 | Un comptage peut être annulé | **FAIL** | Absent |
| CNT-12 | Un comptage est tenant-scoped | **FAIL** | Aucun filtre |
| OPS-01 | `POST /operations/receipt` impose `RECEIPT` | **FAIL** | Type non déduit |
| OPS-02 | `POST /operations/issue` impose `ISSUE` | **FAIL** | Type non déduit |
| OPS-03 | Une opération contient des lignes d'articles | **FAIL** | En-tête seul |
| OPS-04 | Une opération crée des mouvements atomiques | **FAIL** | Aucun mouvement |
| OPS-05 | Un type manquant renvoie 400/422 | **FAIL** | 500 `23502` |
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
| API-04 | Une erreur de saisie ne renvoie pas 500 | **FAIL** | Doublons/FK/defaults |
| API-05 | Aucun SQLSTATE ni message PostgreSQL en production | **FAIL** | `ErrorMapper` |
| API-06 | Une création renvoie 201 et `Location` | **PARTIEL** | 201 sans `Location` ; génériques en 200 |
| API-07 | Une suppression réussie renvoie 204 | **PARTIEL** | Souvent 204, parfois 200 |
| API-08 | Une réponse de création ne contient pas de date nulle due au défaut | **FAIL** | `assigned_at`, `movement_date` |
| API-09 | Les dates sont ISO-8601 UTC | **NON VÉRIFIÉ** | Format non contractuel |
| API-10 | Les créations sensibles acceptent `Idempotency-Key` | **FAIL** | Absent |
| API-11 | Les `PATCH` sensibles utilisent `If-Match`/ETag | **FAIL** | Absent |
| API-12 | Les erreurs ont un `code` stable et documenté | **FAIL** | Codes génériques + SQLSTATE |
| API-13 | L'OpenAPI décrit tous les endpoints, erreurs et exemples | **PARTIEL** | 26 fichiers, 183 avertissements à l'audit initial (non rejoués après `7e3f196`) |
| API-14 | Le CORS est restreint par environnement | **FAIL** | `*` |
| API-15 | Les filtres couvrent les besoins des écrans | **FAIL** | Voir rapports 02 et 10 |
| API-16 | Chaque réponse d'erreur contient un `traceId` | **FAIL** | Absent |
| API-17 | Les enums sont documentés avec leurs libellés | **FAIL** | Absent |
| API-18 | Un UUID invalide renvoie 400, pas 500 | **PARTIEL** | `IllegalArgumentException` → 400, `id()` générique non vérifié |

---

## 11. Performance, exploitation et sécurité

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| NFR-01 | Les DAO utilisent un `DataSource` et un pool | **FAIL** | `DriverManager` |
| NFR-02 | Les DAO utilisent le `DataSource` et la configuration de production (pool, timeouts) | **FAIL** | `DatabaseConnection`/`DriverManager` |
| NFR-03 | Une liste de 1 000 articles reste sous un seuil défini | **NON VÉRIFIÉ** | Aucun test de charge |
| NFR-04 | Le N+1 est éliminé ou borné | **FAIL** | Mapper relationnel |
| NFR-05 | Les listes sont plafonnées | **FAIL** | `pageSize` libre |
| NFR-06 | Les images QR sont mises en cache | **FAIL** | Aucun cache |
| NFR-07 | Aucun secret n'apparaît dans une réponse ou un log | **PARTIEL** | Hash absent ; SQL exposé |
| NFR-08 | HTTPS est obligatoire en production | **FAIL** | Aucune règle |
| NFR-09 | Les migrations sont sauvegardées et réversibles | **NON VÉRIFIÉ** | Hors audit |
| NFR-10 | Les erreurs 500 sont corrélées par `traceId` | **FAIL** | Absent |
| NFR-11 | Les routes sensibles sont limitées en débit | **FAIL** | Absent |
| NFR-12 | Les conflits concurrents renvoient 409 | **FAIL** | 500 ou écrasement |
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
| TST-06 | Le rollback transactionnel est testé | **FAIL** | Aucun test |
| TST-07 | La concurrence est testée | **FAIL** | Aucun test |
| TST-08 | Le stock et la réconciliation sont testés | **FAIL** | Aucun test de solde |
| TST-09 | Les contrats d'erreur et de pagination sont testés | **FAIL** | Aucun test de contrat |
| TST-10 | Le code et les tests sont synchronisés | **FAIL** | Divergence historique 404 |
| TST-11 | Un pipeline CI bloque les régressions | **PARTIEL** | Workflow GitHub Actions présent ; pas de tests tenancy/RBAC/transaction |
| TST-12 | Les performances critiques sont mesurées | **FAIL** | Aucun test |
| TST-13 | La suite actuelle est verte sur l'état courant | **FAIL** | 120 tests exécutés ; 1 échec préexistant (`historyUnknownReturnsNotFound`). |
| TST-14 | Les tests n'écrivent pas dans la base de démonstration partagée | **FAIL** | Nettoyage SQL sur base réelle |

---

## 13. Adéquation frontend

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| FE-01 | Le catalogue peut être listé, filtré et paginé sans total manquant | **PARTIEL** | Liste et filtres partiels |
| FE-02 | Le détail d'un article peut afficher type, catégorie, entrepôt et statut | **PARTIEL** | Réponses imbriquées, relations nulles |
| FE-03 | La création d'un type ne casse pas si les champs facultatifs sont omis | **FAIL** | 500 |
| FE-04 | Les erreurs peuvent être mappées sur des messages utilisateur | **FAIL** | 500/SQLSTATE/soft 200 |
| FE-05 | L'historique d'un article est complet et paginé | **FAIL** | 100 globales |
| FE-06 | Un mouvement peut être créé avec quantité et acteur | **FAIL** | Champs absents |
| FE-07 | Une affectation peut être créée, retournée et auditée | **FAIL** | Cycle incohérent |
| FE-08 | Un scan QR fournit assez d'informations pour agir | **FAIL** | Réponse brute |
| FE-09 | Une étiquette PDF peut être imprimée en lot | **FAIL** | Absent |
| FE-10 | Le dashboard affiche un stock réel | **FAIL** | Champ manuel |
| FE-11 | Le dashboard est tenant-scoped | **FAIL** | Global |
| FE-12 | Les écrans peuvent gérer le multi-entreprise | **FAIL** | Pas de tenant |
| FE-13 | Les dates sont affichables sans rechargement après création | **FAIL** | `null` |
| FE-14 | Les actions sensibles sont idempotentes | **FAIL** | Absent |

---

## 14. Bilan actuel

| Domaine | PASS | PARTIEL | FAIL | NON VÉRIFIÉ |
|---|---:|---:|---:|---:|
| Portes Go/No-Go | 1 | 1 | 6 | 0 |
| Tenancy | 2 | 1 | 12 | 0 |
| Authentification | 14 | 3 | 5 | 0 |
| Catalogue | 1 | 1 | 16 | 0 |
| Stock | 1 | 0 | 17 | 0 |
| Affectations | 0 | 1 | 17 | 0 |
| QR | 2 | 0 | 13 | 0 |
| Comptages / opérations | 0 | 1 | 18 | 0 |
| Contrat HTTP | 0 | 4 | 13 | 1 |
| Performance / sécurité | 0 | 1 | 10 | 2 |
| Tests / CI | 1 | 3 | 10 | 0 |
| Frontend | 0 | 2 | 12 | 0 |
| **Total** | **22** | **18** | **149** | **3** |

**Total : 192 critères** (22 `PASS`, 18 `PARTIEL`, 149 `FAIL`, 3 `NON VÉRIFIÉ`) après le Lot 1. La ligne « Comptages / opérations » contient 19 critères : `CNT-01` à `CNT-12` (12) et `OPS-01` à `OPS-07` (7).

**Lecture :** les `PASS` supplémentaires portent sur l'identité, le RBAC inventaire/administration, le scope entreprise des utilisateurs/rôles, le refresh rotatif, le logout et la révocation. Les portes de tenancy complète, transactions, stock, affectations et concurrence restent bloquantes.

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
