'use strict';
// Void Client – interfejs launchera (bez frameworków = szybki start).

const $ = id => document.getElementById(id);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const ICON_EDIT = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/></svg>';
const ICON_PLUS = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg>';

let S = null; // ustawienia
let versions = null;
let running = false;
const startedAt = Date.now();
let gameSince = 0;

// ───────── Pomocnicze ─────────

let toastTimer;
function toast(msg, kind = '') {
  const t = $('toast');
  t.textContent = msg;
  t.dataset.kind = kind;
  if (kind === 'ok') window.voidSfx?.success();
  if (kind === 'err') window.voidSfx?.error();
  t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), 3200);
}

async function save(patch) {
  Object.assign(S, patch);
  try { S = await window.api.setSettings(patch); } catch (e) { toast(e.message); }
}

const profile = () => S.profiles.find(p => p.id === S.selectedProfile) || S.profiles[0];
const mcLabel = p => (p.mc === 'latest' ? 'najnowsza' : p.mc);
const loaderLabel = p => (p.loader === 'fabric' ? 'Fabric' : 'Vanilla');
const fmt = n => (n >= 1e6 ? (n / 1e6).toFixed(1).replace('.0', '') + ' mln' : n >= 1e3 ? Math.round(n / 1e3) + ' tys.' : String(n));

// ───────── Zakładki ─────────

function goTab(tab) {
  const navs = [...document.querySelectorAll('.side .nav')];
  const b = navs.find(n => n.dataset.tab === tab);
  if (!b) return;
  navs.forEach(n => n.classList.toggle('active', n === b));
  document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.id === 'tab-' + tab));
  $('ind').style.transform = `translateY(${b.offsetTop - navs[0].offsetTop}px)`;
  $('vtitle').textContent = b.dataset.title || '';
  document.body.dataset.view = tab;
  if (tab === 'mods' && !modsLoaded) { modsLoaded = true; runSearch(); }
  if (tab === 'cosmetics') b.querySelector('.nav-new')?.remove();
}
document.querySelector('.side').addEventListener('click', e => {
  const b = e.target.closest('.nav');
  if (b) goTab(b.dataset.tab);
});
document.addEventListener('click', e => {
  const g = e.target.closest('[data-goto]');
  if (g) { goTab(g.dataset.goto); if (g.dataset.goto === 'friends') window.api.refreshFriends().then(renderFriends).catch(() => {}); }
});
window.addEventListener('resize', () => goTab(document.body.dataset.view || 'play'));

// ───────── Pasek tytułu i konto ─────────

$('minBtn').onclick = () => window.api.minimize();
$('closeBtn').onclick = () => window.api.close();

function renderAccount() {
  if (typeof renderFriends === 'function' && $('friendsList')) setTimeout(() => renderFriends(), 0);
  const head = $('accHead');
  if (S.account) {
    $('accName').textContent = S.account.name;
    $('accSub').textContent = 'Konto Microsoft';
    head.src = 'https://mc-heads.net/avatar/' + encodeURIComponent(String(S.account.uuid || S.account.name).replace(/-/g, '')) + '/64';
    head.hidden = false;
  } else {
    head.hidden = true;
    $('accName').textContent = 'Zaloguj się';
    $('accSub').textContent = 'Konto Microsoft';
  }
}

$('accountBtn').onclick = async () => {
  if (S.account) { toast('Zalogowano jako ' + S.account.name + '. Wylogujesz się w Opcjach.'); return; }
  $('accSub').textContent = 'Logowanie…';
  try {
    S.account = await window.api.login();
    toast('Zalogowano: ' + S.account.name, 'ok');
    renderSkin();
  } catch (e) {
    toast(e.message, 'err');
  }
  renderAccount();
};

$('logoutBtn').onclick = async () => {
  await window.api.logout();
  S.account = null;
  renderAccount();
  renderSkin();
  toast('Wylogowano');
};

// ───────── Profile ─────────

function renderProfiles() {
  const box = $('profiles');
  box.innerHTML = S.profiles.map(p => `
    <div class="profile ${p.id === S.selectedProfile ? 'on' : ''} ${p.loader === 'vanilla' ? 'vanilla' : ''}" data-id="${esc(p.id)}" role="button" tabindex="0">
      <i class="pblock" aria-hidden="true"></i>
      <span class="ptext"><b>${esc(p.name)}</b>
      <small>${esc(mcLabel(p))} · ${loaderLabel(p)}${p.optimize ? ' · FPS' : ''}</small></span>
      <button class="edit" type="button" data-edit="${esc(p.id)}" aria-label="Edytuj profil ${esc(p.name)}">${ICON_EDIT}</button>
    </div>`).join('') +
    `<button class="profile add" type="button" id="addProfile" aria-label="Nowy profil">${ICON_PLUS}</button>`;
  const p = profile();
  $('playName').textContent = p.name;
  $('playMeta').textContent = `${mcLabel(p)} · ${loaderLabel(p)} · RAM ${S.ram} GB`;
  if (!running) $('rpcLine2').textContent = `${p.name} · ${mcLabel(p)}`;
  renderModsProfiles();
}

