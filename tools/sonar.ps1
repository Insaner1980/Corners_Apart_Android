#Requires -Version 5.1

[CmdletBinding()]
param(
    [switch]$PlanOnly,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$SonarArgs
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [Console]::OutputEncoding

function Get-RepositoryRoot {
    param([string]$Start)

    $dir = (Resolve-Path -LiteralPath $Start).Path
    while (-not [string]::IsNullOrWhiteSpace($dir)) {
        if (Test-Path -LiteralPath (Join-Path $dir ".git")) {
            return $dir
        }

        $parent = Split-Path -Parent $dir
        if ([string]::IsNullOrWhiteSpace($parent) -or $parent -eq $dir) {
            return (Resolve-Path -LiteralPath $Start).Path
        }
        $dir = $parent
    }
}

function Invoke-SonarCli {
    param([string[]]$Arguments)

    $cli = Get-Command sonar.exe -CommandType Application -ErrorAction SilentlyContinue
    if ($null -eq $cli) {
        throw "sonar.exe ei loytynyt PATHista."
    }

    & $cli.Source @Arguments
    exit $(if ($null -ne $global:LASTEXITCODE) { [int]$global:LASTEXITCODE } else { 0 })
}

function Get-SonarProjectProperties {
    param([string]$RepoRoot)

    $path = Join-Path $RepoRoot "sonar-project.properties"
    if (-not (Test-Path -LiteralPath $path)) {
        throw "sonar-project.properties ei loytynyt: $path"
    }

    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $path -Encoding utf8) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 1) {
            continue
        }

        $key = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        $properties[$key] = $value
    }

    return $properties
}

function Test-GradleSonarTokenConfigured {
    $gradlePropertiesPaths = @(
        (Join-Path $env:USERPROFILE ".gradle\gradle.properties"),
        (Join-Path (Get-Location).Path "gradle.properties")
    )

    if (-not [string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
        return $true
    }

    foreach ($path in $gradlePropertiesPaths) {
        if (-not (Test-Path -LiteralPath $path)) {
            continue
        }

        if (Select-String -LiteralPath $path -Pattern "^\s*systemProp\.sonar\.token\s*=" -Quiet) {
            return $true
        }
    }

    return $false
}

if ($SonarArgs.Count -gt 0) {
    Invoke-SonarCli -Arguments $SonarArgs
}

$repoRoot = Get-RepositoryRoot -Start (Get-Location).Path
$sonarProperties = Get-SonarProjectProperties -RepoRoot $repoRoot
$reportsDir = Join-Path $repoRoot "reports"
$scanReport = Join-Path $reportsDir "sonar.txt"
$issuesReport = Join-Path $reportsDir "sonar-issues.json"
$projectKey = $sonarProperties["sonar.projectKey"]
$hostUrl = $sonarProperties["sonar.host.url"]

if ([string]::IsNullOrWhiteSpace($projectKey)) {
    throw "sonar.projectKey puuttuu sonar-project.properties-tiedostosta."
}

if ([string]::IsNullOrWhiteSpace($hostUrl)) {
    $hostUrl = "https://sonarcloud.io"
}

if ($PlanOnly) {
    Write-Output @(
        "sonar"
        "  - Gradle sonar: reports/sonar.txt"
        "  - optional sonar.exe issue export: reports/sonar-issues.json"
        "  - requires SONAR_TOKEN or systemProp.sonar.token for the full Gradle scan"
        "  - project: $projectKey"
        "  - host: $hostUrl"
    )
    exit 0
}

New-Item -ItemType Directory -Force -Path $reportsDir | Out-Null

Set-Content -LiteralPath $scanReport -Encoding utf8 -Value @(
    "sonar"
    "Root: $repoRoot"
    "Project: $projectKey"
    "Command: reports/sonar.txt :: .\gradlew.bat sonar"
    "Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    ""
)

Push-Location -LiteralPath $repoRoot
try {
    $env:SONAR_HOST_URL = if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { $hostUrl }

    if (-not (Test-GradleSonarTokenConfigured)) {
        Add-Content -LiteralPath $scanReport -Encoding utf8 -Value @(
            "Gradle-skannauksen token puuttuu."
            "SonarQube CLI:n 'sonar auth login' tallentaa tokenin OS Keychainiin CLI:ta varten, mutta Gradle SonarScanner tarvitsee SONAR_TOKEN-ymparistomuuttujan tai systemProp.sonar.token-arvon."
            "Aseta analyysitoken ja aja uudelleen: `$env:SONAR_TOKEN=`"...`"; sonar"
            ""
        )
        Get-Content -LiteralPath $scanReport
        exit 1
    }

    & .\gradlew.bat "sonar" "--console=plain" *>&1 |
        Tee-Object -FilePath $scanReport -Append |
        Out-Host
    $scanExitCode = if ($null -ne $global:LASTEXITCODE) { [int]$global:LASTEXITCODE } else { 0 }

    $cli = Get-Command sonar.exe -CommandType Application -ErrorAction SilentlyContinue
    if ($null -ne $cli) {
        & $cli.Source "list" "issues" "--project" $projectKey "--statuses" "OPEN,CONFIRMED" "--format" "json" *>&1 |
            Tee-Object -FilePath $issuesReport |
            Out-Null
    }

    exit $scanExitCode
}
finally {
    Pop-Location
}
