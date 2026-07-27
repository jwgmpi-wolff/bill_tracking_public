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
public class ProvidersController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly IUserContextService _userContext;

    public ProvidersController(AppDbContext db, IUserContextService userContext)
    {
        _db = db;
        _userContext = userContext;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<ProviderLookupResponse>>> Search([FromQuery] string? query, CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var nameQuery = (query ?? string.Empty).Trim();

        var providersQuery = _db.Providers.AsNoTracking().Where(x => x.UserId == userId);
        if (!string.IsNullOrWhiteSpace(nameQuery))
        {
            providersQuery = providersQuery.Where(x => x.ProviderName.Contains(nameQuery));
        }

        var rows = await providersQuery
            .OrderBy(x => x.ProviderName)
            .Take(20)
            .Select(x => new ProviderLookupResponse(x.ProviderId, x.ProviderName, x.BillingPhone, x.BillingPortalUrl))
            .ToListAsync(cancellationToken);

        return Ok(rows);
    }
}
