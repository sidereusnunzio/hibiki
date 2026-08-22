package com.hibiki.data.repository

import android.media.projection.MediaProjection
import com.hibiki.data.audio.PcmClip
import com.hibiki.data.audio.PlaybackAudioRecorder
import com.hibiki.data.audio.SilenceTrimmer
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.AppException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioCaptureRepository {
    private val recorder = PlaybackAudioRecorder()
    @Volatile private var mediaProjection: MediaProjection? = null

    val isReady: Boolean get() = mediaProjection != null

    fun attachProjection(projection: MediaProjection) {
        mediaProjection = projection
    }

    fun detachProjection() {
        mediaProjection = null
    }

    fun requestStop() {
        recorder.requestStop()
    }

    suspend fun recordClip(maxDurationMs: Long, trimSilence: Boolean): PcmClip =
        withContext(Dispatchers.IO) {
            val projection = mediaProjection ?: throw AppException(AppError.MediaProjectionDenied)
            val raw = recorder.record(projection, maxDurationMs)
            val clip = if (trimSilence) SilenceTrimmer.trim(raw) else raw
            if (clip.isEmpty) throw AppException(AppError.EmptyRecording)
            clip
        }
}
