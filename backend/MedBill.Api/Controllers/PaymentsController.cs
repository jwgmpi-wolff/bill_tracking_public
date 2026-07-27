using System.Text.Json;
using MedBill.Api.Data;
using MedBill.Api.Dtos;
using MedBill.Api.Models;
using MedBill.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MedBill.Api.Controllers;

[ApiController]
[Route("api")]
public class PaymentsController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly IPaymentGateway _payments;
    private readonly IAuditService _audit;
    private readonly IConfiguration _configuration;
    private readonly IUserContextService _userContext;

    public PaymentsController(AppDbContext db, IPaymentGateway payments, IAuditService audit, IConfiguration configuration, IUserContextService userContext)
    {
        _db = db;
        _payments = payments;
        _audit = audit;
        _configuration = configuration;
        _userContext = userContext;
    }

    [HttpPost("bills/{billId:guid}/payments/session")]
    [Authorize]
    public async Task<ActionResult<PaymentSessionResponse>> CreateSession(Guid billId, [FromBody] PaymentSessionRequest request, CancellationToken cancellationToken)
    {
        var user = await _userContext.GetOrCreateUserAsync(User, cancellationToken);
        if (user is null)
        {
            return Unauthorized();
        }

        var userId = user.UserId;
        var bill = await _db.Bills.FirstOrDefaultAsync(x => x.BillId == billId && x.UserId == userId, cancellationToken);
        if (bill is null)
        {
            return NotFound();
        }

        var methodRaw = request.Method?.Trim();
        var isZelle = string.Equals(methodRaw, "Zelle", StringComparison.OrdinalIgnoreCase);
        var isCreditCard = string.Equals(methodRaw, "CreditCard", StringComparison.OrdinalIgnoreCase);
        if (!isZelle && !isCreditCard)
        {
            return BadRequest("Unsupported payment method.");
        }
        var method = isZelle ? "Zelle" : "CreditCard";

        var (sessionId, redirectUrl) = await _payments.CreatePaymentSessionAsync(billId, request.Amount, request.Currency, method, cancellationToken);
        _db.Payments.Add(new PaymentRecord
        {
            PaymentId = Guid.NewGuid(),
            BillId = billId,
            UserId = userId,
            PaymentProvider = method,
            PaymentSessionId = sessionId,
            Amount = request.Amount,
            Currency = request.Currency,
            PaymentStatus = "Pending",
            CreatedAt = DateTimeOffset.UtcNow
        });

        await _db.SaveChangesAsync(cancellationToken);
        await _audit.WriteAsync(userId, "Create", "PaymentSession", sessionId, "Success", HttpContext, cancellationToken);

        return Ok(new PaymentSessionResponse(method, sessionId, redirectUrl));
    }

    [HttpPost("payments/webhook")]
    [AllowAnonymous]
    public async Task<IActionResult> Webhook(CancellationToken cancellationToken)
    {
        using var reader = new StreamReader(Request.Body);
        var payload = await reader.ReadToEndAsync(cancellationToken);
        var signature = Request.Headers["x-provider-signature"].ToString();
        var secret = _configuration["Payments:WebhookSecret"] ?? string.Empty;

        if (!_payments.ValidateWebhook(payload, signature, secret))
        {
            return Unauthorized();
        }

        using var document = JsonDocument.Parse(payload);
        var sessionId = document.RootElement.GetProperty("sessionId").GetString();
        var transactionId = document.RootElement.GetProperty("transactionId").GetString();
        var status = document.RootElement.GetProperty("status").GetString() ?? "Unknown";

        var payment = await _db.Payments.FirstOrDefaultAsync(x => x.PaymentSessionId == sessionId, cancellationToken);
        if (payment is null)
        {
            return NotFound();
        }

        payment.TransactionId = transactionId ?? string.Empty;
        payment.PaymentStatus = status;
        payment.WebhookVerified = true;
        payment.PaidAt = status.Equals("Succeeded", StringComparison.OrdinalIgnoreCase) ? DateTimeOffset.UtcNow : null;

        if (status.Equals("Succeeded", StringComparison.OrdinalIgnoreCase))
        {
            var bill = await _db.Bills.FirstOrDefaultAsync(x => x.BillId == payment.BillId, cancellationToken);
            if (bill is not null)
            {
                bill.AmountPaid += payment.Amount;
                bill.CurrentStatus = bill.AmountPaid >= bill.PatientResponsibilityAmount ? BillStatus.Paid : BillStatus.PatientResponsibility;
                bill.UpdatedAt = DateTimeOffset.UtcNow;
            }
        }

        await _db.SaveChangesAsync(cancellationToken);
        await _audit.WriteAsync(payment.UserId, "Webhook", "Payment", payment.PaymentId.ToString(), "Success", HttpContext, cancellationToken);
        return Ok();
    }
}
