package com.example.vmykai.notifications

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

data class StatusDetail(val label: String, val value: String)

/** Shared "next change" label/value and countdown text formatting, used by both the widgets and the app screen. */
object OutageStatusText {
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(NextChangeCalculator.KYIV_ZONE)

    fun statusDetail(isPowerOn: Boolean, window: OutageWindow?): StatusDetail? {
        if (window == null) return null
        val end = window.end?.let { TIME_FORMATTER.format(it) }
        return if (isPowerOn) {
            val start = TIME_FORMATTER.format(window.start)
            StatusDetail("Наступне відключення", if (end != null) "$start – $end" else start)
        } else {
            StatusDetail("Світло з'явиться о", end ?: "невідомо")
        }
    }

    fun countdownText(isPowerOn: Boolean, window: OutageWindow?, now: Instant): String? {
        val target = if (isPowerOn) window?.start else window?.end
        if (target == null || !target.isAfter(now)) return null
        val minutes = Duration.between(now, target).toMinutes()
        val h = minutes / 60
        val m = minutes % 60
        val duration = when {
            h > 0 && m > 0 -> "$h год $m хв"
            h > 0 -> "$h год"
            else -> "$m хв"
        }
        return if (isPowerOn) "через $duration" else "ще $duration"
    }
}
