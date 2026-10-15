#!/usr/bin/env node
// Gera docs/MATRIZ_RASTREABILIDADE.md a partir dos títulos dos testes Cypress de
// Vendas (cypress/e2e/vendas/*.cy.js). Cada título termina com os requisitos que
// o teste cobre, ex.: "CTV007 — não deve permitir ... (RN0031)".
//   npm run docs:matriz
const fs = require('fs');
const path = require('path');

const raiz = path.join(__dirname, '..');
const pasta = path.join(raiz, 'cypress', 'e2e', 'vendas');

const DRS = {
  RF0031: 'Gerenciar carrinho de compra', RF0032: 'Definir quantidade de itens no carrinho', RF0033: 'Realizar compra',
  RF0034: 'Calcular frete', RF0035: 'Selecionar endereço de entrega', RF0036: 'Selecionar forma de pagamento',
  RF0037: 'Uso de cupom para pagamento', RF0038: 'Finalizar compra (EM PROCESSAMENTO)', RF0039: 'Despachar produtos para entrega',
  RF0040: 'Produtos entregues', RF0041: 'Solicitar troca', RF0042: 'Autorizar trocas', RF0043: 'Visualização de trocas',
  RF0044: 'Confirmar recebimento de itens para troca', RF0045: 'Gerar cupom de troca após recebimento', RF0053: 'Dar baixa em estoque',
  RF0054: 'Realizar reentrada em estoque',
  RN0031: 'Validar estoque para adição de itens no carrinho', RN0032: 'Validar estoque para compra',
  RN0033: 'Uso de cupom promocional (um por compra)', RN0034: 'Uso de diversos cartões (mínimo R$ 10,00 cada)',
  RN0035: 'Cupons junto a cartão (dispensa o mínimo)', RN0036: 'Gerar cupom de troca (troco)', RN0037: 'Validar forma de pagamento',
  RN0038: 'Status APROVADA/REPROVADA conforme pagamento', RN0039: 'Status EM TRANSPORTE', RN0040: 'Status ENTREGUE',
  RN0041: 'Gerar pedido de troca (EM TROCA)', RN0042: 'Status TROCADO após recebimento', RN0043: 'Só itens ENTREGUES podem ser trocados',
  RN0044: 'Bloqueio de produtos no carrinho', RN0045: 'Retirar item do carrinho ao desbloquear', RN0046: 'Notificação de autorização de troca',
  RN0028: 'Validar retorno da operadora (estoque preservado se reprovada)',
  RNF0042: 'Apresentar itens retirados do carrinho', RNF0012: 'Log de transação (escrita)', RNF0011: 'Tempo de resposta (consultas)',
};
const SITUACAO_ESPECIAL = {
  RNF0012: 'Implementado (aspecto `LogDeEscritaAspect`); verificação manual no log do backend',
  RNF0011: 'Não avaliado automaticamente (sem teste de desempenho)',
  RF0034: 'Coberto (frete **fixo** R$ 24,90 — decisão do grupo, ver `docs/JUSTIFICATIVA_FRETE.md`)',
};

const expandir = (texto) => {
  const reqs = new Set(texto.match(/R(?:F|N|NF)\d{4}/g) || []);
  for (const [, pref, a, b] of texto.matchAll(/(R[FN])(\d{4})-R[FN](\d{4})/g)) {
    for (let n = Number(a); n <= Number(b); n++) reqs.add(`${pref}${String(n).padStart(4, '0')}`);
  }
  return reqs;
};

const testes = [];
for (const arq of fs.readdirSync(pasta).filter((f) => f.endsWith('.cy.js')).sort()) {
  const src = fs.readFileSync(path.join(pasta, arq), 'utf8');
  for (const m of src.matchAll(/it\('((CTV\d+) — (.*?))',\s*\(\)/g)) {
    const [, , id, titulo] = m;
    const final = titulo.match(/\(([^()]*)\)\s*$/);
    let reqs = expandir(final ? final[1] : '');
    if (id === 'CTV100') { // o caso completo cita os requisitos no cabeçalho do arquivo
      const cab = src.match(/Requisitos exercitados:([\s\S]*?)\n\/\/ =/);
      if (cab) reqs = expandir(cab[1].replace(/\n\/\/\s*/g, ' '));
    }
    testes.push({ id, titulo: titulo.replace(/\s*\([^()]*\)\s*$/, ''), arq, reqs: [...reqs].sort() });
  }
}

const cobertura = {};
for (const t of testes) for (const r of t.reqs) (cobertura[r] ||= []).push(t.id);

const L = [];
L.push('# Matriz de rastreabilidade — módulo de Vendas (CDU02)', '');
L.push('> **Gerada automaticamente** (`npm run docs:matriz`) a partir dos títulos dos testes em `cypress/e2e/vendas/*.cy.js`; cada título termina com os requisitos que ele cobre. Se mudar um teste, rode o comando de novo.', '');
L.push(`Base: \`DRS_LES_2_2026\` v0.6 · **${testes.length} testes automatizados Cypress** + testes de integração JUnit da API (\`backend/src/test\`).`, '');
L.push('## 1. Requisito → testes', '');
L.push('| Requisito | Descrição (DRS) | Casos de teste Cypress | Situação |', '|---|---|---|---|');
const semTeste = [];
for (const [r, d] of Object.entries(DRS)) {
  const ts = cobertura[r] || [];
  let sit = ts.length ? 'Coberto' : 'Sem teste Cypress';
  if (SITUACAO_ESPECIAL[r]) sit = SITUACAO_ESPECIAL[r];
  else if (!ts.length) semTeste.push(r);
  L.push(`| ${r} | ${d} | ${ts.length ? ts.join(', ') : '—'} | ${sit} |`);
}
L.push('', '## 2. Caso de teste → requisitos', '');
L.push('| ID | Título | Requisitos | Arquivo |', '|---|---|---|---|');
for (const t of testes) L.push(`| ${t.id} | ${t.titulo} | ${t.reqs.join(', ')} | \`${t.arq}\` |`);
L.push('', '## 3. Lacunas conhecidas', '');
L.push('- **RNF0011** (consulta em até 1 s): não há teste de desempenho para Vendas (no módulo de Clientes existe o teste 18, que mede a consulta do front).');
L.push('- **RNF0012** (log de escrita): implementado por aspecto e visível no terminal do backend (`OPERACOES_DE_ESCRITA`); não há asserção automatizada.');
L.push('- **RF0034** (frete): valor fixo (decisão do grupo, justificada em `docs/JUSTIFICATIVA_FRETE.md`); o DRS pede cálculo "com base nos itens e no endereço" mas não define a fórmula.');
L.push('- **RNF0033** (senha criptografada) pertence ao módulo de Clientes e não é verificável pela interface.');
if (semTeste.length) L.push(`- Sem teste Cypress: ${semTeste.join(', ')}`);

fs.writeFileSync(path.join(raiz, 'docs', 'MATRIZ_RASTREABILIDADE.md'), L.join('\n') + '\n');
console.log(`Matriz gerada: ${testes.length} testes, ${Object.keys(cobertura).filter((r) => DRS[r]).length} requisitos do CDU02 cobertos${semTeste.length ? ' — sem teste: ' + semTeste : ''}.`);
