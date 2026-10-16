// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 3) FINALIZAR COMPRA (ENTREGA E PAGAMENTO)
// Base: DRS_LES_2_2026 — RF0033 a RF0038, RN0032 (um teste por requisito)
// As regras de pagamento RN0033 a RN0037 estão em 07-regras-de-negocio-api.cy.js.
//
// Massa de dados: João Silva (cliente 1) tem 1 endereço ("Casa"), 1 cartão
// (VISA final 4242) e 4 cupons: PROMO-BEMVINDO R$ 50 e PROMO-VERAO R$ 30
// (promocionais), TROCA-JOAO-PEQUENO R$ 20 e TROCA-JOAO-GRANDE R$ 500 (troca).
// ============================================================================
import {
  PRODUTO, FRETE, brl, abrirLoja, adicionarNaLoja, irParaCheckout, escolherPrimeiroEndereco,
  pagarComCartao, escolherCupom, confirmarPagamento,
} from '../../support/helpers';

const HEADSET = 349.9;                       // preço unitário
const TOTAL_1_HEADSET = HEADSET + FRETE;     // R$ 374,80

describe('Vendas — Finalizar compra (RF0033 a RF0038, RN0032)', () => {
  beforeEach(() => {
    cy.resetarBase();
    abrirLoja();
  });

  it('CTV006 — deve realizar a compra a partir do carrinho e finalizá-la com o pedido EM PROCESSAMENTO (RF0033, RF0038)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();
    escolherPrimeiroEndereco();
    pagarComCartao();
    cy.get('[data-cy=valor-restante]').should('have.text', brl(0));

    confirmarPagamento();

    cy.get('[data-cy=pedido-id]').should('contain.text', '#3'); // os pedidos #1 e #2 já existem na massa de dados
    cy.get('[data-cy=pedido-status]').should('have.attr', 'data-status', 'EM_PROCESSAMENTO').and('contain.text', 'EM PROCESSAMENTO');
    cy.get('[data-cy=pedido-total]').should('have.text', brl(TOTAL_1_HEADSET));
  });

  it('CTV007 — o frete é calculado e somado ao total da compra (RF0034)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();

    cy.get('[data-cy=frete-valor]').should('have.text', brl(FRETE));
    cy.get('[data-cy=total-compra]').should('have.text', brl(TOTAL_1_HEADSET));
  });

  it('CTV008 — deve cadastrar um novo endereço de entrega durante a compra e incorporá-lo ao perfil do cliente (RF0035)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();

    cy.get('[data-cy=btn-novo-endereco]').click();
    cy.get('[data-cy=i-ne-apelido]').type('Trabalho');
    cy.get('[data-cy=i-ne-tipores]').type('Prédio comercial');
    cy.get('[data-cy=i-ne-tipolog]').type('Avenida');
    cy.get('[data-cy=i-ne-logradouro]').type('Paulista');
    cy.get('[data-cy=i-ne-numero]').type('1000');
    cy.get('[data-cy=i-ne-bairro]').type('Bela Vista');
    cy.get('[data-cy=i-ne-cep]').type('01310-100');
    cy.get('[data-cy=i-ne-cidade]').type('São Paulo');
    cy.get('[data-cy=i-ne-estado]').type('SP');
    cy.get('[data-cy=btn-salvar-endereco]').click();

    cy.get('[data-cy=radio-endereco-card]').should('have.length', 2);
    cy.get('[data-cy=radio-endereco]').eq(1).should('be.checked'); // o novo já vem selecionado
    // incorporado ao perfil: aparece na API do cliente
    cy.apiRequest('GET', '/clientes/1').its('body.enderecos').should('have.length', 2);
  });

  it('CTV009 — deve cadastrar um novo cartão de crédito durante a compra e incorporá-lo ao perfil do cliente (RF0036)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();

    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-nc-numero]').type('5555666677778888');
    cy.get('[data-cy=i-nc-bandeira]').select('MASTERCARD');
    cy.get('[data-cy=i-nc-titular]').type('JOAO SILVA');
    cy.get('[data-cy=i-nc-cvv]').type('321');
    cy.get('[data-cy=btn-salvar-cartao]').click();

    cy.get('[data-cy=pagamento-cartao-row]').should('have.length', 2);
    cy.contains('[data-cy=pagamento-cartao-row]', 'MASTERCARD •••• 8888').find('[data-cy=chk-cartao]').should('be.checked');
    cy.apiRequest('GET', '/clientes/1').its('body.cartoes').should('have.length', 2);
  });

  it('CTV010 — deve pagar com um cupom promocional e completar com cartão de crédito (RF0037)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();
    escolherPrimeiroEndereco();
    escolherCupom('PROMO-BEMVINDO');
    pagarComCartao();

    cy.get('[data-cy=valor-auto-cartao]').should('contain.text', brl(TOTAL_1_HEADSET - 50)); // R$ 324,80 no cartão
    cy.get('[data-cy=valor-restante]').should('have.text', brl(0));
    confirmarPagamento();

    cy.get('[data-cy=pedido-status]').should('have.attr', 'data-status', 'EM_PROCESSAMENTO');
    cy.apiRequest('GET', '/clientes/1/cupons').its('body').then((cupons) => {
      expect(cupons.map((c) => c.codigo)).to.not.include('PROMO-BEMVINDO'); // cupom de uso único
    });
  });

  it('CTV011 — se o estoque mudar antes da finalização, ajusta a quantidade ou remove o item indisponível, com alerta (RN0032)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 5);
    irParaCheckout();
    escolherPrimeiroEndereco();
    pagarComCartao();
    cy.definirEstoque(PRODUTO.HEADSET, 3); // outro cliente comprou no meio tempo

    confirmarPagamento();

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'ajustada de 5 para 3').and('contain.text', 'RN0032');
    cy.get('[data-cy=qtd-item]').should('have.text', '3'); // o carrinho já foi corrigido
    cy.get('[data-cy=pedido-id]').should('not.exist');

    // agora o item some de vez do estoque
    irParaCheckout();
    escolherPrimeiroEndereco();
    pagarComCartao();
    cy.definirEstoque(PRODUTO.HEADSET, 0);

    confirmarPagamento();

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'ficou indisponível').and('contain.text', 'RN0032');
    cy.get('[data-cy=carrinho-vazio]').should('be.visible');
  });
});
