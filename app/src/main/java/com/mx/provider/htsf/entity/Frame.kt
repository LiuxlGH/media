package com.mx.provider.htsf.entity

class Frame<T>(val data: T, val packetType: Int, val frameType: Int) {
    val FRAME_TYPE_KEY_FRAME = 2
    val FRAME_TYPE_INTER_FRAME = 3
    val FRAME_TYPE_CONFIGURATION = 4

    companion object {
        const val FRAME_TYPE_AUDIO = 1
        const val FRAME_TYPE_KEY_FRAME = 2
        const val FRAME_TYPE_INTER_FRAME = 3
        const val FRAME_TYPE_CONFIGURATION = 4
    }
}