$('profiles').addEventListener('click', e => {
  const edit = e.target.closest('[data-edit]');
  if (edit) { openProfile(edit.dataset.edit); return; }
  if (e.target.closest('#addProfile')) { openProfile(null); return; }
  const card = e.target.closest('.profile[data-id]');
  if (card) { save({ selectedProfile: card.dataset.id }); renderProfiles(); }
});
$('profiles').addEventListener('keydown', e => {
  if ((e.key === 'Enter' || e.key === ' ') && e.target.matches('.profile[data-id]')) {
    e.preventDefault();
    save({ selectedProfile: e.target.dataset.id });
    renderProfiles();
  }
});

let editingId = null;
async function openProfile(id) {
  editingId = id;
  const p = id ? S.profiles.find(x => x.id === id) : { name: 'Nowy profil', mc: '1.21.11', loader: 'fabric', optimize: true, voidMod: true };
  $('pdTitle').textContent = id ? 'Edytuj profil' : 'Nowy profil';
  $('pdName').value = p.name;
  $('pdLoader').value = p.loader;
  $('pdOptimize').checked = !!p.optimize;
  $('pdVoid').checked = !!p.voidMod;
  $('pdDelete').classList.toggle('hidden', !id || S.profiles.length < 2);
  const sel = $('pdVersion');
  sel.innerHTML = `<option value="${esc(p.mc)}">${esc(mcLabel(p))}</option>`;
  $('profileDialog').showModal();
  try {
    versions = versions || await window.api.versions();
    sel.innerHTML = `<option value="latest">Najnowsza (${esc(versions.latest)})</option>` +
      versions.releases.map(v => `<option value="${esc(v)}">${esc(v)}</option>`).join('');
    sel.value = p.mc;
  } catch {
    toast('Brak internetu – lista wersji niedostępna');
  }
  syncVoidCheckbox();
}

async function syncVoidCheckbox() {
  const fabric = $('pdLoader').value === 'fabric';
  const v = $('pdVersion').value;
  const mc = v === 'latest' ? (versions && versions.latest) : v;
  let ok = false;
  if (fabric && mc) {
    try { ok = await window.api.voidSupported(mc); } catch { ok = false; }
  }
  $('pdVoid').disabled = !ok;
  if (!ok) $('pdVoid').checked = false;
  $('pdOptimize').disabled = !fabric;
  if (!fabric) $('pdOptimize').checked = false;
}
$('pdLoader').onchange = syncVoidCheckbox;
$('pdVersion').onchange = syncVoidCheckbox;
$('pdCancel').onclick = () => $('profileDialog').close();

$('profileForm').addEventListener('submit', () => {
  const data = {
    name: $('pdName').value.trim() || 'Profil',
    mc: $('pdVersion').value,
    loader: $('pdLoader').value,
    optimize: $('pdOptimize').checked,
    voidMod: $('pdVoid').checked
  };
  let profiles;
  if (editingId) {
    profiles = S.profiles.map(p => (p.id === editingId ? { ...p, ...data } : p));
  } else {
    const id = 'p' + Date.now().toString(36);
    profiles = [...S.profiles, { id, ...data }];
    S.selectedProfile = id;
  }
  save({ profiles, selectedProfile: S.selectedProfile });
  renderProfiles();
});

$('pdDelete').onclick = () => {
  const profiles = S.profiles.filter(p => p.id !== editingId);
  const selectedProfile = S.selectedProfile === editingId ? profiles[0].id : S.selectedProfile;
  save({ profiles, selectedProfile });
  $('profileDialog').close();
  renderProfiles();
};

// ───────── Granie ─────────

function setPhase(phase) {
  window.voidCosmo?.setWalking(phase === 'loading');
  document.body.dataset.phase = phase;
  $('readyText').textContent = phase === 'loading' ? 'Uruchamianie…' : phase === 'running' ? 'Grasz teraz' : `Wszystko gotowe · ${mcLabel(profile())}`;
  $('idleBox').classList.toggle('hidden', phase !== 'idle');
  $('loadBox').classList.toggle('hidden', phase !== 'loading');
  $('runBox').classList.toggle('hidden', phase !== 'running');
  const p = profile();
  if (phase === 'running') {
    $('rpcLine1').textContent = `Minecraft ${mcLabel(p)} · ${p.name}`;
    $('rpcLine2').textContent = 'W menu gry' + (S.discordFps ? ' · 240 FPS' : '');
  } else {
    $('rpcLine1').textContent = 'W menu launchera';
    $('rpcLine2').textContent = `${p.name} · ${mcLabel(p)}`;
  }
}

