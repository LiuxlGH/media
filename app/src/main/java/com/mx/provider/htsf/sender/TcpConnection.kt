package com.mx.provider.htsf.sender

import com.blankj.utilcode.util.NetworkUtils
import com.mx.common.LogU
import com.mx.provider.htsf.entity.VideoInfo
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.*

class TcpConnection(val connected:(String)->Unit, private val disconnected:(String)->Unit, val onCmd:(Int)->Unit) {
    fun connect(msg: String){
        waitDis{
            doConnect(it)
        }
    }
    private var datagramSocket:DatagramSocket?=null
    private var mSocket:Socket?=null
    private lateinit var mWrite: TcpWriter
    lateinit var sendQueue:SendQueue<VideoInfo>

    private fun doConnect(host: String) {
        val port = 23236

        mSocket = Socket()
        val socketAddress = InetSocketAddress(InetAddress.getByName(host), port)
        while (true)  {
            val r = takeIf { !mSocket!!.isConnected }?.runCatching {
                mSocket!!.connect(socketAddress)
            }?.onFailure {
                LogU.e(it.message ?: "connection failed")
            }?.onSuccess {
                LogU.i("connect successfully")
            }?.isFailure
            if(r == true){
                Thread.sleep(1000)
                LogU.i("reconnect $host")
                continue
            }else{
                break
            }
        }
        output = BufferedOutputStream(mSocket!!.getOutputStream())
        input = BufferedInputStream(mSocket!!.getInputStream())
        mWrite = TcpWriter(output, sendQueue){
            disconnected.invoke(it)
            datagramSocket?.close()
            mSocket?.close()
            address=""
            Thread.sleep(300)
            sendQueue.clear()
            Thread.sleep(300)
            connect("")
        }
        mWrite.start()
        connected(host)
    }
    private lateinit var input: BufferedInputStream
    private lateinit var output: BufferedOutputStream

    @Volatile
    private var retry = true
    var address =""
    private fun waitDis(onFoundHost:(String)->Unit){
        Thread({
            val socket = DatagramSocket(23235)
            val buff = ByteArray(1024)
            val pack = DatagramPacket(buff,buff.size)
            while (address.isEmpty()){
                LogU.i("wait new data")
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
            socket.close()
            LogU.i("socket state: ${socket.isClosed}")
            onFoundHost(address)
        },"WaitDis").start()
    }
    private fun search(msg:String,onFoundHost:(String)->Unit){
        Thread({
            val ip = NetworkUtils.getIpAddressByWifi()
            LogU.i("provider ip: $ip")
            var datagramSocket = DatagramSocket()
            val con = msg.toByteArray()
            val broadAddress = InetAddress.getByName("255.255.255.255")
            val port = 23235
            val dataPackage = DatagramPacket(con, con.size, broadAddress, port)
            while (retry) {
                datagramSocket.send(dataPackage)
                LogU.i("send data")
                Thread.sleep(3000)
            }
        },"SearchHost").start()

        Thread({
            val receBuffer = ByteArray(1024)
            val recePacket = DatagramPacket(receBuffer, receBuffer.size)
            datagramSocket = DatagramSocket(23236)
            datagramSocket?.receive(recePacket)
            val f = String(recePacket.data, 0, recePacket.length)
            if(f.contentEquals("dd4dd")){
                address = recePacket.address.hostName
            }
            LogU.i("found address: $address")
            retry = false
            onFoundHost(address)
        },"Sender").start()
    }

    fun stop(){
        input.close()
        output.close()
        mWrite.shutdown()
    }
}