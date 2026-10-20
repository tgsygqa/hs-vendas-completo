# Matriz de rastreabilidade — módulo de Vendas (CDU02)

> **Gerada automaticamente** (`npm run docs:matriz`) a partir dos títulos dos testes em `cypress/e2e/vendas/*.cy.js`; cada título termina com os requisitos que ele cobre. Se mudar um teste, rode o comando de novo.

Base: `DRS_LES_2_2026` v0.6 · **37 testes automatizados Cypress** (27 dedicados, um por requisito do escopo de Vendas + 10 etapas do caso de uso completo) + testes de integração JUnit da API (`backend/src/test`).

## 1. Roteiro mínimo de demonstração (enunciado, seção 7) → testes

O enunciado pede que estes 11 cenários sejam apresentados por **testes funcionais automatizados**. Todos são testes Cypress desta suíte: `npm run test:vendas` roda todos e mostra o título de cada um; `npm run test:vendas:open` roda ao vivo no navegador.

| Cenário | Descrição | Testes (nº) | Arquivo |
|---|---|---|---|
| 1 | Compra aprovada com um cartão | 6, 12 | `03-checkout-pagamento.cy.js`, `04-aprovacao-despacho-entrega.cy.js` |
| 2 | Compra com dois ou mais cartões, com a tentativa de pagar menos de R$ 10,00 em um deles bloqueada | 33 | `07-regras-de-negocio-api.cy.js` |
| 3 | Compra com cupom promocional e cartão, com menos de R$ 10,00 no cartão (permitido) | 34, 10 | `07-regras-de-negocio-api.cy.js`, `03-checkout-pagamento.cy.js` |
| 4 | Compra paga com cupons de troca de valor superior ao da compra, com emissão do cupom de diferença | 35 | `07-regras-de-negocio-api.cy.js` |
| 5 | Compra REPROVADA, com itens desbloqueados e sem baixa de estoque | 13, 12 | `04-aprovacao-despacho-entrega.cy.js` |
| 6 | Expiração do bloqueio no carrinho, com remoção dos itens e desativação do botão de compra | 5 | `02-bloqueio-carrinho.cy.js` |
| 7 | Fluxo do administrador: APROVADA, EM TRANSPORTE, ENTREGUE | 24, 25, 26 | `06-caso-de-uso-completo.cy.js` |
| 8 | Tentativa de solicitar a troca de um pedido que não está ENTREGUE (bloqueada) | 17 | `05-troca.cy.js` |
| 9 | Troca de um item de um pedido e troca de uma compra inteira | 16 | `05-troca.cy.js` |
| 10 | Fluxo da troca: solicitação, visualização pelo administrador, autorização com notificação, recebimento, cupom e reentrada no estoque | 27, 28, 29, 30 | `06-caso-de-uso-completo.cy.js` |
| 11 | Uso do cupom de troca gerado em uma nova compra | 31 | `06-caso-de-uso-completo.cy.js` |

Os cenários 2, 3 e 4 (regras de pagamento) são testes de API (`cy.request`), porque a tela já impede a violação das regras; os demais passam pela interface. Os cenários 7, 10 e 11 são as etapas sequenciais, pela tela, do arquivo `06-caso-de-uso-completo.cy.js` (testes 22 a 31); os testes dedicados 14, 15 e 18 a 21 repetem esses passos isoladamente.

## 2. Requisito → testes

