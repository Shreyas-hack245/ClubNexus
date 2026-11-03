package com.clubnexus.android.feature.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clubnexus.android.data.model.AttendanceRecord
import com.clubnexus.android.data.model.ClubEvent
import com.clubnexus.android.data.model.Member
import com.clubnexus.android.data.model.MemberAttendance
import com.clubnexus.android.data.model.MarkAttendanceRequest
import com.clubnexus.android.data.model.User
import com.clubnexus.android.data.network.ClubNexusApi
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun AttendanceScreen(api: ClubNexusApi, user: User) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    var events by remember { mutableStateOf<List<ClubEvent>>(emptyList()) }
    var records by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var member by remember { mutableStateOf<Member?>(null) }
    var event by remember { mutableStateOf<ClubEvent?>(null) }
    var status by remember { mutableStateOf("PRESENT") }
    var memberHistory by remember { mutableStateOf<MemberAttendance?>(null) }
    var csv by remember { mutableStateOf<ByteArray?>(null) }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) csv?.let { data -> runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(data) } }.onFailure { error = it.apiMessage() } }
    }
    fun refresh() { scope.launch {
        loading = true
        runCatching { Triple(api.attendance(), api.members(), api.events()) }.onSuccess { (a, m, e) -> records = a; members = m; events = e; error = null }.onFailure { error = it.apiMessage() }
        loading = false
    } }
    LaunchedEffect(Unit) { refresh() }
    ScreenFrame("Attendance", "Mark event attendance and review member history, rates, and streaks.", user, {}) {
        ErrorMessage(error)
        InfoCard("Mark attendance") {
            ChoiceField("Member", member?.user?.name ?: "Select member", members.map { it.user.name to it }) { member = it; memberHistory = null }
            ChoiceField("Event", event?.name ?: "General", listOf("General" to null) + events.map { it.name to it }) { event = it }
            ChoiceField("Status", status, listOf("PRESENT", "LATE", "ABSENT").map { it to it }) { status = it }
            Button(enabled = member != null && user.role != "VIEWER", onClick = { scope.launch {
                val target = member ?: return@launch
                runCatching { api.markAttendance(MarkAttendanceRequest(target.id, event?.id, Instant.now().toString(), status)) }
                    .onSuccess { error = null; records = api.attendance() }
                    .onFailure { error = it.apiMessage() }
            } }, modifier = Modifier.fillMaxWidth()) { Text("Mark attendance") }
        }
        ActionRow {
            Button(onClick = { scope.launch { runCatching { csv = api.exportAttendance().bytes(); saveCsv.launch("attendance.csv") }.onFailure { error = it.apiMessage() } } }) { Text("Export CSV") }
            member?.let { TextButton(onClick = { scope.launch { runCatching { memberHistory = api.memberAttendance(it.id) }.onFailure { error = it.apiMessage() } } }) { Text("Member history") } }
        }
        memberHistory?.let { history -> InfoCard("${member?.user?.name} attendance") {
            Text("${history.stats.attended}/${history.stats.total} attended • ${history.stats.percentage}% • ${history.stats.streak} streak")
            history.records.forEach { Text("${it.event?.name ?: "General"} • ${it.status} • ${it.date.toDisplayDate()}") }
        } }
        InfoCard("Attendance history") {
            if (loading) LoadingState()
            if (!loading && records.isEmpty()) EmptyMessage("No attendance records yet.")
            records.forEach { record ->
                Text(record.member.user.name, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Text("${record.event?.name ?: "General"} • ${record.status} • ${record.date.toDisplayDate()}")
                Text("Marked by ${record.markedBy?.user?.name ?: "ClubNexus"}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 7.dp))
            }
        }
    }
}

@Composable
fun <T> ChoiceField(label: String, value: String, choices: List<Pair<String, T>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(value) }
        DropdownMenu(expanded, { expanded = false }) {
            choices.forEach { (title, item) -> DropdownMenuItem(text = { Text(title) }, onClick = { onSelect(item); expanded = false }) }
        }
    }
}
