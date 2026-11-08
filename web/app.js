/*
 * NEXUS — o que acontece na tela.
 * Preferimos funções pequenas e nomes claros a um framework enorme.
 * Tudo fica no computador do usuário; não fazemos chamadas para serviços externos.
 */
'use strict';

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const ico = (name) => `<svg aria-hidden="true"><use href="#i-${name}"></use></svg>`;

function safe(value = '') {
  return String(value ?? '').replace(/[&<>"']/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[char]);
}

function money(value) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
    .format(Number(value) || 0);
}

function dateLabel(value, short = false) {
  if (!value) return 'Sem prazo';
  return new Date(value + 'T12:00:00').toLocaleDateString('pt-BR', short
    ? { day: '2-digit', month: 'short' }
    : { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function daysFromNow(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0')].join('-');
}

const statusName = { novo: 'A começar', andamento: 'Em andamento', concluido: 'Concluído' };
const pages = { inicio: 'INÍCIO', pedidos: 'PROJETOS', clientes: 'CLIENTES', servicos: 'SERVIÇOS' };
let state = { clientes: [], servicos: [], pedidos: [], demonstracao: false };
let activePage = 'inicio';
let noticeTimeout;

async function api(path, options = {}) {
  let response;
  try {
    response = await fetch(path, {
      headers: { 'Content-Type': 'application/json' },
      ...options
    });
  } catch {
    throw new Error('Sem conexão com o NEXUS. Tente abrir o programa novamente.');
  }

  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.error || 'Não foi possível salvar. Tente novamente.');
  return body;
}

async function reload() {
  state = await api('/api/state');
  render();
}

function toast(message, isError = false) {
  const element = $('#toast');
  element.textContent = message;
  element.className = (isError ? 'error ' : '') + 'show';
  clearTimeout(noticeTimeout);
  noticeTimeout = setTimeout(() => element.className = '', 4000);
}

const findClient = id => state.clientes.find(item => item.id === id);
const findService = id => state.servicos.find(item => item.id === id);
const findOrder = id => state.pedidos.find(item => item.id === id);

function changePage(page) {
  if (!(page in pages)) return;
  activePage = page;
  $('#sidebar').classList.remove('open');
  $('#shade').hidden = true;
  render();
  window.scrollTo(0, 0);
}

function render() {
  $$('.nav-link').forEach(link => link.classList.toggle('active', link.dataset.page === activePage));
  $('#breadcrumb').textContent = pages[activePage];
  $('#nav-total').textContent = state.pedidos.filter(item => item.status !== 'concluido').length;
  $('#main-content').innerHTML = ({
    inicio: overview,
    pedidos: ordersPage,
    clientes: clientsPage,
    servicos: servicesPage
  })[activePage]();
}

function intro(kicker, title, italic, text, kind, button) {
  return `<section class="page-intro">
    <div><div class="eyebrow">${safe(kicker)}</div>
      <h1>${safe(title)} <em>${safe(italic)}</em></h1>
      <p class="page-sub">${safe(text)}</p>
    </div>
    ${kind ? `<button class="btn btn-dark" data-add="${kind}">${ico('plus')}${safe(button)}</button>` : ''}
  </section>`;
}

function buttonCreate(kind, label) {
  return `<button class="btn btn-dark" data-add="${kind}">${ico('plus')}${safe(label)}</button>`;
}

function emptyBox(title, text, kind, label) {
  return `<section class="empty"><div class="empty-symbol">✳</div><h3>${safe(title)}</h3>
    <p>${safe(text)}</p>${kind ? buttonCreate(kind, label) : ''}</section>`;
}

function demoHint() {
  return state.demonstracao ? `<div class="banner">
    <span>✳ Você está vendo dados fictícios de exemplo. Pode editar ou excluir esses registros antes de usar com clientes reais.</span>
  </div>` : '';
}

function statusBadge(value) {
  return `<span class="status ${safe(value)}">${safe(statusName[value] || value)}</span>`;
}

function taskCard(order) {
  const client = findClient(order.clienteId);
  return `<button type="button" class="task" data-edit="pedidos" data-id="${safe(order.id)}">
    <small>PROJETO / ${safe(order.id.slice(0, 4).toUpperCase())}</small>
    <strong>${safe(order.titulo)}</strong>
    <p>${safe(client?.empresa || client?.nome || 'Sem cliente')}</p>
    <div class="task-meta"><span>${safe(dateLabel(order.prazo, true))}</span><b>${safe(money(order.valor))}</b></div>
  </button>`;
}

function boardColumn(status) {
  const items = state.pedidos.filter(order => order.status === status);
  return `<div class="board-col">
    <div class="board-col-header"><span><i></i>${safe(statusName[status])}</span><b>${items.length.toString().padStart(2, '0')}</b></div>
    ${items.length ? items.slice(0, 4).map(taskCard).join('') : '<div class="col-empty">Nada por aqui (ainda).</div>'}
  </div>`;
}

function deadlineCard(order) {
  const [year, month, day] = (order.prazo || '').split('-');
  const monthName = order.prazo ? new Date(+year, +month - 1, +day).toLocaleDateString('pt-BR', { month: 'short' }).toUpperCase() : '—';
  return `<div class="deadline"><div class="date-box"><strong>${safe(day || '—')}</strong><small>${safe(monthName)}</small></div>
    <div class="deadline-title"><strong>${safe(order.titulo)}</strong><small>${safe(findClient(order.clienteId)?.nome || 'Cliente')}</small></div></div>`;
}

function welcome() {
  return `<section class="onboarding">
    <div>
      <span class="eyebrow">COMECE PELO SIMPLES</span>
      <h2>Seu espaço começa <em>por aqui.</em></h2>
      <p>Todo projeto começa com alguém e uma ideia. Cadastre um cliente, anote seus serviços e acompanhe o trabalho acontecendo — sem planilha espalhada.</p>
      <button class="btn btn-outline" id="load-example">Explorar com dados de exemplo ${ico('arrow')}</button>
    </div>
    <div class="steps">
      <div class="step"><b>1</b><div><strong>Adicione uma pessoa</strong><span>Quem está do outro lado do projeto?</span></div><button data-go="clientes">Começar →</button></div>
      <div class="step"><b>2</b><div><strong>Liste o que você faz</strong><span>Seu catálogo, sem complicação.</span></div><button data-go="servicos">Serviços →</button></div>
      <div class="step"><b>3</b><div><strong>Crie seu primeiro projeto</strong><span>Valor, prazo e andamento num só lugar.</span></div><button data-go="pedidos">Projetos →</button></div>
    </div>
  </section>`;
}

function overview() {
  const active = state.pedidos.filter(order => order.status !== 'concluido');
  const unpaid = state.pedidos.filter(order => !order.pago).reduce((sum, order) => sum + Number(order.valor), 0);
  const finished = state.pedidos.filter(order => order.status === 'concluido').length;
  const upcoming = active.filter(order => order.prazo).sort((a, b) => a.prazo.localeCompare(b.prazo)).slice(0, 4);
  const firstVisit = state.clientes.length + state.servicos.length + state.pedidos.length === 0;

  return intro('O SEU DIA, MAIS ORGANIZADO', 'Bom trabalho', 'começa aqui.',
    'Clientes, prazos e ideias no mesmo espaço. Sem abrir dez abas para descobrir o que falta.',
    'pedidos', 'Novo projeto') + demoHint() + (firstVisit ? welcome() : '') + `
    <section class="hero"><div class="hero-body"><div class="eyebrow">NEXUS / WORKROOM</div>
      <h2>Menos tempo procurando.<br><em>Mais tempo fazendo.</em></h2>
      <p>Uma ferramenta pequena, feita para acompanhar o trabalho que importa.</p></div>
      <div class="hero-art" aria-hidden="true"><span class="dash"></span><span class="orbit"></span><span class="orbit"></span><span class="orbit"></span><strong>✳</strong></div>
    </section>
    <div class="stats">
      <div class="stat"><span class="stat-label">Projetos ativos</span><strong>${active.length}</strong><p>Entre novos e em andamento</p></div>
      <div class="stat"><span class="stat-label">A receber</span><strong>${safe(money(unpaid))}</strong><p>Soma de projetos não pagos</p></div>
      <div class="stat"><span class="stat-label">Concluídos</span><strong>${finished}</strong><p>Trabalhos finalizados</p></div>
      <div class="stat"><span class="stat-label">Clientes</span><strong>${state.clientes.length}</strong><p>Pessoas cadastradas</p></div>
    </div>
    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-header"><div><div class="eyebrow">EM MOVIMENTO</div><h2>Mapa do trabalho</h2></div><button class="text-btn" data-go="pedidos">Ver todos ${ico('arrow')}</button></div>
        <div class="board">${['novo', 'andamento', 'concluido'].map(boardColumn).join('')}</div>
        <div class="panel-bottom"><span>Clique em qualquer projeto para editar</span><span>${state.pedidos.length} no total</span></div>
      </section>
      <div class="side-stack"><section class="side-quote"><span>UMA ROTINA MAIS LEVE</span><h3>Organizar também é criar espaço.</h3></section>
      <section class="panel deadlines"><h3>Próximas entregas</h3>${upcoming.length ? upcoming.map(deadlineCard).join('') : '<p class="page-sub">Quando você marcar um prazo, ele aparece aqui.</p>'}</section></div>
    </div>`;
}

function searchBar(placeholder) {
  return `<label class="search-field">${ico('search')}<input id="search" type="search" placeholder="${safe(placeholder)}" autocomplete="off" aria-label="Buscar"></label>`;
}

function actions(kind, id) {
  return `<div class="row-actions">
    <button class="icon-button" data-edit="${kind}" data-id="${safe(id)}" title="Editar" aria-label="Editar">${ico('pencil')}</button>
    <button class="icon-button danger" data-delete="${kind}" data-id="${safe(id)}" title="Excluir" aria-label="Excluir">${ico('trash')}</button>
  </div>`;
}

function ordersPage() {
  return intro('CADA COISA NO SEU TEMPO', 'Seus', 'projetos.',
    'Um lugar para acompanhar o combinado: o que precisa começar, o que está andando e o que já foi entregue.',
    'pedidos', 'Novo projeto') + demoHint() + `
    <div class="toolbar">${searchBar('Buscar por projeto ou cliente…')}
      <div style="display:flex;gap:9px"><select class="field-select" id="status-filter" aria-label="Filtrar etapa">
      <option value="all">Todas as etapas</option><option value="novo">A começar</option><option value="andamento">Em andamento</option><option value="concluido">Concluído</option></select>
      <button class="btn btn-outline" id="export">${ico('download')}CSV</button></div></div>
    ${state.pedidos.length ? `<div class="table-shell"><table><thead><tr><th>Projeto</th><th>Cliente</th><th>Etapa</th><th>Valor</th><th>Prazo</th><th>Pago</th><th></th></tr></thead>
    <tbody id="rows">${state.pedidos.map(order => `<tr data-filter="${safe((order.titulo+' '+(findClient(order.clienteId)?.nome||'')+' '+(findClient(order.clienteId)?.empresa||'')).toLowerCase())}" data-status="${safe(order.status)}">
    <td><span class="cell-primary">${safe(order.titulo)}</span><small class="cell-secondary">${safe(findService(order.servicoId)?.nome || 'Serviço')}</small></td>
    <td>${safe(findClient(order.clienteId)?.nome || '—')}</td><td>${statusBadge(order.status)}</td>
    <td>${safe(money(order.valor))}</td><td>${safe(dateLabel(order.prazo, true))}</td>
    <td>${order.pago ? 'Sim' : 'Não'}</td><td>${actions('pedidos',order.id)}</td></tr>`).join('')}</tbody></table></div>`
      : emptyBox('Seu primeiro projeto está esperando.', 'Cadastre um cliente e um serviço antes de criar um projeto.', 'pedidos', 'Criar projeto')}`;
}

function clientsPage() {
  return intro('BONS PROJETOS COMEÇAM COM PESSOAS', 'Gente que', 'faz acontecer.',
    'Seus contatos, empresas e conversas importantes, sem depender da memória.',
    'clientes', 'Novo cliente') + demoHint() + `<div class="toolbar">${searchBar('Buscar cliente ou empresa…')}
    <span class="eyebrow">${state.clientes.length} CADASTRADO(S)</span></div>
    <div class="cards" id="cards">${state.clientes.length ? state.clientes.map(client => `<article class="person" data-filter="${safe((client.nome+' '+client.empresa+' '+client.email).toLowerCase())}">
      <div class="card-top"><div class="initials">${safe(client.nome.slice(0,1).toUpperCase())}</div>${actions('clientes',client.id)}</div>
      <h3>${safe(client.nome)}</h3><p>${safe(client.empresa || 'Contato independente')}</p>
      ${client.email ? `<a href="mailto:${safe(client.email)}">${safe(client.email)}</a>` : ''}
      <div class="card-bottom"><span>${safe(client.telefone || 'Sem telefone')}</span><span>${state.pedidos.filter(o => o.clienteId===client.id).length} projeto(s)</span></div>
    </article>`).join('') : emptyBox('Vamos começar pelos nomes.', 'Adicione as pessoas com quem você trabalha.', 'clientes', 'Cadastrar cliente')}</div>`;
}

function servicesPage() {
  return intro('O QUE VOCÊ FAZ TEM VALOR', 'Seu', 'catálogo.',
    'Organize os serviços que oferece, com descrições simples e preços de referência.',
    'servicos', 'Novo serviço') + demoHint() + `<div class="toolbar">${searchBar('Buscar serviço ou categoria…')}
    <span class="eyebrow">${state.servicos.length} NO CATÁLOGO</span></div>
    <div class="cards" id="cards">${state.servicos.length ? state.servicos.map(service => `<article class="service" data-filter="${safe((service.nome+' '+service.categoria+' '+service.descricao).toLowerCase())}">
      <div class="card-top"><span class="service-tag">${safe(service.categoria || 'Geral')}</span>${actions('servicos',service.id)}</div>
      <h3>${safe(service.nome)}</h3><p>${safe(service.descricao || 'Serviço cadastrado no catálogo.')}</p>
      <strong class="service-price">${safe(money(service.preco))}</strong>
      <div class="card-bottom"><span>Preço de referência</span><button class="text-btn" data-edit="servicos" data-id="${safe(service.id)}">Editar ${ico('arrow')}</button></div>
    </article>`).join('') : emptyBox('O que você entrega?', 'Comece listando o primeiro serviço que oferece.', 'servicos', 'Adicionar serviço')}</div>`;
}

// Formulários: todos usam as mesmas regras de criação e edição.
function input(name, label, type, value, attributes = '') {
  return `<div class="field"><label for="f-${name}">${safe(label)}</label>
    <input type="${type}" id="f-${name}" name="${name}" value="${safe(value ?? '')}" ${attributes}></div>`;
}

function textarea(name, label, value, max = 500) {
  return `<div class="field full"><label for="f-${name}">${safe(label)}</label>
    <textarea id="f-${name}" name="${name}" maxlength="${max}" placeholder="Escreva aqui…">${safe(value || '')}</textarea></div>`;
}

function dropdown(name, label, options, selected) {
  return `<div class="field"><label for="f-${name}">${safe(label)}</label>
    <select name="${name}" id="f-${name}" required>${options.map(option =>
      `<option value="${safe(option.value)}" ${option.value===selected?'selected':''}>${safe(option.label)}</option>`).join('')}</select></div>`;
}

function formFields(kind, row) {
  if (kind === 'clientes') return `
    ${input('nome','Nome *','text',row.nome,'required maxlength="90" placeholder="Ex.: Ana Oliveira"')}
    ${input('empresa','Empresa / marca','text',row.empresa,'maxlength="100" placeholder="Ex.: Café da Ana"')}
    ${input('email','E-mail','email',row.email,'maxlength="120" placeholder="contato@exemplo.com"')}
    ${input('telefone','Telefone','tel',row.telefone,'maxlength="35" placeholder="(31) 99999-9999"')}`;

  if (kind === 'servicos') return `
    ${input('nome','Serviço *','text',row.nome,'required maxlength="100" placeholder="Ex.: Landing page"')}
    ${input('categoria','Categoria','text',row.categoria,'maxlength="45" placeholder="Ex.: Web"')}
    ${input('preco','Preço de referência (R$) *','number',row.preco??0,'required min="0" max="100000000" step="0.01"')}
    ${textarea('descricao','O que está incluso?',row.descricao,280)}`;

  if (kind === 'pedidos') {
    if (!state.clientes.length || !state.servicos.length) return `<p class="page-sub">Antes de criar um projeto, adicione pelo menos um cliente e um serviço.</p>
      <div style="display:flex;gap:9px;margin-top:20px;flex-wrap:wrap"><button class="btn btn-outline" type="button" data-close-go="clientes">Ir para clientes</button><button class="btn btn-outline" type="button" data-close-go="servicos">Ir para serviços</button></div>`;

    const clientOptions = state.clientes.map(c => ({value:c.id,label:c.empresa?`${c.nome} — ${c.empresa}`:c.nome}));
    const serviceOptions = state.servicos.map(s => ({value:s.id,label:s.nome}));
    return `<div class="field full"><label for="f-titulo">Nome do projeto *</label><input name="titulo" id="f-titulo" required maxlength="110" value="${safe(row.titulo||'')}" placeholder="Ex.: Site da cafeteria"></div>
      ${dropdown('clienteId','Cliente *',clientOptions,row.clienteId||clientOptions[0].value)}
      ${dropdown('servicoId','Serviço *',serviceOptions,row.servicoId||serviceOptions[0].value)}
      ${input('valor','Valor combinado (R$) *','number',row.valor??state.servicos[0].preco,'required min="0" max="100000000" step="0.01"')}
      ${input('prazo','Prazo de entrega','date',row.prazo??daysFromNow(7),'')}
      ${dropdown('status','Etapa *',Object.keys(statusName).map(value=>({value,label:statusName[value]})),row.status||'novo')}
      <div class="field"><label>Pagamento</label><label class="check-line"><input type="checkbox" name="pago" ${row.pago?'checked':''}> Já foi pago</label></div>
      ${textarea('observacoes','Notas importantes',row.observacoes)}`;
  }
  return '';
}

function openDialog(kind, id = '') {
  const labels = { clientes: 'cliente', servicos: 'serviço', pedidos: 'projeto' };
  if (!labels[kind]) return;
  const records = state[kind];
  const row = records.find(item => item.id === id) || {};
  const editing = Boolean(id);
  const impossible = kind === 'pedidos' && (!state.clientes.length || !state.servicos.length);
  $('#modal-root').innerHTML = `<div class="modal-backdrop" id="backdrop"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="dialog-title">
    <div class="modal-head"><div><div class="eyebrow">NEXUS / ${editing?'EDIÇÃO':'NOVO REGISTRO'}</div>
      <h2 id="dialog-title">${editing?'Editar':'Novo'} ${labels[kind]}</h2></div>
      <button class="icon-button" id="dismiss" aria-label="Fechar">${ico('x')}</button></div>
    <form id="edit-form" data-kind="${kind}" data-id="${safe(id)}"><div class="form-grid">${formFields(kind,row)}</div>
      <div class="modal-actions"><button class="btn btn-plain" type="button" id="cancel">Cancelar</button>
        ${!impossible?`<button class="btn btn-orange" type="submit">${ico('check')}${editing?'Salvar mudanças':'Adicionar'}</button>`:''}
      </div></form></section></div>`;

  document.body.style.overflow = 'hidden';
  $('#dismiss').addEventListener('click',closeDialog);
  $('#cancel').addEventListener('click',closeDialog);
  $('#backdrop').addEventListener('click',event => { if (event.target.id === 'backdrop') closeDialog(); });
  $('#edit-form').addEventListener('submit',saveForm);
  $('#f-servicoId')?.addEventListener('change', event => {
    if (editing) return;
    const service = findService(event.target.value);
    if (service) $('#f-valor').value = service.preco;
  });
  $('#edit-form input:not([type="checkbox"])')?.focus();
}

function closeDialog() {
  $('#modal-root').innerHTML = '';
  document.body.style.overflow = '';
}

async function saveForm(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const kind = form.dataset.kind;
  const id = form.dataset.id;
  const payload = Object.fromEntries(new FormData(form).entries());
  if (kind === 'pedidos') {
    payload.valor = Number(payload.valor);
    payload.pago = form.elements.pago.checked;
  }
  if (kind === 'servicos') payload.preco = Number(payload.preco);
  const submit = form.querySelector('button[type="submit"]');
  submit.disabled = true;
  try {
    await api(`/api/${kind}${id?'/'+encodeURIComponent(id):''}`, {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    });
    closeDialog();
    await reload();
    toast(id ? 'Pronto, suas alterações foram salvas.' : 'Adicionado ao seu espaço!');
  } catch (error) {
    toast(error.message, true);
    submit.disabled = false;
  }
}

async function deleteRecord(kind, id) {
  const names = {clientes:'cliente',servicos:'serviço',pedidos:'projeto'};
  if (!window.confirm(`Excluir este ${names[kind]}? Essa ação não pode ser desfeita.`)) return;
  try {
    await api(`/api/${kind}/${encodeURIComponent(id)}`, {method: 'DELETE'});
    await reload();
    toast('Registro excluído.');
  } catch (error) { toast(error.message, true); }
}

function exportCSV() {
  const headings = ['Projeto','Cliente','Serviço','Etapa','Valor (R$)','Prazo','Pago','Observações'];
  const rows = state.pedidos.map(order => [order.titulo,findClient(order.clienteId)?.nome||'',
    findService(order.servicoId)?.nome||'',statusName[order.status]||order.status,
    Number(order.valor).toFixed(2).replace('.',','),order.prazo,order.pago?'Sim':'Não',order.observacoes||'']);
  const csv = [headings,...rows].map(row => row.map(value => `"${String(value??'').replaceAll('"','""')}"`).join(';')).join('\r\n');
  const href = URL.createObjectURL(new Blob(['\uFEFF'+csv],{type:'text/csv;charset=utf-8'}));
  const link = document.createElement('a');
  link.href = href;
  link.download = 'nexus-projetos.csv';
  link.click();
  setTimeout(()=>URL.revokeObjectURL(href),1000);
  toast('Planilha CSV exportada.');
}

function filterResults() {
  const typed = ($('#search')?.value || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  const status = $('#status-filter')?.value || 'all';
  $$('[data-filter]').forEach(item => {
    const text = item.dataset.filter.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    item.hidden = !(text.includes(typed) && (status === 'all' || status === item.dataset.status));
  });
}

async function showDemo() {
  if (!window.confirm('Carregar dados fictícios para explorar o NEXUS? Eles ficarão salvos até você excluir cada registro.')) return;
  try {
    state = await api('/api/example', {method:'POST'});
    render();
    toast('Tudo pronto. Agora você pode explorar os exemplos.');
  } catch (error) { toast(error.message,true); }
}

async function exitApp() {
  if (!window.confirm('Encerrar o NEXUS? Tudo que você salvou continuará no seu computador.')) return;
  try {
    await api('/api/exit', {method:'POST'});
    $('#main-content').innerHTML = `<section class="empty" style="margin-top:60px"><span class="empty-symbol">✳</span>
    <h3>Até a próxima!</h3><p>O NEXUS foi encerrado. Seus dados estão salvos no seu computador.</p>
    <p>Você já pode fechar esta aba. Para voltar, abra o NEXUS novamente.</p></section>`;
    toast('NEXUS encerrado.');
  } catch (error) { toast(error.message,true); }
}

document.addEventListener('click', event => {
  const nav = event.target.closest('[data-page]');
  if (nav) return changePage(nav.dataset.page);
  const go = event.target.closest('[data-go]');
  if (go) return changePage(go.dataset.go);
  const closeGo = event.target.closest('[data-close-go]');
  if (closeGo) { closeDialog(); return changePage(closeGo.dataset.closeGo); }
  const add = event.target.closest('[data-add]');
  if (add) return openDialog(add.dataset.add);
  const edit = event.target.closest('[data-edit]');
  if (edit) return openDialog(edit.dataset.edit, edit.dataset.id);
  const remove = event.target.closest('[data-delete]');
  if (remove) return deleteRecord(remove.dataset.delete, remove.dataset.id);
  if (event.target.closest('#export')) return exportCSV();
  if (event.target.closest('#load-example')) return showDemo();
});

document.addEventListener('input', event => { if (event.target.id === 'search') filterResults(); });
document.addEventListener('change', event => { if (event.target.id === 'status-filter') filterResults(); });
document.addEventListener('keydown', event => { if (event.key === 'Escape' && $('#backdrop')) closeDialog(); });
$('#mobile-menu').addEventListener('click', () => { $('#sidebar').classList.add('open'); $('#shade').hidden = false; });
$('#shade').addEventListener('click', () => { $('#sidebar').classList.remove('open'); $('#shade').hidden = true; });
$('#close-app').addEventListener('click', exitApp);
$('#today').textContent = new Date().toLocaleDateString('pt-BR', {weekday:'long',day:'numeric',month:'long'});

reload().catch(error => {
  $('#main-content').innerHTML = emptyBox('Não conseguimos abrir seus dados.',error.message,null,null);
  toast(error.message,true);
});
