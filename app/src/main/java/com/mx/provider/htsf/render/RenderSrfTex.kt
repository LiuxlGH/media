package com.mx.provider.htsf.render

import android.opengl.*
import com.mx.common.GlUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class RenderSrfTex(private val mFboTexId:Int,private val mEncodeDispatcher: EncodeDispatcher) {
    private var mVideoHeight: Int = 0
    private var mVideoWidth: Int = 0
    private var mSavedEglDisplay: EGLDisplay? = null
    private var mSavedEglDrawSurface: EGLSurface? = null
    private var mSavedEglReadSurface: EGLSurface? = null
    private var mSavedEglContext: EGLContext? = null

    private var mProgram = -1
    private var maPositionHandle = -1
    private var maTexCoordHandle = -1
    private var muSamplerHandle = -1
    private var muPosMtxHandle = -1

    private val mSymmetryMtx = GlUtil.createIdentityMtx()
    private val mNormalMtx = GlUtil.createIdentityMtx()

    private val mNormalVtxBuf = GlUtil.createVertexBuffer()
    private val mNormalTexCoordBuf = GlUtil.createTexCoordBuffer()

    private var mCameraTexCoordBuffer: FloatBuffer? = null

    fun setVideoSize(width: Int, height: Int) {
        mVideoWidth = width
        mVideoHeight = height
        initCameraTexCoordBuffer()
    }

    private fun initCameraTexCoordBuffer() {
        val cameraWidth: Int = 2160
        val cameraHeight: Int = 1080
       /* val cameraData: CameraData = CameraHolder.instance().getCameraData()
        val width: Int = cameraData.cameraWidth
        val height: Int = cameraData.cameraHeight
        if (CameraHolder.instance().isLandscape()) {
            cameraWidth = Math.max(width, height)
            cameraHeight = Math.min(width, height)
        } else {
            cameraWidth = Math.min(width, height)
            cameraHeight = Math.max(width, height)
        }*/
        val hRatio = mVideoWidth / cameraWidth.toFloat()
        val vRatio = mVideoHeight / cameraHeight.toFloat()
        val ratio: Float
        if (hRatio > vRatio) {
            ratio = mVideoHeight / (cameraHeight * hRatio)
            val vtx = floatArrayOf( //UV
                    0f, 0.5f + ratio / 2,
                    0f, 0.5f - ratio / 2,
                    1f, 0.5f + ratio / 2,
                    1f, 0.5f - ratio / 2)
            val bb = ByteBuffer.allocateDirect(4 * vtx.size)
            bb.order(ByteOrder.nativeOrder())
            mCameraTexCoordBuffer = bb.asFloatBuffer()
            mCameraTexCoordBuffer!!.put(vtx)
            mCameraTexCoordBuffer!!.position(0)
        } else {
            ratio = mVideoWidth / (cameraWidth * vRatio)
            val vtx = floatArrayOf( //UV
                    0.5f - ratio / 2, 1f,
                    0.5f - ratio / 2, 0f,
                    0.5f + ratio / 2, 1f,
                    0.5f + ratio / 2, 0f)
            val bb = ByteBuffer.allocateDirect(4 * vtx.size)
            bb.order(ByteOrder.nativeOrder())
            mCameraTexCoordBuffer = bb.asFloatBuffer()
            mCameraTexCoordBuffer!!.put(vtx)
            mCameraTexCoordBuffer!!.position(0)
        }
    }

    private val encodeProgram by lazy {
        SurfaceProgram()
    }
    fun draw(){
//        saveRenderState()
        GlUtil.checkGlError("draw_S")

        if (mEncodeDispatcher.isFirstSetup()) {
            mEncodeDispatcher.startSwapData()
            mEncodeDispatcher.makeCurrent()
            initGL()
        } else {
            mEncodeDispatcher.makeCurrent()
        }

//        GLES20.glViewport(0, 0, mVideoWidth, mVideoHeight)
//
//        GLES20.glClearColor(0f, 0f, 0f, 1f)
//        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
//
//        GLES20.glUseProgram(mProgram)
//
//        mNormalVtxBuf.position(0)
//        GLES20.glVertexAttribPointer(maPositionHandle,
//                3, GLES20.GL_FLOAT, false, 4 * 3, mNormalVtxBuf)
//        GLES20.glEnableVertexAttribArray(maPositionHandle)
//
//        mCameraTexCoordBuffer!!.position(0)
//        GLES20.glVertexAttribPointer(maTexCoordHandle,
//                2, GLES20.GL_FLOAT, false, 4 * 2, mCameraTexCoordBuffer)
//        GLES20.glEnableVertexAttribArray(maTexCoordHandle)
//
//        GLES20.glUniform1i(muSamplerHandle, 0)
//
//        //处理前置摄像头镜像
//        /*val cameraData: CameraData = CameraHolder.instance().getCameraData()
//        if (cameraData != null) {
//            val facing: Int = cameraData.cameraFacing
//            if (muPosMtxHandle >= 0) {
//                if (facing == CameraData.FACING_FRONT) {
//                    GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mSymmetryMtx, 0)
//                } else {
//                    GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mNormalMtx, 0)
//                }
//            }
//        }*/
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTexId)
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        //绘制纹理
//        drawWatermark()

//        mEncodeDispatcher.swapBuffers()

        GlUtil.checkGlError("draw_E")

//        restoreRenderState()

    }
    private fun initGL() {
        GlUtil.checkGlError("initGL_S")
        val vertexShader =
                //
                "attribute vec4 position;\n" +
                        "attribute vec4 inputTextureCoordinate;\n" +
                        "varying   vec2 textureCoordinate;\n" +
                        "uniform   mat4 uPosMtx;\n" +
                        "void main() {\n" +
                        "  gl_Position = uPosMtx * position;\n" +
                        "  textureCoordinate   = inputTextureCoordinate.xy;\n" +
                        "}\n"

        val fragmentShader =  //
                //
                "precision mediump float;\n" +
                        "uniform sampler2D uSampler;\n" +
                        "varying vec2 textureCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(uSampler, textureCoordinate);\n" +
                        "}\n";
        mProgram = GlUtil.createProgram(vertexShader, fragmentShader)
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position")
        maTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate")
        muSamplerHandle = GLES20.glGetUniformLocation(mProgram, "uSampler")
        muPosMtxHandle = GLES20.glGetUniformLocation(mProgram, "uPosMtx")
        Matrix.scaleM(mSymmetryMtx, 0, -1f, 1f, 1f)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_BLEND)
        GlUtil.checkGlError("initGL_E")
    }

    private fun saveRenderState() {
        mSavedEglDisplay = EGL14.eglGetCurrentDisplay()
        mSavedEglDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
        mSavedEglReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ)
        mSavedEglContext = EGL14.eglGetCurrentContext()
    }

    private fun restoreRenderState() {
        if (!EGL14.eglMakeCurrent(
                        mSavedEglDisplay,
                        mSavedEglDrawSurface,
                        mSavedEglReadSurface,
                        mSavedEglContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }
}