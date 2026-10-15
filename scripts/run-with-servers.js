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
//   node scripts/run-with-servers.js ao-vivo       -> roda o caso de uso completo (CTV100) com o navegador
//                                                     visível (Edge no Windows, Chrome nos demais), sem a
//                                                     janela inicial do Cypress — útil se ela abrir em branco
//
// Argumentos extras vão direto para o Cypress, por exemplo:
//   node scripts/run-with-servers.js vendas --browser edge
//   node scripts/run-with-servers.js ao-vivo --browser chrome
//
// Por que não o start-server-and-test? Ele encerra os servidores usando o
// "ps-tree", que no Windows chama o wmic.exe — removido do Windows 11 recente.
// Sem ele, o Java ficava rodando depois dos testes e a porta 8080 ficava presa.
// Aqui o encerramento usa "taskkill /T /F" (Windows) ou o grupo de processos (Linux/macOS).
// ---------------------------------------------------------------------------
const { spawn, spawnSync } = require('child_process');
const fs = require('fs');
const http = require('http');
const net = require('net');
const path = require('path');

const raiz = path.join(__dirname, '..');
// O pacote cypress não exporta "bin/cypress" por require.resolve, então montamos o caminho pela pasta do pacote.
const cypressCli = path.join(path.dirname(require.resolve('cypress/package.json')), 'bin', 'cypress');
const isWin = process.platform === 'win32';

// As sondas usam 127.0.0.1 (IPv4) de propósito: é o endereço em que o Cypress (Node 18 embutido) conecta;
// com "localhost" o Node daqui poderia achar o servidor por ::1 enquanto o Cypress, não.
const URL_BACK = 'http://127.0.0.1:8080/api/produtos';
// 5600 (e não a 5500): a 5500 é usada por outros programas comuns — Live Server do VS Code, Oracle (EM Express).
const PORTA_FRONT = 5600;
const URL_FRONT = `http://127.0.0.1:${PORTA_FRONT}/index.html`;

// Nos modos sem janela, a saída do backend (uma linha por operação — RNF0012) vai para um arquivo, para
// não encher o terminal e esconder o resultado dos testes. Nos modos de apresentação ela continua visível.
const LOG_BACK = path.join(raiz, 'logs', 'backend-testes.log');

const MODOS = {
  vendas: { back: true, front: true, logEmArquivo: true, cypress: ['run', '--spec', 'cypress/e2e/vendas/**/*.cy.js'] },
  cliente: { back: false, front: true, cypress: ['run', '--spec', 'cypress/e2e/cliente/**/*.cy.js'] },
  tudo: { back: true, front: true, logEmArquivo: true, cypress: ['run'] },
  'vendas-open': { back: true, front: true, cypress: ['open'] },
  'ao-vivo': {
    back: true, front: true, navegador: true,
    cypress: ['run', '--headed', '--spec', 'cypress/e2e/vendas/06-caso-de-uso-completo.cy.js'],
  },
};

const modo = MODOS[process.argv[2]];
if (!modo) {
  console.error(`Uso: node scripts/run-with-servers.js <${Object.keys(MODOS).join('|')}> [argumentos do Cypress]`);
  process.exit(2);
}

// Argumentos extras (depois do modo) vão para o Cypress. No modo "ao-vivo", se você não escolher o
// navegador, usa o Edge (já vem no Windows) ou o Chrome.
const extras = process.argv.slice(3);
const escolheuNavegador = extras.some((a) => a === '--browser' || a === '-b' || a.startsWith('--browser='));
const argsCypress = [
  ...modo.cypress,
  ...(modo.navegador && !escolheuNavegador ? ['--browser', isWin ? 'edge' : 'chrome'] : []),
  ...extras,
];

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

let ultimoErro = '';
function responde(url) {
  return new Promise((resolve) => {
    const req = http.get(url, (res) => {
      res.resume();
      ultimoErro = `HTTP ${res.statusCode}`;
      resolve(res.statusCode >= 200 && res.statusCode < 400);
    });
    req.on('error', (e) => { ultimoErro = e.code || e.message; resolve(false); });
    req.setTimeout(2000, () => { ultimoErro = 'tempo esgotado'; req.destroy(); resolve(false); });
  });
}

