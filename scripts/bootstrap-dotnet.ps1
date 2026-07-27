param(
    [string]$SdkVersion = "8.0.423",
    [switch]$InstallEfTool,
    [switch]$PersistPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$dotnetRoot = Join-Path $HOME ".dotnet"
$dotnetExe = Join-Path $dotnetRoot "dotnet.exe"
$dotnetTools = Join-Path $dotnetRoot "tools"

function Add-PathIfMissing {
    param([string]$PathToAdd)
    if ([string]::IsNullOrWhiteSpace($PathToAdd)) { return }
    $parts = $env:PATH -split ';'
    if ($parts -notcontains $PathToAdd) {
        $env:PATH = "$PathToAdd;$env:PATH"
    }
}

function Install-DotNetSdk {
    param([string]$Version, [string]$InstallDir)
    $installScript = Join-Path $env:TEMP "dotnet-install.ps1"
    Invoke-WebRequest "https://dot.net/v1/dotnet-install.ps1" -OutFile $installScript
    & $installScript -Version $Version -InstallDir $InstallDir -NoPath -Architecture arm64
}

if (-not (Test-Path $dotnetExe)) {
    Write-Host "No local dotnet found at $dotnetRoot. Installing SDK $SdkVersion..."
    Install-DotNetSdk -Version $SdkVersion -InstallDir $dotnetRoot
}

Add-PathIfMissing -PathToAdd $dotnetRoot
Add-PathIfMissing -PathToAdd $dotnetTools

$sdks = & $dotnetExe --list-sdks
if (-not $sdks) {
    Write-Host "dotnet exists but no SDKs found. Installing SDK $SdkVersion..."
    Install-DotNetSdk -Version $SdkVersion -InstallDir $dotnetRoot
}

$nugetSources = & $dotnetExe nuget list source 2>$null
if (-not ($nugetSources -match "nuget.org")) {
    Write-Host "Adding NuGet source nuget.org..."
    & $dotnetExe nuget add source "https://api.nuget.org/v3/index.json" -n "nuget.org" | Out-Null
}

if ($InstallEfTool) {
    try {
        & $dotnetExe tool update --global dotnet-ef --version "8.*" | Out-Null
    }
    catch {
        & $dotnetExe tool install --global dotnet-ef --version "8.*" | Out-Null
    }
}

if ($PersistPath) {
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $toAdd = @($dotnetRoot, $dotnetTools) | Where-Object { $userPath -notmatch [regex]::Escape($_) }
    if ($toAdd.Count -gt 0) {
        [Environment]::SetEnvironmentVariable("Path", ($toAdd -join ';') + ";" + $userPath, "User")
    }
}

Write-Host "dotnet bootstrap complete."
& $dotnetExe --info
