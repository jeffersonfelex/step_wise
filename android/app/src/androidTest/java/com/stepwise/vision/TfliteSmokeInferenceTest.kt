package com.stepwise.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class TfliteSmokeInferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testAssets = InstrumentationRegistry.getInstrumentation().context.assets

    @Test
    fun smokeModelLoadsAndRunsOneSyntheticInference() {
        loadModel("models/stepwise_b19_smoke.tflite").use { model ->
            Interpreter(model.buffer).use { interpreter ->
                interpreter.allocateTensors()

                val inputTensor = interpreter.getInputTensor(0)
                assertArrayEquals(intArrayOf(1, 256, 256, 3), inputTensor.shape())
                assertEquals(DataType.FLOAT32, inputTensor.dataType())

                val outputs = mutableMapOf<Int, Any>()
                var boxes: Array<Array<FloatArray>>? = null
                var classes: Array<IntArray>? = null
                var scores: Array<FloatArray>? = null
                var detectionCount: IntArray? = null
                for (index in 0 until interpreter.outputTensorCount) {
                    val tensor = interpreter.getOutputTensor(index)
                    outputs[index] = when {
                        tensor.dataType() == DataType.FLOAT32 && tensor.shape().contentEquals(intArrayOf(1, 100, 4)) -> {
                            Array(1) { Array(100) { FloatArray(4) } }.also { boxes = it }
                        }
                        tensor.dataType() == DataType.INT32 && tensor.shape().contentEquals(intArrayOf(1, 100)) -> {
                            Array(1) { IntArray(100) }.also { classes = it }
                        }
                        tensor.dataType() == DataType.FLOAT32 && tensor.shape().contentEquals(intArrayOf(1, 100)) -> {
                            Array(1) { FloatArray(100) }.also { scores = it }
                        }
                        tensor.dataType() == DataType.INT32 && tensor.shape().contentEquals(intArrayOf(1)) -> {
                            IntArray(1).also { detectionCount = it }
                        }
                        else -> ByteBuffer.allocateDirect(tensor.numBytes()).order(ByteOrder.nativeOrder())
                    }
                }

                interpreter.runForMultipleInputsOutputs(arrayOf(syntheticInput()), outputs)

                assertTrue("Object detector smoke model should expose multiple outputs", outputs.size >= 4)
                assertNotNull("Model must expose detection boxes", boxes)
                assertNotNull("Model must expose detection classes", classes)
                assertNotNull("Model must expose detection scores", scores)
                assertNotNull("Model must expose detection count", detectionCount)
                requireNotNull(boxes)
                requireNotNull(scores)
                requireNotNull(detectionCount)
                assertTrue("Detection count must be non-negative", detectionCount[0] >= 0)
                assertTrue("At least one score must be finite", scores[0].any { it.isFinite() })
                assertTrue("At least one box coordinate must be finite", boxes[0].flatMap { it.asIterable() }.any { it.isFinite() })
            }
        }
    }

    @Test
    fun realClassifierModelLoadsAndRunsOneSyntheticInference() {
        loadModel("models/stepwise_real_classifier_v1.tflite").use { model ->
            Interpreter(model.buffer).use { interpreter ->
                interpreter.allocateTensors()

                val inputTensor = interpreter.getInputTensor(0)
                assertArrayEquals(intArrayOf(1, 224, 224, 3), inputTensor.shape())
                assertEquals(DataType.FLOAT32, inputTensor.dataType())

                val outputTensor = interpreter.getOutputTensor(0)
                assertArrayEquals(intArrayOf(1, 17), outputTensor.shape())
                assertEquals(DataType.FLOAT32, outputTensor.dataType())

                val output = Array(1) { FloatArray(17) }
                interpreter.run(syntheticClassifierInput(), output)

                val scores = output[0]
                assertEquals("Classifier must return one score per label", 17, scores.size)
                assertTrue("All classifier scores must be finite", scores.all { it.isFinite() })
                assertEquals("Softmax scores should sum to 1", 1f, scores.sum(), 0.0001f)
                assertTrue("At least one class must have positive confidence", scores.maxOrNull()!! > 0f)
            }
        }
    }

    @Test
    fun realClassifierRunsFiveRealDatasetImagesOnDevice() {
        val labels = listOf(
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
        val samples = listOf(
            RealSample("real_samples/S1_torneira_001.jpg", "S1_torneira"),
            RealSample("real_samples/S2_torneira_P2_001.jpg", "S2_torneira_P2"),
            RealSample("real_samples/S3_torneira_P2_P3_001.jpg", "S3_torneira_P2_P3"),
            RealSample("real_samples/E1_ordem_errada_001.jpg", "E1_ordem_errada"),
            RealSample("real_samples/E2_peca_invertida_001.jpg", "E2_peca_invertida"),
        )

        loadModel("models/stepwise_real_classifier_v1.tflite").use { model ->
            Interpreter(model.buffer).use { interpreter ->
                interpreter.allocateTensors()
                val outputTensor = interpreter.getOutputTensor(0)
                assertArrayEquals(intArrayOf(1, labels.size), outputTensor.shape())

                for (sample in samples) {
                    val output = Array(1) { FloatArray(labels.size) }
                    interpreter.run(realImageInput(sample.assetPath), output)
                    val scores = output[0]
                    val top3 = scores.indices
                        .sortedByDescending { scores[it] }
                        .take(3)
                        .map { labels[it] to scores[it] }

                    Log.i(
                        TAG,
                        "real-sample expected=${sample.expectedLabel} top3=$top3",
                    )
                    assertTrue("${sample.assetPath}: scores must be finite", scores.all { it.isFinite() })
                    assertEquals("${sample.assetPath}: softmax sum", 1f, scores.sum(), 0.0001f)
                    assertTrue("${sample.assetPath}: top label must be known", top3.first().first in labels)
                    assertTrue("${sample.assetPath}: confidence must be positive", top3.first().second > 0f)
                }
            }
        }
    }

    private fun syntheticInput(): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * 256 * 256 * 3 * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder())
        for (y in 0 until 256) {
            for (x in 0 until 256) {
                val inBox = x in 64..144 && y in 72..142
                buffer.putFloat(if (inBox) 40f else 30f)
                buffer.putFloat(if (inBox) 120f else 30f)
                buffer.putFloat(if (inBox) 230f else 30f)
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun syntheticClassifierInput(): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder())
        repeat(224 * 224) {
            buffer.putFloat(0f)
            buffer.putFloat(0f)
            buffer.putFloat(0f)
        }
        buffer.rewind()
        return buffer
    }

    private fun realImageInput(assetPath: String): ByteBuffer {
        val original = testAssets.open(assetPath).use { input ->
            BitmapFactory.decodeStream(input)
        }
        requireNotNull(original) { "Could not decode test asset $assetPath" }
        val resized = Bitmap.createScaledBitmap(original, 224, 224, true)
        if (resized !== original) {
            original.recycle()
        }

        val buffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder())
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resized.getPixel(x, y)
                buffer.putFloat((Color.red(pixel) / 127.5f) - 1f)
                buffer.putFloat((Color.green(pixel) / 127.5f) - 1f)
                buffer.putFloat((Color.blue(pixel) / 127.5f) - 1f)
            }
        }
        resized.recycle()
        buffer.rewind()
        return buffer
    }

    private fun loadModel(assetPath: String): ModelBuffer {
        context.assets.open(assetPath).use { input ->
            val bytes = input.readBytes()
            val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            buffer.put(bytes)
            buffer.rewind()
            return ModelBuffer(buffer)
        }
    }

    private class ModelBuffer(val buffer: ByteBuffer) : Closeable {
        override fun close() = Unit
    }

    private data class RealSample(val assetPath: String, val expectedLabel: String)

    private companion object {
        const val FLOAT_BYTES = 4
        const val TAG = "StepWiseTfliteTest"
    }
}
