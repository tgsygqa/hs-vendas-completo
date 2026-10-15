// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 6) CASO DE USO COMPLETO (CDU02)
//
// UM único teste que percorre, pela interface, TODO o caso de uso "Gerenciar
// Venda Eletrônica" — é o teste para rodar ao vivo na apresentação:
//
//   Cliente  : carrinho -> endereço -> pagamento (cupom + cartão) -> finalizar
//   Admin    : validar pagamento -> despachar -> confirmar entrega
//   Cliente  : solicitar troca
//   Admin    : autorizar -> confirmar recebimento (item volta ao estoque)
//   Cliente  : notificação + cupom de troca -> usa o cupom numa nova compra
//
// Requisitos exercitados: RF0031-RF0045, RF0053, RF0054, RN0031, RN0033,
// RN0037-RN0043, RN0044, RN0046.
// ============================================================================
import {
  PRODUTO, FRETE, brl, abrirLoja, adicionarNaLoja, irParaCheckout, escolherPrimeiroEndereco, pagarComCartao,
  escolherCupom, confirmarPagamento, abrirPedidos, cardPedido, abrirAdmin, linhaPedido, abrirAbaTrocas,
  abrirAbaEstoque, estoqueFisico,
} from '../../support/helpers';

describe('Vendas — Caso de uso completo (CDU02), do carrinho à troca', () => {
  beforeEach(() => {
    cy.resetarBase();
  });

  it('CTV100 — compra, aprovação, despacho, entrega, troca, recebimento e uso do cupom de troca', () => {
    // valores da compra principal: 2 headsets + 1 teclado + frete
    const produtos = 2 * 349.9 + 459.9;                 // R$ 1.159,70
    const total = produtos + FRETE;                      // R$ 1.184,60
    const noCartao = total - 50;                         // R$ 1.134,60 (depois do cupom promocional de R$ 50)

    // ---------------------------------------------------------------- 1. CLIENTE: monta o carrinho
    cy.log('**1. Cliente monta o carrinho (RF0031, RF0032, RN0031, RN0044)**');
    abrirLoja();
    adicionarNaLoja(PRODUTO.HEADSET, 2);
    adicionarNaLoja(PRODUTO.TECLADO, 1);
    cy.get('[data-cy=carrinho-item]').should('have.length', 2);
    cy.get('[data-cy=carrinho-total]').should('have.text', brl(produtos));
    cy.get('[data-cy=bloqueio-contador]').should('be.visible'); // itens bloqueados por 30 min

    // ---------------------------------------------------------------- 2. CLIENTE: entrega e pagamento
    cy.log('**2. Cliente escolhe endereço e paga com cupom + cartão (RF0033-RF0037, RN0033)**');
    irParaCheckout();
    escolherPrimeiroEndereco();
    escolherCupom('PROMO-BEMVINDO');
    pagarComCartao();
    cy.get('[data-cy=frete-valor]').should('have.text', brl(FRETE));
    cy.get('[data-cy=total-compra]').should('have.text', brl(total));
    cy.get('[data-cy=valor-auto-cartao]').should('contain.text', brl(noCartao));
    cy.get('[data-cy=valor-restante]').should('have.text', brl(0));

    // ---------------------------------------------------------------- 3. CLIENTE: finaliza
    cy.log('**3. Compra finalizada: pedido EM PROCESSAMENTO (RF0038)**');
    confirmarPagamento();
    cy.get('[data-cy=pedido-id]').should('contain.text', '#3');
    cy.get('[data-cy=pedido-status]').should('have.attr', 'data-status', 'EM_PROCESSAMENTO');
    cy.get('[data-cy=pedido-total]').should('have.text', brl(total));

    // ---------------------------------------------------------------- 4. ADMIN: valida o pagamento
    cy.log('**4. Administrador valida o pagamento: APROVADA + baixa em estoque (RN0037, RN0038, RF0053)**');
    abrirAdmin();
    linhaPedido(3).should('have.attr', 'data-status', 'EM_PROCESSAMENTO');
    linhaPedido(3).find('[data-cy=btn-validar-pagamento]').click();
    linhaPedido(3).should('have.attr', 'data-status', 'APROVADA');
    abrirAbaEstoque();
    estoqueFisico(PRODUTO.HEADSET).should('have.text', '38'); // 40 − 2
    estoqueFisico(PRODUTO.TECLADO).should('have.text', '29'); // 30 − 1

    // ---------------------------------------------------------------- 5. ADMIN: despacha e entrega
    cy.log('**5. Administrador despacha e confirma a entrega (RF0039, RF0040, RN0039, RN0040)**');
    cy.get('[data-cy=tab-pedidos]').click();
    linhaPedido(3).find('[data-cy=btn-despachar]').click();
    linhaPedido(3).should('have.attr', 'data-status', 'EM_TRANSPORTE');
    linhaPedido(3).find('[data-cy=btn-confirmar-entrega]').click();
    linhaPedido(3).should('have.attr', 'data-status', 'ENTREGUE');

    // ---------------------------------------------------------------- 6. CLIENTE: solicita a troca
    cy.log('**6. Cliente solicita a troca de 1 headset: pedido EM TROCA (RF0041, RN0041, RN0043)**');
    abrirPedidos();
    cardPedido(3).should('have.attr', 'data-status', 'ENTREGUE');
    cardPedido(3).find('[data-cy=btn-solicitar-troca]').click();
    cy.contains('[data-cy=item-troca]', 'Headset').find('[data-cy=chk-item-troca]').check();
    cy.contains('[data-cy=item-troca]', 'Headset').find('[data-cy=input-qtd-troca]').clear().type('1');
    cardPedido(3).find('[data-cy=i-motivo-troca]').type('Falha no microfone do headset');
    cardPedido(3).find('[data-cy=btn-enviar-troca]').click();
    cardPedido(3).should('have.attr', 'data-status', 'EM_TROCA');

    // ---------------------------------------------------------------- 7. ADMIN: autoriza
    cy.log('**7. Administrador visualiza e autoriza a troca (RF0043, RF0042)**');
    abrirAdmin();
    abrirAbaTrocas();
    cy.get('[data-cy=troca-row]').should('have.length', 1).and('contain.text', 'Falha no microfone');
    cy.get('[data-cy=btn-autorizar-troca]').click();
    cy.get('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'TROCA_AUTORIZADA');

    // ---------------------------------------------------------------- 8. CLIENTE: notificação
    cy.log('**8. Cliente é notificado da autorização (RN0046)**');
    abrirPedidos();
    cy.get('[data-cy=notificacao-texto]').should('contain.text', 'AUTORIZADA');
    cardPedido(3).should('have.attr', 'data-status', 'TROCA_AUTORIZADA');

    // ---------------------------------------------------------------- 9. ADMIN: recebimento
    cy.log('**9. Administrador confirma o recebimento; item volta ao estoque e o cupom é gerado (RF0044, RF0054, RN0042, RF0045)**');
    abrirAdmin();
    abrirAbaTrocas();
    cy.get('[data-cy=btn-recebimento-com-estoque]').click();
    cy.get('[data-cy=troca-situacao]').should('have.attr', 'data-situacao', 'TROCADO');
    cy.get('[data-cy=troca-cupom]').should('have.text', 'TROCA-P3-S1');
    abrirAbaEstoque();
    estoqueFisico(PRODUTO.HEADSET).should('have.text', '39'); // 38 + 1 devolvido

    // ---------------------------------------------------------------- 10. CLIENTE: usa o cupom
    cy.log('**10. Cliente vê o cupom de troca e o usa numa nova compra (RF0045, RF0037)**');
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
