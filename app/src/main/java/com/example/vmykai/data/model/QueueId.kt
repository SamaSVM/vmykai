package com.example.vmykai.data.model

data class QueueId(val group: Int, val sub: Int) {

    val display: String get() = "$group.$sub"

    override fun toString(): String = display

    companion object {
        fun parse(value: String): QueueId? {
            val parts = value.split(".")
            if (parts.size != 2) return null
            val group = parts[0].toIntOrNull() ?: return null
            val sub = parts[1].toIntOrNull() ?: return null
            return QueueId(group, sub)
        }

        // Kirovogradoblenergo currently publishes queues 1.1-6.2.
        val ALL: List<QueueId> = (1..6).flatMap { group -> (1..2).map { sub -> QueueId(group, sub) } }
    }
}
