[CmdletBinding()]
param(
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $projectRoot "src\main\java"
$checker = Join-Path $PSScriptRoot "check-before-push.ps1"
$utf8Bom = New-Object System.Text.UTF8Encoding($true)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$changedFiles = [System.Collections.Generic.List[string]]::new()

function Write-CleanMessage {
    param(
        [ValidateSet("INFO", "OK", "WARN")]
        [string]$Level,
        [string]$Message
    )

    $color = switch ($Level) {
        "OK" { "Green" }
        "WARN" { "Yellow" }
        default { "Cyan" }
    }

    Write-Host "[$Level] $Message" -ForegroundColor $color
}

Write-CleanMessage -Level "INFO" -Message "Nettoyage des fichiers Java..."

foreach ($file in @(Get-ChildItem -Path $sourceRoot -Filter "*.java" -Recurse -File)) {
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    $hasBom = $bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191
    $original = [System.IO.File]::ReadAllText($file.FullName)
    $lines = $original -split "`r?`n", -1
    $seenImports = [System.Collections.Generic.HashSet[string]]::new()
    $cleanLines = [System.Collections.Generic.List[string]]::new()
    $blankLinePending = $false
    $importLines = @($lines | Where-Object { $_ -match "^\s*import\s+" })
    $body = ($lines | Where-Object { $_ -notmatch "^\s*import\s+" }) -join "`n"
    $packageLines = @($lines | Where-Object { $_ -match "^\s*package\s+" })
    $keptImports = [System.Collections.Generic.List[string]]::new()
    $externalImports = [System.Collections.Generic.List[string]]::new()
    $localImports = [System.Collections.Generic.List[string]]::new()

    foreach ($importLine in $importLines) {
        if ($importLine -match "^\s*import\s+(?!static\s+)([^;]+)\s*;\s*$" -and $Matches[1] -notmatch "\.\*$") {
            $importName = ($Matches[1] -split "\.")[-1]
            $escapedName = [regex]::Escape($importName)
            if ($body -notmatch "(?m)(?<![\w\$])$escapedName(?![\w\$])") {
                continue
            }
        } elseif ($importLine -match "^\s*import\s+static\s+([^;]+)\s*;\s*$" -and $Matches[1] -notmatch "\.\*$") {
            $importName = ($Matches[1] -split "\.")[-1]
            $escapedName = [regex]::Escape($importName)
            if ($body -notmatch "(?m)(?<![\w\$])$escapedName(?![\w\$])") {
                continue
            }
        }
        $normalizedImport = $importLine.Trim()
        if ($seenImports.Add($normalizedImport)) {
            $keptImports.Add($normalizedImport)
            if ($normalizedImport -match "^import\s+(?:static\s+)?sssm\.dev\.sssm_backend\.") {
                $localImports.Add($normalizedImport)
            } else {
                $externalImports.Add($normalizedImport)
            }
        }
    }

    foreach ($packageLine in $packageLines) {
        $cleanLines.Add($packageLine.Trim())
    }
    if ($packageLines.Count -gt 0 -and ($externalImports.Count -gt 0 -or $localImports.Count -gt 0)) {
        $cleanLines.Add("")
    }
    foreach ($importLine in $externalImports) {
        $cleanLines.Add($importLine)
    }
    if ($externalImports.Count -gt 0 -and $localImports.Count -gt 0) {
        $cleanLines.Add("")
    }
    foreach ($importLine in $localImports) {
        $cleanLines.Add($importLine)
    }
    if ($keptImports.Count -gt 0) {
        $cleanLines.Add("")
        $blankLinePending = $true
    } elseif ($packageLines.Count -gt 0) {
        $blankLinePending = $true
    }

    foreach ($line in $lines) {
        if ($line -match "^\s*(package|import)\s+") {
            continue
        }
        $cleanLine = $line -replace "[ \t]+$", ""

        if ([string]::IsNullOrWhiteSpace($cleanLine)) {
            if ($blankLinePending) {
                continue
            }
            $blankLinePending = $true
        } else {
            $blankLinePending = $false
        }

        $cleanLines.Add($cleanLine)
    }

    while ($cleanLines.Count -gt 0 -and [string]::IsNullOrWhiteSpace($cleanLines[$cleanLines.Count - 1])) {
        $cleanLines.RemoveAt($cleanLines.Count - 1)
    }

    $cleaned = ($cleanLines -join [Environment]::NewLine) + [Environment]::NewLine
    if ($cleaned -cne $original) {
        $encoding = if ($hasBom) { $utf8Bom } else { $utf8NoBom }
        [System.IO.File]::WriteAllText($file.FullName, $cleaned, $encoding)
        $relativePath = $file.FullName.Substring($projectRoot.Length + 1)
        $changedFiles.Add($relativePath)
    }
}

if ($changedFiles.Count -eq 0) {
    Write-CleanMessage -Level "OK" -Message "Aucun nettoyage nécessaire."
} else {
    Write-CleanMessage -Level "OK" -Message "$($changedFiles.Count) fichier(s) nettoyé(s)."
    foreach ($file in $changedFiles) {
        Write-Host " - $file"
    }
}

Write-Host ""
Write-CleanMessage -Level "INFO" -Message "Les imports wildcard et les espaces à l'intérieur du code ne sont pas modifiés automatiquement."
Write-CleanMessage -Level "INFO" -Message "Relancement du contrôle avant push..."

if ($SkipTests) {
    & $checker -SkipTests
} else {
    & $checker
}
exit $LASTEXITCODE
