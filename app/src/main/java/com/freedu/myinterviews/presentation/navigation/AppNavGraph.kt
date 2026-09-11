package com.freedu.myinterviews.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.freedu.myinterviews.data.preferences.SettingsDataStore
import com.freedu.myinterviews.presentation.calendar.CalendarScreen
import com.freedu.myinterviews.presentation.coach.CoachScreen
import com.freedu.myinterviews.presentation.company.ApplicationDetailScreen
import com.freedu.myinterviews.presentation.company.CompanyDetailScreen
import com.freedu.myinterviews.presentation.components.QuickAddSheet
import com.freedu.myinterviews.presentation.contacts.ContactsScreen
import com.freedu.myinterviews.presentation.dashboard.DashboardScreen
import com.freedu.myinterviews.presentation.offers.OfferCompareScreen
import com.freedu.myinterviews.presentation.onboarding.OnboardingScreen
import com.freedu.myinterviews.presentation.pipeline.PipelineScreen
import com.freedu.myinterviews.presentation.prep.PrepHubScreen
import com.freedu.myinterviews.presentation.search.SearchScreen
import com.freedu.myinterviews.presentation.settings.SettingsScreen
import com.freedu.myinterviews.presentation.simport.GmailScreen
import com.freedu.myinterviews.presentation.simport.SmartImportScreen
import com.freedu.myinterviews.presentation.today.TodayScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
    Tab(Routes.PIPELINE, "Pipeline", Icons.Default.ViewKanban),
    Tab(Routes.CALENDAR, "Calendar", Icons.Default.CalendarMonth),
    Tab(Routes.CONTACTS, "Contacts", Icons.Default.Person),
    Tab(Routes.SETTINGS, "Settings", Icons.Default.Settings)
)

@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val settings: SettingsDataStore
) : ViewModel() {
    val settingsFlow = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setOnboarded() {
        viewModelScope.launch { settings.update { it.copy(onboarded = true) } }
    }
}

@Composable
fun AppNavGraph(
    vm: AppShellViewModel = hiltViewModel(),
    startQuickAdd: Boolean = false,
    startRoute: String = Routes.DASHBOARD
) {
    val settings by vm.settingsFlow.collectAsState()
    val s = settings
    if (s == null) return // splash: DataStore loading
    if (!s.onboarded) {
        OnboardingScreen(onDone = { vm.setOnboarded() })
        return
    }
    if (s.appLock) {
        var unlocked by remember { mutableStateOf(false) }
        if (!unlocked) {
            LockScreen(onUnlocked = { unlocked = true })
            return
        }
    }
    RequestNotificationPermissionOnce()
    MainScaffold(startQuickAdd = startQuickAdd, startRoute = startRoute)
}

@Composable
private fun RequestNotificationPermissionOnce() {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var asked by remember { mutableStateOf(false) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!asked && androidx.core.content.ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            asked = true
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun LockScreen(onUnlocked: () -> Unit) {
    var failed by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(96.dp))
        Icon(Icons.Default.Lock, contentDescription = null)
        Spacer(Modifier.height(16.dp))
        Text("Interview Tracker is locked", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(if (failed) "Authentication failed — try again."
        else "Authenticate with biometrics or device credentials to continue.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        val ctx = androidx.compose.ui.platform.LocalContext.current
        Button(onClick = {
            val activity = ctx as? FragmentActivity
            if (activity == null) { onUnlocked(); return@Button }
            showBiometric(activity,
                onSuccess = onUnlocked,
                onFailure = { failed = true })
        }) { Text("Unlock") }
    }
}

private fun showBiometric(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
    val prompt = androidx.biometric.BiometricPrompt(
        activity, executor,
        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationFailed() { onFailure() }
            override fun onAuthenticationError(code: Int, msg: CharSequence) { onFailure() }
        }
    )
    val info = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Interview Tracker")
        .setSubtitle("Use biometrics or device credentials")
        .setAllowedAuthenticators(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()
    runCatching { prompt.authenticate(info) }.onFailure { onSuccess() } // no hardware → skip
}

@Composable
private fun MainScaffold(startQuickAdd: Boolean = false, startRoute: String = Routes.DASHBOARD) {
    val nav = rememberNavController()
    var quickAdd by remember { mutableStateOf(startQuickAdd) }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = TABS.any { it.route == current } ||
        current == Routes.PREP || current == Routes.OFFERS ||
        current == Routes.TODAY || current == Routes.SMART_IMPORT

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = startRoute,
            modifier = Modifier.padding(pad),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenApplication = { nav.navigate(Routes.application(it)) },
                    onQuickAdd = { quickAdd = true },
                    onOpenSearch = { nav.navigate("search") },
                    onOpenPrep = { nav.navigate(Routes.PREP) },
                    onOpenOffers = { nav.navigate(Routes.OFFERS) },
                    onOpenToday = { nav.navigate(Routes.TODAY) },
                    onOpenImport = { nav.navigate(Routes.SMART_IMPORT) }
                )
            }
            composable(Routes.PIPELINE) {
                PipelineScreen(
                    onOpenApplication = { nav.navigate(Routes.application(it)) },
                    onQuickAdd = { quickAdd = true }
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(onOpenApplication = { nav.navigate(Routes.application(it)) })
            }
            composable(Routes.CONTACTS) { ContactsScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.PREP) { PrepHubScreen() }
            composable(Routes.OFFERS) { OfferCompareScreen() }
            composable(Routes.TODAY) {
                TodayScreen(onOpenApplication = { nav.navigate(Routes.application(it)) })
            }
            composable(Routes.SMART_IMPORT) {
                SmartImportScreen(
                    onOpenApplication = { nav.navigate(Routes.application(it)) },
                    onOpenGmail = { nav.navigate(Routes.GMAIL) }
                )
            }
            composable(Routes.GMAIL) {
                GmailScreen(
                    onBack = { nav.popBackStack() },
                    onBodyReady = { body ->
                        nav.previousBackStackEntry
                            ?.savedStateHandle?.set("import_text", body)
                        nav.popBackStack()
                    }
                )
            }
            composable(
                Routes.COACH,
                arguments = listOf(navArgument("appId") { type = NavType.LongType })
            ) {
                CoachScreen(onBack = { nav.popBackStack() })
            }
            composable("search") {
                SearchScreen(
                    onBack = { nav.popBackStack() },
                    onOpenCompany = { nav.navigate(Routes.company(it)) },
                    onOpenApplication = { nav.navigate(Routes.application(it)) }
                )
            }
            composable(
                Routes.COMPANY_DETAIL,
                arguments = listOf(navArgument("companyId") { type = NavType.LongType })
            ) {
                CompanyDetailScreen(
                    onBack = { nav.popBackStack() },
                    onOpenApplication = { nav.navigate(Routes.application(it)) }
                )
            }
            composable(
                Routes.APPLICATION_DETAIL,
                arguments = listOf(navArgument("appId") { type = NavType.LongType })
            ) {
                ApplicationDetailScreen(
                    onBack = { nav.popBackStack() },
                    onOpenCoach = { nav.navigate(Routes.coach(it)) }
                )
            }
        }
    }
    if (quickAdd) {
        QuickAddSheet(
            onDismiss = { quickAdd = false },
            onOpenApplication = { /* detail opened from lists instead */ }
        )
    }
}
