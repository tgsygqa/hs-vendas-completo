# Roteiro da apresentação — CDU02 "Gerenciar Venda Eletrônica"

Tudo aqui foi percorrido de ponta a ponta no navegador antes de virar roteiro. Os valores de tela (R$ 1.184,60 etc.) são os reais da massa de dados.

## 0. Preparação (5 min antes)

1. Dois terminais: `npm run start:back` e `npm run start:front` (ou `npm run start:back:demo`, se for mostrar a expiração do carrinho — seção 3-D).
2. Abra em abas: **Loja** `localhost:5600/vendas.html` · **Meus pedidos** `…/pedidos.html` · **Administração** `…/admin.html`. (Opcional: console do banco `localhost:8080/h2-console`.)
3. Se já usou o sistema: `npm run demo:reset` (volta tudo ao início **sem** reiniciar).
4. Deixe o terminal do backend visível: cada escrita aparece como `OPERACOES_DE_ESCRITA … resultado=sucesso|falha` (**RNF0012**).

**Massa de dados inicial**

| O quê | Valor |
|---|---|
| Cliente | **João Silva** (id 1) — endereço "Casa", cartão VISA •••• 4242 |
| Produtos | RTX 4090 (R$ 14.500,00 · 12 un) · Ryzen 9 (R$ 4.899,90 · 25) · **Headset (R$ 349,90 · 40)** · **Teclado (R$ 459,90 · 30)** · SSD (R$ 1.250,00 · 18) |
| Pedidos | **#1 ENTREGUE** (1 SSD) · **#2 EM TRANSPORTE** (1 Headset) |
| Cupons | PROMO-BEMVINDO R$ 50 · PROMO-VERAO R$ 30 (promocionais) · TROCA-JOAO-PEQUENO R$ 20 · TROCA-JOAO-GRANDE R$ 500 (troca) |
| Frete | R$ 24,90 fixo |

---

## 1. Caminho principal — do carrinho à troca (≈ 6 min)

É exatamente o teste **CTV022** (`06-caso-de-uso-completo.cy.js`).

| # | Onde | O que fazer | O que mostrar / dizer | Req. |
|---|---|---|---|---|
| 1 | Loja | Adicione **2× Headset** e **1× Teclado** | Carrinho **R$ 1.159,70**; o contador "Itens reservados por 29:59" — os itens ficam **bloqueados** para outros clientes | RF0031, RF0032, RN0031, RN0044 |
| 2 | Loja | **Iniciar Compra** → endereço *Casa* → cupom **PROMO-BEMVINDO** → cartão **VISA 4242** | Frete R$ 24,90; total **R$ 1.184,60**; o cartão cobra **R$ 1.134,60** (total − cupom); "Pagamento completo". Só **um** cupom promocional por compra (radio) | RF0033–RF0037, RN0033 |
| 3 | Loja | **Confirmar Pagamento** | **Pedido #3 — EM PROCESSAMENTO** | RF0038 |
| 4 | Admin → Pedidos | #3 → **Validar pagamento** | A "operadora" (simulada) aprova → **APROVADA**. Aba **Estoque**: Headset 40→**38**, Teclado 30→**29** (baixa só na aprovação) | RN0037, RN0038, RF0053 |
| 5 | Admin → Pedidos | **Despachar** → **Confirmar entrega** | EM TRANSPORTE → **ENTREGUE** (cada botão só aparece no status certo) | RF0039, RF0040, RN0039, RN0040 |
| 6 | Meus pedidos | Pedido #3 → **Solicitar troca** → marque o *Headset*, qtd **1**, motivo → **Enviar** | Pedido vira **EM TROCA**. Só pedidos ENTREGUES mostram o botão (o #2, em transporte, não) | RF0041, RN0041, RN0043 |
| 7 | Admin → Trocas | Veja a solicitação (cliente, item, motivo) → **Autorizar** | **TROCA AUTORIZADA**; aviso "O cliente foi notificado" | RF0043, RF0042 |
| 8 | Meus pedidos | **↻ Atualizar** | **Notificação** "troca AUTORIZADA" | RN0046 |
| 9 | Admin → Trocas | **Sim, voltam** (itens retornam ao estoque) | **TROCADO**; cupom **TROCA-P3-S1 de R$ 349,90** gerado; aba Estoque: Headset 38→**39** | RF0044, RF0054, RN0042, RF0045 |
| 10 | Meus pedidos / Loja | Cupom aparece em "Meus cupons"; numa **nova compra** (1 Teclado) escolha **TROCA-P3-S1** + cartão | Cupom de R$ 349,90 + cartão de R$ 134,90 = R$ 484,80 | RF0037, RF0045 |

---

## 2. Rodar os testes automatizados ao vivo (≈ 2 min)

```
npm run test:vendas:open
```
Na janela do Cypress: **E2E Testing → navegador → Start E2E Testing → `06-caso-de-uso-completo.cy.js`**. O mesmo caminho da seção 1 roda sozinho em ~20 s, com cada passo anotado (`cy.log`).
> **Plano B** (se a janela do Cypress abrir em branco): `npm run test:vendas:ao-vivo` roda o mesmo caso de uso num Edge visível, sem a janela inicial do Cypress. Teste antes da apresentação, na máquina que vai usar.
Para o panorama geral: `npm run test:vendas` (headless) mostra **27 testes** passando; `docs/MATRIZ_RASTREABILIDADE.md` liga cada RF/RN aos testes.

