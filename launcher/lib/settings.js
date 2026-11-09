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
  discord: true,
  discordShowServer: false,
  account: null // { name, uuid, refresh }
};

let data = null;
let timer = null;

function load() {
  try {
    data = { ...structuredClone(DEFAULTS), ...JSON.parse(fs.readFileSync(FILE, 'utf8')) };
  } catch {
    data = structuredClone(DEFAULTS);
  }
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

module.exports = { get, publicView, update, setAccount, saveNow };
