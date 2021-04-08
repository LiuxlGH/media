package com.mx.provider.htsf.sender

import com.mx.provider.htsf.entity.Frame
import com.mx.provider.htsf.entity.VideoInfo
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

class ScreenRecordQueue : SendQueue<VideoInfo> {
    private val mFramBuffer = ArrayBlockingQueue<Frame<VideoInfo>>(30, true)
    private val mTotalFrameCount = AtomicInteger(0) //总个数
    private val mGiveUpFrameCount = AtomicInteger(0) //总个数
    private val mKeyFrameCount = AtomicInteger(0) //队列里Key帧的总个数...
    private val mInFrameCount = AtomicInteger(0) //进入总个数
    private val mOutFrameCount = AtomicInteger(0) //输出总个数

    override fun start() {
    }

    override fun stop() {
    }

    override fun setBufferSize(size: Int) {
    }

    override fun putFrame(frame: Frame<VideoInfo>?) {
        frame?.let {
            mFramBuffer.put(it)
            mTotalFrameCount.getAndIncrement()
            mInFrameCount.getAndIncrement()
        }
    }

    override fun takeFrame(): Frame<VideoInfo>? {
        val  frame = mFramBuffer.poll()
        mTotalFrameCount.getAndDecrement()
        mOutFrameCount.getAndIncrement()
        return frame
    }

    override fun clear() {
        mFramBuffer.clear()
    }
}