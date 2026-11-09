// Status na Discordzie (Rich Presence). Startuje w tle, nigdy nie blokuje launchera.
let client = null;
let ready = false;
let pending = null;
let currentId = '';
const started = new Date();

async function start(appId) {
  if (!appId || (client && currentId === appId)) return;
  stop();
  currentId = appId;
  try {
    const { Client } = require('@xhayper/discord-rpc');
    client = new Client({ clientId: appId });
    client.on('ready', () => { ready = true; flush(); });
    client.on('disconnected', () => { ready = false; });
    await client.login();
  } catch (e) {
    console.warn('Discord RPC niedostępny:', e.message); // Discord wyłączony – ignorujemy
    client = null;
    ready = false;
  }
}

function flush() {
  if (!ready || !client || !client.user || !pending) return;
  client.user.setActivity(pending).catch(() => {});
}

function set({ profile, mc, state, since }) {
  pending = {
    details: `${profile} · ${mc}`,
    state,
    startTimestamp: since || started,
    largeImageKey: 'logo',
    largeImageText: 'Void Client – by: 1nonly $',
    instance: false
  };
  flush();
}

function stop() {
  ready = false;
  if (client) {
    try { client.destroy(); } catch { /* ignore */ }
  }
  client = null;
  currentId = '';
}

module.exports = { start, set, stop };
