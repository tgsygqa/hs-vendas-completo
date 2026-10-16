// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 7) REGRAS DE PAGAMENTO PELA API
//
// Regras que a interface NÃO deixa o usuário violar (a tela já impede), mas que
// o backend precisa garantir sozinho: aqui as requisições são feitas direto na
// API REST, com cy.request. Um teste por requisito.
// Base: DRS_LES_2_2026 — RN0033, RN0034, RN0035, RN0036, RN0037
//
// Massa de dados: João (cliente 1) tem o cartão 1, o endereço 1 e os cupons
// 1 PROMO-BEMVINDO R$ 50, 2 PROMO-VERAO R$ 30, 3 TROCA-JOAO-PEQUENO R$ 20,
// 4 TROCA-JOAO-GRANDE R$ 500. Uma compra de 1 headset vale R$ 374,80.
// ============================================================================
import { PRODUTO } from '../../support/helpers';

const finalizar = (corpo) => cy.apiRequest('POST', '/clientes/1/pedidos', Object.assign({ enderecoId: 1, cartoes: [], cupomIds: [] }, corpo));

describe('Vendas — Regras de pagamento validadas pela API (RN0033 a RN0037)', () => {
  beforeEach(() => {
    cy.resetarBase();
  });

  it('32. não deve aceitar dois cupons promocionais na mesma compra (RN0033)', () => {
    cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);

    finalizar({ cartoes: [{ cartaoId: 1, valor: 294.8 }], cupomIds: [1, 2] }).then((r) => {
      expect(r.status).to.eq(409);
      expect(r.body.mensagem).to.contain('RN0033');
    });
  });

  it('33. deve exigir ao menos R$ 10,00 em cada cartão quando há mais de um cartão e nenhum cupom (RN0034)', () => {
    cy.criarCartaoViaApi(1).then((cartao2) => {
      cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);

      // valor limite: R$ 9,99 no segundo cartão é recusado...
      finalizar({ cartoes: [{ cartaoId: 1, valor: 364.81 }, { cartaoId: cartao2.id, valor: 9.99 }] }).then((r) => {
        expect(r.status).to.eq(409);
        expect(r.body.mensagem).to.contain('RN0034');
      });

      // ...e R$ 10,00 é aceito
      finalizar({ cartoes: [{ cartaoId: 1, valor: 364.8 }, { cartaoId: cartao2.id, valor: 10 }] }).its('status').should('eq', 201);
    });
  });

  it('34. deve permitir que o cartão pague menos de R$ 10,00 quando combinado com cupom (RN0035)', () => {
    cy.criarCartaoViaApi(1).then((cartao2) => {
      cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);

      finalizar({ cartoes: [{ cartaoId: 1, valor: 319.8 }, { cartaoId: cartao2.id, valor: 5 }], cupomIds: [1] }).then((r) => {
        expect(r.status).to.eq(201);
        expect(r.body.status).to.eq('EM_PROCESSAMENTO');
      });
    });
  });

  it('35. não deve permitir cupons desnecessários e deve gerar um cupom de troco quando o cupom supera a compra (RN0036)', () => {
    cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);

    // R$ 500 já cobrem os R$ 374,80; o de R$ 20 seria desnecessário
    finalizar({ cupomIds: [4, 3] }).then((r) => {
      expect(r.status).to.eq(409);
      expect(r.body.mensagem).to.contain('RN0036');
    });

    // só o de R$ 500: a compra é aceita e a diferença (500 − 374,80) volta como cupom
    finalizar({ cupomIds: [4] }).its('status').should('eq', 201);
    cy.apiRequest('GET', '/clientes/1/cupons').its('body').then((cupons) => {
      const troco = cupons.find((c) => c.codigo.startsWith('TROCO-'));
      expect(troco, 'cupom de troco').to.exist;
      expect(troco.valor).to.eq(125.2);
    });
  });

  it('36. deve validar a forma de pagamento, recusando um cupom já utilizado (RN0037)', () => {
    cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);
    finalizar({ cartoes: [{ cartaoId: 1, valor: 324.8 }], cupomIds: [1] }).its('status').should('eq', 201);

    cy.adicionarAoCarrinhoViaApi(1, PRODUTO.HEADSET, 1);
    finalizar({ cartoes: [{ cartaoId: 1, valor: 324.8 }], cupomIds: [1] }).then((r) => {
      expect(r.status).to.eq(409);
      expect(r.body.mensagem).to.contain('já foi utilizado');
    });
  });
});
