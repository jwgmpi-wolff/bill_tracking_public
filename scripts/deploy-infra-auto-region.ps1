param(
    [string]$ResourceGroupPrefix = 'rg-medbill-tracker',
    [string[]]$CandidateRegions = @('westus2', 'westus3', 'eastus2', 'eastus', 'centralus', 'southcentralus', 'northcentralus', 'westus'),
    [bool]$DeployAppService = $true,
    [bool]$DeploySql = $true,
    [switch]$SkipWhatIf
)

$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

function New-CliSafePassword {
    $chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#%+=_-'.ToCharArray()
    return (-join (1..28 | ForEach-Object { $chars | Get-Random }))
}

function Test-SqlRegionProvisioning {
    param(
        [Parameter(Mandatory = $true)][string]$ResourceGroup,
        [Parameter(Mandatory = $true)][string]$Region
    )

    $suffix = Get-Random -Minimum 1000 -Maximum 9999
    $probeName = "sqlprobe$suffix"
    $password = New-CliSafePassword

    Write-Host "Probing SQL provisioning in $Region using $probeName..." -ForegroundColor Cyan

    $output = az sql server create `
        --name $probeName `
        --resource-group $ResourceGroup `
        --location $Region `
        --admin-user sqladminuser `
        --admin-password $password `
        --output json 2>&1

    if ($LASTEXITCODE -eq 0) {
        # Cleanup probe resource immediately.
        az sql server delete --name $probeName --resource-group $ResourceGroup --yes --output none | Out-Null
        return @{ Success = $true; Message = 'SQL provisioning probe succeeded.' }
    }

    $text = ($output | Out-String)
    if ($text -match 'ProvisioningDisabled' -or $text -match 'restricted from provisioning') {
        return @{ Success = $false; Message = 'ProvisioningDisabled' }
    }

    return @{ Success = $false; Message = $text.Trim() }
}

$selectedRegion = $null
$selectedResourceGroup = $null

if ($DeploySql) {
    foreach ($region in $CandidateRegions) {
        $rgName = "$ResourceGroupPrefix-$region"
        az group create --name $rgName --location $region --output none | Out-Null

        $probe = Test-SqlRegionProvisioning -ResourceGroup $rgName -Region $region
        if ($probe.Success) {
            $selectedRegion = $region
            $selectedResourceGroup = $rgName
            break
        }

        Write-Host "Region $region rejected: $($probe.Message)" -ForegroundColor Yellow
    }

    if (-not $selectedRegion) {
        throw "No candidate region passed SQL provisioning probe. Try opening a support request or provide different regions."
    }
} else {
    $selectedRegion = $CandidateRegions[0]
    $selectedResourceGroup = "$ResourceGroupPrefix-$selectedRegion"
    az group create --name $selectedResourceGroup --location $selectedRegion --output none | Out-Null
}

Write-Host "Selected region: $selectedRegion" -ForegroundColor Green
Write-Host "Selected resource group: $selectedResourceGroup" -ForegroundColor Green

$deployArgs = @{
    ResourceGroup = $selectedResourceGroup
    Location = $selectedRegion
    DeployAppService = $DeployAppService
    DeploySql = $DeploySql
}

if ($SkipWhatIf) {
    $deployArgs.SkipWhatIf = $true
}

& (Join-Path $PSScriptRoot 'deploy-infra.ps1') @deployArgs
