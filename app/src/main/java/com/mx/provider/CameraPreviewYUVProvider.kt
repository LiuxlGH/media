package com.mx.provider

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.widget.ImageView
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.mx.common.BitmapUtil
import com.mx.common.LogU
import java.util.*

class CameraPreviewYUVProvider(val act: Activity, val iv: ImageView) {

    private lateinit var mPreviewBuilder: CaptureRequest.Builder
    private lateinit var mCameraDevice: CameraDevice
    private lateinit var mHandler:Handler
    init {
        HandlerThread("camera").let {
            it.start()
            mHandler = Handler(it.looper)
        }
    }

    private var mCameraId: String? = null
    private var mImageReader: ImageReader? = null
    var previewSize = Size(1080, 540)
    fun openCamera() {
        val cameraManager = act.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in cameraManager.cameraIdList) {
                //描述相机设备的属性类
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                //获取是前置还是后置摄像头
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                //使用后置摄像头
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    if (map != null) {
                        val sizeMap = map.getOutputSizes(SurfaceTexture::class.java)
                        val sizes = StringBuilder()
                        for (size in sizeMap) {
                            sizes.append(size.width).append(" | ").append(size.height).append("     ")
                        }
                        LogU.d("size->$sizes")
                        mCameraId = cameraId
                    }
                    mImageReader = ImageReader.newInstance(previewSize.width, previewSize.height,
                            ImageFormat.YUV_420_888, 2)
                    mImageReader?.setOnImageAvailableListener(mOnImageAvailableListener, mHandler)
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
//                val surfaceTexture: SurfaceTexture = mTextureView.getSurfaceTexture()
//                surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
//                val previewSurface = Surface(surfaceTexture)
                                        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//                mPreviewBuilder.addTarget(previewSurface)
                                        mPreviewBuilder.addTarget(mImageReader!!.surface)
                                        mCameraDevice.createCaptureSession(listOf(mImageReader!!.surface), mStateCallBack, mHandler)
                                    } catch (e: CameraAccessException) {
                                        e.printStackTrace()
                                    }
                                    LogU.d("mStateCallback----onOpened---")
                                }

                                override fun onDisconnected(camera: CameraDevice) {
                                    LogU.d("mStateCallback----onDisconnected---")
                                    camera.close()
                                }

                                override fun onError(camera: CameraDevice, error: Int) {
                                    LogU.d("mStateCallback----onError---$error")
                                    camera.close()
                                }
                            }, mHandler)
                        }

                        override fun onDenied() {
                        }

                    }).request()
                }
            }
        } catch (r: CameraAccessException) {
        }
    }
    var index = 0
    private val mOnImageAvailableListener = OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        getByteArrayFromImage(image)?.let {
            Sender.instance.take(it)
        }
        image.close()
    }
    fun getByteArrayFromImage(image: Image): ByteArray? {
        val time1 = System.currentTimeMillis()
        val w = image.width
        val h = image.height
//        LogU.i("image w:$w, h:$h")
        val i420Size = w * h * 3 / 2
        val picel1 = ImageFormat.getBitsPerPixel(ImageFormat.NV21)
        val picel2 = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
        val planes = image.planes
        //remaining0 = rowStride*(h-1)+w => 27632= 192*143+176
        val remaining0 = planes[0].buffer.remaining()
        val remaining1 = planes[1].buffer.remaining()
        //remaining2 = rowStride*(h/2-1)+w-1 =>  13807=  192*71+176-1
        val remaining2 = planes[2].buffer.remaining()
        //获取pixelStride，可能跟width相等，可能不相等
        val pixelStride = planes[2].pixelStride
        val rowOffest = planes[2].rowStride
        val nv21 = ByteArray(i420Size+4)
        val yRawSrcBytes = ByteArray(remaining0)
        val uRawSrcBytes = ByteArray(remaining1)
        val vRawSrcBytes = ByteArray(remaining2)
        planes[0].buffer[yRawSrcBytes]
        planes[1].buffer[uRawSrcBytes]
        planes[2].buffer[vRawSrcBytes]
        nv21[0] = (w shr 8).toByte()
        nv21[1] = (w and 0xff).toByte()
        nv21[2] = (h shr 8).toByte()
        nv21[3] = (h and 0xff).toByte()
        if (pixelStride == w) {
            //两者相等，说明每个YUV块紧密相连，可以直接拷贝
            System.arraycopy(yRawSrcBytes, 0, nv21, 4, rowOffest * h)
            System.arraycopy(vRawSrcBytes, 0, nv21, 4+rowOffest * h, rowOffest * h / 2 - 1)
        } else {
            val ySrcBytes = ByteArray(w * h)
            val uSrcBytes = ByteArray(w * h / 2 - 1)
            val vSrcBytes = ByteArray(w * h / 2 - 1)
            for (row in 0 until h) {
                //源数组每隔 rowOffest 个bytes 拷贝 w 个bytes到目标数组
                System.arraycopy(yRawSrcBytes, rowOffest * row, ySrcBytes, w * row, w)

                //y执行两次，uv执行一次
                if (row % 2 == 0) {
                    //最后一行需要减一
                    if (row == h - 2) {
                        System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w - 1)
                    } else {
                        System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w)
                    }
                }
            }
            System.arraycopy(ySrcBytes, 0, nv21, 4, w * h)
            System.arraycopy(vSrcBytes, 0, nv21, 4+w * h, w * h / 2 - 1)
        }
        val time2 = System.currentTimeMillis()
        val bm: Bitmap = BitmapUtil.getBitmapImageFromYUV(nv21.copyOfRange(4,nv21.size), w, h)
        val time3 = System.currentTimeMillis()
        val m = Matrix()
        m.setRotate(90f, bm.width.toFloat() / 2, bm.height.toFloat() / 2)
        val result = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, m, true)

        LogU.d("1-2:" + (time2 - time1) + " 2-3:" + (time3 - time2))
        act.runOnUiThread {
            iv.setImageBitmap(result)
        }
        return nv21
    }
    private val mStateCallBack: CameraCaptureSession.StateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            try {
//                session.capture(request, mSessionCaptureCallback, mCameraHandler);
                mPreviewBuilder.set<Int>(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                val request: CaptureRequest = mPreviewBuilder.build()
                // Finally, we start displaying the camera preview.
                session.setRepeatingRequest(request, null, mHandler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {}
    }

}