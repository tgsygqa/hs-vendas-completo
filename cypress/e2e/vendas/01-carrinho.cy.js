// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 1) CARRINHO DE COMPRA
// Ferramenta: Cypress | Sistema: Hardware Store | Tela: /vendas.html
// Base: DRS_LES_2_2026 — RF0031, RF0032, RN0031 (um teste por requisito)
//
// Cada teste começa com a base zerada (cy.resetarBase): João Silva (cliente 1),
// 5 produtos (RTX 12 un., Ryzen 25, Headset 40, Teclado 30, SSD 18).
// ============================================================================
import { PRODUTO, brl, abrirLoja, adicionarNaLoja } from '../../support/helpers';

describe('Vendas — Carrinho (RF0031, RF0032, RN0031)', () => {
  beforeEach(() => {
    cy.resetarBase();
    abrirLoja();
  });

  it('1. deve gerenciar o carrinho: listar os produtos, adicionar, visualizar e excluir itens (RF0031)', () => {
    cy.get('[data-cy=produto-row]').should('have.length', 5);
    cy.get('[data-cy=carrinho-vazio]').should('be.visible');

    adicionarNaLoja(PRODUTO.HEADSET, 1);
    adicionarNaLoja(PRODUTO.TECLADO, 1);
    cy.get('[data-cy=carrinho-item]').should('have.length', 2);
    cy.get('[data-cy=carrinho-total]').should('have.text', brl(349.9 + 459.9));

    cy.contains('[data-cy=carrinho-item]', 'Headset').find('[data-cy=btn-remover-item]').click();

    cy.get('[data-cy=carrinho-item]').should('have.length', 1).and('contain.text', 'Teclado');
    cy.get('[data-cy=carrinho-total]').should('have.text', brl(459.9));
  });

  it('2. deve definir a quantidade ao adicionar o produto e alterá-la no carrinho (RF0032)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 2);
    cy.get('[data-cy=qtd-item]').should('have.text', '2');
    cy.get('[data-cy=carrinho-total]').should('have.text', brl(699.8));

    cy.get('[data-cy=btn-diminuir-qtd]').click();
    cy.get('[data-cy=qtd-item]').should('have.text', '1');
    cy.get('[data-cy=carrinho-total]').should('have.text', brl(349.9));

    cy.get('[data-cy=btn-aumentar-qtd]').click();
    cy.get('[data-cy=qtd-item]').should('have.text', '2');
    cy.get('[data-cy=carrinho-total]').should('have.text', brl(699.8));
  });

  it('3. não deve permitir adicionar item indisponível nem mais itens do que o estoque (RN0031)', () => {
    adicionarNaLoja(PRODUTO.RTX, 13, { falha: true }); // há só 12 em estoque

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'Estoque insuficiente');
    cy.get('[data-cy=carrinho-vazio]').should('be.visible');

    cy.definirEstoque(PRODUTO.TECLADO, 0);
    cy.reload();

    cy.get(`[data-cy=produto-row][data-id="${PRODUTO.TECLADO}"] [data-cy=produto-estoque]`).should('contain.text', 'Indisponível');
    cy.get(`[data-cy=produto-row][data-id="${PRODUTO.TECLADO}"] [data-cy=btn-adicionar-carrinho]`).should('be.disabled');
  });
});
