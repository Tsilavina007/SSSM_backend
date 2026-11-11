# Collections Postman — API SSSM

Deux collections et deux environnements sont livrés pour l'API SSSM en
local :

| Livrable | Contenu |
|---|---|
| `SSSM-Demo.postman_collection.json` | **Démonographie technique** : 23 requêtes, 84 assertions, scénario contrôlé (authentification, affectation/retour, transfert entre entrepôts). |
| `SSSM-Demo.postman_environment.json` | Environnement de la démonstration (URL, compte). |
| `SSSM-API.postman_collection.json` | **Collection complète** : les 152 routes HTTP du backend, organisées par module puis par ressource, avec assertions. |
| `SSSM-API.postman_environment.json` | Environnement local de l'API (URL, compte). |
| `SSSM-API.postman_environment.recette.json` | Modèle d'environnement pour une autre cible (`baseUrl` à compléter). |
| `docs/database/mock_data/demo_postman.sql` | Fixtures et script de réinitialisation des deux collections. |
| `scripts/demo/reset-postman-demo.ps1` / `.sh` | Wrappers de réinitialisation. |
| `scripts/postman/generate-postman-collection.py` | Générateur de la collection « API complète » à partir des spécifications OpenAPI. |

## 1. Prérequis

- **PostgreSQL** accessible (client `psql` dans le `PATH`).
- **Base de démonstration** chargée avec le schéma
  (`docs/database/sssm_schema2_v01.sql`) et les données de démonstration
  (`docs/database/mock_data/sssm_mock_data.sql`).
- **Backend** démarré sur `http://localhost:8080`.
- **Postman** (application de bureau) ou **Newman** pour l'exécution en ligne
  de commande.
- Compte de démonstration : `demo.admin` / `Demo@2024`.

## 2. Réinitialisation des données

Le script SQL est **idempotent** et ne touche qu'aux lignes portant le
préfixe `DEMO-POSTMAN` (ou `QR-DEMO` pour les articles créés par la collection
API). Les autres données de démonstration restent intactes.

### Windows PowerShell

```powershell
.\scripts\demo\reset-postman-demo.ps1
```

### Linux / macOS

```bash
./scripts/demo/reset-postman-demo.sh
```

Si le fichier n'est pas encore exécutable :

```bash
chmod +x scripts/demo/reset-postman-demo.sh
```

### Appel direct de psql

```bash
psql -h localhost -p 5432 -U postgres -d sssm_db \
  -v ON_ERROR_STOP=1 \
  -f docs/database/mock_data/demo_postman.sql
```

Le script accepte les variables d'environnement `DB_HOST`, `DB_PORT`,
`DB_NAME`, `DB_USER` et `DB_PASSWORD`. Les valeurs par défaut ciblent la base
locale de développement : `localhost:5432`, `sssm_db`, `postgres` / `postgres`.

En fin d'exécution, un récapitulatif rappelle les identifiants créées :

```text
Solde initial | 3 | DEMO-RADIO dans l'entrepôt démo
Solde API     | 4 | DEMO-TOOL dans l'entrepôt API
```

Trois entrepôts sont préparés : l'entrepôt de la démonstration, l'entrepôt de
la collection API et l'entrepôt de destination du transfert de démonstration
(`Entrepôt démo Postman (transfert)`), qui ne porte aucun solde tant que le
transfert n'a pas été joué.

> Le reset supprime les affectations, mouvements, opérations et inventaires
> des fixtures. Ne pas l'exécuter pendant qu'une démonstration est en cours.

## 3. Démonstration technique (scénario piloté)

### Import

1. **Import** → `docs/postman/SSSM-Demo.postman_collection.json` et
   `docs/postman/SSSM-Demo.postman_environment.json`.
2. Sélectionner l'environnement **SSSM — Démo Postman (local)**.

### Déroulé

Exécuter les dossiers **dans l'ordre** :

1. `01 — Authentification` : login, whoami, refresh avec **rotation** des deux
   jetons, whoami avec le jeton renouvelé.
2. `02 — Affectation et retour` : l'article passe `AVAILABLE` → `ASSIGNED` →
   `AVAILABLE`, le solde suit 3 → 2 → 3, et un second retour renvoie
   `409 ASSIGNMENT_ALREADY_RETURNED`.
