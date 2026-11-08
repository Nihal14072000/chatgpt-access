package com.poultrycrm.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private data class CrmModule(
    val name: String,
    val description: String,
    val route: String,
    val records: List<String>
)

private val modules = listOf(
    CrmModule("Farm Operations", "Farms, sheds, flocks and daily operations", "farms",
        listOf("Green Valley Farm · 4 sheds", "Sunrise Poultry · 3 sheds", "Batch BR-2026-014 · 18,400 birds", "Feed and health register", "Medication and vaccination")),
    CrmModule("Inventory", "Products, warehouses and stock movement", "inventory",
        listOf("Broiler Starter Feed · 1,250 kg", "Layer Feed · 860 kg", "Vitamin Supplement · 120 units", "Warehouse A · Main store", "Stock transfers and ledger")),
    CrmModule("Trading", "Customers, suppliers, sales and procurement", "trading",
        listOf("Aarav Foods · Customer", "FreshFarm Supplies · Supplier", "Sales Order SO-1042 · Open", "Quotation QT-208 · Draft", "Procurement PO-512 · Pending")),
    CrmModule("CRM", "Leads, opportunities, follow-ups and complaints", "crm",
        listOf("New distributor lead · Open", "Retail chain opportunity · Qualified", "Follow-up · Today", "Delivery complaint · In review")),
    CrmModule("Logistics", "Employees, vehicles, trips and deliveries", "logistics",
        listOf("Vehicle MH12AB1234 · Available", "Trip TR-2026-041 · In transit", "Driver assignment · Pending", "Delivery DN-802 · Scheduled")),
    CrmModule("Finance", "Receivables, payables, expenses and profitability", "finance",
        listOf("Receivable · ₹82,500", "Payable · ₹41,200", "Expense claim · Pending", "Farm profitability · Current month")),
    CrmModule("Reports & Analytics", "Operational and financial reporting", "reports",
        listOf("Flock performance report", "Feed consumption report", "Sales summary", "Profitability by farm")),
    CrmModule("Administration", "Users, roles, organization and notifications", "admin",
        listOf("User management", "Role permissions", "Organization settings", "Notifications"))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                PoultryCrmApp()
            }
        }
    }
}

@Composable
private fun PoultryCrmApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "dashboard"
    val selectedModule = modules.firstOrNull { it.route == currentRoute }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedModule?.name ?: "Poultry CRM", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (selectedModule != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "dashboard",
                    onClick = { navController.navigate("dashboard") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentRoute == "modules" || selectedModule != null,
                    onClick = { navController.navigate("modules") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Modules") },
                    label = { Text("Modules") }
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") {
                DashboardScreen(onOpenModule = { navController.navigate(it) })
            }
            composable("modules") {
                ModuleDirectory(onOpenModule = { navController.navigate(it) })
            }
            modules.forEach { module ->
                composable(module.route) { ModuleScreen(module) }
            }
        }
    }
}

@Composable
private fun DashboardScreen(onOpenModule: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Farm business overview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("A unified view of poultry operations", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Active flocks", "12", Modifier.weight(1f))
                MetricCard("Live birds", "48,250", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Open orders", "18", Modifier.weight(1f))
                MetricCard("Low stock items", "4", Modifier.weight(1f))
            }
        }
        item { Text("Business modules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        items(modules) { module ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpenModule(module.route) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(module.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(module.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModuleDirectory(onOpenModule: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = modules.filter {
        it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            label = { Text("Search modules") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered) { module ->
                Card(
                    modifier = Modifier.fillMaxWidth().height(150.dp).clickable { onOpenModule(module.route) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(module.name, fontWeight = FontWeight.SemiBold)
                        Text(module.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleScreen(module: CrmModule) {
    var query by remember { mutableStateOf("") }
    val records = module.records.filter { it.contains(query, ignoreCase = true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(module.description, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            label = { Text("Search " + module.name.lowercase()) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(records) { record ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(record, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (records.isEmpty()) {
                item { Text("No matching records", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
