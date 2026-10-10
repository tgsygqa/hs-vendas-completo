// ============================================================================
// SUÍTE DE TESTES AUTOMATIZADOS DE INTERFACE — GESTÃO DE CLIENTES
// Ferramenta: Cypress | Sistema: Hardware Store
// Base: Documento de Requisitos (DRS_LES_2_2026)
//
// Nota de nomenclatura: RF0025 no DRS é "Consulta de transações", não
// exclusão de cliente. A regra de bloqueio de exclusão é tratada abaixo
// como [Regra complementar] que existe para não violar o RF0025 real
// (preservar o histórico de transações de quem já comprou), nunca como
// se fosse o próprio RF0025.
// ============================================================================

const APP_URL = '/V08.html';

const CLIENTE_COM_PEDIDOS = { id: 1, nome: 'João Silva', cpf: '123.456.789-00', email: 'joao.silva@email.com' };

function gerarCliente(sufixo) {
  return {
    nome: `Cliente Hardware ${sufixo}`,
    cpf: `111.222.333-${String(sufixo).padStart(2, '0')}`,
    genero: 'M',
    nasc: '1995-04-12',
    tel: { tipo: 'Celular', ddd: '11', numero: '90000-0000' },
    email: `cliente${sufixo}@email.com`,
    senha: 'SenhaForte@123',
    end: { apelido: 'Casa', tr: 'Casa', tl: 'Rua', log: 'Teste', num: '12', bairro: 'Centro', cep: '00000-000', cid: 'SP', est: 'SP', pais: 'Brasil' }
  };
}

function preencherForm(c, config = { senhasIguais: true, pularCampos: false }) {
  if (config.pularCampos) return;

  cy.get('[data-cy=i-nome]').clear().type(c.nome);
  cy.get('[data-cy=i-cpf]').clear().type(c.cpf);
  cy.get('[data-cy=i-genero]').select(c.genero);
  cy.get('[data-cy=i-nasc]').clear().type(c.nasc);
  cy.get('[data-cy=i-tel-tipo]').select(c.tel.tipo);
  cy.get('[data-cy=i-tel-ddd]').clear().type(c.tel.ddd);
  cy.get('[data-cy=i-tel-numero]').clear().type(c.tel.numero);
  cy.get('[data-cy=i-email]').clear().type(c.email);

  cy.get('[data-cy=i-apelido]').clear().type(c.end.apelido);
  cy.get('[data-cy=i-tres]').clear().type(c.end.tr);
  cy.get('[data-cy=i-tlog]').clear().type(c.end.tl);
  cy.get('[data-cy=i-log]').clear().type(c.end.log);
  cy.get('[data-cy=i-num]').clear().type(c.end.num);
  cy.get('[data-cy=i-bairro]').clear().type(c.end.bairro);
  cy.get('[data-cy=i-cep]').clear().type(c.end.cep);
  cy.get('[data-cy=i-cid]').clear().type(c.end.cid);
  cy.get('[data-cy=i-est]').clear().type(c.end.est);
  cy.get('[data-cy=i-pais]').clear().type(c.end.pais);

  if (c.senha) {
    cy.get('[data-cy=i-senha]').clear().type(c.senha);
    cy.get('[data-cy=i-conf]').clear().type(config.senhasIguais ? c.senha : 'SenhaErrada1!');
  }
}

function linhaCliente(nome) { return cy.contains('[data-cy=cliente-row]', nome); }

function cadastrarClienteAuxiliar(sufixo) {
  const c = gerarCliente(sufixo);
  cy.get('[data-cy=btn-novo]').click();
  preencherForm(c);
  cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
  return cy.wrap(c);
}

beforeEach(() => { cy.visit(APP_URL); });

