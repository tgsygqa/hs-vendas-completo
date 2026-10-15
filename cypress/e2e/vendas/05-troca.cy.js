// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 5) PROCESSO DE TROCA
// Telas: /pedidos.html (cliente) e /admin.html (administrador)
// Base: DRS_LES_2_2026 — RF0041 a RF0045, RF0054, RN0041, RN0042, RN0043, RN0046
//
// Massa de dados: pedido #1 ENTREGUE (1× SSD, R$ 1.250,00 — item de pedido 1)
// e pedido #2 EM_TRANSPORTE. O SSD tem 18 un. em estoque.
// ============================================================================
import {
  PRODUTO, brl, abrirPedidos, cardPedido, abrirAdmin, abrirAbaTrocas, abrirAbaEstoque, estoqueFisico,
  abrirLoja, adicionarNaLoja, irParaCheckout,
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

  // ------------------------------------------------------------ cliente solicita
  it('CTV070 — só pedidos ENTREGUES permitem solicitar troca (RN0043)', () => {
    abrirPedidos();

    cardPedido(1).should('have.attr', 'data-status', 'ENTREGUE');
    cardPedido(1).find('[data-cy=btn-solicitar-troca]').should('be.visible');
    cardPedido(2).should('have.attr', 'data-status', 'EM_TRANSPORTE');
    cardPedido(2).find('[data-cy=btn-solicitar-troca]').should('not.exist');
  });

  it('CTV071 — o cliente solicita a troca de um item e o pedido passa a EM TROCA (RF0041, RN0041)', () => {
    abrirPedidos();

    solicitarTrocaPelaTela(1, { motivo: 'O SSD apresentou falha de leitura' });

    cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'EM TROCA');
    cardPedido(1).should('have.attr', 'data-status', 'EM_TROCA');
    cardPedido(1).find('[data-cy=troca-situacao]').should('contain.text', 'Aguardando autorização');
    cardPedido(1).find('[data-cy=troca-resumo]').should('contain.text', 'O SSD apresentou falha de leitura');
    cardPedido(1).find('[data-cy=btn-solicitar-troca]').should('not.exist'); // já há troca em andamento
  });

  it('CTV072 — a solicitação exige ao menos um item selecionado (RF0041)', () => {
    abrirPedidos();

    cardPedido(1).find('[data-cy=btn-solicitar-troca]').click();
    cardPedido(1).find('[data-cy=i-motivo-troca]').type('Defeito');
    cardPedido(1).find('[data-cy=btn-enviar-troca]').click();

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'Selecione ao menos um item');
    cardPedido(1).should('have.attr', 'data-status', 'ENTREGUE');
  });

  it('CTV073 — a solicitação exige o motivo da troca (RF0041)', () => {
    abrirPedidos();

    solicitarTrocaPelaTela(1, { motivo: '' });

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'Informe o motivo da troca');
    cardPedido(1).should('have.attr', 'data-status', 'ENTREGUE');
  });

  it('CTV074 — não é possível trocar uma quantidade maior do que a comprada (RF0041)', () => {
    abrirPedidos();

    solicitarTrocaPelaTela(1, { quantidade: 2 }); // o pedido tem só 1 SSD

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'maior do que a quantidade comprada');
    cardPedido(1).should('have.attr', 'data-status', 'ENTREGUE');
  });

  it('CTV075 — o cliente pode trocar só parte dos itens e quantidades de um pedido (RN0041)', () => {
    cy.criarPedidoViaApi({ produtoId: PRODUTO.HEADSET, quantidade: 2 }).then((pedido) => {
      cy.avancarPedidoViaApi(pedido.id, 'ENTREGUE');
      abrirPedidos();

      solicitarTrocaPelaTela(pedido.id, { quantidade: 1 }); // troca 1 dos 2 headsets

      cardPedido(pedido.id).should('have.attr', 'data-status', 'EM_TROCA');
      cardPedido(pedido.id).find('[data-cy=troca-resumo]').should('contain.text', '1× Headset Gamer Pro X');
    });
  });

  // ------------------------------------------------------------ administrador visualiza e decide
  it('CTV076 — o administrador visualiza as solicitações de troca com cliente, itens e motivo (RF0043)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }], 'SSD não reconhecido pela placa-mãe');
    abrirAdmin();

    abrirAbaTrocas();

    cy.get('[data-cy=troca-row]').should('have.length', 1);
    cy.get('[data-cy=troca-row]').should('contain.text', 'João Silva').and('contain.text', 'SSD NVMe').and('contain.text', 'SSD não reconhecido');
    cy.get('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'EM_TROCA');
    cy.get('[data-cy=trocas-pendentes]').should('have.text', '1');
  });

  it('CTV077 — o administrador autoriza a troca e o pedido passa a TROCA AUTORIZADA (RF0042)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      abrirAdmin();
      abrirAbaTrocas();

      linhaTroca(troca.id).find('[data-cy=btn-autorizar-troca]').click();

      linhaTroca(troca.id).find('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'TROCA_AUTORIZADA');
      cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'TROCA AUTORIZADA').and('contain.text', 'notificado');
    });
  });

  it('CTV078 — ao autorizar a troca o cliente recebe uma notificação (RN0046)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      cy.autorizarTrocaViaApi(troca.id);

      abrirPedidos();

      cy.get('[data-cy=notificacao]').should('have.length', 1);
      cy.get('[data-cy=notificacao-texto]').should('contain.text', 'AUTORIZADA').and('contain.text', 'pedido #1');
      cy.get('[data-cy=notificacoes-nao-lidas]').should('have.text', '1');
      cardPedido(1).find('[data-cy=troca-situacao]').should('contain.text', 'Autorizada');

      cy.get('[data-cy=btn-marcar-lida]').click();
      cy.get('[data-cy=notificacoes-nao-lidas]').should('have.text', '0');
    });
  });

  it('CTV079 — recusar a troca exige o motivo (caminho alternativo do RF0042)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      abrirAdmin();
      abrirAbaTrocas();

      linhaTroca(troca.id).find('[data-cy=btn-recusar-troca]').click();
      linhaTroca(troca.id).find('[data-cy=btn-confirmar-recusa]').click(); // sem informar o motivo

      cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'Informe o motivo da recusa');
      linhaTroca(troca.id).find('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'EM_TROCA');
    });
  });

  it('CTV080 — a troca recusada devolve o pedido para ENTREGUE, notifica o cliente e permite nova solicitação (caminho alternativo do RF0042)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      abrirAdmin();
      abrirAbaTrocas();

      linhaTroca(troca.id).find('[data-cy=btn-recusar-troca]').click();
      linhaTroca(troca.id).find('[data-cy=i-motivo-recusa]').type('Fora do prazo de troca');
      linhaTroca(troca.id).find('[data-cy=btn-confirmar-recusa]').click();

      linhaTroca(troca.id).find('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'RECUSADA');
      cy.get('[data-cy=msg-sucesso]').should('be.visible').and('contain.text', 'voltou para ENTREGUE');

      abrirPedidos();
      cardPedido(1).should('have.attr', 'data-status', 'ENTREGUE');
      cardPedido(1).find('[data-cy=troca-situacao]').should('contain.text', 'Recusada').and('contain.text', 'Fora do prazo de troca');
      cardPedido(1).find('[data-cy=btn-solicitar-troca]').should('be.visible');
      cy.get('[data-cy=notificacao-texto]').should('contain.text', 'RECUSADA');
    });
  });

  // ------------------------------------------------------------ recebimento, estoque e cupom
  it('CTV081 — confirmar o recebimento com retorno ao estoque: pedido TROCADO e item volta ao estoque (RF0044, RF0054, RN0042)', () => {
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

  it('CTV082 — confirmar o recebimento sem retorno ao estoque não altera o estoque (RF0044, RF0054)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      cy.autorizarTrocaViaApi(troca.id);
      abrirAdmin();
      abrirAbaTrocas();

      linhaTroca(troca.id).find('[data-cy=btn-recebimento-sem-estoque]').click();

      linhaTroca(troca.id).find('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'TROCADO');
      linhaTroca(troca.id).should('contain.text', 'não voltaram ao estoque');
      abrirAbaEstoque();
      estoqueFisico(PRODUTO.SSD).should('have.text', '18'); // continua igual
    });
  });

  it('CTV083 — o recebimento gera um cupom de troca, no valor dos itens trocados, disponível ao cliente (RF0045)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      cy.autorizarTrocaViaApi(troca.id);
      cy.confirmarRecebimentoTrocaViaApi(troca.id, true);

      abrirPedidos();

      cy.contains('[data-cy=cupom-row]', 'TROCA-P1-S1').should('contain.text', brl(1250));
      cardPedido(1).should('have.attr', 'data-status', 'TROCADO');
      cardPedido(1).find('[data-cy=troca-situacao]').should('contain.text', 'Concluída').and('contain.text', 'TROCA-P1-S1');
      cy.get('[data-cy=notificacao-texto]').should('contain.text', 'TROCA-P1-S1');
    });
  });

  it('CTV084 — na troca parcial o cupom vale só os itens trocados (RF0045, RN0041)', () => {
    cy.criarPedidoViaApi({ produtoId: PRODUTO.HEADSET, quantidade: 2 }).then((pedido) => {
      cy.avancarPedidoViaApi(pedido.id, 'ENTREGUE');
      cy.apiRequest('GET', `/pedidos/${pedido.id}`).its('body.itens.0.id').then((itemId) => {
        cy.solicitarTrocaViaApi(pedido.id, [{ itemPedidoId: itemId, quantidade: 1 }]).then((troca) => {
          cy.autorizarTrocaViaApi(troca.id);
          cy.confirmarRecebimentoTrocaViaApi(troca.id, true).then((r) => {
            expect(r.cupomGerado.valor).to.eq(349.9); // 1 unidade, sem o frete
          });
        });
      });
    });
  });

  it('CTV085 — o cupom de troca gerado pode ser usado como pagamento numa nova compra (RF0037, RF0045)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      cy.autorizarTrocaViaApi(troca.id);
      cy.confirmarRecebimentoTrocaViaApi(troca.id, true);

      abrirLoja();
      adicionarNaLoja(PRODUTO.HEADSET, 1);
      irParaCheckout();

      cy.contains('[data-cy=cupom-row]', 'TROCA-P1-S1').should('contain.text', brl(1250)).find('[data-cy=radio-cupom]').check();
      cy.get('[data-cy=valor-restante]').should('have.text', brl(0)); // R$ 1.250,00 cobrem a compra de R$ 374,80
    });
  });

  it('CTV086 — a troca só avança na ordem: não dá para confirmar recebimento sem autorização (RF0042, RF0044)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      cy.apiRequest('PATCH', `/trocas/${troca.id}/confirmar-recebimento`, { itensRetornaramEstoque: true })
        .its('status').should('eq', 409);

      abrirAdmin();
      abrirAbaTrocas();
      linhaTroca(troca.id).find('[data-cy=btn-recebimento-com-estoque]').should('not.exist');
    });
  });
});
