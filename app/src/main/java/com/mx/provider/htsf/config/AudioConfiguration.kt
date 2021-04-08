package com.mx.provider.htsf.config

import android.media.AudioFormat
import android.media.MediaCodecInfo

class AudioConfiguration(builder: Builder) {
    companion object {
        val DEFAULT_FREQUENCY = 44100
        val DEFAULT_MAX_BPS = 64
        val DEFAULT_MIN_BPS = 32
        val DEFAULT_ADTS = 0
        val DEFAULT_MIME = "audio/mp4a-latm"
        val DEFAULT_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        val DEFAULT_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC
        val DEFAULT_CHANNEL_COUNT = 2
        val DEFAULT_AEC = true

        fun createDefault(): AudioConfiguration? {
            return Builder().build()
        }
    }
    var minBps = 0
    var maxBps = 0
    var frequency = 0
    var encoding = 0
    var channelCount = 0
    var adts = 0
    var aacProfile = 0
    var mime: String? = null
    var aec = false

    init {
        minBps = builder.minBps
        maxBps = builder.maxBps
        frequency = builder.frequency
        encoding = builder.encoding
        channelCount = builder.channelCount
        adts = builder.adts
        mime = builder.mime
        aacProfile = builder.aacProfile
        aec = builder.aec
    }

    class Builder {
        var minBps: Int = DEFAULT_MIN_BPS
        var maxBps: Int = DEFAULT_MAX_BPS
        var frequency: Int = DEFAULT_FREQUENCY
        var encoding: Int = DEFAULT_AUDIO_ENCODING
        var channelCount: Int = DEFAULT_CHANNEL_COUNT
        var adts: Int = DEFAULT_ADTS
        var mime: String = DEFAULT_MIME
        var aacProfile: Int = DEFAULT_AAC_PROFILE
        var aec: Boolean = DEFAULT_AEC
        fun setBps(minBps: Int, maxBps: Int): Builder {
            this.minBps = minBps
            this.maxBps = maxBps
            return this
        }

        fun setFrequency(frequency: Int): Builder {
            this.frequency = frequency
            return this
        }

        fun setEncoding(encoding: Int): Builder {
            this.encoding = encoding
            return this
        }

        fun setChannelCount(channelCount: Int): Builder {
            this.channelCount = channelCount
            return this
        }

        fun setAdts(adts: Int): Builder {
            this.adts = adts
            return this
        }

        fun setAacProfile(aacProfile: Int): Builder {
            this.aacProfile = aacProfile
            return this
        }

        fun setMime(mime: String): Builder {
            this.mime = mime
            return this
        }

        fun setAec(aec: Boolean): Builder {
            this.aec = aec
            return this
        }

        fun build(): AudioConfiguration {
            return AudioConfiguration(this)
        }
    }
}