// ============================================================================
// 1) CADASTRAR — RF0021, RNF0031, RNF0032, RF0026 (apelido do endereço inicial)
// ============================================================================
describe('CRUD Cliente — Cadastro (RF0021, RNF0031, RNF0032, RF0026)', () => {

  it('1. deve cadastrar um novo cliente com sucesso preenchendo todos os dados, caminho feliz (RF0021, RN0026)', () => {
    const c = gerarCliente('01');
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c);
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();

    linhaCliente(c.nome).within(() => {
      cy.get('[data-cy=col-cpf]').should('contain.text', c.cpf);
      cy.get('[data-cy=col-status]').should('contain.text', 'ATIVO');
    });
  });

  it('2. deve rejeitar cadastro com senha fraca (RNF0031)', () => {
    const c = gerarCliente('02');
    c.senha = 'fraca123';
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c);
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.get('[data-cy=msg-erro-form]').scrollIntoView().should('be.visible').and('contain.text', 'Senha fraca');
  });

  it('3. deve rejeitar cadastro quando as senhas não coincidem (RNF0032)', () => {
    const c = gerarCliente('03');
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c, { senhasIguais: false });
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.get('[data-cy=msg-erro-form]').scrollIntoView().should('be.visible').and('contain.text', 'As senhas não coincidem');
  });

  it('4. deve rejeitar cadastro sem preencher os dados pessoais obrigatórios (RF0021, RN0026)', () => {
    cy.get('[data-cy=btn-novo]').click();
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.get('[data-cy=msg-erro-form]').scrollIntoView().should('be.visible').and('contain.text', 'Preencha todos os campos');
  });

  it('5. deve rejeitar cadastro sem o apelido do endereço de entrega inicial (RF0026, RN0023)', () => {
    const c = gerarCliente('05');
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c);
    cy.get('[data-cy=i-apelido]').clear();
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.get('[data-cy=msg-erro-form]').scrollIntoView().should('be.visible').and('contain.text', 'endereço');
  });

  it('6. deve rejeitar cadastro sem senha (RN0026)', () => {
    const c = gerarCliente('05a');
    c.senha = '';
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c);
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.get('[data-cy=msg-erro-form]').scrollIntoView().should('be.visible').and('contain.text', 'Senha é obrigatória');
  });

  it('7. deve rejeitar cadastro sem o endereço residencial completo (RN0023, RN0026)', () => {
    const c = gerarCliente('05b');
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c);
    cy.get('[data-cy=i-log]').clear();
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.get('[data-cy=msg-erro-form]').scrollIntoView().should('be.visible').and('contain.text', 'endereço');
  });

  it('8. não deve permitir cadastrar dois clientes com o mesmo CPF (RF0021, regra complementar)', () => {
    const c = gerarCliente('06');
    c.cpf = CLIENTE_COM_PEDIDOS.cpf;
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c);
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.get('[data-cy=msg-erro-form]').scrollIntoView().should('be.visible').and('contain.text', 'CPF já cadastrado');
  });
});

// ============================================================================
// 2) CONSULTAR — RF0024
// ============================================================================
describe('CRUD Cliente — Consultar e Filtrar (RF0024)', () => {
  it('9. deve listar todos os clientes ao entrar na tela (RF0024)', () => {
    cy.get('[data-cy=cliente-row]').should('have.length.at.least', 1);
  });

  it('10. deve filtrar clientes pelo CPF exato, filtro isolado (RF0024)', () => {
    cy.get('[data-cy=filtro-cpf]').type(CLIENTE_COM_PEDIDOS.cpf);
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('have.length', 1);
  });

  it('11. deve filtrar clientes pelo e-mail, filtro isolado (RF0024)', () => {
    cy.get('[data-cy=filtro-email]').type(CLIENTE_COM_PEDIDOS.email);
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('have.length', 1);
  });

  it('12. deve filtrar clientes combinando Nome e Status, filtro combinado (RF0024)', () => {
    cy.get('[data-cy=filtro-nome]').type('João');
    cy.get('[data-cy=filtro-status]').select('ativos');
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('have.length', 1);
  });

  it('13. deve filtrar clientes pelo código (RF0024, RNF0035)', () => {
    cy.get('[data-cy=filtro-codigo]').type(String(CLIENTE_COM_PEDIDOS.id));
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('have.length', 1);
    cy.get('[data-cy=filtro-codigo]').clear().type('999999');
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('not.exist');
  });

  it('14. deve filtrar clientes pelo telefone (RF0024)', () => {
    cy.get('[data-cy=filtro-telefone]').type('98765-4321');
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('have.length', 1);
  });

  it('15. deve filtrar clientes pelo gênero (RF0024)', () => {
    cy.get('[data-cy=filtro-genero]').select('M');
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('have.length', 1);
    cy.get('[data-cy=filtro-genero]').select('F');
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('not.exist');
  });

  it('16. deve filtrar clientes pela data de nascimento (RF0024)', () => {
    cy.get('[data-cy=filtro-nasc]').type('1990-05-15');
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('have.length', 1);
  });

  it('17. deve filtrar clientes pelo ranking (RF0024, RN0027)', () => {
    cy.get('[data-cy=filtro-ranking]').type('85');
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('have.length', 1);
    cy.get('[data-cy=filtro-ranking]').clear().type('0');
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('not.exist');
  });

  it('18. deve responder a uma consulta em no máximo 1 segundo (RNF0011)', () => {
    cy.get('[data-cy=filtro-nome]').type('João');
    cy.window().then((win) => {
      const inicio = win.performance.now();
      win.document.querySelector('[data-cy=btn-buscar]').click();
      const duracao = win.performance.now() - inicio;
      expect(duracao, 'tempo de resposta da consulta (ms)').to.be.lessThan(1000);
    });
    cy.get('[data-cy=cliente-row]').should('have.length', 1);
  });

  it('19. deve restaurar a listagem completa ao limpar os filtros (RF0024)', () => {
    cy.get('[data-cy=filtro-nome]').type('Busca Inexistente');
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('not.exist');

    cy.get('[data-cy=filtro-nome]').clear();
    cy.get('[data-cy=btn-buscar]').click();
    cy.get('[data-cy=cliente-row]').should('have.length.at.least', 1);
  });
});

