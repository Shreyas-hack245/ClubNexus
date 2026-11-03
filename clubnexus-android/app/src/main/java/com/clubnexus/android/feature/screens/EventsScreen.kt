package com.clubnexus.android.feature.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
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
import com.clubnexus.android.data.model.ClubEvent
import com.clubnexus.android.data.model.EventAttendanceRequest
import com.clubnexus.android.data.model.EventRequest
import com.clubnexus.android.data.model.User
import com.clubnexus.android.data.network.ClubNexusApi
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

private val eventTypes = listOf("HACKATHON", "WORKSHOP", "MEETING", "COMPETITION", "SOCIAL", "OTHER")
private val eventStatuses = listOf("UPCOMING", "ONGOING", "COMPLETED", "CANCELLED")

@Composable
fun EventsScreen(api: ClubNexusApi, user: User) {
    val scope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<ClubEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var calendar by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<ClubEvent?>(null) }
    var creating by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ClubEvent?>(null) }
    var deleting by remember { mutableStateOf<ClubEvent?>(null) }
    suspend fun refresh() { runCatching { api.events() }.onSuccess { events = it; error = null }.onFailure { error = it.apiMessage() }; loading = false }
    LaunchedEffect(Unit) { refresh() }
    ScreenFrame("Events", "Manage the calendar, registrations, event attendance, and hackathons.", user) {
        ErrorMessage(error)
        if (user.role == "ADMIN") Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) { Text("+  Create event") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (calendar) OutlinedButton(onClick = { calendar = false }) { Text("List") } else Button(onClick = { calendar = false }) { Text("List") }
            if (calendar) Button(onClick = { calendar = true }) { Text("Calendar") } else OutlinedButton(onClick = { calendar = true }) { Text("Calendar") }
        }
        if (loading) LoadingState()
        if (!loading && events.isEmpty()) EmptyMessage("No events yet.")
        val displayEvents = if (calendar) events.sortedBy { it.startDate } else events
        displayEvents.forEach { event ->
            Card(onClick = { selected = event }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(event.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(event.status, color = MaterialTheme.colorScheme.primary)
                    }
                    if (calendar) Text(event.startDate.toDisplayDate().substringBefore('•'), color = MaterialTheme.colorScheme.primary)
                    Text("${event.type} • ${event.venue}", style = MaterialTheme.typography.bodySmall)
                    Text(event.description)
                    Text("${event.startDate.toDisplayDate()} – ${event.endDate.toDisplayDate()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${event.participants.size}/${event.maxParticipants ?: "∞"} registered", style = MaterialTheme.typography.labelMedium)
                    if (event.type == "HACKATHON") {
                        event.teamFormation?.let { Text("Team formation: $it", style = MaterialTheme.typography.bodySmall) }
                        event.prizeDetails?.let { Text("Prizes: $it", style = MaterialTheme.typography.bodySmall) }
                        event.projectSubmission?.let { url ->
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            Text("Submission link", Modifier.clickable { runCatching { uriHandler.openUri(url) } }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Row {
                        if (user.role != "VIEWER" && event.participants.none { it.member.id == user.member?.id }) TextButton(onClick = { scope.launch { runCatching { api.registerForEvent(event.id) }.onSuccess { refresh() }.onFailure { error = it.apiMessage() } } }) { Text("Register") }
                        TextButton(onClick = { selected = event }) { Text("Details & attendance") }
                        if (user.role == "ADMIN") TextButton(onClick = { editor = event }) { Text("Edit") }
                        if (user.role == "ADMIN") TextButton(onClick = { deleting = event }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    if (creating) EventEditor(null, { creating = false }) { request -> scope.launch { runCatching { api.createEvent(request) }.onSuccess { creating = false; refresh() }.onFailure { error = it.apiMessage() } } }
    editor?.let { event -> EventEditor(event, { editor = null }) { request -> scope.launch { runCatching { api.updateEvent(event.id, request) }.onSuccess { editor = null; refresh() }.onFailure { error = it.apiMessage() } } } }
    selected?.let { event -> EventDetails(event, user, onClose = { selected = null }, onRegister = { scope.launch { runCatching { api.registerForEvent(event.id) }.onSuccess { refresh(); selected = events.firstOrNull { it.id == event.id } ?: event }.onFailure { error = it.apiMessage() } } }, onAttendance = { memberId, attended -> scope.launch { runCatching { api.markEventParticipant(event.id, EventAttendanceRequest(memberId, attended)) }.onSuccess { refresh(); selected = events.firstOrNull { it.id == event.id } ?: event }.onFailure { error = it.apiMessage() } } }) }
    deleting?.let { event -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete event?") }, text = { Text("${event.name} will be removed.") }, confirmButton = { TextButton(onClick = { scope.launch { runCatching { api.deleteEvent(event.id) }.onSuccess { deleting = null; refresh() }.onFailure { error = it.apiMessage(); deleting = null } } }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
}

@Composable
private fun EventDetails(event: ClubEvent, user: User, onClose: () -> Unit, onRegister: () -> Unit, onAttendance: (String, Boolean) -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text(event.name) }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${event.type} • ${event.status}")
            Text(event.description)
            Text("${event.venue}\n${event.startDate.toDisplayDate()} – ${event.endDate.toDisplayDate()}")
            Text("Participants: ${event.participants.size}/${event.maxParticipants ?: "∞"}")
            if (event.type == "HACKATHON") {
                event.teamFormation?.let { Text("Team formation: $it") }
                event.projectSubmission?.let { url ->
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Text("Submission link: $url", Modifier.clickable { runCatching { uriHandler.openUri(url) } }, color = MaterialTheme.colorScheme.primary)
                }
                event.prizeDetails?.let { Text("Prizes: $it") }
            }
            event.registrationLink?.takeIf { it.isNotBlank() }?.let { url ->
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                TextButton(onClick = { runCatching { uriHandler.openUri(url) } }) { Text("Open registration page") }
            }
            if (user.role != "VIEWER" && event.participants.none { it.member.id == user.member?.id }) TextButton(onClick = onRegister) { Text("Register for event") }
            Text("Attendees", fontWeight = FontWeight.SemiBold)
            if (event.participants.isEmpty()) Text("No participants registered yet.")
            event.participants.forEach { participant ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${participant.member.user.name}${if (participant.attended) " • Present" else " • Not marked"}", style = MaterialTheme.typography.bodySmall)
                    if (user.role != "VIEWER") TextButton(onClick = { onAttendance(participant.member.id, !participant.attended) }) { Text(if (participant.attended) "Absent" else "Present") }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Done") } })
}

@Composable
private fun EventEditor(event: ClubEvent?, onClose: () -> Unit, onSubmit: (EventRequest) -> Unit) {
    var name by remember(event) { mutableStateOf(event?.name.orEmpty()) }
    var description by remember(event) { mutableStateOf(event?.description.orEmpty()) }
    var type by remember(event) { mutableStateOf(event?.type ?: "MEETING") }
    var venue by remember(event) { mutableStateOf(event?.venue.orEmpty()) }
    var start by remember(event) { mutableStateOf(event?.startDate?.toLocalDateTimeInput().orEmpty()) }
    var end by remember(event) { mutableStateOf(event?.endDate?.toLocalDateTimeInput().orEmpty()) }
    var status by remember(event) { mutableStateOf(event?.status ?: "UPCOMING") }
    var capacity by remember(event) { mutableStateOf(event?.maxParticipants?.toString() ?: "50") }
    var registrationLink by remember(event) { mutableStateOf(event?.registrationLink.orEmpty()) }
    var bannerUrl by remember(event) { mutableStateOf(event?.bannerUrl.orEmpty()) }
    var teamFormation by remember(event) { mutableStateOf(event?.teamFormation.orEmpty()) }
    var submission by remember(event) { mutableStateOf(event?.projectSubmission.orEmpty()) }
    var prizes by remember(event) { mutableStateOf(event?.prizeDetails.orEmpty()) }
    AlertDialog(onDismissRequest = onClose, title = { Text(if (event == null) "Create event" else "Edit event") }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") })
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2)
            ChoiceField("Type", type, eventTypes.map { it to it }) { type = it }
            OutlinedTextField(venue, { venue = it }, label = { Text("Venue") })
            ChoiceField("Status", status, eventStatuses.map { it to it }) { status = it }
            OutlinedTextField(start, { start = it }, label = { Text("Start (YYYY-MM-DDTHH:MM)") })
            OutlinedTextField(end, { end = it }, label = { Text("End (YYYY-MM-DDTHH:MM)") })
            OutlinedTextField(capacity, { capacity = it }, label = { Text("Capacity") })
            OutlinedTextField(registrationLink, { registrationLink = it }, label = { Text("Registration URL") })
            OutlinedTextField(bannerUrl, { bannerUrl = it }, label = { Text("Banner URL") })
            if (type == "HACKATHON") {
                OutlinedTextField(teamFormation, { teamFormation = it }, label = { Text("Team formation") })
                OutlinedTextField(submission, { submission = it }, label = { Text("Submission link") })
                OutlinedTextField(prizes, { prizes = it }, label = { Text("Prize details") })
            }
            TextButton(onClick = {
                val startIso = start.toIsoDateTime(); val endIso = end.toIsoDateTime()
                if (startIso != null && endIso != null) onSubmit(EventRequest(name.trim(), description.trim(), type, venue.trim(), startIso, endIso, status, registrationLink.trim(), bannerUrl.trim(), capacity.toIntOrNull(), teamFormation, submission, prizes))
            }, enabled = name.length >= 2 && description.length >= 5 && venue.length >= 2) { Text("Save event") }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Close") } })
}

private fun String.toIsoDateTime(): String? = runCatching { LocalDateTime.parse(this).atZone(ZoneId.systemDefault()).toInstant().toString() }.getOrNull()
private fun String.toLocalDateTimeInput(): String = runCatching { java.time.OffsetDateTime.parse(this).toLocalDateTime().toString().take(16) }.getOrElse { take(16) }
