package com.mx.common;

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import android.util.Size
import com.mx.common.GlUtil.checkGlError
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

fun createFloatBuffer(array: FloatArray): FloatBuffer {
    val buffer = ByteBuffer
            // 分配顶点坐标分量个数 * Float占的Byte位数
            .allocateDirect(array.size * 4)
            // 按照本地字节序排序
            .order(ByteOrder.nativeOrder())
            // Byte类型转Float类型
            .asFloatBuffer()

    // 将Dalvik的内存数据复制到Native内存中
    buffer.put(array).position(0)
    return buffer
}
/**
 * 创建一个纹理对象，并且和ES绑定
 *
 * 生成camera特殊的Texture
 * 在Android中Camera产生的preview texture是以一种特殊的格式传送的，
 * 因此shader里的纹理类型并不是普通的sampler2D,而是samplerExternalOES,
 * 在shader的头部也必须声明OES 的扩展。除此之外，external OES的纹理和Sampler2D在使用时没有差别
 * */
fun buildTextureId(target: Int = GLES11Ext.GL_TEXTURE_EXTERNAL_OES): Int {
    val ids = IntArray(1)
    GLES20.glGenTextures(1, ids, 0)
    checkGlError("create texture check")
    val id = ids[0]
    bindSetTexture(target, id)
    return id
}

private fun bindSetTexture(target: Int, id: Int) {
    // 这里的绑定纹理是将GPU的纹理数据和ID对应起来，载入纹理到此ID处
    // 渲染时绑定纹理，是绑定纹理ID到激活的纹理单元
    GLES20.glBindTexture(target, id)
    checkGlError("bind texture : $id check")

    //----设置纹理参数----
    // 纹理过滤器放大缩小
    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)

    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
}

fun unBindTexture(target: Int = GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
    GLES20.glBindTexture(target, GLES20.GL_NONE)
}

/**
 * 释放纹理
 * */
fun releaseTexture(id: IntArray) {
    GLES20.glDeleteTextures(id.size, id, 0)
}

/**
 * 释放帧缓冲和纹理
 * */
fun releaseFrameBufferTexture(frame: IntArray, textureId: IntArray) {
    GLES20.glDeleteFramebuffers(1, frame, 0)
    releaseTexture(textureId)
}

/**
 *
 * @param needEnd when bufferId is INFO_TRY_AGAIN_LATER, is need to break loop
 * */
fun MediaCodec.handleOutputBuffer(bufferInfo: MediaCodec.BufferInfo, defTimeOut: Long,
                                  formatChanged: () -> Unit = {},
                                  render: (bufferId: Int) -> Unit,
                                  needEnd: Boolean = true) {
    loopOut@ while (true) {
        //  获取可用的输出缓存队列
        val outputBufferId = dequeueOutputBuffer(bufferInfo, defTimeOut)
        Log.d("handleOutputBuffer", "output buffer id : $outputBufferId ")
        if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (needEnd) {
                break@loopOut
            }
        } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            formatChanged.invoke()
        } else if (outputBufferId >= 0) {
            render.invoke(outputBufferId)
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                break@loopOut
            }
        }
    }
}


fun createVideoFormat(size: Size, colorFormat: Int = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
                      bitRate: Int, frameRate: Int, iFrameInterval: Int): MediaFormat {
    return MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, size.width, size.height)
            .apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)

                // 大部分机型无效
//                setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
//                setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel11)
//                setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ)

                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            }
}
