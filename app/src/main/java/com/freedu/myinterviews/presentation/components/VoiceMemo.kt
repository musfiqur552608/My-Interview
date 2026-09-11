package com.freedu.myinterviews.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Voice-memo recorder for post-interview reflections (MediaRecorder → app-private
 * filesDir, MediaPlayer playback). The saved absolute path travels in
 * [InterviewRound.voiceMemoUri]; old files are deleted when replaced/cleared.
 */
@Composable
fun VoiceMemoSection(
    existingUri: String,
    onRecorded: (String) -> Unit,
    onCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentPath by remember(existingUri) { mutableStateOf(existingUri) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) error = "Microphone permission denied."
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.stop() }
            runCatching { recorder?.release() }
            runCatching { player?.stop() }
            runCatching { player?.release() }
        }
    }

    fun startRecording() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        runCatching {
            val dir = File(ctx.filesDir, "voice").apply { mkdirs() }
            val file = File(dir, "memo_${System.currentTimeMillis()}.m4a")
            // Drop previous memo file to avoid orphans.
            if (currentPath.isNotBlank()) runCatching { File(currentPath).delete() }
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(ctx)
            } else {
                @Suppress("DEPRECATION") MediaRecorder()
            }
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            currentPath = file.absolutePath
            recording = true
            error = null
        }.onFailure { error = "Could not start recording." }
    }

    fun stopRecording() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        recording = false
        if (currentPath.isNotBlank()) onRecorded(currentPath)
    }

    fun togglePlay() {
        if (playing) {
            runCatching { player?.stop(); player?.release() }
            player = null
            playing = false
            return
        }
        if (currentPath.isBlank()) return
        runCatching {
            val mp = MediaPlayer().apply {
                setDataSource(currentPath)
                prepare()
                setOnCompletionListener { playing = false }
                start()
            }
            player = mp
            playing = true
            error = null
        }.onFailure { error = "Could not play memo." }
    }

    Column(modifier.fillMaxWidth()) {
        Text("Voice memo", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (recording) {
                Button(onClick = ::stopRecording) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp)); Text("Stop")
                }
                Spacer(Modifier.width(8.dp))
                Text("● Recording…", color = MaterialTheme.colorScheme.error)
            } else {
                OutlinedButton(onClick = ::startRecording) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (currentPath.isBlank()) "Record" else "Re-record")
                }
                if (currentPath.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = ::togglePlay) {
                        Icon(
                            if (playing) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Stop playback" else "Play memo"
                        )
                    }
                    IconButton(onClick = {
                        runCatching { File(currentPath).delete() }
                        currentPath = ""
                        onCleared()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete memo")
                    }
                }
            }
        }
        if (error != null) {
            Text(error!!, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }
    }
}
