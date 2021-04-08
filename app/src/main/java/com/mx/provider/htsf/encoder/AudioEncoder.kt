package com.mx.provider.htsf.encoder

import android.media.MediaCodec
import com.mx.provider.htsf.config.AudioConfiguration
import java.nio.ByteBuffer

class AudioEncoder(
    private val mAudioConfiguration: AudioConfiguration,
    val onAudioEncode: (ByteBuffer, MediaCodec.BufferInfo) -> Unit
) {

    private val mBufferInfo: MediaCodec.BufferInfo by lazy {
        MediaCodec.BufferInfo()
    }
    private val mMediaCodec = AudioMediaCodec.getAudioMediaCodec(mAudioConfiguration)

    init {
        mMediaCodec?.start()
    }

    @Synchronized
    fun stop(){

    }

    @Synchronized
    fun offerEncoder(input: ByteArray) {
        if (mMediaCodec == null) {
            return
        }
        val inputBuffers = mMediaCodec.inputBuffers
        val outputBuffers = mMediaCodec.outputBuffers
        val inputBufferIndex = mMediaCodec.dequeueInputBuffer(12000)
        if (inputBufferIndex >= 0) {
            val inputBuffer = inputBuffers[inputBufferIndex]
            inputBuffer.clear()
            inputBuffer.put(input)
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.size, 0, 0)
        }
        var outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 12000)
        while (outputBufferIndex >= 0) {
            val outputBuffer = outputBuffers[outputBufferIndex]
            onAudioEncode(outputBuffer, mBufferInfo)
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false)
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0)
        }
    }
}