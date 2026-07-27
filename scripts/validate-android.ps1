param(
    [switch]$RunBuild,
    [string]$AndroidSdkDir = $env:ANDROID_HOME
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$androidRoot = Join-Path $repoRoot "android"
$gradlewBat = Join-Path $androidRoot "gradlew.bat"

$issues = New-Object System.Collections.Generic.List[string]

if (-not (Test-Path $gradlewBat)) {
    $issues.Add("Missing Android Gradle wrapper: android/gradlew.bat")
}

try {
    java -version *> $null
}
catch {
    $issues.Add("Java is not installed or not on PATH")
}

if ([string]::IsNullOrWhiteSpace($env:ANDROID_KEYSTORE_PATH)) {
    $issues.Add("ANDROID_KEYSTORE_PATH is not set")
}
if ([string]::IsNullOrWhiteSpace($env:ANDROID_KEYSTORE_PASSWORD)) {
    $issues.Add("ANDROID_KEYSTORE_PASSWORD is not set")
}
if ([string]::IsNullOrWhiteSpace($env:ANDROID_KEY_ALIAS)) {
    $issues.Add("ANDROID_KEY_ALIAS is not set")
}
if ([string]::IsNullOrWhiteSpace($env:ANDROID_KEY_PASSWORD)) {
    $issues.Add("ANDROID_KEY_PASSWORD is not set")
}

if ([string]::IsNullOrWhiteSpace($AndroidSdkDir) -and [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) {
    $issues.Add("Android SDK not configured. Set ANDROID_HOME or ANDROID_SDK_ROOT")
}

if ($issues.Count -gt 0) {
    Write-Host "Android validation failed with the following blockers:" -ForegroundColor Yellow
    $issues | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "Android prerequisites look good."

if ($RunBuild) {
    if (-not [string]::IsNullOrWhiteSpace($AndroidSdkDir)) {
        $sdkPath = ($AndroidSdkDir -replace '\\', '/')
        Set-Content -Path (Join-Path $androidRoot "local.properties") -Value "sdk.dir=$sdkPath" -Encoding ascii
    }

    Push-Location $androidRoot
    try {
        & $gradlewBat --no-daemon clean test assembleRelease bundleRelease
    }
    finally {
        Pop-Location
    }
}
