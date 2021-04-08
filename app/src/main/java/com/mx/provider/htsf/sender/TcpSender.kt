package com.mx.provider.htsf.sender

import com.mx.common.LogU
import com.mx.provider.htsf.entity.Frame
import com.mx.provider.htsf.entity.VideoInfo
import com.mx.provider.htsf.pack.TcpPacker

class TcpSender(private val onConnected:(String)->Unit, private val disconnected:(String)->Unit,val onCmd:(Int)->Unit) {
    private val tcpConnection = TcpConnection(onConnected,disconnected,onCmd)
    private val sendQueue = ScreenRecordQueue()

    fun start(){
        tcpConnection.sendQueue = sendQueue
        tcpConnection.connect("bb2bb")
    }

    fun onData(data:ByteArray,type:Int){
        LogU.i("send data type: $type")
        val video = VideoInfo()
        video.data = data
        var frame:Frame<VideoInfo>?=null
        when(type){
            TcpPacker.FIRST_VIDEO->{
                frame = Frame(video,type,Frame.FRAME_TYPE_CONFIGURATION)
            }
            TcpPacker.KEY_FRAME->{
                frame = Frame(video,type,Frame.FRAME_TYPE_KEY_FRAME)
            }
            TcpPacker.INTER_FRAME->{
                frame = Frame(video,type,Frame.FRAME_TYPE_INTER_FRAME)
            }
            TcpPacker.AUDIO->{
                frame = Frame(video,type,Frame.FRAME_TYPE_AUDIO)
            }
        }

        frame?.let {
            sendQueue.putFrame(it)
        }
    }

    fun stop(){
        tcpConnection.stop()
    }
}