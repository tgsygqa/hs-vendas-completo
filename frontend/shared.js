// ===========================================================================
// Utilitarios compartilhados pelas telas de Vendas (vendas.html, pedidos.html,
// admin.html): endereco da API, chamada HTTP, formatacao e navegacao.
// ===========================================================================

// Cliente exibido nas telas do CLIENTE (loja e "meus pedidos"). O projeto nao
// tem autenticacao (fora do escopo do DRS), entao o cliente e escolhido pela
// URL: ?clienteId=2. Sem parametro, usa o Joao Silva semeado (id 1).
const CLIENTE_ID = Number(new URLSearchParams(window.location.search).get('clienteId')) || 1;

// Em localhost a API fica em :8080; no GitHub Codespaces, a porta 5500 do front
// vira a porta 8080 do back trocando o numero no host (ex.: xxx-5500.app.github.dev).
const API_BASE = (() => {
  const { hostname, protocol, port } = window.location;
  if (hostname.endsWith('.github.dev')) {
    const p = port || '5500';
    const novoHost = hostname.replace(`-${p}.`, '-8080.');
    return `${protocol}//${novoHost}/api`;
  }
  return 'http://localhost:8080/api';
})();

function fmt(v) {
  return (Number(v) || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function fmtData(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

// EM_PROCESSAMENTO -> "EM PROCESSAMENTO" (como no DRS). Os testes conferem o
// valor tecnico pelo atributo data-status, nao por este texto.
function statusTexto(status) {
  return String(status || '').replace(/_/g, ' ');
}

function esc(t) {
  return String(t ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

async function api(path, options = {}) {
  const resp = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
  });
  const texto = await resp.text();
  const corpo = texto ? JSON.parse(texto) : null;
  if (!resp.ok) {
    let msg = (corpo && (corpo.mensagem || corpo.message)) || `Erro ${resp.status}`;
    // Erros de validacao (400) trazem o detalhe por campo em "campos".
    if (corpo && corpo.campos) msg += ' ' + Object.values(corpo.campos).join(' ');
    throw new Error(msg);
  }
  return corpo;
}

function mostrarErro(msg) {
  const el = document.getElementById('msg-erro');
  const ok = document.getElementById('msg-sucesso');
  if (ok) ok.style.display = 'none';
  el.textContent = msg;
  el.style.display = 'block';
  el.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function mostrarSucesso(msg) {
  const el = document.getElementById('msg-sucesso');
  esconderErro();
  el.textContent = msg;
  el.style.display = 'block';
}

function esconderErro() {
  const el = document.getElementById('msg-erro');
  if (el) el.style.display = 'none';
}

// Barra de navegacao do topo, igual em todas as telas de Vendas. "ativo" e o
// id da pagina atual. Os links da area do cliente preservam o ?clienteId=.
function renderNav(ativo) {
  const q = new URLSearchParams(window.location.search).get('clienteId');
  const sufixo = q ? `?clienteId=${encodeURIComponent(q)}` : '';
  const link = (id, href, texto, comCliente) =>
    `<a href="${href}${comCliente ? sufixo : ''}" data-cy="nav-${id}" class="${ativo === id ? 'ativo' : ''}">${texto}</a>`;
  return `<nav class="nav">
    ${link('loja', 'vendas.html', '🛒 Loja', true)}
    ${link('pedidos', 'pedidos.html', '📦 Meus pedidos', true)}
    ${link('admin', 'admin.html', '🛠️ Administração', false)}
    ${link('clientes', 'V08.html', '👤 Clientes', false)}
  </nav>`;
}
