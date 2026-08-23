package com.hibiki.ui.contexts

import android.graphics.Bitmap
import com.hibiki.data.media.ImageFileStore
import java.io.File

data class ImageEdit(
    val previewPath: String? = null,
    val originalPath: String? = null,
    val pendingTempPath: String? = null,
) {
    fun applyCropped(bitmap: Bitmap, store: ImageFileStore): ImageEdit {
        pendingTempPath?.let { File(it).delete() }
        val temp = store.createTempFile()
        store.writeJpeg(bitmap, temp)
        return copy(previewPath = temp.absolutePath, pendingTempPath = temp.absolutePath)
    }

    fun persist(store: ImageFileStore): String? {
        val pending = pendingTempPath
        if (pending.isNullOrBlank()) return originalPath
        val persisted = store.persist(File(pending))
        if (originalPath != null && originalPath != persisted) {
            store.delete(originalPath)
        }
        return persisted
    }

    fun discardPending() {
        pendingTempPath?.let { File(it).delete() }
    }
}
