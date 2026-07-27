package com.medbill.tracker.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medbill.tracker.auth.AuthManager
import com.medbill.tracker.data.model.Bill
import com.medbill.tracker.data.model.BillCreateRequest
import com.medbill.tracker.data.model.BillEntryUpdateRequest
import com.medbill.tracker.data.model.DocumentRecord
import com.medbill.tracker.data.model.PaymentMethod
import com.medbill.tracker.data.model.Provider
import com.medbill.tracker.ui.components.MedBillBottomNav
import com.medbill.tracker.ui.navigation.Route
import com.medbill.tracker.vm.AuthViewModel
import com.medbill.tracker.vm.BillsViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class BillSample(
    val providerName: String,
    val providerId: String,
    val invoiceNumber: String,
    val accountNumber: String,
    val serviceDate: String,
    val billDate: String,
    val dueDate: String,
    val totalBilledAmount: String,
    val insurancePaidAmount: String,
    val discountAdjustmentAmount: String,
    val patientResponsibilityAmount: String,
    val notes: String
)

private val norcoSample = BillSample(
    providerName = "Norco",
    providerId = "7f3d6c25-0a8f-4cf8-b0dc-9348786a4a2c",
    invoiceNumber = "46482433",
    accountNumber = "CWI606",
    serviceDate = "2026-05-15",
    billDate = "2026-06-29",
    dueDate = "2026-08-01",
    totalBilledAmount = "128.92",
    insurancePaidAmount = "0.00",
    discountAdjustmentAmount = "0.00",
    patientResponsibilityAmount = "128.92",
    notes = "Norco statement sample from image"
)

private val contosoSample = BillSample(
    providerName = "Contoso Medical Group",
    providerId = "2b6c02c4-ec61-46e4-9e90-2ef376f6017f",
    invoiceNumber = "INV-1001",
    accountNumber = "ACCT-5001",
    serviceDate = "2026-01-01",
    billDate = "2026-01-15",
    dueDate = "2026-02-01",
    totalBilledAmount = "350.00",
    insurancePaidAmount = "225.00",
    discountAdjustmentAmount = "0.00",
    patientResponsibilityAmount = "125.00",
    notes = "Contoso sample"
)

private val footAndAnkleSample = BillSample(
    providerName = "Foot & Ankle Centers of Washington",
    providerId = "8cd0fd53-08b8-4d62-aad6-4d0742a6b670",
    invoiceNumber = "881483",
    accountNumber = "20258080",
    serviceDate = "2026-05-04",
    billDate = "2026-06-13",
    dueDate = "2026-07-13",
    totalBilledAmount = "244.05",
    insurancePaidAmount = "0.00",
    discountAdjustmentAmount = "125.70",
    patientResponsibilityAmount = "118.35",
    notes = "Foot & Ankle statement sample from image"
)

private val premeraSample = BillSample(
    providerName = "Premera Blue Cross (EOB)",
    providerId = "6c53ed6d-1a4a-4919-9e02-7d6a9df276de",
    invoiceNumber = "877358697300",
    accountNumber = "88776",
    serviceDate = "2026-06-08",
    billDate = "2026-06-13",
    dueDate = "2026-07-13",
    totalBilledAmount = "490.25",
    insurancePaidAmount = "224.39",
    discountAdjustmentAmount = "240.93",
    patientResponsibilityAmount = "24.93",
    notes = "Premera EOB sample from image"
)

private val deltaDentalSample = BillSample(
    providerName = "Delta Dental",
    providerId = "f43225b0-4f66-4e14-a1f9-5f5c9bd2d87f",
    invoiceNumber = "20261390518300",
    accountNumber = "96514569",
    serviceDate = "2026-05-15",
    billDate = "2026-05-21",
    dueDate = "2026-06-20",
    totalBilledAmount = "147.00",
    insurancePaidAmount = "147.00",
    discountAdjustmentAmount = "0.00",
    patientResponsibilityAmount = "0.00",
    notes = "Delta Dental EOB sample from image"
)

