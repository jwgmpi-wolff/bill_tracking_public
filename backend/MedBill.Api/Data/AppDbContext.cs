using MedBill.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace MedBill.Api.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }

    public DbSet<UserProfile> Users => Set<UserProfile>();
    public DbSet<Provider> Providers => Set<Provider>();
    public DbSet<Bill> Bills => Set<Bill>();
    public DbSet<BillStatusHistory> BillStatusHistory => Set<BillStatusHistory>();
    public DbSet<DocumentRecord> Documents => Set<DocumentRecord>();
    public DbSet<PaymentRecord> Payments => Set<PaymentRecord>();
    public DbSet<AuditRecord> AuditLog => Set<AuditRecord>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<UserProfile>().HasKey(x => x.UserId);
        modelBuilder.Entity<Provider>().HasKey(x => x.ProviderId);
        modelBuilder.Entity<Bill>().HasKey(x => x.BillId);
        modelBuilder.Entity<BillStatusHistory>().HasKey(x => x.StatusHistoryId);
        modelBuilder.Entity<DocumentRecord>().HasKey(x => x.DocumentId);
        modelBuilder.Entity<PaymentRecord>().HasKey(x => x.PaymentId);
        modelBuilder.Entity<AuditRecord>().HasKey(x => x.AuditId);

        modelBuilder.Entity<Bill>().Property(x => x.TotalBilledAmount).HasPrecision(18, 2);
        modelBuilder.Entity<Bill>().Property(x => x.InsurancePaidAmount).HasPrecision(18, 2);
        modelBuilder.Entity<Bill>().Property(x => x.DiscountAdjustmentAmount).HasPrecision(18, 2);
        modelBuilder.Entity<Bill>().Property(x => x.PatientResponsibilityAmount).HasPrecision(18, 2);
        modelBuilder.Entity<Bill>().Property(x => x.AmountPaid).HasPrecision(18, 2);
        modelBuilder.Entity<PaymentRecord>().Property(x => x.Amount).HasPrecision(18, 2);
    }
}
