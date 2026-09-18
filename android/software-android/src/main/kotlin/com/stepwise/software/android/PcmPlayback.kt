package com.stepwise.software.android

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.stepwise.core.PcmAudioChunk
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger

/** Bounded playback queue; obsolete audio is discarded when the user interrupts. */
internal class PcmPlayback(private val onFailure: () -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<Pair<Int, PcmAudioChunk>>(32)
    private val generation = AtomicInteger()
    private val lock = Any()
    private var track: AudioTrack? = null

    init {
        scope.launch {
            var activeGeneration = -1
            var rate = 0
            try {
                for ((epoch, chunk) in queue) {
                    if (epoch != generation.get()) continue
                    require(chunk.channels == 1)
                    synchronized(lock) {
                        if (activeGeneration != epoch || rate != chunk.sampleRateHz) {
                            track?.release()
                            rate = chunk.sampleRateHz
                            activeGeneration = epoch
                            val minimum = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT)
                            require(minimum > 0)
                            track = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                                .setBufferSizeInBytes(maxOf(minimum, rate / 2)).setTransferMode(AudioTrack.MODE_STREAM).build()
                            check(track?.state == AudioTrack.STATE_INITIALIZED)
                            track?.play()
                        }
                    }
                    var offset = 0
                    while (offset < chunk.bytes.size && epoch == generation.get()) {
                        ensureActive()
                        val written = synchronized(lock) {
                            track!!.write(chunk.bytes, offset, chunk.bytes.size - offset, AudioTrack.WRITE_NON_BLOCKING)
                        }
                        check(written >= 0)
                        offset += written
                        if (written == 0) delay(5)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onFailure()
            } finally {
                synchronized(lock) { track?.release(); track = null }
                queue.close()
            }
        }
    }

    fun enqueue(chunk: PcmAudioChunk) {
        check(queue.trySend(generation.get() to chunk).isSuccess) { "Fila de áudio cheia ou encerrada" }
    }

    fun interrupt() {
        generation.incrementAndGet()
        while (queue.tryReceive().isSuccess) { /* discard pending playback */ }
        synchronized(lock) { track?.pause(); track?.flush() }
    }

    fun close() { interrupt(); queue.close(); scope.cancel() }
}
