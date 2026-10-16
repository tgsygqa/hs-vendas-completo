# Matriz de rastreabilidade — módulo de Vendas (CDU02)

> **Gerada automaticamente** (`npm run docs:matriz`) a partir dos títulos dos testes em `cypress/e2e/vendas/*.cy.js`; cada título termina com os requisitos que ele cobre. Se mudar um teste, rode o comando de novo.

Base: `DRS_LES_2_2026` v0.6 · **27 testes automatizados Cypress** (um teste dedicado por requisito do escopo de Vendas + o caso de uso completo) + testes de integração JUnit da API (`backend/src/test`).

## 1. Requisito → testes

| Requisito | Descrição (DRS) | Teste dedicado | Situação |
|---|---|---|---|
| RF0031 | Gerenciar carrinho de compra | CTV001 | Coberto |
| RF0032 | Definir quantidade de itens no carrinho | CTV002 | Coberto |
| RF0033 | Realizar compra | CTV006 | Coberto |
| RF0034 | Calcular frete | CTV007 | Coberto (frete **fixo** R$ 24,90 — decisão do grupo, ver `docs/JUSTIFICATIVA_FRETE.md`) |
| RF0035 | Selecionar endereço de entrega | CTV008 | Coberto |
| RF0036 | Selecionar forma de pagamento | CTV009 | Coberto |
| RF0037 | Uso de cupom para pagamento | CTV010 | Coberto |
| RF0038 | Finalizar compra (EM PROCESSAMENTO) | CTV006 | Coberto |
| RF0039 | Despachar produtos para entrega | CTV014 | Coberto |
| RF0040 | Produtos entregues | CTV015 | Coberto |
| RF0041 | Solicitar troca | CTV016 | Coberto |
| RF0042 | Autorizar trocas | CTV019 | Coberto |
| RF0043 | Visualização de trocas | CTV018 | Coberto |
| RF0044 | Confirmar recebimento de itens para troca | CTV020 | Coberto |
| RF0045 | Gerar cupom de troca após recebimento | CTV021 | Coberto |
| RF0053 | Dar baixa em estoque | CTV013 | Coberto |
| RF0054 | Realizar reentrada em estoque | CTV020 | Coberto |
| RN0031 | Validar estoque para adição de itens no carrinho | CTV003 | Coberto |
| RN0032 | Validar estoque para compra | CTV011 | Coberto |
| RN0033 | Uso de cupom promocional (um por compra) | CTV023 | Coberto |
| RN0034 | Uso de diversos cartões (mínimo R$ 10,00 cada) | CTV024 | Coberto |
| RN0035 | Cupons junto a cartão (dispensa o mínimo) | CTV025 | Coberto |
| RN0036 | Gerar cupom de troca (troco) | CTV026 | Coberto |
| RN0037 | Validar forma de pagamento | CTV027 | Coberto |
| RN0038 | Status APROVADA/REPROVADA conforme pagamento | CTV012 | Coberto |
| RN0039 | Status EM TRANSPORTE | CTV014 | Coberto |
| RN0040 | Status ENTREGUE | CTV015 | Coberto |
| RN0041 | Gerar pedido de troca (EM TROCA) | CTV016 | Coberto |
| RN0042 | Status TROCADO após recebimento | CTV020 | Coberto |
| RN0043 | Só itens ENTREGUES podem ser trocados | CTV017 | Coberto |
| RN0044 | Bloqueio de produtos no carrinho | CTV004 | Coberto |
| RN0045 | Retirar item do carrinho ao desbloquear | CTV005 | Coberto |
| RN0046 | Notificação de autorização de troca | CTV019 | Coberto |
| RNF0042 | Apresentar itens retirados do carrinho | CTV005 | Coberto |
| RNF0012 | Log de transação (escrita) | — | Implementado (aspecto `LogDeEscritaAspect`); verificação manual no log do backend |
| RNF0011 | Tempo de resposta (consultas) | — | Não avaliado automaticamente (sem teste de desempenho) |

> **CTV022** (`06-caso-de-uso-completo.cy.js`) percorre o caso de uso inteiro pela interface — do carrinho à troca e ao uso do cupom — e por isso exercita, em sequência, os requisitos acima; é o teste para rodar ao vivo na apresentação.

## 2. Caso de teste → requisitos

