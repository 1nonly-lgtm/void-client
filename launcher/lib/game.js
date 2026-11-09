// Wersje gry, Java, Fabric i samo uruchamianie (minecraft-launcher-core).
const { app } = require('electron');
const fs = require('fs');
const path = require('path');
const { getJson, download, pool, exists } = require('./net');

const ROOT = path.join(app.getPath('appData'), '.voidclient');
const MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';
const JAVA_ALL = 'https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json';
// Które wydanie moda Void Client pasuje do której wersji gry: [od (włącznie), do (bez), plik].
const VOID_BUILDS = [
  ['1.20', '1.20.2', '1.20.1'],
  ['1.20.2', '1.20.5', '1.20.4'],
  ['1.20.5', '1.21', '1.20.6'],
  ['1.21', '1.21.2', '1.21.1'],
  ['1.21.2', '1.21.5', '1.21.4'],
  ['1.21.5', '1.21.6', '1.21.5'],
  ['1.21.6', '1.21.9', '1.21.8'],
  ['1.21.9', '1.21.11', '1.21.10'],
  ['1.21.11', '1.22', '1.21.11'],
  ['26.1', '26.2', '26.1.2']
];

function cmpVersion(a, b) {
  const pa = String(a).split('.').map(n => parseInt(n, 10) || 0);
  const pb = String(b).split('.').map(n => parseInt(n, 10) || 0);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const d = (pa[i] || 0) - (pb[i] || 0);
    if (d) return d;
  }
  return 0;
}

/** Nazwa pliku moda dla danej wersji gry albo null. */
function voidBuildFor(mc) {
  const hit = VOID_BUILDS.find(([from, to]) => cmpVersion(mc, from) >= 0 && cmpVersion(mc, to) < 0);
  return hit ? `voidclient-${hit[2]}.jar` : null;
}

// Flagi Javy pod płynność (G1GC, krótkie pauzy GC = mniej przycięć).
const JVM_FLAGS = [
  '-XX:+UseG1GC',
  '-XX:+ParallelRefProcEnabled',
  '-XX:MaxGCPauseMillis=40',
  '-XX:+UnlockExperimentalVMOptions',
  '-XX:+DisableExplicitGC',
  '-XX:G1NewSizePercent=30',
  '-XX:G1MaxNewSizePercent=40',
  '-XX:G1HeapRegionSize=8M',
  '-XX:G1ReservePercent=20',
  '-XX:G1HeapWastePercent=5',
  '-XX:G1MixedGCCountTarget=4',
  '-XX:InitiatingHeapOccupancyPercent=15',
  '-XX:G1MixedGCLiveThresholdPercent=90',
  '-XX:SurvivorRatio=32',
  '-XX:+PerfDisableSharedMem',
  '-XX:MaxTenuringThreshold=1'
];

const instanceDir = id => path.join(ROOT, 'instances', id.replace(/[^a-z0-9_-]/gi, '_'));

// ───────── Wersje ─────────

let manifestCache = null;
async function manifest() {
  if (manifestCache) return manifestCache;
  const cacheFile = path.join(ROOT, 'cache', 'version_manifest_v2.json');
  try {
    manifestCache = await getJson(MANIFEST);
    fs.mkdirSync(path.dirname(cacheFile), { recursive: true });
    fs.writeFileSync(cacheFile, JSON.stringify(manifestCache));
  } catch (e) {
    if (!exists(cacheFile)) throw e; // brak internetu i brak kopii
    manifestCache = JSON.parse(fs.readFileSync(cacheFile, 'utf8'));
  }
  return manifestCache;
}

async function listVersions() {
  const m = await manifest();
  return { latest: m.latest.release, releases: m.versions.filter(v => v.type === 'release').map(v => v.id) };
}

async function resolveMc(mc) {
  return mc === 'latest' ? (await manifest()).latest.release : mc;
}

async function versionJson(mc) {
  const file = path.join(ROOT, 'versions', mc, `${mc}.json`);
  if (exists(file)) return JSON.parse(fs.readFileSync(file, 'utf8'));
  const entry = (await manifest()).versions.find(v => v.id === mc);
  if (!entry) throw new Error(`Nie ma wersji ${mc}`);
  const json = await getJson(entry.url);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(json));
  return json;
}

// ───────── Java (oficjalne paczki od Mojang) ─────────

async function ensureJava(component, emit) {
  const dir = path.join(ROOT, 'runtime', component);
  const javaw = path.join(dir, 'bin', 'javaw.exe');
  if (exists(path.join(dir, '.ok')) && exists(javaw)) return javaw;

  emit({ text: 'Pobieranie Javy…', cur: 0, total: 1 });
  const all = await getJson(JAVA_ALL);
  const platform = process.arch === 'arm64' ? 'windows-arm64' : process.arch === 'ia32' ? 'windows-x86' : 'windows-x64';
  const entry = all[platform] && all[platform][component] && all[platform][component][0];
  if (!entry) throw new Error(`Brak Javy (${component}) dla tego systemu`);
  const man = await getJson(entry.manifest.url);
  const files = Object.entries(man.files).filter(([, f]) => f.type === 'file');
  let done = 0;
  await pool(files, 16, async ([rel, f]) => {
    const dest = path.join(dir, rel);
    if (!exists(dest)) await download(f.downloads.raw.url, dest, f.downloads.raw.sha1);
    done++;
    if (done % 8 === 0 || done === files.length) emit({ text: 'Pobieranie Javy…', cur: done, total: files.length });
  });
  fs.writeFileSync(path.join(dir, '.ok'), entry.version.name || 'ok');
  return javaw;
}

