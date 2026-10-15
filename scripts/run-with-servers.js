#!/usr/bin/env node
// ---------------------------------------------------------------------------
// Sobe o backend e/ou o frontend, espera ficarem prontos, roda o Cypress e, no
// fim (passando, falhando ou com Ctrl+C), DERRUBA tudo — inclusive o Java do
// Spring Boot, que é "neto" do processo que iniciamos.
//
//   node scripts/run-with-servers.js vendas        -> 81 testes de Vendas (headless)
//   node scripts/run-with-servers.js cliente       -> 64 testes de Clientes (só frontend)
//   node scripts/run-with-servers.js tudo          -> Clientes + Vendas
//   node scripts/run-with-servers.js vendas-open   -> abre a interface do Cypress
//
// Por que não o start-server-and-test? Ele encerra os servidores usando o
// "ps-tree", que no Windows chama o wmic.exe — removido do Windows 11 recente.
// Sem ele, o Java ficava rodando depois dos testes e a porta 8080 ficava presa.
// Aqui o encerramento usa "taskkill /T /F" (Windows) ou o grupo de processos (Linux/macOS).
// ---------------------------------------------------------------------------
const { spawn, spawnSync } = require('child_process');
const http = require('http');
const path = require('path');

const raiz = path.join(__dirname, '..');
// O pacote cypress não exporta "bin/cypress" por require.resolve, então montamos o caminho pela pasta do pacote.
const cypressCli = path.join(path.dirname(require.resolve('cypress/package.json')), 'bin', 'cypress');
const isWin = process.platform === 'win32';

const URL_BACK = 'http://localhost:8080/api/produtos';
const URL_FRONT = 'http://localhost:5500/index.html';

const MODOS = {
  vendas: { back: true, front: true, cypress: ['run', '--spec', 'cypress/e2e/vendas/**/*.cy.js'] },
  cliente: { back: false, front: true, cypress: ['run', '--spec', 'cypress/e2e/cliente/**/*.cy.js'] },
  tudo: { back: true, front: true, cypress: ['run'] },
  'vendas-open': { back: true, front: true, cypress: ['open'] },
};

const modo = MODOS[process.argv[2]];
if (!modo) {
  console.error(`Uso: node scripts/run-with-servers.js <${Object.keys(MODOS).join('|')}>`);
  process.exit(2);
}

const filhos = [];

function iniciar(rotulo, comando, args, opcoes = {}) {
  console.log(`[testes] iniciando ${rotulo}...`);
  const filho = spawn(comando, args, {
    cwd: raiz,
    stdio: ['ignore', 'inherit', 'inherit'],
    detached: !isWin, // Linux/macOS: novo grupo de processos, para derrubar a árvore inteira
    ...opcoes,
  });
  filho.rotulo = rotulo;
  filhos.push(filho);
  return filho;
}

function derrubar(filho) {
  if (!filho || filho.pid === undefined || filho.exitCode !== null) return;
  try {
    if (isWin) spawnSync('taskkill', ['/PID', String(filho.pid), '/T', '/F'], { stdio: 'ignore' });
    else process.kill(-filho.pid, 'SIGTERM');
  } catch (_) { /* já terminou */ }
}

let encerrando = false;
function encerrar(codigo) {
  if (encerrando) return;
  encerrando = true;
  filhos.forEach(derrubar);
  // dá um instante para o Windows/Java liberarem as portas
  setTimeout(() => process.exit(codigo), 500);
}
['SIGINT', 'SIGTERM'].forEach((s) => process.on(s, () => encerrar(130)));

function responde(url) {
  return new Promise((resolve) => {
    const req = http.get(url, (res) => { res.resume(); resolve(res.statusCode >= 200 && res.statusCode < 400); });
    req.on('error', () => resolve(false));
    req.setTimeout(2000, () => { req.destroy(); resolve(false); });
  });
}

async function esperar(rotulo, url, filho, limiteMs) {
  const fim = Date.now() + limiteMs;
  let ultimoAviso = Date.now();
  while (Date.now() < fim) {
    if (filho.exitCode !== null) throw new Error(`${rotulo} terminou antes de ficar pronto (código ${filho.exitCode}). Veja o erro acima.`);
    if (await responde(url)) { console.log(`[testes] ${rotulo} pronto.`); return; }
    if (Date.now() - ultimoAviso > 20000) {
      console.log(`[testes] ainda aguardando ${rotulo}... (na primeira vez o Maven baixa dependências e pode levar alguns minutos)`);
      ultimoAviso = Date.now();
    }
    await new Promise((r) => setTimeout(r, 1000));
  }
  throw new Error(`${rotulo} não ficou pronto em ${Math.round(limiteMs / 1000)} s.`);
}

(async () => {
  // Evita testar contra um servidor "esquecido" (por exemplo, o backend em outro perfil).
  if (modo.back && await responde(URL_BACK)) {
    throw new Error('Já existe um backend respondendo na porta 8080. Encerre-o (Ctrl+C no terminal dele) e rode de novo — os testes precisam subir o backend no perfil padrão.');
  }
  if (await responde(URL_FRONT)) {
    throw new Error('Já existe um servidor na porta 5500. Encerre-o (Ctrl+C no terminal dele) e rode de novo.');
  }

  let back = null;
  if (modo.back) back = iniciar('backend (Spring Boot, porta 8080)', process.execPath, [path.join(__dirname, 'mvn.js'), 'spring-boot:run']);
  const front = iniciar('frontend (porta 5500)', process.execPath,
    [require.resolve('http-server/bin/http-server'), 'frontend', '-p', '5500', '-c-1', '--silent']);

  if (back) await esperar('backend', URL_BACK, back, 10 * 60 * 1000);
  await esperar('frontend', URL_FRONT, front, 60 * 1000);

  console.log(`[testes] rodando: cypress ${modo.cypress.join(' ')}\n`);
  const cypress = iniciar('cypress', process.execPath, [cypressCli, ...modo.cypress], { stdio: 'inherit' });
  cypress.on('exit', (codigo, sinal) => {
    console.log(`\n[testes] Cypress terminou (${codigo === null ? sinal : 'código ' + codigo}). Encerrando backend e frontend...`);
    encerrar(codigo === null ? 1 : codigo);
  });
  cypress.on('error', (e) => { console.error('[testes] Não foi possível executar o Cypress:', e.message); encerrar(1); });
})().catch((e) => {
  console.error(`\n[testes] ERRO: ${e.message}`);
  encerrar(1);
});
