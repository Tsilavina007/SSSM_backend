# Intégration continue

Le workflow GitHub Actions [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)
vérifie chaque push et chaque pull request ciblant `dev` ou `main`. Il peut aussi
être lancé manuellement avec `workflow_dispatch`.

## Jobs

### `test` — Tests Java et PostgreSQL

- Java 17 (Temurin) et cache Maven.
- Service PostgreSQL 16 démarré par GitHub Actions.
- Création du schéma avec `doc/database/sssm_schema2_v01.sql`.
- Chargement des données de démonstration avec
  `doc/database/mock_data/sssm_mock_data.sql`.
- Exécution de `./mvnw -B -q test` avec `DB_URL`, `DB_USER`, `DB_PASSWORD` et
  `JWT_SECRET` fournis par l'environnement du job.
- Publication des rapports Surefire en artefact, même en cas d'échec.

### `lint` — Contrôles statiques Java et OpenAPI

- Contrôles Java existants via `bash scripts/check-before-push.sh --skip-tests`
  (packages, imports dupliqués, imports wildcard, espaces en fin de ligne,
  compilation).
- Validation des 26 spécifications OpenAPI avec Redocly CLI.
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
find doc/api -type f -name '*.yaml' -print0 \
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
