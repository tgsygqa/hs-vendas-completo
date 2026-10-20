#!/usr/bin/env node
// Gera docs/MATRIZ_RASTREABILIDADE.md a partir dos títulos dos testes Cypress de
// Vendas (cypress/e2e/vendas/*.cy.js). Cada título termina com os requisitos que
// o teste cobre, ex.: "3. não deve permitir adicionar item indisponível ... (RN0031)".
//   npm run docs:matriz
const fs = require('fs');
const path = require('path');

const raiz = path.join(__dirname, '..');
const pasta = path.join(raiz, 'cypress', 'e2e', 'vendas');

const DRS = {
  RF0031: 'Gerenciar carrinho de compra', RF0032: 'Definir quantidade de itens no carrinho', RF0033: 'Realizar compra',
  RF0034: 'Calcular frete (itens + endereço)', RF0035: 'Selecionar endereço de entrega', RF0036: 'Selecionar forma de pagamento',
  RF0037: 'Uso de cupom para pagamento', RF0038: 'Finalizar compra (EM PROCESSAMENTO)', RF0039: 'Despachar produtos para entrega',
  RF0040: 'Produtos entregues', RF0041: 'Solicitar troca', RF0042: 'Autorizar trocas', RF0043: 'Visualização de trocas',
  RF0044: 'Confirmar recebimento de itens para troca', RF0045: 'Gerar cupom de troca após recebimento', RF0053: 'Dar baixa em estoque',
  RF0054: 'Realizar reentrada em estoque',
  RN0028: 'Validar retorno da operadora (baixa só após efetivada)',
  RN0031: 'Validar estoque para adição de itens no carrinho', RN0032: 'Validar estoque para compra',
  RN0033: 'Uso de cupom promocional (um por compra)', RN0034: 'Uso de diversos cartões (mínimo R$ 10,00 cada)',
  RN0035: 'Cupons junto a cartão (dispensa o mínimo)', RN0036: 'Gerar cupom de troca (troco)', RN0037: 'Validar forma de pagamento (automático)',
  RN0038: 'Status APROVADA/REPROVADA (automático)', RN0039: 'Status EM TRANSPORTE', RN0040: 'Status ENTREGUE',
  RN0041: 'Gerar pedido de troca (EM TROCA)', RN0042: 'Status TROCADO após recebimento', RN0043: 'Só itens ENTREGUES podem ser trocados',
  RN0044: 'Bloqueio de produtos no carrinho', RN0045: 'Retirar item do carrinho ao desbloquear', RN0046: 'Notificação de autorização de troca',
  RNF0042: 'Apresentar itens retirados do carrinho', RNF0012: 'Log de transação (escrita)', RNF0011: 'Tempo de resposta (consultas)',
};
// Roteiro mínimo de demonstração (enunciado, seção 7): cada cenário aponta para os testes Cypress que o apresentam.
const ROTEIRO_MINIMO = [
  ['Compra aprovada com um cartão', ['6', '12']],
  ['Compra com dois ou mais cartões, com a tentativa de pagar menos de R$ 10,00 em um deles bloqueada', ['33']],
  ['Compra com cupom promocional e cartão, com menos de R$ 10,00 no cartão (permitido)', ['34', '10']],
  ['Compra paga com cupons de troca de valor superior ao da compra, com emissão do cupom de diferença', ['35']],
  ['Compra REPROVADA, com itens desbloqueados e sem baixa de estoque', ['13', '12']],
  ['Expiração do bloqueio no carrinho, com remoção dos itens e desativação do botão de compra', ['5']],
  ['Fluxo do administrador: APROVADA, EM TRANSPORTE, ENTREGUE', ['24', '25', '26']],
  ['Tentativa de solicitar a troca de um pedido que não está ENTREGUE (bloqueada)', ['17']],
  ['Troca de um item de um pedido e troca de uma compra inteira', ['16']],
  ['Fluxo da troca: solicitação, visualização pelo administrador, autorização com notificação, recebimento, cupom e reentrada no estoque', ['27', '28', '29', '30']],
  ['Uso do cupom de troca gerado em uma nova compra', ['31']],
];

const SITUACAO_ESPECIAL = {
  RNF0012: 'Aspecto `LogDeEscritaAspect`; verificado pelo teste JUnit `LogDeEscritaIntegrationTest` (e visível no terminal do backend)',
  RF0034: 'Coberto (frete = tarifa do estado + R$ 3,00 por unidade extra — regra do grupo, ver `docs/JUSTIFICATIVA_FRETE.md`)',
  RN0028: 'Coberto (testes 12 e 13; itens de pagamento não aprovado voltam a ficar disponíveis)',
};