$('playBtn').onclick = async () => {
  if (running) return;
  $('errBox').classList.add('hidden');
  $('crashBox').classList.add('hidden');
  if (!S.account) { toast('Najpierw zaloguj się kontem Microsoft (prawy górny róg)', 'err'); $('accountBtn').focus(); return; }
  running = true;
  try { const r = $('playBtn').getBoundingClientRect(); window.voidFx?.boom(r.left + r.width / 2, r.top + r.height / 2, 1); window.voidSfx?.play(); } catch {}
  setPhase('loading');
  $('barFill').style.width = '0%';
  $('loadText').textContent = 'Przygotowanie…';
  $('loadPct').textContent = '';
  try {
    await window.api.launch(profile().id);
  } catch (e) {
    running = false;
    setPhase('idle');
    $('errBox').textContent = e.message;
    $('errBox').classList.remove('hidden');
  }
};

window.api.onProgress(d => {
  $('loadText').textContent = d.text || '';
  if (d.total) {
    const pct = Math.max(0, Math.min(100, Math.round((d.cur / d.total) * 100)));
    $('barFill').style.width = pct + '%';
    $('loadPct').textContent = pct + '%';
  } else {
    $('loadPct').textContent = '';
  }
});

window.api.onState(d => {
  if (d.state === 'running') { gameSince = d.since || Date.now(); setPhase('running'); return; }
  if (d.state === 'closed') {
    running = false;
    gameSince = 0;
    setPhase('idle');
    if (d.crash) showCrash(d.crash, d.profileId);
    else if (d.code && d.code !== 0) {
      $('errBox').textContent = `Gra zamknęła się z błędem (kod ${d.code}). Jeśli to się powtarza, usuń ostatnio dodany mod.`;
      $('errBox').classList.remove('hidden');
    }
  }
});

function clock(ms) {
  const s = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(s / 3600);
  const mm = String(Math.floor((s % 3600) / 60)).padStart(2, '0');
  const ss = String(s % 60).padStart(2, '0');
  return h ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
}

setInterval(() => {
  if (document.hidden) return;
  const t = clock(Date.now() - (gameSince || startedAt));
  $('rpcTime').textContent = `${t} upłynęło`;
  if (gameSince) $('runTime').textContent = t;
}, 1000);

// ───────── Pomocnik przy crashu ─────────

let crashFile = null, crashProfile = null;
function showCrash(c, profileId) {
  crashFile = c.file || null;
  crashProfile = profileId || S.selectedProfile;
  $('crashTitle').textContent = c.title;
  $('crashText').textContent = c.text;
  $('crashFix').classList.toggle('hidden', !crashFile);
  $('crashBox').classList.remove('hidden');
  window.voidSfx?.error();
}
$('crashClose').onclick = () => $('crashBox').classList.add('hidden');
$('crashFix').onclick = async () => {
  if (!crashFile) return;
  try {
    const f = await window.api.disableMod(crashProfile, crashFile);
    toast(`Wyłączono ${f} – plik jest w mods/wyłączone`, 'ok');
    $('crashBox').classList.add('hidden');
    if (crashProfile !== S.selectedProfile) { await save({ selectedProfile: crashProfile }); renderProfiles(); }
    $('playBtn').click();
  } catch (e) { toast(e.message, 'err'); }
};

// ───────── Postać 3D i kosmetyki (three.js, ładowane po starcie) ─────────

let cosmoLib = null;
function loadScript(src) {
  return new Promise(resolve => {
    const el = document.createElement('script');
    el.src = src;
    el.onload = () => resolve(true);
    el.onerror = () => resolve(false);
    document.head.appendChild(el);
  });
}
function loadCosmo() {
  if (!cosmoLib) {
    cosmoLib = (async () => {
      window.__voidCosmoInit = {
        cosmetics: S.cosmetics, slots: S.emoteSlots,
        onChange: ({ cosmetics, slots }) => {
          clearTimeout(loadCosmo.t);
          loadCosmo.t = setTimeout(() => save({ cosmetics, emoteSlots: slots }), 300);
          $('wornSum').textContent = window.voidCosmo?.summary() || 'bez kosmetyków';
        }
      };
      if (!(await loadScript('three.min.js')) || !window.THREE) return false;
      try {
        const ok = await loadScript('cosm.js');
        return ok && !!window.voidCosmo;
      } catch (e) { console.warn('Kosmetyki', e); return false; }
    })();
  }
  return cosmoLib;
}

$('skinLogin').onclick = () => $('accountBtn').click();