private data class ScannedBillData(
    val providerName: String?,
    val providerId: String?,
    val invoiceNumber: String?,
    val accountNumber: String?,
    val serviceDate: String?,
    val billDate: String?,
    val dueDate: String?,
    val totalBilledAmount: String?,
    val insurancePaidAmount: String?,
    val discountAdjustmentAmount: String?,
    val patientResponsibilityAmount: String?
)

private fun normalizeDate(raw: String): String? {
    val parts = raw.replace('.', '-').replace('/', '-').split('-')
    if (parts.size != 3) {
        return null
    }

    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null

    if (year !in 2000..2100 || month !in 1..12 || day !in 1..31) {
        return null
    }

    return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
}

private fun findDateForKeywords(lines: List<String>, keywords: List<String>): String? {
    val dateRegex = Regex("\\b(20\\d{2}[./-]\\d{1,2}[./-]\\d{1,2})\\b")
    for (line in lines) {
        val lower = line.lowercase()
        if (keywords.any { lower.contains(it) }) {
            val match = dateRegex.find(line)?.groupValues?.get(1)
            val normalized = match?.let { normalizeDate(it) }
            if (!normalized.isNullOrBlank()) {
                return normalized
            }
        }
    }
    return null
}

private fun extractAmountForKeywords(lines: List<String>, keywords: List<String>): String? {
    val amountRegex = Regex("(?<!\\d)(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})|\\d+\\.\\d{2})(?!\\d)")
    for (line in lines) {
        val lower = line.lowercase()
        if (keywords.any { lower.contains(it) }) {
            val amount = amountRegex.find(line)?.groupValues?.get(1)?.replace(",", "")
            if (!amount.isNullOrBlank()) {
                return amount
            }
        }
    }
    return null
}

private fun parseBillFromOcrText(text: String): ScannedBillData {
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    val joinedLower = lines.joinToString("\n").lowercase()

    val providerName = when {
        "delta dental" in joinedLower -> "Delta Dental"
        "premera" in joinedLower -> "Premera Blue Cross (EOB)"
        "foot & ankle" in joinedLower || "foot and ankle" in joinedLower -> "Foot & Ankle Centers of Washington"
        "norco" in joinedLower -> "Norco"
        else -> null
    }

    val providerId = when (providerName) {
        "Delta Dental" -> deltaDentalSample.providerId
        "Premera Blue Cross (EOB)" -> premeraSample.providerId
        "Foot & Ankle Centers of Washington" -> footAndAnkleSample.providerId
        "Norco" -> norcoSample.providerId
        else -> null
    }

    val invoiceRegex = Regex("(?:invoice|claim|statement|bill)\\s*(?:#|number|no\\.?|id)?\\s*[:\\-]?\\s*([A-Za-z0-9-]{4,})", RegexOption.IGNORE_CASE)
    val accountRegex = Regex("(?:account|member)\\s*(?:#|number|no\\.?|id)?\\s*[:\\-]?\\s*([A-Za-z0-9-]{4,})", RegexOption.IGNORE_CASE)
    val allDates = Regex("\\b20\\d{2}[./-]\\d{1,2}[./-]\\d{1,2}\\b")
        .findAll(text)
        .mapNotNull { normalizeDate(it.value) }
        .distinct()
        .toList()

    val dueDate = findDateForKeywords(lines, listOf("due")) ?: allDates.getOrNull(2)
    val serviceDate = findDateForKeywords(lines, listOf("service", "date of service")) ?: allDates.firstOrNull()
    val billDate = findDateForKeywords(lines, listOf("bill date", "statement date", "date issued")) ?: allDates.getOrNull(1)

    return ScannedBillData(
        providerName = providerName,
        providerId = providerId,
        invoiceNumber = invoiceRegex.find(text)?.groupValues?.getOrNull(1),
        accountNumber = accountRegex.find(text)?.groupValues?.getOrNull(1),
        serviceDate = serviceDate,
        billDate = billDate,
        dueDate = dueDate,
        totalBilledAmount = extractAmountForKeywords(lines, listOf("total billed", "total amount", "amount billed", "charges")),
        insurancePaidAmount = extractAmountForKeywords(lines, listOf("insurance paid", "paid by plan", "plan paid", "benefit paid")),
        discountAdjustmentAmount = extractAmountForKeywords(lines, listOf("adjustment", "discount", "contractual", "write-off")),
        patientResponsibilityAmount = extractAmountForKeywords(lines, listOf("patient responsibility", "you owe", "amount due", "member responsibility"))
    )
}

