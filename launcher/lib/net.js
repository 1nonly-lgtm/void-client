// Pobieranie plików i JSON-ów – bez zewnętrznych bibliotek.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { Readable } = require('stream');
const { pipeline } = require('stream/promises');

const UA = 'oneonly/void-client/1.0.0 (Void Client launcher)';

async function getJson(url, opts = {}) {
  const res = await fetch(url, { headers: { 'User-Agent': UA, ...(opts.headers || {}) } });
  if (!res.ok) throw new Error(`Błąd sieci ${res.status} (${new URL(url).host})`);
  return res.json();
}

function sha1File(file) {
  return new Promise((resolve, reject) => {
    const h = crypto.createHash('sha1');
    fs.createReadStream(file).on('data', d => h.update(d)).on('end', () => resolve(h.digest('hex'))).on('error', reject);
  });
}

async function download(url, dest, sha1) {
  await fs.promises.mkdir(path.dirname(dest), { recursive: true });
  const tmp = dest + '.part';
  const res = await fetch(url, { headers: { 'User-Agent': UA } });
  if (!res.ok || !res.body) throw new Error(`Nie udało się pobrać ${path.basename(dest)} (${res.status})`);
  await pipeline(Readable.fromWeb(res.body), fs.createWriteStream(tmp));
  if (sha1) {
    const got = await sha1File(tmp);
    if (got !== sha1) {
      await fs.promises.rm(tmp, { force: true });
      throw new Error(`Uszkodzony plik ${path.basename(dest)} – spróbuj ponownie`);
    }
  }
  await fs.promises.rename(tmp, dest);
}

/** Wykonuje zadania równolegle (limit naraz). */
async function pool(items, limit, worker) {
  let i = 0;
  const runners = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (i < items.length) {
      const idx = i++;
      await worker(items[idx], idx);
    }
  });
  await Promise.all(runners);
}

function exists(p) {
  try { fs.accessSync(p); return true; } catch { return false; }
}

module.exports = { UA, getJson, download, pool, exists, sha1File };