$('skinChange').onclick = async () => {
  const btn = $('skinChange');
  btn.disabled = true;
  btn.textContent = 'Wysyłanie…';
  try {
    const data = await window.api.changeSkin($('skinModel').value === 'slim');
    if (data) {
      window.voidCosmo?.setSkin(data);
      toast('Skin zmieniony! W grze zobaczysz go po ponownym wejściu na serwer.', 'ok');
    }
  } catch (e) {
    toast(e.message);
  }
  btn.disabled = false;
  btn.textContent = 'Zmień skin';
};

async function renderSkin() {
  const logged = !!(S && S.account);
  $('skinName').textContent = logged ? S.account.name : 'Gracz';
  $('skinEmpty').classList.toggle('hidden', logged);
  $('skinActions').classList.toggle('hidden', !logged);
  const ok = await loadCosmo();
  if (!ok) { document.body.classList.add('no3d'); return; }
  $('wornSum').textContent = window.voidCosmo.summary() || 'bez kosmetyków';
  if (!logged) { window.voidCosmo.setSkin(null); return; }
  const data = await window.api.skin().catch(() => null);
  if (!S.account) return;
  if (data) $('skinModel').value = data.slim ? 'slim' : 'classic';
  window.voidCosmo.setSkin(data);
}

// ───────── Mody (Modrinth) ─────────

let modsLoaded = false;
let modType = 'mod';
let modOffset = 0;
let modTotal = 0;
let searchSeq = 0;
let installedIds = new Set();

function modsProfileId() { return $('modsProfile').value || S.selectedProfile; }

function renderModsProfiles() {
  const sel = $('modsProfile');
  const cur = sel.value || S.selectedProfile;
  sel.innerHTML = S.profiles.map(p => `<option value="${esc(p.id)}">${esc(p.name)} (${esc(mcLabel(p))})</option>`).join('');
  sel.value = S.profiles.some(p => p.id === cur) ? cur : S.selectedProfile;
}

$('modsProfile').onchange = () => runSearch();

$('typeSeg').addEventListener('click', e => {
  const b = e.target.closest('button');
  if (!b) return;
  modType = b.dataset.type;
  document.querySelectorAll('#typeSeg button').forEach(x => x.classList.toggle('on', x === b));
  runSearch();
});

let debounce;
$('searchInput').addEventListener('input', () => {
  clearTimeout(debounce);
  debounce = setTimeout(runSearch, 300);
});

$('moreBtn').onclick = () => runSearch(true);

async function refreshInstalled() {
  try {
    const list = await window.api.installed(modsProfileId());
    installedIds = new Set(list.map(x => x.id));
    return list;
  } catch {
    installedIds = new Set();
    return [];
  }
}

function modCard(m, mode) {
  const has = installedIds.has(m.id);
  const icon = m.icon ? `<img src="${esc(m.icon)}" alt="" loading="lazy" decoding="async">` : `<div class="ph" aria-hidden="true">${esc((m.title || '?')[0])}</div>`;
  const btn = mode === 'installed'
    ? `<button type="button" class="remove" data-remove="${esc(m.id)}">Usuń</button>`
    : `<button type="button" class="${has ? 'done' : ''}" data-install="${esc(m.id)}" data-title="${esc(m.title)}" ${has ? 'disabled' : ''}>${has ? 'Zainstalowany' : 'Zainstaluj'}</button>`;
  const meta = mode === 'installed' ? esc(m.file) : `${esc(m.author)} · ${fmt(m.downloads)} pobrań`;
  return `<div class="mod">${icon}<div class="info"><b>${esc(m.title)}</b>${m.description ? `<span>${esc(m.description)}</span>` : ''}<small>${meta}</small></div>${btn}</div>`;
}

const SKELETON = '<div class="mod skel" aria-hidden="true"><i></i><div class="info"><b></b><span></span><small></small></div></div>'.repeat(6);

