package com.hibiki.data.backup

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object BackupConstants {
    const val FORMAT_NAME = "hibikibackup"
    const val FORMAT_VERSION = 1
    const val EXTENSION = ".hibikibackup"
    const val MANIFEST_FILE = "manifest.json"
    const val DATABASE_DIR = "database"
    const val MEDIA_DIR = "media"
    const val DATABASE_FILE_NAME = "hibiki.db"

    const val MAX_BACKUP_SIZE_BYTES = 500L * 1024 * 1024
    const val MAX_UNCOMPRESSED_SIZE_BYTES = 2L * 1024 * 1024 * 1024
    const val MAX_FILE_COUNT = 50_000
    const val MAX_SINGLE_FILE_SIZE_BYTES = 100L * 1024 * 1024

    fun defaultExportFileName(): String {
        val date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return "hibiki_backup_$date.zip"
    }
}
