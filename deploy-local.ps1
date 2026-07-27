param(
    [int]$BackendPort = 5199,
    [switch]$NoRestore
)

$ErrorActionPreference = 'Stop'
$repoRoot = $PSScriptRoot
Set-Location $repoRoot

if (-not $NoRestore) {
    Write-Host 'Restoring backend dependencies...' -ForegroundColor Cyan
    dotnet restore .\backend\MedBill.Api\MedBill.Api.csproj
}

$env:ASPNETCORE_ENVIRONMENT = 'Development'
$env:ASPNETCORE_URLS = "http://localhost:$BackendPort"

Write-Host ''
Write-Host 'Starting MedBill API locally...' -ForegroundColor Green
Write-Host "Backend URL: http://localhost:$BackendPort"
Write-Host "Android emulator BASE_URL: http://10.0.2.2:$BackendPort/"
Write-Host ''

# Keep process attached so logs remain visible.
dotnet run --project .\backend\MedBill.Api\MedBill.Api.csproj