const expandir = (texto) => {
  const reqs = new Set(texto.match(/R(?:F|N|NF)\d{4}/g) || []);
  for (const [, pref, a, b] of texto.matchAll(/(R[FN])(\d{4})\s*(?:-|a)\s*R[FN](\d{4})/g)) {
    for (let n = Number(a); n <= Number(b); n++) reqs.add(`${pref}${String(n).padStart(4, '0')}`);
  }
  return reqs;
};

const testes = [];
for (const arq of fs.readdirSync(pasta).filter((f) => f.endsWith('.cy.js')).sort()) {
  const src = fs.readFileSync(path.join(pasta, arq), 'utf8');
  for (const m of src.matchAll(/it\('(\d+)\. (.*?)',\s*\(\)/g)) {
    const [, id, titulo] = m;
    const final = titulo.match(/\(([^()]*)\)\s*$/);
    const reqs = expandir(final ? final[1] : '');
    const completo = arq.startsWith('06-'); // o caso de uso completo percorre os requisitos em sequência
    testes.push({ id, titulo: titulo.replace(/\s*\([^()]*\)\s*$/, ''), arq, reqs: [...reqs].sort(), completo });
  }
}

for (const [, ids] of ROTEIRO_MINIMO) for (const id of ids) {
  if (!testes.some((t) => t.id === id)) throw new Error(`ROTEIRO_MINIMO cita o teste ${id}, que não existe`);
}
const titulos = Object.fromEntries(testes.map((t) => [t.id, t]));

const cobertura = {};
// as etapas do caso de uso completo (arquivo 06) passam por quase todos os requisitos; aparecem à parte, para a tabela mostrar o teste DEDICADO de cada um
for (const t of testes.filter((x) => !x.completo)) for (const r of t.reqs) (cobertura[r] ||= []).push(t.id);
const fluxo = testes.filter((t) => t.completo);

const L = [];
L.push('# Matriz de rastreabilidade — módulo de Vendas (CDU02)', '');
L.push('> **Gerada automaticamente** (`npm run docs:matriz`) a partir dos títulos dos testes em `cypress/e2e/vendas/*.cy.js`; cada título termina com os requisitos que ele cobre. Se mudar um teste, rode o comando de novo.', '');
L.push(`Base: \`DRS_LES_2_2026\` v0.6 · **${testes.length} testes automatizados Cypress** (${testes.length - fluxo.length} dedicados, um por requisito do escopo de Vendas + ${fluxo.length} etapas do caso de uso completo) + testes de integração JUnit da API (\`backend/src/test\`).`, '');
L.push('## 1. Roteiro mínimo de demonstração (enunciado, seção 7) → testes', '');
L.push('O enunciado pede que estes 11 cenários sejam apresentados por **testes funcionais automatizados**. Todos são testes Cypress desta suíte: `npm run test:vendas` roda todos e mostra o título de cada um; `npm run test:vendas:open` roda ao vivo no navegador.', '');
L.push('| Cenário | Descrição | Testes (nº) | Arquivo |', '|---|---|---|---|');
ROTEIRO_MINIMO.forEach(([desc, ids], i) => {
  const arquivos = [...new Set(ids.map((id) => '`' + titulos[id].arq + '`'))].join(', ');
  L.push(`| ${i + 1} | ${desc} | ${ids.join(', ')} | ${arquivos} |`);
});
L.push('', 'Os cenários 2, 3 e 4 (regras de pagamento) são testes de API (`cy.request`), porque a tela já impede a violação das regras; os demais passam pela interface. Os cenários 7, 10 e 11 são as etapas sequenciais, pela tela, do arquivo `06-caso-de-uso-completo.cy.js` (testes 22 a 31); os testes dedicados 14, 15 e 18 a 21 repetem esses passos isoladamente.', '');
L.push('## 2. Requisito → testes', '');
L.push('| Requisito | Descrição (DRS) | Teste dedicado (nº) | Situação |', '|---|---|---|---|');
const semTeste = [];
for (const [r, d] of Object.entries(DRS)) {
  const ts = cobertura[r] || [];
  let sit = ts.length ? 'Coberto' : 'Sem teste Cypress';
  if (SITUACAO_ESPECIAL[r]) sit = SITUACAO_ESPECIAL[r];
  else if (!ts.length) semTeste.push(r);
  L.push(`| ${r} | ${d} | ${ts.length ? ts.join(', ') : '—'} | ${sit} |`);
}
if (fluxo.length) {
  const reqsFluxo = [...new Set(fluxo.flatMap((t) => t.reqs))].sort();
  const fora = Object.keys(DRS).filter((r) => r.match(/^R[FN]\d/) && !reqsFluxo.includes(r));
  L.push('', `> A coluna "Teste dedicado" mostra o teste que existe só para aquele requisito. O arquivo \`${fluxo[0].arq}\` (**testes ${fluxo[0].id} a ${fluxo[fluxo.length - 1].id}**, sequenciais) percorre o caso de uso inteiro pela interface — do carrinho à troca e ao uso do cupom — e exercita ${reqsFluxo.length} requisitos (${reqsFluxo.join(', ')}); é o arquivo para rodar ao vivo na apresentação. Como segue só o caminho feliz, **não substitui** os dedicados: ficam de fora dele ${fora.join(', ')} (e o ramo REPROVADA da RN0038).`);
}
L.push('', '## 3. Caso de teste → requisitos', '');
L.push('| Nº | Título | Requisitos | Arquivo |', '|---|---|---|---|');
for (const t of testes) L.push(`| ${t.id} | ${t.titulo} | ${t.reqs.join(', ')} | \`${t.arq}\` |`);
L.push('', '## 4. Lacunas conhecidas', '');
L.push('- **RNF0012** (log de escrita): implementado por aspecto (`LogDeEscritaAspect`); como o log fica no servidor, a verificação automatizada é o teste JUnit `LogDeEscritaIntegrationTest` (`npm run test:backend`), e não um teste Cypress.');
L.push('- **RF0034** (frete): o DRS pede cálculo "com base nos itens e no endereço" mas não define a fórmula; a regra (tarifa por estado + R$ 3,00 por unidade a partir da 2ª) foi definida pelo grupo e está documentada em `docs/JUSTIFICATIVA_FRETE.md`.');
L.push('- **Fora do escopo, por orientação do professor:** recusa de troca e estorno financeiro **não** são implementados. A autorização da troca é o único desfecho previsto; o DRS RF0044 pede que o administrador informe se os itens trocados retornam ao estoque, e isso é mantido.');
L.push('- **Numeração dos requisitos:** alguns números do enunciado diferem do DRS v0.6 (ver a seção 5). Esta matriz e os títulos dos testes seguem a numeração do DRS v0.6.');
L.push('- **Cenários extras fora da suíte Cypress enxuta:** a suíte tem um teste dedicado por requisito do DRS. Caminhos adicionais (expiração com a tela aberta, dispensar item expirado, validações de formulário…) não têm teste Cypress; vários seguem cobertos pelos testes de integração JUnit (`npm run test:backend`), por exemplo bloqueio entre clientes, expiração sem o job e itens bloqueados enquanto o pagamento é validado.');
L.push('- **RNF0033** (senha criptografada) pertence ao módulo de Clientes e não é verificável pela interface.');
if (semTeste.length) L.push(`- Sem teste Cypress: ${semTeste.join(', ')}`);

L.push('', '## 5. Numeração do enunciado × DRS v0.6', '');
L.push('O texto do enunciado cita alguns requisitos com números diferentes dos do DRS v0.6 (a partir do RF0037 a lista aparece deslocada, porque o enunciado trata o uso de cupom dentro da forma de pagamento). Os testes e esta matriz usam os números do DRS v0.6; a tabela mostra a correspondência.', '');
L.push('| No enunciado | Assunto | No DRS v0.6 (testes) |', '|---|---|---|');
for (const [enun, assunto, drs] of [
  ['RF0036', 'Selecionar forma de pagamento (inclui cupons)', 'RF0036 e RF0037'],
  ['RF0037', 'Finalizar compra (EM PROCESSAMENTO)', 'RF0038'],
  ['RF0038', 'Despachar (EM TRANSPORTE)', 'RF0039'],
  ['RF0039', 'Confirmar a entrega (ENTREGUE)', 'RF0040'],
  ['RF0040', 'Solicitar troca', 'RF0041'],
  ['RF0041', 'Autorizar troca', 'RF0042'],
  ['RF0042', 'Visualizar pedidos EM TROCA', 'RF0043'],
  ['RF0043', 'Confirmar recebimento dos itens da troca', 'RF0044'],
  ['RF0044', 'Gerar o cupom de troca e disponibilizá-lo ao cliente', 'RF0045'],
  ['RNF0045', 'Retirar do carrinho os itens do produto desbloqueado', 'RN0045'],
  ['RNF0046', 'Notificação de troca autorizada', 'RN0046'],
]) L.push(`| ${enun} | ${assunto} | ${drs} |`);

fs.writeFileSync(path.join(raiz, 'docs', 'MATRIZ_RASTREABILIDADE.md'), L.join('\n') + '\n');
console.log(`Matriz gerada: ${testes.length} testes, ${Object.keys(cobertura).filter((r) => DRS[r]).length} requisitos do CDU02 cobertos${semTeste.length ? ' — sem teste: ' + semTeste : ''}.`);
