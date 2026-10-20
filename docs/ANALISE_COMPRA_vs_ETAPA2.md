# Análise: `Compra` + `hardware-store-vendas-etapa2` — a junção é o caminho certo?

**Resposta curta:** sim, a versão final é a junção dos dois, mas **não por cópia simples** — cada pasta tinha a melhor versão de **partes diferentes** do projeto, e a `etapa2` sozinha **não compilava**.

## O que cada pasta continha

| Parte do projeto | `Compra` (`hardware-store-vendas-COMPLETO`) | `etapa2` (`hardware-store-clientes`) | Escolhida |
|---|---|---|---|
| Repositório Git | ✅ clone real de `tgsygqa/hs-compras` (3 commits, até 14/10), com `target/` compilado | ❌ sem `.git` | **Compra** (preserva o histórico) |
| Módulo **Cliente** (`ClienteService`, `ClienteController`, DTOs, `CorsConfig`, filtros do repositório) | ✅ completo | ❌ **ausente** (o próprio README da etapa2 diz que foi *reconstruído de memória* e que faltavam essas classes) | **Compra** |
| Enums/classes de Cliente (`Genero`, `TipoTelefone`, `Telefone`, `Bandeira`) | ✅ originais (`RESIDENCIAL`, `AMERICAN_EXPRESS`, `HIPERCARD`, colunas mapeadas) | ⚠️ recriados com valores diferentes (`FIXO`, `AMEX`, `OUTRO`…; `Telefone` sem mapeamento de colunas) | **Compra** |
| Módulo **Venda**, fluxo de sucesso (carrinho → `EM_PROCESSAMENTO`) | ✅ | ✅ | igual |
| Módulo **Venda**, CDU02 completo (`validarPagamento`, `despachar`, `confirmarEntrega`, `SolicitacaoTroca`, `ItemSolicitacaoTroca`, DTOs de troca, `PagamentoGatewayService`, RN0032) | ❌ | ✅ (camada de modelo e serviço) | **etapa2** |
| `CupomRepository` | ✅ com a consulta `buscarDisponiveisParaCliente` | ❌ sem esse método, mas `CupomService` o chama | **Compra** |
| `vendas.html` | ✅ | idêntico (só comentários a mais) | etapa2 (equivalente) |
| `V08.html` + suíte Cypress de Clientes (64 testes) | ✅ | idênticos | igual |
| `package.json`, `cypress.config.js`, `pom.xml` | ✅ | idênticos | igual |

## Provas (não é opinião)

- `mvn compile` na **`etapa2` sozinha** → **erro de compilação**: `CupomService.java:[26,31] cannot find symbol: buscarDisponiveisParaCliente(Long)`.
- `mvn compile` na **`Compra` sozinha** → compila (mas só tem o fluxo até `EM_PROCESSAMENTO`).
- `mvn compile` na **junção** (cliente + common + CorsConfig da Compra, `venda/` da etapa2, `CupomRepository` da Compra) → compila; depois disso, 22 testes de integração e 81 testes Cypress passam.

## O que faltava para o "caso de uso de vendas completo" (os passos 3 a 7 do plano de 7 etapas que a `etapa2` deixou pendentes)

| Etapa | Estado na `etapa2` | Feito agora |
|---|---|---|
| 3 — Bloqueio/expiração do carrinho (RN0044, RN0045, RNF0042) | só o campo `bloqueadoAte`, sem uso | ✅ bloqueio com prazo, contador, aviso aos 5 min, job de expiração, lista de itens expirados, readicionar/dispensar |
| 4 — Endpoints REST de despacho, entrega, troca | métodos de serviço sem controller | ✅ `PedidoController` ampliado, `TrocaController`, `NotificacaoController` |
| 5/6 — Telas do administrador e do cliente | não existiam | ✅ `admin.html`, `pedidos.html` (+ ajustes em `vendas.html`) |
| 7 — Cypress do fluxo completo | não existia | ✅ 37 testes em 7 arquivos: 27 dedicados (um por requisito do DRS) + 10 etapas do caso de uso completo (testes 22 a 31) |
| RN0032 (alerta e ajuste automático) | só lançava erro | ✅ ajusta a quantidade / remove o item e mostra o alerta |
| RN0046 (notificação de troca autorizada) | não existia | ✅ |
| Validação de pagamento "controlável" (E3 — REPROVADA) | gateway sempre aprovava | ✅ cartão final `0000` é recusado |

