using System.ComponentModel.DataAnnotations.Schema;

namespace MedBill.Api.Models;

public enum BillStatus
{
    New,
    SubmittedToInsurance,
    InsuranceProcessed,
    PatientResponsibility,
    Paid,
    Disputed,
    Overdue
}

public class UserProfile
{
    public Guid UserId { get; set; }
    public string ExternalIdentityId { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset? LastLoginAt { get; set; }
}

public class Provider
{
    public Guid ProviderId { get; set; }
    public Guid UserId { get; set; }
    public string ProviderName { get; set; } = string.Empty;
    public string? BillingPhone { get; set; }
    public string? BillingPortalUrl { get; set; }
    public string? Notes { get; set; }
}

public class Bill
{
    public Guid BillId { get; set; }
    public Guid UserId { get; set; }
    public Guid ProviderId { get; set; }
    public string InvoiceNumber { get; set; } = string.Empty;
    public string? AccountNumber { get; set; }
    public DateOnly ServiceDate { get; set; }
    public DateOnly BillDate { get; set; }
    public DateOnly DueDate { get; set; }
    public decimal TotalBilledAmount { get; set; }
    public decimal InsurancePaidAmount { get; set; }
    public decimal DiscountAdjustmentAmount { get; set; }
    public decimal PatientResponsibilityAmount { get; set; }
    public decimal AmountPaid { get; set; }
    public BillStatus CurrentStatus { get; set; } = BillStatus.New;
    [NotMapped]
    public string? ProviderName { get; set; }
    public bool IsOverdue { get; set; }
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
}

public class BillStatusHistory
{
    public Guid StatusHistoryId { get; set; }
    public Guid BillId { get; set; }
    public BillStatus Status { get; set; }
    public DateTimeOffset ChangedAt { get; set; } = DateTimeOffset.UtcNow;
    public string ChangedBy { get; set; } = string.Empty;
    public string? Notes { get; set; }
}

public class DocumentRecord
{
    public Guid DocumentId { get; set; }
    public Guid BillId { get; set; }
    public Guid UserId { get; set; }
    public string DocumentType { get; set; } = "Invoice";
    public string BlobUri { get; set; } = string.Empty;
    public string FileName { get; set; } = string.Empty;
    public string ContentType { get; set; } = "application/pdf";
    public DateTimeOffset UploadedAt { get; set; } = DateTimeOffset.UtcNow;
    public string OcrStatus { get; set; } = "NotStarted";
    public string Sha256Hash { get; set; } = string.Empty;
}

public class PaymentRecord
{
    public Guid PaymentId { get; set; }
    public Guid BillId { get; set; }
    public Guid UserId { get; set; }
    public string PaymentProvider { get; set; } = "Stripe";
    public string ProviderCustomerId { get; set; } = string.Empty;
    public string PaymentSessionId { get; set; } = string.Empty;
    public string PaymentIntentId { get; set; } = string.Empty;
    public string TransactionId { get; set; } = string.Empty;
    public decimal Amount { get; set; }
    public string Currency { get; set; } = "USD";
    public string PaymentStatus { get; set; } = "Pending";
    public DateTimeOffset? PaidAt { get; set; }
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public bool WebhookVerified { get; set; }
}

public class AuditRecord
{
    public Guid AuditId { get; set; }
    public Guid? UserId { get; set; }
    public string Action { get; set; } = string.Empty;
    public string EntityType { get; set; } = string.Empty;
    public string EntityId { get; set; } = string.Empty;
    public DateTimeOffset Timestamp { get; set; } = DateTimeOffset.UtcNow;
    public string? IpAddress { get; set; }
    public string? DeviceId { get; set; }
    public string Result { get; set; } = "Success";
    public string CorrelationId { get; set; } = string.Empty;
}
