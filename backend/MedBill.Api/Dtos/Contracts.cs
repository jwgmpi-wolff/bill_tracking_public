using MedBill.Api.Models;

namespace MedBill.Api.Dtos;

public record BillCreateRequest(
    Guid ProviderId,
    string? ProviderName,
    string InvoiceNumber,
    string? AccountNumber,
    DateOnly ServiceDate,
    DateOnly BillDate,
    DateOnly DueDate,
    decimal TotalBilledAmount,
    decimal InsurancePaidAmount,
    decimal DiscountAdjustmentAmount,
    decimal PatientResponsibilityAmount,
    string? Notes);

public record BillEntryUpdateRequest(
    Guid ProviderId,
    string? ProviderName,
    string InvoiceNumber,
    string? AccountNumber,
    DateOnly ServiceDate,
    DateOnly BillDate,
    DateOnly DueDate,
    decimal TotalBilledAmount,
    decimal InsurancePaidAmount,
    decimal DiscountAdjustmentAmount,
    decimal PatientResponsibilityAmount,
    string? Notes);

public record BillUpdateRequest(
    BillStatus CurrentStatus,
    decimal AmountPaid,
    string? Notes);

public record PaymentSessionRequest(decimal Amount, string Currency, string Method);

public record PaymentSessionResponse(string Provider, string SessionId, string RedirectUrl);

public record DashboardResponse(decimal TotalOutstanding, decimal PaidThisYear, int OverdueCount, int DueInNext14Days);

public record ProviderLookupResponse(Guid ProviderId, string ProviderName, string? BillingPhone, string? BillingPortalUrl);
