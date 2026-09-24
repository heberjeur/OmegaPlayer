package com.arslandaim.omegaplayer.data.model

data class FolderNode(
    val name: String,
    val path: String,
    val subFolders: MutableMap<String, FolderNode> = mutableMapOf(),
    var videoCount: Int = 0,
    var isFlattened: Boolean = false,
    var directMediaCount: Int = 0
) {
    fun getVisibleChildren(): List<FolderNode> {
        val result = mutableListOf<FolderNode>()

        if (directMediaCount > 0 && subFolders.isNotEmpty() && !isFlattened) {
            result.add(
                this.copy(
                    name = if (name.isEmpty()) "Internal Media" else "$name (Media)",
                    subFolders = mutableMapOf(),
                    videoCount = directMediaCount
                )
            )
        }

        for (child in subFolders.values) {
            if (child.isFlattened) {
                if (child.directMediaCount > 0) {
                    result.add(
                        child.copy(
                            subFolders = mutableMapOf(),
                            videoCount = child.directMediaCount
                        )
                    )
                }
                result.addAll(child.getVisibleChildren())
            } else {
                result.add(child)
            }
        }
        return result
    }
}
