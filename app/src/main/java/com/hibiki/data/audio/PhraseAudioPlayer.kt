package com.hibiki.data.audio

import android.content.Context
import android.media.MediaPlayer

class PhraseAudioPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    fun play(path: String) {
        stop()
        player = MediaPlayer().apply {
            setDataSource(path)
            setOnCompletionListener { stop() }
            prepare()
            start()
        }
    }

    fun stop() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }
}
