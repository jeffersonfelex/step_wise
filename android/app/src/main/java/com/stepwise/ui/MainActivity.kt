package com.stepwise.ui

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stepwise.BuildConfig
import com.stepwise.R
import com.stepwise.audio.MicrophoneCaptureManager
import com.stepwise.audio.TtsManager
import com.stepwise.audio.VoiceInputManager
import com.stepwise.session.DemoSessionController
import com.stepwise.session.DemoSessionState
import com.stepwise.session.VoiceCommand
import com.stepwise.vision.StepwiseImageClassifier
import kotlin.concurrent.thread

/** Installation and permission check only; no capture or DAT session is started here. */
class MainActivity : AppCompatActivity() {
    private data class PermissionRow(
        val permission: String,
        val label: Int,
        val reason: Int,
        val status: Int,
        val button: Int,
    )

    private val rows = listOf(
        PermissionRow(Manifest.permission.RECORD_AUDIO, R.string.microphone,
            R.string.microphone_reason, R.id.microphone_status, R.id.microphone_permission),
        PermissionRow(Manifest.permission.BLUETOOTH_CONNECT, R.string.bluetooth,
            R.string.bluetooth_reason, R.id.bluetooth_status, R.id.bluetooth_permission),
        PermissionRow(Manifest.permission.CAMERA, R.string.phone_camera,
            R.string.camera_reason, R.id.camera_status, R.id.camera_permission),
    )
    private val permissionHistory by lazy { getSharedPreferences("permission_history", MODE_PRIVATE) }
    private val demoController = DemoSessionController()
    private val ttsManager by lazy { TtsManager(this) }
    private val voiceInputManager by lazy { VoiceInputManager(::handleVoiceCommand) }
    private val microphoneCaptureManager by lazy { MicrophoneCaptureManager(this) }

