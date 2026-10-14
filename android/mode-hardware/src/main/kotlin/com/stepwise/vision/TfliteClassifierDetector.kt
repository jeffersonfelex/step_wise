package com.stepwise.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.tensorflow.lite.Interpreter

/**
 * PLANO A do detector visual: classificador TFLite treinado por ml/train.py
 * sobre fotos reais do kit (estados S1–S6 e erros E1–E4 da montagem de torneira).
 *
 * Classifica a imagem inteira, então cada classe vira uma [Detection] com a
 * caixa do frame todo. O [com.stepwise.engine.ProceduralEngine] decide com os
 * limiares de confiança; um modelo fraco simplesmente leva a INCONCLUSIVE.
 *
 * O modelo é carregado na primeira detecção (fora da thread principal). Se o
 * asset faltar ou não bater com os rótulos, [detect] lança exceção e o
 * [CompositeDetector] cai para o detector reserva.
 */
class TfliteClassifierDetector(
    context: Context,
    private val modelAsset: String = DEFAULT_MODEL_ASSET,
    private val labelsAsset: String = DEFAULT_LABELS_ASSET,
    private val inputSize: Int = 224,
    private val topK: Int = 3,
) : VisualDetector, Closeable {

    private val appContext = context.applicationContext
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    override suspend fun detect(frame: ByteArray): DetectionResult = withContext(Dispatchers.Default) {
        val bitmap = decodeScaled(frame) ?: return@withContext DetectionResult(emptyList())
        val scores = try {
            classify(bitmap)
        } finally {
            bitmap.recycle()
        }
        val whole = BoundingBox(0f, 0f, 1f, 1f)
        DetectionResult(
            scores.indices
                .sortedByDescending { scores[it] }
                .take(topK)
                .map { Detection(labels[it], scores[it], whole) }
        )
    }

    @Synchronized // Interpreter não é thread-safe.
    private fun classify(bitmap: Bitmap): FloatArray {
        val model = interpreter ?: load()
        val output = Array(1) { FloatArray(labels.size) }
        model.run(toInput(bitmap), output)
        return output[0]
    }

    private fun load(): Interpreter {
        labels = appContext.assets.open(labelsAsset).bufferedReader(Charsets.UTF_8).use { reader ->
            val array = JSONObject(reader.readText().removePrefix("﻿")).getJSONArray("labels")
            List(array.length()) { array.getString(it) }
        }
        val bytes = appContext.assets.open(modelAsset).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes)
        buffer.rewind()
        val model = Interpreter(buffer)
        val outputShape = model.getOutputTensor(0).shape()
        if (outputShape.last() != labels.size) {
            model.close()
            throw IllegalStateException("Modelo com ${outputShape.last()} saídas para ${labels.size} rótulos")
        }
        interpreter = model
        return model
    }

    /** Decodifica já reduzido (~2x a entrada) para não alocar a foto inteira dos óculos. */
    private fun decodeScaled(frame: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(frame, 0, frame.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= inputSize * 2) sample *= 2
        return BitmapFactory.decodeByteArray(frame, 0, frame.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** RGB float32 normalizado como mobilenet_v2.preprocess_input ([-1, 1]), igual ao treino. */
    private fun toInput(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        if (resized !== bitmap) resized.recycle()
        val buffer = ByteBuffer.allocateDirect(pixels.size * 3 * FLOAT_BYTES).order(ByteOrder.nativeOrder())
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 127.5f - 1f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 127.5f - 1f)
            buffer.putFloat((pixel and 0xFF) / 127.5f - 1f)
        }
        buffer.rewind()
        return buffer
    }

    @Synchronized
    override fun close() {
        interpreter?.close()
        interpreter = null
    }

    companion object {
        const val DEFAULT_MODEL_ASSET = "models/stepwise_real_classifier_v1.tflite"
        const val DEFAULT_LABELS_ASSET = "models/stepwise_real_classifier_v1_labels.json"
        private const val FLOAT_BYTES = 4
    }
}
