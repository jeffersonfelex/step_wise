# CameraAccess — referência e validação B06/B07

## Referência fixada

- Repositório: [Meta Wearables DAT Android](https://github.com/facebook/meta-wearables-dat-android).
- Commit: `974e05c569da8be01c0c9ce8bc5bc79b73ff8540` (14/09/2026, migração para Maven Central).
- Exemplo: `samples/CameraAccess`.
- Checkout local: `.tools/references/meta-wearables-dat-android`, ignorado pelo Git do StepWise.
- Licença original preservada no arquivo `LICENSE` da raiz do checkout. Código oficial não copiado para o app StepWise.
- Local SDK configurado somente no `local.properties` ignorado do exemplo.
- Matriz oficial: Gradle 8.14.1, AGP 8.11.1, Kotlin 2.2.21, DAT 0.9.0, compile/target SDK 36, minSdk 31. JDK local: Temurin 17.0.20.1.
- Build-Tools 35.0.0 instalado automaticamente pelo build oficial; StepWise continua fixado em 36.0.0. Ambas as versões coexistem no SDK.
- Pacote: `com.meta.wearable.dat.externalsampleapps.cameraaccess`; versão 1.0, código 1. Instalação independente de `com.stepwise`.

## Como repetir

Abra `scripts/terminal-java17.cmd` na raiz do StepWise. No terminal preparado:

```bat
cd .tools\references\meta-wearables-dat-android\samples\CameraAccess
gradlew.bat :app:assembleDebug --no-daemon --console=plain --max-workers=2
```

O checkout e o cache `.tools/gradle-cache` são locais e não acompanham um clone do StepWise. Para recriar a referência, clonar o repositório oficial e selecionar o commit acima, preservando a licença. O exemplo usa a chave de assinatura de demonstração fornecida pela Meta; não usar como assinatura de produção.

## Escopo da validação

B06/B07 verificam compilação, instalação, abertura e acesso ao fluxo de registro/Mock Device Kit. O menu de debug permite habilitar MDK, criar Ray-Ban Meta simulados e controlar os estados ligado/aberto/em uso.

No modo real, o [README do exemplo](https://github.com/facebook/meta-wearables-dat-android/blob/974e05c569da8be01c0c9ce8bc5bc79b73ff8540/samples/CameraAccess/README.md) orienta habilitar Developer Mode no Meta AI e usar Connect para registrar. O registro simulado pelo MDK não comprova registro real, pareamento, transmissão Bluetooth, câmera, áudio ou latência dos óculos.

**Compilação aprovada:** `:app:assembleDebug` terminou com BUILD SUCCESSFUL em 9 min 53 s, 37 tarefas executadas. Código oficial sem alterações rastreadas. Avisos não bloqueantes de metadados SDK XML e bibliotecas nativas empacotadas sem remoção de símbolos.

APK de diagnóstico preservado em `.tools/diagnostics/CameraAccess-974e05c-debug.apk`, além da saída original `samples/CameraAccess/app/build/outputs/apk/debug/app-debug.apk`. Ambos locais, ignorados pelo Git.

Primeira tentativa de instalação no POCO recusada com `INSTALL_FAILED_USER_RESTRICTED`. Aguardando confirmação do usuário no aparelho para repetir. B07 ainda depende de instalação/abertura e acesso ao fluxo de registro/MDK.

Após confirmação do usuário, instalação retornou **Success**. CameraAccess 1.0 abriu e exibiu a permissão Android de dispositivos próximos; posteriormente BLUETOOTH_CONNECT foi confirmado como granted=true. Tela inicial observada com instruções de sessão/preview/captura, botão **Connect my glasses** e **Debug menu**. A exploração do MDK foi pausada porque o celular alternou para outro aplicativo; aguardando disponibilidade da tela. Nenhum registro Meta AI ou pareamento real foi concluído.

APK de diagnóstico: 91.096.135 bytes; SHA-256 `6EB87F4FD1CCC168BF879B129DB190151566E5FB49680F70B285CDC90B08A9BD`.

## Resultado final — B06/B07 aprovadas em 16/09/2026

No POCO X8 Pro Max, Android 16/API 36:

1. CameraAccess instalado e aberto; permissão Android BLUETOOTH_CONNECT concedida pelo usuário.
2. Tela inicial de registro exibida com **Connect my glasses** e **Debug menu**.
3. Debug menu → **Enable MockDeviceKit** → **Pair Ray-Ban Meta**: interface confirmou **1 device(s) paired**.
4. Ativados **Power**, **Unfolded** e **Donned**; os três controles confirmados ligados.
5. Fechado o menu: registro simulado levou à tela **Ready**, com **Session: idle**.
6. **Start session**: interface confirmou **Session: started**. O stream permaneceu **stopped**, sem fonte de mídia configurada.
7. **End session**: interface confirmou **Session: stopped** e **Stream: stopped**.
8. MockDeviceKit desabilitado ao encerrar; app voltou a **Connect my glasses**. Nenhum registro real com Meta AI foi feito. StepWise permaneceu instalado.

Evidências locais: `.tools/diagnostics/cameraaccess-mdk-session.png` (captura inspecionada) e `.tools/diagnostics/cameraaccess-mdk-session.xml`. Nenhuma entrada do CameraAccess encontrada no buffer de crash consultado.

**Validado:** referência oficial compilável, instalação/abertura, inicialização suficiente para usar MDK, registro/dispositivo simulados, início e encerramento de sessão simulada. **Não validado:** mídia/frames, captura, registro real Meta AI, pareamento físico, firmware, câmera/áudio dos óculos e latência. Uma sessão iniciada com stream parado não comprova transmissão de vídeo.

Atualização B08: login e Developer Mode no Meta AI confirmados pelo usuário; óculos indisponíveis no momento. Roteiro preparado em [meta-ai-registro-real.md](meta-ai-registro-real.md). Testes com mídia no MDK pertencem às próximas etapas de integração; registro real permanece pendente de hardware.

Meta AI encontrado via ADB: pacote `com.facebook.stella`, versão `289.0.0.25.162`, código `968902270`. A presença do aplicativo não comprova login, Developer Mode ou registro DAT real.
