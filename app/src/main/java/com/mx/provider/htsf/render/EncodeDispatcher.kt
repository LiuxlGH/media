package com.mx.provider.htsf.render

import android.media.MediaCodec
import android.os.Handler
import android.os.HandlerThread
import com.mx.provider.htsf.config.VideoConfiguration
import com.mx.provider.htsf.encoder.VideoMediaCodec
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock

class EncodeDispatcher(private val mConfiguration: VideoConfiguration, private val onVideoEncoded: (bb: ByteBuffer, bi: MediaCodec.BufferInfo) -> Unit) {
    private var isStarted = false
    private lateinit var mBufferInfo: MediaCodec.BufferInfo
    private var mMediaCodec: MediaCodec? = null
    private val encodeLock = ReentrantLock()
    private lateinit var mHandlerThread: HandlerThread
    private lateinit var mEncoderHandler: Handler
    private var mInputSurface:InputSurface? = null
    fun start(){
        if(mMediaCodec!=null||mInputSurface!=null){
            throw RuntimeException("init media codec more than one time")
            return
        }
        mMediaCodec = VideoMediaCodec.getVideoMediaCodec(mConfiguration)
        mHandlerThread = HandlerThread("EncodeHandler")
        mHandlerThread.start()
        mEncoderHandler = Handler(mHandlerThread.looper)
        mBufferInfo = MediaCodec.BufferInfo()
        isStarted = true
    }
    fun stop(){
        isStarted = false
        mEncoderHandler.removeCallbacks(null)
        mHandlerThread.quit()
        encodeLock.lock()
        releaseEncoder()
        encodeLock.unlock()
    }
    fun releaseEncoder(){
        mMediaCodec?.run {
            signalEndOfInputStream()
            stop()
            release()
            mMediaCodec = null
        }
        mInputSurface?.let {
            it.release()
            mInputSurface = null
        }
    }
    fun swapBuffers(){
        mMediaCodec?.let {
            mInputSurface!!.swapBuffers()
            mInputSurface!!.setPresentationTime(System.nanoTime())
        }
    }
    fun startSwapData(){
        mEncoderHandler.post {
            drainEncoder()
        }
    }
    fun makeCurrent(){
        mInputSurface?.makeCurrent()
    }
    fun isFirstSetup():Boolean{
        if(mMediaCodec==null||mInputSurface!= null){
            return false
        }
//        kotlin.runCatching {
            mInputSurface = InputSurface(mMediaCodec!!.createInputSurface())
            mMediaCodec!!.start()
//        }.onFailure {
//            releaseEncoder()
//        }
        return true
    }
    private fun drainEncoder() {
        val outBuffers: Array<ByteBuffer> = mMediaCodec?.outputBuffers!!
        while (isStarted) {
            encodeLock.lock()
            if (mMediaCodec != null) {
                val outBufferIndex: Int = mMediaCodec!!.dequeueOutputBuffer(mBufferInfo, 12000)
                if (outBufferIndex >= 0) {
                    val bb = outBuffers[outBufferIndex]
                    onVideoEncoded(bb, mBufferInfo)
                    mMediaCodec!!.releaseOutputBuffer(outBufferIndex, false)
                } else {
                    try {
                        // wait 10ms
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                encodeLock.unlock()
            } else {
                encodeLock.unlock()
                break
            }
        }
    }
}