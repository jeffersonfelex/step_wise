# B08 — Meta AI, Developer Mode e registro real

## Estado em 16/09/2026

| Item | Evidência / estado |
|---|---|
| Meta AI instalado | Pacote `com.facebook.stella`, versão `289.0.0.25.162`, código `968902270`; confirmado via ADB. |
| Login no Meta AI | Concluído, informado pelo usuário em 16/09/2026; sem coleta de dados da conta. |
| Óculos disponíveis/pareados | Usuário confirmou ausência de óculos disponíveis em 16/09/2026; pareamento real não validado. |
| Developer Mode | Ativado no Meta AI, conforme confirmação do usuário em 16/09/2026. Controle encontrado em Informações do app após cinco toques na versão. Ainda sem validação com óculos vinculados. |
| CameraAccess | Instalado; MDK validado e desabilitado ao terminar. Registro real ainda não executado. |
| StepWise | Ainda sem inicialização DAT, callback de registro ou botão de conexão. Permissões Android concedidas não registram o app no Meta AI. |

## Preparação no POCO

1. Abrir Meta AI e confirmar login na conta que será usada no evento. Registrar apenas que o login está concluído; não guardar senha, código de acesso ou dados da conta no repositório.
2. Caminho confirmado pelo usuário nesta versão: Configurações → Informações do app → tocar cinco vezes em Versão do app → Developer Mode. Usuário confirmou que o modo está ativado, mesmo sem óculos disponíveis.
3. Se o controle depender de óculos vinculados, registrar essa dependência e concluir a ativação com o hardware disponibilizado no evento. Não declarar ativação só porque as opções de desenvolvedor do POCO estão habilitadas.
4. Quando o controle estiver disponível, ativar Developer Mode no Meta AI e conferir o estado efetivo para o dispositivo utilizado. Atualizações do aplicativo/firmware podem exigir nova conferência.
5. Manter CameraAccess e seu APK de diagnóstico disponíveis; caminhos e hash em [cameraaccess-validacao.md](cameraaccess-validacao.md).

## Roteiro de registro real com CameraAccess

Executar com os óculos autorizados pela organização disponíveis. Se já estiverem associados a outra conta, alinhar o procedimento com a coordenação antes de substituir vínculo ou redefinir o dispositivo.

1. Confirmar modelo/firmware dos óculos, carga, conectividade do POCO e login Meta AI.
2. Parear os óculos no Meta AI pelo fluxo oferecido pelo app e confirmar o dispositivo conectado.
3. Conferir Developer Mode para os óculos vinculados. Se houver atualização obrigatória, registrar a mensagem e reservar tempo para o fluxo oficial.
4. Abrir CameraAccess e garantir que **MockDeviceKit esteja desabilitado**. Conceder a permissão Android de dispositivos próximos se solicitada.
5. Tocar **Connect my glasses**. Conferir o redirecionamento ao Meta AI e o aplicativo que está pedindo conexão; aceitar o registro de CameraAccess.
6. Conferir o retorno ao CameraAccess pelo callback `cameraaccess` e o estado registrado. Isso registra apenas CameraAccess; StepWise precisará de registro próprio na fase G.
7. Com os óculos ligados, abertos e em uso, iniciar sessão. Confirmar estado **started**, tratando mensagens de incompatibilidade ou atualização.
8. Iniciar Preview e concluir o pedido de câmera dos óculos quando apresentado. Permissão CAMERA do Android para o celular não substitui a autorização DAT.
9. Confirmar frames reais no Preview. Registrar separadamente resultado de registro, sessão e stream; ausência de erro não comprova vídeo chegando.
10. Parar Preview e encerrar sessão. Captura e gravação só entram quando forem parte do teste definido.

## Configuração do aplicativo de referência

- Código oficial fixado em `974e05c569da8be01c0c9ce8bc5bc79b73ff8540`.
- Pacote CameraAccess: `com.meta.wearable.dat.externalsampleapps.cameraaccess`.
- Callback do manifesto: `cameraaccess`.
- O exemplo compilado mantém os placeholders de APPLICATION_ID/CLIENT_TOKEN vazios, como no código oficial desse commit, e orienta uso de Developer Mode. O guia técnico no mesmo repositório descreve valores `0` para desenvolvimento. Essa diferença deve ser investigada se houver falha real de registro; não inventar credenciais nem considerar o teste MDK como validação desses valores.
- Produção/canais de distribuição usam o identificador e client token emitidos para o aplicativo no Wearables Developer Center. Não são necessários tokens de repositório para baixar o SDK pelo Maven Central.
- Nenhuma conta de desenvolvedor, projeto ou canal externo foi criado nesta etapa.

## Evidência para conclusão

- [x] Login confirmado pelo usuário em 16/09/2026.
- [x] Developer Mode ativado no Meta AI, conforme confirmação do usuário; conferir novamente com os óculos no evento.
- [x] Roteiro de registro salvo localmente para consulta no evento.
- [x] APK de diagnóstico e referência do callback identificados.
- [x] Indisponibilidade atual dos óculos confirmada; responsabilidade pelo pareamento no evento a alinhar com a coordenação.

B08 concluída no escopo de preparação: versão identificada via ADB, login e Developer Mode confirmados pelo usuário e roteiro offline salvo. Registro real, pareamento, câmera e áudio físicos continuam pendentes de hardware. A ativação no aplicativo não comprova configuração/compatibilidade de óculos ainda não vinculados.

## Fontes e limites

- [README oficial CameraAccess no commit utilizado](https://github.com/facebook/meta-wearables-dat-android/blob/974e05c569da8be01c0c9ce8bc5bc79b73ff8540/samples/CameraAccess/README.md).
- [Guia técnico oficial no mesmo commit](https://github.com/facebook/meta-wearables-dat-android/blob/974e05c569da8be01c0c9ce8bc5bc79b73ff8540/AGENTS.md), seções de registro, permissões e Developer Mode; cópia local no checkout de referência.
- [Configuração Meta](https://wearables.developer.meta.com/docs/getting-started-toolkit/): retornou exigência de login na consulta desta sessão. Não foi possível verificar por essa página o caminho exato dos menus da versão instalada.

Este roteiro é uma síntese operacional local, disponível offline. Não substitui a conferência do firmware/app e das instruções da coordenação no evento.
