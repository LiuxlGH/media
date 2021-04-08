package com.mx.provider.htsf.render

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import com.mx.common.LogU
import com.mx.common.createVideoFormat
import com.mx.common.handleOutputBuffer
import com.mx.provider.htsf.config.VideoConfiguration
import com.mx.provider.htsf.encoder.VideoMediaCodec
import java.io.IOException
import java.nio.ByteBuffer

class RenderDispatcher(private val mConfiguration: VideoConfiguration, private val onVideoEncoded: (bb: ByteBuffer, bi: MediaCodec.BufferInfo)->Unit,onSurfaceCreated:(s:Surface)->Unit) {
    private var mMediaCodec: MediaCodec

    var mSurface:Surface?=null
    var suf:Surface?=null

    private val encodeCore by lazy {
        SurfaceRenderCore(mConfiguration.width, mConfiguration.height)
    }

    private val bufferInfo by lazy {
        MediaCodec.BufferInfo()
    }
    private val mediaFormat = createVideoFormat(Size(mConfiguration.width, mConfiguration.height), MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
            8000000, 25, 3)
    init {
/*        try {
            // Mime 决定输出数据格式，这里的AVC代表H264
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        } catch (e: IOException) {
            throw RuntimeException("code c init failed $e")
        }
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)*/
        mMediaCodec = VideoMediaCodec.getVideoMediaCodec(mConfiguration)!!
        suf = mMediaCodec.createInputSurface()
        val txtu = encodeCore.buildEGLSurface(suf!!)
        mSurface = Surface(txtu)
        onSurfaceCreated.invoke(mSurface!!)
    }
    private var isStarted = false
    fun start() {
        mMediaCodec.start()
        isStarted = true
        while (isStarted) {
            drainEncoder(false)
            encodeCore.draw()
            encodeCore.swapData(null)
        }
        drainEncoder(true)
        mMediaCodec.stop()
        mMediaCodec?.release()
        encodeCore.release()
        mSurface?.release()
        mSurface=null
        suf?.release()
        suf = null
    }
    fun stop(){
        isStarted = false
    }
    private fun drainEncoder(isEnd: Boolean = false) {
        if (isEnd) {
            mMediaCodec?.signalEndOfInputStream()
        }
        mMediaCodec?.handleOutputBuffer(bufferInfo, 12000, {
        }, {
            //            手动设置帧间隔
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            val bundle = Bundle()
//            bundle.putLong(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
//            codec.setParameters(bundle)
//            }

            val encodedData = mMediaCodec.getOutputBuffer(it)
            LogU.i("sent " + bufferInfo.size + " bytes to muxer")
            onVideoEncoded.invoke(encodedData!!,bufferInfo)
            /*if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                bufferInfo.size = 0
            }
            if (bufferInfo.size != 0) {
                LogU.i("buffer info offset ${bufferInfo.offset} time is ${bufferInfo.presentationTimeUs} ")
                encodedData?.apply {
                    position(bufferInfo.offset)
                    limit(bufferInfo.offset + bufferInfo.size)
                }
            }*/

            mMediaCodec?.releaseOutputBuffer(it, false)
        }, !isEnd)
    }
}