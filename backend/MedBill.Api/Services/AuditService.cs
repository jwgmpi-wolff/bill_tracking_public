using MedBill.Api.Data;
using MedBill.Api.Models;

namespace MedBill.Api.Services;

public class AuditService : IAuditService
{
    private readonly AppDbContext _db;

    public AuditService(AppDbContext db)
    {
        _db = db;
    }

    public async Task WriteAsync(Guid? userId, string action, string entityType, string entityId, string result, HttpContext context, CancellationToken cancellationToken)
    {
        var record = new AuditRecord
        {
            AuditId = Guid.NewGuid(),
            UserId = userId,
            Action = action,
            EntityType = entityType,
            EntityId = entityId,
            Result = result,
            IpAddress = context.Connection.RemoteIpAddress?.ToString(),
            CorrelationId = context.TraceIdentifier,
            Timestamp = DateTimeOffset.UtcNow
        };

        _db.AuditLog.Add(record);
        await _db.SaveChangesAsync(cancellationToken);
    }
}