## Defeitos encontrados e corrigidos no caminho

1. **`POST /api/clientes/{id}/cartoes` e `/enderecos` devolviam `"id": null`** (bug já existente na `Compra`). Efeito: na tela de compra, o endereço/cartão cadastrado na hora não ficava selecionado e a compra falhava (RF0035/RF0036). Corrigido em `ClienteService` e coberto por teste JUnit e Cypress.
2. **`vendas.html` enviava a bandeira `AMEX`**, mas o backend só conhece `AMERICAN_EXPRESS` (e `HIPERCARD` não estava na lista). Corrigido.
3. **`etapa2` não compilava** (`CupomRepository`, acima).
4. Pedidos **reprovados** deixavam o cupom usado "queimado" — agora o cupom volta a ficar disponível (decisão assumida; o DRS não trata).

## Decisões de configuração que você deve conhecer

- **Banco padrão: H2 em memória** (antes: PostgreSQL obrigatório). O PostgreSQL continua disponível no perfil `postgres`. Motivo: no notebook não é preciso instalar nem configurar nada, e a base nasce limpa a cada execução (importante para testes repetíveis). Isso também combina com o Plano de Testes, que cita H2 em memória.
- **Maven Wrapper** (`backend/mvnw`): o notebook só precisa do JDK.
- **Endpoints `/api/test-support/**`** (reset da base etc.): existem só com `app.test-support.enabled=true` (padrão no H2, desligado no perfil `postgres`).
- `.gitattributes` para normalizar fim de linha (o `.git/config` do seu clone indica Git for Windows: `filemode=false`, `ignorecase=true`).

## Ajustes feitos depois, para seguir o enunciado do professor

| Ponto do enunciado | O que havia | O que foi feito |
|---|---|---|
| **Validação do pagamento é automática** (APROVADA/REPROVADA, RN0037/RN0038) | o administrador clicava em "Validar pagamento" | removido o botão e o endpoint; o sistema valida sozinho (`PagamentoValidacaoJob`, atraso de 10 s configurável), e as telas se atualizam sozinhas |
| **Baixa em estoque só para compras aprovadas; itens de compras não aprovadas desbloqueados e mantidos em estoque** (RF0053, RN0028) | a baixa já era só na aprovação, mas os itens de um pedido EM PROCESSAMENTO não ficavam bloqueados | itens de pedidos EM PROCESSAMENTO ficam bloqueados (não aparecem como disponíveis para outros clientes); se REPROVADA, voltam a ficar disponíveis |
| **Frete "com base nos itens e no endereço", com regra definida e documentada** (RF0034) | valor fixo de R$ 24,90 | `FreteService`: tarifa do estado de entrega + R$ 3,00 por unidade a partir da 2ª; calculado no backend; documentado em `JUSTIFICATIVA_FRETE.md` |
| **Troca de "um item ou toda a compra"** (RF0041) | só marcação item a item | botão "Selecionar todos os itens"; teste 16 cobre os dois casos |
| **Recusa de troca e estorno financeiro não devem ser implementados** | havia "recusar troca" (endpoint, tela, teste JUnit) | removido por completo; o DRS RF0044 (itens voltam ou não ao estoque) foi mantido |
| **Requisitos transversais RNF0011 e RNF0012** | sem teste automatizado | teste Cypress 37 (tempo de resposta ≤ 1 s) e teste JUnit `LogDeEscritaIntegrationTest` |