// Já existe algo escutando nesta porta? Tenta conectar em IPv4 e IPv6: qualquer conexão aceita
// (mesmo que o programa responda com erro ou derrube a conexão) significa que a porta está ocupada.
function portaEmUso(porta) {
  const tentar = (host) => new Promise((resolve) => {
    const s = net.connect({ port: porta, host });
    s.setTimeout(1500);
    s.once('connect', () => { s.destroy(); resolve(true); });
    s.once('timeout', () => { s.destroy(); resolve(false); });
    s.once('error', () => resolve(false));
  });
  return Promise.all([tentar('127.0.0.1'), tentar('::1')]).then((r) => r.some(Boolean));
}

function erroPortaOcupada(porta, quem, dica) {
  const achar = isWin
    ? `  1) netstat -ano | findstr :${porta}      (o número da última coluna é o PID)\n  2) tasklist /FI "PID eq <PID>"            (mostra o nome do programa)\n  3) taskkill /PID <PID> /F                 (encerra, se for seguro)`
    : `  lsof -i :${porta}   (mostra o programa; encerre-o com kill <PID>)`;
  return new Error(`A porta ${porta} (${quem}) já está em uso por outro programa. ${dica}\nPara descobrir qual:\n${achar}`);
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
  throw new Error(`${rotulo} não ficou pronto em ${Math.round(limiteMs / 1000)} s (${url} -> ${ultimoErro || 'sem resposta'}).`);
}

(async () => {
  // Evita testar contra um servidor "esquecido" (por exemplo, o backend em outro perfil) ou
  // contra outro programa que esteja na mesma porta (ex.: servidor de uma execução anterior ou outra ferramenta de desenvolvimento).
  if (modo.back && await portaEmUso(8080)) {
    throw erroPortaOcupada(8080, 'backend', 'Encerre o backend que está rodando (Ctrl+C no terminal dele): os testes precisam subir o backend no perfil padrão.');
  }
  if (await portaEmUso(PORTA_FRONT)) {
    throw erroPortaOcupada(PORTA_FRONT, 'frontend', 'Se for um servidor de uma execução anterior, encerre-o (Ctrl+C no terminal dele).');
  }

  let back = null;
  if (modo.back) {
    let stdioBack = ['ignore', 'inherit', 'inherit'];
    if (modo.logEmArquivo) {
      fs.mkdirSync(path.dirname(LOG_BACK), { recursive: true });
      const fd = fs.openSync(LOG_BACK, 'w');
      stdioBack = ['ignore', fd, fd];
      console.log(`[testes] o log do backend vai para ${path.relative(raiz, LOG_BACK)}`);
    }
    back = iniciar('backend (Spring Boot, porta 8080)', process.execPath, [path.join(__dirname, 'mvn.js'), 'spring-boot:run'], { stdio: stdioBack });
  }
  // Sem "-a": o http-server escuta em 0.0.0.0 (todas as interfaces IPv4), que atende 127.0.0.1.
  // (Não use "-a localhost": no Windows isso escuta só em ::1 e o Cypress, que conecta em 127.0.0.1, recebe ECONNREFUSED.)
  const front = iniciar('frontend (porta ' + PORTA_FRONT + ')', process.execPath,
    [require.resolve('http-server/bin/http-server'), 'frontend', '-p', String(PORTA_FRONT), '-c-1', '--silent']);

  if (back) await esperar('backend', URL_BACK, back, 10 * 60 * 1000);
  await esperar('frontend', URL_FRONT, front, 60 * 1000);

  console.log(`[testes] rodando: cypress ${argsCypress.join(' ')}\n`);
  const cypress = iniciar('cypress', process.execPath, [cypressCli, ...argsCypress], { stdio: 'inherit' });
  cypress.on('exit', (codigo, sinal) => {
    console.log(`\n[testes] Cypress terminou (${codigo === null ? sinal : 'código ' + codigo}). Encerrando backend e frontend...`);
    encerrar(codigo === null ? 1 : codigo);
  });
  cypress.on('error', (e) => { console.error('[testes] Não foi possível executar o Cypress:', e.message); encerrar(1); });
})().catch((e) => {
  console.error(`\n[testes] ERRO: ${e.message}`);
  if (modo.logEmArquivo && fs.existsSync(LOG_BACK)) {
    const fim = fs.readFileSync(LOG_BACK, 'utf8').split(/\r?\n/).filter(Boolean).slice(-30);
    if (fim.length) console.error(`\n[testes] últimas linhas do log do backend (${path.relative(raiz, LOG_BACK)}):\n${fim.join('\n')}`);
  }
  encerrar(1);
});
