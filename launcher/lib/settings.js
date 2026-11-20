// Ustawienia launchera – zapisywane automatycznie do settings.json.
const { app } = require('electron');
const fs = require('fs');
const path = require('path');

const FILE = path.join(app.getPath('userData'), 'settings.json');

const DEFAULT_PROFILES = [
  { id: 'void', name: 'Void Client', mc: '1.21.11', loader: 'fabric', voidMod: true, optimize: true },
  { id: 'vanilla', name: 'Vanilla', mc: 'latest', loader: 'vanilla', voidMod: false, optimize: false }
];

const DEFAULTS = {
  selectedProfile: 'void',
  profiles: DEFAULT_PROFILES,
  ram: 4,
  afterLaunch: 'hide', // hide | close | stay
  windowMode: 'full', // full | window
  jvmFlags: true,
  animations: true,
  sounds: true, // dźwięki kliknięć w launcherze
  discord: true,
  discordServer: true, // adres serwera w statusie
  discordFps: true, // FPS w statusie (wysyła mod Void Client)
  // 2.0
  perfPreset: 'balanced', // quality | balanced | max
  highPriority: true, // wyższy priorytet procesu gry
  smoothGC: true, // ZGC na Javie 21+ (płynność bez przycięć)
  hud60: true, // HUD rysowany w 60 kl./s (mod Void)
  spotify: true, // moduł Spotify w grze
  replay: false, // ReplayMod z Modrinth
  cosmetics: { cape: null, wings: null, hat: null, pet: null, fx: null }, // zapis kosmetyków
  emoteSlots: [], // 24 sloty koła emotek (3 strony × 8)
  account: null // { name, uuid, refresh }
};

let data = null;
let timer = null;

/** Wersja testowa przy pierwszym starcie bierze ustawienia (i konto) ze zwykłego launchera. */
function importFromStable() {
  if (require('../package.json').voidBeta !== true || fs.existsSync(FILE)) return;
  try {
    const old = path.join(app.getPath('appData'), 'Void Client', 'settings.json');
    if (!fs.existsSync(old)) return;
    fs.mkdirSync(path.dirname(FILE), { recursive: true });
    fs.copyFileSync(old, FILE);
    const skins = path.join(app.getPath('appData'), 'Void Client', 'skins');
    if (fs.existsSync(skins)) fs.cpSync(skins, path.join(app.getPath('userData'), 'skins'), { recursive: true });
  } catch (e) { console.warn('Import ustawień:', e.message); }
}

function load() {
  importFromStable();
  try {
    data = { ...structuredClone(DEFAULTS), ...JSON.parse(fs.readFileSync(FILE, 'utf8')) };
  } catch {
    data = structuredClone(DEFAULTS);
  }
  delete data.discordShowServer; // stara nazwa opcji
  if (!Array.isArray(data.profiles) || data.profiles.length === 0) data.profiles = structuredClone(DEFAULT_PROFILES);
  return data;
}

function get() {
  return data || load();
}

/** Wersja bez tokenów – tylko to trafia do okna. */
function publicView() {
  const d = get();
  return { ...d, account: d.account ? { name: d.account.name, uuid: d.account.uuid } : null };
}

function update(patch) {
  const d = get();
  for (const [k, v] of Object.entries(patch || {})) {
    if (k === 'account') continue; // konto zmienia tylko moduł logowania
    if (k in DEFAULTS) d[k] = v;
  }
  scheduleSave();
  return publicView();
}

function setAccount(account) {
  get().account = account;
  saveNow();
}

function scheduleSave() {
  clearTimeout(timer);
  timer = setTimeout(saveNow, 250);
}

function saveNow() {
  clearTimeout(timer);
  if (!data) return;
  try {
    fs.mkdirSync(path.dirname(FILE), { recursive: true });
    const tmp = FILE + '.tmp';
    fs.writeFileSync(tmp, JSON.stringify(data, null, 2));
    fs.renameSync(tmp, FILE);
  } catch (e) {
    console.error('Nie udało się zapisać ustawień', e);
  }
}

app.on('before-quit', saveNow);

/** Ręczny import ze zwykłego launchera (Opcje → „Przenieś ustawienia”). Nowe opcje 2.0 i kosmetyki zostają. */
function importStableNow() {
  const old = path.join(app.getPath('appData'), 'Void Client', 'settings.json');
  if (!fs.existsSync(old)) throw new Error('Nie znaleziono ustawień zwykłego launchera Void Client na tym komputerze');
  const o = JSON.parse(fs.readFileSync(old, 'utf8'));
  const d = get();
  const keep = { cosmetics: d.cosmetics, emoteSlots: d.emoteSlots, perfPreset: d.perfPreset, highPriority: d.highPriority, smoothGC: d.smoothGC, hud60: d.hud60, spotify: d.spotify, replay: d.replay };
  for (const [k, v] of Object.entries(o)) if (k in DEFAULTS) d[k] = v;
  Object.assign(d, keep);
  if (!Array.isArray(d.profiles) || !d.profiles.length) d.profiles = structuredClone(DEFAULT_PROFILES);
  saveNow();
  return d.profiles.map(p => p.id);
}

module.exports = { get, publicView, update, setAccount, saveNow, importStableNow };
