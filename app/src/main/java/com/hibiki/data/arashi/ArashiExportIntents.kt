package com.hibiki.data.arashi

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ArashiExportIntents {
    fun fileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, ArashiExportContract.FILE_PROVIDER_AUTHORITY, file)
    }

    fun importIntent(packageFileUri: Uri, exportId: String): Intent {
        return Intent(ArashiExportContract.ACTION_IMPORT).apply {
            setClassName(ArashiExportContract.ARASHI_PACKAGE, ArashiExportContract.IMPORT_ACTIVITY)
            setDataAndType(packageFileUri, ArashiExportContract.MIME_TYPE)
            putExtra(Intent.EXTRA_STREAM, packageFileUri)
            putExtra(ArashiExportContract.EXTRA_EXPORT_ID, exportId)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri("hibiki-arashi-export", packageFileUri)
        }
    }

    fun isArashiImportAvailable(context: Context): Boolean {
        val probe = Intent(ArashiExportContract.ACTION_IMPORT).setClassName(
            ArashiExportContract.ARASHI_PACKAGE,
            ArashiExportContract.IMPORT_ACTIVITY,
        )
        return context.packageManager.resolveActivity(probe, 0) != null
    }
}
