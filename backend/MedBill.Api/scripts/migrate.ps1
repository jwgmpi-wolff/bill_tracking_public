param(
    [string]$ProjectPath = "../MedBill.Api",
    [string]$StartupProjectPath = "../MedBill.Api",
    [string]$MigrationName = "InitialCreate",
    [string]$ConnectionString = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Push-Location $PSScriptRoot
try {
    $repoRoot = Resolve-Path (Join-Path $PSScriptRoot "../../..")
    $bootstrapScript = Join-Path $repoRoot "scripts/bootstrap-dotnet.ps1"
    if (Test-Path $bootstrapScript) {
        & $bootstrapScript -InstallEfTool
    }

    $env:PATH = "$HOME\.dotnet;$HOME\.dotnet\tools;$env:PATH"
    if (-not [string]::IsNullOrWhiteSpace($ConnectionString)) {
        $env:ConnectionStrings__Sql = $ConnectionString
    }

    dotnet ef migrations add $MigrationName --project $ProjectPath --startup-project $StartupProjectPath
    dotnet ef database update --project $ProjectPath --startup-project $StartupProjectPath
    dotnet ef migrations script --idempotent --project $ProjectPath --startup-project $StartupProjectPath --output "./migration.sql"
    Write-Host "Migrations applied and SQL script generated at backend/MedBill.Api/scripts/migration.sql"
}
finally {
    Pop-Location
}
