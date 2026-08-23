package com.hibiki.data.backup

sealed class BackupExportState {
    data object Preparing : BackupExportState()
    data object CopyingDatabase : BackupExportState()
    data class CopyingMedia(val current: Int, val total: Int) : BackupExportState()
    data object WritingManifest : BackupExportState()
    data object Compressing : BackupExportState()
    data object Validating : BackupExportState()
    data class Completed(val outputFile: java.io.File) : BackupExportState()
    data class Failed(val error: Throwable) : BackupExportState()
}

sealed class BackupImportState {
    data object Preparing : BackupImportState()
    data object Extracting : BackupImportState()
    data object ReadingManifest : BackupImportState()
    data object ValidatingChecksums : BackupImportState()
    data object ValidatingDatabase : BackupImportState()
    data object BackingUpCurrentData : BackupImportState()
    data object ReplacingDatabase : BackupImportState()
    data object ReplacingMedia : BackupImportState()
    data object ReopeningDatabase : BackupImportState()
    data object FinalValidation : BackupImportState()
    data object Completed : BackupImportState()
    data object RollingBack : BackupImportState()
    data class Failed(val error: Throwable) : BackupImportState()
}
