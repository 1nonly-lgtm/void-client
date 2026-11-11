// Void Client – proces główny. Ciężkie moduły ładowane dopiero, gdy są potrzebne (szybki start).
const { app, BrowserWindow, ipcMain, shell, Tray, Menu, nativeImage, dialog, Notification } = require('electron');
const path = require('path');

if (!app.requestSingleInstanceLock()) app.exit(0);

app.commandLine.appendSwitch('disable-features', 'HardwareMediaKeyHandling,MediaSessionService,SpareRendererForSitePerProcess');
app.setAppUserModelId('dev.oneonly.voidclient');

const settings = require('./lib/settings');

// Aplikacja „Void Client” na Discordzie – nazwa i logo w statusie graczy.
const DISCORD_APP_ID = '1569289556715634694';
let win = null;
let tray = null;
let running = null; // { proc, profile, mc, since }

function lazy(name) {
  return require('./lib/' + name);
}

function createWindow() {
  win = new BrowserWindow({
    width: 1180,
    height: 720,
    minWidth: 980,
    minHeight: 620,
    frame: false,
    show: false,
    backgroundColor: '#07040f',
    title: 'Void Client',
    icon: path.join(__dirname, 'build', 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      spellcheck: false,
      backgroundThrottling: true
    }
  });
  win.once('ready-to-show', () => win.show());
  win.loadFile(path.join(__dirname, 'src', 'index.html'));
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https:\/\//.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });
}

function send(channel, data) {
  if (win && !win.isDestroyed()) win.webContents.send(channel, data);
}

function showWindow() {
  if (!win) return;
  if (win.isMinimized()) win.restore();
  win.show();
  win.focus();
  if (tray) { tray.destroy(); tray = null; }
}

function hideToTray() {
  if (!win) return;
  win.hide();
  if (tray) return;
  tray = new Tray(nativeImage.createFromPath(path.join(__dirname, 'build', 'icon.png')).resize({ width: 16, height: 16 }));
  tray.setToolTip('Void Client – gra uruchomiona');
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: 'Pokaż Void Client', click: showWindow },
    { type: 'separator' },
    { label: 'Zamknij launcher', click: () => app.quit() }
  ]));
  tray.on('click', showWindow);
}

// ───────── Discord ─────────

let discordTimer = null;

/** Buduje status z tego, co wiadomo: profil, wersja, serwer i FPS (te dwa wysyła mod Void Client). */
function discordUpdate() {
  clearTimeout(discordTimer);
  discordTimer = setTimeout(discordApply, 300); // kilka zmian naraz = jedna aktualizacja
}

function discordApply() {
  const s = settings.get();
  const discord = lazy('discord');
  if (!s.discord) { discord.stop(); return; }
  discord.start(DISCORD_APP_ID);
  if (!running) {
    const profile = s.profiles.find(p => p.id === s.selectedProfile) || s.profiles[0];
    discord.set({
      details: 'W menu launchera',
      state: `${profile.name} · ${profile.mc === 'latest' ? 'najnowsza' : profile.mc}`
    });
    return;
  }
  const st = running.status || {};
  let where;
  if (st.srv === 'sp') where = 'Świat jednoosobowy';
  else if (st.srv && st.srv !== 'mp') where = s.discordServer ? 'Serwer: ' + st.srv : 'Gra na serwerze';
  else if (st.srv === 'mp') where = 'Gra na serwerze';
  else where = 'W menu gry';
  const parts = [where];
  if (s.discordFps && st.fps > 0) parts.push(st.fps + ' FPS');
  discord.set({
    details: `Minecraft ${running.mc} · ${running.profile.name}`,
    state: parts.join(' · '),
    since: running.since
  });
}

// ───────── IPC ─────────

ipcMain.handle('settings:get', () => settings.publicView());
ipcMain.handle('settings:set', (_, patch) => {
  const out = settings.update(patch);
  if (patch && ['discord', 'selectedProfile', 'discordServer', 'discordFps', 'profiles'].some(k => k in patch)) discordUpdate();
  return out;
});

ipcMain.on('win:min', () => win && win.minimize());
ipcMain.on('win:close', () => app.quit());

ipcMain.handle('versions:list', () => lazy('game').listVersions());

ipcMain.handle('auth:login', async () => { const a = await lazy('auth').login(); lazy('friends').reset(); setTimeout(friendsTick, 500); return a; });
ipcMain.handle('auth:logout', () => { lazy('auth').logout(); lazy('friends').reset(); setTimeout(friendsTick, 100); });
ipcMain.handle('skin:get', () => {
  const acc = settings.get().account;
  return acc ? lazy('skin').getSkin(acc.uuid) : null;
});
ipcMain.handle('skin:change', async (_, { slim }) => {
  const acc = settings.get().account;
  if (!acc) throw new Error('Najpierw zaloguj się kontem Microsoft');
  const pick = await dialog.showOpenDialog(win, {
    title: 'Wybierz skin (PNG 64×64)',
    filters: [{ name: 'Skin PNG', extensions: ['png'] }],
    properties: ['openFile']
  });
  if (pick.canceled || !pick.filePaths[0]) return null;
  const token = await lazy('auth').getAccessToken();
  return lazy('skin').uploadSkin(acc.uuid, pick.filePaths[0], slim, token);
});
ipcMain.handle('void:supported', (_, mc) => !!lazy('game').voidBuildFor(mc));

