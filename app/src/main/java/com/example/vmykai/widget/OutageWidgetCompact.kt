package com.example.vmykai.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.vmykai.MainActivity
import com.example.vmykai.R
import com.example.vmykai.data.local.UserPreferences
import com.example.vmykai.data.model.OutageSchedule
import com.example.vmykai.data.model.QueueId
import com.example.vmykai.notifications.NextChangeCalculator
import com.example.vmykai.notifications.OutageStatusText
import com.example.vmykai.notifications.OutageWindow
import java.time.Instant
import kotlinx.coroutines.flow.first

/** Compact 4x1 widget: status icon, status text, countdown pill, a mini timeline, then a trailing chevron. */
class OutageWidgetCompact : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferences = UserPreferences(context)
        val queueId = preferences.selectedQueue.first()
        val schedule = preferences.cachedSchedule.first()

        provideContent {
            OutageWidgetCompactContent(queueId = queueId, schedule = schedule)
        }
    }
}

class OutageWidgetCompactReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OutageWidgetCompact()
}

/** Full content width, same 10dp-per-side padding convention as the full (4x2) widget. */
private val COMPACT_CONTENT_WIDTH = WIDGET_WIDTH - 20.dp

@Composable
private fun OutageWidgetCompactContent(queueId: QueueId?, schedule: OutageSchedule?) {
    val context = LocalContext.current

    Column(
        verticalAlignment = Alignment.Vertical.CenterVertically,
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WIDGET_BG)
            .cornerRadius(18.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        if (queueId != null && schedule != null) {
            val today = schedule.daysFor(queueId).getOrNull(0)
            if (today != null) {
                val now = Instant.now()
                val isPowerOn = NextChangeCalculator.isPowerOnAt(schedule, queueId, now) ?: true
                val outageWindow = NextChangeCalculator.currentOrNextOutageWindow(schedule, queueId, now)
                val countdown = OutageStatusText.countdownText(isPowerOn, outageWindow, now)

                Row(verticalAlignment = Alignment.Vertical.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                    CompactStatusIcon(isPowerOn)
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    CompactStatus(isPowerOn, outageWindow, modifier = GlanceModifier.defaultWeight())
                    if (countdown != null) {
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        CompactCountdownPill(isPowerOn, countdown)
                    }
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Image(
                        provider = ImageProvider(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        modifier = GlanceModifier.size(14.dp),
                        colorFilter = ColorFilter.tint(WIDGET_SECONDARY),
                    )
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
                Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(DIVIDER_COLOR)) {}
                Spacer(modifier = GlanceModifier.height(2.dp))
                HourRibbon(today.slots, nowSlotIndexIfToday(today.date), COMPACT_RIBBON_STYLE, COMPACT_CONTENT_WIDTH)
            } else {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically, modifier = GlanceModifier.fillMaxSize()) {
                    IconBadge(size = 28.dp, iconSize = 16.dp, cornerRadius = 10.dp)
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "Немає даних",
                        style = TextStyle(color = WIDGET_TEXT),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically, modifier = GlanceModifier.fillMaxSize()) {
                IconBadge(size = 28.dp, iconSize = 16.dp, cornerRadius = 10.dp)
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = queueId?.let { "Черга ${it.display}" } ?: "Черга не обрана",
                    style = TextStyle(fontWeight = FontWeight.Bold, color = WIDGET_TEXT),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }
}

@Composable
private fun CompactStatusIcon(isPowerOn: Boolean) {
    val ringColor = if (isPowerOn) STATUS_ON_RING else STATUS_OFF_RING
    val accentColor = if (isPowerOn) POWER_ON_COLOR else POWER_OFF_COLOR
    val icon = if (isPowerOn) R.drawable.ic_check else R.drawable.ic_power_off
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.size(28.dp).background(ringColor).cornerRadius(14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .size(22.dp)
                .background(ColorProvider(day = accentColor, night = accentColor))
                .cornerRadius(11.dp),
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = null,
                modifier = GlanceModifier.size(12.dp),
                colorFilter = ColorFilter.tint(STATUS_ICON_GLYPH_COLOR),
            )
        }
    }
}

@Composable
private fun CompactCountdownPill(isPowerOn: Boolean, countdown: String) {
    val ringColor = if (isPowerOn) STATUS_ON_RING else STATUS_OFF_RING
    val inkColor = if (isPowerOn) STATUS_ON_INK else STATUS_OFF_INK
    Row(
        verticalAlignment = Alignment.Vertical.CenterVertically,
        modifier = GlanceModifier
            .background(ringColor)
            .cornerRadius(8.dp)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_clock),
            contentDescription = null,
            modifier = GlanceModifier.size(10.dp),
            colorFilter = ColorFilter.tint(inkColor),
        )
        Spacer(modifier = GlanceModifier.width(3.dp))
        Text(text = countdown, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = inkColor))
    }
}

@Composable
private fun CompactStatus(isPowerOn: Boolean, outageWindow: OutageWindow?, modifier: GlanceModifier) {
    val inkColor = if (isPowerOn) STATUS_ON_INK else STATUS_OFF_INK
    Column(modifier = modifier) {
        Text(
            text = if (isPowerOn) "Зараз світло є" else "Зараз світла немає",
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = inkColor),
            maxLines = 1,
        )
        Text(
            text = compactStatusValue(isPowerOn, outageWindow),
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WIDGET_TEXT),
            maxLines = 1,
        )
    }
}

private fun compactStatusValue(isPowerOn: Boolean, window: OutageWindow?): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(NextChangeCalculator.KYIV_ZONE)
    if (window == null) return if (isPowerOn) "—" else "невідомо"
    val end = window.end?.let { formatter.format(it) }
    return if (isPowerOn) {
        val start = formatter.format(window.start)
        if (end != null) "$start–$end" else start
    } else {
        "до ${end ?: "невідомо"}"
    }
}
