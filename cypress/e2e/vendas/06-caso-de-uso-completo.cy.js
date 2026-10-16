// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 6) CASO DE USO COMPLETO (CDU02)
//
// Percorre, pela interface, TODO o caso de uso "Gerenciar Venda Eletrônica" —
// é o arquivo para rodar ao vivo na apresentação. Cada etapa é um teste, com os
// requisitos que ela verifica no título (como no módulo de Clientes):
//
//   22  Cliente : monta o carrinho
//   23  Cliente : endereço + cupom + cartão -> finaliza a compra
//   24  Admin   : valida o pagamento (APROVADA + baixa em estoque)
//   25  Admin   : despacha            26  Admin : confirma a entrega
//   27  Cliente : solicita a troca    28  Admin : visualiza e autoriza
//   29  Cliente : vê a notificação    30  Admin : confirma o recebimento
//   31  Cliente : vê o cupom de troca e o usa numa nova compra
//
// ATENÇÃO — os testes deste arquivo SÃO SEQUENCIAIS: cada etapa continua de onde
// a anterior parou (o pedido #3 vai mudando de status no backend). Por isso a
// base é zerada UMA vez, antes da primeira etapa, e o arquivo deve rodar inteiro
// e em ordem (é o que o Cypress faz ao abrir o arquivo). Sem reteste automático:
// refazer uma etapa isolada, com o pedido já adiante, não faria sentido.
//
// Requisitos exercitados: RF0031 a RF0045, RF0053, RF0054, RN0038 a RN0042,
// RN0044, RN0046.
// Ficam para os testes dedicados (arquivos 01 a 05 e 07), porque o caminho feliz
// do fluxo não passa por eles: RN0031, RN0032, RN0033 a RN0037, RN0043, RN0045,
// RNF0042 e o ramo REPROVADA da RN0038.
// ============================================================================
import {
  PRODUTO, FRETE, brl, abrirLoja, adicionarNaLoja, irParaCheckout, escolherPrimeiroEndereco, pagarComCartao,
  escolherCupom, confirmarPagamento, abrirPedidos, cardPedido, abrirAdmin, linhaPedido, abrirAbaTrocas,
  abrirAbaEstoque, estoqueFisico,
} from '../../support/helpers';

// valores da compra principal: 2 headsets + 1 teclado + frete
const produtos = 2 * 349.9 + 459.9;   // R$ 1.159,70
const total = produtos + FRETE;        // R$ 1.184,60
const noCartao = total - 50;           // R$ 1.134,60 (depois do cupom promocional de R$ 50)

