// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS — VENDAS / 2) BLOQUEIO E EXPIRAÇÃO DO CARRINHO
// Base: DRS_LES_2_2026 — RN0044, RN0045, RNF0042
//
// Para não esperar 30 minutos de verdade, os testes ajustam o prazo do
// bloqueio pelo endpoint de apoio (cy.definirBloqueio) e disparam o job de
// expiração na hora (cy.varrerExpirados).
// ============================================================================
import { PRODUTO, abrirLoja, adicionarNaLoja, irParaCheckout, escolherPrimeiroEndereco, pagarComCartao, confirmarPagamento } from '../../support/helpers';

describe('Vendas — Bloqueio e expiração dos itens do carrinho (RN0044, RN0045, RNF0042)', () => {
  beforeEach(() => {
    cy.resetarBase();
  });

  it('CTV020 — itens no carrinho ficam bloqueados por um prazo, exibido em um contador (RN0044)', () => {
    abrirLoja();
    adicionarNaLoja(PRODUTO.HEADSET, 1);

    cy.get('[data-cy=bloqueio-contador]').invoke('text').should('match', /^(29|30):\d{2}$/); // prazo padrão: 30 min
    cy.get('[data-cy=aviso-expiracao]').should('not.be.visible'); // ainda longe dos 5 minutos finais
  });

  it('CTV021 — o item bloqueado no carrinho de um cliente fica indisponível para os outros (RN0044)', () => {
    cy.criarClienteViaApi().then((maria) => {
      cy.adicionarAoCarrinhoViaApi(1, PRODUTO.RTX, 12); // João bloqueia as 12 placas

      abrirLoja(maria.id);

      cy.get(`[data-cy=produto-row][data-id="${PRODUTO.RTX}"] [data-cy=produto-estoque]`).should('contain.text', 'reservado');
      cy.get(`[data-cy=produto-row][data-id="${PRODUTO.RTX}"] [data-cy=btn-adicionar-carrinho]`).should('be.disabled');
      // os demais produtos seguem livres
      cy.get(`[data-cy=produto-row][data-id="${PRODUTO.RYZEN}"] [data-cy=btn-adicionar-carrinho]`).should('not.be.disabled');
    });
  });

  it('CTV022 — quando o bloqueio expira, o estoque volta a ficar disponível para os outros clientes (RN0044)', () => {
    cy.criarClienteViaApi().then((maria) => {
      cy.adicionarAoCarrinhoViaApi(1, PRODUTO.RTX, 12);
      cy.definirBloqueio(1, -5); // prazo já vencido
      cy.varrerExpirados();

      abrirLoja(maria.id);

      cy.get(`[data-cy=produto-row][data-id="${PRODUTO.RTX}"] [data-cy=produto-estoque]`).should('contain.text', '12 em estoque');
      cy.get(`[data-cy=produto-row][data-id="${PRODUTO.RTX}"] [data-cy=btn-adicionar-carrinho]`).should('not.be.disabled');
    });
  });

  it('CTV023 — o cliente é avisado quando faltam 5 minutos ou menos para o bloqueio expirar (RN0044)', () => {
    abrirLoja();
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    cy.definirBloqueio(1, 240); // faltam 4 minutos
    cy.reload();

    cy.get('[data-cy=aviso-expiracao]').should('be.visible').and('contain.text', 'liberados para outros clientes');
    cy.get('[data-cy=aviso-expiracao-tempo]').invoke('text').should('match', /^0[34]:\d{2}$/);

    // o tempo do aviso corre sozinho, segundo a segundo
    cy.get('[data-cy=aviso-expiracao-tempo]').invoke('text').then((antes) => {
      cy.wait(2200);
      cy.get('[data-cy=aviso-expiracao-tempo]').invoke('text').should('not.eq', antes);
    });
  });

  it('CTV024 — adicionar um novo item renova o prazo de todos (prazo relativo ao último item) (RN0044)', () => {
    abrirLoja();
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    cy.definirBloqueio(1, 240);
    cy.reload();
    cy.get('[data-cy=aviso-expiracao]').should('be.visible');

    adicionarNaLoja(PRODUTO.TECLADO, 1);

    cy.get('[data-cy=aviso-expiracao]').should('not.be.visible'); // prazo voltou para ~30 min
    cy.get('[data-cy=bloqueio-contador]').invoke('text').should('match', /^(29|30):\d{2}$/);
  });

  it('CTV025 — itens com bloqueio vencido saem do carrinho e continuam listados como expirados; comprar fica desabilitado (RN0045, RNF0042)', () => {
    abrirLoja();
    adicionarNaLoja(PRODUTO.HEADSET, 2);
    cy.definirBloqueio(1, -5);
    cy.varrerExpirados();
    cy.reload();

    cy.get('[data-cy=carrinho-vazio]').should('be.visible'); // RN0045: o item saiu do carrinho
    cy.get('[data-cy=aviso-itens-expirados]').should('be.visible');
    cy.get('[data-cy=item-expirado]').should('have.length', 1).and('contain.text', 'Headset Gamer Pro X').and('contain.text', '2 un.');
    cy.get('[data-cy=btn-iniciar-compra]').should('be.disabled'); // RNF0042
  });

  it('CTV026 — o item expirado deve ser adicionado novamente ao carrinho, o que reabilita a compra (RNF0042)', () => {
    abrirLoja();
    adicionarNaLoja(PRODUTO.HEADSET, 2);
    cy.definirBloqueio(1, -5);
    cy.varrerExpirados();
    cy.reload();
    cy.get('[data-cy=item-expirado]').should('have.length', 1);

    cy.get('[data-cy=btn-readicionar-expirado]').click();

    cy.get('[data-cy=item-expirado]').should('not.exist');
    cy.get('[data-cy=carrinho-item]').should('have.length', 1).and('contain.text', 'Headset Gamer Pro X');
    cy.get('[data-cy=qtd-item]').should('have.text', '2');
    cy.get('[data-cy=btn-iniciar-compra]').should('not.be.disabled');
  });

  it('CTV027 — com a tela aberta, o carrinho percebe sozinho que o prazo acabou (RN0044, RNF0042)', () => {
    abrirLoja();
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    cy.definirBloqueio(1, 2); // vence em 2 segundos
    cy.reload();

    // sem nenhuma ação do usuário: o contador zera e o item passa a "expirado"
    cy.get('[data-cy=item-expirado]', { timeout: 15000 }).should('be.visible');
    cy.get('[data-cy=carrinho-vazio]').should('be.visible');
    cy.get('[data-cy=btn-iniciar-compra]').should('be.disabled');
  });

  it('CTV028 — o aviso de item expirado pode ser dispensado sem readicionar (RNF0042)', () => {
    abrirLoja();
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    cy.definirBloqueio(1, -5);
    cy.varrerExpirados();
    cy.reload();

    cy.get('[data-cy=btn-dispensar-expirado]').click();

    cy.get('[data-cy=item-expirado]').should('not.exist');
    cy.get('[data-cy=carrinho-vazio]').should('be.visible');
  });

  it('CTV029 — readicionar um item expirado respeita o estoque se outro cliente o pegou nesse meio tempo (RN0031, RN0044)', () => {
    cy.criarClienteViaApi().then((maria) => {
      abrirLoja();
      adicionarNaLoja(PRODUTO.RTX, 12);
      cy.definirBloqueio(1, -5);
      cy.varrerExpirados();
      cy.adicionarAoCarrinhoViaApi(maria.id, PRODUTO.RTX, 12); // Maria leva as 12 placas
      cy.reload();

      cy.get('[data-cy=btn-readicionar-expirado]').click();

      cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'Estoque insuficiente');
      cy.get('[data-cy=item-expirado]').should('have.length', 1); // continua pendente
    });
  });

  it('CTV030 — ao finalizar com o bloqueio vencido, a compra é recusada e o carrinho mostra o item expirado (RN0044, RNF0042)', () => {
    abrirLoja();
    adicionarNaLoja(PRODUTO.HEADSET, 1);
    irParaCheckout();
    escolherPrimeiroEndereco();
    pagarComCartao();
    cy.definirBloqueio(1, -5); // o prazo vence enquanto o cliente preenche o pagamento

    confirmarPagamento();

    cy.get('[data-cy=msg-erro]').should('be.visible').and('contain.text', 'prazo de bloqueio expirou');
    cy.get('[data-cy=item-expirado]').should('have.length', 1);
    cy.get('[data-cy=btn-iniciar-compra]').should('be.disabled');
  });
});
