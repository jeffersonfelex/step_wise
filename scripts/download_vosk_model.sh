#!/usr/bin/env bash
# Baixa o modelo Vosk (ASR offline, gramática restrita) para
# android/platform-android/src/main/assets/model.
#
# Uso:
#   ./scripts/download_vosk_model.sh
#
# Equivalente à tarefa Gradle `downloadVoskModel`
# (android/platform-android/build.gradle.kts), para quem prefere rodar
# direto do terminal / VS Code sem abrir o Android Studio. Idempotente: se
# o modelo já estiver baixado, não baixa de novo.
#
# Por que baixar antes e não em runtime: o app não deve depender de rede
# para reconhecer voz (privacidade - ver docs/lgpd-privacidade.md) nem de
# um download grande no dia do hackathon.

set -euo pipefail

MODEL_NAME="vosk-model-small-pt-0.3"
MODEL_URL="https://alphacephei.com/vosk/models/${MODEL_NAME}.zip"
# Mesmo valor fixado em android/platform-android/build.gradle.kts.
MODEL_SHA256="6e1ce909032e1afa7a88e68a3d628ecafff302bdf195befab308826c395e93b7"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ASSETS_DIR="${REPO_ROOT}/android/platform-android/src/main/assets/model"
VERSION_MARKER="${ASSETS_DIR}/.vosk-model-version"

if [[ -f "${VERSION_MARKER}" ]] && [[ "$(cat "${VERSION_MARKER}")" == "${MODEL_NAME}" ]]; then
  echo "StepWise: modelo Vosk já presente em ${ASSETS_DIR}, nada a fazer."
  exit 0
fi

command -v curl >/dev/null 2>&1 || { echo "Erro: curl não encontrado." >&2; exit 1; }
command -v unzip >/dev/null 2>&1 || { echo "Erro: unzip não encontrado." >&2; exit 1; }

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

ZIP_PATH="${TMP_DIR}/${MODEL_NAME}.zip"

echo "StepWise: baixando ${MODEL_URL} ..."
curl -fL --progress-bar -o "${ZIP_PATH}" "${MODEL_URL}"

if command -v sha256sum >/dev/null 2>&1; then
  ACTUAL_SHA256="$(sha256sum "${ZIP_PATH}" | cut -d' ' -f1)"
else
  ACTUAL_SHA256="$(shasum -a 256 "${ZIP_PATH}" | cut -d' ' -f1)"
fi
if [[ "${ACTUAL_SHA256}" != "${MODEL_SHA256}" ]]; then
  echo "Erro: SHA-256 do modelo não confere (esperado ${MODEL_SHA256}, obtido ${ACTUAL_SHA256})." >&2
  exit 1
fi

echo "StepWise: descompactando..."
unzip -q "${ZIP_PATH}" -d "${TMP_DIR}/extracted"

rm -rf "${ASSETS_DIR}"
mkdir -p "$(dirname "${ASSETS_DIR}")"
mv "${TMP_DIR}/extracted/${MODEL_NAME}" "${ASSETS_DIR}"

echo "${MODEL_NAME}" > "${VERSION_MARKER}"
echo "StepWise: modelo Vosk pronto em ${ASSETS_DIR}."
