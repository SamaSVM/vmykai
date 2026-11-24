package com.example.vmykai

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.vmykai.data.OutageScheduleRepository
import com.example.vmykai.data.local.NotificationSettings
import com.example.vmykai.data.local.UserPreferences
import com.example.vmykai.data.model.OutageSchedule
import com.example.vmykai.data.model.QueueId
import com.example.vmykai.data.remote.KiroeRemoteScheduleSource
import com.example.vmykai.sync.BatteryOptimization
import com.example.vmykai.ui.main.MainScreen
import com.example.vmykai.ui.onboarding.QueuePickerScreen
import com.example.vmykai.ui.settings.SettingsScreen
import com.example.vmykai.ui.theme.VmykaiTheme
import com.example.vmykai.widget.ExactAlarmPermission
import com.example.vmykai.widget.OutageWidgetUpdater
import com.example.vmykai.widget.WidgetTickScheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VmykaiTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    VmykaiApp()
                }
            }
        }
    }
}

private enum class Screen { Main, Settings, ChangeQueue }

@Composable
private fun VmykaiApp() {
    val context = LocalContext.current
    val preferences = remember { UserPreferences(context) }
    val repository = remember { OutageScheduleRepository(KiroeRemoteScheduleSource(context), preferences) }
    val scope = rememberCoroutineScope()

    var queueId by remember { mutableStateOf<QueueId?>(null) }
    var queueLoaded by remember { mutableStateOf(false) }
    var schedule by remember { mutableStateOf<OutageSchedule?>(null) }
    var notificationSettings by remember {
        mutableStateOf(NotificationSettings(enabled = true, leadMinutes = UserPreferences.DEFAULT_LEAD_MINUTES))
    }
    var isRefreshing by remember { mutableStateOf(false) }
    var syncFailed by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(Screen.Main) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(BatteryOptimization.isIgnoringBatteryOptimizations(context))
    }
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        isIgnoringBatteryOptimizations = BatteryOptimization.isIgnoringBatteryOptimizations(context)
    }

    var canScheduleExactAlarms by remember { mutableStateOf(ExactAlarmPermission.isGranted(context)) }
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        canScheduleExactAlarms = ExactAlarmPermission.isGranted(context)
        WidgetTickScheduler.syncTickState(context)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(Unit) { WidgetTickScheduler.syncTickState(context) }
    LaunchedEffect(Unit) { preferences.selectedQueue.collect { queueId = it; queueLoaded = true } }
    LaunchedEffect(Unit) { preferences.cachedSchedule.collect { schedule = it } }
    LaunchedEffect(Unit) { preferences.notificationSettings.collect { notificationSettings = it } }
    LaunchedEffect(Unit) {
        preferences.setUnseenScheduleChange(false)
        OutageWidgetUpdater.updateAll(context)
    }

    fun refresh() {
        scope.launch {
            isRefreshing = true
            syncFailed = false
            try {
                repository.refresh()
            } catch (e: Exception) {
                syncFailed = true
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(queueLoaded) {
        if (queueLoaded) refresh()
    }

    if (!queueLoaded) return

    val currentQueue = queueId

    val activeScreen = when {
        currentQueue == null || screen == Screen.ChangeQueue -> Screen.ChangeQueue
        screen == Screen.Settings -> Screen.Settings
        else -> Screen.Main
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Crossfade(targetState = activeScreen, label = "screen-crossfade") { target ->
            when (target) {
                Screen.ChangeQueue -> QueuePickerScreen(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    selectedQueueId = currentQueue,
                    onBack = if (currentQueue != null) ({ screen = Screen.Main }) else null,
                    onQueueSelected = { selected ->
                        scope.launch { preferences.setSelectedQueue(selected) }
                        screen = Screen.Main
                    },
                )
                Screen.Settings -> SettingsScreen(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    notificationSettings = notificationSettings,
                    currentQueue = currentQueue ?: return@Crossfade,
                    isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                    canScheduleExactAlarms = canScheduleExactAlarms,
                    onBack = { screen = Screen.Main },
                    onNotificationsEnabledChange = { scope.launch { preferences.setNotificationsEnabled(it) } },
                    onLeadMinutesChange = { scope.launch { preferences.setNotificationLeadMinutes(it) } },
                    onChangeQueue = { screen = Screen.ChangeQueue },
                    onRequestBatteryOptimizationExemption = {
                        batteryOptimizationLauncher.launch(BatteryOptimization.exemptionIntent(context))
                    },
                    onRequestExactAlarmPermission = {
                        exactAlarmLauncher.launch(ExactAlarmPermission.requestIntent(context))
                    },
                )
                Screen.Main -> MainScreen(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    queueId = currentQueue ?: return@Crossfade,
                    schedule = schedule,
                    isRefreshing = isRefreshing,
                    syncFailed = syncFailed,
                    onRefresh = ::refresh,
                    onChangeQueue = { screen = Screen.ChangeQueue },
                    onOpenSettings = { screen = Screen.Settings },
                )
            }
        }
    }
}
