package com.stepwise.core

enum class ImageSource { GLASSES, IMPORTED_DRAWING, TEST_FIXTURE }

/** Encoded image (not raw camera pixels). Do not mutate bytes after submission. */
class ImageFrame(
    val bytes: ByteArray,
    val mimeType: String,
    val source: ImageSource,
    val capturedAtMillis: Long,
) {
    init {
        require(bytes.isNotEmpty())
        require(mimeType in setOf("image/jpeg", "image/png", "image/webp"))
        require(capturedAtMillis >= 0)
    }
}

/** Shared capture port: both modes can use the glasses; tests supply a fake. */
fun interface FrameSource {
    suspend fun capture(): ImageFrame?
}

/** Signed 16-bit little-endian PCM. Sample rate is explicit for provider adapters. */
class PcmAudioChunk(val bytes: ByteArray, val sampleRateHz: Int, val channels: Int = 1) {
    init {
        require(sampleRateHz > 0)
        require(channels in 1..2)
        require(bytes.isNotEmpty() && bytes.size % (2 * channels) == 0)
    }
}
