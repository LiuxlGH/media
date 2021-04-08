package com.mx.provider.htsf.recorder

import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.view.Surface
import com.mx.provider.htsf.config.VideoConfiguration
import com.mx.provider.htsf.encoder.ScreenRecordEncoder
import com.mx.provider.htsf.pack.TcpPacker
import com.mx.provider.htsf.sender.Processor
import com.mx.provider.htsf.sender.TcpSender

class ScreenRecorder():Processor {

    private var mVirtualDisplay: VirtualDisplay?=null
    private var mMediaProjection: MediaProjection?=null

    private var encoder:ScreenRecordEncoder?=null
    lateinit var message:(String)->Unit

    var sender:TcpSender?=null
    private var surface:Surface?=null
    fun start(mediaProjectionManage: MediaProjectionManager, resultCode: Int, data: Intent) {
        mMediaProjection = mediaProjectionManage.getMediaProjection(resultCode, data)
        val videoConfig = VideoConfiguration.createDefault(false)
        val width = videoConfig.width
        val height = videoConfig.height
        val packer = TcpPacker(){data,type->
            sender?.onData(data,type)
        }
        encoder = ScreenRecordEncoder(videoConfig){bb,bi,->
            bb?.let { packer.doVideoPack(it,bi) }
        }
        surface = encoder?.getSurface()
//        sender = TcpSender({
//            encoder?.start()
//        },{
//            encoder?.stop()
//        }){
//            onCmd
//        }
//        sender.start()
        mVirtualDisplay = mMediaProjection?.createVirtualDisplay("ScreenRecorder",width,height,
                1,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,surface,null,null)
    }
    lateinit var onCmd:(Int)->Unit
    override fun start(sender: TcpSender?) {
//                val videoConfig = VideoConfiguration.createHD()
//        val width = videoConfig.width
//        val height = videoConfig.height
////        var sender:TcpSender?=null
//        val packer = TcpPacker(){data,type->
//            sender?.onData(data,type)
//        }
//        val encoder = ScreenRecordEncoder(videoConfig){bb,bi,->
//            bb?.let { packer.doVideoPack(it,bi) }
//        }
//        val surface = encoder?.getSurface()
//        mVirtualDisplay = mMediaProjection?.createVirtualDisplay("ScreenRecorder",width,height,
//                1,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,surface,null,null)

        message("s connected")
        this.sender = sender
        Thread.sleep(1000)
        encoder?.start()
    }

    override fun stop() {
        message("s disconnected")
        encoder?.stop()
        encoder=null
        mMediaProjection?.stop()
        mMediaProjection=null
        mVirtualDisplay?.release()
        mVirtualDisplay=null
        surface?.release()
    }
}