package com.stepwise.glasses

import com.stepwise.core.FrameSource
import com.stepwise.core.ImageFrame
import com.stepwise.core.ImageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared bridge for the CAD tutor or future hardware workflows.
 * The DAT integration must return JPEG-encoded bytes from captureBestFrameFromStream.
 * Captures off the main thread (HEIC decode + JPEG encode) and closes the camera
 * right after each photo, the same battery/privacy policy as the hardware session.
 */
class GlassesFrameSource(
    private val manager: MetaDatSessionManager,
) : FrameSource {
    override suspend fun capture(): ImageFrame? {
        val bytes = withContext(Dispatchers.Default) {
            try {
                manager.captureBestFrameFromStream()
            } finally {
                manager.stopStream()
            }
        } ?: return null
        if (bytes.isEmpty()) return null
        return ImageFrame(bytes, "image/jpeg", ImageSource.GLASSES, System.currentTimeMillis())
    }
}
