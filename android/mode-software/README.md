# Modo software — tutor CAD

Domínio Kotlin/JVM e adaptador Gemini Live. Independente de detectores físicos,
Android SDK e Meta DAT. A interface e áudio Android estão em `../software-android/`.

## Fluxo implementado

1. Selecionar uma imagem do desenho e informar o objetivo (Fusion 360 por padrão).
2. Conectar explicitamente com um token temporário; o desenho é enviado ao Gemini.
3. Receber e revisar um plano de até 20 etapas antes de aceitá-lo.
4. Conversar por texto ou PCM, com resposta falada em streaming e transcrição.
5. Verificar uma etapa com uma imagem e receber `CORRECT`, `KNOWN_ERROR` ou
   `INCONCLUSIVE`. A avaliação precisa corresponder ao objetivo e à tentativa atuais.
6. Confirmar o avanço no app, habilitado somente após avaliação correta.

O modelo não opera o Fusion nem mede diretamente sua geometria. A qualidade da
orientação depende das imagens e do modelo e ainda precisa ser avaliada em bancada.

## Arquivos principais

- `CadProgress.kt`: plano aceito, etapa atual, correlação de tentativas e avanço manual.
- `SoftwareTutorProvider.kt`: entradas e eventos independentes do fornecedor.
- `SoftwareTutorSession.kt`: conexão e captura via `FrameSource`.
- `providers/GeminiLiveProvider.kt`: WebSocket com token temporário e timeout de setup.
- `providers/GeminiLiveCodec.kt`: áudio, imagens, transcrições e ferramentas estruturadas.

O adaptador usa `propose_plan` e `report_assessment`; não interpreta frases livres
como autorização para avançar. O aplicativo revisa localmente os eventos. Não há
chave de API embutida no APK nem gravação de mídia ou conversas em disco pelo app.

## Testes sem Android e sem credenciais

```bash
cd android
./gradlew -p jvm-tests :mode-software:test
```

Incluem progresso, avaliações antigas, PCM, ferramentas inválidas, interrupção,
handshake WebSocket e erro de autenticação com servidor local simulado. Não fazem
chamadas à API real nem comprovam qualidade CAD, latência ou roteamento nos óculos.

## Testar no celular

Veja [o roteiro de teste](../../docs/modo-software-primeiro-fluxo.md), incluindo
como gerar um token no computador. O modelo padrão é `gemini-3.8-live`, editável
na tela; disponibilidade depende da conta e do serviço.

## Próximo trabalho

- Validar a sessão real com token e o POCO, incluindo áudio e interrupções.
- Conectar captura e microfone dos óculos quando a integração compartilhada estiver
  pronta. A imagem usa `GlassesFrameSource`; o microfone Android atual é um fallback.
- Medir latência/qualidade e testar um provedor local com os mesmos contratos.
- Implementar renovação/retomada de sessões, persistência de progresso e métricas.

Não importar o motor físico nem seus detectores. Adaptadores que dependem de
Android pertencem a `software-android`, preservando os testes JVM independentes.
