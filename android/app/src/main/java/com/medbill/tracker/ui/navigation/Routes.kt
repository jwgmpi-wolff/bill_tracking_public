package com.medbill.tracker.ui.navigation

sealed class Route(val path: String) {
    data object SignIn : Route("signin")
    data object Dashboard : Route("dashboard")
    data object Bills : Route("bills")
    data object AddBill : Route("add-bill")
    data object BillDetail : Route("bill-detail/{billId}") {
        fun withBillId(billId: String): String = "bill-detail/$billId"
    }
    data object Payment : Route("payment")
    data object Documents : Route("documents")
    data object Settings : Route("settings")
}
