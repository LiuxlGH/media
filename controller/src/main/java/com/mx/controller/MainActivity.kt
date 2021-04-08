package com.mx.controller

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.Surface
import android.view.TextureView
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.mx.common.LogU
import com.mx.controller.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        a = ActivityMainBinding.inflate(layoutInflater)
        setContentView(a.root)

        bindService(Intent(this,ImageReceiveService::class.java),conn, BIND_AUTO_CREATE)

        a.ivClose.setOnClickListener{
            a.iv.visibility = GONE
        }

        a.ttvDisplay.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                LogU.i("surface available w: $width h: $height")
                c = Surface(surface)
//                c.drawBitmap()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

        }
    }

    private lateinit var a: ActivityMainBinding
    var c:Surface?=null
    var p = Paint()
    private val mHandler = Handler() {
        when (it.what) {
            ImageReceiveService.MSG_IMG -> {
                val b = it.obj as Bitmap
                LogU.i("canvas start to draw bitmap")
                val cc = c?.lockCanvas(null)
                cc?.drawBitmap(b,0f,0f,p)
                c?.unlockCanvasAndPost(cc!!)

                a.iv.takeIf {iv->
                    iv.visibility == VISIBLE
                }?.setImageBitmap(b)
                true
            }
            else -> {
                false
            }
        }
    }

    private val conn = object:ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            (service as ImageReceiveService.MBinder).getService().handler = mHandler
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(conn)
    }
}