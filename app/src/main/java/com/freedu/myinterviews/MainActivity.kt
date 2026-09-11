package com.freedu.myinterviews

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedu.myinterviews.domain.model.ThemeMode
import com.freedu.myinterviews.presentation.navigation.AppNavGraph
import com.freedu.myinterviews.presentation.navigation.AppShellViewModel
import com.freedu.myinterviews.presentation.navigation.Routes
import com.freedu.myinterviews.ui.theme.MyInterviewsTheme
import com.freedu.myinterviews.worker.FollowUpScheduler
import dagger.hilt.android.AndroidEntryPoint

/** FragmentActivity (not ComponentActivity) because the app-lock gate uses BiometricPrompt. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_DEST = "dest"
        const val DEST_QUICK_ADD = "quick_add"
        const val DEST_TODAY = "today"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FollowUpScheduler.scheduleDaily(this)
        val dest = intent?.getStringExtra(EXTRA_DEST)
        enableEdgeToEdge()
        setContent {
            val shell: AppShellViewModel = hiltViewModel()
            val settings by shell.settingsFlow.collectAsState()
            val mode = settings?.themeMode ?: ThemeMode.SYSTEM
            val dark = when (mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.FOCUS -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val startQuickAdd = remember(dest) { mutableStateOf(dest == DEST_QUICK_ADD) }
            MyInterviewsTheme(
                darkTheme = dark,
                dynamicColor = settings?.dynamicColor ?: true,
                amoled = mode == ThemeMode.FOCUS
            ) {
                AppNavGraph(
                    vm = shell,
                    startQuickAdd = startQuickAdd.value,
                    startRoute = if (dest == DEST_TODAY) Routes.TODAY else Routes.DASHBOARD
                )
            }
        }
    }
}
