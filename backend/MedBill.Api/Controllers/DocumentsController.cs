using MedBill.Api.Data;
using MedBill.Api.Models;
using MedBill.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MedBill.Api.Controllers;

[ApiController]
[Route("api/bills/{billId:guid}/documents")]
[Authorize]
public class DocumentsController : ControllerBase
{
    private const long MaxBytes = 10 * 1024 * 1024;
    private static readonly string[] AllowedTypes = [
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "image/png",
        "image/jpeg"
    ];

    private readonly AppDbContext _db;
    private readonly IDocumentService _documents;
    private readonly IAuditService _audit;
    private readonly IUserContextService _userContext;

    public DocumentsController(AppDbContext db, IDocumentService documents, IAuditService audit, IUserContextService userContext)
    {
        _db = db;
        _documents = documents;
        _audit = audit;
        _userContext = userContext;
    }

    [HttpGet]
    public async Task<IActionResult> List(Guid billId, CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var exists = await _db.Bills.AnyAsync(x => x.BillId == billId && x.UserId == userId, cancellationToken);
        if (!exists)
        {
            return NotFound();
        }

        var rows = await _db.Documents.Where(x => x.BillId == billId && x.UserId == userId).ToListAsync(cancellationToken);
        return Ok(rows);
    }

    [HttpPost]
    [RequestSizeLimit(MaxBytes)]
    public async Task<IActionResult> Upload(Guid billId, IFormFile file, [FromForm] string? documentType, CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        if (file.Length == 0 || file.Length > MaxBytes)
        {
            return BadRequest("Invalid file size.");
        }

        if (!AllowedTypes.Contains(file.ContentType))
        {
            return BadRequest("Unsupported content type.");
        }

        var bill = await _db.Bills.FirstOrDefaultAsync(x => x.BillId == billId && x.UserId == userId, cancellationToken);
        if (bill is null)
        {
            return NotFound();
        }

        await using var stream = file.OpenReadStream();
        var blobName = await _documents.UploadAsync(file.FileName, stream, file.ContentType, cancellationToken);
        var hash = Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(file.OpenReadStream()));

        var normalizedType = string.IsNullOrWhiteSpace(documentType)
            ? (file.ContentType.StartsWith("image/", StringComparison.OrdinalIgnoreCase) ? "Scan" : "Document")
            : documentType.Trim();

        var doc = new DocumentRecord
        {
            DocumentId = Guid.NewGuid(),
            BillId = billId,
            UserId = userId,
            DocumentType = normalizedType,
            BlobUri = blobName,
            FileName = file.FileName,
            ContentType = file.ContentType,
            UploadedAt = DateTimeOffset.UtcNow,
            OcrStatus = "NotStarted",
            Sha256Hash = hash
        };

        _db.Documents.Add(doc);
        await _db.SaveChangesAsync(cancellationToken);
        await _audit.WriteAsync(userId, "Upload", "Document", doc.DocumentId.ToString(), "Success", HttpContext, cancellationToken);

        return Created($"/api/bills/{billId}/documents/{doc.DocumentId}", doc);
    }

    [HttpGet("{documentId:guid}/download")]
    public async Task<IActionResult> Download(Guid billId, Guid documentId, CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var doc = await _db.Documents.FirstOrDefaultAsync(x => x.DocumentId == documentId && x.BillId == billId && x.UserId == userId, cancellationToken);
        if (doc is null)
        {
            return NotFound();
        }

        var uri = await _documents.GetDownloadUriAsync(doc.BlobUri, TimeSpan.FromMinutes(10), cancellationToken);
        return Ok(new { url = uri.ToString(), expiresInSeconds = 600 });
    }

    [HttpDelete("{documentId:guid}")]
    public async Task<IActionResult> Delete(Guid billId, Guid documentId, CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var doc = await _db.Documents.FirstOrDefaultAsync(x => x.DocumentId == documentId && x.BillId == billId && x.UserId == userId, cancellationToken);
        if (doc is null)
        {
            return NotFound();
        }

        try
        {
            await _documents.DeleteIfExistsAsync(doc.BlobUri, cancellationToken);
        }
        catch
        {
            // If blob cleanup fails, still remove metadata row to unblock user actions.
        }

        _db.Documents.Remove(doc);
        await _db.SaveChangesAsync(cancellationToken);
        await _audit.WriteAsync(userId, "Delete", "Document", doc.DocumentId.ToString(), "Success", HttpContext, cancellationToken);

        return NoContent();
    }
}
