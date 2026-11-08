package com.poultrycrm.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poultrycrm.android.data.remote.TripsApi
import com.poultrycrm.android.data.remote.requireBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun plRows(value: Any?): List<Map<String, Any?>> {
    val source = when (value) {
        is List<*> -> value
        is Map<*, *> -> value["data"] as? List<*> ?: value["records"] as? List<*> ?: emptyList<Any?>()
        else -> emptyList<Any?>()
    }
    return source.mapNotNull { item -> (item as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value } }
}
private fun plText(value: Any?): String = value?.toString().orEmpty()
private fun Map<String, Any?>.plId(): String = plText(this["id"])
private fun Map<String, Any?>.plName(): String = plText(this["name"] ?: this["vehNumber"] ?: this["vehicleNumber"] ?: this["id"])

@Composable
fun PeopleLogisticsScreen(api: TripsApi) {
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf("Trips") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var vehicles by remember { mutableStateOf(emptyList<Map<String, Any?>>()) }
    var drivers by remember { mutableStateOf(emptyList<Map<String, Any?>>()) }
    var supervisors by remember { mutableStateOf(emptyList<Map<String, Any?>>()) }
    var suppliers by remember { mutableStateOf(emptyList<Map<String, Any?>>()) }
    var destinations by remember { mutableStateOf(emptyList<Map<String, Any?>>()) }
    var trips by remember { mutableStateOf(emptyList<Map<String, Any?>>()) }
    var orderTypes by remember { mutableStateOf(emptyMap<String, Int>()) }
    var vehicle by remember { mutableStateOf("") }
    var driver by remember { mutableStateOf("") }
    var supervisor by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var quantity by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }

    suspend fun refresh() {
        loading = true
        error = ""
        try {
            val data = withContext(Dispatchers.IO) {
                listOf(api.vehicles().requireBody(), api.drivers().requireBody(),
                    api.supervisors().requireBody(), api.buyingParties().requireBody(),
                    api.sellingParties().requireBody(), api.trips().requireBody(),
                    api.orderTypes().requireBody())
            }
            vehicles = plRows(data[0]).filter { it["isDeleted"] != true && it["isActive"] != false }
            drivers = plRows(data[1]).filter { it["isDeleted"] != true && it["isActive"] != false }
            supervisors = plRows(data[2]).filter { it["isDeleted"] != true && it["isActive"] != false }
            suppliers = plRows(data[3]).filter { it["isDeleted"] != true && it["isActive"] != false }
            destinations = plRows(data[4]).filter { it["isDeleted"] != true && it["isActive"] != false }
            trips = plRows(data[5]).filter { it["isDeleted"] != true }
            orderTypes = plRows(data[6]).mapNotNull { row ->
                val name = plText(row["name"]).lowercase()
                val key = when { name.contains("broiler") -> "broiler"; name.contains("cull") -> "culls"; name.contains("feed") -> "feed"; else -> "" }
                val id = (row["id"] as? Number)?.toInt() ?: plText(row["id"]).toIntOrNull() ?: 0
                if (key.isNotEmpty() && id > 0) key to id else null
            }.toMap()
        } catch (e: Exception) {
            error = e.message ?: "Could not load People & Logistics data."
        } finally { loading = false }
    }
    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("People & Logistics", style = MaterialTheme.typography.headlineSmall)
        Text("Employees, vehicles and trip management")
        Spacer(Modifier.height(8.dp))
        ScrollableTabRow(selectedTabIndex = listOf("Trips", "Employees", "Vehicles", "Create trip").indexOf(section)) {
            listOf("Trips", "Employees", "Vehicles", "Create trip").forEach { label ->
                Tab(selected = section == label, onClick = { section = label; error = ""; notice = "" }, text = { Text(label) })
            }
        }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        } else {
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
            when (section) {
                "Employees" -> PlDirectory("Employees / Drivers", drivers, listOf("address", "number", "status"))
                "Vehicles" -> PlDirectory("Company vehicles", vehicles, listOf("registrationNo", "regValidity", "insuranceNo", "insuranceValidity"))
                "Trips" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { scope.launch { refresh() } }) { Text("Refresh") }
                        Button(onClick = { section = "Create trip" }) { Text("+ Create trip") }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(trips) { item ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(plText(item["tripNumber"] ?: item["tripId"] ?: item["id"]), style = MaterialTheme.typography.titleMedium)
                                    Text("Date: " + plText(item["date"] ?: item["tripDate"]))
                                    Text("Route: " + plText(item["route"]))
                                    Text("Status: " + plText(item["status"]))
                                }
                            }
                        }
                        if (trips.isEmpty()) item { Text("No trips found. Select Create trip to add one.") }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        item { Text("Trip assignment", style = MaterialTheme.typography.titleLarge) }
                        item { PlPicker("Vehicle", vehicles, vehicle, { vehicle = it }) }
                        item { PlPicker("Driver", drivers, driver, { driver = it }) }
                        item { PlPicker("Supervisor", supervisors, supervisor, { supervisor = it }) }
                        item { OutlinedTextField(route, { route = it }, Modifier.fillMaxWidth(), label = { Text("Route") }) }
                        item { OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Trip date (YYYY-MM-DD)") }) }
                        item { Divider(); Text("Supplier pickup", style = MaterialTheme.typography.titleMedium) }
                        item { PlPicker("Buying party", suppliers, supplier, { supplier = it }) }
                        item { OutlinedTextField(quantity, { quantity = it }, Modifier.fillMaxWidth(), label = { Text("Broiler quantity (birds)") }) }
                        item { OutlinedTextField(weight, { weight = it }, Modifier.fillMaxWidth(), label = { Text("Broiler weight (kg)") }) }
                        item { OutlinedTextField(rate, { rate = it }, Modifier.fillMaxWidth(), label = { Text("Broiler rate (₹/kg)") }) }
                        item { Divider(); Text("Delivery destination", style = MaterialTheme.typography.titleMedium) }
                        item { PlPicker("Selling party", destinations, destination, { destination = it }) }
                        item {
                            Button(onClick = {
                                val qty = quantity.toIntOrNull() ?: 0
                                val kg = weight.toDoubleOrNull() ?: 0.0
                                val price = rate.toDoubleOrNull() ?: 0.0
                                if (vehicle.isBlank() || driver.isBlank() || supervisor.isBlank() || supplier.isBlank() || destination.isBlank() || route.isBlank() || qty <= 0 || kg <= 0 || orderTypes["broiler"] == null) {
                                    error = "Select all assignment and party fields, enter a route, and provide a positive broiler quantity and weight."
                                } else {
                                    saving = true; error = ""; notice = ""
                                    scope.launch {
                                        try {
                                            val payload = withContext(Dispatchers.IO) {
                                                fun find(list: List<Map<String, Any?>>, id: String) = list.first { it.plId() == id }
                                                fun party(list: List<Map<String, Any?>>, id: String, includeOrder: Boolean): Map<String, Any?> {
                                                    val p = find(list, id)
                                                    val order = if (includeOrder) listOf(mapOf(
                                                        "orderTypeId" to orderTypes.getValue("broiler"), "qty" to qty, "weight" to kg,
                                                        "rate" to price, "total" to (kotlin.math.round(kg * price * 100) / 100),
                                                        "isActive" to true, "isDeleted" to false
                                                    )) else emptyList()
                                                    return mapOf("id" to id.toInt(), "name" to plText(p["name"]), "contact" to plText(p["contact"]),
                                                        "location" to plText(p["location"] ?: p["address"]), "order" to order, "isActive" to true, "isDeleted" to false)
                                                }
                                                val v = find(vehicles, vehicle); val d = find(drivers, driver); val s = find(supervisors, supervisor)
                                                mapOf(
                                                    "vehicle" to mapOf("id" to vehicle.toInt(), "name" to plText(v["name"]), "vehNumber" to plText(v["vehNumber"] ?: v["vehicleNumber"]), "registrationNo" to plText(v["registrationNo"]), "regValidity" to v["regValidity"], "insuranceNo" to plText(v["insuranceNo"]), "insuranceValidity" to v["insuranceValidity"], "isActive" to true, "isDeleted" to false),
                                                    "driver" to mapOf("id" to driver.toInt(), "name" to plText(d["name"]), "address" to plText(d["address"]), "number" to plText(d["number"]), "status" to plText(d["status"]), "isActive" to true, "isDeleted" to false),
                                                    "supervisor" to mapOf("id" to supervisor.toInt(), "name" to plText(s["name"]), "address" to plText(s["address"]), "number" to plText(s["number"]), "status" to plText(s["status"]), "isActive" to true, "isDeleted" to false),
                                                    "buyingParties" to listOf(party(suppliers, supplier, true)),
                                                    "sellingParties" to listOf(party(destinations, destination, false)),
                                                    "route" to route.trim(), "date" to date, "status" to "Assigned"
                                                )
                                            }
                                            withContext(Dispatchers.IO) { api.createTrip(payload).requireBody() }
                                            notice = "Trip created successfully."
                                            vehicle = ""; driver = ""; supervisor = ""; supplier = ""; destination = ""; route = ""; quantity = ""; weight = ""; rate = ""
                                            refresh(); section = "Trips"
                                        } catch (e: Exception) {
                                            error = e.message ?: "Trip could not be created."
                                        } finally { saving = false }
                                    }
                                }
                            }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                                Text(if (saving) "Saving…" else "Create trip")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlDirectory(title: String, rows: List<Map<String, Any?>>, fields: List<String>) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows) { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(row.plName(), style = MaterialTheme.typography.titleMedium)
                        fields.forEach { field -> row[field]?.let { Text(field + ": " + plText(it)) } }
                    }
                }
            }
            if (rows.isEmpty()) item { Text("No records returned by the backend.") }
        }
    }
}

@Composable
private fun PlPicker(label: String, rows: List<Map<String, Any?>>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = rows.firstOrNull { it.plId() == selectedId }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.plName() ?: "Select $label")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                rows.forEach { row ->
                    DropdownMenuItem(text = { Text(row.plName()) }, onClick = { onSelect(row.plId()); expanded = false })
                }
            }
        }
    }
}
