param(
    [string]$ResourceGroup = 'rg-medbill-tracker-eus2',
    [string]$Location = 'eastus2',
    [string]$DeploymentName = ('medbill-deploy-' + (Get-Date -Format 'yyyyMMddHHmmss')),
    [int]$NameSuffix = (Get-Random -Minimum 3000 -Maximum 9999),
    [bool]$DeployAppService = $false,
    [bool]$DeploySql = $false,
    [switch]$SkipWhatIf
)

$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

$appServiceName = "app-medbill-api-$NameSuffix"
$sqlServerName = "sqlmedbill$NameSuffix"
$storageAccountName = "stmedbill$NameSuffix"
$keyVaultName = "kv-medbill-tracker-$NameSuffix"

# Generate a strong SQL password for deployment-time use only.
# Keep characters CLI-safe to avoid shell parsing issues.
$chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#%+=_-'.ToCharArray()
$sqlAdminPassword = -join (1..28 | ForEach-Object { $chars | Get-Random })

$parameterArgs = @(
    "location=$Location"
    "appServiceName=$appServiceName"
    "sqlServerName=$sqlServerName"
    "sqlAdminLogin=sqladminuser"
    "sqlAdminPassword=$sqlAdminPassword"
    "storageAccountName=$storageAccountName"
    "keyVaultName=$keyVaultName"
    "deployAppService=$DeployAppService"
    "deploySql=$DeploySql"
)

Write-Host "Ensuring resource group $ResourceGroup in $Location..." -ForegroundColor Cyan
az group create --name $ResourceGroup --location $Location --output none

if (-not $SkipWhatIf) {
    Write-Host "Running what-if for $DeploymentName..." -ForegroundColor Cyan
    az deployment group what-if `
        --name "$DeploymentName-whatif" `
        --resource-group $ResourceGroup `
        --template-file infra/main.bicep `
        --parameters infra/main.parameters.json `
        --parameters @parameterArgs
}

Write-Host "Running deployment $DeploymentName..." -ForegroundColor Cyan
$deployment = az deployment group create `
    --name $DeploymentName `
    --resource-group $ResourceGroup `
    --template-file infra/main.bicep `
    --parameters infra/main.parameters.json `
    --parameters @parameterArgs `
    --query "{name:name,state:properties.provisioningState,outputs:properties.outputs}" `
    -o json | ConvertFrom-Json

if ($LASTEXITCODE -ne 0 -or -not $deployment) {
    throw "Deployment command failed for $DeploymentName."
}

$failedOps = az deployment operation group list `
    --name $DeploymentName `
    --resource-group $ResourceGroup `
    --query "[?properties.provisioningState=='Failed'].{target:properties.targetResource.resourceName,code:properties.statusCode,message:properties.statusMessage.error.message}" `
    -o json | ConvertFrom-Json

Write-Host "" 
Write-Host "Deployment state: $($deployment.state)" -ForegroundColor Yellow
Write-Host "Resource group: $ResourceGroup"
Write-Host "Location: $Location"
Write-Host "App service name: $appServiceName"
Write-Host "SQL server name: $sqlServerName"
Write-Host "Storage account name: $storageAccountName"
Write-Host "Key vault name: $keyVaultName"
Write-Host "" 

if ($failedOps -and $failedOps.Count -gt 0) {
    Write-Host "Failed operations:" -ForegroundColor Red
    $failedOps | Format-Table -AutoSize
} else {
    Write-Host "No failed operations reported." -ForegroundColor Green
}

if ($deployment.outputs) {
    Write-Host "" 
    Write-Host "Template outputs:" -ForegroundColor Cyan
    $deployment.outputs | ConvertTo-Json -Depth 5
}
