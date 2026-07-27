using System.Security.Claims;
using MedBill.Api.Data;
using MedBill.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace MedBill.Api.Services;

public class UserContextService : IUserContextService
{
    private readonly AppDbContext _db;

    public UserContextService(AppDbContext db)
    {
        _db = db;
    }

    public async Task<UserProfile?> GetOrCreateUserAsync(ClaimsPrincipal principal, CancellationToken cancellationToken)
    {
        var externalId = principal.FindFirstValue("oid") ?? principal.FindFirstValue("sub") ?? principal.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(externalId))
        {
            return null;
        }

        var profile = await _db.Users.FirstOrDefaultAsync(x => x.ExternalIdentityId == externalId, cancellationToken);
        if (profile is not null)
        {
            profile.LastLoginAt = DateTimeOffset.UtcNow;
            await _db.SaveChangesAsync(cancellationToken);
            return profile;
        }

        var email = principal.FindFirstValue("preferred_username") ?? principal.FindFirstValue(ClaimTypes.Email) ?? "unknown@example.com";
        var displayName = principal.FindFirstValue("name") ?? email;

        var newProfile = new UserProfile
        {
            UserId = Guid.NewGuid(),
            ExternalIdentityId = externalId,
            Email = email,
            DisplayName = displayName,
            CreatedAt = DateTimeOffset.UtcNow,
            LastLoginAt = DateTimeOffset.UtcNow
        };

        _db.Users.Add(newProfile);
        await _db.SaveChangesAsync(cancellationToken);
        return newProfile;
    }
}
