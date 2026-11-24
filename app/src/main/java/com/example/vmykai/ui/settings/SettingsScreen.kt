package com.example.vmykai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.vmykai.BuildConfig
import com.example.vmykai.R
import com.example.vmykai.data.local.NotificationSettings
import com.example.vmykai.data.model.QueueId
import com.example.vmykai.ui.components.BackTitleRow
import com.example.vmykai.ui.theme.AppShapes
import com.example.vmykai.ui.theme.Spacing

private val LEAD_MINUTES_PRESETS = listOf(5, 15, 30, 60)

@Composable
fun SettingsScreen(
    notificationSettings: NotificationSettings,
    currentQueue: QueueId,
    onBack: () -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onLeadMinutesChange: (Int) -> Unit,
    onChangeQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
    ) {
        BackTitleRow(title = stringResource(R.string.settings_title), onBack = onBack)
        Spacer(Modifier.height(Spacing.xl))

        SectionLabel(stringResource(R.string.settings_section_notifications))
        Spacer(Modifier.height(Spacing.sm))
        Card(shape = AppShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            ) {
                Text(text = stringResource(R.string.settings_notifications_enabled), modifier = Modifier.weight(1f))
                Switch(
                    checked = notificationSettings.enabled,
                    onCheckedChange = onNotificationsEnabledChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.secondary),
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Card(shape = AppShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
                Text(text = stringResource(R.string.settings_notification_lead_time))
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    LEAD_MINUTES_PRESETS.forEach { minutes ->
                        val isSelected = minutes == notificationSettings.leadMinutes
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLeadMinutesChange(minutes) },
                            label = { Text(stringResource(R.string.settings_notification_lead_time_chip, minutes)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        SectionLabel(stringResource(R.string.settings_section_my_queue))
        Spacer(Modifier.height(Spacing.sm))
        Card(shape = AppShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            ) {
                Text(
                    text = stringResource(R.string.queue_label, currentQueue.display),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(
                    onClick = onChangeQueue,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Text(stringResource(R.string.change_queue_action))
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        SectionLabel(stringResource(R.string.settings_section_about))
        Spacer(Modifier.height(Spacing.sm))
        Card(shape = AppShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.app_title), modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.settings_version_label, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    text = stringResource(R.string.settings_theme_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
