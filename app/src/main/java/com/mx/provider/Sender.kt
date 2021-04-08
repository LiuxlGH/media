package com.mx.provider

import android.graphics.Bitmap
import com.mx.common.LogU
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.lang.StringBuilder
import java.lang.Thread.sleep
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.LinkedTransferQueue

class Sender {

    companion object {
        val instance: Sender by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            Sender()
        }
    }

    val queue = LinkedTransferQueue<ByteArray>()
    fun take(bmp: ByteArray) {
        if(queue.size>1) return
        if (queue.tryTransfer(bmp)) {
            queue.transfer(bmp)
        } else {
            queue.offer(bmp)
            LogU.i("bitmap queue: ${queue.size}")
        }
    }

    var running = true
    var host:String?=null
    fun launchShareCamera(afterConnected:()->Unit){
        launch("cc2cc",afterConnected)
    }
    fun launchShareScreen(afterConnected:()->Unit){
        launch("bb2bb",afterConnected)
    }
    fun launch(msg:String,afterConnected: () -> Unit) {
        LogU.i("launch sender")
        Thread(Runnable {
            while (host == null) {
                host = SocketHelper.scanControllerAddress(msg)
                LogU.i("Controller is found, address : $host")
            }
            val port = 23236

            val socket = Socket()
            val socketAddress = InetSocketAddress(InetAddress.getByName(host), port)
            if (!socket.isConnected)
                socket.connect(socketAddress)
            LogU.i("connect successfully")
            val outputStream = DataOutputStream(socket.getOutputStream())
            afterConnected.invoke()
            while (running) {
                while (queue.size == 0) {
                    sleep(100)
                }
                LogU.i("start to send bitmap")
                val b = queue.poll()
                val s1 = StringBuilder()
                for(i in 0..100){
                    s1.append(b[i].toString())
                }
                LogU.i(s1.toString())
                outputStream.writeInt(b.size)
                outputStream.write(b)
                outputStream.flush()
                LogU.i("bitmap sent, size: ${b.size}")
            }
            outputStream.close()
            socket.shutdownOutput()
            socket.close()

        }).start()
    }
}