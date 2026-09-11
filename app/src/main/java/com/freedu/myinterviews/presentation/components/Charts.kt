package com.freedu.myinterviews.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.myinterviews.domain.model.ApplicationStatus

/**
 * Compose-canvas charts (no third-party chart dependency — lighter APK, fully offline).
 * Deliberate architectural choice over Vico: the dashboard needs only funnel + trend
 * bars, and canvas keeps the build fast and the theming fully Material 3 dynamic.
 */

/** Horizontal funnel: Applied → Screening → Interviewing → Offer → Accepted. */
@Composable
fun FunnelChart(
    counts: Map<ApplicationStatus, Int>,
    modifier: Modifier = Modifier
) {
    val stages = ApplicationStatus.funnelOrder
    val max = (stages.maxOfOrNull { counts[it] ?: 0 } ?: 0).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier.fillMaxWidth().padding(16.dp)) {
        stages.forEach { stage ->
            val count = counts[stage] ?: 0
            val frac = count.toFloat() / max
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stage.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(96.dp)
                )
                Canvas(modifier = Modifier.weight(1f).height(22.dp)) {
                    drawRoundRect(trackColor, cornerRadius = CornerRadius(12f, 12f))
                    if (frac > 0) {
                        drawRoundRect(
                            barColor, cornerRadius = CornerRadius(12f, 12f),
                            size = androidx.compose.ui.geometry.Size(size.width * frac, size.height)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("$count", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp))
            }
            Spacer(Modifier.height(6.dp))
        }
        if (max > 0) {
            val applied = counts[ApplicationStatus.APPLIED] ?: 0
            val offers = (counts[ApplicationStatus.OFFER] ?: 0) + (counts[ApplicationStatus.ACCEPTED] ?: 0)
            val rate = if (applied == 0) 0 else (offers * 100 / applied.coerceAtLeast(1))
            Text(
                "Applied → Offer conversion: $rate%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Vertical bar strip for pass-rate by round type. */
@Composable
fun PassRateBars(
    rates: Map<com.freedu.myinterviews.domain.model.RoundType, Float>,
    modifier: Modifier = Modifier
) {
    val entries = rates.entries.toList()
    if (entries.isEmpty()) return
    val passColor = androidx.compose.ui.graphics.Color(0xFF2E7D32)
    val otherColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    Column(modifier.fillMaxWidth().padding(16.dp)) {
        Text("Pass rate by round type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            entries.forEach { (type, rate) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${(rate * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    Canvas(modifier = Modifier.fillMaxWidth(0.6f).height((24 + rate * 90).dp)) {
                        drawRoundRect(
                            if (rate >= 0.5f) passColor else otherColor,
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                    Text(
                        type.name.lowercase().take(6),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
