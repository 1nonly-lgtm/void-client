// Dodawanie własnych plików przeciągniętych do launchera: mody (.jar), shadery i resource packi (.zip).
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

/** Spis plików w archiwum ZIP/JAR (bez zewnętrznych bibliotek). */
function zipEntries(buf) {
  let eocd = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 65557); i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error('To nie jest poprawny plik .jar/.zip');
  const count = buf.readUInt16LE(eocd + 10);
  let p = buf.readUInt32LE(eocd + 16);
  const out = new Map();
  for (let i = 0; i < count && p + 46 <= buf.length; i++) {
    if (buf.readUInt32LE(p) !== 0x02014b50) break;
    const method = buf.readUInt16LE(p + 10);
    const size = buf.readUInt32LE(p + 20);
    const nameLen = buf.readUInt16LE(p + 28);
    const extraLen = buf.readUInt16LE(p + 30);
    const commentLen = buf.readUInt16LE(p + 32);
    const local = buf.readUInt32LE(p + 42);
    const name = buf.toString('utf8', p + 46, p + 46 + nameLen);
    out.set(name, { method, size, local });
    p += 46 + nameLen + extraLen + commentLen;
  }
  return out;
}

function zipRead(buf, entry) {
  const p = entry.local;
  const start = p + 30 + buf.readUInt16LE(p + 26) + buf.readUInt16LE(p + 28);
  const raw = buf.subarray(start, start + entry.size);
  return entry.method === 8 ? zlib.inflateRawSync(raw) : raw;
}

/** Czym jest plik i jak się nazywa. */
function inspect(file) {
  const ext = path.extname(file).toLowerCase();
  if (ext !== '.jar' && ext !== '.zip') throw new Error(`${path.basename(file)}: obsługiwane są tylko pliki .jar i .zip`);
  const buf = fs.readFileSync(file);
  const entries = zipEntries(buf);
  const base = path.basename(file, ext);

  if (ext === '.jar' || entries.has('fabric.mod.json')) {
    const info = { type: 'mod', title: base, loader: 'unknown' };
    const fmj = entries.get('fabric.mod.json');
    if (fmj) {
      info.loader = 'fabric';
      try {
        const json = JSON.parse(zipRead(buf, fmj).toString('utf8').replace(/^﻿/, ''));
        info.title = json.name || json.id || base;
        info.modId = json.id;
        info.version = json.version;
        info.mcDep = json.depends && json.depends.minecraft;
      } catch { /* zostaje nazwa pliku */ }
    } else if (entries.has('quilt.mod.json')) {
      info.loader = 'quilt';
    } else if (entries.has('META-INF/mods.toml') || entries.has('META-INF/neoforge.mods.toml') || entries.has('mcmod.info')) {
      info.loader = 'forge';
    }
    return info;
  }

  const names = [...entries.keys()];
  if (names.some(n => /^([^/]+\/)?shaders\//i.test(n))) return { type: 'shader', title: base };
  if (names.some(n => /^([^/]+\/)?pack\.mcmeta$/i.test(n))) return { type: 'resourcepack', title: base };
  throw new Error(`${path.basename(file)}: nie wygląda na mod, shader ani resource pack`);
}

const FOLDERS = { mod: 'mods', shader: 'shaderpacks', resourcepack: 'resourcepacks' };

/**
 * Kopiuje pliki do profilu i dopisuje je do listy „Zainstalowane”.
 * @returns lista { file, title, type, ok, error?, warn? }
 */
function add(paths, profile, instanceDir, modrinth) {
  const results = [];
  const idx = modrinth.readIndex(instanceDir);
  for (const src of paths) {
    const file = path.basename(src);
    try {
      const info = inspect(src);
      if (info.type === 'mod' && profile.loader === 'vanilla') {
        throw new Error(`Profil „${profile.name}” jest bez modów – wybierz profil z Fabric`);
      }
      if (info.type === 'mod' && info.loader === 'forge') {
        throw new Error(`${info.title} to mod na Forge – Void Client używa Fabric`);
      }
      const folder = FOLDERS[info.type];
      const destDir = path.join(instanceDir, folder);
      fs.mkdirSync(destDir, { recursive: true });
      // ten sam mod w innej wersji – stary plik usuwamy, żeby gra się nie wysypała
      for (const [id, e] of Object.entries(idx)) {
        if (info.modId && e.modId === info.modId && e.file !== file) {
          fs.rmSync(path.join(instanceDir, e.folder, e.file), { force: true });
          delete idx[id];
        }
      }
      fs.copyFileSync(src, path.join(destDir, file));
      idx['local:' + file] = { title: info.title, file, folder, type: info.type, local: true, modId: info.modId };
      let warn;
      if (info.type === 'mod' && info.loader === 'unknown') warn = 'nie wiadomo, czy to mod na Fabric';
      results.push({ file, title: info.title, type: info.type, ok: true, warn });
    } catch (e) {
      results.push({ file, ok: false, error: e.message });
    }
  }
  modrinth.writeIndex(instanceDir, idx);
  return results;
}

module.exports = { add, inspect };
