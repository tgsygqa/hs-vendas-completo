// ============================================================================
// Constantes e passos reutilizáveis (pela INTERFACE) da suíte de Vendas.
// Importado pelos specs: import { ... } from '../../support/helpers';
// ============================================================================

// Massa de dados criada pelos seeders (ver backend: ProdutoSeeder/DataSeeder/CupomSeeder).
export const PRODUTO = { RTX: 1, RYZEN: 2, HEADSET: 3, TECLADO: 4, SSD: 5 };
// RF0034 — o frete depende do endereço (UF) e dos itens (unidades): tarifa da região + R$ 3,00 por unidade a partir da 2ª.
// Para o endereço "Casa" do João (SP): R$ 24,90 com 1 unidade, R$ 27,90 com 2, R$ 30,90 com 3...
export const FRETE = 24.9; // 1 unidade, entregue em SP
export const freteSp = (unidades) => Math.round((24.9 + 3 * Math.max(0, unidades - 1)) * 100) / 100;

// Valor em reais do jeito que a tela mostra (R$ 1.234,56 — o espaço é
// "não separável", por isso o teste formata do mesmo jeito que o sistema).
export const brl = (valor) => Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

// ---------------------------------------------------------------- Loja (cliente)

export function abrirLoja(clienteId) {
  cy.visit(clienteId ? `/vendas.html?clienteId=${clienteId}` : '/vendas.html');
  cy.get('[data-cy=produto-row]').should('have.length.greaterThan', 0);
}

export function linhaProduto(produtoId) {
  return cy.get(`[data-cy=produto-row][data-id="${produtoId}"]`);
}

// Adiciona o produto pela tela e ESPERA a tela refletir o resultado antes de
// seguir: o carrinho é atualizado de forma assíncrona (chamada à API + novo
// desenho da tela), e digitar/clicar no meio disso geraria testes instáveis.
// Use { falha: true } quando a adição deve ser recusada (aguarda a mensagem de erro).
export function adicionarNaLoja(produtoId, quantidade = 1, { falha = false } = {}) {
  cy.get('[data-cy=cart-pill-count]').invoke('text').then((antes) => {
    cy.get(`[data-cy=produto-row][data-id="${produtoId}"] [data-cy=input-qtd-produto]`).clear().type(String(quantidade));
    cy.get(`[data-cy=produto-row][data-id="${produtoId}"] [data-cy=btn-adicionar-carrinho]`).click();
    if (falha) {
      cy.get('[data-cy=msg-erro]').should('be.visible');
    } else {
      cy.get('[data-cy=cart-pill-count]').should('have.text', String(Number(antes) + quantidade));
    }
  });
}

export function irParaCheckout() {
  cy.get('[data-cy=btn-iniciar-compra]').should('not.be.disabled').click();
  cy.get('[data-cy=btn-finalizar-compra]').should('be.visible');
}

export function escolherPrimeiroEndereco() {
  cy.get('[data-cy=radio-endereco]').first().check();
  // o frete é calculado pelo backend a partir do endereço: espera o valor aparecer antes de seguir
  cy.get('[data-cy=frete-valor]').should('not.contain.text', 'Selecione');
}

export function pagarComCartao(posicao = 0) {
  cy.get('[data-cy=chk-cartao]').eq(posicao).check();
}

export function escolherCupom(codigo) {
  cy.contains('[data-cy=cupom-row]', codigo).find('[data-cy=radio-cupom]').check();
}

export function confirmarPagamento() {
  cy.get('[data-cy=btn-finalizar-compra]').click();
}

// ---------------------------------------------------------------- Meus pedidos (cliente)

export function abrirPedidos(clienteId) {
  cy.visit(clienteId ? `/pedidos.html?clienteId=${clienteId}` : '/pedidos.html');
  cy.get('[data-cy=pedido-card]').should('have.length.greaterThan', 0);
}

export function cardPedido(pedidoId) {
  return cy.get(`[data-cy=pedido-card][data-id="${pedidoId}"]`);
}

// ---------------------------------------------------------------- Administração

export function abrirAdmin() {
  cy.visit('/admin.html');
  cy.get('[data-cy=tabela-pedidos]').should('be.visible');
}

export function linhaPedido(pedidoId) {
  return cy.get(`[data-cy=pedido-row][data-id="${pedidoId}"]`);
}

export function abrirAbaTrocas() {
  cy.get('[data-cy=tab-trocas]').click();
}

export function abrirAbaEstoque() {
  cy.get('[data-cy=tab-estoque]').click();
}

export function estoqueFisico(produtoId) {
  return cy.get(`[data-cy=estoque-row][data-id="${produtoId}"] [data-cy=col-estoque]`);
}
