// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 2) BLOQUEIO E EXPIRAÇÃO DO CARRINHO
// Base: DRS_LES_2_2026 — RN0044, RN0045, RNF0042 (um teste por requisito)
//
// Para não esperar 30 minutos de verdade, os testes ajustam o prazo do
// bloqueio pelo endpoint de apoio (cy.definirBloqueio) e disparam o job de
// expiração na hora (cy.varrerExpirados).
// ============================================================================
import { PRODUTO, abrirLoja, adicionarNaLoja } from '../../support/helpers';

describe('Vendas — Bloqueio e expiração dos itens do carrinho (RN0044, RN0045, RNF0042)', () => {
  beforeEach(() => {
    cy.resetarBase();
  });

  it('4. deve bloquear os itens do carrinho por um prazo, indisponíveis para outros clientes, com aviso 5 minutos antes de expirar (RN0044)', () => {
    cy.criarClienteViaApi().then((maria) => {
      abrirLoja();
      adicionarNaLoja(PRODUTO.RTX, 12); // João bloqueia as 12 placas

      cy.get('[data-cy=bloqueio-contador]').invoke('text').should('match', /^(29|30):\d{2}$/); // prazo padrão: 30 min
      cy.get('[data-cy=aviso-expiracao]').should('not.be.visible'); // ainda longe dos 5 minutos finais

      abrirLoja(maria.id); // outro cliente não consegue comprar o que João bloqueou
      cy.get(`[data-cy=produto-row][data-id="${PRODUTO.RTX}"] [data-cy=produto-estoque]`).should('contain.text', 'reservado');
      cy.get(`[data-cy=produto-row][data-id="${PRODUTO.RTX}"] [data-cy=btn-adicionar-carrinho]`).should('be.disabled');
      cy.get(`[data-cy=produto-row][data-id="${PRODUTO.RYZEN}"] [data-cy=btn-adicionar-carrinho]`).should('not.be.disabled');

      cy.definirBloqueio(1, 240); // faltam 4 minutos
      abrirLoja();
      cy.get('[data-cy=aviso-expiracao]').should('be.visible').and('contain.text', 'liberados para outros clientes');
      cy.get('[data-cy=aviso-expiracao-tempo]').invoke('text').should('match', /^0[34]:\d{2}$/);
    });
  });

  it('5. deve retirar os itens do carrinho ao expirar o bloqueio, apresentá-los como retirados e desabilitar a compra até adicioná-los de novo (RN0045, RNF0042)', () => {
    abrirLoja();
    adicionarNaLoja(PRODUTO.HEADSET, 2);
    cy.definirBloqueio(1, -5); // prazo já vencido
    cy.varrerExpirados();
    cy.reload();

    cy.get('[data-cy=carrinho-vazio]').should('be.visible'); // RN0045: o item saiu do carrinho
    cy.get('[data-cy=aviso-itens-expirados]').should('be.visible');
    cy.get('[data-cy=item-expirado]').should('have.length', 1).and('contain.text', 'Headset Gamer Pro X').and('contain.text', '2 un.');
    cy.get('[data-cy=btn-iniciar-compra]').should('be.disabled'); // RNF0042

    cy.get('[data-cy=btn-readicionar-expirado]').click();

    cy.get('[data-cy=item-expirado]').should('not.exist');
    cy.get('[data-cy=carrinho-item]').should('have.length', 1).and('contain.text', 'Headset Gamer Pro X');
    cy.get('[data-cy=qtd-item]').should('have.text', '2');
    cy.get('[data-cy=btn-iniciar-compra]').should('not.be.disabled');
  });
});
