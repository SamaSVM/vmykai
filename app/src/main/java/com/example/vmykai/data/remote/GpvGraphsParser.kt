package com.example.vmykai.data.remote

import com.example.vmykai.data.model.DaySchedule
import com.example.vmykai.data.model.OutageSchedule
import com.example.vmykai.data.model.QueueId
import com.example.vmykai.data.model.TimeSlot
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Parses the response of POST actual-gpv-graphs into [OutageSchedule].
 *
 * Confirmed shape (captured from a live response): a root object with `"data"` keyed by queue id
 * (e.g. "2.1"), each holding a `"times"` object keyed by "HH:MM" (30-minute steps) whose values are
 * status codes: "0" = power on, "1" = power off, "10" = possible/uncertain outage (confirmed from
 * the site's own `getNewStatusConfig` in its bundled JS). Since the app only shows a plain on/off
 * timeline, "10" is treated as power-on - only a confirmed "1" counts as an outage, matching how the
 * site's own schedule reads (e.g. a 10:00-12:00 outage is encoded as "1" from 10:00 through 11:30,
 * with "10" only at the single slot right before/after as a buffer marker, not part of the outage
 * itself). The response doesn't include tomorrow's schedule when it hasn't been published yet; if a
 * queue entry ever gains a "tomorrow" times object it's picked up the same way.
 */
object GpvGraphsParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawBody: String): OutageSchedule {
        val root = try {
            json.parseToJsonElement(rawBody).jsonObject
        } catch (e: Exception) {
            throw ScheduleParseException("Response is not a JSON object: ${rawBody.take(200)}")
        }

        val queuesObject = (root["data"] ?: root["graphs"] ?: root).let {
            it as? JsonObject ?: throw ScheduleParseException("Expected an object of per-queue graphs")
        }

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val byQueue = buildMap<QueueId, List<DaySchedule>> {
            for ((key, value) in queuesObject) {
                val queueId = QueueId.parse(key) ?: continue
                val entry = value as? JsonObject ?: continue
                put(queueId, parseQueueEntry(entry, today, tomorrow))
            }
        }

        if (byQueue.isEmpty()) {
            throw ScheduleParseException(
                "No recognizable queue entries (expected keys like \"1.1\") in: ${rawBody.take(200)}"
            )
        }

        return OutageSchedule(lastSyncedAt = Instant.now(), byQueue = byQueue)
    }

    private fun parseQueueEntry(entry: JsonObject, today: LocalDate, tomorrow: LocalDate): List<DaySchedule> =
        buildList {
            (entry["times"] as? JsonObject)?.let { add(DaySchedule(today, parseTimes(it))) }
            (entry["tomorrow"] as? JsonObject)?.let { add(DaySchedule(tomorrow, parseTimes(it))) }
        }

    private fun parseTimes(times: JsonObject): List<TimeSlot> =
        times.entries
            .mapNotNull { (timeKey, value) ->
                val startMinute = parseStartMinute(timeKey) ?: return@mapNotNull null
                startMinute to (((value as? JsonPrimitive)?.content) != "1")
            }
            .sortedBy { it.first }
            .map { (startMinute, hasPower) -> TimeSlot(startMinute, hasPower) }

    private fun parseStartMinute(timeKey: String): Int? {
        val (hourPart, minutePart) = timeKey.split(":").takeIf { it.size == 2 } ?: return null
        val hour = hourPart.toIntOrNull() ?: return null
        val minute = minutePart.toIntOrNull() ?: return null
        return hour * 60 + minute
    }
}
