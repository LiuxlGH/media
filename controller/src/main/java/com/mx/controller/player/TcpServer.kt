package com.mx.controller.player

import com.blankj.utilcode.util.NetworkUtils
import com.mx.common.LogU
import java.net.*

class TcpServer {
    private var mSocket: Socket?=null
    private var dataReceiveTask: DataReceiveTask?=null
    private lateinit var serverSocket: ServerSocket
    private var signal:String?=null
    var isPause = false
    set(value) {
        field = value
        dataReceiveTask?.isStart = !isPause
    }
    lateinit var onConnected:()->Unit
    lateinit var onDisconnected:()->Unit
    @Volatile
    private var reTry= true
    fun start(msg:String){
        reTry = true
        Thread({
            val port= 23235
            val broadAddress= InetAddress.getByName("255.255.255.255")
            val socket = DatagramSocket()
            val c = msg.toByteArray()
            val pack = DatagramPacket(c,c.size,broadAddress,port)
            while (reTry){
                socket.send(pack)
                Thread.sleep(3000)
            }
            socket.close()
        },"Search").start()

        Thread({
               acceptData()
        },"AcceptTask").start()
    }
    fun start(){
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

            acceptData()
        }, "Listener").start()
    }

    fun reAccept(){
        Thread({acceptData()},"Accept er").start()
    }

    private fun acceptData() {
        serverSocket = ServerSocket(23236)
        mSocket = serverSocket.accept()
        reTry = false
        onConnected.invoke()
        val inputStream = mSocket?.getInputStream()
        LogU.i("connected: ${mSocket?.inetAddress?.hostName}")
        runReceiveThread(mSocket!!)
    }

    private fun runReceiveThread(scket: Socket) {
        DataReceiveTask(scket){

        }.apply {
            dataReceiveTask = this
            onFrame = {
                LogU.i("frame type ${it.type}")
                PlayActivity.playQueue.put(it)
            }
            onCatch = {
                stop()
            }
        }.run()
    }

    fun stop(){
        Thread(){
            kotlin.runCatching {
                dataReceiveTask?.isStart= false
                dataReceiveTask?.shutdown()
                serverSocket.close()
                mSocket?.close()
                onDisconnected.invoke()
            }.onFailure {

            }
        }.start()
    }
}