package com.mx.provider.htsf.render

import android.media.MediaCodec
import com.mx.common.LogU
import com.mx.provider.htsf.config.AudioConfiguration
import com.mx.provider.htsf.encoder.AudioEncoder
import com.mx.provider.htsf.encoder.AudioUtils
import java.nio.ByteBuffer
import java.util.*

class AudioDispatcher(private val onAudioEncoded:(ByteBuffer,MediaCodec.BufferInfo)->Unit) {

    private val mAudioConfiguration by lazy {
        AudioConfiguration.createDefault()
    }
    private val mAudioRecord by lazy {
        AudioUtils.getAudioRecord(mAudioConfiguration)
    }
    var mute = true
    var isStart = true
    private val mRecordBufferSize by lazy {
        AudioUtils.getRecordBufferSize(mAudioConfiguration)
    }
    private val mRecordBuffer by lazy {
        ByteArray(mRecordBufferSize)
    }
    private lateinit var mAudioEncoder: AudioEncoder
    fun start() {
        isStart = true
        LogU.i("start to record audio")
        mAudioRecord.startRecording()
        mAudioEncoder = AudioEncoder(mAudioConfiguration!!, onAudioEncoded)

        Thread({
            while (isStart) {
                mAudioRecord.read(mRecordBuffer,0,mRecordBufferSize).takeIf {
                    it>0
                }?.apply {
                    if(mute){
                        Arrays.fill(mRecordBuffer,0)
                    }
                    mAudioEncoder.offerEncoder(mRecordBuffer)
                }
            }
        }, "RecordAudio").start()
    }
    fun stop(){
        isStart = false
        mAudioEncoder.stop()
        mAudioRecord.stop()
    }
}