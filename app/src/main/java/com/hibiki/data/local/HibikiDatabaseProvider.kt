package com.hibiki.data.local

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

class HibikiDatabaseProvider(
    private val context: Context,
) {
    @Volatile
    private var database: HibikiDatabase? = null

    private val lock = Any()
    private val _generation = MutableStateFlow(0)
    val generation: StateFlow<Int> = _generation.asStateFlow()

    fun getDatabase(): HibikiDatabase {
        return database ?: synchronized(lock) {
            database ?: HibikiDatabase.create(context).also { database = it }
        }
    }

    fun closeDatabase() {
        synchronized(lock) {
            closeDatabaseInternal()
        }
    }

    fun reopenDatabase() {
        synchronized(lock) {
            if (database != null) return
            database = HibikiDatabase.create(context)
            _generation.value += 1
        }
    }

    fun replaceDatabase(newDatabaseFile: File) {
        require(newDatabaseFile.exists()) { "Replacement database file does not exist: $newDatabaseFile" }
        synchronized(lock) {
            closeDatabaseInternal()
            val dbDir = context.getDatabasePath(DatabaseConstants.DATABASE_NAME).parentFile
                ?: throw IOException("Database directory not available")
            if (!dbDir.exists() && !dbDir.mkdirs()) {
                throw IOException("Cannot create database directory: $dbDir")
            }
            val target = File(dbDir, DatabaseConstants.DATABASE_NAME)
            deleteDatabaseSidecars(target)
            newDatabaseFile.copyTo(target, overwrite = true)
            database = HibikiDatabase.create(context)
            _generation.value += 1
        }
    }

    fun createConsistentDatabaseCopy(destination: File) {
        val db = getDatabase()
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        val sourcePath = getDatabaseFile()
        if (!sourcePath.exists()) {
            throw IOException("Database file not found: $sourcePath")
        }
        destination.parentFile?.mkdirs()
        sourcePath.copyTo(destination, overwrite = true)
    }

    fun validateDatabaseFile(dbFile: File): DatabaseValidationResult {
        if (!dbFile.exists()) {
            return DatabaseValidationResult(false, "Database file missing")
        }
        val tempName = "validate_${System.currentTimeMillis()}.db"
        val tempFile = File(dbFile.parentFile, tempName)
        return try {
            dbFile.copyTo(tempFile, overwrite = true)
            val tempDb = Room.databaseBuilder(
                context.applicationContext,
                HibikiDatabase::class.java,
                tempName,
            )
                .createFromFile(tempFile)
                .build()
            val sqlite = tempDb.openHelper.writableDatabase
            val integrity = sqlite.query("PRAGMA integrity_check").use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else "missing"
            }
            val fkViolations = mutableListOf<String>()
            sqlite.query("PRAGMA foreign_key_check").use { cursor ->
                while (cursor.moveToNext()) {
                    fkViolations += "${cursor.getString(0)}:${cursor.getString(1)}"
                }
            }
            tempDb.close()
            when {
                integrity != "ok" -> DatabaseValidationResult(false, "Integrity check failed: $integrity")
                fkViolations.isNotEmpty() ->
                    DatabaseValidationResult(false, "FK violations: ${fkViolations.joinToString()}")
                else -> DatabaseValidationResult(true, "ok")
            }
        } catch (e: Exception) {
            DatabaseValidationResult(false, e.message ?: "Validation error")
        } finally {
            tempFile.delete()
            context.deleteDatabase(tempName)
        }
    }

    fun getDatabaseFile(): File = context.getDatabasePath(DatabaseConstants.DATABASE_NAME)

    fun deleteDatabaseSidecars(dbFile: File) {
        File("${dbFile.path}-wal").delete()
        File("${dbFile.path}-shm").delete()
        File("${dbFile.path}-journal").delete()
    }

    private fun closeDatabaseInternal() {
        database?.close()
        database = null
    }
}

data class DatabaseValidationResult(
    val isValid: Boolean,
    val message: String,
)
