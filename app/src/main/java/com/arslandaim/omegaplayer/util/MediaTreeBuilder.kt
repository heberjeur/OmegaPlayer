package com.arslandaim.omegaplayer.util

import com.arslandaim.omegaplayer.data.model.FolderNode
import java.io.File

object MediaTreeBuilder {
    fun build(paths: List<Pair<String, Long>>, threshold: Int): FolderNode {
        val root = FolderNode("Internal", "")
        for ((filePath, _) in paths) {
            var node = root
            var dir = File(filePath).parentFile
            val segments = ArrayDeque<String>()
            while (dir != null && dir.absolutePath.length > 1) {
                segments.addFirst(dir.name)
                dir = dir.parentFile
            }
            var built = ""
            for (seg in segments) {
                built = "$built/$seg"
                node = node.subFolders.getOrPut(seg) { FolderNode(seg, built) }
                node.videoCount++
            }
            root.videoCount++
        }
        flatten(root, threshold)
        return skipSingleRootChain(root)
    }

    private fun flatten(node: FolderNode, threshold: Int) {
        node.subFolders.values.forEach { flatten(it, threshold) }
        val sumChildVideos = node.subFolders.values.sumOf { it.videoCount }
        val hasDirectMedia = node.videoCount > sumChildVideos
        if (node.subFolders.isNotEmpty() && node.subFolders.size <= threshold && !hasDirectMedia) {
            node.isFlattened = true
        }
    }

    fun findNode(root: FolderNode, path: String): FolderNode? {
        if (root.path == path) return root
        return root.subFolders.values.firstNotNullOfOrNull { findNode(it, path) }
    }

    fun findParent(root: FolderNode, targetPath: String): FolderNode? {
        for (child in root.subFolders.values) {
            if (child.path == targetPath) return root
            findParent(child, targetPath)?.let { return it }
        }
        return null
    }

    private fun skipSingleRootChain(node: FolderNode): FolderNode {
        var cur = node
        while (cur.subFolders.size == 1 && cur.getVisibleChildren().size == 1) {
            val candidate = cur.getVisibleChildren().first()
            if (candidate.subFolders.isEmpty()) break
            cur = candidate
        }
        return cur
    }
}
