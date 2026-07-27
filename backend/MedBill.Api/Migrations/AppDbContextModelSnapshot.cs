using MedBill.Api.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;

#nullable disable

namespace MedBill.Api.Migrations
{
    [DbContext(typeof(AppDbContext))]
    partial class AppDbContextModelSnapshot : ModelSnapshot
    {
        protected override void BuildModel(ModelBuilder modelBuilder)
        {
#pragma warning disable 612, 618
            modelBuilder
                .HasAnnotation("ProductVersion", "8.0.7")
                .HasAnnotation("Relational:MaxIdentifierLength", 128);

            modelBuilder.Entity("MedBill.Api.Models.AuditRecord", b =>
                {
                    b.Property<Guid>("AuditId").ValueGeneratedOnAdd();
                    b.Property<string>("Action");
                    b.Property<string>("CorrelationId");
                    b.Property<string>("EntityId");
                    b.Property<string>("EntityType");
                    b.Property<string>("IpAddress");
                    b.Property<string>("Result");
                    b.Property<DateTimeOffset>("Timestamp");
                    b.Property<Guid?>("UserId");
                    b.HasKey("AuditId");
                    b.ToTable("AuditLog");
                });

            modelBuilder.Entity("MedBill.Api.Models.Bill", b =>
                {
                    b.Property<Guid>("BillId").ValueGeneratedOnAdd();
                    b.Property<string>("AccountNumber");
                    b.Property<decimal>("AmountPaid").HasPrecision(18, 2);
                    b.Property<DateOnly>("BillDate");
                    b.Property<DateTimeOffset>("CreatedAt");
                    b.Property<int>("CurrentStatus");
                    b.Property<decimal>("DiscountAdjustmentAmount");
                    b.Property<DateOnly>("DueDate");
                    b.Property<decimal>("InsurancePaidAmount");
                    b.Property<string>("InvoiceNumber");
                    b.Property<bool>("IsOverdue");
                    b.Property<decimal>("PatientResponsibilityAmount").HasPrecision(18, 2);
                    b.Property<Guid>("ProviderId");
                    b.Property<DateOnly>("ServiceDate");
                    b.Property<decimal>("TotalBilledAmount").HasPrecision(18, 2);
                    b.Property<DateTimeOffset>("UpdatedAt");
                    b.Property<Guid>("UserId");
                    b.HasKey("BillId");
                    b.ToTable("Bills");
                });

            modelBuilder.Entity("MedBill.Api.Models.BillStatusHistory", b =>
                {
                    b.Property<Guid>("StatusHistoryId").ValueGeneratedOnAdd();
                    b.Property<Guid>("BillId");
                    b.Property<DateTimeOffset>("ChangedAt");
                    b.Property<string>("ChangedBy");
                    b.Property<string>("Notes");
                    b.Property<int>("Status");
                    b.HasKey("StatusHistoryId");
                    b.ToTable("BillStatusHistory");
                });

            modelBuilder.Entity("MedBill.Api.Models.DocumentRecord", b =>
                {
                    b.Property<Guid>("DocumentId").ValueGeneratedOnAdd();
                    b.Property<Guid>("BillId");
                    b.Property<string>("BlobUri");
                    b.Property<string>("ContentType");
                    b.Property<string>("DocumentType");
                    b.Property<string>("FileName");
                    b.Property<string>("OcrStatus");
                    b.Property<string>("Sha256Hash");
                    b.Property<DateTimeOffset>("UploadedAt");
                    b.Property<Guid>("UserId");
                    b.HasKey("DocumentId");
                    b.ToTable("Documents");
                });

            modelBuilder.Entity("MedBill.Api.Models.PaymentRecord", b =>
                {
                    b.Property<Guid>("PaymentId").ValueGeneratedOnAdd();
                    b.Property<decimal>("Amount").HasPrecision(18, 2);
                    b.Property<Guid>("BillId");
                    b.Property<DateTimeOffset>("CreatedAt");
                    b.Property<string>("Currency");
                    b.Property<DateTimeOffset?>("PaidAt");
                    b.Property<string>("PaymentIntentId");
                    b.Property<string>("PaymentProvider");
                    b.Property<string>("PaymentSessionId");
                    b.Property<string>("PaymentStatus");
                    b.Property<string>("ProviderCustomerId");
                    b.Property<string>("TransactionId");
                    b.Property<Guid>("UserId");
                    b.Property<bool>("WebhookVerified");
                    b.HasKey("PaymentId");
                    b.ToTable("Payments");
                });

            modelBuilder.Entity("MedBill.Api.Models.Provider", b =>
                {
                    b.Property<Guid>("ProviderId").ValueGeneratedOnAdd();
                    b.Property<string>("BillingPhone");
                    b.Property<string>("BillingPortalUrl");
                    b.Property<string>("Notes");
                    b.Property<string>("ProviderName");
                    b.Property<Guid>("UserId");
                    b.HasKey("ProviderId");
                    b.ToTable("Providers");
                });

            modelBuilder.Entity("MedBill.Api.Models.UserProfile", b =>
                {
                    b.Property<Guid>("UserId").ValueGeneratedOnAdd();
                    b.Property<DateTimeOffset>("CreatedAt");
                    b.Property<string>("DisplayName");
                    b.Property<string>("Email");
                    b.Property<string>("ExternalIdentityId");
                    b.Property<DateTimeOffset?>("LastLoginAt");
                    b.HasKey("UserId");
                    b.ToTable("Users");
                });
#pragma warning restore 612, 618
        }
    }
}
