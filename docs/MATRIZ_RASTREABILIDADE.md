# Matriz de rastreabilidade — módulo de Vendas (CDU02)

> **Gerada automaticamente** (`npm run docs:matriz`) a partir dos títulos dos testes em `cypress/e2e/vendas/*.cy.js`; cada título termina com os requisitos que ele cobre. Se mudar um teste, rode o comando de novo.

Base: `DRS_LES_2_2026` v0.6 · **81 testes automatizados Cypress** + testes de integração JUnit da API (`backend/src/test`).

## 1. Requisito → testes

| Requisito | Descrição (DRS) | Casos de teste Cypress | Situação |
|---|---|---|---|
| RF0031 | Gerenciar carrinho de compra | CTV001, CTV002, CTV003, CTV006, CTV011, CTV100 | Coberto |
| RF0032 | Definir quantidade de itens no carrinho | CTV002, CTV004, CTV005, CTV009, CTV100 | Coberto |
| RF0033 | Realizar compra | CTV040, CTV100 | Coberto |
| RF0034 | Calcular frete | CTV041, CTV100 | Coberto (frete **fixo** R$ 24,90 — decisão do grupo, ver `docs/JUSTIFICATIVA_FRETE.md`) |
| RF0035 | Selecionar endereço de entrega | CTV042, CTV044, CTV100 | Coberto |
| RF0036 | Selecionar forma de pagamento | CTV040, CTV043, CTV046, CTV057, CTV049, CTV100 | Coberto |
| RF0037 | Uso de cupom para pagamento | CTV043, CTV051, CTV053, CTV085, CTV100 | Coberto |
| RF0038 | Finalizar compra (EM PROCESSAMENTO) | CTV040, CTV056, CTV060, CTV068, CTV100 | Coberto |
| RF0039 | Despachar produtos para entrega | CTV065, CTV068, CTV100 | Coberto |
| RF0040 | Produtos entregues | CTV066, CTV067, CTV068, CTV100 | Coberto |
| RF0041 | Solicitar troca | CTV071, CTV072, CTV073, CTV074, CTV100 | Coberto |
| RF0042 | Autorizar trocas | CTV077, CTV079, CTV080, CTV086, CTV100 | Coberto |
| RF0043 | Visualização de trocas | CTV076, CTV100 | Coberto |
| RF0044 | Confirmar recebimento de itens para troca | CTV081, CTV082, CTV086, CTV100 | Coberto |
| RF0045 | Gerar cupom de troca após recebimento | CTV083, CTV084, CTV085, CTV100 | Coberto |
| RF0053 | Dar baixa em estoque | CTV062, CTV069, CTV100 | Coberto |
| RF0054 | Realizar reentrada em estoque | CTV081, CTV082, CTV100 | Coberto |
| RN0031 | Validar estoque para adição de itens no carrinho | CTV007, CTV008, CTV009, CTV010, CTV029, CTV100 | Coberto |
| RN0032 | Validar estoque para compra | CTV054, CTV055 | Coberto |
| RN0033 | Uso de cupom promocional (um por compra) | CTV050, CTV051, CTV100, CTV110 | Coberto |
| RN0034 | Uso de diversos cartões (mínimo R$ 10,00 cada) | CTV049, CTV111 | Coberto |
| RN0035 | Cupons junto a cartão (dispensa o mínimo) | CTV112 | Coberto |
| RN0036 | Gerar cupom de troca (troco) | CTV052, CTV114, CTV115 | Coberto |
| RN0037 | Validar forma de pagamento | CTV061, CTV100, CTV116, CTV117 | Coberto |
| RN0038 | Status APROVADA/REPROVADA conforme pagamento | CTV057, CTV061, CTV062, CTV063, CTV064, CTV100 | Coberto |
| RN0039 | Status EM TRANSPORTE | CTV065, CTV100, CTV118 | Coberto |
| RN0040 | Status ENTREGUE | CTV066, CTV100, CTV119 | Coberto |
| RN0041 | Gerar pedido de troca (EM TROCA) | CTV071, CTV075, CTV084, CTV100 | Coberto |
| RN0042 | Status TROCADO após recebimento | CTV081, CTV100, CTV121 | Coberto |
| RN0043 | Só itens ENTREGUES podem ser trocados | CTV070, CTV100, CTV120, CTV121 | Coberto |
| RN0044 | Bloqueio de produtos no carrinho | CTV020, CTV021, CTV022, CTV023, CTV024, CTV027, CTV029, CTV030, CTV100 | Coberto |
| RN0045 | Retirar item do carrinho ao desbloquear | CTV025 | Coberto |
| RN0046 | Notificação de autorização de troca | CTV078, CTV100 | Coberto |
| RN0028 | Validar retorno da operadora (estoque preservado se reprovada) | CTV063 | Coberto |
| RNF0042 | Apresentar itens retirados do carrinho | CTV025, CTV026, CTV027, CTV028, CTV030 | Coberto |
| RNF0012 | Log de transação (escrita) | — | Implementado (aspecto `LogDeEscritaAspect`); verificação manual no log do backend |
| RNF0011 | Tempo de resposta (consultas) | — | Não avaliado automaticamente (sem teste de desempenho) |

