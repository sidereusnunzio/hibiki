package com.hibiki.data.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

object AacEncoder {
    fun encodeToM4a(clip: PcmClip, output: File, bitRate: Int = 64_000) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, clip.sampleRate, clip.channelCount)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096)

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        try {
            val pcmBytes = shortsToBytes(clip.samples)
            var offset = 0
            var presentationTimeUs = 0L
            val bytesPerSample = 2 * clip.channelCount
            val frameBytes = clip.sampleRate / 50 * bytesPerSample
            var inputFinished = false
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                if (!inputFinished) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        inputBuffer.clear()
                        if (offset >= pcmBytes.size) {
                            codec.queueInputBuffer(inputIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputFinished = true
                        } else {
                            val remaining = pcmBytes.size - offset
                            val toCopy = minOf(remaining, inputBuffer.remaining(), frameBytes)
                            inputBuffer.put(pcmBytes, offset, toCopy)
                            val pts = presentationTimeUs
                            presentationTimeUs += (toCopy / bytesPerSample) * 1_000_000L / clip.sampleRate
                            offset += toCopy
                            codec.queueInputBuffer(inputIndex, 0, toCopy, pts, 0)
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputIndex >= 0 -> {
                        val encoded = codec.getOutputBuffer(outputIndex)
                        if (encoded != null && bufferInfo.size > 0 && muxerStarted) {
                            encoded.position(bufferInfo.offset)
                            encoded.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encoded, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            break
                        }
                    }
                }
                if (inputFinished && outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // keep draining
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            muxer.release()
        }
    }

    private fun shortsToBytes(samples: ShortArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        var i = 0
        for (sample in samples) {
            bytes[i++] = (sample.toInt() and 0xFF).toByte()
            bytes[i++] = (sample.toInt() shr 8 and 0xFF).toByte()
        }
        return bytes
    }
}