async function runSearch(more = false) {
  const seq = ++searchSeq;
  const box = $('results');
  const p = S.profiles.find(x => x.id === modsProfileId()) || profile();
  if (!more) { modOffset = 0; box.innerHTML = SKELETON; }
  $('moreBtn').classList.add('hidden');

  if (modType === 'installed') {
    const list = await refreshInstalled();
    if (seq !== searchSeq) return;
    $('modsHint').textContent = `Zainstalowane w profilu „${p.name}”`;
    box.innerHTML = list.length
      ? list.map(m => modCard({ ...m, description: (m.local ? 'Własny plik · ' : 'Modrinth · ') + (m.type === 'mod' ? 'Mod' : m.type === 'shader' ? 'Shader' : 'Resource pack') }, 'installed')).join('')
      : '<div class="empty">Nic tu jeszcze nie ma.</div>';
    return;
  }

  $('modsHint').textContent = modType === 'mod' && p.loader === 'vanilla'
    ? `Profil „${p.name}” jest bez modów – wybierz profil z Fabric, żeby instalować mody.`
    : `Wyniki dla ${mcLabel(p)}${modType === 'mod' ? ' · ' + loaderLabel(p) : ''} · zależności instalują się same`;

  try {
    if (!more) await refreshInstalled();
    const res = await window.api.search({ query: $('searchInput').value, type: modType, profileId: p.id, offset: modOffset });
    if (seq !== searchSeq) return;
    modTotal = res.total;
    modOffset += res.hits.length;
    const html = res.hits.map(m => modCard(m)).join('');
    if (more) box.insertAdjacentHTML('beforeend', html);
    else box.innerHTML = html || '<div class="empty">Nic nie znaleziono.</div>';
    $('moreBtn').classList.toggle('hidden', modOffset >= modTotal);
  } catch (e) {
    if (seq !== searchSeq) return;
    box.innerHTML = `<div class="empty">Nie udało się połączyć z Modrinth.<br>${esc(e.message)}</div>`;
  }
}

$('results').addEventListener('click', async e => {
  const inst = e.target.closest('[data-install]');
  const rem = e.target.closest('[data-remove]');
  if (inst) {
    inst.disabled = true;
    inst.textContent = 'Instalowanie…';
    try {
      const names = await window.api.install({ projectId: inst.dataset.install, type: modType, profileId: modsProfileId(), title: inst.dataset.title });
      installedIds.add(inst.dataset.install);
      inst.textContent = 'Zainstalowany';
      inst.classList.add('done', 'just');
      try { const r = inst.getBoundingClientRect(); window.voidFx?.pop(r.left + r.width / 2, r.top + r.height / 2, 1.4); } catch {}
      toast(names.length > 1 ? `Zainstalowano: ${names.join(', ')}` : `Zainstalowano ${names[0]}`, 'ok');
    } catch (err) {
      inst.disabled = false;
      inst.textContent = 'Zainstaluj';
      toast(err.message, 'err');
    }
  } else if (rem) {
    rem.disabled = true;
    await window.api.remove({ projectId: rem.dataset.remove, profileId: modsProfileId() });
    rem.closest('.mod').remove();
    toast('Usunięto');
  }
});

// ───────── Przeciąganie plików (mody, shadery, resource packi) ─────────

const TYPE_PL = { mod: 'mod', shader: 'shader', resourcepack: 'resource pack' };
let dragDepth = 0;
const hasFiles = e => e.dataTransfer && [...e.dataTransfer.types].includes('Files');

function dropProfile() {
  // w zakładce Mody – profil wybrany na liście, w innych – aktywny profil
  const modsTab = $('tab-mods').classList.contains('active');
  return S.profiles.find(p => p.id === (modsTab ? modsProfileId() : S.selectedProfile)) || profile();
}

document.addEventListener('dragenter', e => {
  if (!hasFiles(e)) return;
  e.preventDefault();
  if (dragDepth++ === 0) {
    $('dropTarget').textContent = `do profilu „${dropProfile().name}”`;
    document.body.classList.add('dragging');
  }
});
document.addEventListener('dragover', e => {
  if (!hasFiles(e)) return;
  e.preventDefault();
  e.dataTransfer.dropEffect = 'copy';
});
document.addEventListener('dragleave', e => {
  if (!hasFiles(e)) return;
  if (--dragDepth <= 0) { dragDepth = 0; document.body.classList.remove('dragging'); }
});
document.addEventListener('drop', async e => {
  e.preventDefault(); // bez tego Electron otworzyłby plik zamiast launchera
  dragDepth = 0;
  document.body.classList.remove('dragging');
  const files = [...(e.dataTransfer ? e.dataTransfer.files : [])];
  const paths = files.map(f => window.api.pathFor(f)).filter(Boolean);
  if (!paths.length) return;
  const p = dropProfile();
  try {
    const res = await window.api.addLocal(p.id, paths);
    const ok = res.filter(r => r.ok);
    const bad = res.filter(r => !r.ok);
    let msg = '';
    if (ok.length === 1) msg = `Dodano ${TYPE_PL[ok[0].type]} „${ok[0].title}” do „${p.name}”`;
    else if (ok.length > 1) msg = `Dodano ${ok.length} pliki do „${p.name}”: ${ok.map(r => r.title).join(', ')}`;
    const warns = ok.filter(r => r.warn).map(r => `${r.title}: ${r.warn}`);
    if (warns.length) msg += ` (${warns.join('; ')})`;
    if (bad.length) msg += (msg ? ' · ' : '') + bad.map(r => r.error).join(' · ');
    toast(msg || 'Nic nie dodano', ok.length && !bad.length ? 'ok' : bad.length ? 'err' : '');
    if (ok.length) document.querySelectorAll('.dropzone').forEach(z => { z.classList.remove('done'); void z.offsetWidth; z.classList.add('done'); });
    if (modsLoaded && modType === 'installed') runSearch();
  } catch (err) {
    toast(err.message, 'err');
  }
});
$('dropzone').onclick = () => toast('Złap plik .jar lub .zip w Eksploratorze i upuść go na okno launchera');

