package com.example.vmykai.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.vmykai.data.model.OutageSchedule
import com.example.vmykai.data.model.QueueId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vmykai_prefs")

data class NotificationSettings(val enabled: Boolean, val leadMinutes: Int)

class UserPreferences(private val context: Context) {

    val selectedQueue: Flow<QueueId?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_QUEUE]?.let { QueueId.parse(it) }
    }

    suspend fun setSelectedQueue(queueId: QueueId) {
        context.dataStore.edit { it[SELECTED_QUEUE] = queueId.display }
    }

    val notificationSettings: Flow<NotificationSettings> = context.dataStore.data.map { prefs ->
        NotificationSettings(
            enabled = prefs[NOTIFICATIONS_ENABLED] ?: true,
            leadMinutes = prefs[NOTIFICATION_LEAD_MINUTES] ?: DEFAULT_LEAD_MINUTES,
        )
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setNotificationLeadMinutes(minutes: Int) {
        context.dataStore.edit { it[NOTIFICATION_LEAD_MINUTES] = minutes }
    }

    val cachedSchedule: Flow<OutageSchedule?> = context.dataStore.data.map { prefs ->
        prefs[CACHED_SCHEDULE_JSON]?.let { CachedScheduleSerializer.fromJson(it) }
    }

    suspend fun setCachedSchedule(schedule: OutageSchedule) {
        context.dataStore.edit { it[CACHED_SCHEDULE_JSON] = CachedScheduleSerializer.toJson(schedule) }
    }

    /** True when the schedule changed on the last sync and the user hasn't opened the app since. */
    val hasUnseenScheduleChange: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[UNSEEN_SCHEDULE_CHANGE] ?: false
    }

    suspend fun setUnseenScheduleChange(value: Boolean) {
        context.dataStore.edit { it[UNSEEN_SCHEDULE_CHANGE] = value }
    }

    companion object {
        const val DEFAULT_LEAD_MINUTES = 15

        private val SELECTED_QUEUE = stringPreferencesKey("selected_queue")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val NOTIFICATION_LEAD_MINUTES = intPreferencesKey("notification_lead_minutes")
        private val CACHED_SCHEDULE_JSON = stringPreferencesKey("cached_schedule_json")
        private val UNSEEN_SCHEDULE_CHANGE = booleanPreferencesKey("unseen_schedule_change")
    }
}
