# Hardware Store — Gestão de Clientes

CRUD de Cliente conforme o DRS_LES_2_2026 (RF0021 a RF0028, RN0021 a RN0027, RNF0031 a RNF0035), com três partes independentes na mesma pasta: frontend, backend (Spring Boot) e testes automatizados de interface (Cypress).

## Estrutura

```
hardware-store-clientes/
├── frontend/                     Interface web (HTML/JS puro)
│   ├── V08.html                  Tela de Gestão de Clientes
│   └── index.html                Redireciona para V08.html
├── backend/                      API REST (Spring Boot 3.3 · Java 17 · PostgreSQL)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/hardwarestore/
│       │   ├── cliente/
│       │   │   ├── ClienteServiceApplication.java
│       │   │   ├── config/       CorsConfig, DataSeeder
│       │   │   ├── controller/   ClienteController
│       │   │   ├── dto/          DTOs de entrada e saída
│       │   │   ├── model/        Entidades JPA e enums
│       │   │   ├── repository/   ClienteRepository
│       │   │   └── service/      ClienteService (regras de negócio)
│       │   ├── venda/
│       │   │   ├── config/       ProdutoSeeder, CupomSeeder
│       │   │   ├── controller/   ProdutoController, CarrinhoController, PedidoController, CupomController
│       │   │   ├── dto/          DTOs de entrada e saída
│       │   │   ├── model/        Produto, Carrinho/ItemCarrinho, Pedido/ItemPedido/PagamentoPedido, Cupom
│       │   │   ├── repository/   ProdutoRepository, CarrinhoRepository, PedidoRepository, CupomRepository
│       │   │   └── service/      ProdutoService, CarrinhoService, PedidoService, CupomService
│       │   └── common/
│       │       ├── aspect/       LogDeEscritaAspect (RNF0012)
│       │       └── exception/    GlobalExceptionHandler e exceções de negócio
│       └── resources/application.properties
├── cypress/
│   ├── e2e/cliente/cliente-crud.cy.js     Suíte E2E (64 testes)
│   └── support/e2e.js
├── cypress.config.js
├── package.json                  Scripts npm (front, back, Cypress)
├── pom.xml                       POM agregador (IntelliJ/VS Code detectam o backend)
├── .devcontainer/                Ambiente do Codespace (Java 17, Maven, Node 20, libs do Cypress)
└── .vscode/                      Tasks e extensões recomendadas
```

## Como rodar no Codespace

Abra esta pasta no Codespace (VS Code ou IntelliJ). Se o Codespace pedir para reconstruir o container, aceite: o `.devcontainer` instala Java 17, Maven, Node 20, as bibliotecas do Cypress e roda `npm install`.

Se preferir manter o Codespace atual, basta rodar uma vez, na raiz:

```bash
npm install
```

### Testes Cypress (o que a apresentação exige)

Um comando sobe o frontend na porta 5500 e roda toda a suíte em modo headless:

```bash
npm run test:e2e
```

Para acompanhar os testes na interface do Cypress, use dois terminais:

```bash
npm run start:front     # terminal 1 — http://localhost:5500
npm run cy:open         # terminal 2
```

### Backend

```bash
npm run start:back      # http://localhost:8080
```

Ou `mvn -f backend/pom.xml spring-boot:run`. No VS Code também há tasks prontas em *Terminal → Run Task*.

O banco é PostgreSQL. No Codespace, o `.devcontainer` instala o Postgres e cria o banco `hardwarestore` (usuário e senha `hardwarestore`) automaticamente; fora do Codespace, crie esse banco e usuário a mão, ou ajuste `backend/src/main/resources/application.properties`. Para inspecionar os dados, use `psql -h localhost -U hardwarestore -d hardwarestore` ou uma extensão de banco de dados do VS Code. A cada subida, o `ProdutoSeeder` cria produtos de exemplo e o `DataSeeder` cria o cliente João Silva com endereço, cartão e dois pedidos, o mesmo mock do frontend.

