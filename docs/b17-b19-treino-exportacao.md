# B17-B19 - Linux, treino minimo e exportacao

Data: 2026-09-16.

## Resultado atual

B17, B18 e B19 foram validados neste notebook em 16/09/2026.

A instalacao oficial do WSL foi executada com:

```powershell
wsl.exe --install --distribution Ubuntu --no-launch --web-download
```

O instalador informou que a Plataforma de Maquina Virtual, o Subsistema do Windows para Linux e a distribuicao Ubuntu foram instalados. Depois do reboot, o usuario criou usuario/senha no Ubuntu e a distribuicao apareceu como `Ubuntu`, `Running`, WSL2.

O Ubuntu padrao veio com Python 3.14.4. Para preservar compatibilidade com Model Maker, foi instalado Python 3.10.21 via `uv`, com ambiente de treino no filesystem nativo do Ubuntu:

```bash
~/.venvs/stepwise-train
```

`pip check` passou e o import real abaixo passou:

```bash
python scripts/validate_model_maker_import.py
```

Resultado principal:

```text
tensorflow 2.15.1
object_detector mediapipe_model_maker.python.vision.object_detector
```

B19 foi validado com um dataset COCO sintetico minimo, apenas para provar o pipeline de treino/exportacao. O modelo gerado nao representa o kit real.

Artefato local gerado:

```text
ml/models/b19_smoke/stepwise_b19_smoke.tflite
SHA256 F6A682702E2CD535C31AC8437CB009C73B97101DB445DE0E72FDC90793F86CE9
Tamanho 11.240.068 bytes
```

`ml/models/` e `ml/data/*` estao ignorados pelo Git; o artefato fica como evidencia local.

## Ajuste feito no repo

`ml/requirements.txt` foi alinhado com o stack que passou: Model Maker 0.2.1.4, TensorFlow 2.15.x, MediaPipe 0.10.9, Protobuf 3.20.3 e TensorFlow Datasets/Metadata antigos. Essa combinacao evita a incompatibilidade entre Model Maker legado, TensorFlow 2.16+ e pacotes recentes de metadata/protobuf.

Lock gerado:

```text
ml/requirements-train.lock.txt
```

Scripts adicionados para repeticao/diagnostico:

```powershell
.\scripts\b17_b19_diagnostico.ps1
```

```bash
python scripts/validate_model_maker_import.py
python scripts/make_minimal_coco_dataset.py
python scripts/validate_minimal_coco_loader.py
python scripts/b19_minimal_model_maker_smoke.py
```

## Proximo passo operacional

Executar B20: copiar ou apontar o `.tflite` smoke para o Android e validar se o app consegue carregar/inferir uma imagem de exemplo. Depois, quando o kit real estiver disponivel, substituir o dataset sintetico pela coleta/anotacao real.

O treino smoke prova o pipeline tecnico, nao qualidade visual. O detector final ainda depende das fotos/anotacoes do kit real.
