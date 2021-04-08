package com.mx.provider.htsf.sender

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket

class NetServer(val onCmd:(Int)->Unit) {

    fun start(){

    }
    fun stop(){

    }
}

class WaitDiscoveryTask(val onCmd:(Int)->Unit):Runnable{
    var address:String?=null
    override fun run() {
        val socket = DatagramSocket(23235)
        val buff = ByteArray(1024)
        val pack = DatagramPacket(buff,buff.size)
        while (address==null){
            socket.receive(pack)
            when(val c = String(pack.data).substring(0,5)){
                "ss2ss"->{
                    address = pack.address.hostName
                    onCmd(2)
                }
                "cc2cc"->{
                    address = pack.address.hostName
                    onCmd(4)
                }
            }
        }
    }

}

class SendDataTask:Runnable{
    override fun run() {
    }
}

