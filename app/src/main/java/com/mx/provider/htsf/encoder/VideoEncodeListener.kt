package com.mx.provider.htsf.encoder

import android.media.MediaCodec
import java.nio.ByteBuffer

interface VideoEncodeListener {
    fun onVideoEncode(bb:ByteBuffer, bi:MediaCodec.BufferInfo)
}