describe('Vendas — Caso de uso completo (CDU02), do carrinho à troca (RF0031 a RF0045, RF0053, RF0054, RN0038 a RN0042, RN0044, RN0046)', { retries: 0 }, () => {
  before(() => {
    cy.resetarBase();
  });

  // ------------------------------------------------------------------ CLIENTE: carrinho
  it('22. deve montar o carrinho com 2 headsets e 1 teclado, com os itens bloqueados por 30 minutos (RF0031, RF0032, RN0044)', () => {
    abrirLoja();
    adicionarNaLoja(PRODUTO.HEADSET, 2);
    adicionarNaLoja(PRODUTO.TECLADO, 1);
    cy.get('[data-cy=carrinho-item]').should('have.length', 2);
    cy.get('[data-cy=carrinho-total]').should('have.text', brl(produtos));
    cy.get('[data-cy=bloqueio-contador]').should('be.visible'); // itens bloqueados por 30 min
  });

  // ------------------------------------------------------------------ CLIENTE: entrega, pagamento e finalização
  it('23. deve escolher o endereço, pagar com cupom promocional e cartão, ver o frete somado ao total e finalizar a compra com o pedido EM PROCESSAMENTO (RF0033 a RF0038)', () => {
    abrirLoja();
    cy.get('[data-cy=carrinho-item]').should('have.length', 2); // o carrinho continua no backend
    irParaCheckout();
    escolherPrimeiroEndereco();
    escolherCupom('PROMO-BEMVINDO');
    pagarComCartao();
    cy.get('[data-cy=frete-valor]').should('have.text', brl(FRETE));
    cy.get('[data-cy=total-compra]').should('have.text', brl(total));
    cy.get('[data-cy=valor-auto-cartao]').should('contain.text', brl(noCartao));
    cy.get('[data-cy=valor-restante]').should('have.text', brl(0));

    confirmarPagamento();
    cy.get('[data-cy=pedido-id]').should('contain.text', '#3');
    cy.get('[data-cy=pedido-status]').should('have.attr', 'data-status', 'EM_PROCESSAMENTO');
    cy.get('[data-cy=pedido-total]').should('have.text', brl(total));
  });

  // ------------------------------------------------------------------ ADMIN: pagamento, despacho e entrega
  it('24. deve validar o pagamento, aprovando o pedido (APROVADA) e dando baixa em estoque (RN0038, RF0053)', () => {
    abrirAdmin();
    linhaPedido(3).should('have.attr', 'data-status', 'EM_PROCESSAMENTO');
    linhaPedido(3).find('[data-cy=btn-validar-pagamento]').click();
    linhaPedido(3).should('have.attr', 'data-status', 'APROVADA');
    abrirAbaEstoque();
    estoqueFisico(PRODUTO.HEADSET).should('have.text', '38'); // 40 − 2
    estoqueFisico(PRODUTO.TECLADO).should('have.text', '29'); // 30 − 1
  });

  it('25. deve despachar o pedido aprovado, passando-o a EM TRANSPORTE (RF0039, RN0039)', () => {
    abrirAdmin();
    linhaPedido(3).find('[data-cy=btn-despachar]').click();
    linhaPedido(3).should('have.attr', 'data-status', 'EM_TRANSPORTE');
  });

  it('26. deve confirmar a entrega, passando o pedido a ENTREGUE (RF0040, RN0040)', () => {
    abrirAdmin();
    linhaPedido(3).find('[data-cy=btn-confirmar-entrega]').click();
    linhaPedido(3).should('have.attr', 'data-status', 'ENTREGUE');
  });

  // ------------------------------------------------------------------ CLIENTE: troca
  it('27. deve solicitar a troca de 1 headset do pedido entregue, passando-o a EM TROCA (RF0041, RN0041)', () => {
    abrirPedidos();
    cardPedido(3).should('have.attr', 'data-status', 'ENTREGUE');
    cardPedido(3).find('[data-cy=btn-solicitar-troca]').click();
    cy.contains('[data-cy=item-troca]', 'Headset').find('[data-cy=chk-item-troca]').check();
    cy.contains('[data-cy=item-troca]', 'Headset').find('[data-cy=input-qtd-troca]').clear().type('1');
    cardPedido(3).find('[data-cy=i-motivo-troca]').type('Falha no microfone do headset');
    cardPedido(3).find('[data-cy=btn-enviar-troca]').click();
    cardPedido(3).should('have.attr', 'data-status', 'EM_TROCA');
  });

  // ------------------------------------------------------------------ ADMIN: autorização
  it('28. deve visualizar a solicitação de troca e autorizá-la (RF0042, RF0043)', () => {
    abrirAdmin();
    abrirAbaTrocas();
    cy.get('[data-cy=troca-row]').should('have.length', 1).and('contain.text', 'Falha no microfone');
    cy.get('[data-cy=btn-autorizar-troca]').click();
    cy.get('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'TROCA_AUTORIZADA');
  });

  // ------------------------------------------------------------------ CLIENTE: notificação
  it('29. deve exibir ao cliente a notificação de que a troca foi autorizada (RN0046)', () => {
    abrirPedidos();
    cy.get('[data-cy=notificacao-texto]').should('contain.text', 'AUTORIZADA');
    cardPedido(3).should('have.attr', 'data-status', 'TROCA_AUTORIZADA');
  });

  // ------------------------------------------------------------------ ADMIN: recebimento
  it('30. deve confirmar o recebimento, devolvendo o item ao estoque, gerando o cupom de troca e passando o pedido a TROCADO (RF0044, RF0045, RF0054, RN0042)', () => {
    abrirAdmin();
    abrirAbaTrocas();
    cy.get('[data-cy=btn-recebimento-com-estoque]').click();
    cy.get('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'TROCADO');
    cy.get('[data-cy=troca-cupom]').should('have.text', 'TROCA-P3-S1');
    abrirAbaEstoque();
    estoqueFisico(PRODUTO.HEADSET).should('have.text', '39'); // 38 + 1 devolvido
  });

  // ------------------------------------------------------------------ CLIENTE: cupom de troca
  it('31. deve exibir o cupom de troca ao cliente e permitir usá-lo numa nova compra (RF0045, RF0037)', () => {
    abrirPedidos();
    cardPedido(3).should('have.attr', 'data-status', 'TROCADO');
    cy.contains('[data-cy=cupom-row]', 'TROCA-P3-S1').should('contain.text', brl(349.9));

    abrirLoja();
    adicionarNaLoja(PRODUTO.TECLADO, 1);
    irParaCheckout();
    escolherPrimeiroEndereco();
    escolherCupom('TROCA-P3-S1');
    pagarComCartao();
    confirmarPagamento();
    cy.get('[data-cy=pedido-id]').should('contain.text', '#4');
    cy.get('[data-cy=pedido-status]').should('have.attr', 'data-status', 'EM_PROCESSAMENTO');
  });
});