private suspend fun scanBillFromImage(context: android.content.Context, imageUri: Uri): ScannedBillData {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        val image = InputImage.fromFilePath(context, imageUri)
        val result = recognizer.process(image).await()
        if (result.text.isBlank()) {
            throw IllegalStateException("No readable text found in image")
        }
        parseBillFromOcrText(result.text)
    } finally {
        recognizer.close()
    }
}

private fun readUriBytes(context: android.content.Context, uri: Uri): ByteArray? {
    return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
}

private fun resolveUriFileName(context: android.content.Context, uri: Uri): String {
    var displayName: String? = null
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            displayName = cursor.getString(index)
        }
    }

    return displayName ?: "upload-${System.currentTimeMillis()}"
}

private fun resolveContentType(context: android.content.Context, uri: Uri): String {
    return context.contentResolver.getType(uri) ?: "application/octet-stream"
}

private const val PREFS_NAME = "medbill_prefs"
private const val KEY_DEFAULT_PAYMENT_METHOD = "default_payment_method"

private fun readSavedPaymentMethod(context: android.content.Context): PaymentMethod {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_DEFAULT_PAYMENT_METHOD, PaymentMethod.CreditCard.name) ?: PaymentMethod.CreditCard.name
    return runCatching { PaymentMethod.valueOf(raw) }.getOrDefault(PaymentMethod.CreditCard)
}

private fun saveDefaultPaymentMethod(context: android.content.Context, method: PaymentMethod) {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_DEFAULT_PAYMENT_METHOD, method.name).apply()
}

