package dev.velocity.oneui.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.velocity.oneui.core.Process
import dev.velocity.oneui.ui.SectionCard
import dev.velocity.oneui.ui.UiState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun DiagnosticsScreen(state: UiState) {
    val pendingCount = state.pendingIds.size
    LaunchedEffect(pendingCount) {
        while (state.pendingIds.isNotEmpty()) {
            delay(1500)
            state.refresh()
        }
    }
    Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IntroCard()
        Process.values().forEach { process ->
            DiagnosticsCard(state, process)
        }
    }
}

@Composable
private fun IntroCard() {
    SectionCard(title = "Frame-cadence diagnostics") {
        Text(
            text = "A session posts a Choreographer callback on the target process's main " +
                "thread for 10 seconds and reports the callback gap distribution: median, " +
                "p90, p99, max, and the share of windows missed by more than 1.5x the " +
                "median interval.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "This is an in-situ proxy for main-thread congestion, not SurfaceFlinger " +
                "frame deadline accounting. For authoritative jank numbers use the " +
                "dumpsys gfxinfo and Perfetto protocols in docs/BENCHMARKING.md, and " +
                "compare module-off vs module-on with identical scripted interactions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DiagnosticsCard(state: UiState, process: Process) {
    val pending = state.pendingIds.containsKey(process)
    SectionCard(title = "${process.display} session") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(
                onClick = { state.requestDiagnostics(process) },
                enabled = !pending,
            ) {
                Text(text = if (pending) "Running..." else "Run 10s session")
            }
        }
        state.latestBench[process]?.let { report ->
            MetricRow("Frames sampled", report.frameCount.toString())
            MetricRow("Median interval", formatMs(report.medianMs))
            MetricRow("p90 interval", formatMs(report.p90Ms))
            MetricRow("p99 interval", formatMs(report.p99Ms))
            MetricRow("Max interval", formatMs(report.maxMs))
            MetricRow("Missed windows", "${report.missedCount} (${formatPct(report.missedPercent)})")
            Text(
                text = "Reported for request #${report.requestId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (report.logTail.isNotEmpty()) {
                Text(
                    text = "Last log lines from the target process:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp),
                )
                report.logTail.takeLast(6).forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } ?: Text(
            text = "No session recorded for this process yet. Run one with the module off, " +
                "one with the module on, and compare the distributions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatMs(value: Double): String = String.format(Locale.US, "%.2f ms", value)
private fun formatPct(value: Double): String = String.format(Locale.US, "%.1f%%", value)
