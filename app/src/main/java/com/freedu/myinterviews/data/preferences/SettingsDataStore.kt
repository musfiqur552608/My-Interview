package com.freedu.myinterviews.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import com.freedu.myinterviews.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Shared DataStore delegate (used by DI and by background workers). */
val Context.appSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val reminderHoursBefore: Int = 1,
    val remindDayBefore: Boolean = true,
    val followUpDays: Int = 7,
    val thankYouReminder: Boolean = true,
    val dailySummary: Boolean = false,
    val appLock: Boolean = false,
    val profileId: String = "default",
    val onboarded: Boolean = false,
    val nextInterviewSummary: String = "",
    val weeklyGoal: Int = 5,
    val llmApiKey: String = ""
)

@Singleton
class SettingsDataStore @Inject constructor(
    private val store: DataStore<Preferences>
) {
    private object K {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC = booleanPreferencesKey("dynamic")
        val HOURS = intPreferencesKey("reminder_hours")
        val DAY_BEFORE = booleanPreferencesKey("day_before")
        val FOLLOW_UP = intPreferencesKey("follow_up_days")
        val THANK_YOU = booleanPreferencesKey("thank_you")
        val DAILY = booleanPreferencesKey("daily_summary")
        val LOCK = booleanPreferencesKey("app_lock")
        val PROFILE = stringPreferencesKey("profile")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NEXT = stringPreferencesKey("next_summary")
        val GOAL = intPreferencesKey("weekly_goal")
        val LLM = stringPreferencesKey("llm_key")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(p[K.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = p[K.DYNAMIC] ?: true,
            reminderHoursBefore = p[K.HOURS] ?: 1,
            remindDayBefore = p[K.DAY_BEFORE] ?: true,
            followUpDays = p[K.FOLLOW_UP] ?: 7,
            thankYouReminder = p[K.THANK_YOU] ?: true,
            dailySummary = p[K.DAILY] ?: false,
            appLock = p[K.LOCK] ?: false,
            profileId = p[K.PROFILE] ?: "default",
            onboarded = p[K.ONBOARDED] ?: false,
            nextInterviewSummary = p[K.NEXT] ?: "",
            weeklyGoal = p[K.GOAL] ?: 5,
            llmApiKey = p[K.LLM] ?: ""
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val cur = settingsOnce()
        val next = transform(cur)
        store.edit { e ->
            e[K.THEME] = next.themeMode.name
            e[K.DYNAMIC] = next.dynamicColor
            e[K.HOURS] = next.reminderHoursBefore
            e[K.DAY_BEFORE] = next.remindDayBefore
            e[K.FOLLOW_UP] = next.followUpDays
            e[K.THANK_YOU] = next.thankYouReminder
            e[K.DAILY] = next.dailySummary
            e[K.LOCK] = next.appLock
            e[K.PROFILE] = next.profileId
            e[K.ONBOARDED] = next.onboarded
            e[K.NEXT] = next.nextInterviewSummary
            e[K.GOAL] = next.weeklyGoal
            e[K.LLM] = next.llmApiKey
        }
    }

    private suspend fun settingsOnce(): AppSettings {
        // Suspend until first prefs emission; cheap (DataStore caches in memory).
        val p = store.data.first()
        return AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(p[K.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = p[K.DYNAMIC] ?: true,
            reminderHoursBefore = p[K.HOURS] ?: 1,
            remindDayBefore = p[K.DAY_BEFORE] ?: true,
            followUpDays = p[K.FOLLOW_UP] ?: 7,
            thankYouReminder = p[K.THANK_YOU] ?: true,
            dailySummary = p[K.DAILY] ?: false,
            appLock = p[K.LOCK] ?: false,
            profileId = p[K.PROFILE] ?: "default",
            onboarded = p[K.ONBOARDED] ?: false,
            nextInterviewSummary = p[K.NEXT] ?: "",
            weeklyGoal = p[K.GOAL] ?: 5,
            llmApiKey = p[K.LLM] ?: ""
        )
    }
}