| ID | Título | Requisitos | Arquivo |
|---|---|---|---|
| CTV001 | deve gerenciar o carrinho: listar os produtos, adicionar, visualizar e excluir itens | RF0031 | `01-carrinho.cy.js` |
| CTV002 | deve definir a quantidade ao adicionar o produto e alterá-la no carrinho | RF0032 | `01-carrinho.cy.js` |
| CTV003 | não deve permitir adicionar item indisponível nem mais itens do que o estoque | RN0031 | `01-carrinho.cy.js` |
| CTV004 | itens no carrinho ficam bloqueados por um prazo, indisponíveis para outros clientes, com aviso 5 minutos antes de expirar | RN0044 | `02-bloqueio-carrinho.cy.js` |
| CTV005 | ao expirar o bloqueio os itens saem do carrinho e são apresentados como retirados; comprar fica desabilitado até adicioná-los de novo | RN0045, RNF0042 | `02-bloqueio-carrinho.cy.js` |
| CTV006 | deve realizar a compra a partir do carrinho e finalizá-la com o pedido EM PROCESSAMENTO | RF0033, RF0038 | `03-checkout-pagamento.cy.js` |
| CTV007 | o frete é calculado e somado ao total da compra | RF0034 | `03-checkout-pagamento.cy.js` |
| CTV008 | deve cadastrar um novo endereço de entrega durante a compra e incorporá-lo ao perfil do cliente | RF0035 | `03-checkout-pagamento.cy.js` |
| CTV009 | deve cadastrar um novo cartão de crédito durante a compra e incorporá-lo ao perfil do cliente | RF0036 | `03-checkout-pagamento.cy.js` |
| CTV010 | deve pagar com um cupom promocional e completar com cartão de crédito | RF0037 | `03-checkout-pagamento.cy.js` |
| CTV011 | se o estoque mudar antes da finalização, ajusta a quantidade ou remove o item indisponível, com alerta | RN0032 | `03-checkout-pagamento.cy.js` |
| CTV012 | o pagamento validado aprova o pedido (APROVADA); recusado pela operadora o reprova (REPROVADA) | RN0038 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV013 | a baixa em estoque só acontece quando o pagamento é aprovado | RF0053 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV014 | o administrador despacha um pedido aprovado e ele passa a EM TRANSPORTE | RF0039, RN0039 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV015 | o administrador confirma a entrega e o pedido passa a ENTREGUE | RF0040, RN0040 | `04-aprovacao-despacho-entrega.cy.js` |
| CTV016 | o cliente solicita a troca de um item e o pedido passa a EM TROCA | RF0041, RN0041 | `05-troca.cy.js` |
| CTV017 | só itens de pedidos ENTREGUES podem receber solicitação de troca | RN0043 | `05-troca.cy.js` |
| CTV018 | o administrador visualiza as solicitações de troca com cliente, itens e motivo | RF0043 | `05-troca.cy.js` |
| CTV019 | o administrador autoriza a troca (TROCA AUTORIZADA) e o cliente recebe uma notificação | RF0042, RN0046 | `05-troca.cy.js` |
| CTV020 | o administrador confirma o recebimento com retorno ao estoque: pedido TROCADO e item volta ao estoque | RF0044, RF0054, RN0042 | `05-troca.cy.js` |
| CTV021 | o recebimento gera um cupom de troca, no valor dos itens trocados, disponível ao cliente | RF0045 | `05-troca.cy.js` |
| CTV022 | compra, aprovação, despacho, entrega, troca, recebimento e uso do cupom de troca | todos (fluxo completo) | `06-caso-de-uso-completo.cy.js` |
| CTV023 | não aceita dois cupons promocionais na mesma compra | RN0033 | `07-regras-de-negocio-api.cy.js` |
| CTV024 | com mais de um cartão e sem cupom, cada cartão precisa pagar ao menos R$ 10,00 | RN0034 | `07-regras-de-negocio-api.cy.js` |
| CTV025 | combinado com cupom, o cartão pode pagar menos de R$ 10,00 | RN0035 | `07-regras-de-negocio-api.cy.js` |
| CTV026 | não permite cupons desnecessários e, quando o cupom supera a compra, gera um cupom de troco | RN0036 | `07-regras-de-negocio-api.cy.js` |
| CTV027 | a forma de pagamento é validada: um cupom já utilizado não pode ser usado de novo | RN0037 | `07-regras-de-negocio-api.cy.js` |

## 3. Lacunas conhecidas

- **RNF0011** (consulta em até 1 s): não há teste de desempenho para Vendas (no módulo de Clientes existe o teste 18, que mede a consulta do front).
- **RNF0012** (log de escrita): implementado por aspecto e visível no terminal do backend (`OPERACOES_DE_ESCRITA`); não há asserção automatizada.
- **RF0034** (frete): valor fixo (decisão do grupo, justificada em `docs/JUSTIFICATIVA_FRETE.md`); o DRS pede cálculo "com base nos itens e no endereço" mas não define a fórmula.
- **Cenários extras fora da suíte Cypress enxuta:** a suíte tem um teste dedicado por requisito do DRS. Caminhos adicionais (recusa de troca com motivo — caminho assumido, não está no DRS —, expiração com a tela aberta, dispensar item expirado, validações de formulário…) não têm teste Cypress; vários seguem cobertos pelos testes de integração JUnit (`npm run test:backend`), por exemplo troca recusada, bloqueio entre clientes e expiração sem o job.
- **RNF0033** (senha criptografada) pertence ao módulo de Clientes e não é verificável pela interface.
