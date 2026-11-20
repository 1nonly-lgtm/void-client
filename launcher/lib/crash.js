// Pomocnik przy crashu: czyta raport z gry, mówi po ludzku, co się stało, i wskazuje winny mod.
const fs = require('fs');
const path = require('path');

const SAFE = new Set(['minecraft', 'java', 'fabricloader', 'fabric-loader', 'fabric', 'fabric-api', 'mixinextras', 'voidclient', 'void-client']);

function newest(dir, since, filter) {
  try {
    return fs.readdirSync(dir)
      .filter(filter)
      .map(f => ({ f: path.join(dir, f), t: fs.statSync(path.join(dir, f)).mtimeMs }))
      .filter(x => x.t >= since - 5000)
      .sort((a, b) => b.t - a.t)[0]?.f || null;
  } catch { return null; }
}

const read = f => { try { return fs.readFileSync(f, 'utf8'); } catch { return ''; } };
const norm = s => String(s || '').toLowerCase().replace(/[^a-z0-9]/g, '');

/** Mapa: id moda → plik .jar (z fabric.mod.json, a jak się nie da – po nazwie pliku). */
function modFiles(dir) {
  const mods = path.join(dir, 'mods');
  let files = [];
  try { files = fs.readdirSync(mods).filter(f => f.endsWith('.jar')); } catch { return []; }
  let Zip = null;
  try { Zip = require('adm-zip'); } catch { /* bez czytania jarów */ }
  return files.map(file => {
    let id = null, name = null;
    if (Zip) {
      try {
        const j = JSON.parse(new Zip(path.join(mods, file)).readAsText('fabric.mod.json'));
        id = j.id; name = j.name;
      } catch { /* zwykły plik */ }
    }
    return { file, id, name };
  });
}

function findMod(list, idOrName) {
  const k = norm(idOrName);
  if (!k || SAFE.has(String(idOrName).toLowerCase())) return null;
  return list.find(m => m.id && norm(m.id) === k)
    || list.find(m => m.name && norm(m.name) === k)
    || list.find(m => norm(m.file).startsWith(k))
    || list.find(m => k.length >= 4 && norm(m.file).includes(k))
    || null;
}

/**
 * @returns {{title, text, file?, mod?, kind}} albo null, gdy nic nie znaleziono
 */
function analyze(dir, since) {
  const report = newest(path.join(dir, 'crash-reports'), since, f => f.endsWith('.txt'));
  const text = read(report) + '\n' + read(path.join(dir, 'logs', 'latest.log')).slice(-200000);
  if (!text.trim()) return null;

  if (/OutOfMemoryError|Java heap space/i.test(text)) {
    return { kind: 'ram', title: 'Za mało pamięci RAM', text: 'Grze zabrakło pamięci. Dodaj RAM w Opcjach (np. 6 GB) albo zdejmij ciężki resource pack lub shader.' };
  }
  if (/UnsupportedClassVersionError|compiled by a more recent version of the Java Runtime/i.test(text)) {
    return { kind: 'java', title: 'Mod wymaga nowszej Javy', text: 'Któryś mod jest zrobiony pod nowszą wersję gry niż ten profil. Usuń ostatnio dodany mod albo sprawdź jego wersję w zakładce Mody.' };
  }
  if (/WGL: The driver does not appear to support OpenGL|GLFW error 65542|Pixel format not accelerated|EXCEPTION_ACCESS_VIOLATION[\s\S]{0,4000}(atio6axx|nvoglv|ig\d+icd|igxelpicd)/i.test(text)) {
    return { kind: 'gpu', title: 'Problem ze sterownikiem karty graficznej', text: 'Sterownik grafiki się wysypał. Zaktualizuj sterownik (NVIDIA / AMD / Intel) ze strony producenta i uruchom ponownie komputer.' };
  }

  const list = modFiles(dir);
  const ids = [];
  const push = (id, why) => { if (id && !ids.some(x => x.id === id)) ids.push({ id, why }); };
  let m;
  // Fabric: niezgodne mody / zła wersja
  const inc = /Mod '([^']+)' \(([^)]+)\)[^\n]*?(requires|is incompatible|breaks)[^\n]*/g;
  while ((m = inc.exec(text))) push(m[2], /minecraft/i.test(m[0]) ? 'version' : 'incompatible');
  const ep = /provided by '([^']+)'/g; while ((m = ep.exec(text))) push(m[1], 'crash');
  const mx = /Mixin apply for mod (\S+) failed/g; while ((m = mx.exec(text))) push(m[1], 'crash');
  const mx2 = /from mod ([a-z0-9_\-]+)\]/g; while ((m = mx2.exec(text))) push(m[1], 'crash');
  const sus = /Suspected Mods?:\s*([^\n]+)/g; while ((m = sus.exec(text))) { const k = /\(([a-z0-9_\-]+)\)/i.exec(m[1]); push(k ? k[1] : m[1].trim(), 'crash'); }

  for (const c of ids) {
    const mod = findMod(list, c.id);
    if (!mod) continue;
    const nm = mod.name || mod.id || mod.file;
    if (c.why === 'version') return { kind: 'version', title: `Mod pod złą wersję gry: ${nm}`, text: `${nm} jest zrobiony pod inną wersję Minecrafta niż ten profil. Wyłącz go albo pobierz wersję pod ten profil z zakładki Mody.`, file: mod.file, mod: nm };
    return { kind: 'mod', title: `Grę wysypał mod ${nm}`, text: `Z raportu wynika, że problem robi ${nm}. Możesz go wyłączyć jednym kliknięciem – plik trafi do folderu „wyłączone”, a gra uruchomi się bez niego.`, file: mod.file, mod: nm };
  }

  if (/ZipException|invalid LOC header|zip END header not found|Corrupt|Malformed/i.test(text)) {
    return { kind: 'corrupt', title: 'Uszkodzony plik', text: 'Któryś plik gry albo moda pobrał się z błędem. Usuń ostatnio dodany mod lub uruchom grę jeszcze raz – launcher pobierze brakujące pliki.' };
  }
  return null;
}

/** Przenosi mod do mods/wyłączone i oznacza go, żeby launcher nie pobrał go z powrotem. */
function disable(dir, file) {
  const base = path.basename(String(file || ''));
  if (!base.endsWith('.jar')) throw new Error('To nie jest plik moda');
  const src = path.join(dir, 'mods', base);
  const offDir = path.join(dir, 'mods', 'wyłączone');
  fs.mkdirSync(offDir, { recursive: true });
  fs.renameSync(src, path.join(offDir, base));
  try {
    const modrinth = require('./modrinth');
    const idx = modrinth.readIndex(dir);
    for (const v of Object.values(idx)) if (v.file === base) v.skip = true;
    modrinth.writeIndex(dir, idx);
  } catch { /* brak indeksu */ }
  return base;
}

module.exports = { analyze, disable };
