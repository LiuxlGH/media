package com.mx.provider.htsf.sender

import com.mx.provider.htsf.entity.Frame

interface SendQueue<T> {
    fun start()
    fun stop()
    fun setBufferSize(size: Int)
    fun putFrame(frame: Frame<T>?)
    fun takeFrame(): Frame<T>?
    fun clear()
//    fun setSendQueueListener(listener: SendQueueListener?)
}