# Démonstration technique Postman

Cette démonstration couvre trois parcours du backend SSSM :

1. **Authentification** : génération des jetons au login, appel d'une route
   protégée, puis renouvellement avec **rotation** de l'access token et du
   refresh token.
2. **Affectation et retour** : affectation d'un article à un employé et à un
   poste, contrôle de l'état de l'article et du solde, puis retour avec
   restauration de l'entrepôt d'origine.
3. **Sortie de stock** : mouvement `OUT` avec affectation automatique,
   passage de l'article à `ASSIGNED` et décrémentation du solde.

La collection contient **21 requêtes** et **77 assertions** réparties en quatre
dossiers. Elle est prévue pour une base et un backend **locaux de
démonstration** : ne jamais l'utiliser contre une base de recette ou de
production.

## 1. Fichiers livrés

| Fichier | Rôle |
|---|---|
| `docs/postman/SSSM-Demo.postman_collection.json` | Collection Postman v2.1, importable directement. |
| `docs/postman/SSSM-Demo.postman_environment.json` | Environnement local : URL, compte et UUID des fixtures. |
| `docs/database/mock_data/demo_postman.sql` | Création et remise à zéro des fixtures `DEMO-POSTMAN`. |
| `scripts/demo/reset-postman-demo.ps1` | Wrapper Windows du script SQL. |
| `scripts/demo/reset-postman-demo.sh` | Wrapper Linux/macOS du script SQL. |

## 2. Prérequis

- **PostgreSQL** accessible (client `psql` dans le `PATH`).
- **Base de démonstration** déjà chargée avec le schéma
  (`docs/database/sssm_schema2_v01.sql`) et les données de démonstration
  (`docs/database/mock_data/sssm_mock_data.sql`).
- **Backend** démarré sur `http://localhost:8080`.
- **Postman** (application de bureau) ou **Newman** pour l'exécution en ligne
  de commande.
- Compte de démonstration : `demo.admin` / `Demo@2024`.

Vérifier que le backend répond :

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/v1/auth/whoami
```

Une réponse `401` est normale : la route exige un jeton. Un échec de connexion
signale en revanche que le backend n'est pas démarré.

## 3. Importer la collection et l'environnement

1. Ouvrir Postman, puis **Import**.
2. Sélectionner les deux fichiers :
   - `docs/postman/SSSM-Demo.postman_collection.json` ;
   - `docs/postman/SSSM-Demo.postman_environment.json`.
3. En haut à droite, sélectionner l'environnement
   **SSSM — Démo Postman (local)**.
4. Vérifier que `baseUrl` vaut bien `http://localhost:8080`.

### Variables d'environnement

| Variable | Valeur par défaut | Usage |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | URL du backend. |
| `login` | `demo.admin` | Compte de démonstration. |
| `password` | `Demo@2024` | Mot de passe (stocké comme secret Postman). |
| `warehouseId` | `d0e00000-…-0003` | Entrepôt démo. |
| `itemTypeId` | `d0e00000-…-0002` | Type `DEMO-RADIO`. |
| `employeeId` | `d0e00000-…-0006` | Employé démo. |
| `postId` | `d0e00000-…-0005` | Poste démo. |
| `itemAssignId` | `d0e00000-…-0007` | Article du parcours affectation/retour. |
| `itemOutId` | `d0e00000-…-0008` | Article du parcours sortie de stock. |
| `itemSpareId` | `d0e00000-…-0009` | Article témoin, jamais modifié. |

Les jetons, l'identifiant d'affectation, l'identifiant du mouvement et les
soldes intermédiaires sont capturés automatiquement dans les **variables de
collection**. Il n'y a rien à copier à la main.

## 4. Réinitialiser les données avant la démonstration

Le script SQL est **idempotent** et ne touche qu'aux lignes dont les codes sont
préfixés `DEMO-POSTMAN` ou dont les UUID sont réservés. Les autres données de
démonstration restent intactes.

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

Exemple avec une base dédiée :

```powershell
$env:DB_NAME = "sssm_demo"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "postgres"
.\scripts\demo\reset-postman-demo.ps1
```

En fin d'exécution, le script affiche un récapitulatif et notamment :

```text
Solde initial | 3 | DEMO-RADIO dans l'entrepôt démo
```

> Le reset supprime les affectations et mouvements des fixtures. Ne pas
> l'exécuter pendant que la démonstration est en cours.

## 5. Déroulé de la démonstration

