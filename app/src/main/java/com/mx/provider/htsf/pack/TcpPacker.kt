package com.mx.provider.htsf.pack

import android.media.MediaCodec
import com.mx.common.LogU
import java.nio.ByteBuffer

class TcpPacker(val onPacked: (ByteArray, Int) -> Unit) : AnnexbHelper.AnnexbNaluListener {
    companion object {
        const val HEADER = 0
        const val METADATA = 1
        const val FIRST_VIDEO = 2
        const val AUDIO = 4
        const val KEY_FRAME = 5
        const val INTER_FRAME = 6
    }

    private var isHeaderWrite = false
    private var isKeyFrameWrite = false
    private var mAnnexbHelper = AnnexbHelper()
    private lateinit var mSpsPps: ByteArray
    private val header = byteArrayOf(0x00, 0x00, 0x00, 0x01) //H264的头文件

    init {
        mAnnexbHelper.setAnnexbNaluListener(this)
    }

    fun doVideoPack(bb: ByteBuffer, bi: MediaCodec.BufferInfo){
        mAnnexbHelper.analyseVideoDataonlyH264(bb, bi)
    }

    override fun onSpsPps(sps: ByteArray, pps: ByteArray) {
        val byteBuffer: ByteBuffer = ByteBuffer.allocate(sps.size + 4)
        byteBuffer.put(header)
        byteBuffer.put(sps)
        mSpsPps = byteBuffer.array()

        onPacked(mSpsPps, FIRST_VIDEO)
        val byteBuffer1: ByteBuffer = ByteBuffer.allocate(pps.size + 4)
        byteBuffer1.put(header)
        byteBuffer1.put(pps)
        onPacked(byteBuffer1.array(), FIRST_VIDEO)
        isHeaderWrite = true
    }

    override fun onVideo(data: ByteArray, isKeyFrame: Boolean) {
        LogU.i("packer on video start")
        if (!isHeaderWrite) {
            return
        }
        var packetType = INTER_FRAME
        if (isKeyFrame) {
            isKeyFrameWrite = true
            packetType = KEY_FRAME
        }
        //确保第一帧是关键帧，避免一开始出现灰色模糊界面
        if (!isKeyFrameWrite) {
            return
        }
        val bb: ByteBuffer
        if (isKeyFrame) {
            bb = ByteBuffer.allocate(data.size)
            bb.put(data)
        } else {
            bb = ByteBuffer.allocate(data.size)
            bb.put(data)
        }
        onPacked(bb.array(), packetType)
    }

    fun doAudioPack(bb:ByteBuffer,bi:MediaCodec.BufferInfo){
        bb.position(bi.offset)
        bb.limit(bi.offset + bi.size)
        val audio = ByteArray(bi.size)
        bb.get(audio)
        //一般第一帧都是2个字节
        //一般第一帧都是2个字节
        val length = 7 + audio.size
        val tempBb = ByteBuffer.allocate(length + 4)
        tempBb.put(header)
        tempBb.put(getADTSHeader(length))
        tempBb.put(audio)
        onPacked(tempBb.array(), AUDIO)
    }

    /**
     * 给编码出的aac裸流添加adts头字段
     *
     * @param packetLen
     */
    fun getADTSHeader(packetLen: Int): ByteArray? {
        val packet = ByteArray(7)
        val profile = 2 //AAC LC
        val freqIdx = 4 //16.0KHz
        val chanCfg = 2 //CPE 声道数
        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte()
        packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
        packet[4] = (packetLen and 0x7FF shr 3).toByte()
        packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
        return packet
    }
}