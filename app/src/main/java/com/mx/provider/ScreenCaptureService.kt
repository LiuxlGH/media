package com.mx.provider

import android.app.Service
import android.content.Intent
import android.media.ImageReader
import android.os.IBinder


class ScreenCaptureService :Service(){
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    private class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            try {
                reader.acquireLatestImage()?.let {

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}