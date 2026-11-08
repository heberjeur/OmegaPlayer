package com.mohamedaminelouati.oplayer.data

import com.mohamedaminelouati.oplayer.data.model.FolderNode
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

    @Test
    fun everyOrderHasValidLabelRes() {
        MediaSortOrder.entries.forEach { order ->
            assertTrue(order.labelRes != 0)
        }
    }

    @Test
    fun toMediaSortOrderResolvesValidAndFallbackNames() {
        assertEquals(MediaSortOrder.NAME_ASC, "NAME_ASC".toMediaSortOrder())
        assertEquals(MediaSortOrder.DURATION_DESC, "DURATION_DESC".toMediaSortOrder())
        assertEquals(MediaSortOrder.DATE_DESC, "INVALID_NAME".toMediaSortOrder())
        assertEquals(MediaSortOrder.DATE_DESC, (null as String?).toMediaSortOrder())
    }

    @Test
    fun folderNodesSortedWithOrderOrdersCorrectly() {
        val folders = listOf(
            FolderNode(path = "/media/Zeta", name = "Zeta", videoCount = 15),
            FolderNode(path = "/media/Alpha", name = "Alpha", videoCount = 3),
            FolderNode(path = "/media/Beta", name = "Beta", videoCount = 42)
        )

        val byNameAsc = folders.sortedWithOrder(MediaSortOrder.NAME_ASC).map { it.name }
        assertEquals(listOf("Alpha", "Beta", "Zeta"), byNameAsc)

        val byNameDesc = folders.sortedWithOrder(MediaSortOrder.NAME_DESC).map { it.name }
        assertEquals(listOf("Zeta", "Beta", "Alpha"), byNameDesc)

        val bySizeDesc = folders.sortedWithOrder(MediaSortOrder.SIZE_DESC).map { it.name }
        assertEquals(listOf("Beta", "Zeta", "Alpha"), bySizeDesc)

        val bySizeAsc = folders.sortedWithOrder(MediaSortOrder.SIZE_ASC).map { it.name }
        assertEquals(listOf("Alpha", "Zeta", "Beta"), bySizeAsc)
    }
}
