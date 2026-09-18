package com.stepwise.glasses

import com.stepwise.core.FrameSource
import com.stepwise.core.ImageFrame
import com.stepwise.core.ImageSource

/**
 * Shared bridge for the CAD tutor or future hardware workflows.
 * The DAT integration must return JPEG-encoded bytes from captureBestFrameFromStream.
 * The existing manager still reports NotImplementedError until DAT is integrated.
 */
class GlassesFrameSource(
    private val manager: MetaDatSessionManager,
) : FrameSource {
    override suspend fun capture(): ImageFrame? {
        val bytes = manager.captureBestFrameFromStream() ?: return null
        if (bytes.isEmpty()) return null
        return ImageFrame(bytes, "image/jpeg", ImageSource.GLASSES, System.currentTimeMillis())
    }
}
