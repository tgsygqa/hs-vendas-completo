#!/usr/bin/env node
// Volta a base de demonstração ao estado inicial (pedidos #1 e #2, 4 cupons,
// estoque original) SEM reiniciar o backend. Só funciona com o backend no ar
// e com app.test-support.enabled=true (padrão no H2).
const API = process.env.API_URL || 'http://localhost:8080/api';

fetch(`${API}/test-support/reset`, { method: 'POST' })
  .then((r) => {
    if (!r.ok) throw new Error(`HTTP ${r.status} — o backend está no ar? (perfil postgres desliga este endpoint)`);
    console.log('Base de demonstração restaurada: João Silva (cliente 1), pedidos #1 ENTREGUE e #2 EM TRANSPORTE, 4 cupons, estoque original.');
  })
  .catch((e) => { console.error('Falhou:', e.message); process.exit(1); });
