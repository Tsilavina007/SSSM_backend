[CmdletBinding()]
param(
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $projectRoot "src\main\java"
$mavenWrapper = Join-Path $projectRoot "mvnw.cmd"
$failures = [System.Collections.Generic.List[string]]::new()
$warnings = [System.Collections.Generic.List[string]]::new()

function Write-CheckMessage {
    param(
        [ValidateSet("INFO", "OK", "ERROR", "WARN")]
        [string]$Level,
        [string]$Message
    )

    $color = switch ($Level) {
        "OK" { "Green" }
        "ERROR" { "Red" }
        "WARN" { "Yellow" }
        default { "Cyan" }
    }

    Write-Host "[$Level] $Message" -ForegroundColor $color
}

function Add-Failure {
    param([string]$Message)

    $failures.Add($Message)
}

function Add-Warning {
    param([string]$Message)

    $warnings.Add($Message)
}

function Test-JavaSource {
    Write-CheckMessage -Level "INFO" -Message "Contrôles statiques des fichiers Java..."
    $javaFiles = @(Get-ChildItem -Path $sourceRoot -Filter "*.java" -Recurse -File)

    foreach ($file in $javaFiles) {
        $relativePath = $file.FullName.Substring($sourceRoot.Length + 1)
        $expectedPackage = ($relativePath -replace "\\[^\\]+$", "" -replace "\\", ".")
        $content = Get-Content -Path $file.FullName
        $packageLine = $content | Where-Object { $_ -match "^\s*package\s+([^;]+);" } | Select-Object -First 1

        if ($null -eq $packageLine) {
            Add-Failure "$relativePath : déclaration package absente."
        } elseif ($packageLine -notmatch "^\s*package\s+$([regex]::Escape($expectedPackage))\s*;") {
            Add-Failure "$relativePath : package différent du chemin attendu ($expectedPackage)."
        }

        $duplicateImports = @(
            $content |
                Where-Object { $_ -match "^\s*import\s+" } |
                ForEach-Object { $_.Trim() } |
                Group-Object |
                Where-Object { $_.Count -gt 1 }
        )
        foreach ($duplicate in $duplicateImports) {
            Add-Failure "$relativePath : import dupliqué ($($duplicate.Name))."
        }

        $wildcardImports = @($content | Where-Object { $_ -match "^\s*import\s+.*\.\*;" })
        if ($wildcardImports.Count -gt 0) {
            Add-Warning "$relativePath : import wildcard détecté ($($wildcardImports[0].Trim()))."
        }

        $lineNumber = 0
        foreach ($line in $content) {
            $lineNumber++
            if ($line -match "\s+$") {
                Add-Warning "${relativePath}:$lineNumber : espaces en fin de ligne."
            }
        }
    }

    if ($failures.Count -eq 0) {
        Write-CheckMessage -Level "OK" -Message "Contrôles statiques Java réussis ($($javaFiles.Count) fichiers)."
    }
}

function Invoke-MavenCheck {
    param(
        [string]$Name,
        [string[]]$Arguments
    )

    Write-CheckMessage -Level "INFO" -Message "$Name..."
    Push-Location $projectRoot
    try {
        & $mavenWrapper @Arguments
        if ($LASTEXITCODE -ne 0) {
            Add-Failure "$Name échoué (code de sortie $LASTEXITCODE)."
        } else {
            Write-CheckMessage -Level "OK" -Message "$Name réussi."
        }
    } catch {
        Add-Failure "$Name impossible à exécuter : $($_.Exception.Message)"
    } finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "=== Vérification avant push : sssm_backend ===" -ForegroundColor White
Write-Host "Projet : $projectRoot"
Write-Host ""

if (-not (Test-Path $mavenWrapper)) {
    Add-Failure "mvnw.cmd introuvable dans $projectRoot."
} else {
    Test-JavaSource
    Invoke-MavenCheck -Name "Validation Maven" -Arguments @("validate", "-q")

    if ($SkipTests) {
        Invoke-MavenCheck -Name "Compilation Maven" -Arguments @("-q", "-DskipTests", "compile")
    } else {
        Invoke-MavenCheck -Name "Compilation et tests Maven" -Arguments @("-q", "test")
    }
}

Write-Host ""
if ($warnings.Count -gt 0) {
    Write-Host "=== Avertissements détectés ===" -ForegroundColor Yellow
    for ($index = 0; $index -lt $warnings.Count; $index++) {
        Write-Host "$($index + 1). $($warnings[$index])" -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($failures.Count -gt 0) {
    Write-Host "=== Erreurs détectées ===" -ForegroundColor Red
    for ($index = 0; $index -lt $failures.Count; $index++) {
        Write-Host "$($index + 1). $($failures[$index])" -ForegroundColor Red
    }
    Write-Host ""
    Write-CheckMessage -Level "ERROR" -Message "Vérification échouée : $($failures.Count) erreur(s). Le push doit être corrigé avant de continuer."
    if ($warnings.Count -gt 0) {
        Write-CheckMessage -Level "WARN" -Message "$($warnings.Count) avertissement(s) de style détecté(s)."
    }
    exit 1
}

if ($warnings.Count -gt 0) {
    Write-CheckMessage -Level "WARN" -Message "Vérification réussie avec $($warnings.Count) avertissement(s) de style."
}

Write-CheckMessage -Level "OK" -Message "Toutes les vérifications sont réussies. Le push peut continuer."
exit 0
