package com.freedu.myinterviews.presentation.simport

import android.accounts.AccountManager
import android.app.Activity
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.myinterviews.google.GmailImport
import com.freedu.myinterviews.google.GmailMessage
import com.freedu.myinterviews.google.GoogleAuth
import com.freedu.myinterviews.google.TokenExpiredException
import com.freedu.myinterviews.presentation.components.EmptyState
import kotlinx.coroutines.launch

/**
 * Gmail import (read-only): pick a Google account on the device, list recent
 * interview-related mail, tap one to decode its body straight into Smart
 * Import. OAuth via AccountManager — no passwords, no keys, revocable anytime.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmailScreen(
    onBack: () -> Unit,
    onBodyReady: (String) -> Unit
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<GmailMessage>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var retryAfterConsent by remember { mutableStateOf<(() -> Unit)?>(null) }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // System consent dialog closed — retry whatever needed it.
        retryAfterConsent?.invoke()
        retryAfterConsent = null
    }

    fun loadMail(acct: String) {
        val act = activity ?: run { status = "Needs an Activity context."; return }
        loading = true
        status = null
        scope.launch {
            when (val auth = GoogleAuth.getToken(act, acct, GoogleAuth.GMAIL_READONLY)) {
                is GoogleAuth.AuthResult.Token -> {
                    status = runCatching {
                        messages = GmailImport.listInterviewMail(auth.token)
                        if (messages.isEmpty()) "No interview mail in the last 6 months." else null
                    }.getOrElse {
                        if (it is TokenExpiredException) {
                            GoogleAuth.invalidate(ctx, auth.token)
                            "Session expired — tap Reload."
                        } else "Couldn't load mail: ${it.message}"
                    }
                    loading = false
                }
                is GoogleAuth.AuthResult.Consent -> {
                    loading = false
                    retryAfterConsent = { loadMail(acct) }
                    consentLauncher.launch(auth.intent)
                }
                is GoogleAuth.AuthResult.Error -> {
                    loading = false
                    status = auth.msg
                }
            }
        }
    }

    fun openMessage(m: GmailMessage) {
        val act = activity ?: run { status = "Needs an Activity context."; return }
        val acct = account ?: return
        scope.launch {
            // Per-message loading flag handled by caller via status text.
            status = "Decoding “${m.subject.take(40)}”…"
            when (val auth = GoogleAuth.getToken(act, acct, GoogleAuth.GMAIL_READONLY)) {
                is GoogleAuth.AuthResult.Token -> {
                    val body = runCatching {
                        GmailImport.fetchBody(auth.token, m.id)
                    }.getOrElse {
                        if (it is TokenExpiredException) GoogleAuth.invalidate(ctx, auth.token)
                        ""
                    }
                    status = null
                    if (body.isNotBlank()) onBodyReady(body)
                    else status = "Couldn't decode that message."
                }
                is GoogleAuth.AuthResult.Consent -> {
                    status = null
                    retryAfterConsent = { openMessage(m) }
                    consentLauncher.launch(auth.intent)
                }
                is GoogleAuth.AuthResult.Error -> status = auth.msg
            }
        }
    }

    val accountPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val name = res.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!name.isNullOrBlank()) {
                account = name
                messages = emptyList()
                loadMail(name)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from Gmail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            if (account == null) {
                Text(
                    "Read-only access to find interview emails. Nothing is sent or modified.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { accountPicker.launch(GoogleAuth.pickAccountIntent()) }) {
                    Text("Choose Google account")
                }
            } else {
                Text(
                    "Account: $account", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { loadMail(account!!) }) { Text("Reload") }
                    OutlinedButton(onClick = {
                        account = null; messages = emptyList()
                    }) { Text("Switch account") }
                }
            }
            if (status != null) {
                Spacer(Modifier.height(8.dp))
                Text(status!!, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            if (loading) {
                CircularProgressIndicator()
            } else if (account != null && messages.isEmpty() && status == null) {
                EmptyState(
                    icon = Icons.Default.Mail, title = "No mail loaded",
                    subtitle = "Tap Reload to search for interview emails."
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { m ->
                        Card(
                            Modifier.fillMaxWidth().clickable { openMessage(m) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    m.subject.ifBlank { "(no subject)" },
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${m.from} · ${m.date}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (m.snippet.isNotBlank()) {
                                    Text(
                                        m.snippet.take(140),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }
}
