package com.freedu.myinterviews.presentation.company

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedu.myinterviews.domain.model.DocAttachment
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.Offer
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.presentation.components.StatusChip
import com.freedu.myinterviews.util.DateUtils
import com.freedu.myinterviews.util.PdfReport
import com.freedu.myinterviews.util.StatusUi
import kotlinx.coroutines.launch

/**
 * Application detail with tabs: Rounds | Prep & Reflection | Documents | Offer.
 * Covers CRUD for rounds, prep notes/checklist, reflections, documents and offer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    onBack: () -> Unit,
    onOpenCoach: (Long) -> Unit = {},
    vm: CompanyViewModel = hiltViewModel()
) {
    val app by vm.application.collectAsState()
    val rounds by vm.rounds.collectAsState()
    val docs by vm.documents.collectAsState()
    val offer by vm.offer.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var addRound by remember { mutableStateOf(false) }
    var editRound by remember { mutableStateOf<InterviewRound?>(null) }
    var reflectRound by remember { mutableStateOf<InterviewRound?>(null) }
    var prepRound by remember { mutableStateOf<InterviewRound?>(null) }
    var editApp by remember { mutableStateOf(false) }
    var editOffer by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val uri = res.data?.data ?: return@rememberLauncherForActivityResult
            val a = app ?: return@rememberLauncherForActivityResult
            scope.launch { runCatching { PdfReport.writeApplicationReport(ctx, uri, a, rounds) } }
        }
    }
    val docPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && app != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.saveDocument(
                DocAttachment(applicationId = app!!.id, name = uri.lastPathSegment ?: "file", uri = uri.toString())
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app?.jobTitle ?: "Application", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_TITLE, "interview-report.pdf")
                        }
                        pdfLauncher.launch(intent)
                    }) { Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF") }
                    if (app != null) {
                        IconButton(onClick = { editApp = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if ((tab == 0 || tab == 2) && app != null) {
                ExtendedFloatingActionButton(
                    onClick = { if (tab == 0) addRound = true else docPicker.launch(arrayOf("*/*")) },
                    icon = { Icon(if (tab == 0) Icons.Default.Event else Icons.Default.AttachFile, contentDescription = null) },
                    text = { Text(if (tab == 0) "Round" else "Attach") }
                )
            }
        }
    ) { pad ->
        val a = app
        if (a == null) {
            Column(Modifier.padding(pad).padding(24.dp)) {
                Text("Application not found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(Modifier.padding(pad).fillMaxSize()) {
                Card(
                    Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(Modifier.padding(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(a.jobTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(a.companyName, style = MaterialTheme.typography.bodyMedium)
                            Text("${a.location.ifBlank { "—" }} · ${a.workMode} · Applied ${DateUtils.date(a.appliedDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MatchAndMarketRow(a)
                        }
                        StatusChip(a.status)
                    }
                }
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Rounds") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Prep") })
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Docs") })
                    Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Offer") })
                    Tab(selected = tab == 4, onClick = { tab = 4 }, text = { Text("Journey") })
                }
                when (tab) {
                    0 -> RoundsTab(rounds,
                        onEdit = { editRound = it },
                        onReflect = { reflectRound = it },
                        onPrep = { prepRound = it; tab = 1 },
                        onDelete = { vm.deleteRound(it.id) })
                    1 -> PrepTab(rounds,
                        selected = prepRound,
                        onSelect = { prepRound = it },
                        onSave = { vm.saveRound(it) },
                        onMockInterview = { onOpenCoach(a.id) })
                    2 -> DocsTab(docs,
                        onDelete = { vm.deleteDocument(it) })
                    3 -> OfferTab(offer,
                        appId = a.id,
                        onEdit = { editOffer = true })
                    4 -> JourneyTab(app = a, rounds = rounds, offer = offer)
                }
            }
        }
    }

    if (addRound && app != null) {
        RoundFormDialog(applicationId = app!!.id, onDismiss = { addRound = false }) {
            vm.saveRound(enrich(it, app!!)); addRound = false
        }
    }
    if (editRound != null) {
        RoundFormDialog(applicationId = editRound!!.applicationId, initial = editRound,
            onDismiss = { editRound = null }) {
            vm.saveRound(enrich(it, app)); editRound = null
        }
    }
    if (reflectRound != null) {
        ReflectionDialog(round = reflectRound!!, onDismiss = { reflectRound = null }) {
            vm.saveRound(enrich(it, app)); reflectRound = null
        }
    }
    if (editApp && app != null) {
        ApplicationFormDialog(companyId = app!!.companyId, initial = app, onDismiss = { editApp = false }) {
            vm.saveApplication(it); editApp = false
        }
    }
    if (editOffer) {
        OfferFormDialog(applicationId = app?.id ?: 0, initial = offer, onDismiss = { editOffer = false }) {
            vm.saveOffer(it); editOffer = false
        }
    }
    if (confirmDelete && app != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete application?") },
            text = { Text("Rounds, documents and offer for this role will be removed.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteApplication(app!!.id) { onBack() } }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

private fun enrich(round: InterviewRound, app: JobApplication?): InterviewRound =
    round.copy(
        companyName = app?.companyName.orEmpty(),
        jobTitle = app?.jobTitle.orEmpty()
    )

@Composable
private fun RoundsTab(
    rounds: List<InterviewRound>,
    onEdit: (InterviewRound) -> Unit,
    onReflect: (InterviewRound) -> Unit,
    onPrep: (InterviewRound) -> Unit,
    onDelete: (InterviewRound) -> Unit
) {
    if (rounds.isEmpty()) {
        EmptyState(icon = Icons.Default.Event, title = "No rounds yet",
            subtitle = "Schedule your phone screen, technical or HR round to get reminders.")
        return
    }
    var del by remember { mutableStateOf<InterviewRound?>(null) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Spacer(Modifier.height(8.dp)) }
        items(rounds, key = { it.id }) { r ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.roundType.name.lowercase().replace('_', ' '),
                                fontWeight = FontWeight.SemiBold)
                            Text("${DateUtils.dateTime(r.scheduledAt)} · ${r.durationMin} min · ${r.mode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(StatusUi.roundStatusLabel(r.status),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    if (r.interviewers.isNotBlank()) {
                        Text("With: ${r.interviewers}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Outcome: ${r.outcome.name.lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusUi.roundOutcomeColor(r.outcome))
                        if (r.selfRating > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text("★ ${r.selfRating}/5", style = MaterialTheme.typography.bodySmall)
                        }
                        if (r.thankYouSent) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = "Thank-you sent",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Row {
                        TextButton(onClick = { onPrep(r) }) { Text("Prep") }
                        TextButton(onClick = { onReflect(r) }) { Text("Reflect") }
                        TextButton(onClick = { onEdit(r) }) { Text("Edit") }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { del = r }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete round")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
    if (del != null) {
        AlertDialog(
            onDismissRequest = { del = null },
            title = { Text("Delete round?") },
            text = { Text("Its prep notes and reflection will be removed.") },
            confirmButton = { TextButton(onClick = { onDelete(del!!); del = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { del = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PrepTab(
    rounds: List<InterviewRound>,
    selected: InterviewRound?,
    onSelect: (InterviewRound) -> Unit,
    onSave: (InterviewRound) -> Unit,
    onMockInterview: () -> Unit
) {
    if (rounds.isEmpty()) {
        EmptyState(icon = Icons.Default.Event, title = "No rounds to prepare for",
            subtitle = "Add a round first, then track checklists and resources here.")
        return
    }
    val current = selected?.let { s -> rounds.firstOrNull { it.id == s.id } } ?: rounds.first()
    var notes by remember(current.id) { mutableStateOf(current.prepNotes) }
    var resources by remember(current.id) { mutableStateOf(current.resources) }
    var checklistText by remember(current.id) {
        mutableStateOf(
            current.prepChecklistJson.split(";;").filter { it.isNotBlank() }
                .joinToString("\n") { it.trimStart('✓', '•', '-', ' ') }
        )
    }
    var checks by remember(current.id) {
        mutableStateOf(
            current.prepChecklistJson.split(";;").filter { it.isNotBlank() }
                .map { it.startsWith("✓") to it.trimStart('✓', '•', '-', ' ').trim() }
                .ifEmpty { listOf(false to "Research the company", false to "Revise DSA", false to "Prepare STAR stories") }
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedButton(onClick = onMockInterview, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Psychology, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start AI mock interview")
        }
        Spacer(Modifier.height(8.dp))
        Text("Round", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            rounds.forEach { r ->
                androidx.compose.material3.FilterChip(
                    selected = r.id == current.id,
                    onClick = { onSelect(r) },
                    label = { Text(r.roundType.name.lowercase().take(10)) }
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Checklist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        checks.forEachIndexed { i, (done, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                .fillMaxWidth()
                .clickable { checks = checks.toMutableList().also { it[i] = !done to label } }) {
                Checkbox(done, onCheckedChange = { checks = checks.toMutableList().also { l -> l[i] = it to label } })
                Text(label, modifier = Modifier.weight(1f))
                IconButton(onClick = { checks = checks.toMutableList().also { it.removeAt(i) } }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove item")
                }
            }
        }
        OutlinedTextField(
            checklistText, { checklistText = it },
            label = { Text("Add items (one per line), then tap +") },
            modifier = Modifier.fillMaxWidth(), minLines = 2
        )
        Button(
            onClick = {
                val extra = checklistText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    .map { false to it }
                checks = checks + extra
                checklistText = ""
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) { Text("Add items") }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        OutlinedTextField(notes, { notes = it }, label = { Text("Prep notes (markdown ok)") },
            modifier = Modifier.fillMaxWidth(), minLines = 4)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(resources, { resources = it }, label = { Text("Links / resources") },
            modifier = Modifier.fillMaxWidth(), minLines = 2)
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            onSave(current.copy(
                prepNotes = notes,
                resources = resources,
                prepChecklistJson = checks.joinToString(";;") { (done, label) ->
                    (if (done) "✓" else "•") + label
                }
            ))
        }) { Text("Save prep") }
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun DocsTab(docs: List<DocAttachment>, onDelete: (Long) -> Unit) {
    if (docs.isEmpty()) {
        EmptyState(icon = Icons.Default.AttachFile, title = "No documents",
            subtitle = "Attach resume versions, cover letters or offer letters.")
        return
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Spacer(Modifier.height(8.dp)) }
        items(docs, key = { it.id }) { d ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, fontWeight = FontWeight.SemiBold)
                        Text(d.type, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (d.isSentVersion) Text("sent", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { onDelete(d.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete document")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun OfferTab(offer: Offer?, appId: Long, onEdit: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (offer == null || offer.id == 0L) {
            EmptyState(icon = Icons.Default.Add, title = "No offer yet",
                subtitle = "When an offer arrives, record salary, bonus, equity and deadline here.")
            Button(onClick = onEdit) { Text("Add offer") }
        } else {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tc = com.freedu.myinterviews.util.CompCalc.annualized(
                        offer.baseSalary, offer.bonus, offer.equity
                    )
                    Text("≈ ${com.freedu.myinterviews.util.CompCalc.formatShort(tc)}/yr total comp",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Base: ${offer.baseSalary}", style = MaterialTheme.typography.bodyMedium)
                    Text("Bonus: ${offer.bonus}")
                    if (offer.equity.isNotBlank()) Text("Equity: ${offer.equity}")
                    if (offer.benefits.isNotBlank()) Text("Benefits: ${offer.benefits}")
                    Text(DateUtils.countdown(offer.deadline),
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    if (offer.notes.isNotBlank()) {
                        HorizontalDivider()
                        Text("Negotiation notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(offer.notes)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onEdit) { Text("Edit offer") }
        }
    }
}

/** JD match score + market salary hint under the application header. */
@Composable
private fun MatchAndMarketRow(app: JobApplication) {
    val match = remember(app.resumeText, app.jobDescription) {
        com.freedu.myinterviews.util.MatchScore.score(app.resumeText, app.jobDescription)
    }
    val market = remember(app.jobTitle) {
        com.freedu.myinterviews.util.CompCalc.marketHint(app.jobTitle)
    }
    if (app.jobDescription.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        if (app.resumeText.isBlank()) {
            Text("Paste resume text (edit ✎) to unlock JD match score",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("JD match: ${match.score}% · missing: ${match.missing.take(4).joinToString(", ").ifBlank { "nothing major" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
    if (market != null) {
        Text("Market base (indicative): ${com.freedu.myinterviews.util.CompCalc.formatShort(market.second)} median",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Visual journey timeline: applied → rounds → offer, shareable as text. */
@Composable
private fun JourneyTab(
    app: JobApplication,
    rounds: List<InterviewRound>,
    offer: Offer?
) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        JourneyNode(
            dot = MaterialTheme.colorScheme.primary,
            title = "Applied",
            subtitle = DateUtils.date(app.appliedDate),
            last = false
        )
        rounds.sortedBy { it.scheduledAt }.forEach { r ->
            val dot = when (r.outcome.name) {
                "PASSED" -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
                "FAILED", "GHOSTED" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.secondary
            }
            JourneyNode(
                dot = dot,
                title = "${r.roundType.name.lowercase().replace('_', ' ')} — ${r.status.name.lowercase()}",
                subtitle = "${DateUtils.dateTime(r.scheduledAt)}" +
                    (if (r.selfRating > 0) " · ★${r.selfRating}/5" else "") +
                    (if (r.outcome.name != "PENDING") " · ${r.outcome.name.lowercase()}" else ""),
                last = false
            )
        }
        if (offer != null && offer.id != 0L) {
            JourneyNode(
                dot = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                title = "Offer received",
                subtitle = "Base ${offer.baseSalary} · respond ${DateUtils.countdown(offer.deadline)}",
                last = true
            )
        } else {
            JourneyNode(
                dot = MaterialTheme.colorScheme.surfaceVariant,
                title = app.status.name.lowercase().replaceFirstChar { it.uppercase() },
                subtitle = "Current stage",
                last = true
            )
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                val sb = StringBuilder("${app.jobTitle} @ ${app.companyName} [${app.status}]\n")
                sb.appendLine("Applied ${DateUtils.date(app.appliedDate)}")
                rounds.sortedBy { it.scheduledAt }.forEach { r ->
                    sb.appendLine("- ${r.roundType} ${DateUtils.date(r.scheduledAt)}: ${r.status}/${r.outcome}")
                }
                if (offer != null && offer.id != 0L) {
                    sb.appendLine("Offer: base ${offer.baseSalary}, bonus ${offer.bonus}")
                }
                ctx.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, sb.toString())
                    }, "Share journey"
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Share journey as text") }
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun JourneyNode(
    dot: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    last: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        val lineColor = MaterialTheme.colorScheme.surfaceVariant
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                drawCircle(dot)
            }
            if (!last) {
                androidx.compose.foundation.Canvas(modifier = Modifier.width(2.dp).height(28.dp)) {
                    drawRect(lineColor)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(bottom = if (last) 0.dp else 8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