// ───────── „Co nowego” po aktualizacji ─────────

const WHATS_NEW = [
  'Void Client 2.0: zupełnie nowy wygląd launchera – pasek z ikonami, animowane tło, nowe karty i przyciski',
  'Kosmetyki: 36 animowanych peleryn, 55 skrzydeł (16 w pełnym 3D, w tym 16 małych), czapki, zwierzaki, efekty i 64 emotki – zapisują się same',
  'Koło emotek: ustawiasz w Kosmetyki → Emotki (kliknij pole, potem emotkę albo przeciągnij)',
  'Twoja postać 3D z prawdziwym skinem i założonymi kosmetykami',
  'Pomocnik przy crashu: mówi, który mod wysypał grę, i wyłącza go jednym kliknięciem',
  'Opcje: tryb wydajności (Jakość / Zrównoważony / Maks. FPS), wyższy priorytet gry, płynność bez przycięć (ZGC)',
  'Replay: przełącznik w Opcjach instaluje ReplayMod',
  '12 nowych nieb w grze'
];

function showWhatsNew(version) {
  let last = null;
  try { last = localStorage.getItem('vc.lastVersion'); localStorage.setItem('vc.lastVersion', version); } catch { return; }
  if (!last || last === version) return; // pierwsza instalacja albo ta sama wersja
  $('wnVer').textContent = 'v' + version;
  $('wnList').innerHTML = WHATS_NEW.map(t => `<li>${esc(t)}</li>`).join('');
  $('whatsNew').hidden = false;
  $('wnClose').focus();
}
$('wnClose').onclick = () => { $('whatsNew').hidden = true; };
$('whatsNew').addEventListener('click', e => { if (e.target.id === 'whatsNew') $('whatsNew').hidden = true; });
document.addEventListener('keydown', e => { if (e.key === 'Escape' && !$('whatsNew').hidden) $('whatsNew').hidden = true; });

// ───────── Opcje ─────────

function renderSettings() {
  $('ram').value = S.ram;
  $('ramVal').textContent = S.ram + ' GB';
  ramFill();
  document.querySelectorAll('.pills').forEach(g => {
    g.querySelectorAll('button').forEach(b => b.classList.toggle('on', S[g.dataset.key] === b.dataset.val));
  });
  document.querySelectorAll('.switch').forEach(sw => {
    const on = !!S[sw.dataset.key];
    sw.classList.toggle('on', on);
    sw.setAttribute('aria-pressed', String(on));
  });
  document.body.classList.toggle('anim', !!S.animations);
  document.body.classList.toggle('sfx', S.sounds !== false);
  const dOn = !!S.discord;
  $('discordChip').classList.toggle('on', dOn);
  $('discordText').textContent = dOn ? 'Discord: aktywny' : 'Discord: wyłączony';
  document.querySelectorAll('.toggle-row.sub').forEach(r => r.classList.toggle('off', !dOn));
  if (running) setPhase('running');
}

function ramFill() {
  const r = $('ram'), min = Number(r.min) || 0, max = Number(r.max) || 100;
  r.style.setProperty('--pct', ((Number(r.value) - min) / (max - min) * 100) + '%');
}

$('ram').addEventListener('input', () => {
  $('ramVal').textContent = $('ram').value + ' GB';
  ramFill();
});
$('ram').addEventListener('change', () => {
  save({ ram: Number($('ram').value) });
  renderProfiles();
});

document.querySelectorAll('.pills').forEach(g => g.addEventListener('click', e => {
  const b = e.target.closest('button');
  if (!b) return;
  save({ [g.dataset.key]: b.dataset.val });
  renderSettings();
}));

document.querySelectorAll('.switch').forEach(sw => sw.addEventListener('click', () => {
  save({ [sw.dataset.key]: !S[sw.dataset.key] });
  renderSettings();
}));


$('importBtn').onclick = async () => {
  const btn = $('importBtn');
  btn.disabled = true;
  btn.textContent = 'Przenoszenie…';
  try {
    const r = await window.api.importStable();
    S = r.settings;
    renderAccount(); renderProfiles(); renderSettings(); renderSkin().catch(() => {});
    toast(`Przeniesiono ustawienia launchera i ${r.copied} z ${r.profiles} profili gry (opcje, Void Client, światy, serwery)`, 'ok');
  } catch (e) { toast(e.message, 'err'); }
  btn.disabled = false;
  btn.textContent = 'Przenieś ustawienia ze zwykłego launchera';
};