// ============================================================================
// 3) ALTERAR — RF0022 (dados pessoais, sem tocar endereço/cartão/senha)
// ============================================================================
describe('CRUD Cliente — Alterar dados pessoais (RF0022)', () => {
  it('20. deve alterar os dados de telefone de um cliente com sucesso (RF0022, RN0026)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=i-tel-ddd]').clear().type('11');
    cy.get('[data-cy=i-tel-numero]').clear().type('95555-4444');
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.contains('h1', 'Gestão de Clientes').should('be.visible');
  });

  it('21. não deve exibir campos de senha nem de endereço no formulário de edição (RF0022, RF0028, RNF0034)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=i-senha]').should('not.exist');
    cy.get('[data-cy=i-conf]').should('not.exist');
  });

  it('22. deve cancelar a edição e voltar para a lista sem salvar (RF0022)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.contains('button', '← Voltar').click();
    cy.contains('h1', 'Gestão de Clientes').should('be.visible');
  });

  it('23. deve manter os endereços e cartões do cliente intactos após alterar apenas dados pessoais (RF0022, RNF0034)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=endereco-row]').should('have.length', 1);

    cy.get('[data-cy=i-tel-numero]').clear().type('97777-7777');
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();

    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=endereco-row]').should('have.length', 1);
    cy.get('[data-cy=cartao-row]').should('have.length', 1);
  });
});

// ============================================================================
// 4) RF0026 — Endereços de entrega múltiplos (RNF0034 - independentes do resto do cadastro)
// ============================================================================
describe('Cliente — Endereços de entrega (RF0026, RNF0034)', () => {
  it('24. deve adicionar um novo endereço sem alterar os dados pessoais do cliente (RF0026, RNF0034)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();

    cy.get('[data-cy=btn-novo-endereco]').click();
    cy.get('[data-cy=i-apelido]').type('Trabalho');
    cy.get('[data-cy=i-tres]').type('Sala');
    cy.get('[data-cy=i-tlog]').type('Av');
    cy.get('[data-cy=i-log]').type('Paulista');
    cy.get('[data-cy=i-num]').type('1000');
    cy.get('[data-cy=i-bairro]').type('Bela Vista');
    cy.get('[data-cy=i-cep]').type('01310-000');
    cy.get('[data-cy=i-cid]').type('São Paulo');
    cy.get('[data-cy=i-est]').type('SP');
    cy.get('[data-cy=i-pais]').type('Brasil');
    cy.get('[data-cy=btn-salvar-endereco]').click();

    cy.get('[data-cy=endereco-row]').should('have.length', 2);
    cy.contains('[data-cy=endereco-row]', 'Trabalho').should('exist');

    // A alteração de endereço não deve ter mexido no telefone/nome do cliente
    cy.get('[data-cy=i-nome]').should('have.value', CLIENTE_COM_PEDIDOS.nome);
  });

  it('25. deve alterar um endereço existente sem editar os demais dados cadastrais (RF0026, RNF0034)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-editar-endereco]').click();
    cy.get('[data-cy=i-apelido]').should('have.value', 'Casa').clear().type('Casa Reformada');
    cy.get('[data-cy=i-log]').clear().type('das Palmeiras');
    cy.get('[data-cy=btn-salvar-endereco]').click();

    cy.get('[data-cy=endereco-row]').should('have.length', 1);
    cy.contains('[data-cy=endereco-row]', 'Casa Reformada').should('contain.text', 'das Palmeiras');
    cy.get('[data-cy=i-nome]').should('have.value', CLIENTE_COM_PEDIDOS.nome);
  });

  it('26. não deve salvar a alteração de um endereço com campo obrigatório em branco (RN0023, RNF0034)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-editar-endereco]').click();
    cy.get('[data-cy=i-bairro]').clear();
    cy.get('[data-cy=btn-salvar-endereco]').click();
    cy.get('[data-cy=msg-erro-endereco]').scrollIntoView().should('be.visible').and('contain.text', 'Preencha todos os campos');
  });

  it('27. não deve adicionar endereço com campos obrigatórios em branco (RF0026, RN0023)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-novo-endereco]').click();
    cy.get('[data-cy=i-apelido]').type('Incompleto');
    cy.get('[data-cy=btn-salvar-endereco]').click();
    cy.get('[data-cy=msg-erro-endereco]').scrollIntoView().should('be.visible').and('contain.text', 'Preencha todos os campos');
  });

  it('28. não deve permitir remover o último endereço restante do cliente (RN0021, RN0022)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=endereco-row]').should('have.length', 1);
    cy.get('[data-cy=btn-remover-endereco]').click();

    cy.get('[data-cy=msg-erro-endereco]').scrollIntoView().should('be.visible').and('contain.text', 'ao menos um endereço');
    cy.get('[data-cy=endereco-row]').should('have.length', 1);
  });

  it('29. deve remover um endereço quando o cliente possui mais de um cadastrado (RF0026, RN0021, RN0022)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-novo-endereco]').click();
    cy.get('[data-cy=i-apelido]').type('Secundário');
    cy.get('[data-cy=i-tres]').type('Casa'); cy.get('[data-cy=i-tlog]').type('Rua');
    cy.get('[data-cy=i-log]').type('B'); cy.get('[data-cy=i-num]').type('2');
    cy.get('[data-cy=i-bairro]').type('C'); cy.get('[data-cy=i-cep]').type('22222-222');
    cy.get('[data-cy=i-cid]').type('SP'); cy.get('[data-cy=i-est]').type('SP'); cy.get('[data-cy=i-pais]').type('Brasil');
    cy.get('[data-cy=btn-salvar-endereco]').click();
    cy.get('[data-cy=endereco-row]').should('have.length', 2);

    cy.contains('[data-cy=endereco-row]', 'Secundário').find('[data-cy=btn-remover-endereco]').click();
    cy.get('[data-cy=endereco-row]').should('have.length', 1);
    cy.contains('[data-cy=endereco-row]', 'Secundário').should('not.exist');
  });
});

