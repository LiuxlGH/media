package com.mx.controller.player

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import androidx.viewbinding.ViewBinding
import com.mx.common.LogU
import com.mx.controller.databinding.ActivityPlayBinding

class PlayActivity : AppCompatActivity() {
    private var mDecodeThread: DecodeThread?=null
    private lateinit var videoMediaCodec: VideoMediaCodec
    private lateinit var mHolder: SurfaceHolder
    private var mServer: TcpServer?=null

    private lateinit var vb:ActivityPlayBinding
    companion object{
        val playQueue = ReceiveQueue()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.btnCamera2.setOnClickListener{
            startServer("cc2cc")
        }
        vb.btnScreen2.setOnClickListener{
            startServer("ss2ss")
        }

        mHolder = vb.svPlay.holder
        mHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder?) {
                LogU.i("surface created")
                    videoMediaCodec = VideoMediaCodec(holder)
                    videoMediaCodec.start()
                    mDecodeThread = DecodeThread(videoMediaCodec.codec,playQueue)
                    mDecodeThread!!.start()

            }

            override fun surfaceChanged(
                holder: SurfaceHolder?,
                format: Int,
                width: Int,
                height: Int
            ) {
                LogU.i("surface changed")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                LogU.i("surface destroyed")
                videoMediaCodec.release()
                mDecodeThread?.isPlaying = false
                mServer?.stop()
            }
        })
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        LogU.i("Play destroy")
    }

    fun startServer(cmd:String){
            mServer = TcpServer().apply {
                onConnected = {
                    runOnUiThread {
                        vb.btnCamera2.visibility = View.INVISIBLE
                        vb.btnScreen2.visibility = View.INVISIBLE
                    }
                }
                onDisconnected={
                    runOnUiThread {
                        vb.btnCamera2.visibility = View.VISIBLE
                        vb.btnScreen2.visibility = View.VISIBLE
                    }
                    playQueue.clear()
                }
            }
            mServer!!.start(cmd)

    }
}