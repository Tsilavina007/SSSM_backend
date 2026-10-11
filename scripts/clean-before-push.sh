#!/usr/bin/env bash

set -uo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
SOURCE_ROOT="${PROJECT_ROOT}/src/main/java"
SKIP_TESTS=false

if [[ "${1:-}" == "--skip-tests" ]]; then
    SKIP_TESTS=true
elif [[ $# -gt 0 ]]; then
    printf '[ERROR] Option inconnue : %s\n' "$1"
    printf 'Usage : %s [--skip-tests]\n' "$0"
    exit 1
fi

printf '[INFO] Nettoyage des fichiers Java...\n'
changed_count=0

while IFS= read -r -d '' java_file; do
    temp_file="$(mktemp)"
    awk '
        /^[[:space:]]*import[[:space:]]+/ {
            sub(/^[[:space:]]+/, "")
            sub(/[[:space:]]+$/, "")
            if (seen[$0]++) next
        }
        {
            sub(/[[:space:]]+$/, "")
            if ($0 ~ /^[[:space:]]*$/) {
                if (blank) next
                blank = 1
            } else {
                blank = 0
            }
            lines[++count] = $0
        }
        END {
            while (count > 0 && lines[count] ~ /^[[:space:]]*$/) count--
            for (i = 1; i <= count; i++) print lines[i]
        }
    ' "${java_file}" > "${temp_file}"

    if ! cmp -s "${java_file}" "${temp_file}"; then
        cat "${temp_file}" > "${java_file}"
        changed_count=$((changed_count + 1))
    fi
    rm -f "${temp_file}"
done < <(find "${SOURCE_ROOT}" -type f -name '*.java' -print0)

if [[ "${changed_count}" -eq 0 ]]; then
    printf '[OK] Aucun nettoyage nécessaire.\n'
else
    printf '[OK] %d fichier(s) nettoyé(s).\n' "${changed_count}"
fi

printf '[INFO] Les imports wildcard et les espaces à l'\''intérieur du code ne sont pas modifiés automatiquement.\n'
printf '[INFO] Relancement du contrôle avant push...\n\n'

if [[ "${SKIP_TESTS}" == true ]]; then
    exec "${SCRIPT_DIR}/check-before-push.sh" --skip-tests
fi
exec "${SCRIPT_DIR}/check-before-push.sh"
