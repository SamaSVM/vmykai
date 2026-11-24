package com.example.vmykai.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
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
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import com.example.vmykai.data.model.TimeSlot
import com.example.vmykai.notifications.NextChangeCalculator
import com.example.vmykai.notifications.OutageStatusText
import com.example.vmykai.notifications.OutageWindow
import com.example.vmykai.ui.theme.Palette
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

/** Shared sizing/colors for both the full (4x2) and compact (4x1) widgets - they're separate fixed-size providers. */
val WIDGET_WIDTH = 370.dp
private val FULL_CONTENT_WIDTH = WIDGET_WIDTH - 20.dp
val FULL_HEIGHT = 190.dp
val COMPACT_HEIGHT = 70.dp
const val HOURS_PER_DAY = 24

// Colors come from Palette - the single shared source also used by the Compose theme and ribbon.
val WIDGET_BG = ColorProvider(day = Palette.BgDay, night = Palette.BgNight)
val SURFACE_2 = ColorProvider(day = Palette.SurfaceDay, night = Palette.SurfaceNight)
val WIDGET_TEXT = ColorProvider(day = Palette.TextDay, night = Palette.TextNight)
val WIDGET_SECONDARY = ColorProvider(day = Palette.SecondaryDay, night = Palette.SecondaryNight)
val DIVIDER_COLOR = ColorProvider(day = Palette.DividerDay, night = Palette.DividerNight)
val ICON_BG = ColorProvider(day = Palette.IconBgDay, night = Palette.IconBgNight)
val ICON_COLOR = ColorProvider(day = Palette.IconColor, night = Palette.IconColor)

val POWER_ON_COLOR = Palette.PowerOn
val POWER_OFF_COLOR = Palette.PowerOff
val STATUS_ON_BG = ColorProvider(day = Palette.StatusOnBgDay, night = Palette.StatusOnBgNight)
val STATUS_OFF_BG = ColorProvider(day = Palette.StatusOffBgDay, night = Palette.StatusOffBgNight)
val STATUS_ON_INK = ColorProvider(day = Palette.StatusOnInkDay, night = Palette.StatusOnInkNight)
val STATUS_OFF_INK = ColorProvider(day = Palette.StatusOffInkDay, night = Palette.StatusOffInkNight)
val STATUS_ON_RING = ColorProvider(day = Palette.StatusOnRingDay, night = Palette.StatusOnRingNight)
val STATUS_OFF_RING = ColorProvider(day = Palette.StatusOffRingDay, night = Palette.StatusOffRingNight)
val UNSEEN_DOT_COLOR = ColorProvider(day = Palette.UnseenDotDay, night = Palette.UnseenDotNight)
val STATUS_ICON_GLYPH_COLOR = ColorProvider(day = Color.White, night = Color.White)

class OutageWidgetFull : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferences = UserPreferences(context)
        val queueId = preferences.selectedQueue.first()
        val schedule = preferences.cachedSchedule.first()
        val hasUnseenChange = preferences.hasUnseenScheduleChange.first()

        provideContent {
            OutageWidgetFullContent(queueId = queueId, schedule = schedule, hasUnseenChange = hasUnseenChange)
        }
    }
}

class OutageWidgetFullReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OutageWidgetFull()
}

@Composable
private fun OutageWidgetFullContent(queueId: QueueId?, schedule: OutageSchedule?, hasUnseenChange: Boolean) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WIDGET_BG)
            .cornerRadius(20.dp)
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 6.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        WidgetHeader(queueId)
        Spacer(modifier = GlanceModifier.height(2.dp))

        when {
            queueId == null -> Text(text = "Оберіть чергу в застосунку", style = TextStyle(color = WIDGET_TEXT))
            schedule == null -> Text(text = "Немає даних", style = TextStyle(color = WIDGET_TEXT))
            else -> {
                val today = schedule.daysFor(queueId).getOrNull(0)
                if (today == null) {
                    Text(text = "Немає даних на сьогодні", style = TextStyle(color = WIDGET_TEXT))
                } else {
                    val now = Instant.now()
                    val isPowerOn = NextChangeCalculator.isPowerOnAt(schedule, queueId, now) ?: true
                    val outageWindow = NextChangeCalculator.currentOrNextOutageWindow(schedule, queueId, now)

                    StatusCard(isPowerOn = isPowerOn, outageWindow = outageWindow, now = now)
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    HourRibbon(today.slots, nowSlotIndexIfToday(today.date), FULL_RIBBON_STYLE, FULL_CONTENT_WIDTH)
                }

                Spacer(modifier = GlanceModifier.height(1.dp))
                Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(DIVIDER_COLOR)) {}
                Spacer(modifier = GlanceModifier.height(2.dp))
                LegendFooterRow(schedule, hasUnseenChange)
            }
        }
    }
}

