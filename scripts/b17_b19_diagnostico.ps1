param(
    [switch]$SkipWsl
)

$ErrorActionPreference = "Continue"
$repo = Split-Path -Parent $PSScriptRoot
$python310 = Join-Path $repo ".tools\python310\python.exe"
$venvTools = Join-Path $repo ".venv-tools\Scripts\python.exe"

function StepWise-Run {
    param(
        [string]$Title,
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "== $Title =="
    & $Command
    Write-Host "exit=$LASTEXITCODE"
}

Set-Location $repo

StepWise-Run "B17 Windows WSL status" {
    wsl.exe --status
}

StepWise-Run "B17 WSL distributions" {
    wsl.exe --list --verbose
}

if (-not $SkipWsl) {
    StepWise-Run "B17 Linux Python probe" {
        wsl.exe --exec bash -lc "python3 --version; python3.10 --version; command -v python3.10; python3 -m venv --help >/dev/null && echo venv-ok"
    }
}

StepWise-Run "B16 local Python 3.10" {
    & $python310 --version
}

StepWise-Run "B16 tools venv packages" {
    & $venvTools -m pip list --format=freeze
}

StepWise-Run "B18 requirements pins" {
    Get-Content -Encoding UTF8 .\ml\requirements.txt
}

Write-Host ""
Write-Host "Resultado esperado:"
Write-Host "- Se WSL nao listar Ubuntu e Linux Python falhar, B17 esta bloqueado por instalacao/configuracao do WSL."
Write-Host "- Se WSL funcionar, criar .venv-train no Linux e instalar ml/requirements.txt."
Write-Host "- B19 so passa quando um .tflite real for produzido pelo fluxo de treino/exportacao."
