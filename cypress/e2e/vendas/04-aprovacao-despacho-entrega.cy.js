// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 4) PAGAMENTO, DESPACHO E ENTREGA (ADMINISTRADOR)
// Tela: /admin.html | Base: DRS_LES_2_2026 — RF0038, RF0039, RF0040, RF0053,
// RN0028, RN0037, RN0038, RN0039, RN0040
//
// Regra da "operadora" simulada: cartão com final 0000 é recusado.
// Massa de dados: pedido #1 ENTREGUE (SSD) e #2 EM_TRANSPORTE (Headset).
// ============================================================================
import { PRODUTO, abrirAdmin, linhaPedido, abrirAbaEstoque, estoqueFisico, abrirPedidos, cardPedido } from '../../support/helpers';

describe('Vendas — Validação de pagamento, despacho e entrega (RF0038-RF0040, RF0053, RN0037-RN0040)', () => {
  beforeEach(() => {
    cy.resetarBase();
  });

  it('CTV060 — uma compra recém-finalizada aparece para o administrador EM PROCESSAMENTO, aguardando validação (RF0038)', () => {
    cy.criarPedidoViaApi().then((pedido) => {
      abrirAdmin();

      linhaPedido(pedido.id).should('have.attr', 'data-status', 'EM_PROCESSAMENTO');
      linhaPedido(pedido.id).find('[data-cy=col-cliente]').should('contain.text', 'João Silva');
      linhaPedido(pedido.id).find('[data-cy=btn-validar-pagamento]').should('be.visible');
      linhaPedido(pedido.id).find('[data-cy=btn-despachar]').should('not.exist'); // RN0039: só pedido aprovado é despachado
    });
  });

  it('CTV061 — validar o pagamento com sucesso aprova o pedido (RN0037, RN0038)', () => {
    cy.criarPedidoViaApi().then((pedido) => {
      abrirAdmin();

      linhaPedido(pedido.id).find('[data-cy=btn-validar-pagamento]').click();

      linhaPedido(pedido.id).should('have.attr', 'data-status', 'APROVADA');
      cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'aprovado');
      linhaPedido(pedido.id).find('[data-cy=btn-despachar]').should('be.visible');
    });
  });

  it('CTV062 — a baixa em estoque só acontece quando o pagamento é aprovado (RF0053, RN0038)', () => {
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

  it('CTV063 — pagamento recusado pela operadora reprova o pedido e preserva o estoque (RN0028, RN0038)', () => {
    cy.criarCartaoViaApi(1, { numero: '4111111111110000', bandeira: 'VISA' }).then((cartao) => {
      cy.criarPedidoViaApi({ cartaoId: cartao.id }).then((pedido) => {
        abrirAdmin();

        linhaPedido(pedido.id).find('[data-cy=btn-validar-pagamento]').click();

        linhaPedido(pedido.id).should('have.attr', 'data-status', 'REPROVADA');
        cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'REPROVADO');
        linhaPedido(pedido.id).find('[data-cy=col-motivo]').should('contain.text', '0000');
        linhaPedido(pedido.id).find('[data-cy=btn-despachar]').should('not.exist'); // pedido reprovado não segue o fluxo
        abrirAbaEstoque();
        estoqueFisico(PRODUTO.HEADSET).should('have.text', '40'); // nada foi baixado
      });
    });
  });

  it('CTV064 — o cliente vê o motivo quando o pagamento do pedido é reprovado (RN0038)', () => {
    cy.criarCartaoViaApi(1, { numero: '4111111111110000', bandeira: 'VISA' }).then((cartao) => {
      cy.criarPedidoViaApi({ cartaoId: cartao.id }).then((pedido) => {
        cy.apiRequest('PATCH', `/pedidos/${pedido.id}/validar-pagamento`).its('status').should('eq', 200);

        abrirPedidos();

        cardPedido(pedido.id).should('have.attr', 'data-status', 'REPROVADA');
        cardPedido(pedido.id).find('[data-cy=motivo-reprovacao]').should('contain.text', 'recusado pela operadora');
      });
    });
  });

  it('CTV065 — o administrador despacha um pedido aprovado e ele passa a EM TRANSPORTE (RF0039, RN0039)', () => {
    cy.criarPedidoViaApi().then((pedido) => {
      cy.avancarPedidoViaApi(pedido.id, 'APROVADA');
      abrirAdmin();

      linhaPedido(pedido.id).find('[data-cy=btn-despachar]').click();

      linhaPedido(pedido.id).should('have.attr', 'data-status', 'EM_TRANSPORTE');
      cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'despachado');
      linhaPedido(pedido.id).find('[data-cy=btn-confirmar-entrega]').should('be.visible');
    });
  });

  it('CTV066 — o administrador confirma a entrega e o pedido passa a ENTREGUE (RF0040, RN0040)', () => {
    abrirAdmin();

    linhaPedido(2).should('have.attr', 'data-status', 'EM_TRANSPORTE'); // pedido da massa de dados
    linhaPedido(2).find('[data-cy=btn-confirmar-entrega]').click();

    linhaPedido(2).should('have.attr', 'data-status', 'ENTREGUE');
    cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'ENTREGUE');
    linhaPedido(2).find('[data-cy=btn-confirmar-entrega]').should('not.exist');
  });

  it('CTV067 — o painel filtra os pedidos pelo status (RF0040)', () => {
    abrirAdmin();
    cy.get('[data-cy=pedido-row]').should('have.length', 2);

    cy.get('[data-cy=filtro-status]').select('ENTREGUE');
    cy.get('[data-cy=pedido-row]').should('have.length', 1);
    linhaPedido(1).should('have.attr', 'data-status', 'ENTREGUE');

    cy.get('[data-cy=filtro-status]').select('APROVADA');
    cy.get('[data-cy=sem-pedidos]').should('be.visible');

    cy.get('[data-cy=filtro-status]').select('Todos');
    cy.get('[data-cy=pedido-row]').should('have.length', 2);
  });

  it('CTV068 — o cliente acompanha em "Meus pedidos" o status do pedido a cada ação do administrador (RF0038-RF0040)', () => {
    cy.criarPedidoViaApi().then((pedido) => {
      abrirPedidos();
      cardPedido(pedido.id).find('[data-cy=pedido-status]').should('have.attr', 'data-status', 'EM_PROCESSAMENTO');

      cy.avancarPedidoViaApi(pedido.id, 'APROVADA');
      cy.get('[data-cy=btn-atualizar]').click();
      cardPedido(pedido.id).find('[data-cy=pedido-status]').should('have.attr', 'data-status', 'APROVADA');

      cy.avancarPedidoViaApi(pedido.id, 'EM_TRANSPORTE');
      cy.get('[data-cy=btn-atualizar]').click();
      cardPedido(pedido.id).find('[data-cy=pedido-status]').should('have.attr', 'data-status', 'EM_TRANSPORTE');
    });
  });

  it('CTV069 — pedido aprovado já recebe a baixa em estoque somente uma vez, mesmo se a validação for pedida de novo (RF0053)', () => {
    cy.criarPedidoViaApi({ quantidade: 1 }).then((pedido) => {
      cy.avancarPedidoViaApi(pedido.id, 'APROVADA');

      cy.apiRequest('PATCH', `/pedidos/${pedido.id}/validar-pagamento`).its('status').should('eq', 409);

      abrirAdmin();
      abrirAbaEstoque();
      estoqueFisico(PRODUTO.HEADSET).should('have.text', '39'); // 40 − 1, uma única baixa
    });
  });
});