3. `03 — Transfert entre entrepôts et solde` : mouvement `TRANSFER` de
   l'article `DEMO-POSTMAN-MOVE` vers l'entrepôt de destination, solde source
   3 → 2, solde destination 0 → 1, statut de l'article inchangé et **aucune
   affectation créée**.
4. `04 — Lecture d'ensemble` : état final des données.

| Étape | Article `ASSIGN` | Article `MOVE` | Article `SPARE` | Solde source | Solde destination |
|---|---|---|---|---|---|
| Après reset | `AVAILABLE` | `AVAILABLE` | `AVAILABLE` | `3` | `0` |
| Après affectation (02.2) | `ASSIGNED` | `AVAILABLE` | `AVAILABLE` | `2` | `0` |
| Après retour (02.5) | `AVAILABLE` | `AVAILABLE` | `AVAILABLE` | `3` | `0` |
| Après transfert (03.3) | `AVAILABLE` | `AVAILABLE` (destination) | `AVAILABLE` | `2` | `1` |

À la fin, l'article `DEMO-POSTMAN-MOVE` se trouve dans l'entrepôt de
destination, le solde source reste à `2` et le solde destination vaut `1` :
relancer le reset avant une nouvelle démonstration.

Le détail des requêtes, des résultats attendus et du dépannage figure dans
la section « Scénario de démonstration » de ce document, section 5.

## 4. Collection API complète

### Import

1. **Import** → `docs/postman/SSSM-API.postman_collection.json` et
   `docs/postman/SSSM-API.postman_environment.json`.
2. Sélectionner l'environnement **SSSM — API (local)**.

### Organisation

```text
00 — Authentification                     5 requêtes
01 — Utilisateurs, rôles et permissions  19 requêtes
   Utilisateurs (7) · Rôles (7) · Permissions (5)
02 — Organisation                        15 requêtes
   Sociétés (5) · Agences (5) · Succursales (5)
03 — Personnel                           10 requêtes
   Employés (5) · Documents d'employé (5)
04 — Clients                             10 requêtes
   Clients (5) · Contacts client (5)
05 — Sites et postes                     15 requêtes
   Sites (5) · Postes de sécurité (5) · Exigences de poste (5)
06 — Inventaire                          76 requêtes
   Catégories d'articles (6) · Types d'articles (10) · Entrepôts (6)
   Articles (10) · Affectations (11) · Affectations alias historique (11)
   Mouvements de stock (5) · Soldes de stock (1) · Opérations d'inventaire (3)
   Inventaires physiques (7) · QR code et scans (1) · Étiquettes (2)
   Rapports (1) · Tableau de bord et alertes (2)
07 — Système                              2 requêtes
   Diagnostics (2)
```

**Total : 152 requêtes, 196 assertions.** Les 149 opérations documentées dans
`docs/api` sont couvertes, complétées par `POST /api/v1/auth/logout`,
`GET /ping` et `GET /` (contrôleurs hors spécification).

### Règles d'exécution

- **Authentification** : le Bearer `{{accessToken}}` est défini au niveau de
  la collection et capturé automatiquement par la requête de login. Les
  requêtes publiques (`login`, `refresh`, `logout`, `ping`, `hello`) n'envoient
  pas d'en-tête.
