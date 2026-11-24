package com.example.vmykai.data.local

import com.example.vmykai.data.model.DaySchedule
import com.example.vmykai.data.model.OutageSchedule
import com.example.vmykai.data.model.QueueId
import com.example.vmykai.data.model.TimeSlot
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CachedScheduleSerializerTest {

    @Test
    fun `round-trips a schedule through JSON`() {
        val original = OutageSchedule(
            lastSyncedAt = Instant.parse("2026-11-10T08:00:00Z"),
            byQueue = mapOf(
                QueueId(2, 1) to listOf(
                    DaySchedule(
                        LocalDate.of(2026, 11, 10),
                        listOf(
                            TimeSlot(0, true),
                            TimeSlot(30, false),
                            TimeSlot(60, false),
                        ),
                    ),
                ),
            ),
        )

        val json = CachedScheduleSerializer.toJson(original)
        val restored = CachedScheduleSerializer.fromJson(json)

        assertEquals(original, restored)
    }

    @Test
    fun `returns null for garbage input`() {
        assertEquals(null, CachedScheduleSerializer.fromJson("not json"))
    }
}