$('folderBtn').onclick = () => window.api.openFolder(profile().id);

// ───────── Aktualizacje ─────────

window.api.onUpdate(d => {
  const bar = $('updateBar');
  bar.classList.remove('hidden');
  if (d.state === 'ready') {
    $('updateText').textContent = `Nowa wersja ${d.version} jest gotowa`;
    $('updateBtn').classList.remove('hidden');
  } else {
    $('updateText').textContent = d.percent != null ? `Pobieranie aktualizacji… ${d.percent}%` : `Pobieranie aktualizacji ${d.version || ''}…`;
  }
});

$('updateBtn').onclick = async () => {
  if (running) { toast('Zamknij grę, żeby zainstalować aktualizację'); return; }
  $('updateBtn').disabled = true;
  $('updateText').textContent = 'Za chwilę otworzy się instalator – kliknij „Tak”, jeśli Windows zapyta';
  const res = await window.api.installUpdate().catch(() => false);
  if (res === 'manual') {
    $('updateBtn').disabled = false;
    $('updateText').textContent = 'Pobierz VoidClient-Setup.exe ze strony, która się otworzyła';
  } else if (!res) {
    $('updateBtn').disabled = false;
    toast('Nie udało się uruchomić instalatora – spróbuj ponownie', 'err');
  }
};

// ───────── Start ─────────

(async () => {
  S = await window.api.getSettings();
  window.api.beta?.().then(b => { if (b) { $('betaChip').classList.remove('hidden'); $('importBtn').classList.remove('hidden'); document.body.classList.add('is-beta'); } }).catch(() => {});
  window.api.version().then(v => {
  $('versionChip').textContent = 'v' + v;
  $('aboutVer').textContent = v;
  showWhatsNew(v);
}).catch(() => {});
  renderAccount();
  renderProfiles();
  renderSettings();
  setPhase('idle');
  requestAnimationFrame(() => goTab('play'));
  // Skin ładuje się po pokazaniu okna – nie spowalnia startu.
  const later = cb => (window.requestIdleCallback ? window.requestIdleCallback(cb, { timeout: 600 }) : setTimeout(cb, 50));
  later(() => renderSkin().catch(e => console.warn('Skin', e)));
})();

// ───────── Znajomi ─────────

let friendsData = { list: [], error: '' };

function friendRow(f) {
  const el = document.createElement('div');
  el.className = 'friend' + (f.playing ? ' playing' : f.online ? ' online' : '');
  const img = document.createElement('img');
  img.alt = '';
  img.loading = 'lazy';
  img.src = 'https://mc-heads.net/avatar/' + encodeURIComponent(String(f.uuid).replace(/-/g, '')) + '/36';
  const who = document.createElement('div');
  who.className = 'who';
  const b = document.createElement('b');
  b.textContent = f.name;
  const st = document.createElement('span');
  st.innerHTML = '<i class="dot"></i>';
  st.append(f.status === 'friend' ? (f.playing ? 'Gra w Minecrafta' : f.online ? 'Online w launcherze' : 'Offline')
    : f.status === 'sent' ? 'Zaproszenie wysłane' : 'Chce dodać Cię do znajomych');
  who.append(b, st);
  el.append(img, who);
  const btn = (label, cls, fn) => {
    const x = document.createElement('button');
    x.type = 'button';
    x.textContent = label;
    if (cls) x.className = cls;
    x.onclick = async () => {
      x.disabled = true;
      try { await fn(); } catch (e) { toast(e.message, 'err'); x.disabled = false; }
    };
    el.append(x);
  };
  if (f.status === 'request') {
    btn('Akceptuj', 'ok', async () => { await window.api.addFriend(f.name); toast('Dodano: ' + f.name, 'ok'); });
    btn('Odrzuć', 'no', () => window.api.removeFriend(f.uuid));
  } else {
    btn(f.status === 'sent' ? 'Anuluj' : 'Usuń', 'no', () => window.api.removeFriend(f.uuid));
  }
  return el;
}

