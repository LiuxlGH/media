package com.mx.controller.player

import android.media.MediaCodec
import android.util.Log
import com.mx.common.LogU
import com.mx.controller.player.entity.Frame
import java.nio.ByteBuffer

class DecodeThread(val mediaCodec: MediaCodec, val receQueue: ReceiveQueue):Thread() {

    var isPlaying = true
    private val videoPlay by lazy {
        VideoPlay(mediaCodec)
    }
    private val audioPlay by lazy {
        AudioPlay()
    }

    override fun run() {
        super.run()

        while (isPlaying){
            val frame = receQueue.take()
            when(frame.type){
                Frame.KEY_FRAME,
                Frame.NORMAL_FRAME -> {
                    kotlin.runCatching {
                        videoPlay.decodeH264(frame.bytes)
                    }
                }
                Frame.SPSPPS -> {
                    ByteBuffer.allocate(frame.sps.size + frame.pps.size).apply {
                        put(frame.sps)
                        put(frame.pps)
                        kotlin.runCatching {
                            videoPlay.decodeH264(array())
                        }
                    }
                }
                Frame.AUDIO_FRAME -> {
                    try {
                        audioPlay.playAudio(frame.bytes, 0, frame.bytes.size)
                    } catch (e: Exception) {
                        LogU.e("audio Exception$e")
                    }
                }

            }
        }
    }
}