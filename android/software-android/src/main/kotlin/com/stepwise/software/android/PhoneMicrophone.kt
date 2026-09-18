package com.stepwise.software.android

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.stepwise.core.PcmAudioChunk
import kotlinx.coroutines.*

/** Explicit phone fallback. Glasses audio routing must be validated separately. */
internal object PhoneMicrophone {
    @SuppressLint("MissingPermission") // Activity checks RECORD_AUDIO before starting.
    suspend fun capture(consume: suspend (PcmAudioChunk) -> Unit) = withContext(Dispatchers.IO) {
        val minimum = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT)
        require(minimum > 0)
        val recorder = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(16_000)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(minimum, 6400)).build()
        try {
            check(recorder.state == AudioRecord.STATE_INITIALIZED)
            recorder.startRecording()
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
            val buffer = ByteArray(3200)
            while (isActive) {
                val count = recorder.read(buffer, 0, buffer.size)
                check(count >= 0) { "Falha na captura de áudio" }
                if (count > 0) consume(PcmAudioChunk(buffer.copyOf(count), 16_000))
            }
        } finally {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
            recorder.release()
        }
    }
}
