package com.clubnexus.android.feature.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.clubnexus.android.data.model.Member
import com.clubnexus.android.data.model.MemberRequest
import com.clubnexus.android.data.model.User
import com.clubnexus.android.data.network.ClubNexusApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MembersScreen(api: ClubNexusApi, user: User) {
    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Member?>(null) }
    var detail by remember { mutableStateOf<Member?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDeactivate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(query) {
        delay(250)
        loading = true
        runCatching { api.members() }
            .onSuccess { all -> members = all.filter { "${it.user.name} ${it.user.email} ${it.department}".contains(query.trim(), ignoreCase = true) }; error = null }
            .onFailure { error = it.apiMessage() }
        loading = false
    }
    ScreenFrame("Members", "Search member profiles, projects, borrowed assets, and attendance rates.", user, {}) {
        if (user.role == "ADMIN") Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("+  Add Member") }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search members") }, singleLine = true)
        ErrorMessage(error)
        if (loading && members.isEmpty()) LoadingState()
        if (!loading && members.isEmpty()) EmptyMessage("No members found. Adjust the search query.")
        members.forEach { member ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                selected = member
                detail = null
                scope.launch { runCatching { api.member(member.id) }.onSuccess { detail = it }.onFailure { error = it.apiMessage() } }
            }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(member.user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(member.user.role, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                    Text(member.user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${member.department} • ${member.position} • Attendance ${member.attendanceRate}%", style = MaterialTheme.typography.bodySmall)
                    Text(if (member.isActive) "Active" else "Inactive", style = MaterialTheme.typography.labelSmall, color = if (member.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (showAdd) MemberEditorDialog("Add member", null, onClose = { showAdd = false }) { request ->
        scope.launch {
            runCatching { api.addMember(request) }.onSuccess { showAdd = false; error = null; members = api.members().filter { "${it.user.name} ${it.user.email} ${it.department}".contains(query.trim(), ignoreCase = true) } }.onFailure { error = it.apiMessage() }
        }
    }
    if (selected != null) {
        val current = detail ?: selected!!
        AlertDialog(
            onDismissRequest = { selected = null; editing = false },
            title = { Text(if (editing) "Edit member" else current.user.name) },
            text = {
                if (editing) MemberEditorDialogContent(current) { fields ->
                    scope.launch { runCatching { api.updateMember(current.id, fields) }.onSuccess {
                        editing = false; selected = null; detail = null; members = api.members().filter { "${it.user.name} ${it.user.email} ${it.department}".contains(query.trim(), ignoreCase = true) }
                    }.onFailure { error = it.apiMessage() } }
                } else if (detail == null) LoadingState() else Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(current.user.email)
                    Text("${current.department} • ${current.position}")
                    Text("Role: ${current.user.role} • ${if (current.isActive) "Active" else "Inactive"}")
                    Text("Attendance rate: ${current.attendanceRate}%")
                    Text("Projects: ${current.projectMemberships.size} • Borrowed requests: ${current.borrowedRequests.size}")
                    current.projectMemberships.forEach { Text("Project: ${it.project.name} (${it.role})", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    current.attendanceRecords.take(5).forEach { Text("${it.event?.name ?: "General"} — ${it.status} — ${it.date.toDisplayDate()}", style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = { if (!editing && user.role != "VIEWER") TextButton(onClick = { editing = true }) { Text("Edit") } },
            dismissButton = {
                Row {
                    if (!editing && user.role == "ADMIN" && current.isActive) TextButton(onClick = { confirmDeactivate = true }) { Text("Deactivate", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { selected = null; editing = false }) { Text(if (editing) "Close" else "Done") }
                }
            }
        )
    }
    if (confirmDeactivate && selected != null) AlertDialog(
        onDismissRequest = { confirmDeactivate = false }, title = { Text("Deactivate member?") },
        text = { Text("${selected!!.user.name} will be marked inactive.") },
        confirmButton = { TextButton(onClick = { val id = selected!!.id; scope.launch { runCatching { api.deactivateMember(id) }.onSuccess { confirmDeactivate = false; selected = null; members = api.members().filter { "${it.user.name} ${it.user.email} ${it.department}".contains(query.trim(), ignoreCase = true) } }.onFailure { error = it.apiMessage(); confirmDeactivate = false } } }) { Text("Deactivate") } },
        dismissButton = { TextButton(onClick = { confirmDeactivate = false }) { Text("Cancel") } }
    )
}

@Composable
private fun MemberEditorDialog(title: String, existing: Member?, onClose: () -> Unit, onSubmit: (MemberRequest) -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text(title) }, text = {
        MemberEditorContent(existing, onSubmit)
    }, confirmButton = { TextButton(onClick = onClose) { Text("Close") } })
}

@Composable
private fun MemberEditorContent(existing: Member?, onSubmit: (MemberRequest) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.user?.name.orEmpty()) }
    var email by remember(existing) { mutableStateOf(existing?.user?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var department by remember(existing) { mutableStateOf(existing?.department.orEmpty()) }
    var position by remember(existing) { mutableStateOf(existing?.position.orEmpty()) }
    var role by remember(existing) { mutableStateOf(existing?.user?.role ?: "MEMBER") }
    MemberFields(name, { name = it }, email, { email = it }, password, { password = it }, department, { department = it }, position, { position = it }, role, { role = it }, existing == null) {
        onSubmit(MemberRequest(name.trim(), email.trim().lowercase(), password, role, position.trim(), department.trim()))
    }
}

@Composable
private fun MemberEditorDialogContent(existing: Member, onSubmit: (Map<String, Any?>) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing.user.name) }
    var email by remember(existing) { mutableStateOf(existing.user.email) }
    var department by remember(existing) { mutableStateOf(existing.department) }
    var position by remember(existing) { mutableStateOf(existing.position) }
    var role by remember(existing) { mutableStateOf(existing.user.role) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true)
        OutlinedTextField(department, { department = it }, label = { Text("Department") }, singleLine = true)
        OutlinedTextField(position, { position = it }, label = { Text("Position") }, singleLine = true)
        RolePicker(role, { role = it })
        TextButton(onClick = { onSubmit(mapOf("name" to name.trim(), "email" to email.trim(), "department" to department.trim(), "position" to position.trim(), "role" to role)) }) { Text("Save changes") }
    }
}

@Composable
private fun MemberFields(name: String, onName: (String) -> Unit, email: String, onEmail: (String) -> Unit,
    password: String, onPassword: (String) -> Unit, department: String, onDepartment: (String) -> Unit,
    position: String, onPosition: (String) -> Unit, role: String, onRole: (String) -> Unit, newMember: Boolean, onSave: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(name, onName, label = { Text("Name") }, singleLine = true)
        OutlinedTextField(email, onEmail, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        OutlinedTextField(department, onDepartment, label = { Text("Department") }, singleLine = true)
        OutlinedTextField(position, onPosition, label = { Text("Position") }, singleLine = true)
        if (newMember) OutlinedTextField(password, onPassword, label = { Text("Initial password") }, singleLine = true)
        RolePicker(role, onRole)
        TextButton(onClick = onSave, enabled = name.length >= 2 && email.contains('@') && department.isNotBlank() && position.isNotBlank() && (!newMember || password.length >= 6)) { Text(if (newMember) "Save member" else "Save changes") }
    }
}

@Composable
private fun RolePicker(role: String, onRole: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }) { Text("Role: $role") }
        DropdownMenu(expanded, { expanded = false }) { listOf("ADMIN", "MEMBER", "VIEWER").forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { onRole(item); expanded = false }) } }
    }
}
