package com.hibiki.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.AppException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

class PlaybackAudioRecorder {
    private val lock = Any()
    private val stopRequested = AtomicBoolean(false)

    @Volatile private var buffering = false
    @Volatile private var recording = false
    private val bufferRequested = AtomicBoolean(false)

    private var audioRecord: AudioRecord? = null
    private var loopThread: Thread? = null
    private var rolling: RollingPcmBuffer? = null
    private var recorded = ShortCollector()
    private var recordPreamble = ShortArray(0)
    private var recordStartedAt = 0L
    private var recordMaxDurationMs = 0L
    private var recordDone: CountDownLatch? = null
    @Volatile private var recordError: Throwable? = null
    @Volatile private var recordClip: PcmClip? = null
    private var sampleRate = DEFAULT_SAMPLE_RATE

    @Volatile var onBufferingDied: (() -> Unit)? = null

    val isBuffering: Boolean get() = buffering

    fun startBuffering(
        mediaProjection: MediaProjection,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        durationMs: Int = RollingPcmBuffer.DEFAULT_DURATION_MS,
    ) {
        synchronized(lock) {
            this.sampleRate = sampleRate
            replaceRollingLocked(durationMs)
            if (buffering) return
            bufferRequested.set(true)
            buffering = true
            ensureLoopLocked(mediaProjection)
        }
    }

    fun setBufferDurationMs(durationMs: Int) {
        synchronized(lock) {
            replaceRollingLocked(durationMs)
        }
    }

    fun stopBuffering() {
        synchronized(lock) {
            bufferRequested.set(false)
            buffering = false
            rolling?.clear()
            if (!recording) {
                runCatching { audioRecord?.stop() }
            }
        }
    }

    fun requestStop() {
        stopRequested.set(true)
    }

    fun release() {
        synchronized(lock) {
            bufferRequested.set(false)
            buffering = false
            stopRequested.set(true)
            rolling?.clear()
            runCatching { audioRecord?.stop() }
        }
        loopThread?.join(1_000)
        synchronized(lock) {
            loopThread = null
        }
    }

    fun record(
        mediaProjection: MediaProjection,
        maxDurationMs: Long,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): PcmClip {
        val done = CountDownLatch(1)
        synchronized(lock) {
            if (recording) {
                throw AppException(AppError.AudioNotCapturable)
            }
            this.sampleRate = sampleRate
            stopRequested.set(false)
            recordError = null
            recordClip = null
            recordDone = done
            recordPreamble = if (buffering) rolling?.snapshot() ?: ShortArray(0) else ShortArray(0)
            recorded = ShortCollector()
            recordMaxDurationMs = maxDurationMs
            recordStartedAt = System.currentTimeMillis()
            recording = true
            try {
                ensureLoopLocked(mediaProjection)
            } catch (error: Throwable) {
                recording = false
                recordDone = null
                throw error
            }
        }
        done.await()
        recordError?.let { throw it }
        return recordClip ?: PcmClip(ShortArray(0), sampleRate)
    }

    private fun replaceRollingLocked(durationMs: Int) {
        val next = durationMs.coerceIn(RollingPcmBuffer.MIN_DURATION_MS, RollingPcmBuffer.MAX_DURATION_MS)
        if (rolling?.durationMs != next) {
            rolling = RollingPcmBuffer(sampleRate, next)
        }
    }

    private fun ensureLoopLocked(mediaProjection: MediaProjection) {
        if (loopThread?.isAlive == true && audioRecord != null) return
        val recorder = openRecorder(mediaProjection, sampleRate)
        audioRecord = recorder
        try {
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw AppException(AppError.AudioNotCapturable)
            }
        } catch (error: Throwable) {
            recorder.release()
            audioRecord = null
            buffering = false
            bufferRequested.set(false)
            if (error is AppException) throw error
            throw AppException(AppError.AudioNotCapturable, error)
        }
        loopThread = Thread({ captureLoop(recorder) }, "hibiki-audio-capture").also { it.start() }
    }

    private fun captureLoop(recorder: AudioRecord) {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(1)
        val buffer = ShortArray(minBuffer)
        var unexpectedDeath: Throwable? = null
        try {
            while (true) {
                val keepGoing = synchronized(lock) { buffering || recording }
                if (!keepGoing) break
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    synchronized(lock) {
                        if (buffering) rolling?.write(buffer, 0, read)
                        if (recording) recorded.append(buffer, 0, read)
                    }
                } else if (read < 0) {
                    val error = AppException(AppError.AudioNotCapturable)
                    failRecording(error)
                    unexpectedDeath = error
                    break
                }
                val timedOutOrStopped = synchronized(lock) {
                    recording && (
                        stopRequested.get() ||
                            System.currentTimeMillis() - recordStartedAt >= recordMaxDurationMs
                        )
                }
                if (timedOutOrStopped) {
                    finishRecording()
                }
            }
        } catch (error: Throwable) {
            unexpectedDeath = error
            failRecording(error)
        } finally {
            if (recording) {
                if (unexpectedDeath != null) failRecording(unexpectedDeath)
                else finishRecording()
            }
            val diedWhileBuffering = bufferRequested.getAndSet(false)
            synchronized(lock) {
                buffering = false
                if (audioRecord === recorder) audioRecord = null
                loopThread = null
            }
            runCatching { recorder.stop() }
            recorder.release()
            if (diedWhileBuffering) {
                onBufferingDied?.invoke()
            }
        }
    }

    private fun finishRecording() {
        synchronized(lock) {
            if (!recording) return
            recording = false
            val live = recorded.toArray()
            val combined = ShortArray(recordPreamble.size + live.size)
            recordPreamble.copyInto(combined)
            live.copyInto(combined, recordPreamble.size)
            recordClip = PcmClip(samples = combined, sampleRate = sampleRate)
            recordDone?.countDown()
            recordDone = null
        }
    }

    private fun failRecording(error: Throwable) {
        synchronized(lock) {
            if (!recording) return
            recording = false
            recordError = error
            recordDone?.countDown()
            recordDone = null
        }
    }

    private fun openRecorder(mediaProjection: MediaProjection, sampleRate: Int): AudioRecord {
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
        return recorder
    }

    private class ShortCollector {
        private var data = ShortArray(64 * 1024)
        private var size = 0

        fun append(src: ShortArray, offset: Int, length: Int) {
            if (length <= 0) return
            val needed = size + length
            if (needed > data.size) {
                var cap = data.size
                while (cap < needed) cap *= 2
                data = data.copyOf(cap)
            }
            src.copyInto(data, size, offset, offset + length)
            size += length
        }

        fun toArray(): ShortArray = data.copyOf(size)
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 44_100
    }
}