@Composable
internal fun WidgetHeader(queueId: QueueId?) {
    Row(verticalAlignment = Alignment.Vertical.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        IconBadge(size = 32.dp, iconSize = 18.dp, cornerRadius = 11.dp)
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "Графік відключень",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WIDGET_TEXT),
            )
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = queueId?.let { "Черга ${it.display}" } ?: "Черга не обрана",
                    style = TextStyle(fontSize = 11.sp, color = WIDGET_SECONDARY),
                )
                Spacer(modifier = GlanceModifier.width(2.dp))
                Image(
                    provider = ImageProvider(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    modifier = GlanceModifier.size(12.dp),
                    colorFilter = ColorFilter.tint(WIDGET_SECONDARY),
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .size(26.dp)
                .background(SURFACE_2)
                .cornerRadius(13.dp)
                .clickable(actionRunCallback<RefreshScheduleAction>()),
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Оновити",
                modifier = GlanceModifier.size(14.dp),
                colorFilter = ColorFilter.tint(WIDGET_SECONDARY),
            )
        }
    }
}

@Composable
internal fun IconBadge(size: Dp, iconSize: Dp, cornerRadius: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(size)
            .background(ICON_BG)
            .cornerRadius(cornerRadius),
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_bolt),
            contentDescription = null,
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(ICON_COLOR),
        )
    }
}

@Composable
private fun StatusCard(isPowerOn: Boolean, outageWindow: OutageWindow?, now: Instant) {
    val backgroundColor = if (isPowerOn) STATUS_ON_BG else STATUS_OFF_BG
    val inkColor = if (isPowerOn) STATUS_ON_INK else STATUS_OFF_INK
    val ringColor = if (isPowerOn) STATUS_ON_RING else STATUS_OFF_RING
    val accentColor = if (isPowerOn) POWER_ON_COLOR else POWER_OFF_COLOR
    val icon = if (isPowerOn) R.drawable.ic_check else R.drawable.ic_power_off
    val detail = OutageStatusText.statusDetail(isPowerOn, outageWindow)
    val countdown = OutageStatusText.countdownText(isPowerOn, outageWindow, now)

    Row(
        verticalAlignment = Alignment.Vertical.CenterVertically,
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .padding(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier.size(40.dp).background(ringColor).cornerRadius(20.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(ColorProvider(day = accentColor, night = accentColor))
                    .cornerRadius(16.dp),
            ) {
                Image(
                    provider = ImageProvider(icon),
                    contentDescription = null,
                    modifier = GlanceModifier.size(16.dp),
                    colorFilter = ColorFilter.tint(STATUS_ICON_GLYPH_COLOR),
                )
            }
        }
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = if (isPowerOn) "Зараз світло є" else "Зараз світла немає",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = inkColor),
            )
            if (detail != null) {
                Text(
                    text = "${detail.label} ${detail.value}",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WIDGET_TEXT),
                    maxLines = 1,
                )
            }
            if (countdown != null) {
                Row(
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    modifier = GlanceModifier
                        .background(ringColor)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
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
        }
    }
}

@Composable
private fun LegendFooterRow(schedule: OutageSchedule, hasUnseenChange: Boolean) {
    Row(verticalAlignment = Alignment.Vertical.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        Image(
            provider = ImageProvider(R.drawable.ic_clock),
            contentDescription = null,
            modifier = GlanceModifier.size(10.dp),
            colorFilter = ColorFilter.tint(WIDGET_SECONDARY),
        )
        Spacer(modifier = GlanceModifier.width(3.dp))
        Text(text = "Оновлено: ${formatTime(schedule)}", style = TextStyle(fontSize = 10.sp, color = WIDGET_SECONDARY))
        if (hasUnseenChange) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            Box(
                modifier = GlanceModifier
                    .size(6.dp)
                    .background(UNSEEN_DOT_COLOR)
                    .cornerRadius(3.dp),
            ) {}
        }
    }
}

