package com.hibiki.data.repository

import com.hibiki.data.backup.BackupExportService
import com.hibiki.data.backup.BackupExportState
import com.hibiki.data.backup.BackupImportService
import com.hibiki.data.backup.BackupImportState
import kotlinx.coroutines.flow.Flow
import java.io.File

class BackupRepository(
    private val exportService: BackupExportService,
    private val importService: BackupImportService,
) {
    fun exportBackup(outputFile: File): Flow<BackupExportState> = exportService.export(outputFile)

    fun importBackup(archiveFile: File): Flow<BackupImportState> = importService.import(archiveFile)
}