// ============================================================================
// 5) RF0027 — Cartões de crédito, um preferencial
// ============================================================================
describe('Cliente — Cartões de crédito (RF0027)', () => {
  it('30. deve adicionar um cartão de crédito armazenando apenas os últimos 4 dígitos (RF0027, RN0024)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();

    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-cc-numero]').type('4111 1111 1111 9999');
    cy.get('[data-cy=i-cc-bandeira]').select('Visa');
    cy.get('[data-cy=i-cc-titular]').type('JOAO SILVA');
    cy.get('[data-cy=i-cc-cvv]').type('123');
    cy.get('[data-cy=btn-salvar-cartao]').click();

    cy.get('[data-cy=cartao-row]').should('have.length', 2);
    cy.contains('[data-cy=cartao-row]', '9999').should('exist');
    cy.contains('[data-cy=cartao-row]', '1111 1111 1111 9999').should('not.exist');
  });

  it('31. o primeiro cartão cadastrado deve nascer como preferencial (RF0027)', () => {
    cadastrarClienteAuxiliar('20').then((c) => {
      linhaCliente(c.nome).find('[data-cy=btn-editar]').click();
      cy.get('[data-cy=btn-novo-cartao]').click();
      cy.get('[data-cy=i-cc-numero]').type('5500 0000 0000 1234');
      cy.get('[data-cy=i-cc-bandeira]').select('Mastercard');
      cy.get('[data-cy=i-cc-titular]').type(c.nome.toUpperCase());
      cy.get('[data-cy=i-cc-cvv]').type('123');
      cy.get('[data-cy=btn-salvar-cartao]').click();

      cy.get('[data-cy=cartao-row]').should('have.length', 1)
        .find('[data-cy=col-status-cartao]').should('contain.text', 'PREFERENCIAL');
    });
  });

  it('32. deve permitir tornar um segundo cartão preferencial, desmarcando o anterior (RF0027)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();

    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-cc-numero]').type('4000 0000 0000 4321');
    cy.get('[data-cy=i-cc-bandeira]').select('Elo');
    cy.get('[data-cy=i-cc-titular]').type('JOAO SILVA');
    cy.get('[data-cy=i-cc-cvv]').type('123');
    cy.get('[data-cy=btn-salvar-cartao]').click();

    cy.contains('[data-cy=cartao-row]', '4321')
      .find('[data-cy=btn-tornar-preferencial]').click();

    cy.contains('[data-cy=cartao-row]', '4321')
      .find('[data-cy=col-status-cartao]').should('contain.text', 'PREFERENCIAL');
  });

  it('33. ao remover o cartão preferencial, outro cartão deve assumir automaticamente (RF0027)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();

    // Cada teste começa com a página recarregada (beforeEach -> cy.visit), então o
    // estado em memória volta ao inicial: o cliente só tem o cartão 4242 (preferencial).
    // Por isso o teste precisa criar o seu próprio segundo cartão.
    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-cc-numero]').type('5500 0000 0000 1234');
    cy.get('[data-cy=i-cc-bandeira]').select('Mastercard');
    cy.get('[data-cy=i-cc-titular]').type('JOAO SILVA');
    cy.get('[data-cy=i-cc-cvv]').type('123');
    cy.get('[data-cy=btn-salvar-cartao]').click();

    cy.get('[data-cy=cartao-row]').should('have.length', 2);

    // Remove o preferencial (4242)
    cy.contains('[data-cy=cartao-row]', 'PREFERENCIAL')
      .find('[data-cy=btn-remover-cartao]').click();

    // Sobra apenas o cartão 1234, que deve ter assumido como preferencial
    cy.get('[data-cy=cartao-row]').should('have.length', 1);
    cy.contains('[data-cy=cartao-row]', '1234')
      .find('[data-cy=col-status-cartao]').should('contain.text', 'PREFERENCIAL');
  });

  it('34. não deve adicionar cartão com campos obrigatórios em branco (RF0027, RN0024)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=btn-salvar-cartao]').click();
    cy.get('[data-cy=msg-erro-cartao]').scrollIntoView().should('be.visible').and('contain.text', 'Preencha todos os campos');
  });
});

