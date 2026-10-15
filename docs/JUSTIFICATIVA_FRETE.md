# Justificativa da fórmula de frete (RF0034)

> Rascunho para você adaptar com as suas palavras. O professor pediu que o grupo **explique por que escolheu a fórmula** — aqui estão os argumentos, a limitação que convém admitir e uma alternativa, caso queiram mudar.

## O que o DRS diz

**RF0034 — Calcular frete:** *"O sistema deve calcular o frete da compra com base nos itens selecionados e o endereço apontado pelo cliente."*
O DRS **não define** a fórmula, a tabela de preços, o CEP de origem nem a integração com transportadora.

## O que foi implementado

**Frete fixo de R$ 24,90 por compra**, igual para qualquer carrinho e endereço. É somado ao total da compra e **congelado no pedido** (se o valor mudar no futuro, pedidos antigos não mudam).

## Por que essa fórmula (argumentos)

1. **Os dados de um frete real não existem neste módulo.** Frete de verdade depende de peso, dimensões e distância (CEP de origem → CEP de destino). O `Produto` do módulo de Vendas tem só nome, categoria, fornecedor, preço e estoque; peso e dimensões fazem parte do *Cadastro de Livros/Produtos* (RN0011), que é outro caso de uso, de outro grupo.
2. **Não há tabela oficial nem integração** (Correios, transportadora) no escopo. Qualquer tabela por região ou por peso seria **inventada** — tão arbitrária quanto um valor fixo, só que mais difícil de explicar e de testar.
3. **Previsibilidade para as regras de pagamento.** RN0034, RN0035 e RN0036 exigem que cartões e cupons cubram **exatamente** o total e geram cupom de troco pela diferença. Com um frete determinístico, o total é sempre conhecido, o que torna essas regras verificáveis e os **testes automatizados repetíveis** (o teste de caso de uso completo confere, centavo a centavo, R$ 1.184,60, R$ 1.134,60 no cartão etc.).
4. **Isolamento.** O valor está em uma única constante no backend (`PedidoService.FRETE_PADRAO`) — trocar por uma fórmula real não exige mexer nas regras de pagamento, de cupom nem de status.
5. **Existe no comércio real:** várias lojas praticam *frete único* ("frete fixo") para simplificar.

## Limitação que convém admitir

O RF0034 diz *"com base nos itens e no endereço"* e o frete fixo **não varia com nenhum dos dois**. Apresente como **simplificação consciente** por falta de dados e de fórmula definida, não como atendimento literal do requisito.

## Alternativa, se quiserem atender o texto do RF0034

Uma fórmula simples que usa **itens** e **endereço** com os dados que já existem (UF do endereço e quantidade de itens), por exemplo:

> frete = **taxa da região do estado** de entrega (SP: R$ 14,90 · demais estados: R$ 29,90) **+ R$ 3,00 por unidade a partir da 2ª** ; **frete grátis** acima de R$ 5.000,00.

Prós: usa itens e endereço (atende o texto). Contras: os valores continuam arbitrários (precisam ser justificados do mesmo jeito) e exige mexer em `PedidoService`, na tela de checkout (que hoje repete a constante `FRETE` em `vendas.html`, então o cálculo precisaria vir de um endpoint do backend) e nos testes (`FRETE` em `cypress/support/helpers.js` e `commands.js`). Se quiserem seguir por esse caminho, peça e eu implemento.

## Onde está o valor hoje

| Onde | O quê |
|---|---|
| `backend/.../venda/service/PedidoService.java` | `FRETE_PADRAO` — o valor que vale (grava no pedido) |
| `frontend/vendas.html` | `const FRETE` — repete o valor só para **exibir** o total antes de confirmar |
| `cypress/support/helpers.js`, `commands.js` | `FRETE`/`24.9` — valores esperados nos testes |
| `backend/.../cliente/config/DataSeeder.java` | pedidos de exemplo #1 e #2 (ambos com R$ 24,90, coerentes com a regra) |
