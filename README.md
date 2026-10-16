# Hardware Store — Clientes e Vendas

E-commerce de hardware (DRS_LES_2_2026). Este repositório reúne os dois casos de uso do projeto, com frontend, backend e testes automatizados:

| Módulo | O que cobre | Requisitos |
|---|---|---|
| **Gestão de Clientes** | CRUD de cliente, endereços, cartões, senha, transações | RF0021–RF0028, RN0021–RN0027, RNF0031–RNF0035 |
| **Vendas (CDU02 completo)** | carrinho → pagamento → aprovação → despacho → entrega → troca → cupom | RF0031–RF0045, RF0053, RF0054, RN0031–RN0046, RNF0042 |

> **Novo aqui? Comece por [`docs/GUIA_PASSO_A_PASSO.md`](docs/GUIA_PASSO_A_PASSO.md)** (instalação no notebook, GitHub, VS Code, Cypress e execução manual).
> Para apresentar: [`docs/ROTEIRO_APRESENTACAO.md`](docs/ROTEIRO_APRESENTACAO.md). Rastreabilidade RF/RN → testes: [`docs/MATRIZ_RASTREABILIDADE.md`](docs/MATRIZ_RASTREABILIDADE.md).

## Início rápido

Pré-requisitos: **Git, JDK 17 ou 21, Node.js 20+ (LTS)** — o Maven vem embutido (Maven Wrapper) e o banco é o H2 em memória: **não precisa instalar Maven nem PostgreSQL**.

```bash
npm install                 # 1x: baixa o Cypress, o servidor estático etc.

npm run start:back          # terminal 1 — API em http://localhost:8080
npm run start:front         # terminal 2 — telas em http://localhost:5600
```

Abra <http://localhost:5600>. Telas: **Loja** (`vendas.html`), **Meus pedidos** (`pedidos.html`), **Administração** (`admin.html`) e **Clientes** (`V08.html`).

## Testes automatizados

| Comando | O que faz |
|---|---|
| `npm run test:vendas` | Sobe backend + frontend sozinho e roda os **27 testes Cypress de Vendas** (headless) — um por requisito do DRS |
| `npm run test:cliente` | Sobe o frontend e roda os **64 testes Cypress de Clientes** (não precisa do backend) |
| `npm run test:e2e` | Tudo: Clientes + Vendas |
| `npm run test:lista` | Lista os arquivos de teste e quantos testes cada um tem |
| `npm run test:vendas -- troca` | **Só um arquivo**: o que vem depois do `--` filtra pelo nome (`troca`, `05`, `caso-de-uso`…); aceita vários: `-- 05 06` |
| `npm run test:vendas:open` | Sobe backend + frontend e abre a **interface do Cypress** (bom para apresentar ao vivo) |
| `npm run test:vendas:ao-vivo` | Roda o caso de uso completo (CTV022) num **Edge visível** (Chrome: `-- --browser chrome`), sem a janela inicial do Cypress — plano B se ela abrir em branco |
| `npm run test:backend` | **22 testes de integração JUnit** da API (RN0031–RN0046, fluxo completo) |
| `npm run cy:open` / `cy:run` | Cypress direto (backend e frontend já precisam estar no ar) |

### Atalhos para a demonstração

| Comando | O que faz |
|---|---|
| `npm run start:back:demo` | Backend com prazos de bloqueio **curtos** (3 min, aviso aos 2 min) para mostrar a expiração do carrinho ao vivo. **Só para demonstrar** — os testes Cypress esperam o perfil padrão (`start:back`) |
| `npm run demo:reset` | Volta a base ao estado inicial sem reiniciar o backend |
| `npm run demo:cliente2` | Cadastra a Maria Souza (id 2) — para mostrar o bloqueio **entre clientes** (RN0044) |
| `npm run demo:estoque -- 3 2` | Deixa o produto 3 com 2 unidades — para mostrar o ajuste de estoque da RN0032 |
| `npm run docs:matriz` | Regenera `docs/MATRIZ_RASTREABILIDADE.md` a partir dos títulos dos testes |

Cada teste de Vendas começa com a base zerada (`POST /api/test-support/reset`), então eles são independentes e podem rodar em qualquer ordem.

## Estrutura

```
hardware-store/
├── frontend/                      HTML/JS puro, sem build
│   ├── index.html                 menu das telas
│   ├── vendas.html                Loja: carrinho, bloqueio/contador, checkout, confirmação
│   ├── pedidos.html               Meus pedidos: status, solicitar troca, notificações, cupons
│   ├── admin.html                 Administração: pagamento, despacho, entrega, trocas, estoque
│   ├── V08.html                   Gestão de Clientes (CRUD; dados em memória, sem API)
│   └── shared.css / shared.js     estilo e utilitários das telas de Vendas
├── backend/                       Spring Boot 3.3 · Java 17+ · H2 (padrão) ou PostgreSQL
│   ├── mvnw / mvnw.cmd            Maven Wrapper (não precisa do Maven instalado)
│   └── src/main/java/com/hardwarestore/
│       ├── cliente/               módulo de Clientes
│       ├── venda/                 módulo de Vendas (controller / service / model / dto / repository / config)
│       └── common/                exceções, log de escrita (RNF0012) e suporte a testes
├── cypress/
│   ├── e2e/cliente/               suíte de Clientes (64 testes)
│   ├── e2e/vendas/                suíte de Vendas (27 testes, 7 arquivos)
│   └── support/                   comandos e helpers (reset, montagem de cenário via API)
├── scripts/mvn.js                 roda o Maven Wrapper em qualquer sistema operacional
├── docs/                          guias e matriz de rastreabilidade
└── .devcontainer / .vscode        Codespaces e tarefas do VS Code
```

