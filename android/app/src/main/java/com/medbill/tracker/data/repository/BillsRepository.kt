package com.medbill.tracker.data.repository

import com.medbill.tracker.data.api.ApiModule
import com.medbill.tracker.data.model.Bill
import com.medbill.tracker.data.model.BillCreateRequest
import com.medbill.tracker.data.model.BillEntryUpdateRequest
import com.medbill.tracker.data.model.BillUpdateRequest
import com.medbill.tracker.data.model.Dashboard
import com.medbill.tracker.data.model.DocumentRecord
import com.medbill.tracker.data.model.PaymentSessionRequest
import com.medbill.tracker.data.model.PaymentSessionResponse
import com.medbill.tracker.data.model.PaymentMethod
import com.medbill.tracker.data.model.AuthMe
import com.medbill.tracker.data.model.Provider
import java.math.BigDecimal
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class BillsRepository {
    private val api = ApiModule.createApi()

    suspend fun me(): AuthMe = api.me()

    suspend fun getBills(): List<Bill> = api.getBills()

    suspend fun getBill(id: String): Bill = api.getBill(id)

    suspend fun createBill(request: BillCreateRequest): Bill = api.createBill(request)

    suspend fun updateBill(id: String, request: BillUpdateRequest): Bill = api.updateBill(id, request)

    suspend fun updateBillEntry(id: String, request: BillEntryUpdateRequest): Bill = api.updateBillEntry(id, request)

    suspend fun deleteBill(id: String) = api.deleteBill(id)

    suspend fun getDashboard(): Dashboard = api.getDashboard()

    suspend fun lookupProvidersByName(query: String): List<Provider> = api.getProviders(query)

    suspend fun getDocuments(billId: String): List<DocumentRecord> = api.getDocuments(billId)

    suspend fun uploadDocument(
        billId: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
        documentType: String
    ): DocumentRecord {
        val mediaType = contentType.toMediaType()
        val filePart = MultipartBody.Part.createFormData(
            "file",
            fileName,
            bytes.toRequestBody(mediaType)
        )
        val typePart = documentType.toRequestBody("text/plain".toMediaType())
        return api.uploadDocument(billId, filePart, typePart)
    }

    suspend fun deleteDocument(billId: String, documentId: String) {
        api.deleteDocument(billId, documentId)
    }

    suspend fun createPaymentSession(billId: String, amount: BigDecimal, method: PaymentMethod): PaymentSessionResponse {
        return api.createPaymentSession(billId, PaymentSessionRequest(amount, "USD", method.name))
    }
}
