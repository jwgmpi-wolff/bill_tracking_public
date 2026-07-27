namespace MedBill.Api.Services;

public sealed class StartupState
{
    public bool IsDatabaseReady { get; set; } = true;
    public string? LastStartupError { get; set; }
}
