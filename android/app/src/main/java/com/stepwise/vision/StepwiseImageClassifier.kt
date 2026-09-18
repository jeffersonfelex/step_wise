package com.stepwise.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StepwiseImageClassifier(context: Context) : Closeable {
    data class Classification(
        val label: String,
        val confidence: Float,
    )

    data class DiagnosticResult(
        val assetPath: String,
        val expectedLabel: String,
        val top3: List<Classification>,
    )

    data class AssetSample(
        val assetPath: String,
        val expectedLabel: String,
    )

    private val labels = listOf(
        "P1_torneira",
        "P2",
        "P3",
        "P4",
        "P5",
        "P6",
        "pecas_separadas_juntas",
        "S1_torneira",
        "S2_torneira_P2",
        "S3_torneira_P2_P3",
        "S4_torneira_P2_P3_P4",
        "S5_torneira_P2_P3_P4_P5",
        "S6_final",
        "E1_ordem_errada",
        "E2_peca_invertida",
        "E3_peca_ausente",
        "E4_peca_solta",
    )
    private val interpreter: Interpreter

    init {
        val modelBytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size).order(ByteOrder.nativeOrder())
        modelBuffer.put(modelBytes)
        modelBuffer.rewind()
        interpreter = Interpreter(modelBuffer)
        interpreter.allocateTensors()
    }

    fun classify(bitmap: Bitmap): List<Classification> {
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(bitmap.toInputBuffer(), output)
        return output[0].indices
            .sortedByDescending { output[0][it] }
            .take(3)
            .map { index -> Classification(labels[index], output[0][index]) }
    }

    fun runAssetDiagnostic(context: Context): List<DiagnosticResult> = DIAGNOSTIC_SAMPLES.map { sample ->
        classifyAsset(context, sample)
    }

    fun classifyAsset(context: Context, sample: AssetSample): DiagnosticResult {
        val bitmap = context.assets.open(sample.assetPath).use { BitmapFactory.decodeStream(it) }
        requireNotNull(bitmap) { "Nao foi possivel decodificar ${sample.assetPath}" }
        val top3 = classify(bitmap)
        bitmap.recycle()
        return DiagnosticResult(sample.assetPath, sample.expectedLabel, top3)
    }

    override fun close() {
        interpreter.close()
    }

    private fun Bitmap.toInputBuffer(): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(this, 224, 224, true)
        val buffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * FLOAT_BYTES).order(ByteOrder.nativeOrder())
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resized.getPixel(x, y)
                buffer.putFloat((Color.red(pixel) / 127.5f) - 1f)
                buffer.putFloat((Color.green(pixel) / 127.5f) - 1f)
                buffer.putFloat((Color.blue(pixel) / 127.5f) - 1f)
            }
        }
        if (resized !== this) resized.recycle()
        buffer.rewind()
        return buffer
    }

    companion object {
        fun sampleForExpectedLabel(expectedLabel: String): AssetSample? = DIAGNOSTIC_SAMPLES
            .firstOrNull { it.expectedLabel == expectedLabel }

        private const val MODEL_ASSET = "models/stepwise_real_classifier_v1.tflite"
        private const val FLOAT_BYTES = 4
        private val DIAGNOSTIC_SAMPLES = listOf(
            AssetSample("diagnostic_samples/S1_torneira_sample.jpg", "S1_torneira"),
            AssetSample("diagnostic_samples/S2_torneira_P2_sample.jpg", "S2_torneira_P2"),
            AssetSample("diagnostic_samples/S3_torneira_P2_P3_sample.jpg", "S3_torneira_P2_P3"),
            AssetSample("diagnostic_samples/S4_torneira_P2_P3_P4_sample.jpg", "S4_torneira_P2_P3_P4"),
            AssetSample("diagnostic_samples/S5_torneira_P2_P3_P4_P5_sample.jpg", "S5_torneira_P2_P3_P4_P5"),
            AssetSample("diagnostic_samples/S6_final_sample.jpg", "S6_final"),
            AssetSample("diagnostic_samples/E1_ordem_errada_sample.jpg", "E1_ordem_errada"),
            AssetSample("diagnostic_samples/E2_peca_invertida_sample.jpg", "E2_peca_invertida"),
            AssetSample("diagnostic_samples/E3_peca_ausente_sample.jpg", "E3_peca_ausente"),
            AssetSample("diagnostic_samples/E4_peca_solta_sample.jpg", "E4_peca_solta"),
        )
    }
}
