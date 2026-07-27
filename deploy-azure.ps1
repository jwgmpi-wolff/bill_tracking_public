param(
    [string]$TenantId = '',
    [string]$SubscriptionId = '',
    [string]$ResourceGroup = 'rg-medbill-tracker',
    [string]$Location = 'eastus',
    [int]$NameSuffix = (Get-Random -Minimum 3000 -Maximum 9999),
    [PSCredential]$SqlAdminCredential,
    [string]$ApiAudience = '',
    [bool]$DeployAppService = $true,
    [bool]$DeploySql = $true
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
$repoRoot = $PSScriptRoot
Set-Location $repoRoot

if ([string]::IsNullOrWhiteSpace($SubscriptionId)) {
    throw 'SubscriptionId is required. Example: ./deploy-azure.ps1 -SubscriptionId <guid>'
}

if (-not [string]::IsNullOrWhiteSpace($TenantId)) {
    $currentTenant = az account show --query tenantId -o tsv
    if ($currentTenant -ne $TenantId) {
        throw "Current Azure context tenant '$currentTenant' does not match requested tenant '$TenantId'. Run: az login --tenant $TenantId"
    }
}

Write-Host "Setting subscription context to $SubscriptionId..." -ForegroundColor Cyan
az account set --subscription $SubscriptionId

$sqlAdminLogin = 'sqladminuser'

if ($null -eq $SqlAdminCredential) {
    $chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#%+=_-'.ToCharArray()
    $generated = -join (1..28 | ForEach-Object { $chars | Get-Random })
    $securePassword = ConvertTo-SecureString $generated -AsPlainText -Force
    $SqlAdminCredential = New-Object System.Management.Automation.PSCredential($sqlAdminLogin, $securePassword)
} else {
    $sqlAdminLogin = $SqlAdminCredential.UserName
}

$sqlAdminPasswordPlain = [System.Net.NetworkCredential]::new('', $SqlAdminCredential.Password).Password

$appServiceName = "app-medbill-api-$NameSuffix"
$sqlServerName = "sqlmedbill$NameSuffix"
$storageAccountName = "stmedbill$NameSuffix"
$keyVaultName = "kv-medbill-tracker-$NameSuffix"
$deploymentName = 'medbill-' + (Get-Date -Format 'yyyyMMddHHmmss')

Write-Host "Ensuring resource group $ResourceGroup exists..." -ForegroundColor Cyan
$existingRg = $null
try {
    $existingRg = az group show --name $ResourceGroup --query "{name:name,location:location}" -o json | ConvertFrom-Json
} catch {
    $existingRg = $null
}

if ($null -ne $existingRg) {
    if ($existingRg.location -ne $Location) {
        Write-Host "Resource group $ResourceGroup already exists in $($existingRg.location). Continuing with existing location." -ForegroundColor Yellow
        $Location = $existingRg.location
    } else {
        Write-Host "Resource group $ResourceGroup already exists in $Location. Continuing." -ForegroundColor Yellow
    }
} else {
    Write-Host "Creating resource group $ResourceGroup in $Location..." -ForegroundColor Cyan
    az group create --name $ResourceGroup --location $Location --output none
}

Write-Host "Deploying infrastructure ($deploymentName)..." -ForegroundColor Cyan
$deployment = az deployment group create `
    --name $deploymentName `
    --resource-group $ResourceGroup `
    --template-file .\infra\main.bicep `
    --parameters .\infra\main.parameters.json `
    --parameters location=$Location `
                 appServiceName=$appServiceName `
                 sqlServerName=$sqlServerName `
                 sqlAdminLogin=$sqlAdminLogin `
                 sqlAdminPassword=$sqlAdminPasswordPlain `
                 storageAccountName=$storageAccountName `
                 keyVaultName=$keyVaultName `
                 deployAppService=$DeployAppService `
                 deploySql=$DeploySql `
                 sqlPublicNetworkAccess=Enabled `
                 sqlAllowAzureServices=true `
    --query "{appServiceUrl:properties.outputs.appServiceUrl.value,storageName:properties.outputs.storageName.value}" `
    -o json | ConvertFrom-Json

if (-not $DeployAppService) {
    Write-Host 'Infrastructure deployed with App Service disabled; skipping backend publish.' -ForegroundColor Yellow
    return
}

Write-Host 'Publishing backend package...' -ForegroundColor Cyan
$publishDir = Join-Path $repoRoot '.tmp/publish'
$zipPath = Join-Path $repoRoot '.tmp/medbill-api.zip'

if (Test-Path $publishDir) { Remove-Item $publishDir -Recurse -Force }
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
New-Item -ItemType Directory -Path $publishDir -Force | Out-Null

dotnet publish .\backend\MedBill.Api\MedBill.Api.csproj -c Release -o $publishDir
Compress-Archive -Path (Join-Path $publishDir '*') -DestinationPath $zipPath -Force

Write-Host "Deploying backend to App Service $appServiceName..." -ForegroundColor Cyan
az webapp deploy --resource-group $ResourceGroup --name $appServiceName --src-path $zipPath --type zip --output none

$jwtAuthority = if ([string]::IsNullOrWhiteSpace($TenantId)) { 'https://login.microsoftonline.com/common/v2.0' } else { "https://login.microsoftonline.com/$TenantId/v2.0" }
if ([string]::IsNullOrWhiteSpace($ApiAudience)) {
    throw 'ApiAudience is required. Example: api://<your-api-app-id>'
}

$appSettings = @(
    "Jwt__Authority=$jwtAuthority",
    "Jwt__Audience=$ApiAudience",
    'SeedData__Enabled=false',
    'Startup__FailOnMigrationError=false'
)

if ($DeploySql) {
    $sqlConnection = "Server=tcp:$sqlServerName.database.windows.net,1433;Initial Catalog=medbill;Persist Security Info=False;User ID=$sqlAdminLogin;Password=$sqlAdminPasswordPlain;MultipleActiveResultSets=False;Encrypt=True;TrustServerCertificate=False;Connection Timeout=30;"
    $appSettings += "ConnectionStrings__Sql=$sqlConnection"
}

Write-Host 'Applying App Service configuration...' -ForegroundColor Cyan
az webapp config appsettings set --resource-group $ResourceGroup --name $appServiceName --settings $appSettings --output none
az webapp restart --resource-group $ResourceGroup --name $appServiceName --output none

Write-Host ''
Write-Host 'Deployment complete.' -ForegroundColor Green
Write-Host "App Service URL: $($deployment.appServiceUrl)"
Write-Host "Resource Group: $ResourceGroup"
Write-Host "Subscription: $SubscriptionId"
Write-Host "Tenant: $TenantId"
Write-Host ''
Write-Host 'Save SQL admin password securely if DeploySql=true; it is needed for direct SQL access.' -ForegroundColor Yellow
