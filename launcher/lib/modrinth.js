// Wyszukiwarka i instalacja z Modrinth (https://docs.modrinth.com).
const fs = require('fs');
const path = require('path');
const { getJson, download, exists } = require('./net');

const API = 'https://api.modrinth.com/v2';
const FOLDERS = { mod: 'mods', shader: 'shaderpacks', resourcepack: 'resourcepacks' };

const q = v => encodeURIComponent(typeof v === 'string' ? v : JSON.stringify(v));

async function search({ query = '', type = 'mod', mc, loader, offset = 0 }) {
  const facets = [[`project_type:${type}`]];
  if (mc) facets.push([`versions:${mc}`]);
  if (type === 'mod' && loader && loader !== 'vanilla') facets.push([`categories:${loader}`]);
  const index = query.trim() ? 'relevance' : 'downloads';
  const url = `${API}/search?query=${q(query.trim())}&limit=24&offset=${offset}&index=${index}&facets=${q(facets)}`;
  const res = await getJson(url);
  return {
    total: res.total_hits,
    hits: res.hits.map(h => ({
      id: h.project_id,
      slug: h.slug,
      title: h.title,
      description: h.description,
      icon: h.icon_url || '',
      downloads: h.downloads,
      author: h.author,
      type: h.project_type
    }))
  };
}

function indexFile(instanceDir) {
  return path.join(instanceDir, '.voidclient-content.json');
}

function readIndex(instanceDir) {
  try { return JSON.parse(fs.readFileSync(indexFile(instanceDir), 'utf8')); } catch { return {}; }
}

function writeIndex(instanceDir, idx) {
  fs.mkdirSync(instanceDir, { recursive: true });
  fs.writeFileSync(indexFile(instanceDir), JSON.stringify(idx, null, 2));
}

async function pickVersion(projectId, type, mc, loader) {
  let url = `${API}/project/${projectId}/version?game_versions=${q([mc])}`;
  if (type === 'mod') url += `&loaders=${q([loader])}`;
  const versions = await getJson(url);
  return versions.find(v => v.version_type === 'release') || versions[0] || null;
}

/**
 * Instaluje projekt (i wymagane zależności) do folderu profilu.
 * @returns {Promise<string[]>} nazwy zainstalowanych projektów
 */
async function install({ projectId, type = 'mod', mc, loader, instanceDir, title }, seen = new Set()) {
  if (seen.has(projectId)) return [];
  seen.add(projectId);
  if (type === 'mod' && (!loader || loader === 'vanilla')) {
    throw new Error('Mody wymagają profilu z Fabric/NeoForge – zmień loader w profilu');
  }
  const v = await pickVersion(projectId, type, mc, loader);
  if (!v) throw new Error(`Brak wersji „${title || projectId}” dla ${mc}${type === 'mod' ? ' / ' + loader : ''}`);
  const file = v.files.find(f => f.primary) || v.files[0];
  const folder = path.join(instanceDir, FOLDERS[type] || 'mods');
  const idx = readIndex(instanceDir);
  const prev = idx[projectId];
  if (prev && prev.file !== file.filename) await fs.promises.rm(path.join(instanceDir, prev.folder, prev.file), { force: true });
  const dest = path.join(folder, file.filename);
  if (!exists(dest)) await download(file.url, dest, file.hashes && file.hashes.sha1);
  idx[projectId] = { file: file.filename, folder: FOLDERS[type] || 'mods', title: title || file.filename, type, versionId: v.id, mc };
  writeIndex(instanceDir, idx);

  const installed = [title || file.filename];
  if (type === 'mod') {
    for (const dep of v.dependencies || []) {
      if (dep.dependency_type !== 'required' || !dep.project_id) continue;
      const current = readIndex(instanceDir)[dep.project_id];
      if (current && current.mc === mc) continue;
      try {
        installed.push(...await install({ projectId: dep.project_id, type, mc, loader, instanceDir }, seen));
      } catch (e) {
        console.warn('Zależność pominięta', dep.project_id, e.message);
      }
    }
  }
  return installed;
}

function listInstalled(instanceDir) {
  const idx = readIndex(instanceDir);
  return Object.entries(idx)
    .filter(([, v]) => v.file && exists(path.join(instanceDir, v.folder, v.file)))
    .map(([id, v]) => ({ id, ...v }));
}

async function remove(projectId, instanceDir) {
  const idx = readIndex(instanceDir);
  const e = idx[projectId];
  if (e) await fs.promises.rm(path.join(instanceDir, e.folder, e.file), { force: true });
  delete idx[projectId];
  writeIndex(instanceDir, idx);
}

/** Pakiet pod FPS dla profili Fabric. Pomija sieć, gdy wszystko już jest. */
const FPS_PACK = [
  'fabric-api',
  'sodium',          // nowy silnik renderowania
  'lithium',         // logika gry, fizyka, AI mobów
  'ferrite-core',    // mniej RAM
  'entityculling',   // nie rysuje niewidocznych mobów i skrzyń
  'immediatelyfast', // szybszy HUD, tekst, mapy
  'moreculling',     // pomija niewidoczne ściany bloków
  'modernfix',       // szybszy start i mniej RAM
  'badoptimizations',// drobne optymalizacje renderu
  'dynamic-fps'      // gdy gra jest w tle, nie męczy komputera
];

async function ensurePack(slugs, { mc, loader, instanceDir, onStep }) {
  const idx = readIndex(instanceDir);
  const present = new Set(Object.values(idx)
    .filter(v => v.mc === mc && (v.skip || exists(path.join(instanceDir, v.folder, v.file))))
    .map(v => v.slug));
  const missing = slugs.filter(s => !present.has(s));
  for (const slug of missing) {
    onStep && onStep(`Instalowanie ${slug}…`);
    try {
      const project = await getJson(`${API}/project/${slug}`);
      await install({ projectId: project.id, type: 'mod', mc, loader, instanceDir, title: project.title });
      const after = readIndex(instanceDir);
      if (after[project.id]) { after[project.id].slug = slug; writeIndex(instanceDir, after); }
    } catch (e) {
      console.warn(`Pominięto ${slug}:`, e.message); // np. mod nie wspiera tej wersji gry
      if (!/sieci|fetch/i.test(e.message)) {
        const after = readIndex(instanceDir);
        after['skip:' + slug] = { skip: true, slug, mc };
        writeIndex(instanceDir, after);
      }
    }
  }
}

module.exports = { search, install, listInstalled, remove, ensurePack, FPS_PACK };
