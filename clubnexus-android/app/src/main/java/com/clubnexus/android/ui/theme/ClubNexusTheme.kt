package com.clubnexus.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClubNexusColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF07111F),
    secondary = Color(0xFF7DD3FC),
    background = Color(0xFF070B14),
    onBackground = Color(0xFFE5EEFB),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFE5EEFB),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = Color(0xFFF43F5E)
)

@Composable
fun ClubNexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClubNexusColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