    // The returned map survives Activity recreation through ActivityResultRegistry.
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.isNotEmpty()) {
            val editor = permissionHistory.edit()
            results.keys.forEach { editor.putBoolean(it, true) }
            editor.apply()
        }
        refreshPermissions()
    }

    private val pickImageForVerification = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            findViewById<TextView>(R.id.demo_decision).text = "Fonte local: nenhuma imagem selecionada."
        } else {
            verifyDemoWithPickedImage(uri)
        }
    }

    private val takePictureForVerification = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) {
            findViewById<TextView>(R.id.demo_decision).text = "Camera local: nenhuma foto capturada."
        } else {
            verifyDemoWithBitmap(bitmap, sourceLabel = "Foto da camera")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        val root = findViewById<android.view.View>(R.id.main_scroll)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
        findViewById<TextView>(R.id.build_info).text = getString(
            R.string.build_info, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.BUILD_TYPE
        )
        rows.forEach { row ->
            findViewById<MaterialButton>(row.button).setOnClickListener { request(row) }
        }
        findViewById<MaterialButton>(R.id.demo_correct).setOnClickListener {
            renderDemo(demoController.verifyCorrect(), speak = true)
        }
        findViewById<MaterialButton>(R.id.demo_error).setOnClickListener {
            renderDemo(demoController.verifyKnownError(), speak = true)
        }
        findViewById<MaterialButton>(R.id.demo_inconclusive).setOnClickListener {
            renderDemo(demoController.verifyInconclusive(), speak = true)
        }
        findViewById<MaterialButton>(R.id.demo_visual_verify).setOnClickListener {
            verifyDemoWithLocalImage()
        }
        findViewById<MaterialButton>(R.id.demo_pick_image_verify).setOnClickListener {
            pickImageForVerification.launch("image/*")
        }
        findViewById<MaterialButton>(R.id.demo_camera_verify).setOnClickListener {
            launchCameraVerification()
        }
        findViewById<MaterialButton>(R.id.demo_help).setOnClickListener {
            renderDemo(demoController.help(), speak = true)
        }
        findViewById<MaterialButton>(R.id.demo_repeat).setOnClickListener {
            renderDemo(demoController.repeat(), speak = true)
        }
        findViewById<MaterialButton>(R.id.demo_reset).setOnClickListener {
            renderDemo(demoController.reset(), speak = true)
        }
        findViewById<MaterialButton>(R.id.voice_verify).setOnClickListener {
            simulateVoiceTranscript("verificar")
        }
        findViewById<MaterialButton>(R.id.voice_help).setOnClickListener {
            simulateVoiceTranscript("ajuda")
        }
        findViewById<MaterialButton>(R.id.voice_repeat).setOnClickListener {
            simulateVoiceTranscript("repetir")
        }
        findViewById<MaterialButton>(R.id.voice_unknown).setOnClickListener {
            simulateVoiceTranscript("texto sem comando")
        }
        findViewById<MaterialButton>(R.id.mic_test_start).setOnClickListener {
            startMicrophoneTest()
        }
        findViewById<MaterialButton>(R.id.vision_diag_run).setOnClickListener {
            runVisionDiagnostic()
        }
        renderDemo(demoController.state())
        renderVoiceStatus(VoiceCommand.UNKNOWN)
        refreshPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    override fun onDestroy() {
        microphoneCaptureManager.stop()
        ttsManager.shutdown()
        super.onDestroy()
    }

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(
        this, permission
    ) == PackageManager.PERMISSION_GRANTED

    private fun requiresSettings(permission: String) =
        !granted(permission) && permissionHistory.getBoolean(permission, false) &&
            !shouldShowRequestPermissionRationale(permission)

    private fun request(row: PermissionRow) {
        if (granted(row.permission)) {
            refreshPermissions()
            return
        }
        if (requiresSettings(row.permission)) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)))
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(row.label)
            .setMessage(row.reason)
            .setNegativeButton(R.string.permission_cancel, null)
            .setPositiveButton(R.string.permission_continue) { _, _ ->
                requestPermission.launch(arrayOf(row.permission))
            }
            .show()
    }

    private fun refreshPermissions() {
        rows.forEach { row ->
            val allowed = granted(row.permission)
            val blocked = requiresSettings(row.permission)
            val label = getString(row.label)
            val state = when {
                allowed -> R.string.permission_granted
                blocked -> R.string.permission_blocked
                else -> R.string.permission_not_granted
            }
            findViewById<TextView>(row.status).text = getString(
                R.string.permission_status, label, getString(state)
            )
            findViewById<MaterialButton>(row.button).apply {
                text = getString(when {
                    allowed -> R.string.permission_done
                    blocked -> R.string.permission_settings
                    else -> R.string.permission_request
                }, label)
                isEnabled = !allowed
            }
        }
    }

    private fun launchCameraVerification() {
        if (!granted(Manifest.permission.CAMERA)) {
            findViewById<TextView>(R.id.demo_decision).text = "Camera local: autorize a camera do celular antes de capturar."
            requestPermission.launch(arrayOf(Manifest.permission.CAMERA))
            return
        }
        takePictureForVerification.launch(null)
    }

    private fun verifyDemoWithBitmap(bitmap: android.graphics.Bitmap, sourceLabel: String) {
        val cameraButton = findViewById<MaterialButton>(R.id.demo_camera_verify)
        val pickButton = findViewById<MaterialButton>(R.id.demo_pick_image_verify)
        val decision = findViewById<TextView>(R.id.demo_decision)
        cameraButton.isEnabled = false
        pickButton.isEnabled = false
        decision.text = "$sourceLabel: classificando imagem..."
        thread(name = "stepwise-bitmap-verify") {
            try {
                val top3 = StepwiseImageClassifier(this).use { classifier ->
                    classifier.classify(bitmap)
                }
                bitmap.recycle()
                val best = top3.first()
                val state = demoController.verifyVisualClassification(best.label, best.confidence)
                val confidence = String.format("%.2f", best.confidence)
                val topSummary = top3.joinToString { "${it.label}:${String.format("%.2f", it.confidence)}" }
                runOnUiThread {
                    renderDemo(
                        state.copy(
                            lastDecision = "$sourceLabel: top=${best.label} conf=$confidence\nTop-3: $topSummary\n${state.lastDecision}"
                        ),
                        speak = true,
                    )
                    cameraButton.isEnabled = !state.completed
                    pickButton.isEnabled = !state.completed
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    decision.text = "$sourceLabel: falha - ${error.message}"
                    cameraButton.isEnabled = true
                    pickButton.isEnabled = true
                }
            }
        }
    }

    private fun verifyDemoWithPickedImage(uri: Uri) {
        val button = findViewById<MaterialButton>(R.id.demo_pick_image_verify)
        val decision = findViewById<TextView>(R.id.demo_decision)
        button.isEnabled = false
        decision.text = "Fonte local: classificando imagem selecionada..."
        thread(name = "stepwise-picked-image-verify") {
            try {
                val bitmap = contentResolver.openInputStream(uri).use { input ->
                    BitmapFactory.decodeStream(input)
                }
                requireNotNull(bitmap) { "Nao foi possivel abrir a imagem selecionada" }
                runOnUiThread {
                    button.isEnabled = true
                    verifyDemoWithBitmap(bitmap, sourceLabel = "Imagem selecionada")
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    decision.text = "Fonte local: falha - ${error.message}"
                    button.isEnabled = true
                }
            }
        }
    }

    private fun verifyDemoWithLocalImage() {
        val button = findViewById<MaterialButton>(R.id.demo_visual_verify)
        val decision = findViewById<TextView>(R.id.demo_decision)
        val sample = demoController.expectedVisualSampleForCurrentStep()
        if (sample == null) {
            decision.text = "Diagnostico visual: procedimento concluido ou sem imagem local."
            return
        }
        button.isEnabled = false
        decision.text = "Diagnostico visual: verificando ${sample.expectedLabel}..."
        thread(name = "stepwise-demo-visual-verify") {
            try {
                val result = StepwiseImageClassifier(this).use { classifier ->
                    classifier.classifyAsset(this, sample)
                }
                val best = result.top3.first()
                val state = demoController.verifyVisualClassification(best.label, best.confidence)
                val confidence = String.format("%.2f", best.confidence)
                runOnUiThread {
                    renderDemo(
                        state.copy(
                            lastDecision = "Imagem local ${result.expectedLabel}: top=${best.label} conf=$confidence\n${state.lastDecision}"
                        ),
                        speak = true,
                    )
                    button.isEnabled = !state.completed
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    decision.text = "Diagnostico visual: falha - ${error.message}"
                    button.isEnabled = true
                }
            }
        }
    }

    private fun runVisionDiagnostic() {
        val status = findViewById<TextView>(R.id.vision_diag_status)
        val button = findViewById<MaterialButton>(R.id.vision_diag_run)
        button.isEnabled = false
        status.text = "Diagnostico visual: executando..."
        thread(name = "stepwise-vision-diagnostic") {
            try {
                val results = StepwiseImageClassifier(this).use { classifier ->
                    classifier.runAssetDiagnostic(this)
                }
                val summary = buildString {
                    appendLine("Diagnostico visual: ${results.size} imagens")
                    results.forEach { result ->
                        val best = result.top3.first()
                        val confidence = String.format("%.2f", best.confidence)
                        appendLine("${result.expectedLabel}: top=${best.label} conf=$confidence")
                    }
                }.trim()
                runOnUiThread {
                    status.text = summary
                    button.isEnabled = true
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    status.text = "Diagnostico visual: falha - ${error.message}"
                    button.isEnabled = true
                }
            }
        }
    }

    private fun startMicrophoneTest() {
        val status = findViewById<TextView>(R.id.mic_test_status)
        findViewById<MaterialButton>(R.id.mic_test_start).isEnabled = false
        status.text = "Microfone: iniciando captura..."
        microphoneCaptureManager.startThreeSecondTest(object : MicrophoneCaptureManager.Listener {
            override fun onStarted(info: MicrophoneCaptureManager.CaptureInfo) {
                runOnUiThread {
                    status.text = "Microfone: capturando PCM ${info.sampleRateHz} Hz mono"
                }
            }

            override fun onLevel(rms: Int) {
                runOnUiThread {
                    status.text = "Microfone: capturando, nivel RMS=$rms"
                }
            }

            override fun onFinished(totalBytes: Int) {
                runOnUiThread {
                    status.text = "Microfone: captura concluida, $totalBytes bytes lidos"
                    findViewById<MaterialButton>(R.id.mic_test_start).isEnabled = true
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    status.text = "Microfone: falha - $message"
                    findViewById<MaterialButton>(R.id.mic_test_start).isEnabled = true
                }
            }
        })
    }

    private fun simulateVoiceTranscript(transcript: String) {
        voiceInputManager.startListeningWindow()
        val command = voiceInputManager.handleTranscript(transcript)
        renderVoiceStatus(command)
        if (command == VoiceCommand.UNKNOWN) {
            ttsManager.speak("Nao entendi o comando. Diga verificar, ajuda ou repetir.")
        }
    }

    private fun handleVoiceCommand(command: VoiceCommand) {
        when (command) {
            VoiceCommand.VERIFY -> renderDemo(demoController.verifyCorrect(), speak = true)
            VoiceCommand.HELP -> renderDemo(demoController.help(), speak = true)
            VoiceCommand.REPEAT -> renderDemo(demoController.repeat(), speak = true)
            VoiceCommand.UNKNOWN -> Unit
        }
    }

    private fun renderVoiceStatus(command: VoiceCommand) {
        findViewById<TextView>(R.id.voice_status).text = getString(R.string.voice_status, command.name)
    }

    private fun renderDemo(state: DemoSessionState, speak: Boolean = false) {
        findViewById<TextView>(R.id.demo_status).text = getString(
            R.string.demo_status,
            state.title,
            state.stepText,
        )
        findViewById<TextView>(R.id.demo_instruction).text = state.instruction
        findViewById<TextView>(R.id.demo_decision).text = state.lastDecision
        findViewById<MaterialButton>(R.id.demo_correct).isEnabled = !state.completed
        findViewById<MaterialButton>(R.id.demo_error).isEnabled = !state.completed
        findViewById<MaterialButton>(R.id.demo_inconclusive).isEnabled = !state.completed
        findViewById<MaterialButton>(R.id.demo_visual_verify).isEnabled = !state.completed
        findViewById<MaterialButton>(R.id.demo_pick_image_verify).isEnabled = !state.completed
        findViewById<MaterialButton>(R.id.demo_camera_verify).isEnabled = !state.completed
        if (speak) {
            ttsManager.speak(speechFor(state))
        }
    }

    private fun speechFor(state: DemoSessionState): String {
        if (state.completed) {
            return "Procedimento concluido. Montagem finalizada."
        }
        val feedback = state.lastDecision.substringAfter('\n', missingDelimiterValue = "")
        return if (feedback.isBlank()) {
            state.instruction
        } else {
            "$feedback ${state.instruction}"
        }
    }
}