@Composable
fun AppRoot(billsViewModel: BillsViewModel) {
    val context = LocalContext.current
    val authViewModel = remember { AuthViewModel(AuthManager(context)) }
    val authState by authViewModel.state.collectAsState()
    val billsState by billsViewModel.state.collectAsState()
    val navController = rememberNavController()
    val backstack = navController.currentBackStackEntryAsState().value
    val currentRoute = backstack?.destination?.route

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            billsViewModel.refresh()
            navController.navigate(Route.Dashboard.path) {
                popUpTo(Route.SignIn.path) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (authState.isAuthenticated && currentRoute != Route.SignIn.path) {
                MedBillBottomNav(navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (authState.isAuthenticated) Route.Dashboard.path else Route.SignIn.path,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.SignIn.path) {
                SignInScreen(
                    loading = authState.loading,
                    error = authState.error,
                    onSignIn = { authViewModel.signIn(context as Activity) },
                    onUseDifferentAccount = { authViewModel.signInWithDifferentAccount(context as Activity) }
                )
            }

            composable(Route.Dashboard.path) {
                DashboardScreen(
                    loading = billsState.loading,
                    totalOutstanding = billsState.dashboard?.totalOutstanding?.toPlainString() ?: "0.00",
                    paidThisYear = billsState.dashboard?.paidThisYear?.toPlainString() ?: "0.00",
                    overdueCount = billsState.dashboard?.overdueCount ?: 0,
                    dueSoon = billsState.dashboard?.dueInNext14Days ?: 0
                )
            }

            composable(Route.Bills.path) {
                BillsScreen(
                    bills = billsState.bills,
                    onSelect = {
                        billsViewModel.selectBill(it.billId)
                        navController.navigate(Route.BillDetail.withBillId(it.billId))
                    }
                )
            }

            composable(Route.AddBill.path) {
                AddBillScreen(
                    loading = billsState.loading,
                    providerLookupLoading = billsState.providerLookupLoading,
                    providerMatches = billsState.providerMatches,
                    providerLookupError = billsState.providerLookupError,
                    error = billsState.error,
                    onProviderQuery = { query -> billsViewModel.lookupProvidersByName(query) },
                    onProviderSelected = { _ -> billsViewModel.clearProviderLookup() }
                ) { request, scannedUri ->
                    billsViewModel.addBill(request) { saved, created ->
                        if (saved) {
                            if (created != null && scannedUri != null) {
                                val bytes = readUriBytes(context, scannedUri)
                                if (bytes != null) {
                                    val fileName = resolveUriFileName(context, scannedUri)
                                    val contentType = resolveContentType(context, scannedUri)
                                    billsViewModel.uploadDocument(
                                        billId = created.billId,
                                        fileName = fileName,
                                        contentType = contentType,
                                        bytes = bytes,
                                        documentType = "Scan"
                                    ) {
                                        navController.navigate(Route.Bills.path)
                                    }
                                } else {
                                    navController.navigate(Route.Bills.path)
                                }
                            } else {
                                navController.navigate(Route.Bills.path)
                            }
                        }
                    }
                }
            }

            composable(
                route = Route.BillDetail.path,
                arguments = listOf(navArgument("billId") { type = NavType.StringType })
            ) {
                BillDetailScreen(
                    bill = billsState.selectedBill,
                    loading = billsState.loading,
                    error = billsState.error,
                    onPay = { navController.navigate(Route.Payment.path) },
                    onViewDocs = { navController.navigate(Route.Documents.path) },
                    onSaveEntry = { billId, update -> billsViewModel.updateBillEntry(billId, update) },
                    onDelete = { billId ->
                        billsViewModel.deleteBill(billId) { deleted ->
                            if (deleted) {
                                navController.navigate(Route.Bills.path) {
                                    popUpTo(Route.Bills.path) { inclusive = false }
                                }
                            }
                        }
                    }
                )
            }

            composable(Route.Payment.path) {
                PaymentScreen(
                    bill = billsState.selectedBill,
                    redirectUrl = billsState.paymentRedirectUrl,
                    onPay = { amount, method -> billsViewModel.createPaymentSession(amount, method) },
                    onOpenPayment = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        billsViewModel.clearPaymentRedirect()
                    }
                )
            }

            composable(Route.Documents.path) {
                DocumentsScreen(
                    billId = billsState.selectedBill?.billId,
                    documents = billsState.documents,
                    uploading = billsState.documentUploadLoading,
                    error = billsState.error,
                    onUpload = { billId, uri, type ->
                        val bytes = readUriBytes(context, uri)
                        if (bytes != null) {
                            val fileName = resolveUriFileName(context, uri)
                            val contentType = resolveContentType(context, uri)
                            billsViewModel.uploadDocument(
                                billId = billId,
                                fileName = fileName,
                                contentType = contentType,
                                bytes = bytes,
                                documentType = type
                            )
                        }
                    },
                    onDelete = { billId, documentId ->
                        billsViewModel.deleteDocument(billId, documentId)
                    }
                )
            }

            composable(Route.Settings.path) {
                SettingsScreen(onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Route.SignIn.path) {
                        popUpTo(Route.Dashboard.path) { inclusive = true }
                    }
                })
            }
        }
    }
}

