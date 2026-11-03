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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.clubnexus.android.data.model.Member
import com.clubnexus.android.data.model.Project
import com.clubnexus.android.data.model.ProjectMemberRequest
import com.clubnexus.android.data.model.ProjectRequest
import com.clubnexus.android.data.model.User
import com.clubnexus.android.data.network.ClubNexusApi
import kotlinx.coroutines.launch

private val projectStatuses = listOf("PLANNING", "ACTIVE", "COMPLETED", "ON_HOLD", "CANCELLED")

@Composable
fun ProjectsScreen(api: ClubNexusApi, user: User) {
    val scope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<Project?>(null) }
    var adding by remember { mutableStateOf(false) }
    var teamProject by remember { mutableStateOf<Project?>(null) }
    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    suspend fun refresh() { runCatching { api.projects() }.onSuccess { projects = it; error = null }.onFailure { error = it.apiMessage() }; loading = false }
    LaunchedEffect(Unit) { refresh() }
    ScreenFrame("Projects", "Track project status, tags, GitHub links, team roles, and progress.", user) {
        ErrorMessage(error)
        if (user.role != "VIEWER") Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) { Text("+  Add project") }
        if (loading) LoadingState()
        if (!loading && projects.isEmpty()) EmptyMessage("No projects have been created yet.")
        projects.forEach { project ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(project.status.replace('_', ' '), color = MaterialTheme.colorScheme.primary)
                    }
                    Text(project.description)
                    LinearProgressIndicator(progress = { project.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${project.progress}% • ${project.members.size} team members • Started ${project.startDate.toDisplayDate()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (project.endDate != null) Text("Ends ${project.endDate.toDisplayDate()}", style = MaterialTheme.typography.bodySmall)
                    if (project.tags.isNotEmpty()) Text(project.tags.joinToString("  •  "), color = MaterialTheme.colorScheme.primary)
                    project.githubUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        Text("GitHub repository", Modifier.clickable { runCatching { uriHandler.openUri(url) } }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    project.members.forEach { Text("${it.member.user.name} • ${it.role}", style = MaterialTheme.typography.bodySmall) }
                    Row {
                        if (user.role != "VIEWER") {
                            TextButton(onClick = { editor = project }) { Text("Edit") }
                            TextButton(onClick = { teamProject = project; scope.launch { runCatching { members = api.members() }.onFailure { error = it.apiMessage() } } }) { Text("Manage team") }
                        }
                    }
                }
            }
        }
    }
    if (adding) ProjectEditor(null, { adding = false }) { payload -> scope.launch { runCatching { api.createProject(payload) }.onSuccess { adding = false; refresh() }.onFailure { error = it.apiMessage() } } }
    editor?.let { project -> ProjectEditor(project, { editor = null }) { payload -> scope.launch { runCatching { api.updateProject(project.id, payload) }.onSuccess { editor = null; refresh() }.onFailure { error = it.apiMessage() } } } }
    teamProject?.let { project -> TeamDialog(project, members, user, { teamProject = null }, onAdd = { request -> scope.launch { runCatching { api.addProjectMember(project.id, request) }.onSuccess { refresh(); teamProject = projects.firstOrNull { it.id == project.id } ?: project }.onFailure { error = it.apiMessage() } } }, onRemove = { memberId -> scope.launch { runCatching { api.removeProjectMember(project.id, memberId) }.onSuccess { refresh(); teamProject = projects.firstOrNull { it.id == project.id } ?: project }.onFailure { error = it.apiMessage() } } }) }
}

@Composable
private fun ProjectEditor(project: Project?, onClose: () -> Unit, onSubmit: (ProjectRequest) -> Unit) {
    var name by remember(project) { mutableStateOf(project?.name.orEmpty()) }
    var description by remember(project) { mutableStateOf(project?.description.orEmpty()) }
    var status by remember(project) { mutableStateOf(project?.status ?: "PLANNING") }
    var start by remember(project) { mutableStateOf(project?.startDate?.take(10) ?: java.time.LocalDate.now().toString()) }
    var end by remember(project) { mutableStateOf(project?.endDate?.take(10).orEmpty()) }
    var github by remember(project) { mutableStateOf(project?.githubUrl.orEmpty()) }
    var tags by remember(project) { mutableStateOf(project?.tags?.joinToString(", ").orEmpty()) }
    AlertDialog(onDismissRequest = onClose, title = { Text(if (project == null) "Add project" else "Edit project") }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") })
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2)
            ChoiceField("Status", status, projectStatuses.map { it to it }) { status = it }
            OutlinedTextField(start, { start = it }, label = { Text("Start date (YYYY-MM-DD)") })
            OutlinedTextField(end, { end = it }, label = { Text("End date (optional)") })
            OutlinedTextField(github, { github = it }, label = { Text("GitHub URL") })
            OutlinedTextField(tags, { tags = it }, label = { Text("Tags (comma separated)") })
            TextButton(onClick = {
                val validDate = runCatching { java.time.LocalDate.parse(start); if (end.isNotBlank()) java.time.LocalDate.parse(end) }.isSuccess
                if (validDate) onSubmit(ProjectRequest(name.trim(), description.trim(), status, start.toIsoDate(), end.takeIf { it.isNotBlank() }?.toIsoDate(), github.trim().ifBlank { null }, tags.split(',').map { it.trim() }.filter { it.isNotBlank() }))
            }, enabled = name.length >= 2 && description.length >= 5) { Text("Save project") }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Close") } })
}

private fun String.toIsoDate(): String = if (length == 10) "${this}T00:00:00.000Z" else this

@Composable
private fun TeamDialog(project: Project, members: List<Member>, user: User, onClose: () -> Unit, onAdd: (ProjectMemberRequest) -> Unit, onRemove: (String) -> Unit) {
    var memberId by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CONTRIBUTOR") }
    AlertDialog(onDismissRequest = onClose, title = { Text("${project.name} team") }, text = {
        Column {
            project.members.forEach { assignment ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${assignment.member.user.name} • ${assignment.role}")
                    if (user.role != "VIEWER") TextButton(onClick = { onRemove(assignment.member.id) }) { Text("Remove") }
                }
            }
            ChoiceField("Member", members.firstOrNull { it.id == memberId }?.user?.name ?: "Select member", members.filter { candidate -> project.members.none { it.member.id == candidate.id } }.map { it.user.name to it.id }) { memberId = it }
            ChoiceField("Project role", role, listOf("LEAD", "CONTRIBUTOR", "REVIEWER").map { it to it }) { role = it }
            TextButton(onClick = { if (memberId.isNotBlank()) onAdd(ProjectMemberRequest(memberId, role)) }, enabled = memberId.isNotBlank()) { Text("Add to team") }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Done") } })
}