function renderFriends(d) {
  if (d) friendsData = d;
  const list = friendsData.list || [];
  const box = $('friendsList');
  box.textContent = '';
  const logged = !!(S && S.account);
  $('friendAdd').disabled = !logged;
  const requests = list.filter(f => f.status === 'request');
  const badge = $('friendsBadge');
  badge.textContent = String(requests.length);
  badge.classList.toggle('hidden', requests.length === 0);
  const on = list.filter(f => f.status === 'friend' && f.online).length;
  renderHomeFriends(list, logged);
  $('friendsOnline').innerHTML = logged && list.length ? `<b>${on}</b> online` : '';
  if (!logged) {
    box.innerHTML = '<div class="empty">Zaloguj się kontem Microsoft (prawy górny róg), żeby mieć znajomych z Void Client.</div>';
    return;
  }
  if (friendsData.error) $('friendsHint').textContent = friendsData.error;
  const groups = [
    ['ZAPROSZENIA', requests],
    ['ZNAJOMI', list.filter(f => f.status === 'friend')],
    ['WYSŁANE', list.filter(f => f.status === 'sent')]
  ];
  let any = false;
  for (const [title, items] of groups) {
    if (!items.length) continue;
    any = true;
    const h = document.createElement('h3');
    h.textContent = title;
    box.append(h, ...items.map(friendRow));
  }
  if (!any) box.innerHTML = '<div class="empty">Nie masz jeszcze znajomych. Wpisz nick kogoś, kto gra na Void Client.</div>';
}

function renderHomeFriends(list, logged) {
  const box = $('homeFriends');
  const fr = list.filter(f => f.status === 'friend').sort((a, b) => (b.playing - a.playing) || (b.online - a.online)).slice(0, 5);
  const on = fr.filter(f => f.online).length;
  $('homeOnline').textContent = logged && list.length ? `${on} online` : '';
  box.textContent = '';
  if (!logged || !fr.length) {
    box.innerHTML = `<div class="none">${logged ? 'Dodaj znajomych w zakładce Znajomi' : 'Zaloguj się, żeby widzieć znajomych'}</div>`;
    return;
  }
  for (const f of fr) {
    const row = document.createElement('div');
    row.className = 'fi' + (f.playing ? ' playing' : f.online ? ' online' : '');
    const img = document.createElement('img');
    img.alt = ''; img.loading = 'lazy';
    img.src = 'https://mc-heads.net/avatar/' + encodeURIComponent(String(f.uuid).replace(/-/g, '')) + '/32';
    const who = document.createElement('div');
    const b = document.createElement('b'); b.textContent = f.name;
    const sm = document.createElement('small'); sm.textContent = f.playing ? 'gra w Minecrafta' : f.online ? 'w launcherze' : 'offline';
    who.append(b, sm);
    const dot = document.createElement('span'); dot.className = 'dot';
    row.append(img, who, dot);
    box.append(row);
  }
}

$('friendForm').addEventListener('submit', async e => {
  e.preventDefault();
  const name = $('friendName').value.trim();
  if (!/^[A-Za-z0-9_]{3,16}$/.test(name)) { toast('Wpisz poprawny nick (3–16 znaków)', 'err'); return; }
  $('friendAdd').disabled = true;
  try {
    const n = await window.api.addFriend(name);
    $('friendName').value = '';
    toast('Wysłano zaproszenie do ' + n, 'ok');
  } catch (err) {
    toast(err.message, 'err');
  } finally {
    $('friendAdd').disabled = false;
  }
});

window.api.onFriends(renderFriends);
window.api.friends().then(renderFriends).catch(() => {});
document.querySelector('.nav[data-tab="friends"]').addEventListener('click', () => {
  window.api.refreshFriends().then(renderFriends).catch(() => {});
});

// ───────── Dopieszczenie: światło za kursorem, przechylenie kart, fala po kliknięciu ─────────

document.addEventListener('pointermove', e => {
  if (!document.body.classList.contains('anim')) return;
  const el = e.target.closest('.profile, .nw, .opt, .it, .perf, .mod, .friend, .fr');
  if (!el) return;
  const r = el.getBoundingClientRect();
  const x = (e.clientX - r.left) / r.width, y = (e.clientY - r.top) / r.height;
  el.style.setProperty('--mx', x * 100 + '%');
  el.style.setProperty('--my', y * 100 + '%');
  if (el.classList.contains('it')) { el.style.setProperty('--ry', ((x - .5) * 14) + 'deg'); el.style.setProperty('--rx', ((.5 - y) * 14) + 'deg'); }
}, { passive: true });

document.addEventListener('pointerdown', e => {
  if (!document.body.classList.contains('anim')) return;
  const b = e.target.closest('.play, .ghost, .vbtn, .ct, .seg button, .btn-primary, .primary, .profile, .mod button');
  if (!b || b.disabled) return;
  const r = b.getBoundingClientRect(), s = Math.max(r.width, r.height);
  const d = document.createElement('span');
  d.className = 'rip';
  d.style.width = d.style.height = s + 'px';
  d.style.left = (e.clientX - r.left - s / 2) + 'px';
  d.style.top = (e.clientY - r.top - s / 2) + 'px';
  b.appendChild(d);
  setTimeout(() => d.remove(), 650);
});