- **Identifiants de ressources** : ce sont des **variables de collection**, pas
  des variables d'environnement. Elles sont alimentées par les scripts de test
  (premier élément d'une liste, identifiant renvoyé par une création). Aucun
  identifiant n'est masqué par l'environnement.
- **Requêtes mutantes** : les `PATCH`, `DELETE` et les actions visent la
  ressource **créée dans le même dossier** (`{{createdXxxId}}`), jamais les
  données de démonstration. Si la création a échoué, la requête mutante ne
  cible rien et ne dégrade pas la base.
- **Écritures d'inventaire** : elles visent l'entrepôt isolé
  « Entrepôt démo Postman (API) » (type `DEMO-TOOL`) et les articles
  `DEMO-POSTMAN-API-*`. Le solde de démonstration (`DEMO-RADIO`, 3 articles)
  n'est jamais modifié par la collection API.
- **Statuts attendus** : chaque requête vérifie le statut documenté dans la
  spécification OpenAPI. Les opérations dont le résultat dépend de l'état
  métier (retour, void, restauration, clôture d'inventaire) acceptent un
  succès ou un conflit documenté.

### Variables de collection

| Catégorie | Variables |
|---|---|
| Connexion | `accessToken`, `refreshToken`, `userId`, `companyId`, `roles` |
| Organisation | `agencyId`, `branchId`, `roleId`, `permissionId` |
| Personnel | `employeeId`, `employeeDocumentId` |
| Clients | `clientId`, `clientContactId` |
| Sites | `siteId`, `postId`, `postRequirementId` |
| Inventaire | `itemCategoryId`, `itemTypeId`, `warehouseId`, `itemId`, `assignmentId`, `movementId`, `inventoryCountId` |
| Fixtures isolées | `demoWarehouseId`, `demoEmployeeId`, `demoPostId`, `demoItemAssignId`, `demoItemOutId`, `demoItemSpareId`, `demoApiWarehouseId`, `demoApiItemTypeId`, `demoApiItemAssignId`, `demoApiItemAliasId`, `demoApiItemOpId`, `demoApiItemMoveId` |
| Créations | `createdCompanyId`, `createdUserId`, `createdRoleId`, … (une par ressource) |

## 5. Exécution en ligne de commande

```bash
npm install --global newman
```

Collection API complète :

```bash
newman run docs/postman/SSSM-API.postman_collection.json \
  -e docs/postman/SSSM-API.postman_environment.json \
  --reporters cli
```

Démonstration :

```bash
newman run docs/postman/SSSM-Demo.postman_collection.json \
  -e docs/postman/SSSM-Demo.postman_environment.json \
  --reporters cli
```

Résultats attendus après un reset :

| Collection | Requêtes | Assertions | Échecs |
|---|---:|---:|---:|
| `SSSM-API` | 152 | 196 | 0 |
| `SSSM-Demo` | 23 | 84 | 0 |

Un dossier peut être rejoué isolément avec `--folder "<nom>"`.

> Relancer le reset entre deux exécutions complètes : la collection API crée
> un mouvement `OUT` qui affecte définitivement son article, et la démo
> transfère définitivement `DEMO-POSTMAN-MOVE` vers l'entrepôt de destination.

## 6. Scénario de démonstration

Chaque requête de la collection de démonstration porte une assertion
spécifique :

| Requête | Résultat attendu |
|---|---|
| `01.1 Login` | `200`, `token` et `refreshToken` non vides, utilisateur `demo.admin`, rôle `ADMIN`. |
| `01.2 Whoami` | `200`, même utilisateur : l'access token est accepté. |
| `01.3 Refresh` | `200`, **nouvel** access token et **nouveau** refresh token. |
| `01.4 Whoami` | `200`, toujours le même utilisateur avec le jeton renouvelé. |
| `02.1 Article avant affectation` | `AVAILABLE`, entrepôt démo. |
| `02.2 Créer l'affectation` | `201`, `returned_at` nul, `source_warehouse_id` et `assigned_by` renseignés. |
| `02.3 Article affecté` | `ASSIGNED`, `current_warehouse` nul. |
| `02.4 Solde après affectation` | Solde `2`. |
| `02.5 Retourner l'affectation` | `200`, `returned_at` et `returned_by` posés par le serveur. |
| `02.6 Article retourné` | `AVAILABLE`, entrepôt d'origine restauré. |
| `02.7 Solde après retour` | Solde `3`. |
| `02.8 Rejouer le retour` | `409`, code `ASSIGNMENT_ALREADY_RETURNED`. |
| `03.1 Solde source avant transfert` | Solde `3`. |
| `03.2 Article avant transfert` | `AVAILABLE`, encore dans l'entrepôt source. |
| `03.3 Mouvement `TRANSFER`` | `201`, type `TRANSFER`, quantité `1`, `source_warehouse_id` et `destination_warehouse_id` renseignés, `warehouse_id` aligné sur la destination. |
| `03.4 Article après transfert` | Statut inchangé (`AVAILABLE`), `current_warehouse` = entrepôt de destination. |
| `03.5 Solde source après transfert` | Solde `2` (`solde avant − 1`). |
| `03.6 Solde destination après transfert` | Solde `1`, ligne de solde créée par le transfert. |
| `03.7 Aucune affectation créée` | Aucune affectation pour l'article transféré. |
| `03.8 Historique` | Le mouvement créé est retrouvé. |
| `04.1 à 04.3` | Les trois articles sont présents, aucune affectation active ne subsiste, soldes finaux `2` (source) et `1` (destination). |

Points techniques à commenter pendant la démonstration :

- le refresh token est **opaque, à usage unique et stocké haché** : le
  serveur le consomme, en émet un nouveau et invalide l'ancien ;
- le corps du retour ne porte que `condition_on_return` ; la date et l'acteur
  du retour sont posés par le serveur ;
- une affectation mémorise l'entrepôt d'origine et le restaure au retour ;
- le solde est mis à jour dans la **même transaction** que le mouvement ou
  l'affectation ;
- un mouvement `OUT` crée l'affectation, passe l'article `ASSIGNED`, vide son
  entrepôt et décrémente le solde ;
- un mouvement `TRANSFER` fait **exactement l'inverse** : il déplace l'article
  d'un entrepôt à l'autre, laisse son statut inchangé et ne crée **aucune**
  affectation ; le solde source perd une unité, le solde destination en
  gagne une ;
- le journal des mouvements est **append-only** : `PATCH` et `DELETE` renvoient
  `409 IMMUTABLE_RESOURCE`, et un transfert se revient par un transfert inverse.

## 7. Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| `401` sur les routes protégées | `accessToken` vide ou périmé. | Exécuter `00 — Authentification` en premier. |
| `401` sur le refresh | Refresh token déjà consommé. | Rejouer le login. |
| `400` sur un login | Corps JSON invalide ou mauvais mot de passe. | Vérifier `login`, `password` et `Content-Type`. |
| `409` sur la création d'une affectation | L'article de la fixture est déjà affecté. | Relancer le reset. |
| `404` sur une association (rôle, permission) | La ressource a été supprimée avant l'association. | Respecter l'ordre des requêtes du dossier. |
| `404` sur un `GET` par identifiant | Variable d'identifiant vide (création échouée). | Consulter la réponse de la requête de création. |
| Solde différent de `3` ou `4` | Données modifiées par un passage précédent. | Relancer le reset. |
| `Connection refused` | Backend arrêté. | Démarrer le backend sur le port `8080`. |
| `FATAL: password authentication failed` | Identifiants PostgreSQL incorrects. | Renseigner `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. |
| `psql` introuvable | Client PostgreSQL absent du `PATH`. | Installer le client ou ajouter son dossier `bin` au `PATH`. |
| Accents mal affichés sous Windows PowerShell | Console non UTF-8. | Utiliser PowerShell 7 ou Windows Terminal ; le script est en UTF-8 avec BOM. |

## 8. Régénérer la collection API

La collection « API complète » est **dérivée des spécifications OpenAPI** du
dépôt : chaque opération documentée devient une requête. Après une évolution
des contrôleurs, régénérer les fichiers :

```bash
python scripts/postman/generate-postman-collection.py
```

Le générateur lit `docs/api/**/*.yaml` et écrit :

- `docs/postman/SSSM-API.postman_collection.json` ;
- `docs/postman/SSSM-API.postman_environment.json` ;
- `docs/postman/SSSM-API.postman_environment.recette.json`.

Les choix d'organisation (dossiers, variables, charges utiles, cibles des
requêtes mutantes) sont regroupés en tête du script.

## 9. Portée et sécurité

- Les identifiants `demo.admin` / `Demo@2024` et les secrets JWT par défaut
  sont réservés au **développement local**.
- Le script de reset est **idempotent** et **non destructif** pour les données
  de démonstration hors `DEMO-POSTMAN`.
- La collection API crée des ressources organisationnelles (utilisateurs,
  rôles, agences, clients…) qui sont supprimées par leurs propres requêtes
  `DELETE`. Un rechargement complet des données de démonstration les efface.
- La collection ne couvre pas les scénarios de pagination fine, de
  recherche plein texte ni de concurrence : ces points relèvent des tests
  d'intégration et des contrats OpenAPI.
