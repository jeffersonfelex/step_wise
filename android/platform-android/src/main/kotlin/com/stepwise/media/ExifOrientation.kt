package com.stepwise.media

import android.graphics.Matrix

/**
 * Matriz que desfaz a orientação EXIF de uma foto (valores 1..8, os mesmos em
 * android.media.ExifInterface e androidx.exifinterface). Sem ela, a imagem
 * enviada a um modelo ou exibida pode chegar de lado ou espelhada.
 */
object ExifOrientation {
    fun matrix(orientation: Int): Matrix = Matrix().apply {
        when (orientation) {
            2 -> setScale(-1f, 1f) // FLIP_HORIZONTAL
            3 -> setRotate(180f) // ROTATE_180
            4 -> setScale(1f, -1f) // FLIP_VERTICAL
            5 -> { setRotate(90f); postScale(-1f, 1f) } // TRANSPOSE
            6 -> setRotate(90f) // ROTATE_90
            7 -> { setRotate(-90f); postScale(-1f, 1f) } // TRANSVERSE
            8 -> setRotate(-90f) // ROTATE_270
        }
    }
}
