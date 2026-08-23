package com.hibiki.data.media

import android.content.Context
import com.hibiki.data.local.HibikiDatabaseProvider
import com.hibiki.domain.model.BuiltInUmamusume
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

class UmamusumePortraitSeeder(
    private val context: Context,
    private val databaseProvider: HibikiDatabaseProvider,
    private val imageFileStore: ImageFileStore,
) {
    private val mutex = Mutex()
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun ensureInstalled() = mutex.withLock {
        val refresh = prefs.getInt(KEY_REVISION, 0) != PACK_REVISION
        val dao = databaseProvider.getDatabase().subjectDao()
        BuiltInUmamusume.CHARACTERS.forEach { character ->
            val existing = dao.getById(character.id) ?: return@forEach
            if (!refresh && imageFileStore.exists(existing.imagePath)) return@forEach
            val installed = try {
                context.assets.open("$ASSET_DIR/${character.id}.jpg").let { input ->
                    imageFileStore.installNamed(fileName(character.id), input, overwrite = refresh)
                }
            } catch (_: IOException) {
                return@forEach
            }
            if (existing.imagePath != installed) {
                imageFileStore.delete(existing.imagePath)
            }
            dao.updateImagePath(character.id, installed)
        }
        if (refresh) {
            prefs.edit().putInt(KEY_REVISION, PACK_REVISION).apply()
        }
    }

    companion object {
        const val ASSET_DIR = "umamusume"
        const val PACK_REVISION = 2
        private const val PREFS = "umamusume_portraits"
        private const val KEY_REVISION = "pack_revision"

        fun fileName(id: String): String = "umamusume_$id.jpg"
    }
}
