package com.arslandaim.omegaplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSortOrderTest {

    @Test
    fun everyOrderHasNonBlankLabel() {
        MediaSortOrder.entries.forEach { order ->
            assertTrue(order.label.isNotBlank())
        }
    }

    @Test
    fun labelsAreUnique() {
        val labels = MediaSortOrder.entries.map { it.label }
        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun persistedNamesResolveBackToEntries() {
        MediaSortOrder.entries.forEach { order ->
            assertEquals(order, MediaSortOrder.valueOf(order.name))
        }
    }
}
