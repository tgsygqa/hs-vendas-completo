// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 3) FINALIZAR COMPRA (ENTREGA E PAGAMENTO)
// Base: DRS_LES_2_2026 — RF0033 a RF0038, RN0023 a RN0025 (endereço/cartão
// novos), RN0032, RN0033, RN0035, RN0036
//
// Massa de dados: João Silva (cliente 1) tem 1 endereço ("Casa"), 1 cartão
// (VISA final 4242) e 4 cupons: PROMO-BEMVINDO R$ 50 e PROMO-VERAO R$ 30
// (promocionais), TROCA-JOAO-PEQUENO R$ 20 e TROCA-JOAO-GRANDE R$ 500 (troca).
// ============================================================================
import {
  PRODUTO, FRETE, brl, abrirLoja, adicionarNaLoja, irParaCheckout, escolherPrimeiroEndereco,
  pagarComCartao, escolherCupom, confirmarPagamento, abrirAdmin, linhaPedido,
} from '../../support/helpers';

const HEADSET = 349.9;                       // preço unitário
const TOTAL_1_HEADSET = HEADSET + FRETE;     // R$ 374,80

describe('Vendas — Finalizar compra (RF0033 a RF0038, RN0032, RN0033, RN0036)', () => {
  beforeEach(() => {
    cy.resetarBase();
    abrirLoja();
  });

  it('CTV040 — deve finalizar a compra pagando com cartão e gerar o pedido EM PROCESSAMENTO (RF0033, RF0036, RF0038)', () => {
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

  it('CTV041 — o frete é calculado e somado ao total da compra (RF0034)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();

    cy.get('[data-cy=frete-valor]').should('have.text', brl(FRETE));
    cy.get('[data-cy=total-compra]').should('have.text', brl(TOTAL_1_HEADSET));
  });

  it('CTV042 — a compra exige um endereço de entrega selecionado (RF0035)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();
    pagarComCartao();

    confirmarPagamento();

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'Selecione um endereço de entrega');
    cy.get('[data-cy=pedido-id]').should('not.exist');
  });

  it('CTV043 — a compra não é finalizada se a forma de pagamento não cobre o valor total (RF0036, RF0037)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();
    escolherPrimeiroEndereco();
    cy.get('[data-cy=valor-restante]').should('have.text', brl(TOTAL_1_HEADSET)); // nada selecionado ainda

    confirmarPagamento();

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'cobrir exatamente');
    cy.get('[data-cy=pedido-id]').should('not.exist');
  });

  it('CTV044 — deve cadastrar um novo endereço durante a compra e incorporá-lo ao perfil do cliente (RF0035, RN0023)', () => {
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

  it('CTV045 — não deve salvar um novo endereço com campos obrigatórios vazios (RN0023)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();

    cy.get('[data-cy=btn-novo-endereco]').click();
    cy.get('[data-cy=i-ne-apelido]').type('Incompleto');
    cy.get('[data-cy=btn-salvar-endereco]').click();

    cy.get('[data-cy=msg-erro]').should('be.visible');
    cy.get('[data-cy=radio-endereco-card]').should('have.length', 1);
  });

  it('CTV046 — deve cadastrar um novo cartão durante a compra e incorporá-lo ao perfil do cliente (RF0036, RN0024)', () => {
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

  it('CTV047 — as bandeiras American Express e Hipercard são aceitas no cadastro de cartão (RN0025)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();

    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-nc-numero]').type('371449635398431');
    cy.get('[data-cy=i-nc-bandeira]').select('AMERICAN_EXPRESS');
    cy.get('[data-cy=i-nc-titular]').type('JOAO SILVA');
    cy.get('[data-cy=i-nc-cvv]').type('1234');
    cy.get('[data-cy=btn-salvar-cartao]').click();
    cy.get('[data-cy=pagamento-cartao-row]').should('have.length', 2);
    cy.contains('[data-cy=pagamento-cartao-row]', 'AMERICAN_EXPRESS •••• 8431').should('be.visible');

    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-nc-numero]').type('6062826786276634');
    cy.get('[data-cy=i-nc-bandeira]').select('HIPERCARD');
    cy.get('[data-cy=i-nc-titular]').type('JOAO SILVA');
    cy.get('[data-cy=i-nc-cvv]').type('123');
    cy.get('[data-cy=btn-salvar-cartao]').click();
    cy.get('[data-cy=pagamento-cartao-row]').should('have.length', 3);
    cy.contains('[data-cy=pagamento-cartao-row]', 'HIPERCARD •••• 6634').should('be.visible');
  });

  it('CTV048 — não deve salvar cartão com número inválido (RN0024)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();

    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-nc-numero]').type('123');
    cy.get('[data-cy=i-nc-titular]').type('JOAO SILVA');
    cy.get('[data-cy=i-nc-cvv]').type('321');
    cy.get('[data-cy=btn-salvar-cartao]').click();

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'Numero de cartao invalido');
    cy.get('[data-cy=pagamento-cartao-row]').should('have.length', 1);
  });

  it('CTV057 — compra paga com um cartão cadastrado na hora: a operadora recusa o final 0000 e o pedido é REPROVADO (RF0036, RN0038)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();
    escolherPrimeiroEndereco();

    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-nc-numero]').type('4111111111110000');
    cy.get('[data-cy=i-nc-bandeira]').select('VISA');
    cy.get('[data-cy=i-nc-titular]').type('JOAO SILVA');
    cy.get('[data-cy=i-nc-cvv]').type('123');
    cy.get('[data-cy=btn-salvar-cartao]').click();
    cy.contains('[data-cy=pagamento-cartao-row]', '•••• 0000').find('[data-cy=chk-cartao]').should('be.checked');

    confirmarPagamento();
    cy.get('[data-cy=pedido-status]').should('have.attr', 'data-status', 'EM_PROCESSAMENTO');

    abrirAdmin();
    linhaPedido(3).find('[data-cy=btn-validar-pagamento]').click();
    linhaPedido(3).should('have.attr', 'data-status', 'REPROVADA');
    cy.get('[data-cy=msg-sucesso]').should('contain.text', 'REPROVADO');
  });

  it('CTV049 — a compra pode ser paga com mais de um cartão, dividindo o valor entre eles (RF0036, RN0034)', () => {
    cy.criarCartaoViaApi(1); // MASTERCARD final 8888 (o 2º cartão do João)
    cy.reload();
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();
    escolherPrimeiroEndereco();

    pagarComCartao(0);
    pagarComCartao(1);

    cy.get('[data-cy=valor-auto-cartao]').should('have.length', 2);
    cy.get('[data-cy=valor-auto-cartao]').first().should('contain.text', brl(TOTAL_1_HEADSET / 2)); // R$ 187,40 em cada
    cy.get('[data-cy=valor-restante]').should('have.text', brl(0));
    confirmarPagamento();

    cy.get('[data-cy=pedido-status]').should('have.attr', 'data-status', 'EM_PROCESSAMENTO');
    cy.apiRequest('GET', '/pedidos/3').its('body.pagamentos').should('have.length', 2);
  });

  it('CTV050 — só é possível escolher um cupom promocional por compra; escolher outro troca o anterior (RN0033)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();

    escolherCupom('PROMO-BEMVINDO');
    cy.get('[data-cy=cupom-desconto]').should('contain.text', brl(50));

    escolherCupom('PROMO-VERAO');
    cy.get('[data-cy=cupom-desconto]').should('contain.text', brl(30));
    cy.get('[data-cy=radio-cupom]:checked').should('have.length', 1);
  });

  it('CTV051 — deve pagar com um cupom promocional e completar com cartão; o cupom some da lista (RF0037, RN0033)', () => {
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

  it('CTV052 — cupom de troca maior que a compra paga tudo sozinho e gera um cupom de troco (RN0036)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();
    escolherPrimeiroEndereco();

    escolherCupom('TROCA-JOAO-GRANDE'); // R$ 500,00 para uma compra de R$ 374,80
    cy.get('[data-cy=valor-restante]').should('have.text', brl(0));
    confirmarPagamento();
    cy.get('[data-cy=pedido-status]').should('have.attr', 'data-status', 'EM_PROCESSAMENTO');

    // o troco (500 − 374,80 = 125,20) vira um novo cupom do cliente
    cy.get('[data-cy=btn-ver-pedidos]').click();
    cy.get('[data-cy=cupom-troco-gerado]').should('contain.text', brl(125.2));
    cy.contains('[data-cy=cupom-row]', 'TROCO-').should('contain.text', brl(125.2));
  });

  it('CTV053 — um mesmo cupom não pode ser usado em duas compras (RF0037)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();
    escolherPrimeiroEndereco();
    escolherCupom('PROMO-BEMVINDO');
    pagarComCartao();
    confirmarPagamento();
    cy.get('[data-cy=pedido-id]').should('be.visible');

    cy.get('[data-cy=btn-nova-compra]').click();
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();

    cy.get('[data-cy=cupom-row]').should('have.length', 3); // sobraram 3 dos 4 cupons
    cy.get('[data-cy=cupom-row]').should('not.contain.text', 'PROMO-BEMVINDO');
  });

  it('CTV054 — se o estoque mudar antes da finalização, a quantidade é ajustada automaticamente com um alerta (RN0032)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 5);
    irParaCheckout();
    escolherPrimeiroEndereco();
    pagarComCartao();
    cy.definirEstoque(PRODUTO.HEADSET, 3); // outro cliente comprou no meio tempo

    confirmarPagamento();

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'ajustada de 5 para 3').and('contain.text', 'RN0032');
    cy.get('[data-cy=qtd-item]').should('have.text', '3'); // o carrinho já foi corrigido
    cy.get('[data-cy=pedido-id]').should('not.exist');
  });

  it('CTV055 — se o item ficar indisponível antes da finalização, ele é removido do carrinho com um alerta (RN0032)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 2);
    irParaCheckout();
    escolherPrimeiroEndereco();
    pagarComCartao();
    cy.definirEstoque(PRODUTO.HEADSET, 0);

    confirmarPagamento();

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'ficou indisponível').and('contain.text', 'RN0032');
    cy.get('[data-cy=carrinho-vazio]').should('be.visible');
  });

  it('CTV056 — depois de finalizar a compra o carrinho fica vazio (RF0038)', () => {
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();
    escolherPrimeiroEndereco();
    pagarComCartao();
    confirmarPagamento();
    cy.get('[data-cy=pedido-id]').should('be.visible');

    cy.get('[data-cy=btn-nova-compra]').click();

    cy.get('[data-cy=carrinho-vazio]').should('be.visible');
    cy.get('[data-cy=cart-pill-count]').should('have.text', '0');
  });
});
