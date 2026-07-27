namespace MedBill.Api.Services;

public interface IDocumentService
{
    Task<string> UploadAsync(string fileName, Stream content, string contentType, CancellationToken cancellationToken);
    Task<Uri> GetDownloadUriAsync(string blobName, TimeSpan expiresIn, CancellationToken cancellationToken);
    Task DeleteIfExistsAsync(string blobName, CancellationToken cancellationToken);
}
