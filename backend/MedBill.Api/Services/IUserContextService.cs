using System.Security.Claims;
using MedBill.Api.Models;

namespace MedBill.Api.Services;

public interface IUserContextService
{
    Task<UserProfile?> GetOrCreateUserAsync(ClaimsPrincipal principal, CancellationToken cancellationToken);
}
