# Primeiro fluxo do tutor CAD

## Preparar e instalar

Com JDK 17, Android SDK 34 e celular autorizado no ADB:

```bash
cd android
./gradlew :mode-software:test :app:assembleDebug
./gradlew :app:installDebug
```

Antes de instalar, confira `adb devices -l`: o celular deve aparecer com estado
`device`. Uma lista vazia no WSL significa que esse ambiente não está enxergando
o aparelho; conectar o cabo ao Windows pode não disponibilizá-lo ao ADB do Linux.
Também é possível copiar `android/app/build/outputs/apk/debug/app-debug.apk`
para o celular e instalar manualmente.

Abra StepWise → **Software — modelagem CAD**. Use inicialmente um desenho simples
com cotas legíveis e uma foto do estado do Fusion. Nesta versão são aceitas imagens,
não PDFs. O app limita a entrada a 12 MB, ajusta orientação EXIF e reduz a dimensão
máxima a 2048 pixels antes de codificar JPEG. Cotas muito pequenas podem perder
legibilidade: recorte a área relevante antes de selecionar, se necessário.

## Gerar token temporário no computador

Uma conta com acesso à API Gemini e ao modelo escolhido é necessária. A chave
permanente permanece no computador; o app recebe somente um token temporário.
Na raiz do repositório:

```bash
python3 scripts/create_gemini_live_token.py --output /tmp/stepwise-gemini.token
```

O script pede a chave sem exibi-la (ou lê `GEMINI_API_KEY` do ambiente), solicita
um token de uso único e grava um arquivo privado. No Windows use um caminho local
adequado e `python`. Abra o arquivo localmente e copie seu conteúdo no campo
**Token temporário Gemini** do app. Não envie a chave ou token no chat ou no Git.

Comece a sessão dentro de 10 minutos; a validade solicitada é de 30 minutos.
Cada nova conexão exige novo token. O arquivo não é sobrescrito: use outro nome
ou exclua pessoalmente o arquivo anterior. Para distribuição do produto, substituir
esse procedimento manual por um backend autenticado que emita tokens temporários.

A conexão aguarda até 20 segundos pela confirmação do serviço. Se esse prazo
expirar, a tela apresenta uma mensagem para tentar novamente. Acesso recusado e
limite de uso também têm mensagens próprias; encerrar a tela cancela a tentativa.
O status fica fixo no topo, mesmo ao rolar a conversa. Se a conexão abrir, mas
nenhuma resposta chegar em 20 segundos, o app avisa sem encerrar a sessão.
Ao relatar um erro, informe a mensagem e o código exibidos, sem compartilhar tokens.

## Roteiro no POCO

1. **Selecionar desenho** e preencher o objetivo. O padrão é Fusion 360.
2. Informar token e modelo; **Conectar e enviar desenho** inicia o envio à nuvem.
3. Ouvir/ler o tutor. Se faltar uma medida, responder antes de aceitar o plano.
4. Revisar a lista e tocar em **Aceitar plano**. O app mantém a etapa atual.
5. Modelar no computador. Perguntar por texto ou tocar em **Falar pelo celular**,
   falar e tocar em **Terminar fala**. A captura usa PCM 16 kHz; a resposta usa áudio
   do provedor. **Interromper resposta** limpa a reprodução pendente.
6. Usar **Verificar com foto local (teste)** para fornecer o estado do CAD.
   O resultado só vale para a etapa e o identificador daquela tentativa.
7. Após `CORRECT`, **Confirmar e avançar** libera a próxima instrução. Erro ou
   inconclusivo não liberam avanço. A conclusão também exige confirmação local.
8. **Encerrar sessão** libera conexão, microfone e reprodução.

**Verificar pelos óculos** usa o adaptador compartilhado. Hoje ele informa que a
captura ainda não está implementada; não produz evidência fictícia. Não é necessária
alteração no tutor para trocar a foto de teste pela captura real quando DAT funcionar.
A entrada de áudio atual usa o roteamento escolhido pelo Android, sem garantia de
microfone/alto-falante dos óculos. Essa rota precisa ser validada no dispositivo.

## Dados e ciclo de vida

- O desenho é enviado ao conectar; fotos adicionais, ao solicitar verificação.
- Texto e áudio das perguntas são enviados ao Gemini; respostas voltam por WebSocket.
- O app não grava mídia, token ou transcrição em disco. O token é apagado do campo
  ao conectar. O arquivo criado pelo script no computador deve ser removido após uso.
- O processamento remoto segue os termos/configurações da conta do provedor; não
  se aplica a promessa de processamento exclusivamente local do hardware.
- A sessão é encerrada ao sair da tela ou colocar o app em segundo plano, exceto
  durante o seletor de imagem. Rotação/recriação da Activity não retoma a conversa.
- Não há renovação de token, reconexão automática ou persistência de progresso ainda.
- A sessão remota pode ser encerrada pelo serviço antes da expiração do token.

## O que foi validado e o que falta

Os testes automatizados exercitam o protocolo com MockWebServer e o controle local
do plano. Não enviam mídia ao Google. Build do APK não substitui ensaio no celular.
É necessário verificar a sessão real, as permissões, o áudio, os óculos e a qualidade
de interpretação de desenhos. Medir o tempo até a primeira orientação útil e a
avaliação correlacionada, inclusive após uma sessão prolongada. Não há benchmark
de latência real nem garantia de acerto nesta implementação.

Fontes do protocolo usado:

- [WebSockets Gemini Live](https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket)
- [Tokens temporários](https://ai.google.dev/gemini-api/docs/live-api/ephemeral-tokens)
- [Ferramentas na Live API](https://ai.google.dev/gemini-api/docs/live-api/tools)

## Divisão de trabalho

- Domínio/provedor/testes: `android/mode-software/`.
- Tela e captura/reprodução Android: `android/software-android/`.
- Integração física dos óculos: `android/platform-android/`, coordenada entre equipes.
- O módulo `mode-hardware` permanece independente.