ipcMain.handle('profile:folder', (_, id) => {
  const dir = lazy('game').instanceDir(id);
  require('fs').mkdirSync(dir, { recursive: true });
  return shell.openPath(dir);
});

ipcMain.handle('open:url', (_, url) => {
  if (/^https:\/\//.test(url)) shell.openExternal(url);
});

function profileById(id) {
  const s = settings.get();
  const p = s.profiles.find(x => x.id === id);
  if (!p) throw new Error('Nie znaleziono profilu');
  return p;
}

async function profileMc(p) {
  return p.mc === 'latest' ? (await lazy('game').listVersions()).latest : p.mc;
}

ipcMain.handle('modrinth:search', async (_, { query, type, profileId, offset }) => {
  const p = profileById(profileId);
  return lazy('modrinth').search({ query, type, mc: await profileMc(p), loader: p.loader, offset });
});

ipcMain.handle('modrinth:install', async (_, { projectId, type, profileId, title }) => {
  const p = profileById(profileId);
  return lazy('modrinth').install({
    projectId, type, title, mc: await profileMc(p), loader: p.loader, instanceDir: lazy('game').instanceDir(p.id)
  });
});

ipcMain.handle('modrinth:installed', (_, profileId) => lazy('modrinth').listInstalled(lazy('game').instanceDir(profileId)));
ipcMain.handle('local:add', (_, { profileId, paths }) => {
  const p = profileById(profileId);
  const list = (Array.isArray(paths) ? paths : []).filter(x => typeof x === 'string' && x).slice(0, 50);
  return lazy('local').add(list, p, lazy('game').instanceDir(p.id), lazy('modrinth'));
});
ipcMain.handle('modrinth:remove', (_, { projectId, profileId }) => lazy('modrinth').remove(projectId, lazy('game').instanceDir(profileId)));

ipcMain.handle('game:launch', async (_, profileId) => {
  if (running) throw new Error('Gra już działa');
  const s = settings.get();
  const profile = profileById(profileId);
  const emit = d => send('game:progress', d);
  const { proc, mc } = await lazy('game').launch(profile, s, emit, line => {
    if (!running) return;
    // Mod Void Client co 5 s wypisuje: [VoidStatus] srv=<adres|sp|mp|> fps=<liczba>
    let m = /\[VoidStatus\] srv=(\S*) fps=(\d+)/.exec(line);
    if (m) {
      const prev = running.status || {};
      running.status = { srv: m[1], fps: Number(m[2]) };
      running.modStatus = true;
      if (prev.srv !== m[1] || settings.get().discordFps) discordUpdate();
      return;
    }
    if (running.modStatus) return;
    // Bez moda (np. vanilla) – serwer z logu gry
    m = /Connecting to ([^,\s]+),?\s*(\d+)?/.exec(line);
    if (m) { running.status = { srv: m[1] }; discordUpdate(); return; }
    if (/Starting integrated minecraft server/i.test(line)) { running.status = { srv: 'sp' }; discordUpdate(); return; }
    if (/Stopping server|Disconnected from|lost connection/i.test(line)) { running.status = {}; discordUpdate(); }
  });

  // Wyższy priorytet procesu gry w Windows = mniej przycięć, gdy w tle coś działa.
  try {
    const os = require('os');
    os.setPriority(proc.pid, os.constants.priority.PRIORITY_ABOVE_NORMAL);
  } catch { /* brak uprawnień – gra działa normalnie */ }

  running = { proc, profile, mc, since: Date.now(), status: {} };
  send('game:state', { state: 'running', since: running.since });
  discordUpdate();
  setTimeout(friendsTick, 1000);

  if (s.afterLaunch === 'hide') hideToTray();
  else if (s.afterLaunch === 'close') setTimeout(() => app.quit(), 1500);

  proc.on('close', code => {
    running = null;
    send('game:state', { state: 'closed', code });
    discordUpdate();
    setTimeout(friendsTick, 1000);
    if (settings.get().afterLaunch === 'hide') showWindow();
  });
  return { ok: true };
});

// ───────── Aktualizacje (z wydań na GitHubie) ─────────

let updateReady = false;
let updateFile = null; // pobrany instalator nowej wersji

function setupUpdates() {
  if (!app.isPackaged) return;
  let autoUpdater;
  try {
    ({ autoUpdater } = require('electron-updater'));
  } catch {
    return;
  }
  autoUpdater.autoDownload = true;
  // Bez cichej instalacji przy zamykaniu – na części komputerów Windows prosi wtedy o zgodę
  // i cicha instalacja po cichu się wysypuje. Instalator uruchamiamy sami (niżej).
  autoUpdater.autoInstallOnAppQuit = false;
  autoUpdater.on('update-available', info => send('update:state', { state: 'downloading', version: info.version }));
  autoUpdater.on('download-progress', p => send('update:state', { state: 'downloading', percent: Math.round(p.percent) }));
  autoUpdater.on('update-downloaded', info => {
    updateReady = true;
    updateFile = info.downloadedFile || null;
    send('update:state', { state: 'ready', version: info.version });
  });
  autoUpdater.on('error', e => console.warn('Aktualizacja niedostępna:', e && e.message));
  const check = () => autoUpdater.checkForUpdates().catch(() => {});
  check();
  setInterval(check, 60 * 60 * 1000); // co godzinę, jeśli launcher jest długo otwarty
}

const RELEASES = 'https://github.com/1nonly-lgtm/void-client/releases/latest';

/**
 * Uruchamia zwykły instalator (z paskiem postępu) i zamyka launcher.
 * „start” przez cmd = Windows sam obsłuży pytanie o zgodę (UAC), a 2 s opóźnienia
 * dają launcherowi czas na zamknięcie, zanim instalator zacznie podmieniać pliki.
 * Po instalacji instalator sam włącza nową wersję.
 */
ipcMain.handle('update:install', () => {
  if (!updateReady || running) return false;
  const fs = require('fs');
  if (!updateFile || !fs.existsSync(updateFile)) {
    shell.openExternal(RELEASES);
    return 'manual';
  }
  try {
    const { spawn } = require('child_process');
    // ping = ok. 2 s czekania (timeout nie działa bez konsoli); /s + verbatim = poprawne cudzysłowy przy spacjach w ścieżce
    const cmd = `"ping -n 3 127.0.0.1 >nul & start "" "${updateFile}""`;
    const child = spawn('cmd.exe', ['/d', '/s', '/c', cmd], {
      detached: true,
      stdio: 'ignore',
      windowsHide: true,
      windowsVerbatimArguments: true
    });
    child.unref();
  } catch (e) {
    console.warn('Nie udało się uruchomić instalatora', e);
    shell.openExternal(RELEASES);
    return 'manual';
  }
  try { lazy('discord').stop(); } catch { /* ignore */ }
  setTimeout(() => app.exit(0), 300);
  return true;
});

ipcMain.handle('app:version', () => app.getVersion());

// ───────── Start ─────────

app.on('second-instance', showWindow);
// ───────── Znajomi z Void Client ─────────
// Co minutę: „jestem online” (i czy gram), potem lista znajomych. Gdy znajomy odpali grę – powiadomienie.

let friends = [];
let friendsError = '';
const playingBefore = new Map();
let friendsTimer = null;

async function friendsTick() {
  clearTimeout(friendsTimer);
  friendsTimer = setTimeout(friendsTick, 60 * 1000);
  if (!settings.get().account) { friends = []; friendsError = ''; send('friends:update', { list: [], error: '' }); return; }
  try {
    const f = lazy('friends');
    await f.heartbeat(!!running);
    friends = await f.list();
    friendsError = '';
    for (const fr of friends) {
      const was = playingBefore.get(fr.uuid);
      if (fr.playing && was === false && Notification.isSupported()) {
        new Notification({ title: 'Void Client', body: fr.name + ' właśnie odpalił Minecrafta', icon: path.join(__dirname, 'build', 'icon.png'), silent: false }).show();
      }
      playingBefore.set(fr.uuid, !!fr.playing);
    }
  } catch (e) {
    friendsError = e.message || String(e);
  }
  send('friends:update', { list: friends, error: friendsError });
}

ipcMain.handle('friends:list', () => ({ list: friends, error: friendsError }));
ipcMain.handle('friends:refresh', async () => { await friendsTick(); return { list: friends, error: friendsError }; });
ipcMain.handle('friends:add', async (_, name) => { const n = await lazy('friends').add(name); friendsTick(); return n; });
ipcMain.handle('friends:remove', async (_, uuid) => { await lazy('friends').remove(uuid); friendsTick(); });

app.whenReady().then(() => {
  createWindow();
  // Discord startuje po pokazaniu okna, żeby nie spowalniać uruchamiania.
  setTimeout(discordUpdate, 1500);
  // Sprawdzanie aktualizacji w tle, gdy okno już działa.
  setTimeout(setupUpdates, 4000);
  setTimeout(friendsTick, 2500);
});
app.on('window-all-closed', () => {
  if (!running) app.quit();
});
app.on('before-quit', () => {
  try { lazy('discord').stop(); } catch { /* ignore */ }
});
