package com.mx.provider.htsf.sender

import com.mx.common.ByteUtil
import com.mx.common.LogU
import com.mx.provider.htsf.entity.Frame
import com.mx.provider.htsf.entity.VideoInfo
import java.io.BufferedOutputStream
import java.nio.ByteBuffer

class TcpWriter(private val bos: BufferedOutputStream,
                private val sendQueue: SendQueue<VideoInfo>,
                private val disconnected: (String) -> Unit) :Thread(){
    @Volatile
    private var isStarted = true

    override fun run() {
        super.run()
        while (isStarted) {
            sendQueue.takeFrame()?.let {
                it.data.data?.let { it1 -> sendData(it1) }
            }
        }
    }

    private fun sendData(buff: ByteArray){
        kotlin.runCatching {
            LogU.i("send data size ${buff.size}")
            DataByteBuilder(0x00, 1, 11, null, buff).build().let {
                bos.write(it)
                bos.flush()
            }
        }.onFailure {
            LogU.e("failed: ${it.message}")
            isStarted = false
            disconnected(it.message?:"Fail no error")
        }
    }

    fun shutdown(){
        isStarted = false
        interrupt()
    }

    class DataByteBuilder(private val encodeVersion:Byte,
                            private val mainCmd:Int,
                            private val subCmd:Int,
                          private val body:String?,
                            private val buff:ByteArray?){
        fun build(): ByteArray {
            var bodyLength = 0
            var buffLength = 0
            body?.run {
                bodyLength = length
            }
            buff?.run {
                buffLength = size
            }
            val bb = ByteBuffer.allocate(17+bodyLength+buffLength)
            bb.put(encodeVersion)/*encode version*/
            bb.put(ByteUtil.int2Bytes(mainCmd))/*main commend*/
            bb.put(ByteUtil.int2Bytes(subCmd))/*sub commend*/
            bb.put(ByteUtil.int2Bytes(bodyLength))
            bb.put(ByteUtil.int2Bytes(buffLength))
            body?.run {
                bb.put(toByteArray())
            }
            buff?.let {
                bb.put(it)
            }
            return bb.array()
        }
    }
}