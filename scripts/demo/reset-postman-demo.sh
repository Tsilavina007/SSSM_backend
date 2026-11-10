#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
SQL_FILE="${PROJECT_ROOT}/docs/database/mock_data/demo_postman.sql"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-sssm_db}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

printf '\n=== Réinitialisation des données de démonstration Postman ===\n'
printf 'Base    : %s\n' "${DB_NAME}"
printf 'Serveur : %s:%s\n' "${DB_HOST}" "${DB_PORT}"
printf 'Fichier : %s\n\n' "${SQL_FILE}"

if [[ ! -f "${SQL_FILE}" ]]; then
    printf '[ERROR] Script SQL introuvable : %s\n' "${SQL_FILE}"
    exit 1
fi

if ! command -v psql >/dev/null 2>&1; then
    printf '[ERROR] psql introuvable dans le PATH. Installez le client PostgreSQL et ajoutez-le au PATH.\n'
    exit 1
fi

if [[ -n "${DB_PASSWORD:-}" ]]; then
    export PGPASSWORD="${DB_PASSWORD}"
fi

psql \
    -X \
    -h "${DB_HOST}" \
    -p "${DB_PORT}" \
    -U "${DB_USER}" \
    -d "${DB_NAME}" \
    -v ON_ERROR_STOP=1 \
    -f "${SQL_FILE}"

printf '\n[OK] Données de démonstration Postman réinitialisées (solde initial : 3).\n'
