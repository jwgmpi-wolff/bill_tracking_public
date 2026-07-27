using MedBill.Api.Data;
using MedBill.Api.Dtos;
using MedBill.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MedBill.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class DashboardController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly IUserContextService _userContext;

    public DashboardController(AppDbContext db, IUserContextService userContext)
    {
        _db = db;
        _userContext = userContext;
    }

    [HttpGet]
    public async Task<ActionResult<DashboardResponse>> Get(CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var bills = await _db.Bills.Where(x => x.UserId == userId).ToListAsync(cancellationToken);

        var totalOutstanding = bills.Where(x => x.CurrentStatus.ToString() != "Paid").Sum(x => x.PatientResponsibilityAmount - x.AmountPaid);
        var paidThisYear = bills.Where(x => x.UpdatedAt.Year == DateTimeOffset.UtcNow.Year).Sum(x => x.AmountPaid);
        var overdueCount = bills.Count(x => x.IsOverdue);
        var dueSoon = bills.Count(x => x.DueDate >= DateOnly.FromDateTime(DateTime.UtcNow) && x.DueDate <= DateOnly.FromDateTime(DateTime.UtcNow.AddDays(14)));

        return Ok(new DashboardResponse(totalOutstanding, paidThisYear, overdueCount, dueSoon));
    }

    [HttpGet("/api/audit")]
    public async Task<IActionResult> Audit(CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var rows = await _db.AuditLog.Where(x => x.UserId == userId).OrderByDescending(x => x.Timestamp).Take(200).ToListAsync(cancellationToken);
        return Ok(rows);
    }
}
