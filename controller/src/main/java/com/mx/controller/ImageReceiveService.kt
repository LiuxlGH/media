package com.mx.controller

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.blankj.utilcode.util.*
import com.mx.common.BitmapUtil
import com.mx.common.LogU
import java.io.DataInputStream
import java.lang.StringBuilder
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.nio.ByteBuffer
import kotlin.math.sign


class ImageReceiveService : Service() {
    private var signal:String?=null
    override fun onBind(intent: Intent?): IBinder? {

        Thread({
            LogU.i("start to listen")

            val ip = NetworkUtils.getIpAddressByWifi()
            LogU.i("ip: $ip")
            val socket = DatagramSocket(23235)
            val buff = ByteArray(1024)
            val packet = DatagramPacket(buff, buff.size)
            while (true) {
                socket.receive(packet)
                val d = String(packet.data)
                LogU.d("receive data: $d")
                if (d.startsWith("cc2cc")) {
                    signal ="cc2cc"
                }else if(d.startsWith("bb2bb")){
                    signal ="bb2bb"
                }
                signal?.run {
                    val c = "dd4dd".toByteArray()
                    val address = packet.address
                    val port = 23236
                    val spac = DatagramPacket(c, c.size, address, port)
                    socket.send(spac)
                    LogU.d("send dd to $address  $port")
                }
                if(signal!=null)
                break
            }
            socket.close()
        }, "Listener").start()

        Thread({
            val serverSocket = ServerSocket(23236)
            val socket = serverSocket.accept()
            val dis = DataInputStream(socket.getInputStream())
            while (running) {
                LogU.i("start")
                val t = dis.readInt()
                LogU.i("receive bitmap start: $t")
                var buff = ByteArray(1024)
                var length: Int = 0
                var progress: Int = 0
                var remain: Int = t
                var b = ByteArray(t)
                while (dis.read(buff, 0, buff.size).also { length = it } != -1) {
                    System.arraycopy(buff, 0, b, progress, length)
                    progress += length
                    remain -= length
                    if (remain == 0) {
                        break
                    }
                    if (buff.size > remain) {
                        buff = ByteArray(remain)
                    }
                }
                LogU.i("receive bitmap end: ${b.size}")
                val s = StringBuilder()
                for(i in 0..100){
                    s.append(b[i].toString())
                }
                LogU.i(s.toString())
                ThreadUtils.executeByIo(DecodeTask(b))
                LogU.i("end")
            }
        }, "ImageReceiver").start()
        if(mBinder==null){
            mBinder = MBinder()
        }
        return mBinder
    }

    inner class DecodeTask(val b: ByteArray): ThreadUtils.Task<Bitmap>() {
        init {
            LogU.i("decode start")
        }
        override fun doInBackground(): Bitmap? {
            when(signal){
                "bb2bb"->return ConvertUtils.bytes2Bitmap(b)
                "cc2cc"->return decodeNv21(b)!!
                else -> return null
            }
//            return NV21ToBitmap(this@ImageReceiveService).nv21ToBitmap(b,ScreenUtils.getAppScreenWidth(),ScreenUtils.getAppScreenHeight())
            /**byte2Byffer(b).let {
                var bitmap = Bitmap.createBitmap(1080 + 32 / 4, 2160, Bitmap.Config.ARGB_4444);
                    bitmap.copyPixelsFromBuffer(it);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, 1080, 2160);
                return bitmap
            }
            return Bitmap.createBitmap(1080,2160,Bitmap.Config.ARGB_4444)**/
        }
        fun byte2Byffer(byteArray: ByteArray): ByteBuffer? {

            //初始化一个和byte长度一样的buffer
            val buffer: ByteBuffer = ByteBuffer.allocate(byteArray.size)
            // 数组放到buffer中
            buffer.put(byteArray)
            //重置 limit 和postion 值 否则 buffer 读取数据不对
            buffer.flip()
            return buffer
        }
        override fun onSuccess(result: Bitmap?) {
            handler?.let {
                val m = it.obtainMessage(MSG_IMG)
                m.obj = result
                it.sendMessage(m)
                LogU.i("decode on success")
            }
        }

        override fun onCancel() {

        }

        override fun onFail(t: Throwable?) {
            LogU.e(t?.localizedMessage.toString())
        }

    }

    private var mBinder:Binder?=null

    var running = true
    var handler:Handler?=null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return super.onStartCommand(intent, flags, startId)
    }

    inner class MBinder : Binder() {
        fun getService():ImageReceiveService{
            return this@ImageReceiveService
        }
    }

    companion object{
        val MSG_IMG = 0X31
    }

    private val sw = ScreenUtils.getAppScreenWidth()
    private val sh = ScreenUtils.getAppScreenHeight()
    private fun decodeNv21(nv21:ByteArray):Bitmap?{
        val time2 = System.currentTimeMillis()
        val ww = nv21.copyOfRange(0,2)
        val hh = nv21.copyOfRange(2,4)
        val w = (ww[0].toInt() shl 8) xor (ww[1].toInt() and 0xFF)
        val h = (hh[0].toInt() shl 8) xor (hh[1].toInt() and 0xFF)
        val bm: Bitmap = BitmapUtil.getBitmapImageFromYUV(nv21.copyOfRange(4,nv21.size), w, h)
        val time3 = System.currentTimeMillis()
        val m = Matrix()
        val c = if(h.toFloat()/sw>w.toFloat()/sh) sw.toFloat()/h else sh.toFloat()/w
        m.setScale(c,c)
        m.setRotate(90f, bm.width.toFloat() / 2, bm.height.toFloat() / 2)
        val result = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, m, true)
        LogU.i(" 2-3:" + (time3 - time2))
        return result
    }
}