## 2. Caso de teste → requisitos

| ID | Título | Requisitos | Arquivo |
|---|---|---|---|
| CTV001 | deve listar os produtos disponíveis com preço e estoque | RF0031 | `01-carrinho.cy.js` |
| CTV002 | deve adicionar um produto ao carrinho escolhendo a quantidade | RF0031, RF0032 | `01-carrinho.cy.js` |
| CTV003 | adicionar o mesmo produto de novo soma na mesma linha do carrinho | RF0031 | `01-carrinho.cy.js` |
| CTV004 | deve alterar a quantidade de um item já no carrinho | RF0032 | `01-carrinho.cy.js` |
| CTV005 | diminuir a quantidade até zero remove o item | RF0032 | `01-carrinho.cy.js` |
| CTV006 | deve excluir um item do carrinho | RF0031 | `01-carrinho.cy.js` |
| CTV007 | não deve permitir adicionar mais itens do que o estoque | RN0031 | `01-carrinho.cy.js` |
| CTV008 | o estoque considera o que o cliente já tem no carrinho | RN0031 | `01-carrinho.cy.js` |
| CTV009 | aumentar a quantidade no carrinho acima do estoque é recusado | RF0032, RN0031 | `01-carrinho.cy.js` |
| CTV010 | produto sem estoque aparece como indisponível e não pode ser adicionado | RN0031 | `01-carrinho.cy.js` |
| CTV011 | o carrinho continua o mesmo ao recarregar a página | RF0031 | `01-carrinho.cy.js` |
| CTV020 | itens no carrinho ficam bloqueados por um prazo, exibido em um contador | RN0044 | `02-bloqueio-carrinho.cy.js` |
| CTV021 | o item bloqueado no carrinho de um cliente fica indisponível para os outros | RN0044 | `02-bloqueio-carrinho.cy.js` |
| CTV022 | quando o bloqueio expira, o estoque volta a ficar disponível para os outros clientes | RN0044 | `02-bloqueio-carrinho.cy.js` |
| CTV023 | o cliente é avisado quando faltam 5 minutos ou menos para o bloqueio expirar | RN0044 | `02-bloqueio-carrinho.cy.js` |
| CTV024 | adicionar um novo item renova o prazo de todos (prazo relativo ao último item) | RN0044 | `02-bloqueio-carrinho.cy.js` |
| CTV025 | itens com bloqueio vencido saem do carrinho e continuam listados como expirados; comprar fica desabilitado | RN0045, RNF0042 | `02-bloqueio-carrinho.cy.js` |
| CTV026 | o item expirado deve ser adicionado novamente ao carrinho, o que reabilita a compra | RNF0042 | `02-bloqueio-carrinho.cy.js` |
| CTV027 | com a tela aberta, o carrinho percebe sozinho que o prazo acabou | RN0044, RNF0042 | `02-bloqueio-carrinho.cy.js` |
| CTV028 | o aviso de item expirado pode ser dispensado sem readicionar | RNF0042 | `02-bloqueio-carrinho.cy.js` |
| CTV029 | readicionar um item expirado respeita o estoque se outro cliente o pegou nesse meio tempo | RN0031, RN0044 | `02-bloqueio-carrinho.cy.js` |
| CTV030 | ao finalizar com o bloqueio vencido, a compra é recusada e o carrinho mostra o item expirado | RN0044, RNF0042 | `02-bloqueio-carrinho.cy.js` |
| CTV040 | deve finalizar a compra pagando com cartão e gerar o pedido EM PROCESSAMENTO | RF0033, RF0036, RF0038 | `03-checkout-pagamento.cy.js` |
| CTV041 | o frete é calculado e somado ao total da compra | RF0034 | `03-checkout-pagamento.cy.js` |
| CTV042 | a compra exige um endereço de entrega selecionado | RF0035 | `03-checkout-pagamento.cy.js` |
| CTV043 | a compra não é finalizada se a forma de pagamento não cobre o valor total | RF0036, RF0037 | `03-checkout-pagamento.cy.js` |
| CTV044 | deve cadastrar um novo endereço durante a compra e incorporá-lo ao perfil do cliente | RF0035, RN0023 | `03-checkout-pagamento.cy.js` |
| CTV045 | não deve salvar um novo endereço com campos obrigatórios vazios | RN0023 | `03-checkout-pagamento.cy.js` |
| CTV046 | deve cadastrar um novo cartão durante a compra e incorporá-lo ao perfil do cliente | RF0036, RN0024 | `03-checkout-pagamento.cy.js` |
| CTV047 | as bandeiras American Express e Hipercard são aceitas no cadastro de cartão | RN0025 | `03-checkout-pagamento.cy.js` |
| CTV048 | não deve salvar cartão com número inválido | RN0024 | `03-checkout-pagamento.cy.js` |
| CTV057 | compra paga com um cartão cadastrado na hora: a operadora recusa o final 0000 e o pedido é REPROVADO | RF0036, RN0038 | `03-checkout-pagamento.cy.js` |
| CTV049 | a compra pode ser paga com mais de um cartão, dividindo o valor entre eles | RF0036, RN0034 | `03-checkout-pagamento.cy.js` |
| CTV050 | só é possível escolher um cupom promocional por compra; escolher outro troca o anterior | RN0033 | `03-checkout-pagamento.cy.js` |
| CTV051 | deve pagar com um cupom promocional e completar com cartão; o cupom some da lista | RF0037, RN0033 | `03-checkout-pagamento.cy.js` |
| CTV052 | cupom de troca maior que a compra paga tudo sozinho e gera um cupom de troco | RN0036 | `03-checkout-pagamento.cy.js` |
| CTV053 | um mesmo cupom não pode ser usado em duas compras | RF0037 | `03-checkout-pagamento.cy.js` |
| CTV054 | se o estoque mudar antes da finalização, a quantidade é ajustada automaticamente com um alerta | RN0032 | `03-checkout-pagamento.cy.js` |
| CTV055 | se o item ficar indisponível antes da finalização, ele é removido do carrinho com um alerta | RN0032 | `03-checkout-pagamento.cy.js` |
| CTV056 | depois de finalizar a compra o carrinho fica vazio | RF0038 | `03-checkout-pagamento.cy.js` |
| CTV060 | uma compra recém-finalizada aparece para o administrador EM PROCESSAMENTO, aguardando validação | RF0038 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV061 | validar o pagamento com sucesso aprova o pedido | RN0037, RN0038 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV062 | a baixa em estoque só acontece quando o pagamento é aprovado | RF0053, RN0038 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV063 | pagamento recusado pela operadora reprova o pedido e preserva o estoque | RN0028, RN0038 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV064 | o cliente vê o motivo quando o pagamento do pedido é reprovado | RN0038 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV065 | o administrador despacha um pedido aprovado e ele passa a EM TRANSPORTE | RF0039, RN0039 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV066 | o administrador confirma a entrega e o pedido passa a ENTREGUE | RF0040, RN0040 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV067 | o painel filtra os pedidos pelo status | RF0040 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV068 | o cliente acompanha em "Meus pedidos" o status do pedido a cada ação do administrador | RF0038, RF0039, RF0040 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV069 | pedido aprovado já recebe a baixa em estoque somente uma vez, mesmo se a validação for pedida de novo | RF0053 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV070 | só pedidos ENTREGUES permitem solicitar troca | RN0043 | `05-troca.cy.js` |
| CTV071 | o cliente solicita a troca de um item e o pedido passa a EM TROCA | RF0041, RN0041 | `05-troca.cy.js` |
| CTV072 | a solicitação exige ao menos um item selecionado | RF0041 | `05-troca.cy.js` |
| CTV073 | a solicitação exige o motivo da troca | RF0041 | `05-troca.cy.js` |
| CTV074 | não é possível trocar uma quantidade maior do que a comprada | RF0041 | `05-troca.cy.js` |
| CTV075 | o cliente pode trocar só parte dos itens e quantidades de um pedido | RN0041 | `05-troca.cy.js` |
| CTV076 | o administrador visualiza as solicitações de troca com cliente, itens e motivo | RF0043 | `05-troca.cy.js` |
| CTV077 | o administrador autoriza a troca e o pedido passa a TROCA AUTORIZADA | RF0042 | `05-troca.cy.js` |
| CTV078 | ao autorizar a troca o cliente recebe uma notificação | RN0046 | `05-troca.cy.js` |
| CTV079 | recusar a troca exige o motivo | RF0042 | `05-troca.cy.js` |
| CTV080 | a troca recusada devolve o pedido para ENTREGUE, notifica o cliente e permite nova solicitação | RF0042 | `05-troca.cy.js` |
| CTV081 | confirmar o recebimento com retorno ao estoque: pedido TROCADO e item volta ao estoque | RF0044, RF0054, RN0042 | `05-troca.cy.js` |
| CTV082 | confirmar o recebimento sem retorno ao estoque não altera o estoque | RF0044, RF0054 | `05-troca.cy.js` |
| CTV083 | o recebimento gera um cupom de troca, no valor dos itens trocados, disponível ao cliente | RF0045 | `05-troca.cy.js` |
| CTV084 | na troca parcial o cupom vale só os itens trocados | RF0045, RN0041 | `05-troca.cy.js` |
| CTV085 | o cupom de troca gerado pode ser usado como pagamento numa nova compra | RF0037, RF0045 | `05-troca.cy.js` |
| CTV086 | a troca só avança na ordem: não dá para confirmar recebimento sem autorização | RF0042, RF0044 | `05-troca.cy.js` |
| CTV100 | compra, aprovação, despacho, entrega, troca, recebimento e uso do cupom de troca | RF0031, RF0032, RF0033, RF0034, RF0035, RF0036, RF0037, RF0038, RF0039, RF0040, RF0041, RF0042, RF0043, RF0044, RF0045, RF0053, RF0054, RN0031, RN0033, RN0037, RN0038, RN0039, RN0040, RN0041, RN0042, RN0043, RN0044, RN0046 | `06-caso-de-uso-completo.cy.js` |
| CTV110 | não aceita dois cupons promocionais na mesma compra | RN0033 | `07-regras-de-negocio-api.cy.js` |
| CTV111 | com mais de um cartão e sem cupom, cada cartão precisa pagar ao menos R$ 10,00 | RN0034 | `07-regras-de-negocio-api.cy.js` |
| CTV112 | combinado com cupom, o cartão pode pagar menos de R$ 10,00 | RN0035 | `07-regras-de-negocio-api.cy.js` |
| CTV113 | os pagamentos precisam cobrir exatamente o valor da compra |  | `07-regras-de-negocio-api.cy.js` |
| CTV114 | não permite usar cupons desnecessários além do que cobre a compra | RN0036 | `07-regras-de-negocio-api.cy.js` |
| CTV115 | cupom maior que a compra é aceito e gera cupom de troco com a diferença | RN0036 | `07-regras-de-negocio-api.cy.js` |
| CTV116 | um cupom já utilizado não pode ser usado de novo | RN0037 | `07-regras-de-negocio-api.cy.js` |
| CTV117 | cupom de troca de outro cliente não pode ser usado | RN0037 | `07-regras-de-negocio-api.cy.js` |
| CTV118 | o despacho só vale para pedido aprovado | RN0039 | `07-regras-de-negocio-api.cy.js` |
| CTV119 | a entrega só pode ser confirmada para pedido em transporte | RN0040 | `07-regras-de-negocio-api.cy.js` |
| CTV120 | só itens de pedidos ENTREGUES podem receber solicitação de troca | RN0043 | `07-regras-de-negocio-api.cy.js` |
| CTV121 | uma troca só pode ser decidida uma vez e o pedido TROCADO não aceita nova troca | RN0042, RN0043 | `07-regras-de-negocio-api.cy.js` |
| CTV122 | pedido ou troca inexistente devolve 404 |  | `07-regras-de-negocio-api.cy.js` |

## 3. Lacunas conhecidas

- **RNF0011** (consulta em até 1 s): não há teste de desempenho para Vendas (no módulo de Clientes existe o teste 18, que mede a consulta do front).
- **RNF0012** (log de escrita): implementado por aspecto e visível no terminal do backend (`OPERACOES_DE_ESCRITA`); não há asserção automatizada.
- **RF0034** (frete): valor fixo (decisão do grupo, justificada em `docs/JUSTIFICATIVA_FRETE.md`); o DRS pede cálculo "com base nos itens e no endereço" mas não define a fórmula.
- **RNF0033** (senha criptografada) pertence ao módulo de Clientes e não é verificável pela interface.
