package com.example.vmykai.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueIdTest {

    @Test
    fun `parses a valid queue id`() {
        assertEquals(QueueId(2, 1), QueueId.parse("2.1"))
    }

    @Test
    fun `rejects malformed input`() {
        assertNull(QueueId.parse("2"))
        assertNull(QueueId.parse("2.1.3"))
        assertNull(QueueId.parse("a.b"))
    }

    @Test
    fun `lists all 12 queues from 1-1 to 6-2`() {
        assertEquals(12, QueueId.ALL.size)
        assertEquals(QueueId(1, 1), QueueId.ALL.first())
        assertEquals(QueueId(6, 2), QueueId.ALL.last())
    }

    @Test
    fun `displays as group-dot-sub`() {
        assertEquals("2.1", QueueId(2, 1).display)
    }
}
