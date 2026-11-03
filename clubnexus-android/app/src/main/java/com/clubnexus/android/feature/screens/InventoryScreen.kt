package com.clubnexus.android.feature.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clubnexus.android.data.model.ApproveBorrowRequest
import com.clubnexus.android.data.model.BorrowItemRequest
import com.clubnexus.android.data.model.BorrowRequest
import com.clubnexus.android.data.model.InventoryItem
import com.clubnexus.android.data.model.InventoryItemRequest
import com.clubnexus.android.data.model.User
import com.clubnexus.android.data.network.ClubNexusApi
import kotlinx.coroutines.launch

@Composable
fun InventoryScreen(api: ClubNexusApi, user: User) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var requests by remember { mutableStateOf<List<BorrowRequest>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var addItem by remember { mutableStateOf(false) }
    var borrowItem by remember { mutableStateOf<InventoryItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    suspend fun refresh() { runCatching { api.inventory(search.ifBlank { null }) }.onSuccess { items = it }.onFailure { error = it.apiMessage() }; runCatching { requests = api.borrowRequests() }.onFailure { error = it.apiMessage() }; loading = false }
    LaunchedEffect(search) { kotlinx.coroutines.delay(250); loading = true; refresh() }
    ScreenFrame("Inventory", "Manage assets, borrow requests, approvals, returns, and due dates.", user) {
        ErrorMessage(error)
        if (user.role != "VIEWER") Button(onClick = { addItem = true }, modifier = Modifier.fillMaxWidth()) { Text("+  Add item") }
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search inventory") }, singleLine = true)
        if (loading && items.isEmpty()) LoadingState()
        if (!loading && items.isEmpty()) EmptyMessage("No inventory items found.")
        items.forEach { item ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    item.imageUrl?.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let { url ->
                        AsyncImage(model = url, contentDescription = item.name, modifier = Modifier.fillMaxWidth().height(170.dp), contentScale = ContentScale.Crop)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${item.availableQuantity}/${item.totalQuantity}", color = if (item.availableQuantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                    Text(item.description)
                    Text(item.category, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { borrowItem = item }, enabled = item.availableQuantity > 0 && user.role != "VIEWER") { Text("Borrow") }
                }
            }
        }
        InfoCard("Borrow requests") {
            if (requests.isEmpty()) EmptyMessage("No borrow requests yet.")
            requests.forEach { request ->
                Text("${request.item.name} × ${request.quantity}", fontWeight = FontWeight.SemiBold)
                Text("${request.borrower.user.name} • Due ${request.dueDate.toDisplayDate()} • ${request.purpose}", style = MaterialTheme.typography.bodySmall)
                Text("${request.status}${request.notes?.let { " • $it" }.orEmpty()}", color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (request.status == "PENDING" && user.role == "ADMIN") {
                        TextButton(onClick = { scope.launch { runCatching { api.approveBorrow(request.id, ApproveBorrowRequest(true)) }.onSuccess { refresh() }.onFailure { error = it.apiMessage() } } }) { Text("Approve") }
                        TextButton(onClick = { scope.launch { runCatching { api.approveBorrow(request.id, ApproveBorrowRequest(false)) }.onSuccess { refresh() }.onFailure { error = it.apiMessage() } } }) { Text("Reject", color = MaterialTheme.colorScheme.error) }
                    }
                    if (request.status == "APPROVED" && user.role != "VIEWER") TextButton(onClick = { scope.launch { runCatching { api.returnBorrow(request.id) }.onSuccess { refresh() }.onFailure { error = it.apiMessage() } } }) { Text("Mark returned") }
                }
                androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 6.dp))
            }
        }
    }
    if (addItem) ItemDialog(onClose = { addItem = false }) { request -> scope.launch { runCatching { api.addInventoryItem(request) }.onSuccess { addItem = false; refresh() }.onFailure { error = it.apiMessage() } } }
    borrowItem?.let { item -> BorrowDialog(item, onClose = { borrowItem = null }) { request -> scope.launch { runCatching { api.borrow(request) }.onSuccess { borrowItem = null; refresh() }.onFailure { error = it.apiMessage() } } } }
}

@Composable
private fun ItemDialog(onClose: () -> Unit, onSubmit: (InventoryItemRequest) -> Unit) {
    var name by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var category by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("1") }; var available by remember { mutableStateOf("1") }; var image by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, title = { Text("Add inventory item") }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") })
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2)
            OutlinedTextField(category, { category = it }, label = { Text("Category") })
            OutlinedTextField(total, { total = it }, label = { Text("Total quantity") })
            OutlinedTextField(available, { available = it }, label = { Text("Available quantity") })
            OutlinedTextField(image, { image = it }, label = { Text("Image URL") })
            TextButton(onClick = { val t = total.toIntOrNull() ?: 0; val a = available.toIntOrNull() ?: 0; onSubmit(InventoryItemRequest(name.trim(), description.trim(), category.trim(), t, a, image.trim())) }, enabled = name.length >= 2 && description.length >= 2 && category.isNotBlank() && (total.toIntOrNull() ?: 0) > 0 && (available.toIntOrNull() ?: -1) >= 0 && (available.toIntOrNull() ?: 0) <= (total.toIntOrNull() ?: 0)) { Text("Save item") }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Close") } })
}

@Composable
private fun BorrowDialog(item: InventoryItem, onClose: () -> Unit, onSubmit: (BorrowItemRequest) -> Unit) {
    var quantity by remember { mutableStateOf("1") }; var days by remember { mutableStateOf("7") }; var purpose by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, title = { Text("Borrow ${item.name}") }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity (available ${item.availableQuantity})") })
            OutlinedTextField(days, { days = it }, label = { Text("Days") })
            OutlinedTextField(purpose, { purpose = it }, label = { Text("Purpose") }, minLines = 2)
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") })
            TextButton(onClick = { onSubmit(BorrowItemRequest(item.id, quantity.toIntOrNull() ?: 1, days.toIntOrNull() ?: 7, purpose.trim(), notes.trim())) }, enabled = (quantity.toIntOrNull() ?: 0) in 1..item.availableQuantity && (days.toIntOrNull() ?: 0) > 0 && purpose.length >= 3) { Text("Submit request") }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Close") } })
}
