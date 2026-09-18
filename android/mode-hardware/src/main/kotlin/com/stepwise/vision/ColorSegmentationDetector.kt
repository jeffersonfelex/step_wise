package com.stepwise.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color

/**
 * PLANO B (determinístico) do detector visual — segmentação por cor.
 *
 * Motivo (feedback da mentoria, 15/09): o detector customizado (MediaPipe/
 * LiteRT) é o maior risco do projeto — precisa de dataset visto do ponto
 * de vista dos óculos, treinado e validado a tempo. Como as peças da
 * montagem já têm cor própria ("engrenagem azul", "engrenagem vermelha"),
 * um classificador HSV é uma alternativa simples, previsível e rápida de
 * validar sob pressão de tempo. Não precisa de nenhuma biblioteca externa:
 * usa só android.graphics.Color/Bitmap, já presentes no Android.
 *
 * Limitação assumida (também apontada pela mentoria): um único frame 2D
 * visto de cima não dá profundidade — "encaixada" vs. "só apoiada" pode
 * ficar ambíguo. Por isso o MVP deve restringir os erros conhecidos aos
 * que não dependem de profundidade (peça errada no eixo, peça faltando,
 * ordem trocada).
 *
 * Ajustar [colorRanges] com os tons reais do kit físico antes do evento
 * (a iluminação do local muda os valores de HSV observados).
 */
class ColorSegmentationDetector(
    private val colorRanges: List<ColorClassRange> = defaultColorRanges(),
    private val minMatchingPixelsRatio: Float = 0.01f, // % mínima da imagem para considerar detecção válida
    private val samplingStride: Int = 4 // amostra 1 em N pixels por eixo, para custo de CPU previsível
) : VisualDetector {

    override suspend fun detect(frame: ByteArray): DetectionResult {
        val bitmap = BitmapFactory.decodeByteArray(frame, 0, frame.size) ?: return DetectionResult(emptyList())
        return DetectionResult(detections = classify(bitmap))
    }

    private fun classify(bitmap: Bitmap): List<Detection> {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) return emptyList()

        val hsv = FloatArray(3)
        val detections = mutableListOf<Detection>()

        for (range in colorRanges) {
            var minX = width
            var minY = height
            var maxX = 0
            var maxY = 0
            var matchCount = 0
            var sampleCount = 0

            var y = 0
            while (y < height) {
                var x = 0
                while (x < width) {
                    val pixel = bitmap.getPixel(x, y)
                    Color.colorToHSV(pixel, hsv)
                    sampleCount++

                    if (range.matches(hsv[0], hsv[1], hsv[2])) {
                        matchCount++
                        if (x < minX) minX = x
                        if (y < minY) minY = y
                        if (x > maxX) maxX = x
                        if (y > maxY) maxY = y
                    }
                    x += samplingStride
                }
                y += samplingStride
            }

            val matchRatio = if (sampleCount > 0) matchCount.toFloat() / sampleCount else 0f
            if (matchRatio >= minMatchingPixelsRatio && maxX >= minX && maxY >= minY) {
                detections += Detection(
                    className = range.className,
                    confidence = matchRatio.coerceAtMost(1f),
                    boundingBox = BoundingBox(
                        left = minX.toFloat() / width,
                        top = minY.toFloat() / height,
                        right = maxX.toFloat() / width,
                        bottom = maxY.toFloat() / height
                    )
                )
            }
        }

        return detections
    }

    /** Faixa de HSV (matiz 0-360, saturação/valor 0-1) associada a uma classe do procedimento. */
    data class ColorClassRange(
        val className: String,
        val hueMin: Float,
        val hueMax: Float,
        val minSaturation: Float = 0.35f,
        val minValue: Float = 0.25f
    ) {
        fun matches(hue: Float, saturation: Float, value: Float): Boolean {
            if (saturation < minSaturation || value < minValue) return false
            return if (hueMin <= hueMax) {
                hue in hueMin..hueMax
            } else {
                // faixa "circular" (ex.: vermelho cruzando o 0/360)
                hue >= hueMin || hue <= hueMax
            }
        }
    }

    companion object {
        /**
         * TODO: calibrar com fotos reais do kit (celular na altura da
         * testa, ultra-wide, iluminação do local do evento) antes de usar
         * como plano B na demo.
         */
        fun defaultColorRanges(): List<ColorClassRange> = listOf(
            ColorClassRange(className = "engrenagem_azul", hueMin = 200f, hueMax = 250f),
            ColorClassRange(className = "engrenagem_vermelha", hueMin = 350f, hueMax = 10f)
        )
    }
}
