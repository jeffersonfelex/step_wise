package com.stepwise.glasses

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.meta.wearable.dat.camera.Camera
import com.meta.wearable.dat.camera.Stream
import com.meta.wearable.dat.camera.addCamera
import com.meta.wearable.dat.camera.types.PhotoData
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.StreamState
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Encapsula a sessão real com os óculos (Ray-Ban Meta Gen 2) via Meta
 * Wearables Device Access Toolkit (com.meta.wearable:mwdat-core/mwdat-camera,
 * ver settings.gradle.kts para o repositório/token do GitHub Packages).
 *
 * IMPORTANTE — origem desta implementação: escrita a partir da documentação
 * oficial (wearables.developer.meta.com) e do código-fonte real do sample
 * `samples/CameraAccess` do repositório facebook/meta-wearables-dat-android
 * (CameraViewModel.kt), lidos remotamente nesta sessão. NÃO foi compilada
 * nem testada aqui (este ambiente não tem Android SDK/JDK 17 nem os óculos
 * físicos) — a primeira coisa a checar num erro de compilação em Android
 * Studio são os `import com.meta.wearable.dat.*` abaixo (nomes de pacote
 * confirmados a partir do código-fonte real, exceto `AutoDeviceSelector`,
 * inferido por convenção do mesmo pacote de `DeviceSelector`).
 *
 * Decisão revisada nesta integração: usar `stream.capturePhoto()` (uma foto
 * HEIC/Bitmap por verificação) em vez de decodificar o stream de vídeo HEVC
 * ao vivo. O motivo anterior documentado aqui (relato de bug: capturePhoto()
 * não entregaria a foto com o HFP ativo) segue válido e não foi confirmado
 * nem descartado — decodificar HEVC ao vivo exigiria um pipeline
 * MediaCodec/Surface completo (ver HevcDecoder do sample oficial), que não
 * dá para escrever com confiança sem compilar/testar no hardware real hoje.
 * Troca de escopo consciente (permitida pelo §14.1 — troca de
 * biblioteca/abordagem, não do problema/usuário/valor). Teste com os óculos
 * reais assim que possível: se capturePhoto() falhar/travar com um comando
 * de voz recém-falado, a mitigação mais rápida é reduzir por um instante o
 * volume/uso do HFP ao redor da chamada, ou voltar a abrir o stream de
 * vídeo e usar QualityGate.pickBest sobre os frames dele.
 *
 * Regras de bateria (ver POLICIES no diagrama / BatteryMonitor):
 *  - addCamera(...) só quando uma verificação é pedida (por voz), nunca em
 *    polling contínuo.
 *  - camera.stop() (via stopStream()) deve ser chamado ao encerrar a sessão.
 */
