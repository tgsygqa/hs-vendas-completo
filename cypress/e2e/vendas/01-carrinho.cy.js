// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 1) CARRINHO DE COMPRA
// Ferramenta: Cypress | Sistema: Hardware Store | Tela: /vendas.html
// Base: DRS_LES_2_2026 — RF0031, RF0032, RN0031
//
// Cada teste começa com a base zerada (cy.resetarBase): João Silva (cliente 1),
// 5 produtos (RTX 12 un., Ryzen 25, Headset 40, Teclado 30, SSD 18).
// ============================================================================
import { PRODUTO, brl, abrirLoja, linhaProduto, adicionarNaLoja } from '../../support/helpers';

describe('Vendas — Carrinho (RF0031, RF0032, RN0031)', () => {
  beforeEach(() => {
    cy.resetarBase();
    abrirLoja();
  });

  it('CTV001 — deve listar os produtos disponíveis com preço e estoque (RF0031)', () => {
    cy.get('[data-cy=produto-row]').should('have.length', 5);
    linhaProduto(PRODUTO.RTX).should('contain.text', 'RTX 4090').and('contain.text', brl(14500));
    cy.get(`[data-cy=produto-row][data-id="${PRODUTO.RTX}"] [data-cy=produto-estoque]`).should('contain.text', '12 em estoque');
    cy.get('[data-cy=carrinho-vazio]').should('be.visible');
    cy.get('[data-cy=btn-iniciar-compra]').should('be.disabled');
  });

  it('CTV002 — deve adicionar um produto ao carrinho escolhendo a quantidade (RF0031, RF0032)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 2);

    cy.get('[data-cy=carrinho-item]').should('have.length', 1).and('contain.text', 'Headset Gamer Pro X');
    cy.get('[data-cy=qtd-item]').should('have.text', '2');
    cy.get('[data-cy=carrinho-total]').should('have.text', brl(699.8));
    cy.get('[data-cy=cart-pill-count]').should('have.text', '2');
    cy.get('[data-cy=btn-iniciar-compra]').should('not.be.disabled');
  });

  it('CTV003 — adicionar o mesmo produto de novo soma na mesma linha do carrinho (RF0031)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    cy.get('[data-cy=qtd-item]').should('have.text', '1');
    adicionarNaLoja(PRODUTO.HEADSET, 2);

    cy.get('[data-cy=carrinho-item]').should('have.length', 1);
    cy.get('[data-cy=qtd-item]').should('have.text', '3');
  });

  it('CTV004 — deve alterar a quantidade de um item já no carrinho (RF0032)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);

    cy.get('[data-cy=btn-aumentar-qtd]').click();
    cy.get('[data-cy=qtd-item]').should('have.text', '2');
    cy.get('[data-cy=carrinho-total]').should('have.text', brl(699.8));

    cy.get('[data-cy=btn-diminuir-qtd]').click();
    cy.get('[data-cy=qtd-item]').should('have.text', '1');
    cy.get('[data-cy=carrinho-total]').should('have.text', brl(349.9));
  });

  it('CTV005 — diminuir a quantidade até zero remove o item (RF0032)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    cy.get('[data-cy=btn-diminuir-qtd]').click();

    cy.get('[data-cy=carrinho-item]').should('not.exist');
    cy.get('[data-cy=carrinho-vazio]').should('be.visible');
  });

  it('CTV006 — deve excluir um item do carrinho (RF0031)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    adicionarNaLoja(PRODUTO.TECLADO, 1);
    cy.get('[data-cy=carrinho-item]').should('have.length', 2);

    cy.contains('[data-cy=carrinho-item]', 'Headset').find('[data-cy=btn-remover-item]').click();

    cy.get('[data-cy=carrinho-item]').should('have.length', 1).and('contain.text', 'Teclado');
    cy.get('[data-cy=carrinho-total]').should('have.text', brl(459.9));
  });

  it('CTV007 — não deve permitir adicionar mais itens do que o estoque (RN0031)', () => {
    adicionarNaLoja(PRODUTO.RTX, 13, { falha: true }); // há só 12 em estoque

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'Estoque insuficiente');
    cy.get('[data-cy=carrinho-vazio]').should('be.visible');
  });

  it('CTV008 — o estoque considera o que o cliente já tem no carrinho (RN0031)', () => {
    adicionarNaLoja(PRODUTO.RTX, 12);
    cy.get('[data-cy=qtd-item]').should('have.text', '12');

    adicionarNaLoja(PRODUTO.RTX, 1, { falha: true }); // 12 + 1 > 12

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'Estoque insuficiente');
    cy.get('[data-cy=qtd-item]').should('have.text', '12');
  });

  it('CTV009 — aumentar a quantidade no carrinho acima do estoque é recusado (RN0031, RF0032)', () => {
    adicionarNaLoja(PRODUTO.RTX, 12);

    cy.get('[data-cy=btn-aumentar-qtd]').click();

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'Estoque insuficiente');
    cy.get('[data-cy=qtd-item]').should('have.text', '12');
  });

  it('CTV010 — produto sem estoque aparece como indisponível e não pode ser adicionado (RN0031)', () => {
    cy.definirEstoque(PRODUTO.TECLADO, 0);
    cy.reload();

    cy.get(`[data-cy=produto-row][data-id="${PRODUTO.TECLADO}"] [data-cy=produto-estoque]`).should('contain.text', 'Indisponível');
    cy.get(`[data-cy=produto-row][data-id="${PRODUTO.TECLADO}"] [data-cy=btn-adicionar-carrinho]`).should('be.disabled');
    cy.get(`[data-cy=produto-row][data-id="${PRODUTO.HEADSET}"] [data-cy=btn-adicionar-carrinho]`).should('not.be.disabled');
  });

  it('CTV011 — o carrinho continua o mesmo ao recarregar a página (RF0031)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 2);
    cy.get('[data-cy=carrinho-item]').should('have.length', 1);

    cy.reload();

    cy.get('[data-cy=carrinho-item]').should('have.length', 1).and('contain.text', 'Headset Gamer Pro X');
    cy.get('[data-cy=qtd-item]').should('have.text', '2');
  });
});
