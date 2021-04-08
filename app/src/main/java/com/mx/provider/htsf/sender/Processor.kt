package com.mx.provider.htsf.sender

interface Processor {
    fun start(sender: TcpSender?)
    fun stop()
}