class MetaDatSessionManager(
    private val qualityGate: QualityGate = QualityGate()
) {

    private var session: DeviceSession? = null
    private var camera: Camera? = null
    private var stream: Stream? = null

    /**
     * Garante uma sessão + stream ativos com os óculos e captura uma foto
     * (JPEG re-codificado a partir do HEIC/Bitmap entregue pelo SDK),
     * validada pelo QualityGate. Retorna null se a sessão/stream não
     * conectarem a tempo, ou se a captura falhar — o chamador
     * (SessionController) trata null como INCONCLUSIVE.
     */
    suspend fun captureBestFrameFromStream(windowMillis: Long = STREAM_WINDOW_MILLIS_DEFAULT): ByteArray? {
        val activeStream = ensureStreamingStream() ?: return null

        val jpeg = withTimeoutOrNull(PHOTO_CAPTURE_TIMEOUT_MILLIS) {
            var captured: ByteArray? = null
            activeStream
                .capturePhoto()
                .onSuccess { photo -> captured = decodePhotoToJpeg(photo) }
                .onFailure { error, _ ->
                    Log.w(TAG, "capturePhoto() falhou: ${error.description}")
                }
            captured
        }

        if (jpeg == null) {
            Log.w(TAG, "capturePhoto() sem retorno em ${PHOTO_CAPTURE_TIMEOUT_MILLIS}ms.")
            return null
        }
        return if (qualityGate.isUsable(jpeg)) jpeg else null
    }

    /** Plano B mantido por compatibilidade — o fluxo principal já é capturePhoto() acima. */
    suspend fun capturePhoto(): ByteArray =
        captureBestFrameFromStream()
            ?: throw IllegalStateException("Não foi possível capturar uma foto dos óculos agora.")

    /** Encerra o stream de câmera (mantém a sessão com os óculos conectada). */
    fun stopStream() {
        camera?.close() // Camera é Closeable; fechar cascateia para o Stream filho.
        camera = null
        stream = null
    }

    // -------------------------------------------------------------------
    // Sessão/stream — ver DeviceSessionState/StreamState (SDK real).
    // -------------------------------------------------------------------

    private suspend fun ensureStreamingStream(): Stream? {
        val activeSession = ensureSessionStarted() ?: return null

        stream?.let { existing ->
            if (existing.state.first() == StreamState.STREAMING) return existing
            // Estado != STREAMING (ex.: óculos desconectaram no meio da sessão) — descarta e reabre.
            stopStream()
        }

        val newCamera = runCatching {
            var added: Camera? = null
            activeSession
                .addCamera(
                    StreamConfiguration(
                        videoQuality = VideoQuality.HIGH,
                        frameRate = STREAM_FPS_DEFAULT,
                        compressVideo = true
                    )
                )
                .onSuccess { added = it }
                .onFailure { error, _ -> Log.w(TAG, "addCamera() falhou: ${error.description}") }
            added
        }.getOrNull() ?: return null

        camera = newCamera
        val newStream = newCamera.stream
        stream = newStream

        newStream.start().onFailure { error, _ ->
            Log.w(TAG, "stream.start() falhou: ${error.description}")
        }

        val reached = withTimeoutOrNull(STREAM_START_TIMEOUT_MILLIS) {
            newStream.state.first {
                it == StreamState.STREAMING || it == StreamState.STOPPED || it == StreamState.CLOSED
            }
        }
        if (reached != StreamState.STREAMING) {
            Log.w(TAG, "Stream de câmera não chegou a STREAMING a tempo (estado=$reached).")
            stopStream()
            return null
        }
        return newStream
    }

    private suspend fun ensureSessionStarted(): DeviceSession? {
        session?.let { existing ->
            when (existing.state.first()) {
                DeviceSessionState.STARTED -> return existing
                DeviceSessionState.STOPPED -> { /* recria abaixo */ }
                else -> {
                    val reached = withTimeoutOrNull(SESSION_START_TIMEOUT_MILLIS) {
                        existing.state.first {
                            it == DeviceSessionState.STARTED || it == DeviceSessionState.STOPPED
                        }
                    }
                    if (reached == DeviceSessionState.STARTED) return existing
                }
            }
        }

        val created = runCatching {
            var result: DeviceSession? = null
            Wearables.createSession(AutoDeviceSelector())
                .onSuccess { result = it }
                .onFailure { error, _ -> Log.w(TAG, "createSession() falhou: ${error.description}") }
            result
        }.getOrNull() ?: return null

        session = created
        created.start()

        val reached = withTimeoutOrNull(SESSION_START_TIMEOUT_MILLIS) {
            created.state.first { it == DeviceSessionState.STARTED || it == DeviceSessionState.STOPPED }
        }
        if (reached != DeviceSessionState.STARTED) {
            Log.w(
                TAG,
                "Sessão com os óculos não conectou a tempo (estado=$reached) — " +
                    "óculos pareados, ligados e com o app Meta AI configurado?"
            )
            session = null
            return null
        }
        return created
    }

    // -------------------------------------------------------------------
    // PhotoData (HEIC/Bitmap) -> JPEG, com correção de orientação EXIF.
    // Lógica replicada de CameraViewModel.decodePhoto/decodeWithOrientation
    // do sample oficial (ver KDoc da classe).
    // -------------------------------------------------------------------

    private fun decodePhotoToJpeg(photo: PhotoData): ByteArray? {
        val bitmap =
            when (photo) {
                is PhotoData.Bitmap -> photo.bitmap
                is PhotoData.HEIC -> decodeHeicWithOrientation(photo.data)
            } ?: return null

        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    // Óculos gravam a orientação num campo EXIF que o BitmapFactory não
    // aplica sozinho para HEIC — sem isso a imagem chega de lado para a IA.
    private fun decodeHeicWithOrientation(data: ByteBuffer): Bitmap? {
        val buffer = data.duplicate().apply { rewind() }
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap == null || bitmap.width == 0 || bitmap.height == 0) {
            bitmap?.recycle()
            Log.w(TAG, "Falha ao decodificar a foto HEIC dos óculos.")
            return null
        }

        val matrix = exifOrientationMatrix(bytes)
        if (matrix.isIdentity) return bitmap

        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                bitmap.recycle()
            }
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Falha ao rotacionar a foto (sem memória); usando sem rotação.", e)
            bitmap
        }
    }

    private fun exifOrientationMatrix(bytes: ByteArray): Matrix {
        val orientation =
            try {
                ByteArrayInputStream(bytes).use { input ->
                    ExifInterface(input)
                        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            } catch (e: IOException) {
                Log.w(TAG, "Falha ao ler orientação EXIF.", e)
                ExifInterface.ORIENTATION_NORMAL
            }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        return matrix
    }

    companion object {
        private const val TAG = "MetaDatSessionManager"
        const val STREAM_FPS_DEFAULT = 7
        const val STREAM_WINDOW_MILLIS_DEFAULT = 1_000L
        private const val SESSION_START_TIMEOUT_MILLIS = 8_000L
        private const val STREAM_START_TIMEOUT_MILLIS = 5_000L
        private const val PHOTO_CAPTURE_TIMEOUT_MILLIS = 6_000L
        private const val JPEG_QUALITY = 90
    }
}