/**
 * Visual knobs for [HourRibbon] - the full (4x2) and compact (4x1) widgets use different presets.
 * The "now" marker is a short connector line below the track leading down to a plain time label -
 * intentionally not a floating pill above the track, which used to add more height than the track
 * itself and caused the full widget's hour-label row to clip.
 */
internal data class RibbonStyle(
    val trackHeight: Dp,
    val showHourLabels: Boolean,
    val hourMarks: List<Int>,
    val hourLabelTextSp: Float,
    val markerLineHeight: Dp,
    val markerTextSp: Float,
)

internal val FULL_RIBBON_STYLE = RibbonStyle(
    trackHeight = 11.dp,
    showHourLabels = true,
    hourMarks = listOf(0, 6, 12, 18, 24),
    hourLabelTextSp = 9.5f,
    markerLineHeight = 4.dp,
    markerTextSp = 9f,
)

internal val COMPACT_RIBBON_STYLE = RibbonStyle(
    trackHeight = 10.dp,
    showHourLabels = true,
    hourMarks = listOf(0, 6, 12, 18, 24),
    hourLabelTextSp = 9f,
    markerLineHeight = 4.dp,
    markerTextSp = 9f,
)

/**
 * Glance/RemoteViews silently caps how many sibling views a single Row/Column can hold, so a row of
 * 48 weighted Spacers (one per half-hour slot) only ever rendered its first child, which then
 * expanded to fill the whole width via defaultWeight - showing as one solid color. Rendering the
 * ribbon as a single bitmap sidesteps that limit entirely, and also lets us draw the rounded-pill
 * track, the dimmed "already passed" overlay and the floating "now" marker (label + line + dot)
 * that Glance's layout primitives can't express on their own.
 */
@Composable
internal fun HourRibbon(slots: List<TimeSlot>, nowSlotIndex: Int?, style: RibbonStyle, widthDp: Dp) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val isDarkTheme =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val bitmap = remember(slots, widthDp, density, nowSlotIndex, style, isDarkTheme) {
        renderRibbonBitmap(slots, widthDp, density, nowSlotIndex, style, isDarkTheme)
    }
    val heightDp = (bitmap.height / density).dp

    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        modifier = GlanceModifier.width(widthDp).height(heightDp),
        contentScale = ContentScale.FillBounds,
    )
}

