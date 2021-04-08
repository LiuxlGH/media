package com.mx.provider

import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.TextureView.SurfaceTextureListener
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ToastUtils
import com.mx.common.LogU
import com.mx.provider.databinding.ActivityMainBinding
import com.mx.provider.htsf.pack.TcpPacker
import com.mx.provider.htsf.recorder.CameraRecorder
import com.mx.provider.htsf.recorder.ScreenRecorder
import com.mx.provider.htsf.render.RenderDispatcher
import com.mx.provider.htsf.sender.Processor
import com.mx.provider.htsf.sender.TcpSender


class MainActivity : AppCompatActivity() {

    private var type = 0
    private var cameraProcessor:Processor?=null
    private var screenProcessor:Processor?=null
    private var processor:Processor?=null
    var sender:TcpSender?=null
    lateinit var binding:ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tryStartScreenShot()

        screenProcessor = ScreenRecorder()
        (screenProcessor as ScreenRecorder).apply {
            message = {
                runOnUiThread {
                    binding.tvMsg.text = it
                }
            }
            processor = this
        }

        cameraProcessor = CameraRecorder(binding.ttv).apply {
            message = {
                runOnUiThread {
                    binding.tvMsg.text = it
                }
            }
        }

        sender = TcpSender({
            processor?.start(sender)
        }, {
            processor?.stop()
        }){
            when(it){
                2-> {
                    if (screenProcessor == null) {

                    }
                    processor = screenProcessor
                        (processor as ScreenRecorder).start(getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager, screenResultCode!!, screenData!!)

                }
                4-> {
                    if(cameraProcessor==null) {

                    }
                    processor = cameraProcessor
                }
                else -> null
            }
        }
        sender?.start()


        binding.btnShareScreen.setOnClickListener {
            Sender.instance.launchShareScreen {
                type = 1
                tryStartScreenShot()
            }
        }
        binding.btnShareCamera.setOnClickListener {
            Sender.instance.launchShareCamera {
                CameraPreviewYUVProvider(this, binding.iv).openCamera()
            }
        }
        binding.btnShareScreen2.setOnClickListener{
            type = 2
            tryStartScreenShot()
        }
        binding.btnShareCamera2.setOnClickListener {
            CameraRecorder(binding.ttv).apply {
                message = {
                    runOnUiThread {
                        binding.tvMsg.text = it
                    }
                }
//                start()
            }
        }
    }
    private val TAG= "MainActivity"

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val context = applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(cameraId)
    }

    /** Readers used as buffers for camera still shots */
    private lateinit var imageReader: ImageReader

    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [cameraThread] */
    private val cameraHandler = Handler(cameraThread.looper)

    /** The [CameraDevice] that will be opened in this fragment */
    private lateinit var camera: CameraDevice

    private var cameraId = "1"
    private var pixelFormat=ImageFormat.JPEG

    private fun tryStartScreenShot() {
        val mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager?
        if (mProjectionManager != null) {
            LogU.i("start to request permission")
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
        }
    }

    private val REQUEST_MEDIA_PROJECTION = 0xA1
    private var screenResultCode:Int?=null
    private var screenData:Intent?=null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION && data != null) {
            if (resultCode == RESULT_OK) {
                LogU.i("Permission is granted, start service")
                screenData = data
                screenResultCode = resultCode
                when(type){
                    1 -> {
                        startService(Intent(this, ActionAccessibilityService::class.java).apply {
                            putExtra("D", data)
                        })
                    }
                    2 -> {
                        if (processor == null|| processor !is ScreenRecorder) {
                            screenProcessor = ScreenRecorder()
                            (screenProcessor as ScreenRecorder).apply {
                                message = {
                                    runOnUiThread {
                                        binding.tvMsg.text = it
                                    }
                                }
                                processor = this
                            }.start(getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager, resultCode, data)
                        } else {
                            (processor as ScreenRecorder).start(getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager, resultCode, data)
                        }
                    }
                }
//                finish()
            } else if (resultCode == RESULT_CANCELED) {
            }
        }
    }
}

class FormatItem(s: String, id: String?, rawSensor: Int) {

}
