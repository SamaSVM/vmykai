package com.example.vmykai.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.example.vmykai.R
import com.example.vmykai.data.model.OutageSchedule
import com.example.vmykai.data.model.QueueId
import com.example.vmykai.data.model.TimeSlot
import com.example.vmykai.notifications.NextChangeCalculator
import com.example.vmykai.notifications.OutageStatusText
import com.example.vmykai.notifications.OutageWindow
import com.example.vmykai.ui.components.TimelineRibbon
import com.example.vmykai.ui.theme.AppShapes
import com.example.vmykai.ui.theme.Palette
import com.example.vmykai.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    queueId: QueueId,
    schedule: OutageSchedule?,
    isRefreshing: Boolean,
    syncFailed: Boolean,
    onRefresh: () -> Unit,
    onChangeQueue: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
        ) {
            MainHeader(queueId = queueId, onRefresh = onRefresh, onChangeQueue = onChangeQueue, onOpenSettings = onOpenSettings)
            Spacer(Modifier.height(Spacing.lg))

            if (syncFailed) {
                SyncFailedBanner(schedule = schedule)
                Spacer(Modifier.height(Spacing.lg))
            }

            val days = schedule?.daysFor(queueId).orEmpty()
            val today = days.getOrNull(0)

            var isPowerOn = true
            var outageWindow: OutageWindow? = null
            val now = Instant.now()

            if (schedule != null && today != null) {
                isPowerOn = NextChangeCalculator.isPowerOnAt(schedule, queueId, now) ?: true
                outageWindow = NextChangeCalculator.currentOrNextOutageWindow(schedule, queueId, now)
                StatusCard(isPowerOn = isPowerOn, outageWindow = outageWindow, now = now)
                Spacer(Modifier.height(Spacing.xl))
            }

            Text(text = stringResource(R.string.today_label), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            DayCard {
                if (today != null) {
                    val statusText = stringResource(
                        if (isPowerOn) R.string.status_power_on else R.string.status_power_off,
                    )
                    val detail = OutageStatusText.statusDetail(isPowerOn, outageWindow)
                    val ribbonDescription = if (detail != null) {
                        "$statusText. ${detail.label}: ${detail.value}."
                    } else {
                        statusText
                    }
                    TimelineRibbon(
                        slots = today.slots,
                        nowSlotIndex = currentSlotIndexIfToday(today.date),
                        contentDescription = ribbonDescription,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    EmptyDayState(text = stringResource(R.string.today_not_available_label))
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            Text(text = stringResource(R.string.tomorrow_label), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            DayCard {
                val tomorrow = days.getOrNull(1)
                if (tomorrow != null) {
                    TimelineRibbon(
                        slots = tomorrow.slots,
                        contentDescription = stringResource(R.string.tomorrow_label),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    EmptyDayState(text = stringResource(R.string.tomorrow_not_published))
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = if (schedule != null) {
                    stringResource(R.string.last_synced_label, formatTime(schedule))
                } else {
                    stringResource(R.string.last_synced_never)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MainHeader(
    queueId: QueueId,
    onRefresh: () -> Unit,
    onChangeQueue: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, AppShapes.IconBadge),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_bolt),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
            )
        }
        Spacer(Modifier.width(Spacing.sm + Spacing.xs))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.app_title), style = MaterialTheme.typography.titleLarge)
            val changeQueueDescription = stringResource(R.string.queue_label, queueId.display) + ", " +
                stringResource(R.string.change_queue_action)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onChangeQueue)
                    .clearAndSetSemantics {
                        contentDescription = changeQueueDescription
                        role = Role.Button
                    },
            ) {
                Text(
                    text = stringResource(R.string.queue_label, queueId.display),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(2.dp))
                Image(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_action))
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
        }
    }
}

@Composable
private fun SyncFailedBanner(schedule: OutageSchedule?) {
    Card(
        shape = AppShapes.StatusCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = if (schedule != null) {
                    stringResource(R.string.sync_failed_label, formatTime(schedule))
                } else {
                    stringResource(R.string.sync_failed_no_data_label)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun DayCard(content: @Composable () -> Unit) {
    Card(
        shape = AppShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) { content() }
    }
}

@Composable
private fun EmptyDayState(text: String) {
    Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private data class StatusPalette(val background: Color, val ink: Color, val ring: Color)

@Composable
private fun statusPalette(isPowerOn: Boolean): StatusPalette {
    return if (isPowerOn) {
        StatusPalette(
            background = MaterialTheme.colorScheme.secondaryContainer,
            ink = MaterialTheme.colorScheme.onSecondaryContainer,
            ring = ringTint(isPowerOn = true),
        )
    } else {
        StatusPalette(
            background = MaterialTheme.colorScheme.tertiaryContainer,
            ink = MaterialTheme.colorScheme.onTertiaryContainer,
            ring = ringTint(isPowerOn = false),
        )
    }
}

@Composable
private fun ringTint(isPowerOn: Boolean): Color {
    val isDark = isSystemInDarkTheme()
    return if (isPowerOn) {
        if (isDark) Palette.StatusOnRingNight else Palette.StatusOnRingDay
    } else {
        if (isDark) Palette.StatusOffRingNight else Palette.StatusOffRingDay
    }
}

@Composable
private fun StatusCard(isPowerOn: Boolean, outageWindow: OutageWindow?, now: Instant) {
    val palette = statusPalette(isPowerOn)
    val accentColor = if (isPowerOn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
    val icon = if (isPowerOn) R.drawable.ic_check else R.drawable.ic_power_off
    val detail = OutageStatusText.statusDetail(isPowerOn, outageWindow)
    val countdown = OutageStatusText.countdownText(isPowerOn, outageWindow, now)

    Card(
        shape = AppShapes.StatusCard,
        colors = CardDefaults.cardColors(containerColor = palette.background),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(Spacing.md + Spacing.xs),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(52.dp).background(palette.ring, AppShapes.Ring),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).background(accentColor, AppShapes.Ring),
                ) {
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(Color.White),
                    )
                }
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPowerOn) stringResource(R.string.status_power_on) else stringResource(R.string.status_power_off),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                )
                if (detail != null) {
                    Text(
                        text = detail.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = detail.value, style = MaterialTheme.typography.titleLarge)
                }
                if (countdown != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(palette.ring, AppShapes.Pill)
                            .padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.xs),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_clock),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            colorFilter = ColorFilter.tint(palette.ink),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(text = countdown, style = MaterialTheme.typography.labelMedium, color = palette.ink)
                    }
                }
            }
        }
    }
}

private fun currentSlotIndexIfToday(date: LocalDate): Int? {
    val now = ZonedDateTime.now(NextChangeCalculator.KYIV_ZONE)
    if (date != now.toLocalDate()) return null
    return (now.hour * 60 + now.minute) / TimeSlot.MINUTES_PER_SLOT
}

private fun formatTime(schedule: OutageSchedule): String {
    val zoned = schedule.lastSyncedAt.atZone(NextChangeCalculator.KYIV_ZONE)
    return DateTimeFormatter.ofPattern("HH:mm").format(zoned)
}
