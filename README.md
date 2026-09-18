# StepWise

Tutor por imagem e voz usando os óculos Ray-Ban Meta e um celular Android.
Dois modos podem evoluir de forma independente:

- **Hardware:** acompanhar a montagem de peças físicas.
- **Software:** interpretar um desenho técnico e orientar a modelagem CAD,
  inicialmente no Fusion 360, observando a tela pelos óculos.

## Estado deste checkout

A base foi separada em módulos. O hardware preserva o código existente de
motor procedural, detectores e checkpoints, ainda com integrações pendentes.
O software tem contratos de provedor multimodal, sessão e testes com entradas
simuladas. Nenhuma LLM, captura DAT real ou conversa de áudio foi conectada por
esta reorganização. A tela companion permite selecionar o modo e mostra seu status.

## Onde cada equipe trabalha

| Diretório | Responsabilidade |
|---|---|
| [`android/mode-software/`](android/mode-software/README.md) | Tutor CAD, desenho técnico e integração com modelos |
| [`android/mode-hardware/`](android/mode-hardware/README.md) | Motor físico, detectores, procedimentos, banco e testes |
| `android/core/` | Contratos comuns de objetivos, resultados e mídia, sem Android |
| `android/platform-android/` | Óculos, áudio Android e estado da conexão |
| `android/app/` | Composição dos módulos e interface companion |
| `ml/` | Treino/exportação da percepção física |

Os modos não importam um ao outro. Ambos usam os óculos; o software recebe
captura por uma interface, permitindo desenvolvimento sem o dispositivo.

Leia o [guia de desenvolvimento paralelo](docs/desenvolvimento-modular.md)
para os contratos, limites de dependência e integração de branches antigas.

## Desenvolver o modo software sem Android SDK

Requer JDK 17. O Gradle Wrapper baixa a versão correta do Gradle na primeira execução.

```bash
cd android
./gradlew -p jvm-tests :mode-software:test
```

No Windows: `gradlew.bat -p jvm-tests :mode-software:test`.
Os testes usam provedores falsos; não precisam de API key, internet durante a
execução após baixar dependências, dataset ou óculos.

## Desenvolver e compilar o Android

Requer JDK 17, Android SDK 34 e Build Tools 34.0.0.
Abra `android/` no Android Studio e configure o SDK em `local.properties`
(usando `local.properties.example` como referência).

```bash
cd android
./gradlew :mode-hardware:testDebugUnitTest :platform-android:testDebugUnitTest :app:assembleDebug
```

## Conferir separação dos módulos

Da raiz, com Python 3:

```bash
python3 scripts/check_module_boundaries.py
```

As dependências de detecção física e Room ficam em `mode-hardware`; SDKs dos
óculos e áudio ficam em `platform-android`. O módulo software não depende desses
SDKs para compilar ou testar. Não colocar credenciais de provedores no código.

## Documentação anterior

- [Arquitetura do modo hardware](docs/architecture-stepwise.mmd).
- [Política legada do modo hardware](docs/lgpd-privacidade.md).
- [Pipeline de ML físico](ml/README.md).

Esses documentos descrevem o escopo físico original, inclusive partes planejadas.
A política de processamento local do hardware não deve ser apresentada como uma
promessa do futuro modo software conectado à nuvem.
