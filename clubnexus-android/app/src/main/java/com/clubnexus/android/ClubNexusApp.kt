package com.clubnexus.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.clubnexus.android.data.auth.SessionStore
import com.clubnexus.android.data.network.ApiClient
import com.clubnexus.android.feature.auth.LoginScreen
import com.clubnexus.android.feature.auth.SessionViewModel
import com.clubnexus.android.feature.auth.SessionViewModelFactory
import com.clubnexus.android.feature.screens.*

private data class AppDestination(val route: String, val label: String, val icon: ImageVector)
private val appDestinations = listOf(
    AppDestination("dashboard", "Dashboard", Icons.Default.Dashboard),
    AppDestination("members", "Members", Icons.Default.People),
    AppDestination("attendance", "Attendance", Icons.Default.BarChart),
    AppDestination("inventory", "Inventory", Icons.Default.Inventory2),
    AppDestination("projects", "Projects", Icons.Default.FolderOpen),
    AppDestination("events", "Events", Icons.Default.CalendarMonth),
    AppDestination("email", "Email Studio", Icons.Default.Mail)
)

@Composable
fun ClubNexusApp() {
    val context = LocalContext.current
    val sessionStore = remember(context) { SessionStore(context.applicationContext) }
    val sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(sessionStore))
    val session by sessionViewModel.session.collectAsState()
    when {
        BuildConfig.API_BASE_URL.isBlank() -> ApiConfigurationRequired(session?.user?.name, sessionViewModel::logout)
        sessionViewModel.restoring -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        session == null -> LoginScreen(sessionViewModel)
        else -> {
            val api = remember(sessionStore) { ApiClient.create(sessionStore) }
            MainShell(session!!.user, sessionViewModel, api)
        }
    }
}

@Composable
private fun ApiConfigurationRequired(name: String?, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("ClubNexus", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Production API URL required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Build with CLUBNEXUS_PROD_API_URL set to your HTTPS Express API base URL ending in /api/.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        name?.let { Text("Signed in as $it") }
        TextButton(onClick = onLogout) { Text("Sign out") }
    }
}

@Composable
private fun MainShell(user: com.clubnexus.android.data.model.User, auth: SessionViewModel, api: com.clubnexus.android.data.network.ClubNexusApi) {
    val controller = rememberNavController()
    val backStack by controller.currentBackStackEntryAsState()
    val primary = appDestinations.take(3) + AppDestination("more", "More", Icons.Default.MoreHoriz)
    Scaffold(bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            primary.forEach { dest ->
                val selected = backStack?.destination?.hierarchy?.any { it.route == dest.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = { controller.navigate(dest.route) { popUpTo(controller.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label, maxLines = 1) }
                )
            }
        }
    }) { padding ->
        NavHost(controller, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen(api, user, auth::logout) }
            composable("members") { MembersScreen(api, user) }
            composable("attendance") { AttendanceScreen(api, user) }
            composable("inventory") { InventoryScreen(api, user) }
            composable("projects") { ProjectsScreen(api, user) }
            composable("events") { EventsScreen(api, user) }
            composable("email") { EmailStudioScreen(api, user) }
            composable("more") {
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    Text("ClubNexus", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("More", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${user.name} • ${user.role}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    appDestinations.drop(3).forEach { dest ->
                        TextButton(onClick = { controller.navigate(dest.route) { launchSingleTop = true } }) {
                            Icon(dest.icon, contentDescription = null)
                            Text(dest.label, Modifier.padding(start = 12.dp))
                        }
                    }
                    TextButton(onClick = auth::logout) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
