-- Optional SQL seed script for local/dev manual seeding.
-- App-level seed logic is implemented in Data/SeedData.cs and runs when SeedData:Enabled=true.

IF NOT EXISTS (SELECT 1 FROM Users WHERE ExternalIdentityId = 'demo-external-id')
BEGIN
    DECLARE @UserId uniqueidentifier = NEWID();
    DECLARE @ProviderId uniqueidentifier = NEWID();
    DECLARE @BillId uniqueidentifier = NEWID();

    INSERT INTO Users (UserId, ExternalIdentityId, Email, DisplayName, CreatedAt, LastLoginAt)
    VALUES (@UserId, 'demo-external-id', 'demo@medbill.local', 'Demo User', SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO Providers (ProviderId, UserId, ProviderName, BillingPhone, BillingPortalUrl, Notes)
    VALUES (@ProviderId, @UserId, 'Contoso Medical Group', '+1-555-0100', 'https://example.org/billing', 'Seed data');

    INSERT INTO Bills (
        BillId, UserId, ProviderId, InvoiceNumber, AccountNumber, ServiceDate, BillDate, DueDate,
        TotalBilledAmount, InsurancePaidAmount, DiscountAdjustmentAmount, PatientResponsibilityAmount,
        AmountPaid, CurrentStatus, IsOverdue, CreatedAt, UpdatedAt
    )
    VALUES (
        @BillId, @UserId, @ProviderId, 'INV-1001', 'ACCT-5001', CAST(DATEADD(day, -20, GETUTCDATE()) AS date),
        CAST(DATEADD(day, -10, GETUTCDATE()) AS date), CAST(DATEADD(day, 10, GETUTCDATE()) AS date),
        1200.00, 800.00, 50.00, 350.00, 0.00, 'PatientResponsibility', 0, SYSUTCDATETIME(), SYSUTCDATETIME()
    );
END