| Requisito | Descrição (DRS) | Teste dedicado (nº) | Situação |
|---|---|---|---|
| RF0031 | Gerenciar carrinho de compra | 1 | Coberto |
| RF0032 | Definir quantidade de itens no carrinho | 2 | Coberto |
| RF0033 | Realizar compra | 6 | Coberto |
| RF0034 | Calcular frete (itens + endereço) | 7 | Coberto (frete = tarifa do estado + R$ 3,00 por unidade extra — regra do grupo, ver `docs/JUSTIFICATIVA_FRETE.md`) |
| RF0035 | Selecionar endereço de entrega | 8 | Coberto |
| RF0036 | Selecionar forma de pagamento | 9 | Coberto |
| RF0037 | Uso de cupom para pagamento | 10 | Coberto |
| RF0038 | Finalizar compra (EM PROCESSAMENTO) | 6 | Coberto |
| RF0039 | Despachar produtos para entrega | 14 | Coberto |
| RF0040 | Produtos entregues | 15 | Coberto |
| RF0041 | Solicitar troca | 16 | Coberto |
| RF0042 | Autorizar trocas | 19 | Coberto |
| RF0043 | Visualização de trocas | 18 | Coberto |
| RF0044 | Confirmar recebimento de itens para troca | 20 | Coberto |
| RF0045 | Gerar cupom de troca após recebimento | 21 | Coberto |
| RF0053 | Dar baixa em estoque | 13 | Coberto |
| RF0054 | Realizar reentrada em estoque | 20 | Coberto |
| RN0028 | Validar retorno da operadora (baixa só após efetivada) | 13 | Coberto (testes 12 e 13; itens de pagamento não aprovado voltam a ficar disponíveis) |
| RN0031 | Validar estoque para adição de itens no carrinho | 3 | Coberto |
| RN0032 | Validar estoque para compra | 11 | Coberto |
| RN0033 | Uso de cupom promocional (um por compra) | 32 | Coberto |
| RN0034 | Uso de diversos cartões (mínimo R$ 10,00 cada) | 33 | Coberto |
| RN0035 | Cupons junto a cartão (dispensa o mínimo) | 34 | Coberto |
| RN0036 | Gerar cupom de troca (troco) | 35 | Coberto |
| RN0037 | Validar forma de pagamento (automático) | 12, 36 | Coberto |
| RN0038 | Status APROVADA/REPROVADA (automático) | 6, 12 | Coberto |
| RN0039 | Status EM TRANSPORTE | 14 | Coberto |
| RN0040 | Status ENTREGUE | 15 | Coberto |
| RN0041 | Gerar pedido de troca (EM TROCA) | 16 | Coberto |
| RN0042 | Status TROCADO após recebimento | 20 | Coberto |
| RN0043 | Só itens ENTREGUES podem ser trocados | 17 | Coberto |
| RN0044 | Bloqueio de produtos no carrinho | 4 | Coberto |
| RN0045 | Retirar item do carrinho ao desbloquear | 5 | Coberto |
| RN0046 | Notificação de autorização de troca | 19 | Coberto |
| RNF0042 | Apresentar itens retirados do carrinho | 5 | Coberto |
| RNF0012 | Log de transação (escrita) | — | Aspecto `LogDeEscritaAspect`; verificado pelo teste JUnit `LogDeEscritaIntegrationTest` (e visível no terminal do backend) |
| RNF0011 | Tempo de resposta (consultas) | 37 | Coberto |

> A coluna "Teste dedicado" mostra o teste que existe só para aquele requisito. O arquivo `06-caso-de-uso-completo.cy.js` (**testes 22 a 31**, sequenciais) percorre o caso de uso inteiro pela interface — do carrinho à troca e ao uso do cupom — e exercita 26 requisitos (RF0031, RF0032, RF0033, RF0034, RF0035, RF0036, RF0037, RF0038, RF0039, RF0040, RF0041, RF0042, RF0043, RF0044, RF0045, RF0053, RF0054, RN0028, RN0037, RN0038, RN0039, RN0040, RN0041, RN0042, RN0044, RN0046); é o arquivo para rodar ao vivo na apresentação. Como segue só o caminho feliz, **não substitui** os dedicados: ficam de fora dele RN0031, RN0032, RN0033, RN0034, RN0035, RN0036, RN0043, RN0045 (e o ramo REPROVADA da RN0038).

## 3. Caso de teste → requisitos

