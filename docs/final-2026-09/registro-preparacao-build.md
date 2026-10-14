# Preparação do build — 16/09/2026

## Estado anterior (B02)

- Branch de origem: `dev/kauan`.
- Commit de origem: `06aa99660b2fbfae4bfdf67c5c8831965801ab3a`.
- Branch de preparação: `prep/final-2026-09-18`.
- Modificados antes desta etapa: `.gitignore` e `README.md`.
- Não rastreados antes desta etapa: `.tmp_analysis/`, `.tmp_pdf_reader/`, `AI Glasses Brasil/`, `docs/PLANO_ACAO_FINAL_STEPWISE.md`, `docs/ambiente-validado.md` e `scripts/`.
- Instalações/configurações locais ignoradas: `.tools/`, `android/.idea/` e `android/.gradle/`.
- Arquivos preexistentes preservados. Nenhum reset, limpeza, commit ou envio remoto feito nesta etapa.

## Escopo

Preparar Wrapper Gradle 8.14.1 e verificar sua execução com JDK 17. A matriz antiga do aplicativo (AGP 8.5.2, Kotlin 1.9.24, SDK 34) ainda será migrada conforme o roadmap; a instalação do Wrapper não comprova compilação nem integração DAT.

## Wrapper e Java

- Wrapper gerado com a distribuição oficial Gradle 8.14.1, em projeto mínimo temporário; tarefa `wrapper` concluída com sucesso.
- Distribuição verificada: SHA-256 `845952a9d6afa783db70bb3b0effaae45ae5542ca2bb7929619e8af49cb634cf`, também fixado no `gradle-wrapper.properties`.
- Wrapper JAR: SHA-256 `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`. Referência: [checksums oficiais](https://gradle.org/release-checksums/).
- `android/gradlew.bat --version`: sucesso; Gradle 8.14.1, Launcher JVM e Daemon JVM em Temurin 17.0.20.1.
- Cache dos testes em `.tools/gradle-cache`. `scripts/terminal-java17.cmd` prepara JAVA_HOME e GRADLE_USER_HOME somente na sessão. Na IDE, usar o mesmo caminho em **Gradle user home** para reaproveitar os downloads; configuração da interface ainda não conferida.
- `android/local.properties` aponta para o SDK deste notebook e permanece ignorado pelo Git.
- B09 permanece parcial até adicionar o catálogo de versões. O Wrapper está preparado para versionamento; nenhum commit foi criado.

## Validação da configuração do aplicativo

`gradlew.bat -p android help --no-daemon --console=plain` terminou com **BUILD SUCCESSFUL**, em 3 min 54 s, usando Temurin 17.0.20.1. Plugins e scripts da configuração atual foram carregados. Isso não executa compilação, testes, instalação ou validação da matriz planejada.

Avisos: configuração de métricas Android sem permissão para gravar `analytics.settings` no perfil do Windows (não impediu a tarefa); recursos Gradle obsoletos incompatíveis com Gradle 9. Não foi feita migração para Gradle 9 nem alteração de permissões para métricas.

Próximo passo: catálogo de versões e aplicação controlada da matriz prevista (B09/B10), seguida de compilação do APK. A seleção da IDE foi preparada em arquivos locais, mas a sincronização pela interface ainda não foi observada.

## Migração da matriz (B09/B10)

Em 16/09/2026, adicionado `android/gradle/libs.versions.toml` e migradas as dependências/plugins para aliases. Matriz aplicada: AGP 8.11.1, Kotlin 2.2.21, KSP 2.2.21-2.0.5, Room 2.8.4, DAT 0.9.0; compile/target SDK 36, minSdk 31, Build-Tools 36.0.0 e toolchain Java 17.

DAT core/camera declarados em implementation e Mock Device Kit somente em debugImplementation. Maven Central conforme o [README oficial atual](https://raw.githubusercontent.com/facebook/meta-wearables-dat-android/main/README.md); removido comentário com repositório fictício. Nenhum token ou ID de aplicação inventado. Registro/inicialização DAT continuam pendentes. Analytics e crash reporting DAT desativados no manifesto conforme opções oficiais.

Correções necessárias do scaffold: adicionado ícone vetorial básico para substituir recurso inexistente; removida referência ao arquivo ProGuard inexistente (minificação permanece desativada). Não foi implementado fluxo de permissões nem tela funcional nesta etapa.

Validação solicitada: `:app:assembleDebug :app:testDebugUnitTest`; registrar o resultado abaixo após conclusão. O teste existente cobre somente imagem inutilizável retornando INCONCLUSIVE; não comprova lógica completa nem transação Room.

### Resultado

**BUILD SUCCESSFUL** em 16/09/2026: `:app:assembleDebug :app:testDebugUnitTest --no-daemon --console=plain --max-workers=2`, 43 tarefas (18 executadas, 25 atualizadas), 2 min 16 s na execução final. Primeiro processamento demorou 8 min 16 s e parou na permissão de criação de `C:\Users\user\.android\debug.keystore.lock`; repetido com autorização de escrita no perfil, sem trocar a assinatura por uma chave de produção.

- APK: `android/app/build/outputs/apk/debug/app-debug.apk`, 43.559.897 bytes, applicationId `com.stepwise`, versão `0.1.0-mvp` (código 1), minSdk 31.
- Room/KSP geraram `AppDatabase_Impl.kt` e `SessionStateDao_Impl.kt`; Kotlin compilou.
- Artefatos DAT core/camera/mockdevice 0.9.0 resolvidos pelo Maven Central e incluídos na variante debug. Integração funcional não iniciada.
- Avisos não bloqueantes: leitor de metadados SDK XML mais antigo que o arquivo instalado; bibliotecas nativas DAT empacotadas sem remoção de símbolos. Revisitar tamanho e empacotamento no pacote final; não instalar NDK apenas para silenciar aviso.
- Não executados: instalação/abertura no POCO, runtime permissions, transação Room, sessão DAT e build offline.

Próximo passo: B11/B12, implementar tela mínima identificando a versão e validar instalação/abertura no POCO. B06/B07 (CameraAccess oficial) continuam pendentes. O APK atual compila o scaffold, mas sua Activity ainda não oferece interface funcional.

## Tela mínima e instalação — B11/B12

Tela XML implementada com versão/build reais via BuildConfig, estado individual de microfone/Bluetooth/câmera do celular e explicação antes do pedido de autorização. Negação mantém a tela acessível; quando o Android deixa de oferecer o diálogo, o botão leva às configurações do aplicativo. Estado atualizado ao retornar. Nenhuma captura ou sessão DAT é iniciada. Insets de barras e recorte da tela tratados para SDK 36; conteúdo rolável.

`assembleDebug testDebugUnitTest --offline --no-daemon --max-workers=2`: **BUILD SUCCESSFUL**, 3 min 6 s, 45 tarefas, teste existente aprovado. O daemon Kotlin encontrou restrição de escrita no perfil e o próprio compilador concluiu pela alternativa interna. Nenhum download necessário nessa execução incremental; isso não garante reconstrução sem cache em outro notebook.

Primeira instalação via ADB recusada pelo POCO: `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`. A Activity não existia no telefone após a tentativa. Solicitada ao usuário habilitação/confirmação da instalação USB. B12 permanece pendente até instalar e abrir. B11 implementada, com validação de permissões no telefone ainda pendente.

### Validação final no POCO — 16/09/2026

Após confirmação do usuário de que instalação USB estava ativa:

- `adb -d install -r .../app-debug.apk`: **Success**.
- `am start -W -n com.stepwise/.ui.MainActivity`: **Status ok**, abertura fria, TotalTime 718 ms (tempo de lançamento informado pelo Android; não é latência funcional do projeto).
- UI Automator confirmou versão `0.1.0-mvp`, build `1`, variante debug e estados iniciais não autorizados.
- Na captura visual seguinte, as três permissões já estavam autorizadas. `dumpsys package com.stepwise` confirmou RECORD_AUDIO, BLUETOOTH_CONNECT e CAMERA com `granted=true` e USER_SET. Nenhuma concessão via ADB foi usada.
- App encerrado com `am force-stop` e reaberto: **Status ok**, TotalTime 343 ms. UI voltou com versão/build corretos, permissões autorizadas e botões correspondentes desabilitados. Processo vivo após inspeção; nenhuma entrada com `com.stepwise` no buffer de crash consultado.
- Captura de tela inspecionada: conteúdo legível, versão visível, sem sobreposição com as barras do sistema. Evidências locais ignoradas pelo Git: `.tools/stepwise-screen.png` e `.tools/stepwise-ui.xml`.
- APK instalado: SHA-256 `CF7D704AE7B8F8B551A87C1DA8B14A9DBBBFFCEBB9C904B7C40DCD0063AEF730`.

B11/B12 concluídas dentro do escopo de tela mínima; B14 validada pelo build offline anterior. Recusa, bloqueio permanente e revogação nas configurações **ainda não exercitados**; autorizações existentes foram preservadas. Sem captura de câmera/microfone, pareamento ou sessão DAT. Próxima tarefa: B13 (teste insert/read Room).

## B13 — teste instrumentado do Room

Teste em `android/app/src/androidTest/java/com/stepwise/data/db/SessionStateDaoTest.kt`, usando o schema e DAO reais. Cada caso cria um arquivo de banco com nome exclusivo `stepwise-room-test-<UUID>.db`, fecha e remove somente esse arquivo ao terminar. Não usa o singleton nem o arquivo `stepwise.db`.

Cobertura:

1. Consulta vazia, inserção com ID gerado, leitura de todos os campos e persistência após fechar/reabrir o banco.
2. Último checkpoint por ordem de inserção (contrato atual do DAO), isolamento entre procedimentos, consulta inexistente e remoção de um procedimento preservando o outro.

Execução: `gradlew.bat -p android :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.stepwise.data.db.SessionStateDaoTest --no-daemon --console=plain --max-workers=2`, com JDK 17 e cache local configurados como nas etapas anteriores.

A [documentação oficial Room](https://developer.android.com/training/data-storage/room/testing-db) recomenda testes em dispositivo Android. Aqui o banco temporário é em disco para também verificar reabertura; não é um mock. O teste não valida morte do processo, migrações, concorrência, identificação do aluno ou restauração pelo controlador da sessão.

### Resultado B13

**Aprovado no POCO X8 Pro Max (modelo técnico 2602BPC18G), Android 16/API 36, em 16/09/2026.** Dois testes executados, zero falhas, zero erros e zero ignorados. `connectedDebugAndroidTest` terminou com BUILD SUCCESSFUL em 4 min 16 s (inclui compilação/instalação; não representa latência do banco). Relatório: `android/app/build/reports/androidTests/connected/debug/index.html`; XML em `android/app/build/outputs/androidTest-results/connected/`.

Confirmados insert/read com todos os campos, ID autogerado, persistência após fechar/reabrir conexão, último registro por inserção, isolamento entre procedimentos e limpeza seletiva. Schema/DAO de produção não precisaram de alteração. O banco temporário é removido pelo teardown de cada teste.

**Efeito do executor:** ao terminar, o Gradle removeu os APKs do dispositivo, inclusive com.stepwise, conforme verificado por `run-as` e tentativa de abertura. A reinstalação foi solicitada imediatamente, mas o POCO retornou INSTALL_FAILED_USER_RESTRICTED; aguardando confirmação física do usuário. Essa remoção pode apagar dados/permissões do app; em próximas execuções instrumentadas, usar aparelho/perfil de testes ou verificar a política de retenção dos APKs antes de executar sobre dados úteis. Não repetir essa tarefa sobre sessões reais sem tratar esse efeito.

B13 concluída; reinstalação do app de demonstração acompanhada separadamente. B06/B07 (CameraAccess oficial) continuam pendentes e são a próxima validação Android recomendada antes da integração DAT no StepWise.

**Reinstalação concluída:** após confirmação do usuário, `adb install -r` retornou Success e `am start -W` retornou Status ok (abertura fria, 571 ms). RECORD_AUDIO, CAMERA e BLUETOOTH_CONNECT retornaram `granted=false`, coerente com remoção/reinstalação. App deixado aberto no POCO; usuário informado de que pode autorizar novamente pela interface. Não houve concessão por ADB.
