# Justificativa da fórmula de frete (RF0034)

> O professor pediu que o grupo **defina a regra do frete e a documente**. Este arquivo é o documento da regra: o que o DRS diz, a fórmula adotada, por que ela foi escolhida e as limitações que convém admitir na apresentação.

## O que o DRS diz

**RF0034 — Calcular frete:** *"O sistema deve calcular o frete da compra com base nos itens selecionados e o endereço apontado pelo cliente."*
O DRS **não define** a fórmula, a tabela de preços, o CEP de origem nem a integração com transportadora. Por isso a regra abaixo é uma **decisão do grupo**.

## A regra adotada

> **frete = tarifa do estado de entrega + R$ 3,00 por unidade a partir da 2ª unidade do carrinho**

| Estado do endereço de entrega | Tarifa |
|---|---|
| São Paulo (SP) | R$ 24,90 |
| Demais estados do Sudeste e do Sul (RJ, MG, ES, PR, SC, RS) | R$ 29,90 |
| Demais estados (Norte, Nordeste e Centro-Oeste) | R$ 39,90 |

| Como o RF0034 é atendido | Na regra |
|---|---|
| **Endereço apontado pelo cliente** | a **UF** do endereço escolhido define a tarifa (aceita a sigla, "SP", ou o nome, "São Paulo") |
| **Itens selecionados** | a **quantidade de unidades** do carrinho define o adicional (R$ 3,00 por unidade extra) |

Exemplos (entrega em SP): 1 unidade = R$ 24,90 · 2 unidades = R$ 27,90 · 3 unidades = R$ 30,90. A mesma compra de 1 unidade entregue na Bahia custa R$ 39,90.

O valor é calculado **no backend** (`FreteService`), consultado pela tela de checkout (`GET /api/clientes/{id}/frete?enderecoId=`) e **gravado no pedido** — se a regra mudar no futuro, os pedidos antigos não mudam. O frete é somado ao total, e é esse total que as regras de pagamento (RN0034 a RN0036) conferem.

## Por que essa fórmula (argumentos)

1. **Usa exatamente o que o requisito cita:** itens (quantidade) e endereço (estado). Não depende de dados que o módulo de Vendas não tem.
2. **Os dados de um frete real não existem neste módulo.** Frete de verdade depende de peso, dimensões e distância (CEP de origem → destino). O `Produto` de Vendas só tem nome, categoria, fornecedor, preço e estoque; peso e dimensões pertencem ao cadastro de produtos, que é outro caso de uso.
3. **Reflete a lógica real, de forma simples:** quanto mais longe do centro de distribuição (supõe-se que seja em SP), mais caro; quanto mais volume, mais caro.
4. **Determinística.** O total é sempre previsível, o que torna as regras de pagamento (RN0034–RN0036: cartões e cupons cobrem **exatamente** o total e o troco vira cupom) e os **testes automatizados** repetíveis — o teste do caso de uso completo confere, centavo a centavo, R$ 30,90 de frete e R$ 1.190,60 de total.
5. **Isolada.** A regra está num único serviço (`FreteService`) com as tarifas em constantes — trocar a tabela ou a fórmula não mexe nas regras de pagamento, de cupom nem de status.

## Limitações que convém admitir

- É uma **tabela simples por região**, sem peso, dimensões, prazo, transportadora ou CEP de origem.
- O "centro de distribuição" em SP é uma suposição do grupo.
- O campo "estado" do endereço é texto livre: se não for reconhecido como estado brasileiro, aplica-se a tarifa mais alta (demais estados).

## Onde está a regra no código

| Onde | O quê |
|---|---|
| `backend/.../venda/service/FreteService.java` | a fórmula e as tarifas (constantes) |
| `backend/.../venda/service/PedidoService.java` | usa o `FreteService` ao finalizar a compra e grava o frete no pedido |
| `backend/.../venda/controller/PedidoController.java` | `GET /api/clientes/{id}/frete?enderecoId=` — usado pela tela de checkout |
| `frontend/vendas.html` | só **exibe** o valor devolvido pelo backend (não repete a regra) |
| `cypress/support/helpers.js` | `freteSp(unidades)` — valor esperado nos testes para o endereço de SP |
| `backend/src/test/.../VendasFluxoIntegrationTest.java` | teste JUnit `freteConformeItensEEndereco` (SP, RJ e Bahia, 1 a 3 unidades) |
| `backend/.../cliente/config/DataSeeder.java` | pedidos de exemplo #1 e #2 (1 unidade em SP = R$ 24,90, coerentes com a regra) |
