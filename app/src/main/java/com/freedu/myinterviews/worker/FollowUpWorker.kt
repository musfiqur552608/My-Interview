package com.freedu.myinterviews.worker

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.freedu.myinterviews.data.local.TrackerDatabase
import com.freedu.myinterviews.data.preferences.SettingsDataStore
import com.freedu.myinterviews.data.preferences.appSettingsStore
import com.freedu.myinterviews.data.repository.TrackerRepositoryImpl
import com.freedu.myinterviews.util.Insights
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Daily "needs follow-up" detector. Builds its own Room instance (same file —
 * Room tolerates multiple instances; closed after each run) so no DI is needed
 * inside the worker process path. Fully offline.
 */
class FollowUpWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val db = Room.databaseBuilder(
            applicationContext, TrackerDatabase::class.java, "tracker.db"
        ).fallbackToDestructiveMigration().build()
        try {
            val repo = TrackerRepositoryImpl(db)
            val settings = SettingsDataStore(applicationContext.appSettingsStore).settings.first()
            val apps = repo.observeApplications(settings.profileId).first()
            val rounds = repo.observeAllRounds().first()
            val stale = Insights.staleApplications(apps, rounds, settings.followUpDays)
            if (stale.isNotEmpty()) {
                val names = stale.take(3).joinToString(", ") {
                    "${it.companyName} (${it.jobTitle})"
                }
                postNotification(
                    applicationContext, CHANNEL_REMINDERS, 4242,
                    title = if (stale.size == 1) "1 application needs a follow-up"
                    else "${stale.size} applications need a follow-up",
                    text = "$names — consider a polite nudge email."
                )
            }
            return Result.success()
        } catch (_: Exception) {
            return Result.retry()
        } finally {
            runCatching { db.close() }
        }
    }
}

object FollowUpScheduler {
    /** Idempotent (KEEP) — safe to call from MainActivity.onCreate. */
    fun scheduleDaily(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<FollowUpWorker>(24, TimeUnit.HOURS)
            .addTag("followup_daily")
            .build()
        runCatching {
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "followup_daily", ExistingPeriodicWorkPolicy.KEEP, req
            )
        }
    }
}
