# Passo a passo — levar o projeto para o notebook e rodar tudo

Este guia leva você de **"notebook sem nada"** até **"testes Cypress rodando"**. Os comandos de exemplo são para **Windows** (PowerShell ou Prompt de Comando); macOS/Linux têm notas ao lado.

> **Resumo em 5 comandos** (depois de instalar Git, JDK 17 e Node.js — seção 1):
> ```
> git clone https://github.com/tgsygqa/hs-vendas-completo.git
> cd hs-vendas-completo
> npm install
> npm run test:vendas          # roda os 81 testes Cypress de Vendas sozinho
> npm run test:vendas:open     # ou abre a interface do Cypress (para apresentar)
> ```

---

## 1. O que instalar no notebook (uma vez só)

| Ferramenta | Versão | Para quê | Windows (`winget`) | Download manual |
|---|---|---|---|---|
| **Git** | qualquer recente | baixar o código do GitHub | `winget install Git.Git` | <https://git-scm.com> |
| **JDK** | **17** (ou 21) | rodar o backend Spring Boot | `winget install EclipseAdoptium.Temurin.17.JDK` | <https://adoptium.net> |
| **Node.js** | **LTS 20 ou 22** | frontend estático e Cypress | `winget install OpenJS.NodeJS.LTS` | <https://nodejs.org> |
| **VS Code** | qualquer | editor | `winget install Microsoft.VisualStudioCode` | <https://code.visualstudio.com> |

**Não precisa instalar:** Maven (vem junto, pelo *Maven Wrapper*), PostgreSQL (o banco padrão é o H2, em memória) nem o Cypress (o `npm install` baixa).

- No instalador do **Temurin**, na tela *Custom Setup*, marque **"Set JAVA_HOME variable"** e **"Add to PATH"**.
- **Feche e reabra** o VS Code/terminal depois de instalar, para o PATH atualizar.
- Use **JDK 17 a 21**. JDK 22+ quebra o Lombok desta versão do Spring Boot.

**Windows 10 — três cuidados que evitam 90% dos problemas:**

1. **`winget` pode não existir** no seu Windows 10 (ele vem com o *Instalador de Aplicativo* da Microsoft Store, nem sempre atualizado). Se `winget` não for reconhecido, **baixe pelos links da tabela** acima — o resultado é o mesmo.
2. **PowerShell bloqueia o `npm`.** O Windows 10 vem com a política de scripts *Restricted*, e o primeiro `npm install` costuma falhar com *"npm.ps1 não pode ser carregado porque a execução de scripts foi desabilitada"*. Solução (uma vez): abra o PowerShell e rode `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` (responda `S`). Ou use o **Prompt de Comando** como terminal do VS Code.
3. **Pasta do projeto: use um caminho curto e fora do OneDrive**, por exemplo `C:\projetos\hardware-store`. Em *Área de Trabalho* / *Documentos* o OneDrive costuma estar sincronizando e atrapalha o `node_modules` e o `backend\target`; e caminhos longos estouram o limite de 260 caracteres do Windows.

**Confira** (num terminal novo):

```
git --version
java -version      # deve mostrar 17.x ou 21.x
node -v            # v20.x ou v22.x
npm -v
```

