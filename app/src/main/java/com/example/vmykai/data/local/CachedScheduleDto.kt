package com.example.vmykai.data.local

import com.example.vmykai.data.model.DaySchedule
import com.example.vmykai.data.model.OutageSchedule
import com.example.vmykai.data.model.QueueId
import com.example.vmykai.data.model.TimeSlot
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class CachedTimeSlotDto(val startMinute: Int, val hasPower: Boolean)

@Serializable
private data class CachedDayScheduleDto(val date: String, val slots: List<CachedTimeSlotDto>)

@Serializable
private data class CachedScheduleDto(
    val lastSyncedAt: String,
    val byQueue: Map<String, List<CachedDayScheduleDto>>,
)

/** Serializes [OutageSchedule] to/from JSON for storage in DataStore. */
object CachedScheduleSerializer {

    private val json = Json { ignoreUnknownKeys = true }

    fun toJson(schedule: OutageSchedule): String {
        val dto = CachedScheduleDto(
            lastSyncedAt = schedule.lastSyncedAt.toString(),
            byQueue = schedule.byQueue.entries.associate { (queueId, days) ->
                queueId.display to days.map { day ->
                    CachedDayScheduleDto(
                        date = day.date.toString(),
                        slots = day.slots.map { CachedTimeSlotDto(it.startMinute, it.hasPower) },
                    )
                }
            },
        )
        return json.encodeToString(CachedScheduleDto.serializer(), dto)
    }

    fun fromJson(raw: String): OutageSchedule? = try {
        val dto = json.decodeFromString(CachedScheduleDto.serializer(), raw)
        val byQueue = dto.byQueue.mapNotNull { (key, days) ->
            val queueId = QueueId.parse(key) ?: return@mapNotNull null
            queueId to days.map { day ->
                DaySchedule(
                    date = LocalDate.parse(day.date),
                    slots = day.slots.map { TimeSlot(it.startMinute, it.hasPower) },
                )
            }
        }.toMap()
        OutageSchedule(lastSyncedAt = Instant.parse(dto.lastSyncedAt), byQueue = byQueue)
    } catch (e: Exception) {
        null
    }
}
