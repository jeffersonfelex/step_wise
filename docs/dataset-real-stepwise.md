# StepWise — dataset real da montagem torneira/conexões

**Data:** 17/09/2026
**Status:** dataset capturado, renomeado e dividido para o primeiro ciclo de treino.
**Caminho local:** `dataset stepwise/`
**Artefatos derivados:** `ml/data/stepwise_real/`

## Decisão de escopo visual

O kit físico usado para a final foi ajustado para uma montagem com torneira e conexões rosqueáveis, por disponibilidade imediata. O procedimento real agora usa seis componentes:

1. `P1_torneira`
2. `P2`
3. `P3`
4. `P4`
5. `P5`
6. `P6`

Para acelerar a validação antes do evento, o primeiro modelo será tratado como **classificação de imagem inteira**, não como detecção com caixas. A saída esperada é uma classe procedural ou visual, por exemplo `S3_torneira_P2_P3`, `E1_ordem_errada` ou `P4`.

Essa decisão reduz o tempo de anotação e permite validar rapidamente:

```text
imagem real -> modelo -> classe prevista + confiança -> decisão procedural
```

## Contagem por classe

| Classe | Imagens |
|---|---:|
| `P1_torneira` | 31 |
| `P2` | 51 |
| `P3` | 35 |
| `P4` | 35 |
| `P5` | 29 |
| `P6` | 47 |
| `pecas_separadas_juntas` | 53 |
| `S1_torneira` | 23 |
| `S2_torneira_P2` | 27 |
| `S3_torneira_P2_P3` | 35 |
| `S4_torneira_P2_P3_P4` | 47 |
| `S5_torneira_P2_P3_P4_P5` | 55 |
| `S6_final` | 10 |
| `E1_ordem_errada` | 30 |
| `E2_peca_invertida` | 22 |
| `E3_peca_ausente` | 29 |
| `E4_peca_solta` | 22 |

**Total:** 581 imagens.

## Divisão atual

A divisão foi gerada por `scripts/prepare_stepwise_dataset.py`, com `seed = 42`, estratificada por classe e sem copiar imagens.

| Split | Imagens |
|---|---:|
| `train` | 411 |
| `val` | 85 |
| `test` | 85 |

Arquivos gerados:

```text
ml/data/stepwise_real/manifest.json
ml/data/stepwise_real/splits/all.csv
ml/data/stepwise_real/splits/train.csv
ml/data/stepwise_real/splits/val.csv
ml/data/stepwise_real/splits/test.csv
```

Hash do índice:

```text
43309df0b7dd086c904266a54d6129682616e6b86d28d7c083e4c20a4530c1d1
```

## Limites conhecidos

- A captura foi feita com celular, não com os óculos; no evento ainda será necessário validar perspectiva, foco, atraso e iluminação dos Ray-Ban Meta.
- O conjunto `S6_final` tem apenas 10 imagens; é suficiente para um primeiro teste, mas fraco para métrica confiável.
- A classificação de imagem inteira é uma rota pragmática para a final. Ela não prova encaixe mecânico, torque ou conexão perfeita quando a evidência visual não mostra isso.
- As classes de peça isolada ajudam o modelo a reconhecer componentes, mas a decisão do produto deve usar principalmente estados de montagem e erros.

## Próximo passo

Treinar um primeiro classificador pequeno, exportar `.tflite` e validar no POCO com imagens reais do dataset. Se o modelo errar muito entre estados próximos, reduzir o escopo de decisão para classes proceduralmente essenciais:

- `S1_torneira`
- `S2_torneira_P2`
- `S3_torneira_P2_P3`
- `S4_torneira_P2_P3_P4`
- `S5_torneira_P2_P3_P4_P5`
- `S6_final`
- `E1_ordem_errada`
- `E2_peca_invertida`
- `E3_peca_ausente`
- `E4_peca_solta`

## Primeiro modelo real

Em 17/09/2026 foi treinado um primeiro classificador técnico com MobileNetV2 pequeno (`alpha=0.35`), TensorFlow 2.15.1 no WSL e as divisões descritas acima.

Artefatos principais:

```text
ml/models/stepwise_real_classifier_v1/stepwise_real_classifier_v1.tflite
android/app/src/main/assets/models/stepwise_real_classifier_v1.tflite
android/app/src/main/assets/models/stepwise_real_classifier_v1_labels.json
android/app/src/main/assets/models/stepwise_real_classifier_v1_manifest.json
```

SHA-256 do `.tflite`:

```text
222DF1B55815B7E6ADD809E7AE0709C6522DB65F2F08BFAFF121397CDE7C8720
```

Validações feitas:

- TFLite abriu no ambiente Linux de treino.
- Entrada validada: `[1, 224, 224, 3]`, `float32`.
- Saída validada: `[1, 17]`, `float32`, softmax somando 1.0.
- Teste instrumentado no POCO executou 3 testes com sucesso, incluindo 5 imagens reais do dataset.

Resultado de qualidade inicial:

```text
test accuracy: 0.1647
```

Esse valor ainda é baixo. O modelo atual deve ser usado como prova de pipeline real: dataset -> treino -> exportação -> Android. O próximo ciclo precisa melhorar qualidade visual antes de usar a predição como decisão principal do motor.


### E17 técnico concluído

Em 17/09/2026, o teste `TfliteSmokeInferenceTest` executou no POCO:

```text
Starting 3 tests on 2602BPC18G - 16
Finished 3 tests on 2602BPC18G - 16
BUILD SUCCESSFUL
```

O teste incluiu cinco imagens reais empacotadas em `android/app/src/androidTest/assets/real_samples/`. O critério aqui foi validar pipeline, pré-processamento, labels e formato das confianças no dispositivo. A qualidade do modelo continua baixa e deve ser melhorada antes de usar a predição como decisão principal.

## Avaliação E20/E21

Em 17/09/2026 foi criado `ml/evaluate.py` e executado nos modelos disponíveis.

Resultado do modelo de 17 classes em `test`:

```text
accuracy: 0.0941
top3_accuracy: 0.2353
threshold 0.35: 100% de abstenção
comportamento observado: prevê P2 para todas as imagens
```

Resultado do modelo procedural de 10 classes em `test`:

```text
accuracy: 0.0930
top3_accuracy: 0.2326
threshold 0.35: 100% de abstenção
comportamento observado: prevê E3_peca_ausente para todas as imagens
```

Decisão: os modelos atuais provam o pipeline, mas não devem avançar etapa automaticamente. Para a final, o motor deve começar conservador: se a confiança/modelo não for confiável, retornar `INCONCLUSIVE` ou usar um modo de demonstração identificado. O próximo ciclo de percepção deve usar transfer learning com pesos pré-treinados, coleta adicional ou simplificação visual da bancada.
