// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 4) PAGAMENTO, DESPACHO E ENTREGA (ADMINISTRADOR)
// Tela: /admin.html | Base: DRS_LES_2_2026 — RF0039, RF0040, RF0053,
// RN0038, RN0039, RN0040 (um teste por requisito; RN0037 está em 07)
//
// Regra da "operadora" simulada: cartão com final 0000 é recusado.
// Massa de dados: pedido #1 ENTREGUE (SSD) e #2 EM_TRANSPORTE (Headset).
// ============================================================================
import { PRODUTO, abrirAdmin, linhaPedido, abrirAbaEstoque, estoqueFisico } from '../../support/helpers';

describe('Vendas — Validação de pagamento, despacho e entrega (RF0039, RF0040, RF0053, RN0038-RN0040)', () => {
  beforeEach(() => {
    cy.resetarBase();
  });

  it('12. deve aprovar o pedido (APROVADA) quando o pagamento é validado e reprová-lo (REPROVADA) quando a operadora o recusa (RN0038)', () => {
    cy.criarCartaoViaApi(1, { numero: '4111111111110000', bandeira: 'VISA' }).then((cartaoRecusado) => {
      cy.criarPedidoViaApi().then((aprovado) => {
        cy.criarPedidoViaApi({ cartaoId: cartaoRecusado.id }).then((recusado) => {
          abrirAdmin();

          linhaPedido(aprovado.id).find('[data-cy=btn-validar-pagamento]').click();
          linhaPedido(aprovado.id).should('have.attr', 'data-status', 'APROVADA');
          cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'aprovado');
          linhaPedido(aprovado.id).find('[data-cy=btn-despachar]').should('be.visible');

          linhaPedido(recusado.id).find('[data-cy=btn-validar-pagamento]').click();
          linhaPedido(recusado.id).should('have.attr', 'data-status', 'REPROVADA');
          cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'REPROVADO');
          linhaPedido(recusado.id).find('[data-cy=col-motivo]').should('contain.text', '0000');
          linhaPedido(recusado.id).find('[data-cy=btn-despachar]').should('not.exist'); // pedido reprovado não segue o fluxo
        });
      });
    });
  });

  it('13. deve dar baixa em estoque somente quando o pagamento é aprovado (RF0053)', () => {
    cy.criarPedidoViaApi({ produtoId: PRODUTO.HEADSET, quantidade: 2 }).then((pedido) => {
      abrirAdmin();
      abrirAbaEstoque();
      estoqueFisico(PRODUTO.HEADSET).should('have.text', '40'); // pedido criado, mas ainda não aprovado

      cy.get('[data-cy=tab-pedidos]').click();
      linhaPedido(pedido.id).find('[data-cy=btn-validar-pagamento]').click();
      linhaPedido(pedido.id).should('have.attr', 'data-status', 'APROVADA');

      abrirAbaEstoque();
      estoqueFisico(PRODUTO.HEADSET).should('have.text', '38'); // 40 − 2
    });
  });

  it('14. deve despachar o pedido aprovado, passando-o a EM TRANSPORTE (RF0039, RN0039)', () => {
    cy.criarPedidoViaApi().then((pedido) => {
      cy.avancarPedidoViaApi(pedido.id, 'APROVADA');
      abrirAdmin();

      linhaPedido(pedido.id).find('[data-cy=btn-despachar]').click();

      linhaPedido(pedido.id).should('have.attr', 'data-status', 'EM_TRANSPORTE');
      cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'despachado');
      linhaPedido(pedido.id).find('[data-cy=btn-confirmar-entrega]').should('be.visible');
    });
  });

  it('15. deve confirmar a entrega, passando o pedido a ENTREGUE (RF0040, RN0040)', () => {
    abrirAdmin();

    linhaPedido(2).should('have.attr', 'data-status', 'EM_TRANSPORTE'); // pedido da massa de dados
    linhaPedido(2).find('[data-cy=btn-confirmar-entrega]').click();

    linhaPedido(2).should('have.attr', 'data-status', 'ENTREGUE');
    cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'ENTREGUE');
    linhaPedido(2).find('[data-cy=btn-confirmar-entrega]').should('not.exist');
  });
});
