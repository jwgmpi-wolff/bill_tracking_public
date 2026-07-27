namespace MedBill.Api.Services;

public interface IAuditService
{
    Task WriteAsync(Guid? userId, string action, string entityType, string entityId, string result, HttpContext context, CancellationToken cancellationToken);
}