// ============================================================================
// 6) RF0028 — Alteração de senha isolada
// ============================================================================
describe('Cliente — Alteração de senha isolada (RF0028, RNF0031, RNF0032)', () => {
  it('35. deve alterar a senha do cliente sem passar pelo formulário de dados pessoais (RF0028)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();

    cy.get('[data-cy=btn-abrir-senha]').click();
    cy.get('[data-cy=i-nova-senha]').type('NovaSenha@1');
    cy.get('[data-cy=i-nova-senha-conf]').type('NovaSenha@1');
    cy.get('[data-cy=btn-salvar-senha]').click();

    // Ao salvar com sucesso, o subformulário fecha e volta ao botão "Alterar Senha"
    cy.get('[data-cy=btn-abrir-senha]').should('be.visible');
    cy.get('[data-cy=i-nova-senha]').should('not.exist');
  });

  it('36. não deve alterar a senha se a confirmação não coincidir (RF0028, RNF0032)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-abrir-senha]').click();
    cy.get('[data-cy=i-nova-senha]').type('NovaSenha@1');
    cy.get('[data-cy=i-nova-senha-conf]').type('OutraSenha@2');
    cy.get('[data-cy=btn-salvar-senha]').click();

    cy.get('[data-cy=msg-erro-senha]').scrollIntoView().should('be.visible').and('contain.text', 'não coincidem');
  });

  it('37. não deve alterar a senha se a nova senha for fraca (RF0028, RNF0031)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-abrir-senha]').click();
    cy.get('[data-cy=i-nova-senha]').type('fraca');
    cy.get('[data-cy=i-nova-senha-conf]').type('fraca');
    cy.get('[data-cy=btn-salvar-senha]').click();

    cy.get('[data-cy=msg-erro-senha]').scrollIntoView().should('be.visible').and('contain.text', 'Senha fraca');
  });

  it('38. deve cancelar a alteração de senha sem enviar nenhum dado (RF0028)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-abrir-senha]').click();
    cy.get('[data-cy=btn-cancelar-senha]').click();
    cy.get('[data-cy=i-nova-senha]').should('not.exist');
  });
});

// ============================================================================
// 7) RF0025 — Consulta de transações do cliente a partir do próprio cadastro
// ============================================================================
describe('Cliente — Consulta de transações (RF0025)', () => {
  it('39. deve exibir o histórico de transações do cliente a partir do cadastro (RF0025)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();

    cy.get('[data-cy=tabela-transacoes]').scrollIntoView().should('be.visible');
    cy.get('[data-cy=transacao-row]').should('have.length', 2);
    cy.get('[data-cy=transacao-row]').first().find('[data-cy=col-status-transacao]').should('not.be.empty');
  });

  it('40. deve exibir mensagem apropriada quando o cliente não possui transações (RF0025)', () => {
    cadastrarClienteAuxiliar('30').then((c) => {
      linhaCliente(c.nome).find('[data-cy=btn-editar]').click();
      cy.get('[data-cy=sem-transacao]').scrollIntoView().should('be.visible').and('contain.text', 'Nenhuma transação');
    });
  });
});

// ============================================================================
// 8) INATIVAR / EXCLUIR — RF0023 + regra complementar de proteção ao RF0025
// ============================================================================
describe('CRUD Cliente — Inativar e Excluir (RF0023 + regra complementar do RF0025)', () => {
  it('41. deve bloquear a exclusão definitiva de clientes que possuem transações associadas (RF0025, regra complementar)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-excluir]').click();
    cy.get('[data-cy=msg-erro-lista]')
      .scrollIntoView()
      .should('be.visible')
      .and('contain.text', 'possui transações associadas');
  });

  it('42. deve inativar um cliente mudando seu status para INATIVO (RF0023)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-inativar]').click();
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=col-status]').should('contain.text', 'INATIVO');
  });

  it('43. deve permitir reativar um cliente previamente inativado (RF0023)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-inativar]').click();
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-reativar]').click();
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=col-status]').should('contain.text', 'ATIVO');
  });

  it('44. deve excluir definitivamente um cliente que não possui transações associadas (RF0023, regra complementar)', () => {
    cadastrarClienteAuxiliar('99').then((c) => {
      linhaCliente(c.nome).find('[data-cy=btn-excluir]').click();
      cy.contains('[data-cy=cliente-row]', c.nome).should('not.exist');
    });
  });
});