## O caso de uso de Vendas (CDU02)

```
Cliente                         Administrador                      Status do pedido
────────────────────────────────────────────────────────────────────────────────────
monta o carrinho  ──(RN0044: itens bloqueados por 30 min, aviso aos 5 min)
escolhe endereço, cartões e cupom
finaliza a compra ───────────────────────────────────────────────▶ EM PROCESSAMENTO
                                valida o pagamento ──▶ operadora   APROVADA  (baixa em estoque)
                                                                   REPROVADA (estoque preservado)
                                despacha ────────────────────────▶ EM TRANSPORTE
                                confirma a entrega ──────────────▶ ENTREGUE
solicita a troca (itens + motivo) ───────────────────────────────▶ EM TROCA
                                autoriza (ou recusa, com motivo) ▶ TROCA AUTORIZADA  → cliente notificado (RN0046)
                                confirma recebimento ────────────▶ TROCADO
                                 (itens voltam ou não ao estoque; gera o cupom de troca — RF0045)
usa o cupom de troca numa nova compra
```

### Regras e decisões de implementação

- **Operadora de cartão (simulada).** `PagamentoGatewayService` aprova tudo, **exceto** compras com algum cartão terminado em `0000` — assim dá para mostrar o caminho REPROVADA de forma repetível (cadastre um cartão `4111 1111 1111 0000`). Numa compra reprovada o estoque não é tocado (RN0028) e os cupons usados voltam a ficar disponíveis.
- **Quando o pagamento é validado.** Depois de finalizada, a compra fica `EM PROCESSAMENTO` (RF0038); o administrador aciona **Validar pagamento** no painel (simula o retorno da operadora).
- **Bloqueio de carrinho (RN0044/RN0045/RNF0042).** O prazo (padrão 30 min, `app.carrinho.bloqueio-minutos`) é renovado a cada item incluído. O estoque que está bloqueado em carrinhos de outros clientes some da disponibilidade deles. Vencido o prazo, o item sai do carrinho e continua listado como *expirado*; o botão de compra fica desabilitado até o cliente adicioná-lo de novo.
- **RN0032.** Na finalização o estoque é conferido de novo: a quantidade é ajustada automaticamente (ou o item sai do carrinho) e o cliente recebe o alerta.
- **Frete.** Valor fixo de R$ 24,90 por compra — escolha do grupo, já que o DRS (RF0034) não define a fórmula. A justificativa para apresentar está em [`docs/JUSTIFICATIVA_FRETE.md`](docs/JUSTIFICATIVA_FRETE.md).
- **Troca recusada.** O DRS só descreve “autorizar”; a recusa (com motivo, pedido volta a `ENTREGUE`) é um caminho assumido.
- **Sem login.** O DRS não define autenticação: “cliente” e “administrador” são telas diferentes. O cliente das telas de cliente vem da URL (`?clienteId=2`; padrão: João Silva, id 1).
- **Endereço do pedido** é uma referência ao endereço do cliente (não um instantâneo) — simplificação conhecida.

## Banco de dados

Padrão: **H2 em memória** — nasce a cada subida do backend já com os dados de demonstração (5 produtos, o cliente João Silva com endereço e cartão, pedidos #1 `ENTREGUE` e #2 `EM TRANSPORTE`, 4 cupons). Para inspecionar: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:hardwarestore`, usuário `sa`, senha vazia).

PostgreSQL (opcional): `npm run start:back:postgres` (usa `localhost:5432`, banco/usuário/senha `hardwarestore`; o `.devcontainer` do Codespace já instala e cria). O perfil `postgres` recria o esquema a cada subida porque os seeders não são idempotentes.

## API (resumo)

| Área | Endpoints |
|---|---|
| Catálogo | `GET /api/produtos?clienteId=` |
| Carrinho | `GET /api/clientes/{id}/carrinho` · `POST …/carrinho/itens` · `PATCH …/itens/{itemId}` · `DELETE …/itens/{itemId}` · `POST …/expirados/{id}/readicionar` · `DELETE …/expirados/{id}` |
| Compra | `POST /api/clientes/{id}/pedidos` · `GET /api/clientes/{id}/pedidos` · `GET /api/clientes/{id}/cupons` |
| Administração | `GET /api/pedidos?status=` · `PATCH /api/pedidos/{id}/validar-pagamento` · `…/despachar` · `…/confirmar-entrega` |
| Troca | `POST /api/pedidos/{id}/trocas` · `GET /api/trocas` · `PATCH /api/trocas/{id}/autorizar` · `…/recusar` · `…/confirmar-recebimento` |
| Notificações | `GET /api/clientes/{id}/notificacoes` · `PATCH …/{id}/lida` |
| Clientes | `/api/clientes` (CRUD, endereços, cartões, senha, transações) |
| Suporte a testes | `POST /api/test-support/reset` e afins — só existem com `app.test-support.enabled=true` (padrão no H2; **desligado** no perfil `postgres`) |

Erros de regra de negócio voltam como **409**, validação como **400**, não encontrado como **404**, sempre com `{"mensagem": "..."}`.

## Observações

- Use **JDK 17 a 21** (o Lombok desta versão do Spring Boot não suporta JDKs muito novos).
- Na primeira execução o Maven baixa as dependências (alguns minutos) e o `npm install` baixa o binário do Cypress (~250 MB).
- RNF0012 (log de escrita): o aspecto `LogDeEscritaAspect` registra, no terminal do backend, toda operação de escrita (data/hora, operação, usuário/1º argumento, resultado).
- O backend valida o CPF com dígitos verificadores; os CPFs fictícios da suíte de Clientes (`111.222.333-XX`) só valem no front, que guarda os dados em memória.
