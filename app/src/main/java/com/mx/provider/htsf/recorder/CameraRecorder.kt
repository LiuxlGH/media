package com.mx.provider.htsf.recorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.Utils
import com.mx.common.LogU
import com.mx.provider.htsf.config.VideoConfiguration
import com.mx.provider.htsf.encoder.ScreenRecordEncoder
import com.mx.provider.htsf.pack.TcpPacker
import com.mx.provider.htsf.render.EncodeDispatcher
import com.mx.provider.htsf.render.RecordRender
import com.mx.provider.htsf.render.RenderDispatcher
import com.mx.provider.htsf.render.SurfaceRenderCore
import com.mx.provider.htsf.sender.Processor
import com.mx.provider.htsf.sender.TcpSender


class CameraRecorder(private val mtxtView: TextureView):Processor {
    private lateinit var mPreviewBuilder: CaptureRequest.Builder
    private var mCameraDevice: CameraDevice?=null
    private var mHandler: Handler
    init {
        HandlerThread("camera").let {
            it.start()
            mHandler = Handler(it.looper)
        }
    }
    lateinit var message:(String)->Unit
    lateinit var surface:Surface
//    var sender: TcpSender? = null
    var render:RenderDispatcher?=null
    override fun start(sender: TcpSender?) {
        val videoConfig = VideoConfiguration.createDefault(true)
        val packer = TcpPacker() { data, type ->
            sender?.onData(data, type)
        }
//        sender = TcpSender({
            message("connected:")
            render = RenderDispatcher(videoConfig, { bb, bi ->
                bb?.let { packer.doVideoPack(it, bi) }
            }) { s ->
                mSurface = s
                openCamera()
            }
            render?.start()
//        }, {
//        }){
//            onCmd
//        }

        /*val encodeDispatcher = EncodeDispatcher(videoConfig) { bb, bi ->
                bb?.let { packer.doVideoPack(it, bi) }
        }*/
//        encodeDispatcher.start()
//        val render = RecordRender(encodeDispatcher)
//        sender?.start()


    }

    override fun stop() {
        message("disconnect")
        render?.stop()
        mCameraDevice?.close()
    }

    lateinit var onCmd:(Int)->Unit
    private lateinit var mSurface:Surface
    private var mCameraId: String? = null
    private var mImageReader: ImageReader? = null
    var previewSize = Size(VideoConfiguration.DEFAULT_WIDTH, VideoConfiguration.DEFAULT_HEIGHT)
    val cameraManager = Utils.getApp().getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun openCamera() {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                //描述相机设备的属性类
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                //获取是前置还是后置摄像头
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                //使用后置摄像头
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    if (map != null) {
                        val sizeMap = map.getOutputSizes(SurfaceTexture::class.java)
                        val sizes = StringBuilder()
                        for (size in sizeMap) {
                            sizes.append(size.width).append(" | ").append(size.height).append("     ")
                        }
                        LogU.i("size->$sizes")
                        mCameraId = cameraId
                    }
                    mImageReader = ImageReader.newInstance(previewSize.width, previewSize.height,
                            ImageFormat.YUV_420_888, 2)
                    mImageReader?.setOnImageAvailableListener(onImageAvailableListener, mHandler)
                    val params = arrayOf(Manifest.permission.CAMERA)
//                    if (!PermissionUtil.checkPermission(mContext, params)) {
//                        PermissionUtil.requestPermission(mContext, "", 0, params)
//                    }
                    PermissionUtils.permission(PermissionConstants.CAMERA).callback(object : PermissionUtils.SimpleCallback {
                        @SuppressLint("MissingPermission")
                        override fun onGranted() {
                            cameraManager.openCamera(mCameraId!!, object : CameraDevice.StateCallback() {
                                override fun onOpened(camera: CameraDevice) {
                                    try {
                                        mCameraDevice = camera
                                        val surfaceTexture: SurfaceTexture = mtxtView.surfaceTexture
//                                        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
                                        val previewSurface = Surface(surfaceTexture)
                                        mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
//                                        mPreviewBuilder.addTarget(previewSurface)
//                                        mPreviewBuilder.addTarget(mImageReader!!.surface)
                                        mPreviewBuilder.addTarget(mSurface)
                                        mCameraDevice!!.createCaptureSession(listOf(mSurface), mStateCallBack, mHandler)
                                    } catch (e: CameraAccessException) {
                                        e.printStackTrace()
                                    }
                                    LogU.i("mStateCallback----onOpened---")
                                }

                                override fun onDisconnected(camera: CameraDevice) {
                                    LogU.i("mStateCallback----onDisconnected---")
                                    camera.close()
                                }

                                override fun onError(camera: CameraDevice, error: Int) {
                                    LogU.e("mStateCallback----onError---$error")
                                    camera.close()
                                }
                            }, mHandler)
                        }

                        override fun onDenied() {
                            ToastUtils.showShort("denied")
                        }

                    }).request()
                }
            }
        } catch (r: CameraAccessException) {
            LogU.e("CameraAccessException ${r.message!!}")
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        ToastUtils.showShort("aa")

    }
    private val mStateCallBack: CameraCaptureSession.StateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            try {
//                session.capture(request, mSessionCaptureCallback, mCameraHandler);
                mPreviewBuilder.set<Int>(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                adaptFpsRange(30, mPreviewBuilder);
                val request: CaptureRequest = mPreviewBuilder.build()
                // Finally, we start displaying the camera preview.
                session.setRepeatingRequest(request, null, mHandler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {}
    }

    private fun adaptFpsRange(expectedFps: Int, builderInputSurface: CaptureRequest.Builder) {
        val fpsRanges: Array<Range<Int>>? = getSupportedFps()
        if (fpsRanges != null && fpsRanges.size > 0) {
            var closestRange: Range<Int> = fpsRanges[0]
            var measure: Int = Math.abs(closestRange.getLower() - expectedFps) + Math.abs(
                    closestRange.getUpper() - expectedFps)
            for (range in fpsRanges) {
                if (range.getLower() <= expectedFps && range.getUpper() >= expectedFps) {
                    val curMeasure: Int = Math.abs(range.getLower() - expectedFps) + Math.abs(range.getUpper() - expectedFps)
                    if (curMeasure < measure) {
                        closestRange = range
                        measure = curMeasure
                    }
                }
            }
            builderInputSurface.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, closestRange)
        }
    }

    fun getSupportedFps(): Array<Range<Int>>? {
        return try {
            val characteristics: CameraCharacteristics = getCameraCharacteristics() ?: return null
            characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        } catch (e: IllegalStateException) {
            null
        }
    }
    fun getCameraCharacteristics(): CameraCharacteristics? {
        return try {
            cameraManager.getCameraCharacteristics(java.lang.String.valueOf(mCameraId))
        } catch (e: CameraAccessException) {
            null
        }
    }


}