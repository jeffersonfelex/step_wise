package com.stepwise.vision

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Carrega o modelo empacotado e roda uma inferência sintética (sem fotos do dataset). */
@RunWith(AndroidJUnit4::class)
class TfliteClassifierDetectorTest {

    @Test
    fun modeloEmpacotadoRodaEDevolveTop3Normalizado() = runBlocking {
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
        val jpeg = ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            it.toByteArray()
        }

        TfliteClassifierDetector(ApplicationProvider.getApplicationContext()).use { detector ->
            val result = detector.detect(jpeg)

            assertEquals(3, result.detections.size)
            assertTrue(result.detections.all { it.confidence in 0f..1f })
            assertTrue(result.detections.zipWithNext().all { (a, b) -> a.confidence >= b.confidence })
        }
    }

    @Test
    fun bytesQueNaoSaoImagemNaoGeramDeteccao() = runBlocking {
        TfliteClassifierDetector(ApplicationProvider.getApplicationContext()).use { detector ->
            assertTrue(detector.detect(byteArrayOf(1, 2, 3)).detections.isEmpty())
        }
    }
}