Exécuter les dossiers **dans l'ordre**. On peut soit ouvrir chaque requête et
cliquer sur **Send**, soit utiliser **Run folder** pour un parcours complet.
Le Bearer de la collection est mis à jour automatiquement par les scripts de
test : aucune recopie de jeton n'est nécessaire.

### Parcours 01 — Authentification (4 requêtes)

| Requête | Action | Résultat attendu |
|---|---|---|
| `01.1 Login — génération des jetons` | `POST /api/v1/auth/login` | `200`, `token` et `refreshToken` non vides, utilisateur `demo.admin`, rôle `ADMIN`. |
| `01.2 Whoami — vérifier l'access token` | `GET /api/v1/auth/whoami` | `200`, même utilisateur : l'access token est accepté. |
| `01.3 Refresh — rotation des jetons` | `POST /api/v1/auth/refresh` | `200`, **nouvel** access token et **nouveau** refresh token, différent des précédents. |
| `01.4 Whoami — vérifier le nouveau token` | `GET /api/v1/auth/whoami` | `200`, toujours le même utilisateur avec le jeton renouvelé. |

Points à commenter pendant la démonstration :

- le login renvoie un **access token JWT** et un **refresh token opaque** ;
- le refresh s'envoie dans le corps (`{"refreshToken": "…"}`), sans en-tête
  `Authorization`, ce qui permet de renouveler un access token expiré ;
- le refresh token est **à usage unique** : le serveur le consomme, en émet un
  nouveau et invalide l'ancien. Rejouer l'ancien refresh token renvoie `401`.

Les valeurs précédentes sont conservées dans les variables de collection
`previousAccessToken` et `previousRefreshToken` : elles servent aux assertions
de rotation et peuvent être montrées dans la console Postman.

### Parcours 02 — Affectation et retour (8 requêtes)

| Requête | Action | Résultat attendu |
|---|---|---|
| `02.1 Vérifier l'article avant affectation` | `GET /inventory/items/{{itemAssignId}}` | `AVAILABLE`, entrepôt démo. |
| `02.2 Créer l'affectation` | `POST /item-assignments` | `201`, `returned_at` nul, `source_warehouse_id` renseigné, `assigned_by` renseigné. |
| `02.3 Vérifier l'article affecté` | `GET /inventory/items/{{itemAssignId}}` | `ASSIGNED`, `current_warehouse` nul. |
| `02.4 Vérifier le solde après affectation` | `GET /inventory/stock-balances` | Solde `2`. |
| `02.5 Retourner l'affectation` | `POST /item-assignments/{{assignmentId}}/return` | `200`, `returned_at` et `returned_by` posés par le serveur. |
| `02.6 Vérifier l'article retourné` | `GET /inventory/items/{{itemAssignId}}` | `AVAILABLE`, entrepôt démo restauré. |
| `02.7 Vérifier le solde après retour` | `GET /inventory/stock-balances` | Solde `3`. |
| `02.8 Rejouer le retour — 409 attendu` | `POST …/return` avec `{}` | `409`, code `ASSIGNMENT_ALREADY_RETURNED`. |

Points à commenter :

- le corps du retour suit `ItemAssignmentReturnRequest` : seul
  `condition_on_return` est lu ;
- `assigned_at`, `returned_at`, `assigned_by` et `returned_by` sont posés par
  le serveur, pas par le client ;
- l'affectation mémorise l'entrepôt d'origine (`source_warehouse_id`) pour le
  restaurer au retour ;
- le solde est mis à jour dans la même transaction que l'affectation et le
  retour ;
- le second retour est un **test négatif volontaire** : il prouve l'idempotence
  métier et le code d'erreur stable.

### Parcours 03 — Sortie de stock et solde (6 requêtes)

| Requête | Action | Résultat attendu |
|---|---|---|
| `03.1 Vérifier le solde avant sortie` | `GET /inventory/stock-balances` | Solde `3`. |
| `03.2 Créer le mouvement OUT` | `POST /inventory/movements` | `201`, type `OUT`, quantité `1`, `source_warehouse_id` renseigné, `performed_by` renseigné. |
| `03.3 Vérifier l'article sorti` | `GET /inventory/items/{{itemOutId}}` | `ASSIGNED`, `current_warehouse` nul. |
| `03.4 Vérifier le solde après sortie` | `GET /inventory/stock-balances` | Solde `2`. |
| `03.5 Vérifier l'affectation automatique` | `GET /item-assignments/item/{{itemOutId}}` | Une affectation active existe, avec l'entrepôt d'origine et `assigned_by`. |
| `03.6 Retrouver le mouvement dans l'historique` | `GET /inventory/movements?search=DEMO-POSTMAN-OUT` | Le mouvement créé est présent. |

