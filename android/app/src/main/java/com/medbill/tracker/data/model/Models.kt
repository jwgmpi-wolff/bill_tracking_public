package com.medbill.tracker.data.model

import java.math.BigDecimal

enum class BillStatus {
    New,
    SubmittedToInsurance,
    InsuranceProcessed,
    PatientResponsibility,
    Paid,
    Disputed,
    Overdue
}

enum class PaymentMethod {
    CreditCard,
    Zelle
}

data class Bill(
    val billId: String,
    val providerId: String,
    val providerName: String? = null,
    val invoiceNumber: String,
    val accountNumber: String?,
    val serviceDate: String,
    val billDate: String,
    val dueDate: String,
    val totalBilledAmount: BigDecimal,
    val insurancePaidAmount: BigDecimal,
    val discountAdjustmentAmount: BigDecimal,
    val patientResponsibilityAmount: BigDecimal,
    val amountPaid: BigDecimal,
    val currentStatus: BillStatus
)

data class Provider(
    val providerId: String,
    val providerName: String
)

data class DocumentRecord(
    val documentId: String,
    val billId: String,
    val documentType: String? = null,
    val fileName: String,
    val contentType: String,
    val uploadedAt: String,
    val sha256Hash: String
)

data class BillCreateRequest(
    val providerId: String,
    val providerName: String?,
    val invoiceNumber: String,
    val accountNumber: String?,
    val serviceDate: String,
    val billDate: String,
    val dueDate: String,
    val totalBilledAmount: BigDecimal,
    val insurancePaidAmount: BigDecimal,
    val discountAdjustmentAmount: BigDecimal,
    val patientResponsibilityAmount: BigDecimal,
    val notes: String?
)

data class BillUpdateRequest(
    val currentStatus: BillStatus,
    val amountPaid: BigDecimal,
    val notes: String?
)

data class BillEntryUpdateRequest(
    val providerId: String,
    val providerName: String?,
    val invoiceNumber: String,
    val accountNumber: String?,
    val serviceDate: String,
    val billDate: String,
    val dueDate: String,
    val totalBilledAmount: BigDecimal,
    val insurancePaidAmount: BigDecimal,
    val discountAdjustmentAmount: BigDecimal,
    val patientResponsibilityAmount: BigDecimal,
    val notes: String?
)

data class Dashboard(
    val totalOutstanding: BigDecimal,
    val paidThisYear: BigDecimal,
    val overdueCount: Int,
    val dueInNext14Days: Int
)

data class PaymentSessionRequest(
    val amount: BigDecimal,
    val currency: String,
    val method: String
)

data class PaymentSessionResponse(
    val provider: String,
    val sessionId: String,
    val redirectUrl: String
)

data class AuthMe(
    val userId: String,
    val email: String,
    val displayName: String
)
