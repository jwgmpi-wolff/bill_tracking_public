using MedBill.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace MedBill.Api.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly IUserContextService _userContext;

    public AuthController(IUserContextService userContext)
    {
        _userContext = userContext;
    }

    [HttpGet("me")]
    [Authorize]
    public async Task<IActionResult> Me(CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        return Ok(new
        {
            user.UserId,
            user.Email,
            user.DisplayName
        });
    }

    [HttpGet("session")]
    [Authorize]
    public IActionResult Session()
    {
        var oid = User.FindFirst("oid")?.Value;
        var tenantId = User.FindFirst("tid")?.Value;
        var subject = User.FindFirst("sub")?.Value;

        return Ok(new
        {
            authenticated = User.Identity?.IsAuthenticated ?? false,
            objectId = oid,
            tenantId,
            subject
        });
    }
}