Endpoints (`/api/clientes`): `POST /`, `GET /` (filtros nome, cpf, email, ativo), `GET /{id}`, `PUT /{id}`, `PATCH /{id}/inativar`, `PATCH /{id}/reativar`, `DELETE /{id}`, `GET /{id}/transacoes`, `POST /{id}/enderecos`, `DELETE /{id}/enderecos/{enderecoId}`, `POST /{id}/cartoes`, `PATCH /{id}/cartoes/{cartaoId}/preferencial`, `DELETE /{id}/cartoes/{cartaoId}`, `PATCH /{id}/senha`.

### Módulo de Vendas (fluxo de sucesso - entrega de 06/10)

Implementa exatamente o escopo definido pelo professor para a apresentação de 06/10: do carrinho até a finalização da compra, parando no status `EM_PROCESSAMENTO`. **Não** inclui validação de pagamento, aprovação/reprovação, despacho, confirmação de entrega, baixa em estoque, nem bloqueio/expiração de item no carrinho - tudo isso está explicitamente fora do escopo desta fase (instruções do professor) e fica para a entrega de 20/10, com o caso de uso completo.

Endpoints:
- `GET /api/produtos` - lista produtos disponíveis.
- `GET /api/clientes/{id}/carrinho`, `POST /api/clientes/{id}/carrinho/itens`, `PATCH /api/clientes/{id}/carrinho/itens/{itemId}`, `DELETE /api/clientes/{id}/carrinho/itens/{itemId}` - RF0031/RF0032, com RN0031 (estoque) validado em cada adição/alteração.
- `GET /api/clientes/{id}/cupons` - cupons (promocionais de uso geral + de troca do cliente) disponíveis para pagamento.
- `POST /api/clientes/{id}/pedidos` - finaliza a compra (RF0033 a RF0038): recebe `enderecoId`, uma lista `cartoes` (cada item com `cartaoId` e `valor`) e uma lista opcional `cupomIds`. Valida RN0033 (um promocional por compra), RN0034 (mínimo R$ 10,00 por cartão quando há mais de um, sem cupom combinado), RN0035 (a exigência dos R$ 10,00 é dispensada quando há cupom combinado) e RN0036 (cupom de troco quando os cupons superam o valor da compra, sem permitir cupom desnecessário). Devolve o pedido já criado com status `EM_PROCESSAMENTO`.
- `GET /api/pedidos`, `GET /api/pedidos/{id}` - consulta.

Endereço e cartão novos cadastrados **durante** a compra (RF0035/RF0036) não têm endpoint próprio: o front chama primeiro `POST /api/clientes/{id}/enderecos` ou `/cartoes` (que já incorporam ao perfil), pega o id devolvido, e só então finaliza a compra com esse id - os mesmos endpoints do módulo Cliente, reaproveitados.

RNF0011 (tempo de resposta) não exige código específico - nenhuma consulta do projeto faz nada custoso o bastante para se aproximar de 1 segundo. RNF0012 (log das operações de escrita) é cuidado por um aspecto (`common/aspect/LogDeEscritaAspect`), que intercepta automaticamente todo método `@Transactional` dos módulos de Cliente e Vendas - nenhum service precisa chamar log manualmente.

## Estado atual da integração

- O frontend (`V08.html`) guarda os dados em memória e **ainda não chama a API**. Os testes Cypress do CRUD de Cliente validam o frontend sozinho, por isso não precisam do backend no ar. O módulo de Vendas ainda não tem frontend (próxima fase).
- O backend cobre RNF0033 (senha em hash BCrypt), RNF0031/RNF0032 (senha forte e confirmação), RNF0012 (log de escrita) e as regras de negócio de RF0021 a RF0028 e RF0031 a RF0038.
- Diferenças do backend em relação ao frontend atual do Cliente: não existe endpoint para alterar um endereço (RNF0034), a listagem não filtra por código, telefone, gênero, data de nascimento e ranking (RF0024), e o ranking (RN0027) nasce em 0 sem cálculo.
- O backend valida o CPF com dígitos verificadores; os CPFs fictícios da suíte Cypress (`111.222.333-XX`) seriam recusados caso o frontend fosse ligado a ele.

## Observações

- Use JDK 17 a 21. O Lombok da versão do Spring Boot usada aqui não suporta JDKs muito novos (a pasta `.idea` original apontava para o JDK 26, e por isso ela não foi incluída).
- Na primeira execução o Maven baixa as dependências; leva alguns minutos.
