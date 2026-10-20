// ============================================================================
// Comandos customizados do Cypress usados pela suíte de Vendas.
// Todos conversam com a API do backend (Cypress.env('apiUrl')) — nunca pela
// interface — para montar rapidamente o cenário de cada teste ("arrange").
// ============================================================================

const api = () => Cypress.env('apiUrl');

// Chamada genérica à API. Não falha em status 4xx/5xx, para que os testes
// possam verificar as recusas das regras de negócio (409, 400...).
Cypress.Commands.add('apiRequest', (method, path, body) =>
  cy.request({ method, url: `${api()}${path}`, body, failOnStatusCode: false })
);

// Volta a base ao estado inicial dos seeders (João Silva = cliente 1, 5
// produtos, pedidos #1 ENTREGUE e #2 EM_TRANSPORTE, 4 cupons). Chamado no
// beforeEach de cada teste de Vendas: por isso eles não dependem uns dos outros.
Cypress.Commands.add('resetarBase', () => {
  cy.request('POST', `${api()}/test-support/reset`).its('status').should('eq', 200);
  // O sistema valida o pagamento sozinho (RN0037/RN0038) alguns segundos depois da compra. Nos testes isso fica
  // DESLIGADO por padrão, para cada um decidir quando o pagamento é validado (cy.validarPagamentos); o teste da
  // validação automática liga de novo com cy.definirValidacaoAutomatica.
  cy.definirValidacaoAutomatica(-1);
});

// ---- situações difíceis de provocar pela interface ----

Cypress.Commands.add('definirEstoque', (produtoId, quantidade) => {
  cy.request('PUT', `${api()}/test-support/produtos/${produtoId}/estoque?quantidade=${quantidade}`)
    .its('status').should('eq', 200);
});

// Prazo de bloqueio do carrinho = agora + N segundos (negativo = já vencido).
Cypress.Commands.add('definirBloqueio', (clienteId, segundos) => {
  cy.request('PUT', `${api()}/test-support/carrinhos/${clienteId}/bloqueio?segundos=${segundos}`)
    .its('status').should('eq', 200);
});

// Roda agora o job que libera os itens de carrinho vencidos.
Cypress.Commands.add('varrerExpirados', () => {
  cy.request('POST', `${api()}/test-support/carrinhos/varrer`).its('status').should('eq', 200);
});

// Atraso (em segundos) da validação automática do pagamento; negativo = desligada.
Cypress.Commands.add('definirValidacaoAutomatica', (segundos) => {
  cy.request('PUT', `${api()}/test-support/pagamentos/validacao-automatica?segundos=${segundos}`)
    .its('status').should('eq', 200);
});

// Faz agora o que o sistema faz sozinho depois do atraso: valida o pagamento dos pedidos
// EM_PROCESSAMENTO (todos, ou só o informado) e devolve quantos foram validados.
Cypress.Commands.add('validarPagamentos', (pedidoId) =>
  cy.request('POST', `${api()}/test-support/pagamentos/validar${pedidoId ? `?pedidoId=${pedidoId}` : ''}`)
    .then((r) => r.body.validados)
);

// ---- montagem de cenário ----

Cypress.Commands.add('criarClienteViaApi', (sobrescrever = {}) => {
  const corpo = Object.assign({
    nome: 'Maria Souza',
    cpf: '529.982.247-25', // CPF com dígitos verificadores válidos (o backend valida)
    genero: 'F',
    dataNascimento: '1992-03-10',
    telefone: { tipo: 'CELULAR', ddd: '11', numero: '91234-5678' },
    email: 'maria.souza@email.com',
    senha: 'SenhaForte@123',
    confirmacaoSenha: 'SenhaForte@123',
    endereco: {
      apelido: 'Casa', tipoResidencia: 'Casa', tipoLogradouro: 'Rua', logradouro: 'das Palmeiras',
      numero: '10', bairro: 'Centro', cep: '01000-000', cidade: 'São Paulo', estado: 'SP', pais: 'Brasil',
    },
  }, sobrescrever);
  return cy.apiRequest('POST', '/clientes', corpo).then((r) => {
    expect(r.status, 'cadastro do cliente').to.eq(201);
    return r.body;
  });
});

Cypress.Commands.add('adicionarAoCarrinhoViaApi', (clienteId, produtoId, quantidade) =>
  cy.apiRequest('POST', `/clientes/${clienteId}/carrinho/itens`, { produtoId, quantidade }).then((r) => {
    expect(r.status, 'adicionar ao carrinho').to.eq(201);
    return r.body;
  })
);

