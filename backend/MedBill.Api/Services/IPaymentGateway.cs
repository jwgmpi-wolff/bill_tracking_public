namespace MedBill.Api.Services;

public interface IPaymentGateway
{
    Task<(string sessionId, string redirectUrl)> CreatePaymentSessionAsync(Guid billId, decimal amount, string currency, string method, CancellationToken cancellationToken);
    bool ValidateWebhook(string payload, string signature, string sharedSecret);
}
