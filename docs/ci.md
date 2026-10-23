# Intégration continue

Le workflow GitHub Actions [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)
vérifie chaque push et chaque pull request ciblant `dev` ou `main`. Il peut aussi
être lancé manuellement avec `workflow_dispatch`.

## Jobs

### `test` — Tests Java et PostgreSQL

- Java 17 (Temurin) et cache Maven.
- Service PostgreSQL 16 démarré par GitHub Actions.
- Création du schéma avec `docs/database/sssm_schema2_v01.sql`.
- Chargement des données de démonstration avec
  `docs/database/mock_data/sssm_mock_data.sql`.
- Garde-fou de schéma : vérification que l'enum `security.movement_type`
  contient bien `DESTRUCTION` (utilisé par les mouvements et l'openapi).
- Exécution de `./mvnw -B -q test` avec `DB_URL`, `DB_USER`, `DB_PASSWORD` et
  `JWT_SECRET` fournis par l'environnement du job.
- Publication des rapports Surefire en artefact, même en cas d'échec.

La suite est volontairement réduite : **7 classes, 49 tests**. Elle ne
vérifie pas tous les comportements métier, mais la conformité entre le
schéma SQL appliqué et ce que l'application attend, le contrat du scan QR
(tenant-scoped, enrichi, journalisé, limité en débit) et le contrat des
comptages (instantané figé, périmètre, clôture idempotente, ajustements,
annulation, pagination).

| Classe | Tests | Objet |
|---|---:|---|
| `DatabaseSchemaConformanceTests` | 15 | Tables lues par les DAO, colonnes exposées par l'API, énumérations Java ↔ PostgreSQL, types des colonnes du cycle de vie des affectations, colonnes d'archivage, tables du stock, clés étrangères, index unique partiel, application des règles de void, clés primaires, journal `qr_scan`, instantané des comptages, tables de la base ↔ scripts SQL |
| `SchemaScriptsConformanceTests` | 5 | Inclusion de tous les scripts par le runner principal, absence d'opération destructive, ordre et unicité des scripts, tables ciblées par les données de démonstration, contenu de la migration Lot 4 |
| `SssmBackendApplicationTests` | 1 | Démarrage du contexte Spring |
| `QrScanApiTests` | 13 | Résolution enrichie, 404 inconnu, 404 code d'une autre entreprise, journal, code immuable, cache et 304, étiquette et planche |
| `QrScanRateLimiterTests` | 4 | Limitation de débit des résolutions de QR code |
| `InventoryCountApiTests` | 10 | Instantané figé, article hors périmètre, cloisonnement, comptage vide, clôture idempotente, ajustements et solde, annulation, pagination, comptage inconnu |
| `PingControllerTest` | 1 | Endpoint technique `/ping` |

Les tests de conformité de schéma n'utilisent pas Spring : ils ouvrent une
connexion JDBC avec la même configuration que l'application (`DB_URL`,
`DB_USER`, `DB_PASSWORD`), ce qui les rend rapides et indépendants du contexte
applicatif. Les tests d'API (`QrScanApiTests`, `InventoryCountApiTests`)
utilisent `@SpringBootTest` et MockMvc sur la même base.

### `lint` — Contrôles statiques Java et OpenAPI

- Contrôles Java existants via `bash scripts/check-before-push.sh --skip-tests`
  (packages, imports dupliqués, imports wildcard, espaces en fin de ligne,
  compilation).
- Validation des 27 spécifications OpenAPI avec Redocly CLI.
- Les avertissements Redocly attendus (`license`, `localhost`, `operationId`)
  ne bloquent pas le job ; les erreurs de structure ou de référence échouent.

### `package` — Artefact exécutable

- Déclenché uniquement si les jobs `test` et `lint` réussissent.
- Exécution de `./mvnw -B -q -DskipTests package`.
- Publication du jar Spring Boot en artefact.

## Protection de branche

Pour empêcher un merge sans vérification, activer la protection de `dev` et de
`main` dans GitHub et rendre obligatoires les checks suivants :

- `Tests (Java 17 + PostgreSQL)` ;
- `Lint (Java + OpenAPI)` ;
- `Package`.

## Reproduction locale

Les commandes principales peuvent être rejouées localement :

```bash
./mvnw -B -q test
bash scripts/check-before-push.sh --skip-tests
find docs/api -type f -name '*.yaml' -print0 \
  | xargs -0 npx --yes @redocly/cli@2.65.1 lint --format=summary
```

Pour les tests d'intégration, une base PostgreSQL doit être accessible et
initialisée avec le schéma et les données de démonstration. Le workflow utilise
la configuration suivante :

```text
DB_URL=jdbc:postgresql://127.0.0.1:5432/sssm_db
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=ci-only-jwt-secret-with-at-least-32-characters
```

## Formatage Java

Le job `lint` ne lance pas `fmt:format` globalement : le dépôt n'est pas encore
formaté avec cet outil et la version actuelle du plugin ne prend pas en charge
toutes les syntaxes Java 17 utilisées (records, pattern matching). Les contrôles
statiques et la compilation restent exécutés en CI.
