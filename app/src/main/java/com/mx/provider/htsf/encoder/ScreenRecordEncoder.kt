package com.mx.provider.htsf.encoder

import android.annotation.TargetApi
import android.media.MediaCodec
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.mx.common.LogU
import com.mx.provider.htsf.config.VideoConfiguration
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock

class ScreenRecordEncoder(private val videoConfig: VideoConfiguration, private val onVideoEncode:(ByteBuffer?,MediaCodec.BufferInfo)->Unit) {

    private var mBufferInfo: MediaCodec.BufferInfo?=null
    private lateinit var mHandler: Handler
    private lateinit var mHandlerThread: HandlerThread

    @Volatile
    private var isStarted = false
    private val encodeLock = ReentrantLock()

    private val mMediaCodec: MediaCodec? by lazy<MediaCodec?> {
        VideoMediaCodec.getVideoMediaCodec(videoConfig)
    }

    private var mPause = false

    fun start() {
        mHandlerThread = HandlerThread("SREncoder")
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
        mBufferInfo = MediaCodec.BufferInfo()
        mMediaCodec?.start()
        mHandler.post(swapDataRunnable)
        isStarted = true
    }

    fun stop(){
        kotlin.runCatching {
            isStarted = false
            mHandler.removeCallbacks(null)
            mHandlerThread.quit()
            encodeLock.lock()
            mBufferInfo=null
            mMediaCodec?.signalEndOfInputStream()
            releaseEncoder()
            encodeLock.unlock()
            LogU.i("Screen encoder stopped")
        }.onFailure {
            LogU.e("screen record encoder stop failed")
        }
    }
    fun setPause(pause: Boolean) {
        mPause = pause
    }

    private fun releaseEncoder(){
        mMediaCodec?.run {
            stop()
            release()
            null
        }
    }
    fun getSurface():Surface{
        return mMediaCodec!!.createInputSurface()
    }

    private val swapDataRunnable = Runnable { drainEncoder() }

    private fun drainEncoder() {
        while (isStarted) {
            encodeLock.lock()
            if (mMediaCodec != null) {
                val outBufferIndex = mMediaCodec!!.dequeueOutputBuffer(mBufferInfo!!, 12000)
                if (outBufferIndex >= 0) {
                    LogU.i("media codec out put buffer")
                    val bb = mMediaCodec!!.getOutputBuffer(outBufferIndex)
                    takeIf {
                        !mPause
                    }?.apply {
                        onVideoEncode(bb,mBufferInfo!!)
                    }
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

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun setRecorderBps(bps: Int) {
        if (mMediaCodec == null) {
            return
        }
        val bitrate = Bundle()
        bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps * 1024)
        //        bitrate.putInt(MediaCodec.);
        mMediaCodec!!.setParameters(bitrate)
    }
}