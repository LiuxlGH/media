package com.mx.provider.htsf.render

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.mx.provider.htsf.config.VideoConfiguration
import com.mx.provider.htsf.encoder.VideoMediaCodec
import com.mx.provider.htsf.render.gl.EglHelper
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class RecordRender(private val encodeDispatcher:EncodeDispatcher) : GLSurfaceView.Renderer,SurfaceTexture.OnFrameAvailableListener{
    private lateinit var mRenderSrfTex: RenderSrfTex
    private var mTextureId: Int?=null
    var surfaceTexture:SurfaceTexture

    private val eglHelper = EglHelper()

    init {
//        eglHelper.initEgl(encodeDispatcher.)

//        encodeDispatcher.isFirstSetup()
            initTextureId()
            surfaceTexture = SurfaceTexture(mTextureId!!)
            surfaceTexture.setOnFrameAvailableListener(this)

    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(1f, 1f, 1f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun initTextureId() {
        val txtutes = IntArray(1)
        GLES20.glGenTextures(1, txtutes, 0)
        mTextureId = txtutes[0]
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId!!)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat());//设置MIN 采样方式
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat());//设置MAG采样方式
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat());//设置S轴拉伸方式
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat());//设置T轴拉伸方式


        mRenderSrfTex = RenderSrfTex(mTextureId!!, encodeDispatcher).apply {
            VideoConfiguration.createDefault(true).let {
                setVideoSize(VideoMediaCodec.getVideoSize(it.width),VideoMediaCodec.getVideoSize(it.height))
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        mRenderSrfTex.draw();
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
    }

}