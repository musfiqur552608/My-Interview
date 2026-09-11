package com.freedu.myinterviews.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.freedu.myinterviews.MainActivity
import java.util.concurrent.TimeUnit

const val CHANNEL_REMINDERS = "interview_reminders"
const val CHANNEL_SUMMARY = "daily_summary"

/**
 * Fires a local notification for one interview round.
 * Plain [CoroutineWorker] (no Hilt injection) to keep the dependency graph small;
 * all data needed travels in input [Data]. Fully offline.
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Upcoming interview"
        val detail = inputData.getString(KEY_DETAIL).orEmpty()
        val roundId = inputData.getLong(KEY_ROUND_ID, -1)
        postNotification(
            applicationContext, CHANNEL_REMINDERS,
            id = ((roundId % Int.MAX_VALUE).toInt()) + 1000,
            title = title, text = detail
        )
        return Result.success()
    }

    companion object {
        const val KEY_ROUND_ID = "round_id"
        const val KEY_TITLE = "title"
        const val KEY_DETAIL = "detail"
    }
}

object ReminderScheduler {
    fun schedule(
        ctx: Context,
        roundId: Long,
        triggerAtMillis: Long,
        title: String,
        detail: String
    ) {
        val delay = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val data = Data.Builder()
            .putLong(ReminderWorker.KEY_ROUND_ID, roundId)
            .putString(ReminderWorker.KEY_TITLE, title)
            .putString(ReminderWorker.KEY_DETAIL, detail)
            .build()
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("round_$roundId")
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "reminder_${roundId}_$triggerAtMillis", ExistingWorkPolicy.KEEP, req
        )
    }

    fun cancelForRound(ctx: Context, roundId: Long) {
        runCatching { WorkManager.getInstance(ctx).cancelAllWorkByTag("round_$roundId") }
    }
}

fun ensureChannel(ctx: Context, id: String, name: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(id) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }
}

fun postNotification(ctx: Context, channelId: String, id: Int, title: String, text: String) {
    ensureChannel(ctx, channelId, channelId)
    val intent = Intent(ctx, MainActivity::class.java)
    val pi = PendingIntent.getActivity(
        ctx, id, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notif = NotificationCompat.Builder(ctx, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setContentIntent(pi)
        .setAutoCancel(true)
        .build()
    val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    runCatching { mgr.notify(id, notif) }
}