// Cadastra um cartão para o cliente e devolve o cartão criado (com id).
Cypress.Commands.add('criarCartaoViaApi', (clienteId, sobrescrever = {}) => {
  const corpo = Object.assign({ numero: '5555666677778888', bandeira: 'MASTERCARD', titular: 'JOAO SILVA', cvv: '321' }, sobrescrever);
  return cy.apiRequest('POST', `/clientes/${clienteId}/cartoes`, corpo).then((r) => {
    expect(r.status, 'cadastro do cartão').to.eq(201);
    return r.body;
  });
});

// Faz uma compra completa pela API (carrinho -> finalizar, paga tudo em UM
// cartão) e devolve o pedido, que nasce EM_PROCESSAMENTO. Compra um produto
// (produtoId/quantidade) ou vários (itens: [{ produtoId, quantidade }]).
Cypress.Commands.add('criarPedidoViaApi', (opcoes = {}) => {
  const { clienteId = 1, produtoId = 3, quantidade = 1, cartaoId = 1, enderecoId = 1 } = opcoes;
  const itens = opcoes.itens || [{ produtoId, quantidade }];
  itens.forEach((item) => cy.adicionarAoCarrinhoViaApi(clienteId, item.produtoId, item.quantidade));
  return cy.apiRequest('GET', `/clientes/${clienteId}/carrinho`).then((carrinho) => {
    // total = produtos + frete (calculado pelo backend para o endereço e as unidades do carrinho)
    return cy.apiRequest('GET', `/clientes/${clienteId}/frete?enderecoId=${enderecoId}`).then((f) => {
      const total = Math.round((carrinho.body.total + f.body.valor) * 100) / 100;
      return cy.apiRequest('POST', `/clientes/${clienteId}/pedidos`, {
        enderecoId, cartoes: [{ cartaoId, valor: total }], cupomIds: [],
      });
    });
  }).then((r) => {
    expect(r.status, 'finalizar compra').to.eq(201);
    return r.body;
  });
});

// Leva um pedido até o status pedido ('APROVADA' | 'EM_TRANSPORTE' | 'ENTREGUE'): a aprovação é
// a validação do pagamento que o sistema faz sozinho (aqui adiantada pelo suporte a testes) e as
// demais são ações do administrador. Só executa as etapas que faltam, então pode ser chamado
// de novo para avançar mais um passo.
Cypress.Commands.add('avancarPedidoViaApi', (pedidoId, destino) => {
  const etapas = [
    ['APROVADA', null],
    ['EM_TRANSPORTE', 'despachar'],
    ['ENTREGUE', 'confirmar-entrega'],
  ];
  const ordem = ['EM_PROCESSAMENTO', ...etapas.map(([status]) => status)];
  expect(ordem.indexOf(destino), `destino "${destino}" válido`).to.be.greaterThan(0);

  cy.apiRequest('GET', `/pedidos/${pedidoId}`).then((atual) => {
    etapas.forEach(([status, acao], i) => {
      const posicao = i + 1;
      if (posicao > ordem.indexOf(atual.body.status) && posicao <= ordem.indexOf(destino)) {
        if (acao === null) {
          cy.validarPagamentos(pedidoId).should('eq', 1);
          cy.apiRequest('GET', `/pedidos/${pedidoId}`).its('body.status').should('eq', status);
        } else {
          cy.apiRequest('PATCH', `/pedidos/${pedidoId}/${acao}`).then((r) => {
            expect(r.status, acao).to.eq(200);
            expect(r.body.status).to.eq(status);
          });
        }
      }
    });
  });
});

// O cliente solicita a troca de itens de um pedido ENTREGUE. `itens` =
// [{ itemPedidoId, quantidade }]. Devolve a solicitação criada.
Cypress.Commands.add('solicitarTrocaViaApi', (pedidoId, itens, motivoCliente = 'Produto com defeito') =>
  cy.apiRequest('POST', `/pedidos/${pedidoId}/trocas`, { itens, motivoCliente }).then((r) => {
    expect(r.status, 'solicitar troca').to.eq(201);
    return r.body;
  })
);

// Autoriza (e opcionalmente já confirma o recebimento de) uma solicitação de troca.
Cypress.Commands.add('autorizarTrocaViaApi', (trocaId) => {
  cy.apiRequest('PATCH', `/trocas/${trocaId}/autorizar`).its('status').should('eq', 200);
});

Cypress.Commands.add('confirmarRecebimentoTrocaViaApi', (trocaId, itensRetornaramEstoque) =>
  cy.apiRequest('PATCH', `/trocas/${trocaId}/confirmar-recebimento`, { itensRetornaramEstoque }).then((r) => {
    expect(r.status, 'confirmar recebimento').to.eq(200);
    return r.body;
  })
);
