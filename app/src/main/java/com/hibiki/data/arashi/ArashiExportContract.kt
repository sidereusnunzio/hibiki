package com.hibiki.data.arashi

object ArashiExportContract {
    const val SCHEMA_VERSION = 1
    const val SOURCE_APP = "hibiki"

    const val ARASHI_PACKAGE = "com.arashi.vocab"
    const val IMPORT_ACTIVITY = "com.arashi.vocab.hibiki.HibikiImportActivity"
    const val ACTION_IMPORT = "com.arashi.vocab.action.IMPORT_HIBIKI_PHRASES"

    const val MIME_TYPE = "application/vnd.hibiki.arashi-export+zip"
    const val FILE_PROVIDER_AUTHORITY = "com.hibiki.fileprovider"

    const val EXTRA_EXPORT_ID = "com.hibiki.arashi.EXTRA_EXPORT_ID"
    const val EXTRA_IMPORT_RESULT = "com.hibiki.arashi.EXTRA_IMPORT_RESULT"
    const val EXTRA_IMPORT_ERROR = "com.hibiki.arashi.EXTRA_IMPORT_ERROR"

    const val MANIFEST_FILE = "manifest.json"
    const val AUDIO_DIR = "audio"

    const val RESULT_OK = -1
    const val RESULT_CANCELED = 0
}
