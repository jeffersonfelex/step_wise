# Execução em blocos — final StepWise

Atualizado em 16/09/2026. Este índice agrupa a execução; os IDs e critérios do [roadmap](PLANO_ACAO_FINAL_STEPWISE.md) continuam sendo a referência para marcar cada tarefa concluída.

## Ordem prática

| Bloco | Tarefas agrupadas | Entrega e validação de saída |
|---|---|---|
| 1. Ferramentas Windows | B15+B16; parte Windows de B21/B23/B26 | Python isolado, bibliotecas importáveis, imagem processada e versões registradas. |
| 2. Viabilidade do treino | B17–B19; parte Linux de B21/B22 | Treino mínimo exporta um `.tflite`; ambiente reproduzível. Limitar diagnóstico do Model Maker a 60 minutos. B20 fecha a prova no Android. |
| 3. Contratos e procedimento | C01–C12 | Interfaces, JSON validado e exemplos corretos/incorretos/inconclusivos. C06–C10 dependem da montagem física A04–A08; contratos genéricos podem avançar antes. |
| 4. Decisão | D01–D07 | Motor puro, sem avanço duplicado, com abstenção; uma suíte de testes cobre o conjunto. |
| 5. Adaptação e recuperação | D08–D20 | Redução de ajuda por etapa, transações e recuperação; testes do motor e Room. D13 será conferido com áudio no bloco 6. |
| 6. Primeiro fluxo utilizável | G1 + F1 + integração de H1 aplicável | Fonte local, botões, instrução falada e persistência em uma sessão completa. Usar evidências de teste identificadas; ainda não comprova visão real. |
| 7. Visão do kit | E1–E5 + B20, se ainda pendente | Coleta e anotação antes do treino; depois exportação, inferência Android, qualidade e contingência. Validar cada subfase antes de consumir seus resultados. |
| 8. Voz e óculos simulados | F2/F3 + G2/G3 | Comandos, adaptador DAT e mídia MDK. Testar rotas disponíveis; áudio/câmera dos óculos reais continuam pendentes do hardware. |
| 9. Aceitação e viagem | H + I; fechar B21–B26 aplicáveis | Matriz de testes, evidências, pacote offline, backup e roteiro do evento. |

Os blocos 7 e 8 podem trocar de ordem conforme disponibilidade do kit. A equipe pode coletar fotos enquanto o software avança. Se Linux exigir reinicialização ou o treino ficar bloqueado, avançar contratos e motor, mantendo a pendência explícita.

## Como economizar tempo e tokens

- Ler apenas arquivos relacionados ao bloco e manter o status no topo do roadmap.
- Agrupar consultas independentes e alterações relacionadas; compilar ao fechar uma mudança coerente.
- Executar os testes relevantes uma vez por conjunto de alterações. Repetir quando houver correção ou risco novo.
- Atualizar documentação e apresentar um resumo ao concluir cada bloco, sem pedir autorização para cada ID já previsto.
- Manter os passos pequenos dentro do bloco. Agrupamento não dispensa critérios nem significa implementar uma fase inteira sem verificações intermediárias necessárias.
- Conferências no celular, decisões sobre o kit e confirmação de instalação continuam exigindo participação quando o dispositivo solicitar.

**Pendências humanas prioritárias:** confirmar kit e seis estados físicos (A02/A04–A08), responsáveis e orientação da organização sobre o celular (A03/A09). Sem o kit validado, não congelar regras visuais nem treinar o modelo definitivo.
