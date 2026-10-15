// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 7) REGRAS DE NEGÓCIO PELA API
//
// Regras que a interface NÃO deixa o usuário violar (a tela já impede), mas que
// o backend precisa garantir sozinho: aqui as requisições são feitas direto na
// API REST, com cy.request.
// Base: DRS_LES_2_2026 — RN0033, RN0034, RN0035, RN0036, RN0037, RN0039,
// RN0040, RN0042, RN0043
//
// Massa de dados: João (cliente 1) tem o cartão 1, o endereço 1 e os cupons
// 1 PROMO-BEMVINDO R$ 50, 2 PROMO-VERAO R$ 30, 3 TROCA-JOAO-PEQUENO R$ 20,
// 4 TROCA-JOAO-GRANDE R$ 500. Uma compra de 1 headset vale R$ 374,80.
// ============================================================================
import { PRODUTO } from '../../support/helpers';

const finalizar = (corpo) => cy.apiRequest('POST', '/clientes/1/pedidos', Object.assign({ enderecoId: 1, cartoes: [], cupomIds: [] }, corpo));

describe('Vendas — Regras de negócio validadas pela API (RN0033 a RN0043)', () => {
  beforeEach(() => {
    cy.resetarBase();
  });

  it('CTV110 — não aceita dois cupons promocionais na mesma compra (RN0033)', () => {
    cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);

    finalizar({ cartoes: [{ cartaoId: 1, valor: 294.8 }], cupomIds: [1, 2] }).then((r) => {
      expect(r.status).to.eq(409);
      expect(r.body.mensagem).to.contain('RN0033');
    });
  });

  it('CTV111 — com mais de um cartão e sem cupom, cada cartão precisa pagar ao menos R$ 10,00 (RN0034)', () => {
    cy.criarCartaoViaApi(1).then((cartao2) => {
      cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);

      finalizar({ cartoes: [{ cartaoId: 1, valor: 369.8 }, { cartaoId: cartao2.id, valor: 5 }] }).then((r) => {
        expect(r.status).to.eq(409);
        expect(r.body.mensagem).to.contain('RN0034');
      });
    });
  });

  it('CTV112 — combinado com cupom, o cartão pode pagar menos de R$ 10,00 (RN0035)', () => {
    cy.criarCartaoViaApi(1).then((cartao2) => {
      cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);

      finalizar({ cartoes: [{ cartaoId: 1, valor: 319.8 }, { cartaoId: cartao2.id, valor: 5 }], cupomIds: [1] }).then((r) => {
        expect(r.status).to.eq(201);
        expect(r.body.status).to.eq('EM_PROCESSAMENTO');
      });
    });
  });

  it('CTV113 — os pagamentos precisam cobrir exatamente o valor da compra', () => {
    cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);

    finalizar({ cartoes: [{ cartaoId: 1, valor: 300 }] }).its('status').should('eq', 409); // falta
    finalizar({ cartoes: [{ cartaoId: 1, valor: 400 }] }).its('status').should('eq', 409); // sobra
    finalizar({}).its('status').should('eq', 409);                                          // nada informado
  });

  it('CTV114 — não permite usar cupons desnecessários além do que cobre a compra (RN0036)', () => {
    cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);

    // R$ 500 já cobrem os R$ 374,80; o de R$ 20 seria desnecessário
    finalizar({ cupomIds: [4, 3] }).then((r) => {
      expect(r.status).to.eq(409);
      expect(r.body.mensagem).to.contain('RN0036');
    });
  });

  it('CTV115 — cupom maior que a compra é aceito e gera cupom de troco com a diferença (RN0036)', () => {
    cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);

    finalizar({ cupomIds: [4] }).its('status').should('eq', 201);

    cy.apiRequest('GET', '/clientes/1/cupons').its('body').then((cupons) => {
      const troco = cupons.find((c) => c.codigo.startsWith('TROCO-'));
      expect(troco, 'cupom de troco').to.exist;
      expect(troco.valor).to.eq(125.2);
    });
  });

  it('CTV116 — um cupom já utilizado não pode ser usado de novo (RN0037)', () => {
    cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);
    finalizar({ cartoes: [{ cartaoId: 1, valor: 324.8 }], cupomIds: [1] }).its('status').should('eq', 201);

    cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);
    finalizar({ cartoes: [{ cartaoId: 1, valor: 324.8 }], cupomIds: [1] }).then((r) => {
      expect(r.status).to.eq(409);
      expect(r.body.mensagem).to.contain('já foi utilizado');
    });
  });

  it('CTV117 — cupom de troca de outro cliente não pode ser usado (RN0037)', () => {
    cy.criarClienteViaApi().then((maria) => {
      cy.adicionarAoCarrinhoViaApi(maria.id, PRODUTO.HEADSET, 1);
      cy.apiRequest('GET', `/clientes/${maria.id}`).its('body.enderecos.0.id').then((enderecoId) => {
        cy.apiRequest('POST', `/clientes/${maria.id}/pedidos`, { enderecoId, cartoes: [], cupomIds: [3] }).then((r) => {
          expect(r.status).to.eq(409);
          expect(r.body.mensagem).to.contain('não pertence a este cliente');
        });
      });
    });
  });

  it('CTV118 — o despacho só vale para pedido aprovado (RN0039)', () => {
    cy.criarPedidoViaApi().then((pedido) => {
      cy.apiRequest('PATCH', `/pedidos/${pedido.id}/despachar`).its('status').should('eq', 409); // ainda EM_PROCESSAMENTO
      cy.avancarPedidoViaApi(pedido.id, 'APROVADA');
      cy.apiRequest('PATCH', `/pedidos/${pedido.id}/despachar`).its('status').should('eq', 200);
    });
  });

  it('CTV119 — a entrega só pode ser confirmada para pedido em transporte (RN0040)', () => {
    cy.criarPedidoViaApi().then((pedido) => {
      cy.avancarPedidoViaApi(pedido.id, 'APROVADA');
      cy.apiRequest('PATCH', `/pedidos/${pedido.id}/confirmar-entrega`).its('status').should('eq', 409); // APROVADA ainda
    });
  });

  it('CTV120 — só itens de pedidos ENTREGUES podem receber solicitação de troca (RN0043)', () => {
    cy.criarPedidoViaApi().then((pedido) => {
      cy.apiRequest('GET', `/pedidos/${pedido.id}`).its('body.itens.0.id').then((itemId) => {
        const corpo = { itens: [{ itemPedidoId: itemId, quantidade: 1 }], motivoCliente: 'Defeito' };
        cy.apiRequest('POST', `/pedidos/${pedido.id}/trocas`, corpo).then((r) => {
          expect(r.status).to.eq(409);
          expect(r.body.mensagem).to.contain('ENTREGUE');
        });
      });
    });
  });

  it('CTV121 — uma troca só pode ser decidida uma vez e o pedido TROCADO não aceita nova troca (RN0042, RN0043)', () => {
    cy.solicitarTrocaViaApi(1, [{ itemPedidoId: 1, quantidade: 1 }]).then((troca) => {
      cy.autorizarTrocaViaApi(troca.id);
      cy.apiRequest('PATCH', `/trocas/${troca.id}/autorizar`).its('status').should('eq', 409); // já autorizada
      cy.confirmarRecebimentoTrocaViaApi(troca.id, false);
      cy.apiRequest('GET', '/pedidos/1').its('body.status').should('eq', 'TROCADO');
      cy.apiRequest('POST', '/pedidos/1/trocas', { itens: [{ itemPedidoId: 1, quantidade: 1 }], motivoCliente: 'x' })
        .its('status').should('eq', 409);
    });
  });

  it('CTV122 — pedido ou troca inexistente devolve 404', () => {
    cy.apiRequest('GET', '/pedidos/999').its('status').should('eq', 404);
    cy.apiRequest('PATCH', '/pedidos/999/despachar').its('status').should('eq', 404);
    cy.apiRequest('PATCH', '/trocas/999/autorizar').its('status').should('eq', 404);
  });
});