// ───────── Fabric ─────────

async function ensureFabric(mc) {
  const versionsDir = path.join(ROOT, 'versions');
  try {
    const loaders = await getJson(`https://meta.fabricmc.net/v2/versions/loader/${mc}`);
    const pick = loaders.find(l => l.loader.stable) || loaders[0];
    if (!pick) throw new Error(`Fabric nie wspiera wersji ${mc}`);
    const id = `fabric-loader-${pick.loader.version}-${mc}`;
    const file = path.join(versionsDir, id, `${id}.json`);
    if (!exists(file)) {
      const profile = await getJson(`https://meta.fabricmc.net/v2/versions/loader/${mc}/${pick.loader.version}/profile/json`);
      fs.mkdirSync(path.dirname(file), { recursive: true });
      fs.writeFileSync(file, JSON.stringify(profile));
    }
    return id;
  } catch (e) {
    // offline – użyj już pobranego Fabrica
    const local = exists(versionsDir) && fs.readdirSync(versionsDir).filter(d => d.startsWith('fabric-loader-') && d.endsWith('-' + mc)).sort().pop();
    if (local) return local;
    throw e;
  }
}

function voidModSource(file) {
  return app.isPackaged
    ? path.join(process.resourcesPath, 'resources', 'mods', file)
    : path.join(__dirname, '..', 'resources', 'mods', file);
}

function syncVoidMod(dir, enabled, mc) {
  const dest = path.join(dir, 'mods', 'voidclient.jar');
  const file = enabled ? voidBuildFor(mc) : null;
  if (!file) {
    fs.rmSync(dest, { force: true });
    return false;
  }
  const src = voidModSource(file);
  if (!exists(src)) {
    console.warn('Brak pliku moda', file);
    return false;
  }
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.copyFileSync(src, dest); // mały plik – kopiowanie zawsze, żeby pasował do wybranej wersji
  return true;
}

// ───────── Start gry ─────────

const STEP_NAMES = {
  assets: 'Pobieranie zasobów gry…',
  'assets-copy': 'Kopiowanie zasobów…',
  natives: 'Pobieranie natywnych bibliotek…',
  classes: 'Pobieranie bibliotek…',
  'classes-custom': 'Pobieranie bibliotek Fabric…',
  'classes-maven-custom': 'Pobieranie bibliotek Fabric…'
};

/**
 * @param profile profil z ustawień
 * @param s ustawienia
 * @param emit callback postępu: {text, cur, total}
 * @param onLine callback linii logu gry
 */
async function launch(profile, s, emit, onLine) {
  emit({ text: 'Logowanie…' });
  const authorization = await require('./auth').getLaunchAuth();

  const mc = await resolveMc(profile.mc);
  emit({ text: 'Sprawdzanie wersji ' + mc + '…' });
  const vjson = await versionJson(mc);
  const javaPath = await ensureJava((vjson.javaVersion && vjson.javaVersion.component) || 'jre-legacy', emit);

  const dir = instanceDir(profile.id);
  fs.mkdirSync(dir, { recursive: true });

  let custom;
  if (profile.loader === 'fabric') {
    emit({ text: 'Przygotowanie Fabric…' });
    custom = await ensureFabric(mc);
    const withVoid = syncVoidMod(dir, profile.voidMod, mc);
    const modrinth = require('./modrinth');
    const pack = profile.optimize ? modrinth.FPS_PACK : withVoid ? ['fabric-api'] : [];
    if (pack.length) await modrinth.ensurePack(pack, { mc, loader: 'fabric', instanceDir: dir, onStep: text => emit({ text }) });
  }

  const { Client } = require('minecraft-launcher-core');
  const launcher = new Client();
  const ram = Math.max(1, Math.min(32, Number(s.ram) || 4));

  launcher.on('progress', e => emit({ text: STEP_NAMES[e.type] || 'Pobieranie plików…', cur: e.task, total: e.total }));
  launcher.on('data', line => onLine && onLine(String(line)));
  launcher.on('debug', line => { if (/error/i.test(String(line))) console.warn(String(line)); });

  emit({ text: 'Uruchamianie gry…' });
  const proc = await launcher.launch({
    authorization,
    root: ROOT,
    version: { number: mc, type: 'release', ...(custom ? { custom } : {}) },
    memory: { max: ram + 'G', min: Math.min(2, ram) + 'G' },
    javaPath,
    customArgs: s.jvmFlags ? JVM_FLAGS : [],
    window: s.windowMode === 'full' ? { fullscreen: true } : { width: 1280, height: 720 },
    overrides: {
      gameDirectory: dir,
      maxSockets: 32,
      detached: s.afterLaunch === 'close'
    }
  });
  if (!proc) throw new Error('Gra się nie uruchomiła – sprawdź połączenie i spróbuj ponownie');
  return { proc, mc };
}

module.exports = { ROOT, instanceDir, listVersions, launch, voidBuildFor };
