package com.example.vmykai.notifications

import com.example.vmykai.data.model.OutageSchedule
import com.example.vmykai.data.model.QueueId
import java.time.Instant
import java.time.ZoneId

data class StatusChange(val at: Instant, val powerWillBeOn: Boolean)

/** The outage window that is either happening right now or will happen next. [end] is null if the schedule doesn't extend far enough to know when power returns. */
data class OutageWindow(val start: Instant, val end: Instant?)

object NextChangeCalculator {

    val KYIV_ZONE: ZoneId = ZoneId.of("Europe/Kyiv")

    private fun timelineOf(schedule: OutageSchedule, queueId: QueueId, zone: ZoneId): List<Pair<Instant, Boolean>> {
        val days = schedule.daysFor(queueId).sortedBy { it.date }
        return days.flatMap { day ->
            day.slots.map { slot ->
                val start = day.date.atStartOfDay(zone).plusMinutes(slot.startMinute.toLong()).toInstant()
                start to slot.hasPower
            }
        }.sortedBy { it.first }
    }

    /** Finds the next slot boundary, after [now], where the queue's power status flips. */
    fun nextChange(
        schedule: OutageSchedule,
        queueId: QueueId,
        now: Instant,
        zone: ZoneId = KYIV_ZONE,
    ): StatusChange? {
        val timeline = timelineOf(schedule, queueId, zone)
        for (i in 0 until timeline.size - 1) {
            val (_, currentPower) = timeline[i]
            val (nextStart, nextPower) = timeline[i + 1]
            if (nextStart.isAfter(now) && nextPower != currentPower) {
                return StatusChange(at = nextStart, powerWillBeOn = nextPower)
            }
        }
        return null
    }

    /** Whether the queue has power at [now], or null if there's no data covering that moment. */
    fun isPowerOnAt(schedule: OutageSchedule, queueId: QueueId, now: Instant, zone: ZoneId = KYIV_ZONE): Boolean? {
        val timeline = timelineOf(schedule, queueId, zone)
        return timeline.lastOrNull { it.first <= now }?.second
    }

    /**
     * The outage window relevant to [now]: if power is currently off, the start/end of that outage;
     * otherwise the next upcoming one. Returns null if there's no outage left in the known schedule.
     */
    fun currentOrNextOutageWindow(
        schedule: OutageSchedule,
        queueId: QueueId,
        now: Instant,
        zone: ZoneId = KYIV_ZONE,
    ): OutageWindow? {
        val timeline = timelineOf(schedule, queueId, zone)
        val currentIndex = timeline.indexOfLast { it.first <= now }
        if (currentIndex == -1) return null

        val outageStartIndex = if (!timeline[currentIndex].second) {
            var startIdx = currentIndex
            while (startIdx > 0 && !timeline[startIdx - 1].second) startIdx--
            startIdx
        } else {
            (currentIndex + 1 until timeline.size).firstOrNull { !timeline[it].second } ?: return null
        }

        val start = timeline[outageStartIndex].first
        val endIndex = (outageStartIndex + 1 until timeline.size).firstOrNull { timeline[it].second }
        val end = endIndex?.let { timeline[it].first }
        return OutageWindow(start, end)
    }
}
