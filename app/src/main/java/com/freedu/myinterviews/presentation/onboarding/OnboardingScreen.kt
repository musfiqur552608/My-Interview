package com.freedu.myinterviews.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class Page(val icon: ImageVector, val title: String, val body: String)

private val PAGES = listOf(
    Page(Icons.Default.Work, "Track every application",
        "Companies → applications → interview rounds in one offline-first pipeline."),
    Page(Icons.Default.CalendarMonth, "Never miss a round",
        "Calendar view, reminders before each interview and thank-you nudges after."),
    Page(Icons.Default.Analytics, "Prepare and reflect",
        "Prep checklists, question bank, post-interview reflections and offer comparison."),
    Page(Icons.Default.Notifications, "Private by default",
        "All data stays on your device. Optional CSV backup and app lock in Settings.")
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        HorizontalPager(state = pager, modifier = Modifier.weight(3f)) { i ->
            val p = PAGES[i]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(p.icon, contentDescription = null, modifier = Modifier.padding(24.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text(p.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(p.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(PAGES.size) { i ->
                Text(if (i == pager.currentPage) "● " else "○ ",
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (pager.currentPage < PAGES.size - 1) {
                OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Skip") }
                Button(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    modifier = Modifier.weight(1f)
                ) { Text("Next") }
            } else {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
