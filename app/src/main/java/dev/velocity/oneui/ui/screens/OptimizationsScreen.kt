package dev.velocity.oneui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.velocity.oneui.catalog.HookCatalog
import dev.velocity.oneui.catalog.Verification
import dev.velocity.oneui.core.Process
import dev.velocity.oneui.ui.SectionCard
import dev.velocity.oneui.ui.StatusChip
import dev.velocity.oneui.ui.UiState

@Composable
fun OptimizationsScreen(state: UiState) {
    Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HookCatalog.hooks.forEach { hook ->
            HookCard(state, hook)
        }
        ScopeCard()
    }
}

@Composable
private fun HookCard(state: UiState, hook: HookCatalog.HookInfo) {
    SectionCard(title = hook.title) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VerificationChip(hook.verification)
            Text(
                text = hook.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Text(
            text = hook.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Scoped to: ${hook.processes.joinToString { it.display }} | default: ${hook.defaultProfileBehavior}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        hook.processes.forEach { process ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = process.display,
                    style = MaterialTheme.typography.bodyMedium,
                )
                StatusChip(status = state.statusFor(process, hook.id)?.status)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (state.hasOverride(hook.id)) "Override active" else "Follows profile",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = state.effectiveEnabled(hook.id),
                onCheckedChange = { enabled -> state.setOverride(hook.id, enabled) },
            )
        }
        if (state.hasOverride(hook.id)) {
            TextButton(onClick = { state.setOverride(hook.id, null) }) {
                Text(text = "Reset to profile default")
            }
        }
    }
}

@Composable
private fun VerificationChip(verification: Verification) {
    val label = when (verification) {
        Verification.VERIFIED_STABLE -> "STABLE API"
        Verification.REQUIRES_CONFIRMATION -> "VERIFY ON DEVICE"
    }
    Box(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(50),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ScopeCard() {
    SectionCard(title = "Why only these optimizations?") {
        Text(
            text = "Every candidate is judged by the same rule: a measurable, defensible " +
                "benefit AND a contained failure mode. Many popular 'speed module' tricks " +
                "fail that test - see docs/HOOKS.md for the full rejected list and rationale " +
                "(input latency, animation shortening, GPU pipeline, thermal and memory " +
                "tampering are all rejected with reasons).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