---

## 3. Fluxos alternativos (escolha os que couberem no tempo)

### A) Pagamento reprovado (E3 do CDU) — RN0038 / RN0028
1. Loja: 1 Headset → Iniciar Compra → endereço → **+ Cadastrar novo cartão**: número `4111 1111 1111 0000` (só dígitos: `4111111111110000`), VISA, titular, CVV `123` → **Salvar Cartão** (já vem selecionado) → **Confirmar Pagamento**.
2. Admin → **Validar pagamento** → **REPROVADA**: mensagem "Cartão final 0000 recusado pela operadora"; estoque **não muda** (aba Estoque); sem botão de despachar.
3. Meus pedidos: o cliente vê o **motivo da reprovação**.
> A "operadora" é simulada: **cartão terminado em 0000 é recusado**; os demais, aprovados. Se usou cupom, ele **volta a ficar disponível**.

### B) Estoque mudou durante a compra — RN0032
1. Loja: **5× Headset** → Iniciar Compra → endereço e cartão marcados (não confirme ainda).
2. Em outro terminal: `npm run demo:estoque -- 3 3` (outro cliente "levou" o estoque: sobram 3).
3. **Confirmar Pagamento** → alerta "A quantidade de Headset foi ajustada de 5 para 3…" e o **carrinho já aparece corrigido**. (Com `npm run demo:estoque -- 3 0` o item é **removido** com alerta de indisponível.)

### C) Cupom maior que a compra gera troco — RN0036
Loja: 1 Headset (R$ 374,80) → cupom **TROCA-JOAO-GRANDE (R$ 500)**, sem cartão → "Pagamento completo". Em Meus pedidos aparece **"Cupom de troco gerado — R$ 125,20"**, já disponível para a próxima compra. (Selecionar também o de R$ 20 seria recusado: cupom desnecessário.)

### D) Bloqueio entre clientes e expiração — RN0044 / RN0045 / RNF0042
> Use o backend com prazos curtos: `npm run start:back:demo` (bloqueio de **3 min**, aviso aos 2 min, varredura a cada 10 s).
1. `npm run demo:cliente2` (cadastra a **Maria**; depois de um `demo:reset` é preciso rodar de novo).
2. João (Loja): **12× RTX 4090**. Maria (`vendas.html?clienteId=2`): a RTX aparece **"Indisponível — reservado em outros carrinhos"** e o botão fica desabilitado (os outros produtos seguem livres).
3. No carrinho do João o **contador** corre; com ~2 min restantes aparece o **aviso de expiração** ("Seus itens serão liberados…").
4. Aos 3 min os itens **saem do carrinho** e ficam listados em **"Itens retirados do carrinho"**; **Iniciar Compra fica desabilitado**. A RTX volta a aparecer disponível para a Maria.
5. João clica **Adicionar novamente** → item volta, compra reabilitada (ou **dispensar**).

### E) Troca recusada
Meus pedidos → pedido **#1** (ENTREGUE) → solicitar troca → Admin → Trocas → **Recusar** (informe o motivo) → pedido volta a **ENTREGUE**, cliente é notificado e **pode solicitar de novo**.

---

## 4. Perguntas que o professor pode fazer

| Pergunta | Resposta honesta |
|---|---|
| Como é calculado o frete? E por que essa fórmula? | **Valor fixo de R$ 24,90 por compra — decisão do grupo.** O RF0034 pede cálculo por itens e endereço, mas não define a fórmula, e este módulo não tem os dados de um frete real (peso, dimensões, CEP de origem). O valor fixo deixa o total previsível (necessário para as regras RN0034–RN0036 de pagamento exato e para testes repetíveis) e fica isolado numa constante, fácil de trocar. **Admita a limitação:** não varia com itens nem endereço. Argumentos completos e uma fórmula alternativa em [`JUSTIFICATIVA_FRETE.md`](JUSTIFICATIVA_FRETE.md). |
| Existe login / perfil de administrador? | Não: o DRS não define autenticação. "Cliente" e "Administrador" são **telas separadas**; o cliente das telas de cliente vem da URL (`?clienteId=`). |
| A operadora de cartão é real? | Não, é **simulada** (`PagamentoGatewayService`): final **0000** é recusado — para mostrar APROVADA e REPROVADA de forma repetível. |
| Quem valida o pagamento? | Ao finalizar, o pedido fica EM PROCESSAMENTO (RF0038). A validação (RN0037/RN0038) é disparada pelo botão do painel, que simula o retorno da operadora. |
| E se o administrador recusar a troca? | O DRS só descreve "autorizar"; a **recusa (com motivo)** é um caminho assumido — pedido volta a ENTREGUE e o cliente é notificado. |
| Como garantem que os testes não dependem uns dos outros? | Cada teste começa com `POST /api/test-support/reset` (base zerada com os mesmos ids). Esse endpoint só existe com `app.test-support.enabled=true`. |
| Cobertura dos requisitos? | `docs/MATRIZ_RASTREABILIDADE.md` — todos os RF/RN do CDU02 têm teste; lacunas declaradas: RNF0011 (desempenho) e RNF0012 (log; implementado, verificação manual). |
| O que o cliente vê se o item expirar? | Lista "itens retirados do carrinho", compra desabilitada até adicionar de novo (RNF0042). |
