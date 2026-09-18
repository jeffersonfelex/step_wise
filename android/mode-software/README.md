# Modo software — ponto de partida

Este é o diretório de trabalho da tutoria CAD. Kotlin/JVM, sem Android SDK,
Meta DAT, detectores de peças ou dependências de um provedor de IA.
Os óculos continuam sendo a entrada de imagem e voz em produção.

## Implementado nesta base

- `CadTask`: desenho técnico, objetivo e aplicação CAD (Fusion 360 por padrão).
- `SoftwareTutorProvider` / `SoftwareTutorConnection`: conexão substituível com
  IA local ou remota; entradas de texto, PCM, verificação com imagem e interrupção.
- `SoftwareTutorEvent`: texto e áudio incrementais, plano proposto, avaliação,
  término de turno e erro.
- `SoftwareTutorSession`: ciclo de vida e captura de evidência via `FrameSource`.
  Sem imagem, `verify` retorna `false` e não envia uma verificação ao modelo.
- Testes com provedor e captura falsos, executáveis sem chaves ou hardware.

Nenhum provedor real está conectado. Os testes não simulam competência CAD;
verificam os contratos e o fluxo. Não há envio à nuvem nesta alteração.

## Como testar

Da pasta `android/`, com JDK 17:

```bash
./gradlew -p jvm-tests :mode-software:test
```

No Windows, substitua `./gradlew` por `gradlew.bat`.

## Próximas implementações da equipe software

1. Implementar `SoftwareTutorProvider` para o provedor escolhido. SDKs de nuvem
   JVM podem ficar em um subpacote `providers` aqui. Runtimes que dependem de
   Android devem ficar em um módulo adaptador Android separado, dependente deste.
2. Coletar `session.events` antes de enviar entradas; conectar áudio incremental
   à reprodução no celular/óculos e interromper/limpar a reprodução local junto
   de `SoftwareTutorInput.Interrupt`.
3. Conectar entrada PCM dos óculos; não reutilizar o parser de três comandos do
   hardware como reconhecedor de conversa livre.
4. Revisar/aceitar `PlanProposed`, manter objetivo atual e histórico na aplicação.
   Uma `Assessment` é evidência; não avança etapas automaticamente.
5. Conectar `GlassesFrameSource` na composição do app quando Meta DAT estiver
   implementado; nos testes usar `FrameSource` falso como no teste de sessão.
6. Medir latência e qualidade antes de escolher entre inferência local e nuvem.

O dono da sessão serializa chamadas de entrada/ciclo de vida em uma coroutine;
a coleta de eventos ocorre concorrentemente. Encerrar a conexão no fim da sessão
e propagar cancelamento no adaptador. Não persistir bytes de áudio/imagem por padrão.

Trabalhe neste módulo sem importar `com.stepwise.vision`, `procedure` ou o
`SessionController` legado: eles pertencem ao hardware.
