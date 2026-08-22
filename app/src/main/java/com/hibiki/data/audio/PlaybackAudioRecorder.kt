package com.hibiki.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.AppException
import java.util.concurrent.atomic.AtomicBoolean

class PlaybackAudioRecorder {
    private val stopRequested = AtomicBoolean(false)

    fun requestStop() {
        stopRequested.set(true)
    }

    fun record(
        mediaProjection: MediaProjection,
        maxDurationMs: Long,
        sampleRate: Int = 44_100,
    ): PcmClip {
        stopRequested.set(false)
        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
        if (minBuffer <= 0) {
            throw AppException(AppError.AudioNotCapturable)
        }
        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channel)
            .build()
        val recorder = try {
            AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(minBuffer * 2)
                .setAudioPlaybackCaptureConfig(captureConfig)
                .build()
        } catch (error: SecurityException) {
            throw AppException(AppError.AudioNotCapturable, error)
        } catch (error: IllegalArgumentException) {
            throw AppException(AppError.AudioNotCapturable, error)
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw AppException(AppError.AudioNotCapturable)
        }

        val collected = ArrayList<Short>()
        val buffer = ShortArray(minBuffer)
        val started = System.currentTimeMillis()
        try {
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw AppException(AppError.AudioNotCapturable)
            }
            while (!stopRequested.get() && System.currentTimeMillis() - started < maxDurationMs) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    for (i in 0 until read) collected += buffer[i]
                } else if (read < 0) {
                    throw AppException(AppError.AudioNotCapturable)
                }
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
        return PcmClip(samples = collected.toShortArray(), sampleRate = sampleRate)
    }
}
