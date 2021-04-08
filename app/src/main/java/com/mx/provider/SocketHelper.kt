package com.mx.provider

import com.blankj.utilcode.util.NetworkUtils
import com.mx.common.LogU
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.*

class SocketHelper {

    companion object {
        var retry = true
        var address =""
        fun scanControllerAddress(msg:String): String {
            val ip = NetworkUtils.getIpAddressByWifi()
            LogU.i("provider ip: $ip")
            var datagramSocket = DatagramSocket()
            val con = msg.toByteArray()
            val broadAddress = InetAddress.getByName("255.255.255.255")
            val port = 23235
            val dataPackage = DatagramPacket(con, con.size, broadAddress, port)
            Thread(Runnable {
                val receBuffer = ByteArray(1024)
                val recePacket = DatagramPacket(receBuffer, receBuffer.size)
                datagramSocket = DatagramSocket(23236)
                datagramSocket.receive(recePacket)
                val f = String(recePacket.data, 0, recePacket.length)
                if(f.contentEquals("dd4dd")){
                    address = recePacket.address.hostName
                }
                LogU.i("found address: $address")
                retry = false
            }).start()
            while (retry) {
                datagramSocket.send(dataPackage)
                LogU.i("send data")
                Thread.sleep(3000)
            }
            return address
        }

        fun send(b: ByteArray) {
            val address = ""
            val port = 23236
            val socket = Socket()
            val socketAddress = InetSocketAddress(InetAddress.getByName(address), port)
            if (!socket.isConnected)
                socket.connect(socketAddress)
            val outputStream = socket.getOutputStream()
            while (true) {
                outputStream.write(b)
//            outputStream.write()
            }
            outputStream.close()
            socket.shutdownOutput()
            socket.close()
        }
    }
}