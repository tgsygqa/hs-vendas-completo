#!/usr/bin/env node
// ---------------------------------------------------------------------------
// Executa o Maven Wrapper do backend (backend/mvnw ou backend/mvnw.cmd), em
// qualquer sistema operacional, SEM precisar ter o Maven instalado - so o
// JDK 17+ (a primeira execucao baixa o Maven e as dependencias, leva alguns
// minutos). Usado pelos scripts do package.json:
//
//   node scripts/mvn.js spring-boot:run
//   node scripts/mvn.js spring-boot:run -Dspring-boot.run.profiles=postgres
//   node scripts/mvn.js test
// ---------------------------------------------------------------------------
const { spawn } = require('child_process');
const path = require('path');

const backendDir = path.join(__dirname, '..', 'backend');
const isWin = process.platform === 'win32';
const args = process.argv.slice(2);

// No Windows, .cmd precisa de shell. No Linux/macOS usa "sh mvnw", que funciona
// mesmo se o arquivo perdeu a permissao de execucao ao passar pelo Git/zip.
const comando = isWin ? 'mvnw.cmd' : 'sh';
const argumentos = isWin ? args : ['./mvnw', ...args];

const filho = spawn(comando, argumentos, { cwd: backendDir, stdio: 'inherit', shell: isWin });

filho.on('error', (e) => {
  console.error('Nao foi possivel executar o Maven Wrapper:', e.message);
  console.error('Confira se o JDK 17+ esta instalado: abra um terminal e rode "java -version".');
  process.exit(1);
});
filho.on('exit', (codigo, sinal) => process.exit(codigo === null ? (sinal ? 1 : 0) : codigo));

// Ctrl+C no terminal derruba tambem o Maven/Spring Boot.
['SIGINT', 'SIGTERM'].forEach((s) => process.on(s, () => filho.kill(s)));
