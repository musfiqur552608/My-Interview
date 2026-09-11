package com.freedu.myinterviews.presentation.company

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.Contact
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.Offer
import com.freedu.myinterviews.domain.model.RoundOutcome
import com.freedu.myinterviews.domain.model.RoundStatus
import com.freedu.myinterviews.domain.model.RoundType
import com.freedu.myinterviews.domain.model.WorkMode
import com.freedu.myinterviews.util.DateUtils

/** Shared add/edit dialogs used from Dashboard (quick-add FAB), Pipeline and detail screens. */

@Composable
fun CompanyFormDialog(
    initial: Company? = null,
    onDismiss: () -> Unit,
    onSave: (Company) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var website by remember { mutableStateOf(initial?.website.orEmpty()) }
    var industry by remember { mutableStateOf(initial?.industry.orEmpty()) }
    var source by remember { mutableStateOf(initial?.source.orEmpty()) }
    var tags by remember { mutableStateOf(initial?.tags?.joinToString(", ").orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    // Dossier + accent
    var accent by remember { mutableStateOf(initial?.accentColor ?: 0L) }
    var size by remember { mutableStateOf(initial?.size.orEmpty()) }
    var funding by remember { mutableStateOf(initial?.funding.orEmpty()) }
    var difficulty by remember { mutableStateOf(initial?.difficulty ?: "") }
    var rating by remember { mutableStateOf(initial?.rating ?: 0f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add company" else "Edit company") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("Company name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Accent color", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    AccentSwatch(0, accent == 0L) { accent = 0 }
                    listOf(0xFF6750A4, 0xFF2E7D32, 0xFF1565C0, 0xFFC62828, 0xFFE65100, 0xFF00695C).forEach { c ->
                        AccentSwatch(c, accent == c) { accent = c }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(website, { website = it }, label = { Text("Website") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(industry, { industry = it }, label = { Text("Industry") }, singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(source, { source = it }, label = { Text("Source") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text("Company dossier", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(size, { size = it }, label = { Text("Size (e.g. 200)") }, singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(funding, { funding = it }, label = { Text("Funding (Series B)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(difficulty, { difficulty = it }, label = { Text("Interview difficulty") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Rating: ", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                    (0..5).forEach { i ->
                        TextButton(onClick = { rating = i.toFloat() }) {
                            Text(if (i <= rating.toInt() && i > 0) "★" else "☆")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(tags, { tags = it }, label = { Text("Tags (comma separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        (initial ?: Company(name = name.trim())).copy(
                            name = name.trim(), website = website.trim(), industry = industry.trim(),
                            source = source.trim(),
                            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            notes = notes.trim(),
                            accentColor = accent, size = size.trim(), funding = funding.trim(),
                            difficulty = difficulty.trim(), rating = rating
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AccentSwatch(argb: Long, selected: Boolean, onPick: () -> Unit) {
    val color = if (argb == 0L) androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
    else androidx.compose.ui.graphics.Color(argb.toULong())
    androidx.compose.material3.Surface(
        onClick = onPick,
        modifier = Modifier.padding(end = 8.dp).size(32.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = color,
        border = if (selected) androidx.compose.foundation.BorderStroke(
            2.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary
        ) else null
    ) {}
}

@Composable
fun ApplicationFormDialog(
    companyId: Long,
    initial: JobApplication? = null,
    onDismiss: () -> Unit,
    onSave: (JobApplication) -> Unit
) {
    var title by remember { mutableStateOf(initial?.jobTitle.orEmpty()) }
    var link by remember { mutableStateOf(initial?.jobLink.orEmpty()) }
    var location by remember { mutableStateOf(initial?.location.orEmpty()) }
    var salaryMin by remember { mutableStateOf(initial?.salaryMin?.takeIf { it > 0 }?.toString().orEmpty()) }
    var salaryMax by remember { mutableStateOf(initial?.salaryMax?.takeIf { it > 0 }?.toString().orEmpty()) }
    var resume by remember { mutableStateOf(initial?.resumeVersion.orEmpty()) }
    var resumeText by remember { mutableStateOf(initial?.resumeText.orEmpty()) }
    var desc by remember { mutableStateOf(initial?.jobDescription.orEmpty()) }
    var status by remember { mutableStateOf(initial?.status ?: ApplicationStatus.APPLIED) }
    var mode by remember { mutableStateOf(initial?.workMode ?: WorkMode.HYBRID) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add application" else "Edit application") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(title, { title = it }, label = { Text("Job title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    EnumDropDown("Status", status, { status = it }, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    EnumDropDown("Mode", mode, { mode = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(location, { location = it }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(salaryMin, { salaryMin = it.filter(Char::isDigit) }, label = { Text("Salary min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(salaryMax, { salaryMax = it.filter(Char::isDigit) }, label = { Text("Salary max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(link, { link = it }, label = { Text("Job link") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(resume, { resume = it }, label = { Text("Resume version used") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(resumeText, { resumeText = it }, label = { Text("Resume text (for JD match score)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(desc, { desc = it }, label = { Text("Description / notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onSave(
                    (initial ?: JobApplication(companyId = companyId, jobTitle = title.trim())).copy(
                        companyId = initial?.companyId ?: companyId,
                        jobTitle = title.trim(), jobLink = link.trim(), location = location.trim(),
                        salaryMin = salaryMin.toLongOrNull() ?: 0,
                        salaryMax = salaryMax.toLongOrNull() ?: 0,
                        resumeVersion = resume.trim(), resumeText = resumeText,
                        jobDescription = desc.trim(),
                        status = status, workMode = mode
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun <T : Enum<T>> EnumDropDown(
    label: String,
    value: T,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${value.name.lowercase().replace('_', ' ')}", maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val constants = value.javaClass.enumConstants?.toList().orEmpty()
            constants.forEach { e ->
                @Suppress("UNCHECKED_CAST")
                val item = e as T
                DropdownMenuItem(text = { Text(item.name.lowercase().replace('_', ' ')) }, onClick = { onPick(item); open = false })
            }
        }
    }
}

@Composable
fun RoundFormDialog(
    applicationId: Long,
    initial: InterviewRound? = null,
    onDismiss: () -> Unit,
    onSave: (InterviewRound) -> Unit
) {
    var type by remember { mutableStateOf(initial?.roundType ?: RoundType.TECHNICAL) }
    var status by remember { mutableStateOf(initial?.status ?: RoundStatus.UPCOMING) }
    var outcome by remember { mutableStateOf(initial?.outcome ?: RoundOutcome.PENDING) }
    var mode by remember { mutableStateOf(initial?.mode ?: "Video") }
    var interviewers by remember { mutableStateOf(initial?.interviewers.orEmpty()) }
    var link by remember { mutableStateOf(initial?.platformLink.orEmpty()) }
    var duration by remember { mutableStateOf((initial?.durationMin ?: 60).toString()) }
    var daysFromNow by remember {
        mutableStateOf(
            initial?.let {
                val d = ((it.scheduledAt - System.currentTimeMillis()) / 86_400_000L).toInt().coerceAtLeast(0)
                d.toString()
            } ?: "1"
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Schedule round" else "Edit round") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Scheduled ${DateUtils.dateTime(initial?.scheduledAt ?: System.currentTimeMillis())}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    EnumDropDown("Type", type, { type = it }, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    EnumDropDown("Status", status, { status = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    EnumDropDown("Outcome", outcome, { outcome = it }, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(mode, { mode = it }, label = { Text("Mode") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(daysFromNow, { daysFromNow = it.filter(Char::isDigit) },
                        label = { Text("Days from now") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(duration, { duration = it.filter(Char::isDigit) },
                        label = { Text("Mins") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(interviewers, { interviewers = it }, label = { Text("Interviewer(s)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(link, { link = it }, label = { Text("Platform link (Zoom/Meet)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val days = daysFromNow.toLongOrNull() ?: 1
                val base = initial?.scheduledAt ?: System.currentTimeMillis()
                // Keep time-of-day, shift date if new round; edits keep original timestamp.
                val scheduled = if (initial == null) System.currentTimeMillis() + days * 86_400_000L else base
                onSave(
                    (initial ?: InterviewRound(applicationId = applicationId)).copy(
                        applicationId = initial?.applicationId ?: applicationId,
                        roundType = type, status = status, outcome = outcome, mode = mode.trim(),
                        interviewers = interviewers.trim(), platformLink = link.trim(),
                        durationMin = duration.toIntOrNull() ?: 60,
                        scheduledAt = scheduled
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ReflectionDialog(
    round: InterviewRound,
    onDismiss: () -> Unit,
    onSave: (InterviewRound) -> Unit
) {
    var questions by remember { mutableStateOf(round.questionsAsked) }
    var answers by remember { mutableStateOf(round.yourAnswers) }
    var well by remember { mutableStateOf(round.wentWell) }
    var improve by remember { mutableStateOf(round.toImprove) }
    var rating by remember { mutableStateOf(round.selfRating) }
    var thanks by remember { mutableStateOf(round.thankYouSent) }
    var voiceUri by remember { mutableStateOf(round.voiceMemoUri) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post-interview reflection") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                com.freedu.myinterviews.presentation.components.VoiceMemoSection(
                    existingUri = voiceUri,
                    onRecorded = { voiceUri = it },
                    onCleared = { voiceUri = "" }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(questions, { questions = it }, label = { Text("Questions asked") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(answers, { answers = it }, label = { Text("Your answers") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Self-rating: $rating/5")
                    Spacer(Modifier.width(8.dp))
                    (1..5).forEach { i ->
                        TextButton(onClick = { rating = i }) { Text(if (i <= rating) "★" else "☆") }
                    }
                }
                OutlinedTextField(well, { well = it }, label = { Text("What went well") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(improve, { improve = it }, label = { Text("What to improve") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(thanks, { thanks = it })
                    Text("Thank-you email sent")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(round.copy(questionsAsked = questions, yourAnswers = answers, selfRating = rating,
                    wentWell = well, toImprove = improve, thankYouSent = thanks,
                    voiceMemoUri = voiceUri,
                    status = RoundStatus.COMPLETED))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ContactFormDialog(
    initial: Contact? = null,
    companyId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (Contact) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var role by remember { mutableStateOf(initial?.role.orEmpty()) }
    var email by remember { mutableStateOf(initial?.email.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var linkedin by remember { mutableStateOf(initial?.linkedIn.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add contact" else "Edit contact") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(role, { role = it }, label = { Text("Role") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(linkedin, { linkedin = it }, label = { Text("LinkedIn") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Relationship notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave((initial ?: Contact(name = name.trim(), companyId = companyId)).copy(
                    name = name.trim(), role = role.trim(), email = email.trim(),
                    phone = phone.trim(), linkedIn = linkedin.trim(), notes = notes.trim()
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun OfferFormDialog(
    applicationId: Long,
    initial: Offer? = null,
    onDismiss: () -> Unit,
    onSave: (Offer) -> Unit
) {
    var base by remember { mutableStateOf(initial?.baseSalary?.takeIf { it > 0 }?.toString().orEmpty()) }
    var bonus by remember { mutableStateOf(initial?.bonus?.takeIf { it > 0 }?.toString().orEmpty()) }
    var equity by remember { mutableStateOf(initial?.equity.orEmpty()) }
    var benefits by remember { mutableStateOf(initial?.benefits.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var deadlineDays by remember {
        mutableStateOf(initial?.deadline?.takeIf { it > 0 }?.let {
            ((it - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0).toString()
        }.orEmpty())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null || initial.id == 0L) "Add offer" else "Edit offer") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(base, { base = it.filter(Char::isDigit) }, label = { Text("Base salary") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(bonus, { bonus = it.filter(Char::isDigit) }, label = { Text("Bonus") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(equity, { equity = it }, label = { Text("Equity") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(benefits, { benefits = it }, label = { Text("Benefits") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(deadlineDays, { deadlineDays = it.filter(Char::isDigit) },
                    label = { Text("Respond within (days)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Negotiation notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val days = deadlineDays.toLongOrNull()
                onSave((initial ?: Offer(applicationId = applicationId)).copy(
                    applicationId = initial?.applicationId ?: applicationId,
                    baseSalary = base.toLongOrNull() ?: 0, bonus = bonus.toLongOrNull() ?: 0,
                    equity = equity.trim(), benefits = benefits.trim(), notes = notes.trim(),
                    deadline = if (days != null) System.currentTimeMillis() + days * 86_400_000L else (initial?.deadline ?: 0)
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
