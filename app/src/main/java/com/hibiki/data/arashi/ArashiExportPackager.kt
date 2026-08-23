package com.hibiki.data.arashi

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArashiExportPackager {
    fun write(
        outputFile: File,
        manifestJson: String,
        audioFiles: Map<String, File>,
    ) {
        if (outputFile.exists()) outputFile.delete()
        outputFile.parentFile?.mkdirs()
        ZipOutputStream(outputFile.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry(ArashiExportContract.MANIFEST_FILE))
            zos.write(manifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            audioFiles.forEach { (fileName, source) ->
                require(!fileName.contains("..") && !fileName.contains('/') && !fileName.contains('\\')) {
                    "Invalid audio file name: $fileName"
                }
                if (!source.isFile) return@forEach
                zos.putNextEntry(ZipEntry("${ArashiExportContract.AUDIO_DIR}/$fileName"))
                source.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }
}
