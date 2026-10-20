# Roteiro da apresentação — CDU02 "Gerenciar Venda Eletrônica"

O enunciado (seção 7, "Roteiro mínimo de demonstração") pede que 11 cenários sejam apresentados **por meio de testes funcionais automatizados**. A seção 1 abaixo é a ordem de apresentação: cada cenário aponta para o teste Cypress que o mostra. As demais seções (passo a passo manual pelas telas, fluxos alternativos e perguntas) são apoio, caso o professor peça para ver algo na tela.

Tudo aqui foi percorrido de ponta a ponta no navegador antes de virar roteiro. Os valores de tela (R$ 1.190,60 etc.) são os reais da massa de dados.

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
| Frete | Tarifa do **estado** de entrega (SP R$ 24,90 · RJ/MG/ES/PR/SC/RS R$ 29,90 · demais R$ 39,90) **+ R$ 3,00 por unidade a partir da 2ª** |
| Validação do pagamento | **Automática**: o sistema valida sozinho ≈ 10 s depois da compra (a tela se atualiza sozinha) |

---

## 1. Roteiro mínimo de demonstração — testes automatizados (≈ 4 min)

**Como rodar ao vivo:** `npm run test:vendas:open` → *E2E Testing* → navegador → abra os arquivos da tabela (ou **Run 7 integration specs** para rodar todos os 37 testes de uma vez). Sem janela: `npm run test:vendas` mostra o título e o ✓ de cada teste no terminal. Plano B se a janela do Cypress abrir em branco: `npm run test:vendas:ao-vivo` (caso de uso completo num Edge visível).

| Cenário do enunciado | Teste (nº) | Arquivo | O que o teste mostra |
|---|---|---|---|
| 1) Compra aprovada com um cartão | **6** e 12 | `03-checkout-pagamento` · `04-aprovacao-despacho-entrega` | Compra pela tela com um cartão → pedido EM PROCESSAMENTO → o sistema valida sozinho → **APROVADA** |
| 2) Dois ou mais cartões; pagar menos de R$ 10,00 em um deles é bloqueado | **33** | `07-regras-de-negocio-api` | R$ 9,99 no 2º cartão → HTTP 409 (RN0034); R$ 10,00 → aceito |
| 3) Cupom promocional + cartão, com menos de R$ 10,00 no cartão (permitido) | **34** (e 10) | `07-regras-de-negocio-api` · `03-checkout-pagamento` | Com cupom promocional, cartão de R$ 5,00 é aceito (RN0035) |
| 4) Cupons de troca de valor superior ao da compra, com cupom de diferença | **35** | `07-regras-de-negocio-api` | Cupom de R$ 500 numa compra de R$ 374,80 → gera cupom de troco de R$ 125,20; cupom a mais é recusado (RN0036) |
| 5) Compra REPROVADA, itens desbloqueados e sem baixa de estoque | **13** (e 12) | `04-aprovacao-despacho-entrega` | Cartão final 0000 → REPROVADA; itens deixam de estar bloqueados e o estoque não muda; só a compra aprovada dá baixa |
| 6) Expiração do bloqueio no carrinho: itens removidos e botão de compra desativado | **5** | `02-bloqueio-carrinho` | Bloqueio vencido → itens saem do carrinho, aparecem como retirados e **Iniciar Compra** fica desabilitado |
| 7) Fluxo do administrador: APROVADA → EM TRANSPORTE → ENTREGUE | **24, 25, 26** | `06-caso-de-uso-completo` | Aprovação automática, **Despachar** e **Confirmar entrega** na tela do administrador |
| 8) Troca de pedido que não está ENTREGUE é bloqueada | **17** | `05-troca` | Pedido EM TRANSPORTE não tem o botão de troca e a API devolve 409 (RN0043) |
| 9) Troca de um item e troca de uma compra inteira | **16** | `05-troca` | Troca do SSD do pedido #1 (um item) e do pedido #3 inteiro, com **Selecionar todos os itens** |
| 10) Fluxo da troca: solicitação, visualização, autorização com notificação, recebimento, cupom e reentrada | **27, 28, 29, 30** | `06-caso-de-uso-completo` | Cliente solicita → admin visualiza e autoriza → cliente é notificado → admin confirma o recebimento → cupom emitido e item de volta ao estoque |
| 11) Uso do cupom de troca em uma nova compra | **31** | `06-caso-de-uso-completo` | Compra nova paga com o cupom **TROCA-P3-S1** |