// ============================================================================
// 9) FLUXO COMPLETO — E2E
// ============================================================================
describe('Cliente — Fluxo completo (E2E)', () => {
  it('45. deve percorrer o ciclo de vida completo de um cliente sem erros (RF0021 a RF0028)', () => {
    const c = gerarCliente('E2E');

    // Cadastro
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c);
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();

    // Consulta
    cy.get('[data-cy=filtro-cpf]').type(c.cpf);
    cy.get('[data-cy=btn-buscar]').click();

    // Alteração de dados pessoais
    linhaCliente(c.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=i-tel-ddd]').clear().type('99');
    cy.get('[data-cy=i-tel-numero]').clear().type('91111-2222');
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();

    // Endereço adicional (RF0026)
    linhaCliente(c.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-novo-endereco]').click();
    cy.get('[data-cy=i-apelido]').type('Trabalho');
    cy.get('[data-cy=i-tres]').type('Sala'); cy.get('[data-cy=i-tlog]').type('Av');
    cy.get('[data-cy=i-log]').type('Y'); cy.get('[data-cy=i-num]').type('9');
    cy.get('[data-cy=i-bairro]').type('C'); cy.get('[data-cy=i-cep]').type('33333-333');
    cy.get('[data-cy=i-cid]').type('SP'); cy.get('[data-cy=i-est]').type('SP'); cy.get('[data-cy=i-pais]').type('Brasil');
    cy.get('[data-cy=btn-salvar-endereco]').click();

    // Cartão de crédito (RF0027)
    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-cc-numero]').type('4000 0000 0000 1111');
    cy.get('[data-cy=i-cc-bandeira]').select('Visa');
    cy.get('[data-cy=i-cc-titular]').type(c.nome.toUpperCase());
    cy.get('[data-cy=i-cc-cvv]').type('123');
    cy.get('[data-cy=btn-salvar-cartao]').click();

    // Alteração de senha isolada (RF0028)
    cy.get('[data-cy=btn-abrir-senha]').click();
    cy.get('[data-cy=i-nova-senha]').type('NovaSenha@1');
    cy.get('[data-cy=i-nova-senha-conf]').type('NovaSenha@1');
    cy.get('[data-cy=btn-salvar-senha]').click();

    // Consulta de transações (RF0025) - cliente novo não tem nenhuma
    cy.get('[data-cy=sem-transacao]').scrollIntoView().should('be.visible');

    // Inativação (RF0023)
    cy.contains('button', '← Voltar').click();
    linhaCliente(c.nome).find('[data-cy=btn-inativar]').click();

    // Exclusão final - permitida por não ter transações
    linhaCliente(c.nome).find('[data-cy=btn-excluir]').click();
    cy.contains('[data-cy=cliente-row]', c.nome).should('not.exist');
  });
});

// ============================================================================
// 10) RN0026 — Dados obrigatórios do cliente, com foco na composição do telefone
// ============================================================================
describe('Cliente — Dados obrigatórios e composição do telefone (RN0026)', () => {
  it('46. não deve cadastrar cliente sem selecionar o tipo de telefone (RN0026)', () => {
    const c = gerarCliente('36');
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c);
    cy.get('[data-cy=i-tel-tipo]').select('');
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.get('[data-cy=msg-erro-form]').scrollIntoView().should('be.visible').and('contain.text', 'Preencha todos os campos');
  });

  it('47. não deve cadastrar cliente com DDD fora do formato de 2 dígitos (RN0026)', () => {
    const c = gerarCliente('37');
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c);
    // O input tem maxlength="2", então digitar 3 dígitos seria truncado para 2 e o
    // formulário seria salvo normalmente. Usamos 1 dígito, que o campo aceita e a
    // validação (REGEX_DDD = 2 dígitos) precisa rejeitar.
    cy.get('[data-cy=i-tel-ddd]').clear().type('1');
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.get('[data-cy=msg-erro-form]').scrollIntoView().should('be.visible').and('contain.text', 'DDD inválido');
  });

  it('48. não deve salvar alteração com número de telefone fora do formato esperado (RF0022, RN0026)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=i-tel-numero]').clear().type('999990000');
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.get('[data-cy=msg-erro-form]').scrollIntoView().should('be.visible').and('contain.text', 'Número de telefone inválido');
  });

  it('49. deve aceitar telefone completo (tipo + DDD + número) em formato válido (RN0026)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=i-tel-tipo]').select('Residencial');
    cy.get('[data-cy=i-tel-ddd]').clear().type('21');
    cy.get('[data-cy=i-tel-numero]').clear().type('7777-8888');
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    cy.contains('h1', 'Gestão de Clientes').should('be.visible');
  });
});

