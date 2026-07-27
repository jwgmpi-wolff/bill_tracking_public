using MedBill.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace MedBill.Api.Data;

public static class SeedData
{
    public static async Task ApplyAsync(AppDbContext db, ILogger logger, CancellationToken cancellationToken)
    {
        if (await db.Users.AnyAsync(cancellationToken))
        {
            return;
        }

        var demoUser = new UserProfile
        {
            UserId = Guid.NewGuid(),
            ExternalIdentityId = "demo-external-id",
            Email = "demo@medbill.local",
            DisplayName = "Demo User",
            CreatedAt = DateTimeOffset.UtcNow,
            LastLoginAt = DateTimeOffset.UtcNow
        };

        var provider = new Provider
        {
            ProviderId = Guid.NewGuid(),
            UserId = demoUser.UserId,
            ProviderName = "Contoso Medical Group",
            BillingPhone = "+1-555-0100",
            BillingPortalUrl = "https://example.org/billing"
        };

        var bill = new Bill
        {
            BillId = Guid.NewGuid(),
            UserId = demoUser.UserId,
            ProviderId = provider.ProviderId,
            InvoiceNumber = "INV-1001",
            AccountNumber = "ACCT-5001",
            ServiceDate = DateOnly.FromDateTime(DateTime.UtcNow.AddDays(-20)),
            BillDate = DateOnly.FromDateTime(DateTime.UtcNow.AddDays(-10)),
            DueDate = DateOnly.FromDateTime(DateTime.UtcNow.AddDays(10)),
            TotalBilledAmount = 1200.00m,
            InsurancePaidAmount = 800.00m,
            DiscountAdjustmentAmount = 50.00m,
            PatientResponsibilityAmount = 350.00m,
            AmountPaid = 0m,
            CurrentStatus = BillStatus.PatientResponsibility,
            IsOverdue = false,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        };

        db.Users.Add(demoUser);
        db.Providers.Add(provider);
        db.Bills.Add(bill);
        db.BillStatusHistory.Add(new BillStatusHistory
        {
            StatusHistoryId = Guid.NewGuid(),
            BillId = bill.BillId,
            Status = BillStatus.PatientResponsibility,
            ChangedAt = DateTimeOffset.UtcNow,
            ChangedBy = "seed",
            Notes = "Seeded sample bill"
        });

        await db.SaveChangesAsync(cancellationToken);
        logger.LogInformation("Seed data applied for MedBill API demo profile.");
    }
}
