package com.stepwise.software.android

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import com.stepwise.core.ImageFrame
import com.stepwise.core.ImageSource
import com.stepwise.media.ExifOrientation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Importa uma imagem escolhida pelo usuário (desenho técnico ou foto de teste):
 * limita o tamanho lido, reduz para no máximo [MAX_SIDE] px, aplica a orientação
 * EXIF e recodifica em JPEG. Bloqueante: chamar fora da thread principal.
 */
object ImageImport {
    const val MAX_INPUT_BYTES = 12 * 1024 * 1024
    const val MAX_SIDE = 2048

    fun readAsJpeg(resolver: ContentResolver, uri: Uri, source: ImageSource): ImageFrame {
        val input = resolver.openInputStream(uri) ?: error("Imagem indisponível")
        val bytes = input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_INPUT_BYTES) { "Imagem muito grande" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        require(options.outWidth > 0 && options.outHeight > 0) { "Formato inválido" }
        options.inJustDecodeBounds = false
        options.inSampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > MAX_SIDE) options.inSampleSize *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: error("Formato inválido")
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = ExifOrientation.matrix(orientation)
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val encoded = try {
            ByteArrayOutputStream().use {
                check(oriented.compress(Bitmap.CompressFormat.JPEG, 90, it))
                it.toByteArray()
            }
        } finally {
            if (oriented !== bitmap) oriented.recycle()
            bitmap.recycle()
        }
        return ImageFrame(encoded, "image/jpeg", source, System.currentTimeMillis())
    }
}