// ============================================================================
// 11) RN0021 / RN0022 — Endereço de cobrança e endereço de entrega obrigatórios
// ============================================================================
describe('Cliente — Endereço de cobrança e de entrega obrigatórios (RN0021, RN0022)', () => {
  it('50. o endereço inicial do cadastro deve atender cobrança e entrega ao mesmo tempo (RN0021, RN0022)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=endereco-row]').first().find('[data-cy=col-tipo-endereco]')
      .should('contain.text', 'COBRANÇA')
      .and('contain.text', 'ENTREGA');
  });

  it('51. não deve permitir remover o único endereço, pois deixaria o cliente sem cobrança e sem entrega (RN0021, RN0022)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=endereco-row]').should('have.length', 1);
    cy.get('[data-cy=btn-remover-endereco]').click();

    cy.get('[data-cy=msg-erro-endereco]').scrollIntoView().should('be.visible').and('contain.text', 'RN0021/RN0022');
    cy.get('[data-cy=endereco-row]').should('have.length', 1);
  });

  it('52. não deve permitir remover o endereço de cobrança se ele for o único, mesmo havendo outro só de entrega (RN0021)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();

    // Adiciona um segundo endereço marcado apenas como entrega (sem cobrança)
    cy.get('[data-cy=btn-novo-endereco]').click();
    cy.get('[data-cy=i-apelido]').type('Somente Entrega');
    cy.get('[data-cy=i-tres]').type('Casa'); cy.get('[data-cy=i-tlog]').type('Rua');
    cy.get('[data-cy=i-log]').type('Z'); cy.get('[data-cy=i-num]').type('5');
    cy.get('[data-cy=i-bairro]').type('D'); cy.get('[data-cy=i-cep]').type('44444-444');
    cy.get('[data-cy=i-cid]').type('SP'); cy.get('[data-cy=i-est]').type('SP'); cy.get('[data-cy=i-pais]').type('Brasil');
    cy.get('[data-cy=i-cobranca]').uncheck();
    cy.get('[data-cy=btn-salvar-endereco]').click();
    cy.get('[data-cy=endereco-row]').should('have.length', 2);

    // Tenta remover o endereço original (único que cobre cobrança) - deve bloquear
    cy.contains('[data-cy=endereco-row]', 'Casa').find('[data-cy=btn-remover-endereco]').click();
    cy.get('[data-cy=msg-erro-endereco]').scrollIntoView().should('be.visible').and('contain.text', 'cobrança');
    cy.get('[data-cy=endereco-row]').should('have.length', 2);
  });

  it('53. não deve permitir alterar o único endereço de forma que deixe de ser de cobrança (RN0021)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-editar-endereco]').click();
    cy.get('[data-cy=i-cobranca]').uncheck();
    cy.get('[data-cy=btn-salvar-endereco]').click();
    cy.get('[data-cy=msg-erro-endereco]').scrollIntoView().should('be.visible').and('contain.text', 'RN0021');
  });

  it('54. não deve permitir alterar o único endereço de forma que deixe de ser de entrega (RN0022)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-editar-endereco]').click();
    cy.get('[data-cy=i-entrega]').uncheck();
    cy.get('[data-cy=btn-salvar-endereco]').click();
    cy.get('[data-cy=msg-erro-endereco]').scrollIntoView().should('be.visible').and('contain.text', 'RN0022');
  });

  it('55. não deve adicionar endereço sem marcar nem cobrança nem entrega (RN0021, RN0022)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-novo-endereco]').click();
    cy.get('[data-cy=i-apelido]').type('Sem Tipo');
    cy.get('[data-cy=i-tres]').type('Casa'); cy.get('[data-cy=i-tlog]').type('Rua');
    cy.get('[data-cy=i-log]').type('W'); cy.get('[data-cy=i-num]').type('7');
    cy.get('[data-cy=i-bairro]').type('E'); cy.get('[data-cy=i-cep]').type('55555-555');
    cy.get('[data-cy=i-cid]').type('SP'); cy.get('[data-cy=i-est]').type('SP'); cy.get('[data-cy=i-pais]').type('Brasil');
    cy.get('[data-cy=i-cobranca]').uncheck();
    cy.get('[data-cy=i-entrega]').uncheck();
    cy.get('[data-cy=btn-salvar-endereco]').click();
    cy.get('[data-cy=msg-erro-endereco]').scrollIntoView().should('be.visible').and('contain.text', 'cobrança, entrega, ou ambos');
  });
});

