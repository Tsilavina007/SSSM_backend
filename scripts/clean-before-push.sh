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
            import_lines[++import_count] = $0
            next
        }
        /^[[:space:]]*package[[:space:]]+/ {
            package_lines[++package_count] = $0
            next
        }
        {
            body[++body_count] = $0
        }
        END {
            for (i = 1; i <= package_count; i++) {
                line = package_lines[i]
                sub(/^[[:space:]]*/, "", line)
                sub(/[[:space:]]+$/, "", line)
                print line
            }
            if (package_count > 0 && import_count > 0) print ""
            for (i = 1; i <= import_count; i++) {
                line = import_lines[i]
                normalized = line
                sub(/^[[:space:]]*/, "", normalized)
                sub(/[[:space:]]+$/, "", normalized)
                if (seen[normalized]++) continue
                import_name = normalized
                sub(/^import[[:space:]]+static[[:space:]]+/, "", import_name)
                sub(/^import[[:space:]]+/, "", import_name)
                sub(/;$/, "", import_name)
                if (import_name ~ /\.\*$/) {
                    if (normalized ~ /^import[[:space:]]+(static[[:space:]]+)?sssm\.dev\.sssm_backend\./) {
                        local_imports[++local_count] = normalized
                    } else {
                        external_imports[++external_count] = normalized
                    }
                    continue
                }
                split(import_name, parts, ".")
                simple_name = parts[length(parts)]
                used = 0
                pattern = "(^|[^[:alnum:]_$])" simple_name "([^[:alnum:]_$]|$)"
                for (j = 1; j <= body_count; j++) {
                    if (body[j] ~ pattern) {
                        used = 1
                        break
                    }
                }
                if (used) {
                    if (normalized ~ /^import[[:space:]]+(static[[:space:]]+)?sssm\.dev\.sssm_backend\./) {
                        local_imports[++local_count] = normalized
                    } else {
                        external_imports[++external_count] = normalized
                    }
                }
            }
            for (i = 1; i <= external_count; i++) print external_imports[i]
            if (external_count > 0 && local_count > 0) print ""
            for (i = 1; i <= local_count; i++) print local_imports[i]
            if (import_count > 0) print ""
            if (package_count > 0 || import_count > 0) blank = 1
            for (i = 1; i <= body_count; i++) {
                line = body[i]
                sub(/[[:space:]]+$/, "", line)
                if (line ~ /^[[:space:]]*$/) {
                    if (blank) continue
                    blank = 1
                } else {
                    blank = 0
                }
                lines[++count] = line
            }
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
