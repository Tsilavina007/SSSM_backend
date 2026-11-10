[CmdletBinding()]
param(
    [string]$DbHost = $(if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }),
    [string]$DbPort = $(if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }),
    [string]$Database = $(if ($env:DB_NAME) { $env:DB_NAME } else { "sssm_db" }),
    [string]$DbUser = $(if ($env:DB_USER) { $env:DB_USER } else { "postgres" }),
    [string]$DbPassword = $(if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" })
)

$ErrorActionPreference = "Stop"

if ($PSVersionTable.PSVersion.Major -lt 6) {
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
}

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$sqlFile = Join-Path $projectRoot "docs\database\mock_data\demo_postman.sql"
$exitCode = 0

Write-Host ""
Write-Host "=== Réinitialisation des données de démonstration Postman ===" -ForegroundColor White
Write-Host "Base    : $Database"
Write-Host "Serveur : ${DbHost}:${DbPort}"
Write-Host "Fichier : $sqlFile"
Write-Host ""

if (-not (Test-Path $sqlFile)) {
    Write-Host "[ERROR] Script SQL introuvable : $sqlFile" -ForegroundColor Red
    exit 1
}

$psqlCommand = Get-Command psql -ErrorAction SilentlyContinue
if ($null -eq $psqlCommand) {
    Write-Host "[ERROR] psql introuvable dans le PATH. Installez le client PostgreSQL et ajoutez-le au PATH." -ForegroundColor Red
    exit 1
}

$previousPassword = $env:PGPASSWORD
try {
    if (-not [string]::IsNullOrWhiteSpace($DbPassword)) {
        $env:PGPASSWORD = $DbPassword
    }

    & $psqlCommand.Source `
        -X `
        -h $DbHost `
        -p $DbPort `
        -U $DbUser `
        -d $Database `
        -v ON_ERROR_STOP=1 `
        -f $sqlFile

    $exitCode = $LASTEXITCODE
} catch {
    Write-Host "[ERROR] Échec de l'exécution de psql : $($_.Exception.Message)" -ForegroundColor Red
    $exitCode = 1
} finally {
    if ($null -eq $previousPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    } else {
        $env:PGPASSWORD = $previousPassword
    }
}

Write-Host ""
if ($exitCode -eq 0) {
    Write-Host "[OK] Données de démonstration Postman réinitialisées (solde initial : 3)." -ForegroundColor Green
} else {
    Write-Host "[ERROR] La réinitialisation a échoué (code de sortie $exitCode)." -ForegroundColor Red
}

exit $exitCode
