using Azure.Identity;
using Azure.Storage.Blobs;
using Azure.Storage.Sas;

namespace MedBill.Api.Services;

public class BlobDocumentService : IDocumentService
{
    private readonly BlobContainerClient _container;

    public BlobDocumentService(IConfiguration configuration)
    {
        var account = configuration["Storage:AccountName"] ?? throw new InvalidOperationException("Missing storage account name.");
        var container = configuration["Storage:DocumentsContainer"] ?? "documents";
        var serviceClient = new BlobServiceClient(new Uri($"https://{account}.blob.core.windows.net"), new DefaultAzureCredential());
        _container = serviceClient.GetBlobContainerClient(container);
    }

    public async Task<string> UploadAsync(string fileName, Stream content, string contentType, CancellationToken cancellationToken)
    {
        await _container.CreateIfNotExistsAsync(cancellationToken: cancellationToken);
        var blobName = $"{DateTime.UtcNow:yyyy/MM/dd}/{Guid.NewGuid()}-{fileName}";
        var client = _container.GetBlobClient(blobName);
        await client.UploadAsync(content, overwrite: false, cancellationToken);
        return blobName;
    }

    public Task<Uri> GetDownloadUriAsync(string blobName, TimeSpan expiresIn, CancellationToken cancellationToken)
    {
        var blobClient = _container.GetBlobClient(blobName);
        var uri = blobClient.GenerateSasUri(BlobSasPermissions.Read, DateTimeOffset.UtcNow.Add(expiresIn));
        return Task.FromResult(uri);
    }

    public async Task DeleteIfExistsAsync(string blobName, CancellationToken cancellationToken)
    {
        var blobClient = _container.GetBlobClient(blobName);
        await blobClient.DeleteIfExistsAsync(cancellationToken: cancellationToken);
    }
}
