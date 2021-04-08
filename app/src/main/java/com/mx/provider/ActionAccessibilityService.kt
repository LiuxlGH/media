package com.mx.provider

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.blankj.utilcode.util.ThreadUtils
import com.mx.common.LogU
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList

class ActionAccessibilityService : AccessibilityService(), ScreenShotHelper.OnScreenShotListener {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        LogU.i("event package name: ${event?.packageName}")
    }

    override fun onInterrupt() {
        LogU.i("action accessibility on interrupt")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // 截屏的回调
        val screenShotHelper = ScreenShotHelper(this, Activity.RESULT_OK, intent?.getParcelableExtra("D"), this)
        screenShotHelper.startScreenShot()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun searchView(x: Int, y: Int): AccessibilityNodeInfo? {
        val node = rootInActiveWindow
        val nodes:ArrayList<AccessibilityNodeInfo> = ArrayList()
        searchNode(node, x, y, nodes)

        var fNode:AccessibilityNodeInfo? = null
        for(n in nodes){
            if(fNode==null){
                fNode = n
            }
        }
        return null
    }

    private fun searchNode(
            node: AccessibilityNodeInfo,
            x: Int,
            y: Int, list: ArrayList<AccessibilityNodeInfo>
    ) {
        val c = node.childCount
        if(c==0){
            list.add(node)
        }else {
            for (i in 0 until c) {
                val n = node.getChild(i)
                val r = Rect()
                node.getBoundsInScreen(r)
                if (x >= r.left && x <= r.right && y >= r.bottom && y <= r.top) {
                    searchNode(n, x, y, list)
                }
            }
        }
    }

    var lastShotTime=0L
    override fun onShotFinish(bitmap: ByteArray?) {
//        bitmap?.let {
//            val t = Calendar.getInstance().timeInMillis
//            if(t-lastShotTime<100){
//                return
//            }
//            lastShotTime = t
//            ThreadUtils.executeByIo(EncodePostTask(it))
//        }
        bitmap?.let {
            LogU.i("shot finished bitmap length: ${it.size}")
            Sender.instance.take(it)
        }
    }

    inner class EncodePostTask(val bitmap:Bitmap):ThreadUtils.Task<ByteArray>() {
        init {
            LogU.i("Encode task start")
        }
        override fun doInBackground(): ByteArray {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.WEBP, 20, baos)
            return baos.toByteArray()
        }

        override fun onSuccess(result: ByteArray?) {
            result?.let {
                LogU.i("shot finished bitmap length: ${it.size}")
                Sender.instance.take(it)
            }
        }

        override fun onCancel() {
        }

        override fun onFail(t: Throwable?) {
        }

    }

}