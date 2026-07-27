using MedBill.Api.Data;
using MedBill.Api.Dtos;
using MedBill.Api.Models;
using MedBill.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MedBill.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class BillsController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly IAuditService _audit;
    private readonly IUserContextService _userContext;

    public BillsController(AppDbContext db, IAuditService audit, IUserContextService userContext)
    {
        _db = db;
        _audit = audit;
        _userContext = userContext;
    }

    private async Task<string?> EnsureProviderForUserAsync(Guid userId, Guid providerId, string? providerName, CancellationToken cancellationToken)
    {
        var normalizedName = string.IsNullOrWhiteSpace(providerName) ? null : providerName.Trim();
        var provider = await _db.Providers.FirstOrDefaultAsync(x => x.ProviderId == providerId && x.UserId == userId, cancellationToken);
        if (provider is null)
        {
            provider = new Provider
            {
                ProviderId = providerId,
                UserId = userId,
                ProviderName = normalizedName ?? "Unknown Provider"
            };
            _db.Providers.Add(provider);
        }
        else if (!string.IsNullOrWhiteSpace(normalizedName) && !string.Equals(provider.ProviderName, normalizedName, StringComparison.Ordinal))
        {
            provider.ProviderName = normalizedName;
        }

        return provider.ProviderName;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<Bill>>> GetBills(CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var bills = await _db.Bills.Where(b => b.UserId == userId).OrderBy(b => b.DueDate).ToListAsync(cancellationToken);
        if (bills.Count > 0)
        {
            var providerIds = bills.Select(b => b.ProviderId).Distinct().ToList();
            var providers = await _db.Providers
                .Where(p => p.UserId == userId && providerIds.Contains(p.ProviderId))
                .ToDictionaryAsync(p => p.ProviderId, p => p.ProviderName, cancellationToken);

            foreach (var bill in bills)
            {
                if (providers.TryGetValue(bill.ProviderId, out var providerName))
                {
                    bill.ProviderName = providerName;
                }
            }
        }
        return Ok(bills);
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<Bill>> GetBill(Guid id, CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var bill = await _db.Bills.FirstOrDefaultAsync(x => x.BillId == id && x.UserId == userId, cancellationToken);
        if (bill is null)
        {
            return NotFound();
        }

        bill.ProviderName = await _db.Providers
            .Where(p => p.ProviderId == bill.ProviderId && p.UserId == userId)
            .Select(p => p.ProviderName)
            .FirstOrDefaultAsync(cancellationToken);

        return Ok(bill);
    }

    [HttpPost]
    public async Task<ActionResult<Bill>> CreateBill([FromBody] BillCreateRequest request, CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var effectiveProviderName = await EnsureProviderForUserAsync(userId, request.ProviderId, request.ProviderName, cancellationToken);
        var bill = new Bill
        {
            BillId = Guid.NewGuid(),
            UserId = userId,
            ProviderId = request.ProviderId,
            InvoiceNumber = request.InvoiceNumber,
            AccountNumber = request.AccountNumber,
            ServiceDate = request.ServiceDate,
            BillDate = request.BillDate,
            DueDate = request.DueDate,
            TotalBilledAmount = request.TotalBilledAmount,
            InsurancePaidAmount = request.InsurancePaidAmount,
            DiscountAdjustmentAmount = request.DiscountAdjustmentAmount,
            PatientResponsibilityAmount = request.PatientResponsibilityAmount,
            CurrentStatus = BillStatus.New,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        };

        _db.Bills.Add(bill);
        _db.BillStatusHistory.Add(new BillStatusHistory
        {
            StatusHistoryId = Guid.NewGuid(),
            BillId = bill.BillId,
            Status = BillStatus.New,
            ChangedAt = DateTimeOffset.UtcNow,
            ChangedBy = userId.ToString(),
            Notes = request.Notes
        });

        await _db.SaveChangesAsync(cancellationToken);
        await _audit.WriteAsync(userId, "Create", "Bill", bill.BillId.ToString(), "Success", HttpContext, cancellationToken);

        bill.ProviderName = effectiveProviderName;

        return CreatedAtAction(nameof(GetBill), new { id = bill.BillId }, bill);
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<Bill>> UpdateBill(Guid id, [FromBody] BillUpdateRequest request, CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var bill = await _db.Bills.FirstOrDefaultAsync(x => x.BillId == id && x.UserId == userId, cancellationToken);
        if (bill is null)
        {
            return NotFound();
        }

        bill.CurrentStatus = request.CurrentStatus;
        bill.AmountPaid = request.AmountPaid;
        bill.IsOverdue = bill.DueDate < DateOnly.FromDateTime(DateTime.UtcNow) && request.CurrentStatus != BillStatus.Paid;
        bill.UpdatedAt = DateTimeOffset.UtcNow;

        _db.BillStatusHistory.Add(new BillStatusHistory
        {
            StatusHistoryId = Guid.NewGuid(),
            BillId = bill.BillId,
            Status = request.CurrentStatus,
            ChangedAt = DateTimeOffset.UtcNow,
            ChangedBy = userId.ToString(),
            Notes = request.Notes
        });

        await _db.SaveChangesAsync(cancellationToken);
        await _audit.WriteAsync(userId, "Update", "Bill", bill.BillId.ToString(), "Success", HttpContext, cancellationToken);

        bill.ProviderName = await _db.Providers
            .Where(p => p.ProviderId == bill.ProviderId && p.UserId == userId)
            .Select(p => p.ProviderName)
            .FirstOrDefaultAsync(cancellationToken);

        return Ok(bill);
    }

    [HttpPut("{id:guid}/entry")]
    public async Task<ActionResult<Bill>> UpdateBillEntry(Guid id, [FromBody] BillEntryUpdateRequest request, CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        if (string.IsNullOrWhiteSpace(request.InvoiceNumber))
        {
            return BadRequest("Invoice number is required.");
        }

        var userId = user.UserId;
        var bill = await _db.Bills.FirstOrDefaultAsync(x => x.BillId == id && x.UserId == userId, cancellationToken);
        if (bill is null)
        {
            return NotFound();
        }

        var effectiveProviderName = await EnsureProviderForUserAsync(userId, request.ProviderId, request.ProviderName, cancellationToken);

        bill.ProviderId = request.ProviderId;
        bill.InvoiceNumber = request.InvoiceNumber.Trim();
        bill.AccountNumber = request.AccountNumber;
        bill.ServiceDate = request.ServiceDate;
        bill.BillDate = request.BillDate;
        bill.DueDate = request.DueDate;
        bill.TotalBilledAmount = request.TotalBilledAmount;
        bill.InsurancePaidAmount = request.InsurancePaidAmount;
        bill.DiscountAdjustmentAmount = request.DiscountAdjustmentAmount;
        bill.PatientResponsibilityAmount = request.PatientResponsibilityAmount;
        bill.IsOverdue = bill.DueDate < DateOnly.FromDateTime(DateTime.UtcNow) && bill.CurrentStatus != BillStatus.Paid;
        bill.UpdatedAt = DateTimeOffset.UtcNow;

        _db.BillStatusHistory.Add(new BillStatusHistory
        {
            StatusHistoryId = Guid.NewGuid(),
            BillId = bill.BillId,
            Status = bill.CurrentStatus,
            ChangedAt = DateTimeOffset.UtcNow,
            ChangedBy = userId.ToString(),
            Notes = request.Notes ?? "Bill entry updated"
        });

        await _db.SaveChangesAsync(cancellationToken);
        await _audit.WriteAsync(userId, "UpdateEntry", "Bill", bill.BillId.ToString(), "Success", HttpContext, cancellationToken);

        bill.ProviderName = effectiveProviderName;

        return Ok(bill);
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteBill(Guid id, CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var bill = await _db.Bills.FirstOrDefaultAsync(x => x.BillId == id && x.UserId == userId, cancellationToken);
        if (bill is null)
        {
            return NotFound();
        }

        _db.Bills.Remove(bill);
        await _db.SaveChangesAsync(cancellationToken);
        await _audit.WriteAsync(userId, "Delete", "Bill", bill.BillId.ToString(), "Success", HttpContext, cancellationToken);
        return NoContent();
    }
}
