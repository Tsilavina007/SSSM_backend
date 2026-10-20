#!/usr/bin/env bash

set -uo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
SOURCE_ROOT="${PROJECT_ROOT}/src/main/java"
MVN_WRAPPER="${PROJECT_ROOT}/mvnw"
SKIP_TESTS=false

declare -a FAILURES=()
declare -a WARNINGS=()

if [[ "${1:-}" == "--skip-tests" ]]; then
    SKIP_TESTS=true
elif [[ $# -gt 0 ]]; then
    echo "[ERROR] Option inconnue : $1"
    echo "Usage : $0 [--skip-tests]"
    exit 1
fi

info() {
    printf '[INFO] %s\n' "$1"
}

ok() {
    printf '[OK] %s\n' "$1"
}

error() {
    printf '[ERROR] %s\n' "$1"
}

warn() {
    printf '[WARN] %s\n' "$1"
}

add_failure() {
    FAILURES+=("$1")
}

add_warning() {
    WARNINGS+=("$1")
}

check_java_sources() {
    local java_file relative_path expected_package package_declaration line_number line
    local import duplicate wildcard

    info "Contrôles statiques des fichiers Java..."

    while IFS= read -r -d '' java_file; do
        relative_path="${java_file#"${SOURCE_ROOT}"/}"
        expected_package="$(dirname "${relative_path}" | tr '/' '.')"
        package_declaration="$(sed -n 's/^[[:space:]]*package[[:space:]]\+\([^;]*\);.*/\1/p' "${java_file}" | head -n 1)"

        if [[ -z "${package_declaration}" ]]; then
            add_failure "${relative_path} : déclaration package absente."
        elif [[ "${package_declaration}" != "${expected_package}" ]]; then
            add_failure "${relative_path} : package différent du chemin attendu (${expected_package})."
        fi

        while IFS= read -r duplicate; do
            [[ -n "${duplicate}" ]] || continue
            add_failure "${relative_path} : import dupliqué (${duplicate})."
        done < <(
            grep -E '^[[:space:]]*import[[:space:]]+' "${java_file}" |
                sed 's/^[[:space:]]*//' |
                sort |
                uniq -d
        )

        if grep -qE '^[[:space:]]*import[[:space:]].*\.\*;' "${java_file}"; then
            wildcard="$(grep -E '^[[:space:]]*import[[:space:]].*\.\*;' "${java_file}" | head -n 1 | sed 's/^[[:space:]]*//')"
            add_warning "${relative_path} : import wildcard détecté (${wildcard})."
        fi

        line_number=0
        while IFS= read -r line || [[ -n "${line}" ]]; do
            line_number=$((line_number + 1))
            if [[ "${line}" =~ [[:space:]]$ ]]; then
                add_warning "${relative_path}:${line_number} : espaces en fin de ligne."
            fi
        done < "${java_file}"
    done < <(find "${SOURCE_ROOT}" -type f -name '*.java' -print0)

    if [[ ${#FAILURES[@]} -eq 0 ]]; then
        local java_count
        java_count="$(find "${SOURCE_ROOT}" -type f -name '*.java' | wc -l | tr -d ' ')"
        ok "Contrôles statiques Java réussis (${java_count} fichiers)."
    fi
}

run_maven_check() {
    local name="$1"
    shift

    info "${name}..."
    if ! (cd "${PROJECT_ROOT}" && bash "${MVN_WRAPPER}" "$@"); then
        add_failure "${name} échoué."
    else
        ok "${name} réussi."
    fi
}

printf '\n=== Vérification avant push : sssm_backend ===\n'
printf 'Projet : %s\n\n' "${PROJECT_ROOT}"

if [[ ! -f "${MVN_WRAPPER}" ]]; then
    add_failure "mvnw introuvable dans ${PROJECT_ROOT}."
else
    check_java_sources
    run_maven_check "Validation Maven" validate -q

    if [[ "${SKIP_TESTS}" == true ]]; then
        run_maven_check "Compilation Maven" -q -DskipTests compile
    else
        run_maven_check "Compilation et tests Maven" -q test
    fi
fi

printf '\n'

if [[ ${#WARNINGS[@]} -gt 0 ]]; then
    printf '=== Avertissements détectés ===\n'
    for index in "${!WARNINGS[@]}"; do
        printf '%d. %s\n' "$((index + 1))" "${WARNINGS[$index]}"
    done
    printf '\n'
    warn "Vérification réussie avec ${#WARNINGS[@]} avertissement(s) de style."
fi

if [[ ${#FAILURES[@]} -gt 0 ]]; then
    printf '=== Erreurs détectées ===\n'
    for index in "${!FAILURES[@]}"; do
        printf '%d. %s\n' "$((index + 1))" "${FAILURES[$index]}"
    done
    printf '\n'
    error "Vérification échouée : ${#FAILURES[@]} erreur(s). Le push doit être corrigé avant de continuer."
    exit 1
fi

ok "Toutes les vérifications sont réussies. Le push peut continuer."
exit 0