@Composable
private fun SignInScreen(
    loading: Boolean,
    error: String?,
    onSignIn: () -> Unit,
    onUseDifferentAccount: () -> Unit
) {
    val canUseDifferentAccount = error?.contains("already signed in", ignoreCase = true) == true

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MedBill Tracker", style = MaterialTheme.typography.headlineMedium)
        Text("Sign in with Microsoft Entra ID to continue", modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        Button(onClick = onSignIn, enabled = !loading) {
            Text(if (loading) "Signing in..." else "Sign in")
        }
        if (canUseDifferentAccount) {
            Button(
                onClick = onUseDifferentAccount,
                enabled = !loading,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Use different account")
            }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun DashboardScreen(loading: Boolean, totalOutstanding: String, paidThisYear: String, overdueCount: Int, dueSoon: Int) {
    if (loading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        StatCard("Total Outstanding", "$totalOutstanding")
        StatCard("Paid This Year", "$paidThisYear")
        StatCard("Overdue Bills", overdueCount.toString())
        StatCard("Due in 14 Days", dueSoon.toString())
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun BillsScreen(bills: List<Bill>, onSelect: (Bill) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bills", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(bills) { bill ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onSelect(bill) }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Invoice ${bill.invoiceNumber}", style = MaterialTheme.typography.titleMedium)
                        if (!bill.providerName.isNullOrBlank()) {
                            Text("Provider ${bill.providerName}")
                        }
                        Text("Due ${bill.dueDate}")
                        Text("Status ${bill.currentStatus}")
                        Text("Patient responsibility ${bill.patientResponsibilityAmount}")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBillScreen(
    loading: Boolean,
    providerLookupLoading: Boolean,
    providerMatches: List<Provider>,
    providerLookupError: String?,
    error: String?,
    onProviderQuery: (String) -> Unit,
    onProviderSelected: (Provider) -> Unit,
    onSubmit: (BillCreateRequest, Uri?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var providerSearch by remember { mutableStateOf("") }
    var providerId by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var serviceDate by remember { mutableStateOf("2026-01-01") }
    var billDate by remember { mutableStateOf("2026-01-02") }
    var dueDate by remember { mutableStateOf("2026-01-30") }
    var totalBilledAmount by remember { mutableStateOf("0.00") }
    var insurancePaidAmount by remember { mutableStateOf("0.00") }
    var discountAdjustmentAmount by remember { mutableStateOf("0.00") }
    var patientResponsibilityAmount by remember { mutableStateOf("0.00") }
    var notes by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var scanInProgress by remember { mutableStateOf(false) }
    var scannedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        scannedImageUri = uri

        scope.launch {
            scanInProgress = true
            scanError = null
            runCatching {
                scanBillFromImage(context, uri)
            }.onSuccess { scanned ->
                if (!scanned.providerName.isNullOrBlank()) {
                    providerSearch = scanned.providerName
                }
                if (!scanned.providerId.isNullOrBlank()) {
                    providerId = scanned.providerId
                }
                if (!scanned.invoiceNumber.isNullOrBlank()) {
                    invoiceNumber = scanned.invoiceNumber
                }
                if (!scanned.accountNumber.isNullOrBlank()) {
                    accountNumber = scanned.accountNumber
                }
                if (!scanned.serviceDate.isNullOrBlank()) {
                    serviceDate = scanned.serviceDate
                }
                if (!scanned.billDate.isNullOrBlank()) {
                    billDate = scanned.billDate
                }
                if (!scanned.dueDate.isNullOrBlank()) {
                    dueDate = scanned.dueDate
                }
                if (!scanned.totalBilledAmount.isNullOrBlank()) {
                    totalBilledAmount = scanned.totalBilledAmount
                }
                if (!scanned.insurancePaidAmount.isNullOrBlank()) {
                    insurancePaidAmount = scanned.insurancePaidAmount
                }
                if (!scanned.discountAdjustmentAmount.isNullOrBlank()) {
                    discountAdjustmentAmount = scanned.discountAdjustmentAmount
                }
                if (!scanned.patientResponsibilityAmount.isNullOrBlank()) {
                    patientResponsibilityAmount = scanned.patientResponsibilityAmount
                }
                if (notes.isBlank()) {
                    notes = "Scanned from picture"
                }
            }.onFailure {
                scanError = "Could not scan image. You can still enter manually."
            }
            scanInProgress = false
        }
    }

    fun applySample(sample: BillSample) {
        providerSearch = sample.providerName
        providerId = sample.providerId
        invoiceNumber = sample.invoiceNumber
        accountNumber = sample.accountNumber
        serviceDate = sample.serviceDate
        billDate = sample.billDate
        dueDate = sample.dueDate
        totalBilledAmount = sample.totalBilledAmount
        insurancePaidAmount = sample.insurancePaidAmount
        discountAdjustmentAmount = sample.discountAdjustmentAmount
        patientResponsibilityAmount = sample.patientResponsibilityAmount
        notes = sample.notes
        localError = null
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Add Bill", style = MaterialTheme.typography.headlineMedium) }
        item {
            Button(enabled = !loading && !scanInProgress, onClick = { imagePicker.launch("image/*") }) {
                Text(if (scanInProgress) "Scanning..." else "Scan from Picture")
            }
        }
        item { Text("You can scan a bill/EOB photo, then adjust any field manually.") }
        if (!scanError.isNullOrBlank()) {
            item { Text(scanError.orEmpty(), color = MaterialTheme.colorScheme.error) }
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(enabled = !loading, onClick = { applySample(norcoSample) }) {
                    Text("Use Norco Sample")
                }
                Button(enabled = !loading, onClick = { applySample(footAndAnkleSample) }) {
                    Text("Use Foot & Ankle")
                }
                Button(enabled = !loading, onClick = { applySample(premeraSample) }) {
                    Text("Use Premera")
                }
                Button(enabled = !loading, onClick = { applySample(deltaDentalSample) }) {
                    Text("Use Delta Dental")
                }
                Button(enabled = !loading, onClick = { applySample(contosoSample) }) {
                    Text("Use Contoso Sample")
                }
            }
        }
        item {
            OutlinedTextField(
                value = providerSearch,
                onValueChange = {
                    providerSearch = it
                    if (it.trim().length >= 2) {
                        onProviderQuery(it)
                    } else {
                        onProviderQuery("")
                    }
                },
                label = { Text("Provider Name Lookup") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (providerLookupLoading) {
            item { Text("Searching providers...") }
        }
        if (!providerLookupError.isNullOrBlank()) {
            item { Text(providerLookupError, color = MaterialTheme.colorScheme.error) }
        }
        if (providerMatches.isNotEmpty()) {
            items(providerMatches) { provider ->
                Card(modifier = Modifier.fillMaxWidth().clickable {
                    providerSearch = provider.providerName
                    providerId = provider.providerId
                    onProviderSelected(provider)
                }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(provider.providerName, style = MaterialTheme.typography.titleMedium)
                        Text("ID ${provider.providerId}")
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = providerId,
                onValueChange = { providerId = it },
                label = { Text("Provider ID") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, label = { Text("Invoice Number") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("Account Number") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = serviceDate, onValueChange = { serviceDate = it }, label = { Text("Service Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = billDate, onValueChange = { billDate = it }, label = { Text("Bill Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = totalBilledAmount, onValueChange = { totalBilledAmount = it }, label = { Text("Total Billed") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = insurancePaidAmount, onValueChange = { insurancePaidAmount = it }, label = { Text("Insurance Paid") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = discountAdjustmentAmount, onValueChange = { discountAdjustmentAmount = it }, label = { Text("Adjustments") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = patientResponsibilityAmount, onValueChange = { patientResponsibilityAmount = it }, label = { Text("Patient Responsibility") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()) }
        if (!error.isNullOrBlank()) {
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }
        if (!localError.isNullOrBlank()) {
            item { Text(localError.orEmpty(), color = MaterialTheme.colorScheme.error) }
        }
        item {
            Button(enabled = !loading, onClick = {
                localError = null

                val parsedProvider = runCatching { UUID.fromString(providerId) }.getOrNull()
                if (parsedProvider == null) {
                    localError = "Provider ID must be a valid GUID."
                    return@Button
                }

                if (invoiceNumber.isBlank()) {
                    localError = "Invoice Number is required."
                    return@Button
                }

                val total = totalBilledAmount.toBigDecimalOrNull()
                val insurance = insurancePaidAmount.toBigDecimalOrNull()
                val adjustments = discountAdjustmentAmount.toBigDecimalOrNull()
                val responsibility = patientResponsibilityAmount.toBigDecimalOrNull()

                if (total == null || insurance == null || adjustments == null || responsibility == null) {
                    localError = "Amounts must be valid decimals like 128.00 (use only one decimal point)."
                    return@Button
                }

                onSubmit(
                    BillCreateRequest(
                        providerId = parsedProvider.toString(),
                        providerName = providerSearch.ifBlank { null },
                        invoiceNumber = invoiceNumber,
                        accountNumber = accountNumber.ifBlank { null },
                        serviceDate = serviceDate,
                        billDate = billDate,
                        dueDate = dueDate,
                        totalBilledAmount = total,
                        insurancePaidAmount = insurance,
                        discountAdjustmentAmount = adjustments,
                        patientResponsibilityAmount = responsibility,
                        notes = notes.ifBlank { null }
                    ),
                    scannedImageUri
                )
            }) {
                Text(if (loading) "Saving..." else "Save Bill")
            }
        }
        if (loading) {
            item {
                TextButton(enabled = false, onClick = {}) {
                    Text("Please wait while bill and dashboard refresh")
                }
            }
        }
    }
}

@Composable
private fun BillDetailScreen(
    bill: Bill?,
    loading: Boolean,
    error: String?,
    onPay: () -> Unit,
    onViewDocs: () -> Unit,
    onSaveEntry: (String, BillEntryUpdateRequest) -> Unit,
    onDelete: (String) -> Unit
) {
    if (bill == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Select a bill from Bills tab.")
        }
        return
    }

    var providerId by remember(bill.billId) { mutableStateOf(bill.providerId) }
    var providerName by remember(bill.billId) { mutableStateOf(bill.providerName.orEmpty()) }
    var invoiceNumber by remember(bill.billId) { mutableStateOf(bill.invoiceNumber) }
    var accountNumber by remember(bill.billId) { mutableStateOf(bill.accountNumber.orEmpty()) }
    var serviceDate by remember(bill.billId) { mutableStateOf(bill.serviceDate) }
    var billDate by remember(bill.billId) { mutableStateOf(bill.billDate) }
    var dueDate by remember(bill.billId) { mutableStateOf(bill.dueDate) }
    var totalBilledAmount by remember(bill.billId) { mutableStateOf(bill.totalBilledAmount.toPlainString()) }
    var insurancePaidAmount by remember(bill.billId) { mutableStateOf(bill.insurancePaidAmount.toPlainString()) }
    var discountAdjustmentAmount by remember(bill.billId) { mutableStateOf(bill.discountAdjustmentAmount.toPlainString()) }
    var patientResponsibilityAmount by remember(bill.billId) { mutableStateOf(bill.patientResponsibilityAmount.toPlainString()) }
    var localError by remember(bill.billId) { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Bill Detail", style = MaterialTheme.typography.headlineMedium) }
        if (!bill.providerName.isNullOrBlank()) {
            item { Text("Provider ${bill.providerName}") }
        }
        item { Text("Status ${bill.currentStatus}") }
        item { Text("Amount paid ${bill.amountPaid}") }

        item { OutlinedTextField(value = providerId, onValueChange = { providerId = it }, label = { Text("Provider ID") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = providerName, onValueChange = { providerName = it }, label = { Text("Provider Name") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, label = { Text("Invoice Number") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("Account Number") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = serviceDate, onValueChange = { serviceDate = it }, label = { Text("Service Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = billDate, onValueChange = { billDate = it }, label = { Text("Bill Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = totalBilledAmount, onValueChange = { totalBilledAmount = it }, label = { Text("Total Billed") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = insurancePaidAmount, onValueChange = { insurancePaidAmount = it }, label = { Text("Insurance Paid") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = discountAdjustmentAmount, onValueChange = { discountAdjustmentAmount = it }, label = { Text("Adjustments") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = patientResponsibilityAmount, onValueChange = { patientResponsibilityAmount = it }, label = { Text("Patient Responsibility") }, modifier = Modifier.fillMaxWidth()) }

        if (!localError.isNullOrBlank()) {
            item { Text(localError.orEmpty(), color = MaterialTheme.colorScheme.error) }
        }
        if (!error.isNullOrBlank()) {
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }

        item {
            Button(enabled = !loading, onClick = {
            localError = null

            val parsedProvider = runCatching { UUID.fromString(providerId) }.getOrNull()
            if (parsedProvider == null) {
                localError = "Provider ID must be a valid GUID."
                return@Button
            }

            if (invoiceNumber.isBlank()) {
                localError = "Invoice Number is required."
                return@Button
            }

            val total = totalBilledAmount.toBigDecimalOrNull()
            val insurance = insurancePaidAmount.toBigDecimalOrNull()
            val adjustments = discountAdjustmentAmount.toBigDecimalOrNull()
            val responsibility = patientResponsibilityAmount.toBigDecimalOrNull()
            if (total == null || insurance == null || adjustments == null || responsibility == null) {
                localError = "Amounts must be valid decimals like 128.00."
                return@Button
            }

            onSaveEntry(
                bill.billId,
                BillEntryUpdateRequest(
                    providerId = parsedProvider.toString(),
                    providerName = providerName.ifBlank { null },
                    invoiceNumber = invoiceNumber,
                    accountNumber = accountNumber.ifBlank { null },
                    serviceDate = serviceDate,
                    billDate = billDate,
                    dueDate = dueDate,
                    totalBilledAmount = total,
                    insurancePaidAmount = insurance,
                    discountAdjustmentAmount = adjustments,
                    patientResponsibilityAmount = responsibility,
                    notes = "Entry edited in app"
                )
            )
            }) {
                Text(if (loading) "Saving Changes..." else "Save Entry Changes")
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPay, enabled = !loading) { Text("Pay") }
                Button(onClick = onViewDocs, enabled = !loading) { Text("Documents") }
                Button(onClick = { onDelete(bill.billId) }, enabled = !loading) {
                    Text(if (loading) "Deleting..." else "Delete Bill")
                }
            }
        }
    }
}

@Composable
private fun PaymentScreen(bill: Bill?, redirectUrl: String?, onPay: (BigDecimal, PaymentMethod) -> Unit, onOpenPayment: (String) -> Unit) {
    val context = LocalContext.current
    var selectedMethod by remember { mutableStateOf(readSavedPaymentMethod(context)) }
    var saveAsDefault by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Payment", style = MaterialTheme.typography.headlineMedium)
        if (bill == null) {
            Text("No bill selected.")
        } else {
            val due = (bill.patientResponsibilityAmount - bill.amountPaid).coerceAtLeast(BigDecimal.ZERO)
            Text("Invoice ${bill.invoiceNumber}")
            Text("Amount Due $due")
            Text("Payment method")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedMethod == PaymentMethod.CreditCard,
                    onClick = { selectedMethod = PaymentMethod.CreditCard }
                )
                Text("Credit Card")
                RadioButton(
                    selected = selectedMethod == PaymentMethod.Zelle,
                    onClick = { selectedMethod = PaymentMethod.Zelle }
                )
                Text("Zelle")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = saveAsDefault, onCheckedChange = { saveAsDefault = it })
                Text("Save this as my default payment method")
            }

            Button(onClick = {
                if (saveAsDefault) {
                    saveDefaultPaymentMethod(context, selectedMethod)
                }
                onPay(due, selectedMethod)
            }) {
                Text(if (selectedMethod == PaymentMethod.Zelle) "Pay with Zelle" else "Pay with Credit Card")
            }

            if (!redirectUrl.isNullOrBlank()) {
                Text("Payment session ready")
                Button(onClick = { onOpenPayment(redirectUrl) }) {
                    Text(if (selectedMethod == PaymentMethod.Zelle) "Open Zelle" else "Open Secure Payment Page")
                }
            }
        }
    }
}

@Composable
private fun DocumentsScreen(
    billId: String?,
    documents: List<DocumentRecord>,
    uploading: Boolean,
    error: String?,
    onUpload: (billId: String, uri: Uri, documentType: String) -> Unit,
    onDelete: (billId: String, documentId: String) -> Unit
) {
    val scanPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !billId.isNullOrBlank()) {
            onUpload(billId, uri, "Scan")
        }
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !billId.isNullOrBlank()) {
            onUpload(billId, uri, "Document")
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Documents", style = MaterialTheme.typography.headlineMedium)

        if (billId.isNullOrBlank()) {
            Text("Select a bill first to upload or view documents.", modifier = Modifier.padding(top = 10.dp))
        } else {
            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(enabled = !uploading, onClick = { scanPicker.launch("image/*") }) {
                    Text(if (uploading) "Uploading..." else "Upload Scan")
                }
                Button(enabled = !uploading, onClick = { docPicker.launch("*/*") }) {
                    Text("Upload Document")
                }
            }

            if (!error.isNullOrBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
            }

            LazyColumn(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(documents) { doc ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(doc.fileName, style = MaterialTheme.typography.titleMedium)
                            Text("Type ${doc.documentType ?: "Document"}")
                            Text(doc.contentType)
                            Text("Uploaded ${doc.uploadedAt}")
                            Button(
                                onClick = { onDelete(billId, doc.documentId) },
                                enabled = !uploading,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(if (uploading) "Working..." else "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onSignOut: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Profile")
        Text("Notifications")
        Text("Export Data")
        Text("Delete Account Request")
        Button(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}
