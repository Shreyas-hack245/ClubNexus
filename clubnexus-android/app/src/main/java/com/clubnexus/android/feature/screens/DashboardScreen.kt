package com.clubnexus.android.feature.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.clubnexus.android.data.model.DashboardData
import com.clubnexus.android.data.model.User
import com.clubnexus.android.data.network.ClubNexusApi
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(api: ClubNexusApi, user: User, onLogout: () -> Unit) {
    var data by remember { mutableStateOf<DashboardData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    fun refresh() { scope.launch { loading = true; error = null; runCatching { api.dashboard() }.onSuccess { data = it }.onFailure { error = it.apiMessage() }; loading = false } }
    LaunchedEffect(Unit) { refresh() }
    ScreenFrame("Dashboard", "A live command center for members, events, inventory, projects, and club communications.", user, onLogout) {
        ErrorMessage(error)
        if (loading) LoadingState()
        data?.let { dashboard ->
            val stats = listOf(
                "Total Members" to dashboard.stats.members,
                "Upcoming Events" to dashboard.stats.upcomingEvents,
                "Items Borrowed" to dashboard.stats.borrowed,
                "Active Projects" to dashboard.stats.activeProjects
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                stats.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (label, value) -> Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(14.dp)) { Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } }
                } }
            }
            InfoCard("Recent Activity", Modifier.padding(top = 16.dp)) {
                if (dashboard.activity.isEmpty()) EmptyMessage("No recent activity yet.")
                dashboard.activity.forEach { activity ->
                    Text(activity.type.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(activity.text, style = MaterialTheme.typography.bodyMedium)
                    Text(activity.at.toDisplayDate(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 9.dp))
                }
            }
        }
    }
}
