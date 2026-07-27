targetScope = 'resourceGroup'

@description('Deployment location')
param location string = resourceGroup().location

@description('App service name')
param appServiceName string = 'app-medbill-api'

@description('SQL server name (globally unique)')
param sqlServerName string = 'sql-medbill-server'

@description('SQL admin login')
param sqlAdminLogin string

@secure()
@description('SQL admin password')
param sqlAdminPassword string

@description('Storage account name (globally unique, lowercase)')
param storageAccountName string = 'stmedbilltracker'

@description('Key Vault name')
param keyVaultName string = 'kv-medbill-tracker'

@description('Deploy App Service plan/site/Application Insights resources')
param deployAppService bool = true

@description('Deploy Azure SQL server and database resources')
param deploySql bool = true

@allowed([
  'Enabled'
  'Disabled'
])
@description('SQL Server public network access mode. Use Enabled unless private connectivity is configured.')
param sqlPublicNetworkAccess string = 'Enabled'

@description('Allow Azure services to access SQL Server (creates firewall rule 0.0.0.0).')
param sqlAllowAzureServices bool = true

resource plan 'Microsoft.Web/serverfarms@2023-12-01' = if (deployAppService) {
  name: '${appServiceName}-plan'
  location: location
  sku: {
    name: 'F1'
    tier: 'Free'
    capacity: 1
  }
}

resource app 'Microsoft.Web/sites@2023-12-01' = if (deployAppService) {
  name: appServiceName
  location: location
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    serverFarmId: plan.id
    httpsOnly: true
    siteConfig: {
      ftpsState: 'Disabled'
      minTlsVersion: '1.2'
      appSettings: [
        {
          name: 'WEBSITE_RUN_FROM_PACKAGE'
          value: '1'
        }
      ]
    }
  }
}

resource sqlServer 'Microsoft.Sql/servers@2023-08-01-preview' = if (deploySql) {
  name: sqlServerName
  location: location
  properties: {
    administratorLogin: sqlAdminLogin
    administratorLoginPassword: sqlAdminPassword
    publicNetworkAccess: sqlPublicNetworkAccess
    version: '12.0'
  }
}

resource sqlAllowAzureServicesRule 'Microsoft.Sql/servers/firewallRules@2023-08-01-preview' = if (deploySql && sqlAllowAzureServices && sqlPublicNetworkAccess == 'Enabled') {
  parent: sqlServer
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

resource sqlDb 'Microsoft.Sql/servers/databases@2023-08-01-preview' = if (deploySql) {
  parent: sqlServer
  name: 'medbill'
  location: location
  sku: {
    name: 'S0'
    tier: 'Standard'
  }
  properties: {
    zoneRedundant: false
  }
}

resource storage 'Microsoft.Storage/storageAccounts@2023-05-01' = {
  name: storageAccountName
  location: location
  sku: {
    name: 'Standard_LRS'
  }
  kind: 'StorageV2'
  properties: {
    minimumTlsVersion: 'TLS1_2'
    allowBlobPublicAccess: false
    supportsHttpsTrafficOnly: true
  }
}

resource documentsContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  name: '${storage.name}/default/documents'
  properties: {
    publicAccess: 'None'
  }
}

resource releasesContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  name: '${storage.name}/default/releases'
  properties: {
    publicAccess: 'None'
  }
}

resource kv 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: keyVaultName
  location: location
  properties: {
    enabledForDeployment: false
    enabledForTemplateDeployment: false
    enableSoftDelete: true
    enablePurgeProtection: true
    tenantId: subscription().tenantId
    sku: {
      name: 'standard'
      family: 'A'
    }
    accessPolicies: []
  }
}

resource ai 'Microsoft.Insights/components@2020-02-02' = if (deployAppService) {
  name: '${appServiceName}-ai'
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
  }
}

output appServiceUrl string = deployAppService ? 'https://${appServiceName}.azurewebsites.net' : ''
output sqlDbName string = deploySql ? sqlDb.name : ''
output storageName string = storage.name
