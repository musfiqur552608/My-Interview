package com.freedu.myinterviews.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.presentation.components.LoadingState
import com.freedu.myinterviews.presentation.components.SectionHeader
import com.freedu.myinterviews.presentation.components.StatCard
import com.freedu.myinterviews.util.AnalyticsRange
import com.freedu.myinterviews.util.CompCalc
import com.freedu.myinterviews.util.DateUtils

/**
 * Interactive analytics dashboard. Every chart is tappable: tapping bars,
 * funnel stages, heatmap cells, offers and rating bars reveals the underlying
 * detail. The range selector (30D / 90D / 6M / All) recomputes everything via
 * [AnalyticsEngine] — pure, unit-tested, no extra dependencies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(vm: AnalyticsViewModel = hiltViewModel()) {
    val data by vm.data.collectAsState()
    val range by vm.range.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Analytics", fontWeight = FontWeight.Bold) }) }
    ) { pad ->
        val d = data
        if (d == null) {
            LoadingState(Modifier.padding(pad))
            return@Scaffold
        }
        if (d.appsInRange == 0 && d.range == AnalyticsRange.ALL) {
            Column(Modifier.padding(pad).fillMaxSize()) {
                EmptyState(
                    icon = Icons.Default.Analytics,
                    title = "No data yet",
                    subtitle = "Track a few applications and your charts will come alive here."
                )
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsRange.values().forEach { r ->
                        FilterChip(
                            selected = range == r,
                            onClick = { vm.setRange(r) },
                            label = { Text(r.label) }
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("${d.appsInRange}", "Applied", Modifier.weight(1f))
                    StatCard("${d.interviewsInRange}", "Interviews", Modifier.weight(1f))
                    StatCard("${(d.conversion * 100).toInt()}%", "Offer rate", Modifier.weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("${d.offersTotal}", "Offers", Modifier.weight(1f))
                    StatCard(d.bestDay, "Top apply day", Modifier.weight(1f))
                    StatCard(
                        d.overallResponseDays?.let { "${"%.1f".format(it)}d" } ?: "—",
                        "Avg response", Modifier.weight(1f)
                    )
                }
            }
            item {
                SectionHeader("Applications over time — tap a bar")
                AnalyticsCard { ActivityChart(d.buckets.map { it.count }, d.buckets.map { it.label to it.count }) }
            }
            item {
                SectionHeader("Funnel — tap a stage")
                AnalyticsCard { FunnelInteractive(d) }
            }
            item {
                SectionHeader("Weekly activity — tap a day")
                AnalyticsCard { Heatmap(d.heat.map { it.dayStart to it.count }) }
            }
            item {
                SectionHeader("Offers compared — tap an offer")
                AnalyticsCard { OffersInteractive(d) }
            }
            item {
                SectionHeader("Response time by company")
                AnalyticsCard { ResponseRows(d) }
            }
            item {
                SectionHeader("Self-ratings — tap a bar")
                AnalyticsCard { RatingsInteractive(d) }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun AnalyticsCard(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

/** Tap-to-inspect bar chart. Tap math mirrors draw math exactly. */
@Composable
private fun ActivityChart(counts: List<Int>, labels: List<Pair<String, Int>>) {
    var selected by remember(counts) { mutableIntStateOf(-1) }
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val bar = MaterialTheme.colorScheme.primary
    val barSel = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant
    if (counts.isEmpty() || counts.all { it == 0 }) {
        Text("Nothing in this range — try a wider window.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Text("Peak: ${counts.maxOrNull()} in one ${if (counts.size > 31) "period" else "day"}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Canvas(
        Modifier.fillMaxWidth().height(170.dp)
            .pointerInput(counts) {
                detectTapGestures { tap ->
                    val n = counts.size
                    if (n == 0) return@detectTapGestures
                    selected = ((tap.x / size.width) * n).toInt().coerceIn(0, n - 1)
                }
            }
    ) {
        val n = counts.size
        val bw = size.width / n
        counts.forEachIndexed { i, c ->
            val h = (size.height - 8) * (c.toFloat() / max)
            val left = i * bw + bw * 0.18f
            drawRoundRect(
                track, topLeft = Offset(left, 0f),
                size = Size(bw * 0.64f, size.height),
                cornerRadius = CornerRadius(8f, 8f)
            )
            if (h > 0) {
                drawRoundRect(
                    if (i == selected) barSel else bar,
                    topLeft = Offset(left, size.height - h),
                    size = Size(bw * 0.64f, h),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    if (selected >= 0 && selected < labels.size) {
        val (label, count) = labels[selected]
        Text("$label — $count application${if (count == 1) "" else "s"}",
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
    } else {
        Text("Tap any bar for the exact count.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FunnelInteractive(d: com.freedu.myinterviews.util.AnalyticsData) {
    var selected by remember(d) { mutableIntStateOf(-1) }
    val bar = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val max = (d.funnel.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    d.funnel.forEachIndexed { i, stage ->
        val frac = stage.count.toFloat() / max
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { selected = if (selected == i) -1 else i }
                .background(
                    if (selected == i) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else Color.Transparent
                )
                .padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Text(
                stage.status.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(110.dp)
            )
            Canvas(Modifier.weight(1f).height(20.dp)) {
                drawRoundRect(track, cornerRadius = CornerRadius(10f, 10f))
                if (frac > 0) {
                    drawRoundRect(
                        bar, cornerRadius = CornerRadius(10f, 10f),
                        size = Size(size.width * frac, size.height)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("${stage.count}", fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp))
        }
    }
    Spacer(Modifier.height(4.dp))
    if (selected in d.funnel.indices) {
        val s = d.funnel[selected]
        Text(
            "${s.status.name.lowercase().replaceFirstChar { it.uppercase() }}: ${s.count}" +
                (s.conversionFromPrev?.let { " · ${(it * 100).toInt()}% of previous stage" } ?: " · top of funnel"),
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        Text("Tap a stage for its conversion rate.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Heatmap(cells: List<Pair<Long, Int>>) {
    var selected by remember(cells) { mutableStateOf<Pair<Long, Int>?>(null) }
    val max = (cells.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val base = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val weeks = cells.chunked(7)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("", "M", "", "W", "", "F", "").forEach { dow ->
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    Text(dow, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { (day, count) ->
                    val isSel = selected?.first == day
                    Box(
                        Modifier.size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (count == 0) empty
                                else base.copy(alpha = 0.25f + 0.75f * count / max)
                            )
                            .then(
                                if (isSel) Modifier.border(
                                    2.dp, MaterialTheme.colorScheme.tertiary,
                                    RoundedCornerShape(6.dp)
                                ) else Modifier
                            )
                            .clickable { selected = if (isSel) null else day to count }
                    )
                }
                // Pad short final week (shouldn't happen — engine emits full weeks).
                repeat(7 - week.size) {
                    Box(Modifier.size(24.dp))
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Less", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        (0..4).forEach { i ->
            Box(
                Modifier.size(12.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (i == 0) empty else base.copy(alpha = 0.25f + 0.75f * i / 4))
            )
            Spacer(Modifier.width(2.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text("More", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        selected?.let { (day, count) ->
            Text("${DateUtils.date(day)} — $count",
                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun OffersInteractive(d: com.freedu.myinterviews.util.AnalyticsData) {
    if (d.offers.isEmpty()) {
        Text("No offers yet — they will stack up here for comparison.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var selected by remember(d) { mutableIntStateOf(0) }
    val max = d.offers.maxOf { it.totalAnnual }.coerceAtLeast(1)
    val bar = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    d.offers.forEachIndexed { i, o ->
        val frac = o.totalAnnual.toFloat() / max
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { selected = i }
                .background(
                    if (selected == i) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else Color.Transparent
                )
                .padding(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    o.offer.companyName.ifBlank { "Offer ${i + 1}" },
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(CompCalc.formatShort(o.totalAnnual),
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                drawRoundRect(track, cornerRadius = CornerRadius(8f, 8f))
                if (frac > 0) {
                    drawRoundRect(
                        bar, cornerRadius = CornerRadius(8f, 8f),
                        size = Size(size.width * frac, size.height)
                    )
                }
            }
        }
    }
    if (selected in d.offers.indices) {
        val o = d.offers[selected].offer
        Spacer(Modifier.height(6.dp))
        Text(
            "Base ${o.baseSalary} · Bonus ${o.bonus} · Equity ${o.equity.ifBlank { "—" }} · " +
                "Total ≈ ${CompCalc.formatShort(d.offers[selected].totalAnnual)}/yr" +
                (if (o.deadline > 0) " · ${DateUtils.countdown(o.deadline)}" else ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResponseRows(d: com.freedu.myinterviews.util.AnalyticsData) {
    if (d.responses.isEmpty()) {
        Text("Response times appear once rounds are scheduled.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    d.overallResponseDays?.let {
        Text("Average first response: ${"%.1f".format(it)} days",
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
    }
    d.responses.forEach { r ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(r.company, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("${"%.1f".format(r.avgDays)}d avg · ${r.samples} round${if (r.samples == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RatingsInteractive(d: com.freedu.myinterviews.util.AnalyticsData) {
    if (d.ratings.isEmpty()) {
        Text("Rate your rounds in post-interview reflections to unlock this.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var selected by remember(d) { mutableIntStateOf(-1) }
    val bar = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant
    d.ratings.forEachIndexed { i, r ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { selected = if (selected == i) -1 else i }
                .background(
                    if (selected == i) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else Color.Transparent
                )
                .padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Text(
                r.type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(110.dp)
            )
            Canvas(Modifier.weight(1f).height(20.dp)) {
                drawRoundRect(track, cornerRadius = CornerRadius(10f, 10f))
                drawRoundRect(
                    bar, cornerRadius = CornerRadius(10f, 10f),
                    size = Size(size.width * (r.avg / 5f).coerceIn(0f, 1f), size.height)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("%.1f".format(r.avg), fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp))
        }
    }
    Spacer(Modifier.height(4.dp))
    if (selected in d.ratings.indices) {
        val r = d.ratings[selected]
        Text("${r.type.name.lowercase().replace('_', ' ')} — ${"%.1f".format(r.avg)}/5 across ${r.count} rated round${if (r.count == 1) "" else "s"}",
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
    } else {
        Text("Tap a bar for sample sizes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
