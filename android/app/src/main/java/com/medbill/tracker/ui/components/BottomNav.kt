package com.medbill.tracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.medbill.tracker.ui.navigation.Route

data class NavItem(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
fun MedBillBottomNav(navController: NavHostController) {
    val items = listOf(
        NavItem(Route.Dashboard.path, "Dashboard") { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
        NavItem(Route.Bills.path, "Bills") { Icon(Icons.Default.Receipt, contentDescription = "Bills") },
        NavItem(Route.AddBill.path, "Add") { Icon(Icons.Default.Add, contentDescription = "Add Bill") },
        NavItem(Route.Documents.path, "Docs") { Icon(Icons.Default.Description, contentDescription = "Documents") },
        NavItem(Route.Settings.path, "Settings") { Icon(Icons.Default.Settings, contentDescription = "Settings") }
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) },
                icon = item.icon,
                label = { Text(item.label) }
            )
        }
    }
}