**Ordem sugerida (≈ 4 min):** (1) abra `06-caso-de-uso-completo` — cenários **7, 10 e 11** pela tela, do carrinho à troca; (2) rode os demais arquivos (ou **Run all specs**) e aponte, na tabela, os testes dos cenários 1 a 6, 8 e 9. A rastreabilidade completa está em `docs/MATRIZ_RASTREABILIDADE.md`.

---

## 2. Caminho principal manual — do carrinho à troca (≈ 6 min)

Apoio, se o professor pedir para ver na tela. São exatamente os **testes 22 a 31** (`06-caso-de-uso-completo.cy.js`): uma etapa por teste, cada uma com os requisitos no título.

| # | Onde | O que fazer | O que mostrar / dizer | Req. |
|---|---|---|---|---|
| 1 | Loja | Adicione **2× Headset** e **1× Teclado** | Carrinho **R$ 1.159,70**; o contador "Itens reservados por 29:59" — os itens ficam **bloqueados** para outros clientes | RF0031, RF0032, RN0031, RN0044 |
| 2 | Loja | **Iniciar Compra** → endereço *Casa* → cupom **PROMO-BEMVINDO** → cartão **VISA 4242** | Frete **R$ 30,90** (SP R$ 24,90 + 2 unidades extras × R$ 3,00); total **R$ 1.190,60**; o cartão cobra **R$ 1.140,60** (total − cupom); "Pagamento completo". Só **um** cupom promocional por compra (radio) | RF0033–RF0037, RN0033 |
| 3 | Loja | **Confirmar Pagamento** | **Pedido #3 — EM PROCESSAMENTO** | RF0038 |
| 4 | Admin → Pedidos | **Não clique em nada**: aguarde ≈ 10 s com o pedido #3 na tela | O **sistema valida o pagamento sozinho** (a "operadora" simulada aprova) e o pedido passa de EM PROCESSAMENTO a **APROVADA**, sem ação de ninguém. Aba **Estoque**: antes da aprovação Headset 40 e Teclado 30 (itens só bloqueados); depois, Headset **38** e Teclado **29** (baixa só na aprovação) | RN0037, RN0038, RN0028, RF0053 |
| 5 | Admin → Pedidos | **Despachar** → **Confirmar entrega** | EM TRANSPORTE → **ENTREGUE** (cada botão só aparece no status certo) | RF0039, RF0040, RN0039, RN0040 |
| 6 | Meus pedidos | Pedido #3 → **Solicitar troca** → marque o *Headset*, qtd **1**, motivo → **Enviar** | Pedido vira **EM TROCA**. A troca pode ser de **um item** (como aqui) ou da **compra toda** (botão "Selecionar todos os itens"). Só pedidos ENTREGUES mostram o botão (o #2, em transporte, não) | RF0041, RN0041, RN0043 |
| 7 | Admin → Trocas | Veja a solicitação (cliente, item, motivo) → **Autorizar** | **TROCA AUTORIZADA**; aviso "O cliente foi notificado" | RF0043, RF0042 |
| 8 | Meus pedidos | (a tela se atualiza sozinha) | **Notificação** "troca AUTORIZADA" | RN0046 |
| 9 | Admin → Trocas | **Sim, voltam** (itens retornam ao estoque) | **TROCADO**; cupom **TROCA-P3-S1 de R$ 349,90** gerado; aba Estoque: Headset 38→**39** | RF0044, RF0054, RN0042, RF0045 |
| 10 | Meus pedidos / Loja | Cupom aparece em "Meus cupons"; numa **nova compra** (1 Teclado) escolha **TROCA-P3-S1** + cartão | Cupom de R$ 349,90 + cartão de R$ 134,90 = R$ 484,80 | RF0037, RF0045 |

---

## 3. Fluxos alternativos (escolha os que couberem no tempo)

### A) Pagamento reprovado (E3 do CDU) — RN0038 / RN0028
1. Loja: 1 Headset → Iniciar Compra → endereço → **+ Cadastrar novo cartão**: número `4111 1111 1111 0000` (só dígitos: `4111111111110000`), VISA, titular, CVV `123` → **Salvar Cartão** (já vem selecionado) → **Confirmar Pagamento**.
2. Admin → Pedidos: o pedido aparece **EM PROCESSAMENTO**, com o aviso de que o pagamento está sendo validado pelo sistema. Sem clicar em nada, em ≈ 10 s ele passa a **REPROVADA**: mensagem "Cartão final 0000 recusado pela operadora"; estoque **não muda** (aba Estoque: os itens foram **desbloqueados** e continuam em estoque); sem botão de despachar.
3. Meus pedidos: o cliente vê o **motivo da reprovação**.
> A "operadora" é simulada: **cartão terminado em 0000 é recusado**; os demais, aprovados. Se usou cupom, ele **volta a ficar disponível**. Enquanto o pedido está EM PROCESSAMENTO, a quantidade do pedido fica **bloqueada** (outro cliente não a vê como disponível); a baixa só ocorre se o pagamento for aprovado.

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