| Nº | Título | Requisitos | Arquivo |
|---|---|---|---|
| 1 | deve gerenciar o carrinho: listar os produtos, adicionar, visualizar e excluir itens | RF0031 | `01-carrinho.cy.js` |
| 2 | deve definir a quantidade ao adicionar o produto e alterá-la no carrinho | RF0032 | `01-carrinho.cy.js` |
| 3 | não deve permitir adicionar item indisponível nem mais itens do que o estoque | RN0031 | `01-carrinho.cy.js` |
| 4 | deve bloquear os itens do carrinho por um prazo, indisponíveis para outros clientes, com aviso 5 minutos antes de expirar | RN0044 | `02-bloqueio-carrinho.cy.js` |
| 5 | deve retirar os itens do carrinho ao expirar o bloqueio, apresentá-los como retirados e desabilitar a compra até adicioná-los de novo | RN0045, RNF0042 | `02-bloqueio-carrinho.cy.js` |
| 6 | deve realizar a compra com um cartão, finalizando-a com o pedido EM PROCESSAMENTO e vendo-o APROVADA após a validação do pagamento | RF0033, RF0038, RN0038 | `03-checkout-pagamento.cy.js` |
| 7 | deve calcular o frete com base nos itens e no endereço e somá-lo ao total da compra | RF0034 | `03-checkout-pagamento.cy.js` |
| 8 | deve cadastrar um novo endereço de entrega durante a compra e incorporá-lo ao perfil do cliente | RF0035 | `03-checkout-pagamento.cy.js` |
| 9 | deve cadastrar um novo cartão de crédito durante a compra e incorporá-lo ao perfil do cliente | RF0036 | `03-checkout-pagamento.cy.js` |
| 10 | deve pagar com um cupom promocional e completar com cartão de crédito | RF0037 | `03-checkout-pagamento.cy.js` |
| 11 | deve ajustar a quantidade ou remover o item indisponível, com alerta, se o estoque mudar antes da finalização | RN0032 | `03-checkout-pagamento.cy.js` |
| 12 | deve validar o pagamento automaticamente, aprovando o pedido (APROVADA) ou reprovando-o (REPROVADA) quando a operadora recusa o cartão | RN0037, RN0038 | `04-aprovacao-despacho-entrega.cy.js` |
| 13 | deve dar baixa em estoque somente quando o pagamento é aprovado e manter em estoque, desbloqueados, os itens de compras reprovadas | RF0053, RN0028 | `04-aprovacao-despacho-entrega.cy.js` |
| 14 | deve despachar o pedido aprovado, passando-o a EM TRANSPORTE | RF0039, RN0039 | `04-aprovacao-despacho-entrega.cy.js` |
| 15 | deve confirmar a entrega, passando o pedido a ENTREGUE | RF0040, RN0040 | `04-aprovacao-despacho-entrega.cy.js` |
| 16 | deve solicitar a troca de um item ou de toda a compra, passando o pedido a EM TROCA | RF0041, RN0041 | `05-troca.cy.js` |
| 17 | não deve permitir solicitar troca de itens de pedidos que não estejam ENTREGUES | RN0043 | `05-troca.cy.js` |
| 18 | deve visualizar as solicitações de troca com cliente, itens e motivo | RF0043 | `05-troca.cy.js` |
| 19 | deve autorizar a troca (TROCA AUTORIZADA) e notificar o cliente | RF0042, RN0046 | `05-troca.cy.js` |
| 20 | deve confirmar o recebimento com retorno ao estoque: pedido TROCADO e item de volta ao estoque | RF0044, RF0054, RN0042 | `05-troca.cy.js` |
| 21 | deve gerar um cupom de troca no valor dos itens trocados, disponível ao cliente | RF0045 | `05-troca.cy.js` |
| 22 | deve montar o carrinho com 2 headsets e 1 teclado, com os itens bloqueados por 30 minutos | RF0031, RF0032, RN0044 | `06-caso-de-uso-completo.cy.js` |
| 23 | deve escolher o endereço, pagar com cupom promocional e cartão, ver o frete somado ao total e finalizar a compra com o pedido EM PROCESSAMENTO | RF0033, RF0034, RF0035, RF0036, RF0037, RF0038 | `06-caso-de-uso-completo.cy.js` |
| 24 | deve validar o pagamento sozinho, aprovando o pedido (APROVADA) e dando baixa em estoque só agora | RF0053, RN0028, RN0037, RN0038 | `06-caso-de-uso-completo.cy.js` |
| 25 | deve despachar o pedido aprovado, passando-o a EM TRANSPORTE | RF0039, RN0039 | `06-caso-de-uso-completo.cy.js` |
| 26 | deve confirmar a entrega, passando o pedido a ENTREGUE | RF0040, RN0040 | `06-caso-de-uso-completo.cy.js` |
| 27 | deve solicitar a troca de 1 headset do pedido entregue, passando-o a EM TROCA | RF0041, RN0041 | `06-caso-de-uso-completo.cy.js` |
| 28 | deve visualizar a solicitação de troca e autorizá-la | RF0042, RF0043 | `06-caso-de-uso-completo.cy.js` |
| 29 | deve exibir ao cliente a notificação de que a troca foi autorizada | RN0046 | `06-caso-de-uso-completo.cy.js` |
| 30 | deve confirmar o recebimento, devolvendo o item ao estoque, gerando o cupom de troca e passando o pedido a TROCADO | RF0044, RF0045, RF0054, RN0042 | `06-caso-de-uso-completo.cy.js` |
| 31 | deve exibir o cupom de troca ao cliente e permitir usá-lo numa nova compra | RF0037, RF0045 | `06-caso-de-uso-completo.cy.js` |
| 32 | não deve aceitar dois cupons promocionais na mesma compra | RN0033 | `07-regras-de-negocio-api.cy.js` |
| 33 | deve exigir ao menos R$ 10,00 em cada cartão quando há mais de um cartão e nenhum cupom | RN0034 | `07-regras-de-negocio-api.cy.js` |
| 34 | deve permitir que o cartão pague menos de R$ 10,00 quando combinado com cupom | RN0035 | `07-regras-de-negocio-api.cy.js` |
| 35 | não deve permitir cupons desnecessários e deve gerar um cupom de troco quando o cupom supera a compra | RN0036 | `07-regras-de-negocio-api.cy.js` |
| 36 | deve validar a forma de pagamento, recusando um cupom já utilizado | RN0037 | `07-regras-de-negocio-api.cy.js` |
| 37 | deve responder às consultas de Vendas em no máximo 1 segundo | RNF0011 | `07-regras-de-negocio-api.cy.js` |

