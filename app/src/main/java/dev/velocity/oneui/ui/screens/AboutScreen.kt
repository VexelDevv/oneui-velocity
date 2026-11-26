package dev.velocity.oneui.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.velocity.oneui.core.BuildConstants
import dev.velocity.oneui.ui.SectionCard
import dev.velocity.oneui.ui.UiState

@Composable
fun AboutScreen(state: UiState) {
    Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard(title = "About") {
            Text(
                text = "Velocity for One UI, version ${state.version}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Targets Samsung One UI 8 (Android 16, API 36). Requires Magisk, " +
                    "Zygisk and LSPosed. Project and documentation: " + BuildConstants.GITHUB_URL,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "Setup checklist") {
            val steps = listOf(
                "1. Install the APK (CI artifact or built locally).",
                "2. Open LSPosed, enable Velocity for One UI.",
                "3. Set the module scope to com.android.systemui and com.sec.android.app.launcher.",
                "4. Reboot once (LSPosed loads the entry class at process start).",
                "5. Reopen this app: the Dashboard should show both processes as reported.",
                "6. Pick a profile, or toggle optimizations individually.",
                "7. Optional: run diagnostics sessions in the Diagnostics tab.",
            )
            steps.forEach { step ->
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SectionCard(title = "Safety model") {
            Text(
                text = "No system_server or zygote hooks. No background services, no polling, " +
                    "no wakeups. Every hook reports status and can be disabled live; any " +
                    "incompatible hook automatically reports UNSUPPORTED and stays " +
                    "uninstalled instead of crashing its host.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "If SystemUI ever misbehaves: switch the profile to Balanced (or disable " +
                    "the suspect hook) - it unhooks within a second. If things still look wrong, " +
                    "disable the module in LSPosed and reboot. Reset all configuration by " +
                    "clearing the app's data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "Honest scope statement") {
            Text(
                text = "This module ships only optimizations with a defensible, measurable " +
                    "benefit and contained failure modes, plus the instrumentation to verify " +
                    "them on your device. It does not promise fake frame rates, does not " +
                    "shorten One UI animations, does not bypass thermal protections (Samsung " +
                    "GOS is intentionally untouched), and reports UNSUPPORTED rather than " +
                    "pretending when a hook does not match this firmware.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
