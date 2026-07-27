param(
    [string]$ConnectionString = $env:MEDBILL_SQL_CONNECTION_STRING,
    [string]$Project = "backend/MedBill.Api/MedBill.Api.csproj",
    [string]$StartupProject = "backend/MedBill.Api/MedBill.Api.csproj"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

if ([string]::IsNullOrWhiteSpace($ConnectionString)) {
    throw "Connection string missing. Provide -ConnectionString or set MEDBILL_SQL_CONNECTION_STRING."
}

& (Join-Path $repoRoot "scripts/bootstrap-dotnet.ps1") -InstallEfTool

$env:ConnectionStrings__Sql = $ConnectionString
$env:PATH = "$HOME\.dotnet;$HOME\.dotnet\tools;$env:PATH"

Push-Location $repoRoot
try {
    dotnet ef database update --project $Project --startup-project $StartupProject
    dotnet ef migrations list --project $Project --startup-project $StartupProject
}
finally {
    Pop-Location
}
