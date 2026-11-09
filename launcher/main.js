// Void Client – proces główny. Ciężkie moduły ładowane dopiero, gdy są potrzebne (szybki start).
const { app, BrowserWindow, ipcMain, shell, Tray, Menu, nativeImage, dialog } = require('electron');
const path = require('path');

if (!app.requestSingleInstanceLock()) app.exit(0);

app.commandLine.appendSwitch('disable-features', 'HardwareMediaKeyHandling,MediaSessionService,SpareRendererForSitePerProcess');
app.setAppUserModelId('dev.oneonly.voidclient');

const settings = require('./lib/settings');
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

function discordUpdate(state) {
  const s = settings.get();
  const discord = lazy('discord');
  if (!s.discord || !s.discordAppId) { discord.stop(); return; }
  discord.start(s.discordAppId.trim());
  const profile = s.profiles.find(p => p.id === s.selectedProfile) || s.profiles[0];
  discord.set({
    profile: running ? running.profile.name : profile.name,
    mc: running ? running.mc : (profile.mc === 'latest' ? 'najnowsza' : profile.mc),
    state,
    since: running ? running.since : undefined
  });
}

// ───────── IPC ─────────

ipcMain.handle('settings:get', () => settings.publicView());
ipcMain.handle('settings:set', (_, patch) => {
  const out = settings.update(patch);
  if (patch && ('discord' in patch || 'discordAppId' in patch || 'selectedProfile' in patch)) {
    discordUpdate(running ? 'W grze' : 'W menu launchera');
  }
  return out;
});

ipcMain.on('win:min', () => win && win.minimize());
ipcMain.on('win:close', () => app.quit());

ipcMain.handle('versions:list', () => lazy('game').listVersions());

ipcMain.handle('auth:login', () => lazy('auth').login());
ipcMain.handle('auth:logout', () => lazy('auth').logout());
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
ipcMain.handle('modrinth:remove', (_, { projectId, profileId }) => lazy('modrinth').remove(projectId, lazy('game').instanceDir(profileId)));

ipcMain.handle('game:launch', async (_, profileId) => {
  if (running) throw new Error('Gra już działa');
  const s = settings.get();
  const profile = profileById(profileId);
  const emit = d => send('game:progress', d);
  let server = null;

  const { proc, mc } = await lazy('game').launch(profile, s, emit, line => {
    const m = /Connecting to ([^,\s]+),?\s*(\d+)?/.exec(line);
    if (m) {
      server = m[1];
      if (s.discordShowServer) discordUpdate('Na serwerze: ' + server);
    }
    if (/Stopping!|Disconnecting from server|Left the game/i.test(line) && server) {
      server = null;
      discordUpdate('W grze');
    }
  });

  // Wyższy priorytet procesu gry w Windows = mniej przycięć, gdy w tle coś działa.
  try {
    const os = require('os');
    os.setPriority(proc.pid, os.constants.priority.PRIORITY_ABOVE_NORMAL);
  } catch { /* brak uprawnień – gra działa normalnie */ }

  running = { proc, profile, mc, since: new Date() };
  send('game:state', { state: 'running' });
  discordUpdate('W grze');

  if (s.afterLaunch === 'hide') hideToTray();
  else if (s.afterLaunch === 'close') setTimeout(() => app.quit(), 1500);

  proc.on('close', code => {
    running = null;
    send('game:state', { state: 'closed', code });
    discordUpdate('W menu launchera');
    if (settings.get().afterLaunch === 'hide') showWindow();
  });
  return { ok: true };
});

// ───────── Aktualizacje (z wydań na GitHubie) ─────────

let updateReady = false;

function setupUpdates() {
  if (!app.isPackaged) return;
  let autoUpdater;
  try {
    ({ autoUpdater } = require('electron-updater'));
  } catch {
    return;
  }
  autoUpdater.autoDownload = true;
  autoUpdater.autoInstallOnAppQuit = true;
  autoUpdater.on('update-available', info => send('update:state', { state: 'downloading', version: info.version }));
  autoUpdater.on('download-progress', p => send('update:state', { state: 'downloading', percent: Math.round(p.percent) }));
  autoUpdater.on('update-downloaded', info => {
    updateReady = true;
    send('update:state', { state: 'ready', version: info.version });
  });
  autoUpdater.on('error', e => console.warn('Aktualizacja niedostępna:', e && e.message));
  const check = () => autoUpdater.checkForUpdates().catch(() => {});
  check();
  setInterval(check, 60 * 60 * 1000); // co godzinę, jeśli launcher jest długo otwarty
}

ipcMain.handle('update:install', () => {
  if (!updateReady || running) return false;
  const { autoUpdater } = require('electron-updater');
  autoUpdater.quitAndInstall(true, true); // cicha instalacja i ponowne uruchomienie
  return true;
});

ipcMain.handle('app:version', () => app.getVersion());

// ───────── Start ─────────

app.on('second-instance', showWindow);
app.whenReady().then(() => {
  createWindow();
  // Discord startuje po pokazaniu okna, żeby nie spowalniać uruchamiania.
  setTimeout(() => discordUpdate('W menu launchera'), 1500);
  // Sprawdzanie aktualizacji w tle, gdy okno już działa.
  setTimeout(setupUpdates, 4000);
});
app.on('window-all-closed', () => {
  if (!running) app.quit();
});
app.on('before-quit', () => {
  try { lazy('discord').stop(); } catch { /* ignore */ }
});
