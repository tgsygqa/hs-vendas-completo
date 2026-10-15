#!/usr/bin/env node
// Define o estoque de um produto direto no backend, para demonstrar a regra
// RN0032 (o estoque muda entre o carrinho e a finalização da compra).
//   npm run demo:estoque -- 3 2      -> deixa o Headset (produto 3) com 2 unidades
// Produtos: 1 RTX 4090 · 2 Ryzen 9 · 3 Headset · 4 Teclado · 5 SSD
const API = process.env.API_URL || 'http://localhost:8080/api';
const [produtoId, quantidade] = process.argv.slice(2);

if (!produtoId || quantidade === undefined) {
  console.error('Uso: npm run demo:estoque -- <idDoProduto> <quantidade>   (ex.: npm run demo:estoque -- 3 2)');
  process.exit(1);
}

fetch(`${API}/test-support/produtos/${produtoId}/estoque?quantidade=${quantidade}`, { method: 'PUT' })
  .then((r) => {
    if (!r.ok) throw new Error(`HTTP ${r.status} — produto inexistente ou backend fora do ar`);
    console.log(`Estoque do produto ${produtoId} agora é ${quantidade}.`);
  })
  .catch((e) => { console.error('Falhou:', e.message); process.exit(1); });
