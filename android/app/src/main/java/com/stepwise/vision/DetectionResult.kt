package com.stepwise.vision

/** Caixa delimitadora de uma peça detectada, em coordenadas normalizadas. */
data class BoundingBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Uma detecção individual retornada pelo detector visual local. */
data class Detection(
    val className: String,
    val confidence: Float,
    val boundingBox: BoundingBox
)

/** Conjunto de detecções para um único frame. */
data class DetectionResult(val detections: List<Detection>)
