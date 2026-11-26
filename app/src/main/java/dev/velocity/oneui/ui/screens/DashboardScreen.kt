package dev.velocity.oneui.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.velocity.oneui.core.Process
import dev.velocity.oneui.profiles.Profile
import dev.velocity.oneui.ui.SectionCard
import dev.velocity.oneui.ui.StatusChip
import dev.velocity.oneui.ui.UiState

@Composable
fun DashboardScreen(state: UiState) {
    Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProcessStatusSection(state)
        ProfileSection(state)
        NoteCard()
    }
}

@Composable
private fun ProcessStatusSection(state: UiState) {
    SectionCard(title = "Live hook status") {
        if (state.statuses.isEmpty()) {
            Text(
                text = "No target process has reported yet. Enable the module in LSPosed with scope " +
                    "com.android.systemui and com.sec.android.app.launcher, reboot, then reopen this app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.statuses.forEach { processStatus ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = processStatus.process.display,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val lastSeen = if (processStatus.lastSeen > 0) {
                        DateUtils.getRelativeTimeSpanString(processStatus.lastSeen).toString()
                    } else "never"
                    Text(
                        text = "reported: $lastSeen" +
                            if (processStatus.version.isNotEmpty()) " | v${processStatus.version}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${state.activeHooksFor(processStatus.process)} active",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.activeHooksFor(processStatus.process) > 0)
                        Color(0xFF1E8E3E) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProfileSection(state: UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Performance profile (applies live, no reboot)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Profile.values().forEach { profile ->
            val selected = state.profile == profile
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable { state.setProfile(profile) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = profile.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        if (selected) {
                            Text(
                                text = "SELECTED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = profile.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCard() {
    SectionCard(title = "How switching works") {
        Text(
            text = "Changing a profile or a toggle writes the configuration and notifies the " +
                "hooked processes; they install or unhook the affected optimizations within a " +
                "second. Nothing persists across a reboot except your choices - the module " +
                "itself is inert until LSPosed loads it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "This module deliberately ships only optimizations whose benefit is " +
                "measurable and whose failure modes are contained. It never shortens " +
                "animation durations, never kills background apps and never touches " +
                "Samsung GOS thermal management.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