## 4. Lacunas conhecidas

- **RNF0012** (log de escrita): implementado por aspecto (`LogDeEscritaAspect`); como o log fica no servidor, a verificação automatizada é o teste JUnit `LogDeEscritaIntegrationTest` (`npm run test:backend`), e não um teste Cypress.
- **RF0034** (frete): o DRS pede cálculo "com base nos itens e no endereço" mas não define a fórmula; a regra (tarifa por estado + R$ 3,00 por unidade a partir da 2ª) foi definida pelo grupo e está documentada em `docs/JUSTIFICATIVA_FRETE.md`.
- **Fora do escopo, por orientação do professor:** recusa de troca e estorno financeiro **não** são implementados. A autorização da troca é o único desfecho previsto; o DRS RF0044 pede que o administrador informe se os itens trocados retornam ao estoque, e isso é mantido.
- **Numeração dos requisitos:** alguns números do enunciado diferem do DRS v0.6 (ver a seção 5). Esta matriz e os títulos dos testes seguem a numeração do DRS v0.6.
- **Cenários extras fora da suíte Cypress enxuta:** a suíte tem um teste dedicado por requisito do DRS. Caminhos adicionais (expiração com a tela aberta, dispensar item expirado, validações de formulário…) não têm teste Cypress; vários seguem cobertos pelos testes de integração JUnit (`npm run test:backend`), por exemplo bloqueio entre clientes, expiração sem o job e itens bloqueados enquanto o pagamento é validado.
- **RNF0033** (senha criptografada) pertence ao módulo de Clientes e não é verificável pela interface.

## 5. Numeração do enunciado × DRS v0.6

O texto do enunciado cita alguns requisitos com números diferentes dos do DRS v0.6 (a partir do RF0037 a lista aparece deslocada, porque o enunciado trata o uso de cupom dentro da forma de pagamento). Os testes e esta matriz usam os números do DRS v0.6; a tabela mostra a correspondência.

| No enunciado | Assunto | No DRS v0.6 (testes) |
|---|---|---|
| RF0036 | Selecionar forma de pagamento (inclui cupons) | RF0036 e RF0037 |
| RF0037 | Finalizar compra (EM PROCESSAMENTO) | RF0038 |
| RF0038 | Despachar (EM TRANSPORTE) | RF0039 |
| RF0039 | Confirmar a entrega (ENTREGUE) | RF0040 |
| RF0040 | Solicitar troca | RF0041 |
| RF0041 | Autorizar troca | RF0042 |
| RF0042 | Visualizar pedidos EM TROCA | RF0043 |
| RF0043 | Confirmar recebimento dos itens da troca | RF0044 |
| RF0044 | Gerar o cupom de troca e disponibilizá-lo ao cliente | RF0045 |
| RNF0045 | Retirar do carrinho os itens do produto desbloqueado | RN0045 |
| RNF0046 | Notificação de troca autorizada | RN0046 |
