package com.mx.controller.player

import android.os.SystemClock
import com.mx.common.LogU
import com.mx.controller.player.entity.Frame
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import kotlin.experimental.and

class DataReceiveTask(val socket: Socket, private val readSpeed:(String)->Unit={}) {

    private var mSps: ByteArray?=null
    private var mPPs: ByteArray?=null
    private lateinit var inputStream:InputStream
    private lateinit var outputStream:OutputStream
    init {
        kotlin.runCatching {
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
        }
    }
    var isStart = true
    private val mAnalyticDataUtils = AnalyticDataUtils().apply {
        setOnAnalyticDataListener {
            readSpeed(it)
        }
    }

    fun run() {
        kotlin.runCatching {
            while (isStart) {
                val head = mAnalyticDataUtils.readByte(inputStream, 17)
                if (head == null || head.isEmpty()) {
                    SystemClock.sleep(10)
                    continue
                }
                val header = mAnalyticDataUtils.analysisHeader(head)
                if (header == null || header.buffSize == 0) {
                    SystemClock.sleep(10)
                    continue
                }
                if (header.encodeVersion != (0x00.toByte())) {
                    continue
                }
                val headData = mAnalyticDataUtils.analyticData(inputStream, header)
                if (headData == null || headData.buff == null) continue
                /* */
                decodeFrame(headData.buff)
            }
        }.onFailure {
            onCatch.invoke(it)
        }
    }
    lateinit var onCatch:(Throwable)->Unit

    private fun decodeFrame(frame: ByteArray) {
        var isKeyFrame = false
        when{
            // ignore the nalu type aud(9)
            isAccessUnitDelimiter(frame) -> {

            }
            //pps
            isPps(frame) -> {
                mPPs = frame
                mSps?.let { onSpsPps(it, mPPs!!) }
            }
            //sps
            isSps(frame)->{
                mSps = frame
                mPPs?.let { onSpsPps(mSps!!, it) }
            }
            //
            isAudio(frame)->{
                val temp = ByteArray(frame.size - 4)
                System.arraycopy(frame, 4, temp, 0, frame.size - 4)
                onVideo(temp, Frame.AUDIO_FRAME)
            }

            //IDR
            else ->{
                onVideo(frame,if(isKeyFrame(frame)) Frame.KEY_FRAME else Frame.NORMAL_FRAME)
            }
        }
     }

    lateinit var onFrame:(Frame)->Unit

    private fun onVideo(data: ByteArray, t: Int) {
        Frame().apply {
            when(t){
                Frame.KEY_FRAME->{
                    type = Frame.KEY_FRAME
                }
                Frame.NORMAL_FRAME->{
                    type = Frame.NORMAL_FRAME
                }
                Frame.AUDIO_FRAME->{
                    type = Frame.AUDIO_FRAME
                }
            }
            bytes = data
            onFrame(this)
        }
    }

    private fun onSpsPps(sps: ByteArray, pps: ByteArray) {
        LogU.i("sps pps")
        Frame().apply{
            type =Frame.SPSPPS
            this.sps = sps
            this.pps = pps
            onFrame(this)
        }
    }

    private fun isKeyFrame(frame: ByteArray): Boolean {
        if (frame.size < 5) {
            return false
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        val nal_unit_type = (frame[4] and 0x1f).toInt()
        return nal_unit_type == IDR
    }
    private fun isAudio(frame: ByteArray): Boolean {
        return if (frame.size < 5) {
            false
        } else frame[4] == 0xFF.toByte() && frame[5] == 0xF9.toByte()
    }
    private fun isSps(frame: ByteArray): Boolean {
        if (frame.size < 5) {
            return false
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        val nal_unit_type = (frame[4] and 0x1f).toInt()
        return nal_unit_type == SPS
    }

    private fun isPps(frame: ByteArray): Boolean {
        if (frame.size < 5) {
            return false
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        val nal_unit_type = (frame[4] and 0x1f).toInt()
        return nal_unit_type == PPS
    }
    private fun isAccessUnitDelimiter(frame: ByteArray): Boolean {
        if (frame.size < 5) {
            return false
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        val nal_unit_type = frame[4] and 0x1f
        return nal_unit_type.toInt() == AccessUnitDelimiter
    }

    // Coded slice of a non-IDR picture slice_layer_without_partitioning_rbsp( )
    val NonIDR = 1

    // Coded slice of an IDR picture slice_layer_without_partitioning_rbsp( )
    val IDR = 5

    // Supplemental enhancement information (SEI) sei_rbsp( )
    val SEI = 6

    // Sequence parameter set seq_parameter_set_rbsp( )
    val SPS = 7

    // Picture parameter set pic_parameter_set_rbsp( )
    val PPS = 8

    // Access unit delimiter access_unit_delimiter_rbsp( )
    val AccessUnitDelimiter = 9
    fun shutdown() {
        kotlin.runCatching {
            socket.close()
            isStart = false
        }
    }

}