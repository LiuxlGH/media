package com.mx.provider.htsf.encoder

import android.media.MediaCodec
import android.media.MediaFormat
import com.mx.provider.htsf.config.AudioConfiguration

class AudioMediaCodec {
    companion object {
        fun getAudioMediaCodec(configuration: AudioConfiguration): MediaCodec? {
            val format = MediaFormat.createAudioFormat(
                configuration.mime!!,
                configuration.frequency,
                configuration.channelCount
            )
            if (configuration.mime.equals(AudioConfiguration.DEFAULT_MIME)) {
                format.setInteger(MediaFormat.KEY_AAC_PROFILE, configuration.aacProfile)
            }
            format.setInteger(MediaFormat.KEY_BIT_RATE, configuration.maxBps * 1024)
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, configuration.frequency)
            val maxInputSize = AudioUtils.getRecordBufferSize(configuration)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize)
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, configuration.channelCount)
            var mediaCodec: MediaCodec? = null
            try {
                mediaCodec = configuration.mime?.let { MediaCodec.createEncoderByType(it) }
                mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            } catch (e: Exception) {
                e.printStackTrace()
                if (mediaCodec != null) {
                    mediaCodec.stop()
                    mediaCodec.release()
                    mediaCodec = null
                }
            }
            return mediaCodec
        }
    }
}