const { contextBridge, ipcRenderer } = require('electron');

const call = async (channel, payload) => {
  try {
    return await ipcRenderer.invoke(channel, payload);
  } catch (e) {
    throw new Error(String(e.message || e).replace(/^Error invoking remote method '[^']+': (Error: )?/, ''));
  }
};

contextBridge.exposeInMainWorld('api', {
  getSettings: () => call('settings:get'),
  setSettings: patch => call('settings:set', patch),
  minimize: () => ipcRenderer.send('win:min'),
  close: () => ipcRenderer.send('win:close'),
  versions: () => call('versions:list'),
  login: () => call('auth:login'),
  logout: () => call('auth:logout'),
  skin: () => call('skin:get'),
  changeSkin: slim => call('skin:change', { slim }),
  voidSupported: mc => call('void:supported', mc),
  launch: profileId => call('game:launch', profileId),
  search: opts => call('modrinth:search', opts),
  install: opts => call('modrinth:install', opts),
  installed: profileId => call('modrinth:installed', profileId),
  remove: opts => call('modrinth:remove', opts),
  openFolder: profileId => call('profile:folder', profileId),
  openUrl: url => call('open:url', url),
  onProgress: cb => ipcRenderer.on('game:progress', (_, d) => cb(d)),
  onState: cb => ipcRenderer.on('game:state', (_, d) => cb(d)),
  onUpdate: cb => ipcRenderer.on('update:state', (_, d) => cb(d)),
  installUpdate: () => call('update:install'),
  version: () => call('app:version')
});
