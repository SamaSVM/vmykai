package com.example.vmykai.data.remote

import android.content.Context
import com.example.vmykai.data.model.OutageSchedule

class KiroeRemoteScheduleSource(
    context: Context,
    private val fetcher: GpvGraphsFetcher = GpvGraphsFetcher(context),
) : RemoteScheduleSource {

    override suspend fun fetchSchedule(): OutageSchedule {
        val rawBody = fetcher.fetchGraphsJson(PAGE_URL, GRAPHS_URL)
        return GpvGraphsParser.parse(rawBody)
    }

    companion object {
        private const val BASE_URL = "https://kiroe.com.ua"
        private const val PAGE_URL = "$BASE_URL/electricity-blackout"
        private const val GRAPHS_URL = "$PAGE_URL/actual-gpv-graphs"
    }
}
