// Status na Discordzie (Rich Presence). Startuje w tle, nigdy nie blokuje launchera.
let client = null;
let ready = false;
let pending = null;
let currentId = '';
let lastSent = 0;
let flushTimer = null;
const started = Date.now();

const SITE = 'https://1nonly-lgtm.github.io/void-client/';

async function start(appId) {
  if (!appId || (client && currentId === appId)) return;
  stop();
  currentId = appId;
  try {
    const { Client } = require('@xhayper/discord-rpc');
    client = new Client({ clientId: appId });
    client.on('ready', () => { ready = true; flush(); });
    client.on('disconnected', () => { ready = false; client = null; currentId = ''; });
    await client.login();
  } catch (e) {
    console.warn('Discord RPC niedostępny:', e.message); // Discord wyłączony – spróbujemy przy następnej zmianie
    client = null;
    currentId = '';
    ready = false;
  }
}

// Discord przyjmuje najwyżej ~5 zmian na 20 s – wysyłamy co najmniej co 4 s.
function flush() {
  if (!ready || !client || !client.user || !pending) return;
  const wait = 4000 - (Date.now() - lastSent);
  if (wait > 0) {
    if (!flushTimer) flushTimer = setTimeout(() => { flushTimer = null; flush(); }, wait);
    return;
  }
  lastSent = Date.now();
  client.user.setActivity(pending).catch(() => {});
}

const cut = (s, n = 120) => (s.length > n ? s.slice(0, n - 1) + '…' : s);

/**
 * @param details pierwsza linia (np. „Minecraft 1.21.11 · Void Client”)
 * @param state druga linia (np. „Na serwerze: mc.hypixel.net · 240 FPS”)
 * @param since kiedy zaczęła się gra (ms) – Discord pokazuje z tego licznik czasu
 */
function set({ details, state, since }) {
  pending = {
    type: 0, // „Gra w …”
    details: cut(details || 'Void Client'),
    state: cut(state || 'W menu launchera'),
    startTimestamp: Math.floor(Number(since) || started),
    largeImageKey: 'logo',
    largeImageText: 'Void Client – by: 1nonly $',
    buttons: [{ label: 'Pobierz Void Client', url: SITE }],
    instance: false
  };
  flush();
}

function stop() {
  ready = false;
  clearTimeout(flushTimer);
  flushTimer = null;
  if (client) {
    try { client.destroy(); } catch { /* ignore */ }
  }
  client = null;
  currentId = '';
}

module.exports = { start, set, stop };
