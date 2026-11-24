package com.example.vmykai.data.remote

import com.example.vmykai.data.model.OutageSchedule

/** Fetches the latest outage schedule for all queues from kiroe.com.ua. */
interface RemoteScheduleSource {
    suspend fun fetchSchedule(): OutageSchedule
}

class CloudflareBypassFailedException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ScheduleParseException(message: String) : Exception(message)
