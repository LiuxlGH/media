package com.mx.controller.player

import com.mx.controller.player.entity.Frame
import java.util.concurrent.ArrayBlockingQueue

class ReceiveQueue {
    private var mPlayQueue: ArrayBlockingQueue<Frame> = ArrayBlockingQueue(30,true)

    fun take():Frame{
        val frame = mPlayQueue.take()
        return frame
    }
    fun put(frame:Frame){
        mPlayQueue.put(frame)
    }
    fun clear(){
        mPlayQueue.clear()
    }
}