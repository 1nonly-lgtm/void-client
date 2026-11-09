// Skin i peleryna gracza z oficjalnego profilu Mojang. Zapisywane lokalnie, żeby pokazywały się od razu.
const { app } = require('electron');
const fs = require('fs');
const path = require('path');
const { getJson, UA } = require('./net');

const DIR = path.join(app.getPath('userData'), 'skins');
const MAX_AGE = 30 * 60 * 1000; // odświeżanie co 30 minut

async function toDataUrl(url) {
  const res = await fetch(url.replace(/^http:/, 'https:'), { headers: { 'User-Agent': UA } });
  if (!res.ok) throw new Error('Nie udało się pobrać tekstury');
  return 'data:image/png;base64,' + Buffer.from(await res.arrayBuffer()).toString('base64');
}

function readCache(file) {
  try { return JSON.parse(fs.readFileSync(file, 'utf8')); } catch { return null; }
}

async function fetchFresh(uuid, file) {
  const profile = await getJson(`https://sessionserver.mojang.com/session/minecraft/profile/${uuid}`);
  const prop = (profile.properties || []).find(p => p.name === 'textures');
  if (!prop) throw new Error('Brak tekstur w profilu');
  const tex = JSON.parse(Buffer.from(prop.value, 'base64').toString('utf8')).textures || {};
  const out = {
    skin: tex.SKIN ? await toDataUrl(tex.SKIN.url) : null,
    slim: !!(tex.SKIN && tex.SKIN.metadata && tex.SKIN.metadata.model === 'slim'),
    cape: tex.CAPE ? await toDataUrl(tex.CAPE.url) : null,
    at: Date.now()
  };
  fs.mkdirSync(DIR, { recursive: true });
  fs.writeFileSync(file, JSON.stringify(out));
  return out;
}

/** Zwraca { skin, slim, cape } jako obrazki data: albo null. */
async function getSkin(uuid) {
  if (!uuid) return null;
  const id = String(uuid).replace(/-/g, '');
  const file = path.join(DIR, id + '.json');
  const cached = readCache(file);
  if (cached && Date.now() - cached.at < MAX_AGE) return cached;
  try {
    return await fetchFresh(id, file);
  } catch (e) {
    console.warn('Skin niedostępny:', e.message);
    return cached; // brak internetu – pokaż ostatni zapisany
  }
}

/** Wysyła nowy skin na konto (oficjalne API Minecrafta) i zwraca odświeżony podgląd. */
async function uploadSkin(uuid, filePath, slim, token) {
  const png = fs.readFileSync(filePath);
  if (png.length < 24 || png.toString('ascii', 1, 4) !== 'PNG') throw new Error('To nie jest plik PNG');
  const w = png.readUInt32BE(16);
  const h = png.readUInt32BE(20);
  if (w !== 64 || (h !== 64 && h !== 32)) throw new Error(`Skin musi mieć 64×64 piksele (ten ma ${w}×${h})`);

  const form = new FormData();
  form.append('variant', slim ? 'slim' : 'classic');
  form.append('file', new Blob([png], { type: 'image/png' }), 'skin.png');
  const res = await fetch('https://api.minecraftservices.com/minecraft/profile/skins', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + token, 'User-Agent': UA },
    body: form
  });
  if (res.status === 401) throw new Error('Sesja wygasła – zaloguj się ponownie');
  if (res.status === 429) throw new Error('Za dużo prób – spróbuj za minutę');
  if (!res.ok) throw new Error('Serwer odrzucił skin (kod ' + res.status + ')');

  const id = String(uuid).replace(/-/g, '');
  const local = {
    skin: 'data:image/png;base64,' + png.toString('base64'),
    slim: !!slim,
    cape: (readCache(path.join(DIR, id + '.json')) || {}).cape || null,
    at: Date.now()
  };
  fs.mkdirSync(DIR, { recursive: true });
  fs.writeFileSync(path.join(DIR, id + '.json'), JSON.stringify(local));
  return local;
}

module.exports = { getSkin, uploadSkin };
