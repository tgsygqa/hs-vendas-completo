#!/usr/bin/env node
// Cadastra a cliente "Maria Souza" (id 2) no backend. Serve para demonstrar o
// bloqueio de itens entre clientes (RN0044): o que o João coloca no carrinho
// fica indisponível para a Maria, que abre a loja em
//   http://localhost:5600/vendas.html?clienteId=2
// (O módulo de Clientes da tela V08.html guarda dados só em memória no navegador;
// por isso este cadastro é feito direto na API.)
const API = process.env.API_URL || 'http://localhost:8080/api';

const maria = {
  nome: 'Maria Souza', cpf: '529.982.247-25', genero: 'F', dataNascimento: '1992-03-10',
  telefone: { tipo: 'CELULAR', ddd: '11', numero: '91234-5678' }, email: 'maria.souza@email.com',
  senha: 'SenhaForte@123', confirmacaoSenha: 'SenhaForte@123',
  endereco: { apelido: 'Casa', tipoResidencia: 'Casa', tipoLogradouro: 'Rua', logradouro: 'das Palmeiras',
    numero: '10', bairro: 'Centro', cep: '01000-000', cidade: 'São Paulo', estado: 'SP', pais: 'Brasil' },
};

(async () => {
  const r = await fetch(`${API}/clientes`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(maria) });
  const corpo = await r.json().catch(() => ({}));
  if (r.status === 201) {
    console.log(`Cliente criada: ${corpo.nome} (id ${corpo.id}).`);
    console.log(`Loja da Maria:   http://localhost:5600/vendas.html?clienteId=${corpo.id}`);
    console.log(`Pedidos da Maria: http://localhost:5600/pedidos.html?clienteId=${corpo.id}`);
  } else if (r.status === 409) {
    console.log('A Maria já está cadastrada (provavelmente id 2): http://localhost:5600/vendas.html?clienteId=2');
  } else {
    throw new Error(`HTTP ${r.status} ${JSON.stringify(corpo)}`);
  }
})().catch((e) => { console.error('Falhou:', e.message, '— o backend está no ar?'); process.exit(1); });
