package com.stepwise.vision

/**
 * Detector visual local (MediaPipe / LiteRT) — roda inteiramente no
 * smartphone. Recebe 1 frame e retorna classes, bounding boxes e scores.
 *
 * TODO: integrar com o modelo customizado exportado por ml/export_litert.py
 * (ver diretório ml/ na raiz do repositório).
 */
interface VisualDetector {
    suspend fun detect(frame: ByteArray): DetectionResult
}

class MediaPipeLiteRtDetector : VisualDetector {
    override suspend fun detect(frame: ByteArray): DetectionResult {
        // TODO: carregar modelo LiteRT (.tflite) e executar inferência local.
        return DetectionResult(detections = emptyList())
    }
}