### E) Frete conforme itens e endereço — RF0034
1. Loja: 1 Headset → Iniciar Compra → endereço *Casa* (SP): frete **R$ 24,90**.
2. **+ Cadastrar novo endereço** com estado **BA** (ou outro estado fora do Sudeste/Sul) e selecione-o: o frete passa a **R$ 39,90**. Com estado RJ/MG/ES/PR/SC/RS seria R$ 29,90.
3. Volte à Loja, some mais 2 unidades ao carrinho e repita: cada unidade a partir da 2ª soma **R$ 3,00** (3 unidades em SP = R$ 30,90).
> A regra é do grupo (o DRS não traz a fórmula) e está explicada em `JUSTIFICATIVA_FRETE.md`; o frete calculado é gravado no pedido.

### F) Troca da compra toda — RF0041
Com um pedido ENTREGUE de dois ou mais itens, Meus pedidos → **Solicitar troca** → **Selecionar todos os itens (trocar a compra inteira)** → motivo → **Enviar**. O pedido vira EM TROCA com todos os itens na solicitação. (A recusa de troca e o estorno financeiro não fazem parte do escopo.)

---

## 4. Perguntas que o professor pode fazer

| Pergunta | Resposta honesta |
|---|---|
| Como é calculado o frete? E por que essa fórmula? | **Tarifa do estado de entrega + R$ 3,00 por unidade a partir da 2ª — regra definida pelo grupo e documentada.** O RF0034 pede cálculo "com base nos itens e no endereço", mas não define a fórmula. O **endereço** entra pelo estado (SP R$ 24,90; RJ/MG/ES/PR/SC/RS R$ 29,90; demais R$ 39,90) e os **itens** pela quantidade de unidades. Não usamos peso, dimensões nem CEP de origem porque o módulo não tem esses dados. O cálculo roda no backend (`FreteService`), a tela só exibe o valor, e ele é gravado no pedido. **Admita a limitação:** é uma simplificação (uma tabela por região, sem transportadora). Argumentos completos em [`JUSTIFICATIVA_FRETE.md`](JUSTIFICATIVA_FRETE.md). |
| Existe login / perfil de administrador? | Não: o DRS não define autenticação. "Cliente" e "Administrador" são **telas separadas**; o cliente das telas de cliente vem da URL (`?clienteId=`). |
| A operadora de cartão é real? | Não, é **simulada** (`PagamentoGatewayService`): final **0000** é recusado — para mostrar APROVADA e REPROVADA de forma repetível. |
| Quem valida o pagamento? | **O sistema, automaticamente.** Ao finalizar, o pedido fica EM PROCESSAMENTO (RF0038) e um job (`PagamentoValidacaoJob`) valida a forma de pagamento (cupons + retorno da operadora simulada) e muda o status para APROVADA ou REPROVADA (RN0037/RN0038). Nenhum usuário aciona isso. O atraso é de 10 s, só para dar tempo de ver o status. |
| Quando o estoque sofre a baixa? | Só quando o pagamento é **aprovado** (RF0053/RN0028). Enquanto o pedido está EM PROCESSAMENTO os itens ficam bloqueados; se o pagamento for reprovado são desbloqueados e continuam em estoque. |
| E se o administrador recusar a troca? Há estorno financeiro? | **Não implementamos nenhum dos dois, por orientação do professor.** O administrador só autoriza a troca (RF0042); o fim do processo é o cupom de troca (RF0045). |
| Dá para trocar a compra inteira? | Sim: o cliente escolhe **um item ou todos** (botão "Selecionar todos os itens"). |
| Como garantem que os testes não dependem uns dos outros? | Cada teste começa com `POST /api/test-support/reset` (base zerada com os mesmos ids). Esse endpoint só existe com `app.test-support.enabled=true`. |
| Cobertura dos requisitos? | `docs/MATRIZ_RASTREABILIDADE.md` — todos os RF/RN do CDU02 têm teste Cypress; RNF0011 (tempo de resposta) tem o teste 37 e RNF0012 (log de escrita) tem teste JUnit (`LogDeEscritaIntegrationTest`). |
| O que o cliente vê se o item expirar? | Lista "itens retirados do carrinho", compra desabilitada até adicionar de novo (RNF0042). |