Points à commenter :

- un `OUT` exige `warehouse_id` (ou `source_warehouse_id`), `item_id`,
  `employee_id` et `post_id` ;
- la sortie est une **opération métier atomique** : création du mouvement,
  création de l'affectation, passage de l'article à `ASSIGNED`, vidage de son
  entrepôt et décrémentation du solde ;
- le journal des mouvements est **append-only** : le mouvement reste
  consultable après la démonstration.

### Parcours 04 — Lecture d'ensemble (3 requêtes)

| Requête | Résultat attendu |
|---|---|
| `04.1 Lister les articles de démo` | Les trois articles `DEMO-POSTMAN-*` sont présents ; `DEMO-POSTMAN-OUT` est `ASSIGNED`. |
| `04.2 Lister les affectations actives` | L'affectation créée automatiquement par le `OUT` figure dans la liste. |
| `04.3 Lister les soldes de démo` | Solde final `2`. |

## 6. États attendus

| Étape | Article `ASSIGN` | Article `OUT` | Article `SPARE` | Solde `DEMO-RADIO` |
|---|---|---|---|---|
| Après reset | `AVAILABLE` | `AVAILABLE` | `AVAILABLE` | `3` |
| Après affectation (02.2) | `ASSIGNED` | `AVAILABLE` | `AVAILABLE` | `2` |
| Après retour (02.5) | `AVAILABLE` | `AVAILABLE` | `AVAILABLE` | `3` |
| Après sortie (03.2) | `AVAILABLE` | `ASSIGNED` | `AVAILABLE` | `2` |

À la fin de la démonstration, l'article `DEMO-POSTMAN-OUT` reste affecté et le
solde reste à `2`. **Relancer le reset avant toute nouvelle démonstration
complète.**

## 7. Exécution en ligne de commande avec Newman

Installer Newman si nécessaire :

```bash
npm install --global newman
```

Puis, depuis la racine du projet :

```bash
newman run docs/postman/SSSM-Demo.postman_collection.json \
  -e docs/postman/SSSM-Demo.postman_environment.json \
  --reporters cli
```

Résultat attendu après un reset :

```text
requests:   21 executed
assertions: 77 executed, 0 failed
```

Le code de sortie de Newman est `0` si toutes les assertions passent. Penser à
relancer le reset entre deux exécutions complètes.

## 8. Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| `401` sur `01.2` ou `01.4` | `accessToken` vide ou périmé. | Exécuter `01.1`, puis `01.3` dans l'ordre. |
| `401` sur `01.3` | Refresh token déjà consommé ou absent. | Rejouer `01.1` pour obtenir un nouveau refresh token. |
| `400` sur `01.1` | Corps JSON invalide ou mauvais mot de passe. | Vérifier `login`, `password` et `Content-Type: application/json`. |
| `409 ASSIGNMENT_ALREADY_RETURNED` sur `02.5` | Les données n'ont pas été remises à zéro. | Relancer le reset, puis reprendre au début. |
| `02.1` renvoie `ASSIGNED` ou un entrepôt nul | Une démonstration précédente n'a pas été réinitialisée. | Relancer le reset. |
| Solde différent de `3` en `02.4` ou `03.1` | Données modifiées par un passage précédent. | Relancer le reset. |
| Erreur d'affectation active sur `03.2` | L'article `OUT` est déjà affecté. | Relancer le reset. |
| `Connection refused` | Backend arrêté. | Démarrer le backend sur le port `8080`. |
| `FATAL: password authentication failed` | Identifiants PostgreSQL incorrects. | Renseigner `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. |
| `psql` introuvable | Client PostgreSQL absent du `PATH`. | Installer le client PostgreSQL ou ajouter son dossier `bin` au `PATH`. |
| Accents mal affichés sous Windows PowerShell | Console non UTF-8. | Utiliser PowerShell 7 ou Windows Terminal ; le script est en UTF-8 avec BOM. |

## 9. Portée et sécurité

- Les fixtures sont **isolées** : UUID fixes et préfixe `DEMO-POSTMAN`, aucune
  modification des autres articles, employés, entrepôts ou mouvements.
- Le script de reset est **idempotent** : il peut être rejoué sans dupliquer de
  données.
- Les identifiants `demo.admin` / `Demo@2024` et les secrets JWT par défaut sont
  réservés au **développement local**. Ne pas les réutiliser en recette ou en
  production.
- La collection ne teste pas l'inscription, le logout, les rôles fins ni les
  erreurs de validation : ces points relèvent des tests d'intégration et des
  contrats OpenAPI du dépôt.
