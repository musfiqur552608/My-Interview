package com.freedu.myinterviews.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.freedu.myinterviews.MainActivity
import com.freedu.myinterviews.R
import com.freedu.myinterviews.util.DateUtils

/**
 * Home-screen widget showing the next upcoming interview + live countdown.
 * Refreshed whenever rounds change (see CompanyViewModel.saveRound) and every
 * 30 min by the system (updatePeriodMillis) so the countdown stays fresh.
 */
class NextInterviewWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val summary = prefs.getString(KEY_NEXT, "No upcoming interviews")!!
        val nextAt = prefs.getLong(KEY_NEXT_AT, 0)
        val countdown = if (nextAt > System.currentTimeMillis()) {
            DateUtils.relativeDay(nextAt)
        } else ""
        ids.forEach { id ->
            val views = RemoteViews(ctx.packageName, R.layout.widget_next_interview)
            views.setTextViewText(R.id.widget_title, "Next interview")
            views.setTextViewText(R.id.widget_summary, summary)
            views.setTextViewText(R.id.widget_countdown, countdown)
            views.setOnClickPendingIntent(R.id.widget_root, contentIntent(ctx, 0, null))
            mgr.updateAppWidget(id, views)
        }
    }

    companion object {
        const val PREFS = "widget_prefs"
        const val KEY_NEXT = "next_summary"
        const val KEY_NEXT_AT = "next_at"

        fun refreshWidget(ctx: Context, summary: String, nextAt: Long = 0) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_NEXT, summary)
                .putLong(KEY_NEXT_AT, nextAt)
                .apply()
            val mgr = AppWidgetManager.getInstance(ctx)
            val cn = android.content.ComponentName(ctx, NextInterviewWidget::class.java)
            NextInterviewWidget().onUpdate(ctx, mgr, mgr.getAppWidgetIds(cn))
        }
    }
}

/** Deep-link pending intent into MainActivity (optional destination extra). */
fun contentIntent(ctx: Context, requestCode: Int, dest: String?): PendingIntent {
    val intent = Intent(ctx, MainActivity::class.java).apply {
        if (dest != null) putExtra(MainActivity.EXTRA_DEST, dest)
    }
    return PendingIntent.getActivity(
        ctx, requestCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/**
 * Quick-log widget: one-tap quick-add + jump straight to Interview-day mode.
 */
class QuickLogWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(ctx.packageName, R.layout.widget_quick_log)
            views.setOnClickPendingIntent(
                R.id.widget_btn_add, contentIntent(ctx, 11, MainActivity.DEST_QUICK_ADD)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_today, contentIntent(ctx, 12, MainActivity.DEST_TODAY)
            )
            mgr.updateAppWidget(id, views)
        }
    }
}
