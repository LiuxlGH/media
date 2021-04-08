package com.mx.provider.htsf.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import com.mx.common.createVideoFormat
import com.mx.provider.htsf.config.VideoConfiguration
import java.io.IOException
import kotlin.math.ceil


class VideoMediaCodec {
    companion object{
        fun getVideoMediaCodec(videoConfiguration: VideoConfiguration):MediaCodec?{
            /*val mMF = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, getVideoSize(1080), getVideoSize(2160))
            mMF.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface )

            mMF.setInteger(MediaFormat.KEY_BIT_RATE, 600 * 1024)
            mMF.setInteger(MediaFormat.KEY_FRAME_RATE, 10)
//            if (mPrimeColorFormat != 0){
//            }
            mMF.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3); //关键帧间隔时间 单位s
//            mMF.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, (1000000 / 45).toLong())

//            mMF.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
//                        mMF.setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ)
            var mMC = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mMC.configure(mMF, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            return mMC*/
            val videoWidth: Int = VideoMediaCodec.getVideoSize(videoConfiguration.width)
            val videoHeight: Int = VideoMediaCodec.getVideoSize(videoConfiguration.height)
            if (Build.MANUFACTURER.equals("XIAOMI", ignoreCase = true)) {
                videoConfiguration.maxBps = 500
                videoConfiguration.fps = 10
                videoConfiguration.ifi = 3
            }
            val format = MediaFormat.createVideoFormat(videoConfiguration.mime, videoWidth, videoHeight)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, videoConfiguration.maxBps * 1024)
            var fps = videoConfiguration.fps
            //TODO:BLACK LIST HELPER
            //设置摄像头预览帧率
//            if (BlackListHelper.deviceInFpsBlacklisted()) {
//                SopCastLog.d(SopCastConstant.TAG, "Device in fps setting black list, so set mediacodec fps 15")
//                fps = 15
//            }
            format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoConfiguration.ifi)
            // -----------------ADD BY XU.WANG 当画面静止时,重复最后一帧--------------------------------------------------------
            format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, (1000000 / 45).toLong())
            //------------------MODIFY BY XU.WANG 为解决MIUI9.5花屏而增加...-------------------------------
            /*VBR：Variable BitRate，动态比特率，其码率可以随着图像的复杂程度的不同而变化，因此其编码效率比较高，Motion发生时，马赛克很少。码率控制算法根据图像内容确定使用的比特率，图像内容比较简单则分配较少的码率(似乎码字更合适)，图像内容复杂则分配较多的码字，这样既保证了质量，又兼顾带宽限制。这种算法优先考虑图像质量。

               ABR：Average BitRate，平均比特率 是VBR的一种插值参数。ABR在指定的文件大小内，以每50帧 （30帧约1秒）为一段，低频和不敏感频率使用相对低的流量，高频和大动态表现时使用高流量，可以做为VBR和CBR的一种折衷选择。

                CBR：Constant BitRate，是以恒定比特率方式进行编码，有Motion发生时，由于码率恒定，只能通过增大QP来减少码字大小，图像质量变差，当场景静止时，图像质量又变好，因此图像质量不稳定。优点是压缩速度快，缺点是每秒流量都相同容易导致空间浪费。

                CVBR：Constrained Variable it Rate，VBR的一种改进，兼顾了CBR和VBR的优点：在图像内容静止时，节省带宽，有Motion发生时，利用前期节省的带宽来尽可能的提高图像质量，达到同时兼顾带宽和图像质量的目的。这种方法通常会让用户输入最大码率和最小码率，静止时，码率稳定在最小码率，运动时，码率大于最小码率，但是又不超过最大码率。
————————————————*/
            if (Build.MANUFACTURER.equals("XIAOMI", ignoreCase = true)) {
                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            } else {
                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
            }
            format.setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ)
            var mediaCodec: MediaCodec? = null
            try {
                mediaCodec = MediaCodec.createEncoderByType(videoConfiguration.mime)
                mediaCodec.reset()
                mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            } catch (e: Exception) {
                e.printStackTrace()
                if (mediaCodec != null) {
                    mediaCodec.stop()
                    mediaCodec.release()
                    mediaCodec = null
                }
            }
            return mediaCodec
        }

        fun getVideoMediaCodec2(width:Int, height:Int): MediaCodec {
            val f = createVideoFormat(Size(width, height), CodecCapabilities.COLOR_FormatSurface,
                    6000000, 25, 5)
            try {
                // Mime 决定输出数据格式，这里的AVC代表H264
                return MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
                    configure(f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                }
            } catch (e: IOException) {
                throw RuntimeException("code c init failed $e")
            }
        }

        fun getVideoSize(size: Int): Int {
            val multiple = ceil(size / 16.0).toInt()
            return multiple * 16
        }

        private fun getSupportColorFormat(): Int {
            val numCodecs = MediaCodecList.getCodecCount()
            var codecInfo: MediaCodecInfo? = null
            var i = 0
            while (i < numCodecs && codecInfo == null) {
                val info = MediaCodecList.getCodecInfoAt(i)
                if (info.isEncoder) {
                    val types = info.supportedTypes
                    var found = false
                    var j = 0
                    while (j < types.size && !found) {
                        if (types[j] == "video/avc") {
                            found = true
                        }
                        j++
                    }
                    if (found) {
                        codecInfo = info
                    }
                }
                i++
            }
            val capabilities = codecInfo!!.getCapabilitiesForType("video/avc")
            val j = capabilities.colorFormats
            for (types in j.indices) {
                val colorFormat = j[types]
                when (colorFormat) {
                    CodecCapabilities.COLOR_FormatYUV420Planar, CodecCapabilities.COLOR_FormatYUV420PackedPlanar, CodecCapabilities.COLOR_FormatYUV420SemiPlanar, CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar, CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar, CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar -> return colorFormat
                }
            }
            return -1
        }
    }
}