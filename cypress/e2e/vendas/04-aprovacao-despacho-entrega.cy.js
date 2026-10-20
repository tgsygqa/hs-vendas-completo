// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 4) PAGAMENTO, DESPACHO E ENTREGA (ADMINISTRADOR)
// Tela: /admin.html | Base: DRS_LES_2_2026 — RF0039, RF0040, RF0053, RN0028,
// RN0037, RN0038, RN0039, RN0040 (um teste por requisito)
//
// A validação do pagamento NÃO é uma ação do administrador: o sistema valida sozinho
// (job agendado) e muda o pedido de EM PROCESSAMENTO para APROVADA ou REPROVADA. Os testes
// desligam esse job (cy.resetarBase) e adiantam a validação com cy.validarPagamentos; só o
// teste 12 liga o job de verdade.
//
// Regra da "operadora" simulada: cartão com final 0000 é recusado.
// Massa de dados: pedido #1 ENTREGUE (SSD) e #2 EM_TRANSPORTE (Headset).
// ============================================================================
import { PRODUTO, abrirAdmin, linhaPedido, abrirAbaEstoque, estoqueFisico } from '../../support/helpers';

// Espera (consultando a API) o pedido chegar ao status, para o teste não depender de "sleep" fixo.
function esperarStatus(pedidoId, status, tentativas = 20) {
  cy.apiRequest('GET', `/pedidos/${pedidoId}`).then((r) => {
    if (r.body.status === status) return;
    expect(tentativas, `pedido ${pedidoId}: esperado ${status}, está ${r.body.status}`).to.be.greaterThan(0);
    cy.wait(500);
    esperarStatus(pedidoId, status, tentativas - 1);
  });
}

// A tela do administrador se atualiza sozinha (a cada 3 s); por isso estas conferências esperam mais que o padrão.
const linhaEspera = (id) => cy.get(`[data-cy=pedido-row][data-id="${id}"]`, { timeout: 15000 });
const celulaEstoque = (produtoId, coluna) => cy.get(`[data-cy=estoque-row][data-id="${produtoId}"] [data-cy=${coluna}]`, { timeout: 15000 });

describe('Vendas — Validação automática do pagamento, despacho e entrega (RF0039, RF0040, RF0053, RN0028, RN0037 a RN0040)', () => {
  beforeEach(() => {
    cy.resetarBase();
  });

  it('12. deve validar o pagamento automaticamente, aprovando o pedido (APROVADA) ou reprovando-o (REPROVADA) quando a operadora recusa o cartão (RN0037, RN0038)', () => {
    cy.criarCartaoViaApi(1, { numero: '4111111111110000', bandeira: 'VISA' }).then((cartaoRecusado) => {
      cy.criarPedidoViaApi().then((aprovado) => {
        cy.criarPedidoViaApi({ cartaoId: cartaoRecusado.id }).then((recusado) => {
          abrirAdmin();
          // RF0038: a compra nasce EM PROCESSAMENTO e o administrador não tem botão para validar o pagamento
          linhaPedido(aprovado.id).should('have.attr', 'data-status', 'EM_PROCESSAMENTO');
          linhaPedido(aprovado.id).find('[data-cy=aguardando-validacao]').should('be.visible');
          linhaPedido(aprovado.id).find('[data-cy=btn-despachar]').should('not.exist');

          // liga a validação automática (1 s): ninguém clica em nada e a tela se atualiza sozinha
          cy.definirValidacaoAutomatica(1);
          linhaEspera(aprovado.id).should('have.attr', 'data-status', 'APROVADA');
          linhaPedido(aprovado.id).find('[data-cy=btn-despachar]').should('be.visible');
          linhaEspera(recusado.id).should('have.attr', 'data-status', 'REPROVADA');
          linhaPedido(recusado.id).find('[data-cy=col-motivo]').should('contain.text', '0000');
          linhaPedido(recusado.id).find('[data-cy=btn-despachar]').should('not.exist'); // pedido reprovado não segue o fluxo
          cy.definirValidacaoAutomatica(-1);
        });
      });
    });
  });

  it('13. deve dar baixa em estoque somente quando o pagamento é aprovado e manter em estoque, desbloqueados, os itens de compras reprovadas (RF0053, RN0028)', () => {
    cy.criarCartaoViaApi(1, { numero: '4111111111110000', bandeira: 'VISA' }).then((cartaoRecusado) => {
      cy.criarPedidoViaApi({ produtoId: PRODUTO.HEADSET, quantidade: 2 }).then((aprovado) => {
        cy.criarPedidoViaApi({ produtoId: PRODUTO.HEADSET, quantidade: 1, cartaoId: cartaoRecusado.id }).then((recusado) => {
          abrirAdmin();
          abrirAbaEstoque();
          // pedidos criados, mas ainda não validados: sem baixa, porém os 3 itens já ficam bloqueados
          estoqueFisico(PRODUTO.HEADSET).should('have.text', '40');
          celulaEstoque(PRODUTO.HEADSET, 'col-reservado').should('have.text', '3');
          celulaEstoque(PRODUTO.HEADSET, 'col-disponivel').should('have.text', '37');

          // pagamento reprovado: o item é desbloqueado e continua em estoque
          cy.validarPagamentos(recusado.id).should('eq', 1);
          celulaEstoque(PRODUTO.HEADSET, 'col-reservado').should('have.text', '2');
          estoqueFisico(PRODUTO.HEADSET).should('have.text', '40');

          // pagamento aprovado: só agora há baixa em estoque (40 − 2)
          cy.validarPagamentos(aprovado.id).should('eq', 1);
          celulaEstoque(PRODUTO.HEADSET, 'col-estoque').should('have.text', '38');
          celulaEstoque(PRODUTO.HEADSET, 'col-reservado').should('have.text', '0');
          celulaEstoque(PRODUTO.HEADSET, 'col-disponivel').should('have.text', '38');
        });
      });
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
