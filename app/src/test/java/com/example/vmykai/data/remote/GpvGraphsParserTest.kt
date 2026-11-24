package com.example.vmykai.data.remote

import com.example.vmykai.data.model.QueueId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GpvGraphsParserTest {

    @Test
    fun `parses the real response shape with status codes 0, 1 and 10`() {
        val json = """
            {"success":true,"data":{"2.1":{"times":{
                "00:00":"0","00:30":"10","01:00":"1","01:30":"0"
            }}}}
        """.trimIndent()

        val schedule = GpvGraphsParser.parse(json)

        val days = schedule.daysFor(QueueId(2, 1))
        assertEquals(1, days.size)
        val slots = days[0].slots
        assertEquals(4, slots.size)
        assertTrue(slots[0].hasPower)
        assertEquals(0, slots[0].startMinute)
        assertTrue(slots[1].hasPower) // "10" (possible outage) is treated as power-on, not an outage
        assertEquals(30, slots[1].startMinute)
        assertFalse(slots[2].hasPower) // "1" is the only status that counts as an outage
        assertEquals(60, slots[2].startMinute)
        assertTrue(slots[3].hasPower)
        assertEquals(90, slots[3].startMinute)
    }

    @Test
    fun `unwraps a graphs envelope`() {
        val json = """{"graphs": {"3.2": {"times": {"00:00": "0"}}}}"""

        val schedule = GpvGraphsParser.parse(json)

        assertEquals(1, schedule.daysFor(QueueId(3, 2)).size)
    }

    @Test
    fun `picks up a tomorrow times object when present`() {
        val json = """
            {"data":{"1.1":{"times":{"00:00":"0"},"tomorrow":{"00:00":"1"}}}}
        """.trimIndent()

        val schedule = GpvGraphsParser.parse(json)

        val days = schedule.daysFor(QueueId(1, 1))
        assertEquals(2, days.size)
        assertFalse(days[1].slots.all { it.hasPower })
    }

    @Test
    fun `throws a descriptive error for unrecognized shapes`() {
        assertThrows(ScheduleParseException::class.java) {
            GpvGraphsParser.parse("""{"unexpected":"shape"}""")
        }
    }
}