private fun renderRibbonBitmap(
    slots: List<TimeSlot>,
    widthDp: Dp,
    density: Float,
    nowSlotIndex: Int?,
    style: RibbonStyle,
    isDarkTheme: Boolean,
): Bitmap {
    val widthPx = (widthDp.value * density).toInt().coerceAtLeast(1)
    val trackHeightPx = (style.trackHeight.value * density).toInt().coerceAtLeast(1)
    val markerLineHeightPx = style.markerLineHeight.value * density
    val gapPx = 2f * density
    val tickHeightPx = 4f * density
    val hourLabelRowHeightPx =
        if (style.showHourLabels) style.hourLabelTextSp * density * 1.3f + tickHeightPx + gapPx else 0f
    // Space below the track is always reserved (even when there's no "now" marker to draw, e.g. the
    // tomorrow ribbon) so today/tomorrow ribbons stay the same height.
    val nowLabelHeightPx = style.markerTextSp * density * 1.3f
    val totalHeightPx =
        (hourLabelRowHeightPx + trackHeightPx + markerLineHeightPx + nowLabelHeightPx)
            .toInt()
            .coerceAtLeast(trackHeightPx)

    val bitmap = Bitmap.createBitmap(widthPx, totalHeightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val secondaryColor = (if (isDarkTheme) Palette.SecondaryNight else Palette.SecondaryDay).toArgb()
    val dividerColor = (if (isDarkTheme) Palette.DividerNight else Palette.DividerDay).toArgb()
    val markerColor = (if (isDarkTheme) Palette.TextNight else Palette.TextDay).toArgb()

    val trackTop = hourLabelRowHeightPx
    val trackBottom = trackTop + trackHeightPx

    if (style.showHourLabels) {
        val tickPaint = Paint().apply { color = dividerColor; strokeWidth = 1f * density }
        val textPaint = Paint().apply {
            color = secondaryColor
            textSize = style.hourLabelTextSp * density
            isAntiAlias = true
        }
        val tickTop = 0f
        val tickBottom = tickHeightPx
        val baselineY = tickBottom + gapPx + style.hourLabelTextSp * density
        style.hourMarks.forEachIndexed { i, hour ->
            textPaint.textAlign = when (i) {
                0 -> Paint.Align.LEFT
                style.hourMarks.lastIndex -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            val x = widthPx * (hour.toFloat() / HOURS_PER_DAY)
            val tickX = x.coerceIn(0.5f * density, widthPx - 0.5f * density)
            canvas.drawLine(tickX, tickTop, tickX, tickBottom, tickPaint)
            canvas.drawText("%02d:00".format(hour), x, baselineY, textPaint)
        }
    }

    val slotWidth = if (slots.isNotEmpty()) widthPx.toFloat() / slots.size else 0f
    if (slots.isNotEmpty()) {
        val clipPath = Path().apply {
            addRoundRect(
                RectF(0f, trackTop, widthPx.toFloat(), trackBottom),
                trackHeightPx / 2f,
                trackHeightPx / 2f,
                Path.Direction.CW,
            )
        }
        canvas.save()
        canvas.clipPath(clipPath)
        val paint = Paint()
        val powerOnArgb = Palette.PowerOn.toArgb()
        val powerOffArgb = Palette.PowerOff.toArgb()
        // Draw one rect per *run* of same-state slots rather than one rect per slot - abutting
        // same-color rects at non-integer slot boundaries can leave hairline gaps when rasterized,
        // which showed up as visible seams/"cuts" across the track.
        var runStart = 0
        while (runStart < slots.size) {
            val hasPower = slots[runStart].hasPower
            var runEnd = runStart + 1
            while (runEnd < slots.size && slots[runEnd].hasPower == hasPower) runEnd++
            paint.color = if (hasPower) powerOnArgb else powerOffArgb
            canvas.drawRect(runStart * slotWidth, trackTop, runEnd * slotWidth, trackBottom, paint)
            runStart = runEnd
        }
        if (nowSlotIndex != null && nowSlotIndex in slots.indices) {
            val elapsedX = (nowSlotIndex + 0.5f) * slotWidth
            val elapsedPaint = Paint().apply { color = AndroidColor.argb(0x61, 0xFF, 0xFF, 0xFF) }
            canvas.drawRect(0f, trackTop, elapsedX, trackBottom, elapsedPaint)
        }
        canvas.restore()
    }

    if (nowSlotIndex != null && slots.isNotEmpty() && nowSlotIndex in slots.indices) {
        val x = (nowSlotIndex + 0.5f) * slotWidth
        val lineX = x.coerceIn(1f * density, widthPx - 1f * density)

        val linePaint = Paint().apply { color = markerColor; strokeWidth = 2f * density }
        canvas.drawLine(lineX, trackBottom, lineX, trackBottom + markerLineHeightPx, linePaint)

        val labelPaint = Paint().apply {
            color = markerColor
            textSize = style.markerTextSp * density
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        val nowText = nowTimeLabel()
        val halfTextWidth = labelPaint.measureText(nowText) / 2
        val labelX = x.coerceIn(halfTextWidth, widthPx - halfTextWidth)
        val labelTop = trackBottom + markerLineHeightPx
        val baselineY = labelTop - labelPaint.ascent()
        canvas.drawText(nowText, labelX, baselineY, labelPaint)
    }

    return bitmap
}

private fun nowTimeLabel(): String {
    val now = ZonedDateTime.now(NextChangeCalculator.KYIV_ZONE)
    return "%02d:%02d".format(now.hour, now.minute)
}

internal fun nowSlotIndexIfToday(date: LocalDate): Int? {
    val now = ZonedDateTime.now(NextChangeCalculator.KYIV_ZONE)
    if (date != now.toLocalDate()) return null
    return (now.hour * 60 + now.minute) / TimeSlot.MINUTES_PER_SLOT
}

internal fun formatTime(schedule: OutageSchedule): String {
    val zoned = schedule.lastSyncedAt.atZone(NextChangeCalculator.KYIV_ZONE)
    return DateTimeFormatter.ofPattern("HH:mm").format(zoned)
}
