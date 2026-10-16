// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 5) PROCESSO DE TROCA
// Telas: /pedidos.html (cliente) e /admin.html (administrador)
// Base: DRS_LES_2_2026 — RF0041 a RF0045, RF0054, RN0041, RN0042, RN0043, RN0046
// (um teste por requisito)
//
// Massa de dados: pedido #1 ENTREGUE (1× SSD, R$ 1.250,00 — item de pedido 1)
// e pedido #2 EM_TRANSPORTE. O SSD tem 18 un. em estoque.
// ============================================================================
import {
  PRODUTO, brl, abrirPedidos, cardPedido, abrirAdmin, abrirAbaTrocas, abrirAbaEstoque, estoqueFisico,
} from '../../support/helpers';

// Preenche e envia o formulário de troca do pedido, pela tela do cliente.
function solicitarTrocaPelaTela(pedidoId, { quantidade, motivo = 'Produto com defeito' } = {}) {
  cardPedido(pedidoId).find('[data-cy=btn-solicitar-troca]').click();
  cardPedido(pedidoId).find('[data-cy=chk-item-troca]').first().check();
  if (quantidade) cardPedido(pedidoId).find('[data-cy=input-qtd-troca]').first().clear().type(String(quantidade));
  if (motivo) cardPedido(pedidoId).find('[data-cy=i-motivo-troca]').type(motivo);
  cardPedido(pedidoId).find('[data-cy=btn-enviar-troca]').click();
}

function linhaTroca(trocaId) {
  return cy.get(`[data-cy=troca-row][data-id="${trocaId}"]`);
}

describe('Vendas — Processo de troca (RF0041 a RF0045, RF0054, RN0041 a RN0043, RN0046)', () => {
  beforeEach(() => {
    cy.resetarBase();
  });

  it('16. deve solicitar a troca de um item, passando o pedido a EM TROCA (RF0041, RN0041)', () => {
    abrirPedidos();

    solicitarTrocaPelaTela(1, { motivo: 'O SSD apresentou falha de leitura' });

    cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'EM TROCA');
    cardPedido(1).should('have.attr', 'data-status', 'EM_TROCA');
    cardPedido(1).find('[data-cy=troca-situacao]').should('contain.text', 'Aguardando autorização');
    cardPedido(1).find('[data-cy=troca-resumo]').should('contain.text', 'O SSD apresentou falha de leitura');
  });

  it('17. não deve permitir solicitar troca de itens de pedidos que não estejam ENTREGUES (RN0043)', () => {
    abrirPedidos();

    cardPedido(1).should('have.attr', 'data-status', 'ENTREGUE');
    cardPedido(1).find('[data-cy=btn-solicitar-troca]').should('be.visible');
    cardPedido(2).should('have.attr', 'data-status', 'EM_TRANSPORTE');
    cardPedido(2).find('[data-cy=btn-solicitar-troca]').should('not.exist');

    // e o backend também recusa, mesmo sem passar pela tela
    cy.apiRequest('GET', '/pedidos/2').its('body.itens.0.id').then((itemId) => {
      cy.apiRequest('POST', '/pedidos/2/trocas', { itens: [{ itemPedidoId: itemId, quantidade: 1 }], motivoCliente: 'Defeito' }).then((r) => {
        expect(r.status).to.eq(409);
        expect(r.body.mensagem).to.contain('ENTREGUE');
      });
    });
  });

  it('18. deve visualizar as solicitações de troca com cliente, itens e motivo (RF0043)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }], 'SSD não reconhecido pela placa-mãe');
    abrirAdmin();

    abrirAbaTrocas();

    cy.get('[data-cy=troca-row]').should('have.length', 1);
    cy.get('[data-cy=troca-row]').should('contain.text', 'João Silva').and('contain.text', 'SSD NVMe').and('contain.text', 'SSD não reconhecido');
    cy.get('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'EM_TROCA');
    cy.get('[data-cy=trocas-pendentes]').should('have.text', '1');
  });

  it('19. deve autorizar a troca (TROCA AUTORIZADA) e notificar o cliente (RF0042, RN0046)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      abrirAdmin();
      abrirAbaTrocas();

      linhaTroca(troca.id).find('[data-cy=btn-autorizar-troca]').click();

      linhaTroca(troca.id).find('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'TROCA_AUTORIZADA');
      cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'TROCA AUTORIZADA').and('contain.text', 'notificado');

      abrirPedidos();
      cy.get('[data-cy=notificacao]').should('have.length', 1);
      cy.get('[data-cy=notificacao-texto]').should('contain.text', 'AUTORIZADA').and('contain.text', 'pedido #1');
      cardPedido(1).find('[data-cy=troca-situacao]').should('contain.text', 'Autorizada');
    });
  });

  it('20. deve confirmar o recebimento com retorno ao estoque: pedido TROCADO e item de volta ao estoque (RF0044, RF0054, RN0042)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      cy.autorizarTrocaViaApi(troca.id);
      abrirAdmin();
      abrirAbaEstoque();
      estoqueFisico(PRODUTO.SSD).should('have.text', '18');
      abrirAbaTrocas();

      linhaTroca(troca.id).find('[data-cy=btn-recebimento-com-estoque]').click();

      linhaTroca(troca.id).find('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'TROCADO');
      cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'devolvidos ao estoque');
      abrirAbaEstoque();
      estoqueFisico(PRODUTO.SSD).should('have.text', '19'); // reentrada em estoque
    });
  });

  it('21. deve gerar um cupom de troca no valor dos itens trocados, disponível ao cliente (RF0045)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      cy.autorizarTrocaViaApi(troca.id);
      cy.confirmarRecebimentoTrocaViaApi(troca.id, true);

      abrirPedidos();

      cy.contains('[data-cy=cupom-row]', 'TROCA-P1-S1').should('contain.text', brl(1250));
      cardPedido(1).should('have.attr', 'data-status', 'TROCADO');
      cardPedido(1).find('[data-cy=troca-situacao]').should('contain.text', 'Concluída').and('contain.text', 'TROCA-P1-S1');
    });
  });
});
