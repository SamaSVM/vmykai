package com.example.vmykai.notifications

import com.example.vmykai.data.model.DaySchedule
import com.example.vmykai.data.model.OutageSchedule
import com.example.vmykai.data.model.QueueId
import com.example.vmykai.data.model.TimeSlot
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextChangeCalculatorTest {

    private val queueId = QueueId(2, 1)
    private val zone = NextChangeCalculator.KYIV_ZONE
    private val today = LocalDate.of(2026, 11, 10)

    private fun scheduleWithHourlyPower(hasPowerByHour: List<Boolean>): OutageSchedule {
        val slots = hasPowerByHour.flatMapIndexed { hour, hasPower ->
            listOf(TimeSlot(hour * 60, hasPower), TimeSlot(hour * 60 + 30, hasPower))
        }
        return OutageSchedule(
            lastSyncedAt = Instant.now(),
            byQueue = mapOf(queueId to listOf(DaySchedule(today, slots))),
        )
    }

    @Test
    fun `finds the next hour where power turns off`() {
        val hours = MutableList(24) { true }
        hours[10] = false
        val schedule = scheduleWithHourlyPower(hours)

        val now = today.atTime(8, 0).atZone(zone).toInstant()
        val change = NextChangeCalculator.nextChange(schedule, queueId, now, zone)

        assertEquals(today.atTime(10, 0).atZone(zone).toInstant(), change?.at)
        assertEquals(false, change?.powerWillBeOn)
    }

    @Test
    fun `finds the next hour where power turns back on`() {
        val hours = MutableList(24) { true }
        hours[10] = false
        val schedule = scheduleWithHourlyPower(hours)

        val now = today.atTime(10, 30).atZone(zone).toInstant()
        val change = NextChangeCalculator.nextChange(schedule, queueId, now, zone)

        assertEquals(today.atTime(11, 0).atZone(zone).toInstant(), change?.at)
        assertEquals(true, change?.powerWillBeOn)
    }

    @Test
    fun `returns null when no further status change is scheduled`() {
        val hours = MutableList(24) { true }
        val schedule = scheduleWithHourlyPower(hours)

        val now = today.atTime(8, 0).atZone(zone).toInstant()
        val change = NextChangeCalculator.nextChange(schedule, queueId, now, zone)

        assertNull(change)
    }

    @Test
    fun `returns null for a queue with no data`() {
        val schedule = scheduleWithHourlyPower(List(24) { true })
        val now = today.atTime(8, 0).atZone(zone).toInstant()

        val change = NextChangeCalculator.nextChange(schedule, QueueId(5, 2), now, zone)

        assertNull(change)
    }
}
