package com.example.vmykai.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vmykai.data.model.TimeSlot
import com.example.vmykai.notifications.NextChangeCalculator
import com.example.vmykai.ui.theme.Palette
import java.time.ZonedDateTime

private const val HOURS_PER_DAY = 24
private val HOUR_MARKS = listOf(0, 4, 8, 12, 16, 20, 24)
private val TRACK_HEIGHT = 11.dp
private val MARKER_LINE_HEIGHT = 4.dp
private const val HOUR_LABEL_SP = 10f
private const val MARKER_TEXT_SP = 9f

/**
 * A rounded-capsule timeline ribbon with hour ticks and a dimmed overlay for already-elapsed slots -
 * matches the look of the home-screen widget's ribbon (see [com.example.vmykai.widget.HourRibbon]),
 * reimplemented against the live Compose `Canvas` (via its `nativeCanvas`) using the same drawing
 * algorithm since the app isn't bound by Glance/RemoteViews' sibling-view cap that forced a bitmap
 * renderer there. The "now" marker is a short connector line below the track leading to a plain time
 * label, intentionally not a floating pill above the track.
 */
@Composable
fun TimelineRibbon(
    slots: List<TimeSlot>,
    modifier: Modifier = Modifier,
    nowSlotIndex: Int? = null,
    contentDescription: String? = null,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val secondaryColor = (if (isDarkTheme) Palette.SecondaryNight else Palette.SecondaryDay).toArgb()
    val dividerColor = (if (isDarkTheme) Palette.DividerNight else Palette.DividerDay).toArgb()
    val markerColor = (if (isDarkTheme) Palette.TextNight else Palette.TextDay).toArgb()
    val powerOnArgb = Palette.PowerOn.toArgb()
    val powerOffArgb = Palette.PowerOff.toArgb()

    val hourLabelRowHeight = HOUR_LABEL_SP * 1.3f + 4f + 2f
    // Space below the track is always reserved (even without a "now" marker, e.g. the tomorrow
    // ribbon) so today/tomorrow ribbons stay the same height.
    val nowLabelHeight = MARKER_TEXT_SP * 1.3f
    val totalHeightDp = (hourLabelRowHeight + TRACK_HEIGHT.value + MARKER_LINE_HEIGHT.value + nowLabelHeight).dp

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.fillMaxWidth().height(totalHeightDp).then(semanticsModifier)) {
        val density = this.density
        val widthPx = size.width
        val trackHeightPx = TRACK_HEIGHT.toPx()
        val markerLineHeightPx = MARKER_LINE_HEIGHT.toPx()
        val gapPx = 2f * density
        val tickHeightPx = 4f * density
        val trackTop = HOUR_LABEL_SP * density * 1.3f + tickHeightPx + gapPx
        val trackBottom = trackTop + trackHeightPx

        val canvas = drawContext.canvas.nativeCanvas

        run {
            val tickPaint = Paint().apply { color = dividerColor; strokeWidth = 1f * density }
            val textPaint = Paint().apply {
                color = secondaryColor
                textSize = HOUR_LABEL_SP * density
                isAntiAlias = true
            }
            val tickTop = 0f
            val tickBottom = tickHeightPx
            val baselineY = tickBottom + gapPx + HOUR_LABEL_SP * density
            HOUR_MARKS.forEachIndexed { i, hour ->
                textPaint.textAlign = when (i) {
                    0 -> Paint.Align.LEFT
                    HOUR_MARKS.lastIndex -> Paint.Align.RIGHT
                    else -> Paint.Align.CENTER
                }
                val x = widthPx * (hour.toFloat() / HOURS_PER_DAY)
                val tickX = x.coerceIn(0.5f * density, widthPx - 0.5f * density)
                canvas.drawLine(tickX, tickTop, tickX, tickBottom, tickPaint)
                canvas.drawText("%02d:00".format(hour), x, baselineY, textPaint)
            }
        }

        val slotWidth = if (slots.isNotEmpty()) widthPx / slots.size else 0f
        if (slots.isNotEmpty()) {
            val clipPath = Path().apply {
                addRoundRect(
                    RectF(0f, trackTop, widthPx, trackBottom),
                    trackHeightPx / 2f,
                    trackHeightPx / 2f,
                    Path.Direction.CW,
                )
            }
            canvas.save()
            canvas.clipPath(clipPath)
            val paint = Paint()
            // Draw one rect per *run* of same-state slots rather than one rect per slot - abutting
            // same-color rects at non-integer slot boundaries can leave hairline gaps when
            // rasterized, which showed up as visible seams/"cuts" across the track.
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
                textSize = MARKER_TEXT_SP * density
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
    }
}

private fun nowTimeLabel(): String {
    val now = ZonedDateTime.now(NextChangeCalculator.KYIV_ZONE)
    return "%02d:%02d".format(now.hour, now.minute)
}
