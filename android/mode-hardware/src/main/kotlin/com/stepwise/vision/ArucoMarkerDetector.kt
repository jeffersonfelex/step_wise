package com.stepwise.vision

/**
 * PLANO B (determinístico) do detector visual — marcadores ArUco/AprilTag.
 *
 * Alternativa à segmentação por cor (ver ColorSegmentationDetector) para
 * quando a identidade/posição da peça precisa ser mais robusta do que cor
 * permite (ex.: iluminação muito variável no local do evento). Um
 * marcador colado em cada peça e na base dá identidade + posição em
 * milissegundos, sem depender de um modelo treinado.
 *
 * Biblioteca sugerida: BoofCV (`org.boofcv:boofcv-core` /
 * `boofcv-android`) — é pure Kotlin/Java, sem binários nativos, o que
 * reduz risco de integração sob pressão de tempo comparado a
 * OpenCV + módulo aruco. Ver dependências comentadas em app/build.gradle.kts.
 *
 * [markerIdToClassName] mapeia o ID numérico de cada marcador para a
 * classe esperada pelo motor procedural (mesmos nomes usados no JSON do
 * procedimento, ex.: "engrenagem_azul"). Definir esse mapeamento ao
 * imprimir/colar os marcadores nas peças reais.
 */
class ArucoMarkerDetector(
    private val markerIdToClassName: Map<Int, String>
) : VisualDetector {

    override suspend fun detect(frame: ByteArray): DetectionResult {
        // TODO: decodificar `frame` para o formato de imagem esperado pelo
        // BoofCV (ex.: GrayU8) e rodar o detector de fiduciais quadrados
        // binários (ex.: FiducialDetector do BoofCV) sobre a imagem.
        // TODO: para cada marcador detectado, mapear o ID via
        // markerIdToClassName e converter o quadrilátero em BoundingBox
        // normalizada (0..1) pela largura/altura do frame.
        // TODO: confidence pode ser fixo em 1.0 (detecção binária) ou
        // usar a métrica de qualidade de decodificação do BoofCV.
        return DetectionResult(detections = emptyList())
    }
}
