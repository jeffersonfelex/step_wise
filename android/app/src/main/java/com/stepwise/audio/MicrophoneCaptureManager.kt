package com.stepwise.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread
import kotlin.math.sqrt

class MicrophoneCaptureManager(private val context: Context) {
    data class CaptureInfo(
        val sampleRateHz: Int,
        val channelConfig: Int,
        val encoding: Int,
        val bufferSizeBytes: Int,
    )

    interface Listener {
        fun onStarted(info: CaptureInfo)
        fun onLevel(rms: Int)
        fun onFinished(totalBytes: Int)
        fun onError(message: String)
    }

    @Volatile private var running = false
    private var recorder: AudioRecord? = null

    fun startThreeSecondTest(listener: Listener) {
        if (running) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            listener.onError("Permissao de microfone ausente")
            return
        }

        val sampleRate = 16_000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
        if (minBuffer <= 0) {
            listener.onError("Formato PCM 16 kHz mono indisponivel")
            return
        }
        val bufferSize = minBuffer * 2

        @Suppress("MissingPermission")
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            encoding,
            bufferSize,
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            listener.onError("AudioRecord nao inicializou")
            return
        }

        recorder = audioRecord
        running = true
        thread(name = "stepwise-mic-test") {
            var totalBytes = 0
            try {
                audioRecord.startRecording()
                listener.onStarted(CaptureInfo(sampleRate, channelConfig, encoding, bufferSize))
                val buffer = ShortArray(bufferSize / 2)
                val deadline = System.currentTimeMillis() + 3_000
                while (running && System.currentTimeMillis() < deadline) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        totalBytes += read * 2
                        listener.onLevel(buffer.rms(read))
                    }
                }
                listener.onFinished(totalBytes)
            } catch (error: Throwable) {
                listener.onError(error.message ?: "Falha ao capturar audio")
            } finally {
                running = false
                try { audioRecord.stop() } catch (_: Throwable) {}
                audioRecord.release()
                recorder = null
            }
        }
    }

    fun stop() {
        running = false
    }

    private fun ShortArray.rms(count: Int): Int {
        if (count <= 0) return 0
        var sum = 0.0
        for (index in 0 until count) {
            val sample = this[index].toDouble()
            sum += sample * sample
        }
        return sqrt(sum / count).toInt()
    }
}