*macOS:* `brew install git openjdk@17 node` · *Ubuntu/Debian:* `sudo apt install git openjdk-17-jdk nodejs npm` (se o Node do apt for antigo, use <https://nodejs.org>).

**Extensões do VS Code (opcionais)** — ao abrir a pasta, o VS Code sugere as recomendadas (*Extension Pack for Java*, *Spring Boot*). Elas ajudam a ler o código; **não são necessárias para rodar**.

---

## 2. Levar o código para o notebook pelo GitHub

A versão final do código (histórico completo, inclusive os commits anteriores do grupo) está publicada em:

**<https://github.com/tgsygqa/hs-vendas-completo>** (branch `main`)

A versão anterior continua em `tgsygqa/hs-compras` (a `main` de lá não foi alterada; existe só uma branch `vendas-completo` com a mesma versão do código).

### 2.1 No notebook (primeira vez)

```
git clone https://github.com/tgsygqa/hs-vendas-completo.git
cd hs-vendas-completo
code .                            # abre no VS Code
```

Se o repositório for **privado**, o Git pede o login do GitHub na primeira vez (abre o navegador) — entre com a conta dona do repositório ou uma que tenha acesso. O notebook precisa de internet nesta etapa e no `npm install`.

### 2.2 Depois, para enviar mudanças que você fizer

```
git status                        # mostra os arquivos alterados/novos
git add -A
git commit -m "descreva a mudança"
git push
```

*(O VS Code faz o mesmo pela aba **Controle do Código-Fonte**: escreva a mensagem, **Commit**, **Sync Changes**.)* Se o `git commit` reclamar de identidade, configure uma vez: `git config --global user.name "Seu Nome"` e `git config --global user.email "seu@email.com"`.

### 2.3 Se preferir usar a pasta recebida em vez de clonar

A pasta `hardware-store` do pacote `.zip` é um clone do repositório antigo (`hs-compras`). Para usá-la com o repositório novo: `git remote set-url origin https://github.com/tgsygqa/hs-vendas-completo.git`. Mas o jeito mais simples é clonar de novo (2.1).

---

## 3. Preparar o ambiente (no notebook)

No terminal integrado do VS Code (**Terminal → Novo Terminal**), na pasta do projeto:

```
npm install
```

Isso baixa o Cypress (o binário tem ~250 MB, então **demora alguns minutos na primeira vez**) e o servidor estático (`http-server`). Quando terminar, confira:

```
npx cypress verify          # deve terminar com "✔ Verified Cypress!"
```

> **Windows + PowerShell:** se aparecer *"a execução de scripts foi desabilitada neste sistema"*, rode uma vez `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`, **ou** troque o terminal do VS Code para **Command Prompt** (seta ao lado do `+` do terminal).

---

## 4. Conferir que o backend está saudável (opcional, 1 minuto)

```
npm run test:backend
```

Na **primeira vez** o Maven Wrapper baixa o Maven e as dependências (2–5 min). Resultado esperado no fim:

```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

São testes de integração JUnit da API (fluxo completo + regras RN0031–RN0046) — rodam em segundos, sem navegador.

---

## 5. Rodar a aplicação **manualmente**

Abra **dois terminais** (no VS Code: Terminal → Novo Terminal, e o botão **Dividir terminal**):

| Terminal | Comando | Pronto quando |
|---|---|---|
| 1 — backend | `npm run start:back` | aparece `Started ClienteServiceApplication in … seconds` (porta **8080**) |
| 2 — frontend | `npm run start:front` | o terminal **fica em silêncio** (é o normal; no máximo um aviso `DeprecationWarning`, inofensivo) — confirme abrindo <http://localhost:5500> |

*(Equivalentes sem o npm: `cd backend` + `mvnw.cmd spring-boot:run` (Windows) ou `./mvnw spring-boot:run` (macOS/Linux); e `npx http-server frontend -p 5500 -c-1`.)*

Abra no navegador **<http://localhost:5500>** e escolha a tela:

| Tela | Endereço | Quem usa |
|---|---|---|
| Loja | <http://localhost:5500/vendas.html> | cliente |
| Meus pedidos | <http://localhost:5500/pedidos.html> | cliente |
| Administração | <http://localhost:5500/admin.html> | administrador |
| Gestão de Clientes | <http://localhost:5500/V08.html> | CRUD de clientes |
| Console do banco (H2) | <http://localhost:8080/h2-console> | JDBC URL `jdbc:h2:mem:hardwarestore`, usuário `sa`, senha vazia |

Para **parar**, `Ctrl+C` em cada terminal. Como o banco é em memória, **ao reiniciar o backend tudo volta ao estado inicial**. Sem reiniciar, rode `npm run demo:reset`.

O roteiro completo, clique a clique, está em [`ROTEIRO_APRESENTACAO.md`](ROTEIRO_APRESENTACAO.md).

---

## 6. Rodar os testes **automatizados** (Cypress)

### 6.1 Tudo sozinho, sem janela (headless)

```
npm run test:vendas          # 81 testes do caso de uso de Vendas (sobe backend e frontend sozinho)
npm run test:cliente         # 64 testes do módulo de Clientes (só o frontend)
npm run test:e2e             # os dois
```

O comando (`scripts/run-with-servers.js`) sobe o backend e o frontend, espera ficarem prontos, roda o Cypress e **desliga tudo no fim** (inclusive se você apertar `Ctrl+C`). Na **primeira** vez o backend demora mais (download do Maven). Se o terminal parecer parado por 2–3 minutos, é o Maven baixando — espere.

Resultado esperado no fim (algo como):

```
  (Run Finished)
       Spec                                  Tests  Passing  Failing …
  ✔  01-carrinho.cy.js                          11       11        0
  ✔  02-bloqueio-carrinho.cy.js                 11       11        0
  ✔  03-checkout-pagamento.cy.js                18       18        0
  ✔  04-aprovacao-despacho-entrega.cy.js        10       10        0
  ✔  05-troca.cy.js                             17       17        0
  ✔  06-caso-de-uso-completo.cy.js               1        1        0
  ✔  07-regras-de-negocio-api.cy.js             13       13        0
  ✔  All specs passed!                          81       81        0
```

Se algum teste falhar, o Cypress salva uma imagem em `cypress/screenshots/`.

### 6.2 Com a interface do Cypress (para mostrar ao vivo)

```
npm run test:vendas:open
```

Sobe backend + frontend e abre o Cypress. Na janela: **E2E Testing → escolha o navegador (Electron ou Chrome) → Start E2E Testing** → clique no arquivo desejado. Sugestão para a apresentação: **`06-caso-de-uso-completo.cy.js`** — um único teste que percorre o CDU02 inteiro pela tela (~20 s). Ao fechar a janela do Cypress, o backend e o frontend são desligados.

### 6.3 Passo a passo **manual** dos testes (sem os atalhos)

Se preferir controlar cada peça (3 terminais):

```
# terminal 1 — backend                       # terminal 2 — frontend
npm run start:back                           npm run start:front

# terminal 3 — Cypress (depois que os dois acima estiverem no ar)
npx cypress run --spec "cypress/e2e/vendas/**/*.cy.js"        # headless
npx cypress open                                              # interface
```

Um teste isolado: `npx cypress run --spec "cypress/e2e/vendas/05-troca.cy.js"`.

### 6.4 Como os testes ficam independentes

Cada teste de Vendas começa chamando `POST /api/test-support/reset`, que apaga a base e recarrega os dados iniciais (cliente 1 = João Silva, 5 produtos, pedidos #1 e #2, 4 cupons). Por isso não importa a ordem nem se você mexeu na tela antes. Esse endpoint só existe com `app.test-support.enabled=true` (padrão no H2).

---

## 7. Problemas comuns

| Sintoma | Causa e solução |
|---|---|
| `'npm' não é reconhecido` / `'node' …` | Node não instalado ou terminal aberto antes da instalação → instale e **reabra o VS Code**. |
| `execução de scripts desabilitada` (PowerShell) | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` ou use o **Prompt de Comando**. |
| `java` não reconhecido / `JAVA_HOME` | Instale o JDK 17 marcando *Set JAVA_HOME* e *Add to PATH*; reabra o terminal. Confira `java -version`. |
| Erro de compilação do Lombok / `Unsupported class file` / `ExceptionInInitializerError` | JDK novo demais (22+). Use JDK **17 ou 21**. |
| `Port 8080 was already in use` / `EADDRINUSE 5500` | Algo já usa a porta (talvez o backend de um teste anterior). Windows: `netstat -ano \| findstr :8080` e `taskkill /PID <número> /F`. macOS/Linux: `lsof -i :8080` e `kill <pid>`. |
| `npm run test:vendas` diz *"Já existe um backend respondendo na porta 8080"* (ou 5500) | Sobrou um servidor de uma execução anterior (ou você deixou `npm run start:back` aberto). Feche-o com `Ctrl+C` no terminal dele — ou, se não achar, `taskkill /F /IM java.exe` (Windows) / `pkill -f spring-boot` (macOS/Linux). O comando recusa de propósito, para os testes não rodarem contra um backend em outro perfil. |
| Terminal "parado" por minutos na primeira execução | O Maven está baixando dependências (2–5 min). Espere. |
| Firewall do Windows pergunta sobre o Java/Node | Clique em **Permitir** (rede privada). |
| `npm install` trava no Cypress / erro de download | Firewall ou proxy bloqueando `download.cypress.io`. Libere o domínio ou, atrás de proxy, defina `HTTPS_PROXY` antes; depois `npx cypress install`. Rede da faculdade costuma bloquear — teste com o celular como roteador. |
| Cypress diz `ECONNREFUSED 127.0.0.1:8080` | O backend não está no ar. Com `test:vendas`/`test:vendas:open` ele sobe sozinho; com `cy:open`/`cy:run` suba antes `npm run start:back` e `npm run start:front`. |
| Tela da loja mostra "Não foi possível carregar os dados da API" | Backend fora do ar ou ainda subindo (leva ~10 s). |
| Os testes CTV023/CTV024 (aviso de expiração) falham | O backend foi iniciado com `start:back:demo` (prazos curtos). Para os testes use o padrão `npm run start:back` — ou `npm run test:vendas`, que já sobe o certo. |
| Teste de Vendas falha logo no `beforeEach` com erro 404 no `reset` | O backend foi iniciado no perfil `postgres` (que desliga o endpoint de teste). Use o padrão `npm run start:back`. |
| Em macOS/Linux: `mvnw: Permission denied` | O `npm run …` já usa `sh mvnw`. Se rodar a mão: `sh ./mvnw spring-boot:run` ou `chmod +x backend/mvnw`. |
| Quero PostgreSQL em vez do H2 | `npm run start:back:postgres` (precisa de um Postgres em `localhost:5432`, banco/usuário/senha `hardwarestore`). O Codespace já instala. |

### No GitHub Codespaces

Abra o repositório no Codespace; o `.devcontainer` instala Java, Node e as bibliotecas do Cypress e roda `npm install`. Depois é só `npm run test:vendas`. As telas detectam o Codespace sozinhas (porta 5500 do front → 8080 da API); deixe a porta **8080 pública** (aba *Ports*) para o navegador conseguir chamar a API.
