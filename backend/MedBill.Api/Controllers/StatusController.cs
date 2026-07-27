using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MedBill.Api.Services;

namespace MedBill.Api.Controllers;

[ApiController]
[AllowAnonymous]
public class StatusController : ControllerBase
{
    private readonly StartupState _startupState;

    public StatusController(StartupState startupState)
    {
        _startupState = startupState;
    }

    [HttpGet("/")]
    public IActionResult Root()
    {
        return Ok(new
        {
            service = "MedBill API",
            status = _startupState.IsDatabaseReady ? "ok" : "degraded",
            databaseReady = _startupState.IsDatabaseReady
        });
    }

    [HttpGet("/health")]
    public IActionResult Health()
    {
        var payload = new
        {
            status = _startupState.IsDatabaseReady ? "healthy" : "degraded",
            databaseReady = _startupState.IsDatabaseReady,
            error = _startupState.LastStartupError
        };

        if (!_startupState.IsDatabaseReady)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, payload);
        }

        return Ok(payload);
    }
}