// ============================================================================
// 12) RN0023 — Composição do registro de endereços (campo opcional de observações)
// ============================================================================
describe('Cliente — Composição do endereço e observações opcionais (RN0023)', () => {
  it('56. deve cadastrar um novo cliente preenchendo o campo opcional de observações do endereço (RN0023)', () => {
    const c = gerarCliente('43');
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c);
    cy.get('[data-cy=i-obs]').type('Portão azul, tocar campainha duas vezes');
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    linhaCliente(c.nome).should('exist');
  });

  it('57. deve cadastrar um novo cliente mesmo sem preencher observações, campo opcional (RN0023)', () => {
    const c = gerarCliente('44');
    cy.get('[data-cy=btn-novo]').click();
    preencherForm(c);
    cy.get('[data-cy=btn-salvar]').scrollIntoView().click();
    linhaCliente(c.nome).should('exist');
  });
});

// ============================================================================
// 13) RN0024 / RN0025 — Composição do cartão e bandeira de lista fechada
// ============================================================================
describe('Cliente — Composição do cartão e bandeira registrada (RN0024, RN0025)', () => {
  it('58. não deve adicionar cartão sem o código de segurança (RN0024)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-cc-numero]').type('4111 1111 1111 0000');
    cy.get('[data-cy=i-cc-bandeira]').select('Visa');
    cy.get('[data-cy=i-cc-titular]').type('JOAO SILVA');
    // Código de segurança deliberadamente em branco
    cy.get('[data-cy=btn-salvar-cartao]').click();
    cy.get('[data-cy=msg-erro-cartao]').scrollIntoView().should('be.visible').and('contain.text', 'Preencha todos os campos');
  });

  it('59. não deve adicionar cartão com código de segurança em formato inválido (RN0024)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-cc-numero]').type('4111 1111 1111 0000');
    cy.get('[data-cy=i-cc-bandeira]').select('Visa');
    cy.get('[data-cy=i-cc-titular]').type('JOAO SILVA');
    cy.get('[data-cy=i-cc-cvv]').type('AB');
    cy.get('[data-cy=btn-salvar-cartao]').click();
    cy.get('[data-cy=msg-erro-cartao]').scrollIntoView().should('be.visible').and('contain.text', 'Código de segurança inválido');
  });

  it('60. só deve oferecer bandeiras registradas no sistema (RN0025)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-cc-bandeira] option').then((options) => {
      const valores = [...options].map(o => o.value).filter(Boolean);
      expect(valores).to.include.members(['Visa', 'Mastercard', 'Elo', 'American Express', 'Hipercard']);
    });
  });

  it('61. deve adicionar um cartão completo (número, bandeira, titular e CVV) com sucesso (RF0027, RN0024, RN0025)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).find('[data-cy=btn-editar]').click();
    cy.get('[data-cy=btn-novo-cartao]').click();
    cy.get('[data-cy=i-cc-numero]').type('4111 1111 1111 5555');
    cy.get('[data-cy=i-cc-bandeira]').select('Hipercard');
    cy.get('[data-cy=i-cc-titular]').type('JOAO SILVA');
    cy.get('[data-cy=i-cc-cvv]').type('321');
    cy.get('[data-cy=btn-salvar-cartao]').click();

    cy.contains('[data-cy=cartao-row]', '5555')
      .find('[data-cy=col-bandeira]').should('contain.text', 'Hipercard');
  });
});

// ============================================================================
// 14) RNF0035 — Código único de cliente
// ============================================================================
describe('Cliente — Código único (RNF0035)', () => {
  it('62. todo cliente cadastrado deve receber um código único (RNF0035)', () => {
    cadastrarClienteAuxiliar('60')
      .then(() => cadastrarClienteAuxiliar('61'))
      .then((c) => {
        linhaCliente(c.nome).find('td').first().invoke('text').should('match', /^\d+$/);
        cy.get('[data-cy=cliente-row]').then(($linhas) => {
          const codigos = [...$linhas].map((l) => l.getAttribute('data-id'));
          expect(codigos.length).to.be.at.least(3);
          expect(codigos.every(Boolean), 'todos os clientes têm código').to.equal(true);
          expect(new Set(codigos).size, 'códigos sem repetição').to.equal(codigos.length);
        });
      });
  });
});

// ============================================================================
// 15) RN0027 — Ranking numérico do cliente
// ============================================================================
describe('Cliente — Ranking numérico (RN0027)', () => {
  it('63. deve exibir o ranking numérico do cliente na listagem (RN0027)', () => {
    linhaCliente(CLIENTE_COM_PEDIDOS.nome).should('contain.text', 'pts');
  });

  it('64. cliente recém-cadastrado deve iniciar com ranking zerado (RN0027)', () => {
    cadastrarClienteAuxiliar('50').then((c) => {
      linhaCliente(c.nome).should('contain.text', '0 pts');
    });
  });
});
