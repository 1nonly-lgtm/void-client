/* Kosmetyki Void Client – podgląd 3D (three.js r128). 1 jednostka = 1 piksel skina Minecrafta. */
{
const T = THREE;
const PI = Math.PI;
const rnd = (s => () => (s = (s * 16807) % 2147483647) / 2147483647)(7);
const hsl = (h, s, l, a) => a === undefined ? `hsl(${h},${s}%,${l}%)` : `hsla(${h},${s}%,${l}%,${a})`;
const HSH = (x, y, s) => { const v = Math.sin(x * 127.1 + y * 311.7 + s * 74.7) * 43758.5453; return v - Math.floor(v); };

/* ---------------- peleryny: 10×16 pikseli, kolor zależny od czasu ---------------- */
const CAPES = [
 ['pustka', 'Pustka', (x, y, t) => { const dx = x - 4.5, dy = y - 7.5, r = Math.hypot(dx, dy), a = Math.atan2(dy, dx); const v = Math.sin(a * 3 + r * .9 - t * 2.2); return hsl(268, 70, Math.max(6, 10 + v * 14 + (r < 1.6 ? 40 : 0) - r * 1.2)); }],
 ['zorza', 'Zorza', (x, y, t) => { const w = Math.sin(x * .7 + t * 1.6) * 2 + Math.sin(x * .3 - t) * 1.5; const d = Math.abs(y - 6 - w); return d < 3 ? hsl(150 + d * 25 + Math.sin(t + x) * 20, 80, 55 - d * 12) : hsl(220, 50, 8 + y * .5); }],
 ['plomien', 'Płomień', (x, y, t) => { const n = HSH(x, Math.floor(y + t * 8), 3); const h = 15 - y + n * 6 + Math.sin(x + t * 5) * 1.5; return h > 9 ? hsl(48, 100, 60) : h > 5 ? hsl(25, 100, 52) : h > 1 ? hsl(5, 85, 38) : hsl(0, 40, 8); }],
 ['galaktyka', 'Galaktyka', (x, y, t) => { const s = HSH(x, y, 7); const tw = s > .86 ? (Math.sin(t * 3 + s * 40) * .5 + .5) : 0; return tw > .2 ? hsl(240, 60, 55 + tw * 40) : hsl(250 + Math.sin(x * .6 + y * .3) * 15, 60, 10 + Math.sin(x * .5 - y * .4 + t * .3) * 5); }],
 ['neon', 'Neon', (x, y, t) => { const e = x === 0 || x === 9 || y === 0 || y === 15; if (!e) return hsl(270, 40, 9); const p = y === 0 ? x : x === 9 ? 9 + y : y === 15 ? 24 + (9 - x) : 33 + (15 - y); const d = ((p - t * 14) % 50 + 50) % 50; return d < 8 ? hsl(300 - d * 6, 95, 65 - d * 4) : hsl(280, 60, 22); }],
 ['ender', 'Ender', (x, y, t) => HSH(x, Math.floor(y + t * 4), 11) > .9 ? hsl(285, 90, 70) : hsl(275, 45, 7 + HSH(x, y, 2) * 4)],
 ['tecza', 'Tęcza', (x, y, t) => hsl((y * 18 + x * 6 - t * 90) % 360, 85, 58)],
 ['glitch', 'Glitch', (x, y, t) => { const r = Math.floor(t * 6); const sh = HSH(0, y, r) > .8 ? Math.floor(HSH(1, y, r) * 4) - 2 : 0; const xx = x + sh; return (xx + y) % 4 === 0 ? hsl(185, 90, 55) : HSH(xx, y, r) > .92 ? hsl(330, 95, 60) : hsl(220, 30, 10); }],
 ['lawa', 'Lawa', (x, y, t) => { const v = Math.sin(x * .9 + t * 1.2) + Math.sin(y * .7 - t * 1.7) + Math.sin((x + y) * .5 + t); return v > 1.2 ? hsl(45, 100, 62) : v > .2 ? hsl(22, 100, 50) : v > -.8 ? hsl(8, 85, 32) : hsl(10, 30, 10); }],
 ['ocean', 'Ocean', (x, y, t) => { const w = Math.sin(x * .8 - t * 2 + y * .5) * .5 + .5; const foam = HSH(x, Math.floor(y - t * 3), 5) > .94; return foam ? hsl(190, 60, 85) : hsl(200 + w * 15, 80, 22 + w * 20 + y * .8); }],
 ['matrix', 'Matrix', (x, y, t) => { const sp = 4 + HSH(x, 0, 9) * 6; const head = (t * sp + HSH(x, 1, 9) * 16) % 22; const d = head - y; return d >= 0 && d < 1 ? hsl(120, 90, 80) : d > 0 && d < 7 ? hsl(130, 90, 45 - d * 5) : hsl(140, 40, 5); }],
 ['plazma', 'Plazma', (x, y, t) => { const v = Math.sin(x * .6 + t) + Math.sin(y * .4 + t * 1.3) + Math.sin(Math.hypot(x - 5, y - 8) * .8 - t * 2); return hsl(280 + v * 40, 90, 45 + v * 8); }],
 ['sniezyca', 'Śnieżyca', (x, y, t) => HSH(Math.floor(x + Math.sin(t + y) * .8), Math.floor(y - t * 5), 13) > .86 ? hsl(200, 40, 95) : hsl(210, 45, 26 + y * 1.4)],
 ['zloto', 'Złoto', (x, y, t) => { const s = ((x + y * .6 - t * 9) % 18 + 18) % 18; return s < 2 ? hsl(50, 100, 85) : hsl(42, 85, 40 + ((x * 3 + y * 5) % 7) * 2); }],
 ['krysztal', 'Kryształ', (x, y, t) => { const f = Math.abs(((x * 1.7 + y) % 6) - 3) + Math.abs(((y * 1.3 - x) % 5) - 2.5); return hsl(190 + f * 12 + Math.sin(t * 2 + x) * 10, 80, 30 + f * 6 + (HSH(x, y, Math.floor(t * 3)) > .95 ? 35 : 0)); }],
 ['sakura', 'Sakura', (x, y, t) => HSH(Math.floor(x - Math.sin(t * .8 + y * .3) * 1.2), Math.floor(y - t * 2.2), 17) > .88 ? hsl(340, 90, 85) : hsl(330 - y * 2, 55, 30 + y * 2)],
 ['mglawica', 'Mgławica', (x, y, t) => { const v = Math.sin(x * .5 + t * .4) + Math.sin(y * .35 - t * .3) + Math.sin((x - y) * .4 + t * .2); const st = HSH(x, y, 23) > .93 && Math.sin(t * 4 + x * y) > 0; return st ? hsl(200, 80, 90) : hsl(250 + v * 30, 75, Math.max(6, 14 + v * 9)); }],
 ['burza', 'Burza', (x, y, t) => { const k = Math.floor(t / 2.2), fl = (t % 2.2) < .14; const x0 = 4 + Math.round(Math.sin(y * 1.3 + k * 7) * 2); if (fl && Math.abs(x - x0) < 1) return hsl(210, 100, 95); return hsl(228, 25, 12 + (fl ? 22 : 0) + HSH(x >> 1, y >> 1, k) * 6); }],
 ['dzungla', 'Dżungla', (x, y, t) => { const sw = Math.round(Math.sin(t * 1.5 + y * .4) * .7); const h = HSH(x + sw, y, 31); return h > .55 ? hsl(105 + h * 40, 65, 22 + h * 20) : hsl(140, 50, 8 + h * 6); }],
 ['lod', 'Lód', (x, y, t) => { const crack = (x * 3 + y * 2) % 7 === 0 || (x * 2 - y + 40) % 9 === 0; const band = Math.abs(((x + y - t * 8) % 24 + 24) % 24 - 6) < 1.2; return band ? hsl(190, 100, 92) : hsl(195, 70, crack ? 78 : 48 + HSH(x, y, 4) * 10); }],
 ['krew', 'Krew', (x, y, t) => { const d = (t * 2.4 + HSH(x, 0, 41) * 16) % 22; return y < d - 1 ? hsl(355, 80, 24) : y < d ? hsl(355, 90, 45) : hsl(350, 20, 8 + HSH(x, y, 2) * 4); }],
 ['cyber', 'Cyber', (x, y, t) => { const tr = x % 3 === 1 || y % 4 === 2; if (!tr) return hsl(220, 40, 7); const p = ((x + y * 2 - t * 12) % 24 + 24) % 24; return p < 2 ? hsl(180, 100, 75) : hsl(190, 70, 24); }],
 ['ogniki', 'Ogniki', (x, y, t) => { let b = 0; for (let k = 0; k < 5; k++) { const fx = 4.5 + Math.sin(t * .7 + k * 2.1) * 4, fy = 8 + Math.sin(t * .5 + k * 1.3) * 7; const d = Math.hypot(x - fx, y - fy); b = Math.max(b, (1 - d / 1.6) * (Math.sin(t * 3 + k) * .4 + .6)); } return b > .2 ? hsl(58, 100, 50 + b * 30) : hsl(170, 45, 8 + y * .4); }],
 ['retro', 'Retro', (x, y, t) => { if (y < 9) { const d = Math.hypot(x - 4.5, y - 8); if (d < 4.6 && !(y > 5 && y % 2 === 0)) return hsl(30 + y * 4, 100, 60); return hsl(300 - y * 6, 70, 18 + y * 3); } const gy = ((y - 9) * 2 + t * 6) % 4 < 1, gx = (x * 2 - 9) % 4 === 0; return gy || gx ? hsl(310, 100, 60) : hsl(270, 60, 10); }],
 ['wodospad', 'Wodospad', (x, y, t) => { const sp = ((y - t * 14 + HSH(x, 0, 51) * 16) % 16 + 16) % 16; return sp < 2 ? hsl(195, 80, 92) : hsl(200 + x * 2, 75, 30 + sp * 1.6); }],
 ['portal', 'Portal', (x, y, t) => { const dx = x - 4.5, dy = y - 7.5, a = Math.atan2(dy, dx), r = Math.hypot(dx, dy); const v = Math.sin(a * 2 - r * 1.2 + t * 4); const sp = HSH(x, y, Math.floor(t * 5)) > .93; return sp ? hsl(290, 100, 85) : hsl(275 + v * 15, 85, 22 + v * 14 + Math.max(0, 6 - r) * 3); }],
 ['ksiezyc', 'Księżycowa noc', (x, y, t) => { const d = Math.hypot(x - 6, y - 4); if (d < 2.6) return hsl(50, 30, d < 1.2 ? 95 : 82); const st = HSH(x, y, 61) > .88 && Math.sin(t * 3 + x * y) > 0; return st ? '#fff' : y > 12 ? hsl(250, 30, 8) : hsl(235, 55, 14 + y * .6); }],
 ['siatka', 'Neonowa siatka', (x, y, t) => { if (y < 6) return hsl(300 - y * 10, 80, 20 + y * 6); const gy = ((y - 6) * 2 - t * 5) % 4; return (gy < 1 && gy > -1) || x % 3 === 0 ? hsl(310, 100, 62) : hsl(265, 70, 9); }],
 ['kamuflaz', 'Kamuflaż', (x, y, t) => { const v = Math.sin(x * .9 + Math.sin(y * .5 + t * .4)) + Math.sin(y * .7 - t * .3 + x * .3); return v > .8 ? hsl(85, 30, 40) : v > 0 ? hsl(95, 35, 26) : v > -.8 ? hsl(35, 30, 28) : hsl(80, 20, 14); }],
 ['serca', 'Serduszka', (x, y, t) => { const yy = ((y + t * 3) % 8 + 8) % 8, xx = (x + Math.floor((y + t * 3) / 8) * 3) % 5; const H2 = [[0, 1, 0, 1, 0], [1, 1, 1, 1, 1], [0, 1, 1, 1, 0], [0, 0, 1, 0, 0]]; return yy < 4 && H2[Math.floor(yy)][xx] ? hsl(340, 90, 62) : hsl(330, 60, 85); }],
 ['pioruny', 'Pioruny', (x, y, t) => { const k = Math.floor(t * 2.5); const x0 = Math.floor(HSH(k, 0, 71) * 8) + 1 + (y % 4 < 2 ? 0 : 1) - (y > 8 ? 1 : 0); const on = (t * 2.5) % 1 < .35; return on && Math.abs(x - x0) < 1 ? hsl(55, 100, 70) : hsl(225, 60, 12 + (on ? 8 : 0)); }],
 ['szachownica', 'Szachownica', (x, y, t) => { const c = ((x >> 1) + (y >> 1)) % 2; const p = Math.sin(t * 3 - (x + y) * .4) * .5 + .5; return c ? hsl(270, 80, 20 + p * 35) : hsl(270, 30, 6); }],
 ['wulkan', 'Wulkan', (x, y, t) => { const cone = y > 6 && Math.abs(x - 4.5) < (y - 5) * .55; if (cone) return Math.abs(x - 4.5) < .9 && HSH(x, Math.floor(y - t * 6), 3) > .3 ? hsl(25, 100, 55) : hsl(15, 20, 14 + y); const lava = y < 7 && HSH(x, Math.floor(y + t * 5), 81) > .82; return lava ? hsl(35, 100, 60) : hsl(355, 50, 10 + (6 - Math.min(6, y)) * 2); }],
 ['oko', 'Oko', (x, y, t) => { const bl = (t % 4) < .15; const d = Math.hypot((x - 4.5) * 1.2, y - 7.5); if (bl) return Math.abs(y - 7.5) < .8 ? '#1f1033' : hsl(270, 40, 10); if (d < 1.4) return '#0a0a0a'; if (d < 3) return hsl(160 + Math.sin(t) * 20, 80, 45); if (d < 4.6) return hsl(0, 0, 92); return hsl(270, 40, 10 + HSH(x, y, 9) * 4); }],
 ['deszcz', 'Deszcz', (x, y, t) => { const d = ((y - t * 18 + HSH(x, 0, 91) * 16) % 16 + 16) % 16; return d < 1.5 ? hsl(205, 70, 80) : hsl(215, 30, 16 + Math.sin(x * .7 + t) * 3); }],
 ['zachod2', 'Zachód słońca', (x, y, t) => { if (y > 10) return hsl(270, 30, 10 + Math.sin(x + t * 2) * 3 * (HSH(x, y, 3) > .7)); const d = Math.hypot(x - 4.5, y - 9); if (d < 3.2) return hsl(45, 100, 70); return hsl(20 + y * 22 - t * 0, 85, 55 - y * 2.5); }],
];

/* ---------------- tekstury pomocnicze ---------------- */
const canvas = (w, h) => { const c = document.createElement('canvas'); c.width = w; c.height = h; return c; };
const nearest = tex => { tex.magFilter = T.NearestFilter; tex.minFilter = T.NearestFilter; return tex; };
const noiseTex = (w, h, base, spread, deco) => {
  const c = canvas(w, h), g = c.getContext('2d');
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { g.fillStyle = hsl(base[0], base[1], base[2] + (rnd() - .5) * spread); g.fillRect(x, y, 1, 1); }
  if (deco) deco(g, w, h);
  return nearest(new T.CanvasTexture(c));
};
const boxMats = (w, h, d, base, spread, front) => [
  [d, h], [d, h], [w, d], [w, d], [w, h], [w, h]
].map(([a, b], i) => new T.MeshLambertMaterial({ map: noiseTex(a, b, base, spread, i === 4 ? front : null) }));
const box = (w, h, d, mat) => new T.Mesh(new T.BoxGeometry(w, h, d), mat);

/* ---------------- postać ---------------- */
function makePlayer() {
  const root = new T.Group();
  const skin = [28, 45, 62], shirt = [205, 55, 42], pants = [235, 40, 28];
  const head = new T.Group(); head.position.set(0, 24, 0); root.add(head);
  const face = (g) => { g.fillStyle = '#fff'; g.fillRect(1, 4, 2, 1); g.fillRect(5, 4, 2, 1); g.fillStyle = '#3b2a8a'; g.fillRect(2, 4, 1, 1); g.fillRect(5, 4, 1, 1); g.fillStyle = '#8a5a3a'; g.fillRect(3, 6, 2, 1); g.fillStyle = '#4a2f1a'; g.fillRect(0, 0, 8, 2); g.fillRect(0, 2, 1, 1); g.fillRect(7, 2, 1, 1); };
  const hm = boxMats(8, 8, 8, skin, 6, face);
  const hair = noiseTex(8, 8, [25, 45, 18], 6); hm[2].map = hair; hm[5].map = hair;
  const hb = box(8, 8, 8, hm); hb.position.y = 4; head.add(hb);
  const body = box(8, 12, 4, boxMats(8, 12, 4, shirt, 7)); body.position.y = 18; root.add(body);
  const limb = (x, y, base, top) => { const g = new T.Group(); g.position.set(x, y, 0); const m = box(4, 12, 4, boxMats(4, 12, 4, base, 7)); m.position.y = -6; g.add(m); if (top) { const s = box(4.05, 3, 4.05, boxMats(4, 3, 4, skin, 6)); s.position.y = -10.5; g.add(s); } root.add(g); return g; };
  const armR = limb(-6, 24, shirt, true), armL = limb(6, 24, shirt, true);
  const legR = limb(-2, 12, pants), legL = limb(2, 12, pants);
  return { root, head, armR, armL, legR, legL };
}

/* ---------------- peleryna z falowaniem ---------------- */
function makeCape() {
  const c = canvas(10, 16), g = c.getContext('2d'), tex = nearest(new T.CanvasTexture(c));
  const geo = new T.PlaneGeometry(10, 16, 1, 16); geo.translate(0, -8, 0);
  const mat = new T.MeshBasicMaterial({ map: tex, side: T.DoubleSide });
  const mesh = new T.Mesh(geo, mat);
  const pivot = new T.Group(); pivot.position.set(0, 24, -2.4); pivot.add(mesh);
  const base = geo.attributes.position.array.slice();
  return {
    obj: pivot, fn: null,
    update(t, walk) {
      if (!this.fn) return;
      for (let y = 0; y < 16; y++) for (let x = 0; x < 10; x++) { g.fillStyle = this.fn(9 - x, y, t); g.fillRect(x, y, 1, 1); }
      tex.needsUpdate = true;
      pivot.rotation.x = .12 + walk * .55 + Math.sin(t * 1.7) * .05;
      const p = geo.attributes.position.array;
      for (let i = 0; i < p.length; i += 3) { const yy = -base[i + 1] / 16; p[i + 2] = -Math.sin(t * 3.2 + yy * 5 + base[i] * .15) * (.5 + walk) * yy; }
      geo.attributes.position.needsUpdate = true;
    }
  };
}

/* ---------------- skrzydła ---------------- */
function wingCanvas(kind) {
  const S = 128, c = canvas(S, S), g = c.getContext('2d');
  const root = [4, 44];
  const FEATH = { anielskie: [220, 30, 96, 225], zlote: [44, 90, 70, 38], feniks: [28, 100, 60, 8], natury: [110, 55, 52, 85], sniezne: [200, 60, 95, 210], kruka: [250, 15, 30, 260], pawie: [175, 70, 45, 220], rozowe: [330, 85, 82, 300] };
  const MEMB = { demona: [355, 70, 22], smocze: [272, 60, 30], lodowe: [195, 80, 70], cienia: [265, 30, 6], energia: [190, 100, 60], nietoperza: [20, 30, 16] };
  if (FEATH[kind]) {
    const [h, sa, l, h2] = FEATH[kind];
    const layer = (n, len0, len1, w, lo, y0, ang0, ang1) => { for (let i = 0; i < n; i++) { const u = i / (n - 1); const x = 6 + u * 104, y = y0 - u * 34 - Math.sin(u * PI) * 6; const len = len0 + (len1 - len0) * u, a = ang0 + (ang1 - ang0) * u;
      g.save(); g.translate(x, y); g.rotate(a); const gr = g.createLinearGradient(0, 0, 0, len); gr.addColorStop(0, hsl(h, sa, l - lo)); gr.addColorStop(1, hsl(h2, sa, l - lo - 18)); g.fillStyle = gr; g.strokeStyle = hsl(h2, sa * .6, Math.max(20, l - 35)); g.lineWidth = 1.5;
      g.beginPath(); g.moveTo(-w, 0); g.quadraticCurveTo(-w * 1.1, len * .7, 0, len); g.quadraticCurveTo(w * 1.1, len * .7, w, 0); g.closePath(); g.fill(); g.stroke(); g.beginPath(); g.moveTo(0, 2); g.lineTo(0, len - 4); g.stroke(); g.restore(); } };
    layer(11, 46, 74, 7, 8, 58, .35, -.15); layer(10, 30, 44, 7, 4, 48, .3, -.05); layer(9, 16, 24, 7, 0, 40, .2, 0);
    g.fillStyle = hsl(h, sa, Math.min(99, l + 4)); g.beginPath(); g.moveTo(2, 30); g.quadraticCurveTo(60, -4, 118, 4); g.quadraticCurveTo(70, 26, 4, 52); g.closePath(); g.fill();
    g.strokeStyle = hsl(h2, sa * .6, Math.max(20, l - 30)); g.lineWidth = 1.5; g.stroke();
  } else if (kind === 'motyle' || kind === 'wrozki') {
    const lobe = (cx, cy, rx, ry, h) => { const gr = g.createRadialGradient(cx, cy, 2, cx, cy, rx); if (kind === 'wrozki') { gr.addColorStop(0, hsl(h, 90, 92, .85)); gr.addColorStop(.6, hsl(h + 60, 80, 75, .5)); gr.addColorStop(1, hsl(h + 120, 70, 70, .25)); } else { gr.addColorStop(0, hsl(h, 95, 70)); gr.addColorStop(.7, hsl(h + 40, 90, 50)); gr.addColorStop(1, hsl(h + 70, 80, 25)); } g.fillStyle = gr; g.beginPath(); g.ellipse(cx, cy, rx, ry, -.4, 0, PI * 2); g.fill(); g.strokeStyle = kind === 'wrozki' ? 'rgba(255,255,255,.9)' : '#1a1028'; g.lineWidth = kind === 'wrozki' ? 2 : 3; g.stroke(); };
    if (kind === 'wrozki') { lobe(62, 28, 56, 22, 180); lobe(50, 78, 40, 18, 290); g.strokeStyle = 'rgba(255,255,255,.6)'; g.lineWidth = 1; for (let i = 0; i < 6; i++) { g.beginPath(); g.moveTo(6, 44); g.lineTo(40 + i * 14, 12 + i * 12); g.stroke(); } }
    else { lobe(60, 30, 54, 30, 280); lobe(46, 84, 34, 26, 190); g.fillStyle = '#fff'; [[80, 22], [96, 34], [52, 86]].forEach(([x, y]) => { g.beginPath(); g.arc(x, y, 5, 0, PI * 2); g.fill(); }); }
  } else if (kind === 'mechaniczne') {
    for (let i = 0; i < 7; i++) { g.save(); g.translate(6, 44); g.rotate(-.55 + i * .2); const len = 70 + i * 6 - Math.abs(i - 3) * 8; const gr = g.createLinearGradient(0, 0, len, 0); gr.addColorStop(0, '#6b7280'); gr.addColorStop(.6, '#cbd5e1'); gr.addColorStop(1, '#94a3b8'); g.fillStyle = gr; g.fillRect(0, -6, len, 12); g.strokeStyle = '#1f2937'; g.lineWidth = 2; g.strokeRect(0, -6, len, 12); g.fillStyle = '#b45309'; for (let k = 14; k < len; k += 16) { g.beginPath(); g.arc(k, 0, 2.2, 0, PI * 2); g.fill(); } g.restore(); }
    g.fillStyle = '#b45309'; g.beginPath(); g.arc(8, 44, 9, 0, PI * 2); g.fill(); g.strokeStyle = '#78350f'; g.lineWidth = 3; g.stroke();
  } else if (kind === 'kosciane') {
    const tips = [[120, 10], [108, 70], [80, 108], [44, 118]];
    g.lineCap = 'round'; g.strokeStyle = '#e7e5e4'; g.lineWidth = 6; tips.forEach(([x, y]) => { g.beginPath(); g.moveTo(root[0], root[1]); g.lineTo(x, y); g.stroke(); });
    g.strokeStyle = '#a8a29e'; g.lineWidth = 2; tips.forEach(([x, y]) => { g.beginPath(); g.moveTo(root[0], root[1]); g.lineTo(x, y); g.stroke(); });
    g.fillStyle = '#f5f5f4'; tips.forEach(([x, y]) => { g.beginPath(); g.arc(x, y, 4, 0, PI * 2); g.fill(); });
    g.strokeStyle = 'rgba(80,20,30,.6)'; g.lineWidth = 1.5; g.beginPath(); g.moveTo(tips[0][0], tips[0][1]); for (let i = 1; i < 4; i++) g.quadraticCurveTo((tips[i - 1][0] + tips[i][0]) / 2 - 18, (tips[i - 1][1] + tips[i][1]) / 2 - 10, tips[i][0], tips[i][1]); g.stroke();
  } else {
    const tips = [[120, 10], [108, 70], [80, 108], [44, 118]];
    g.beginPath(); g.moveTo(root[0], root[1] - 22);
    g.lineTo(tips[0][0], tips[0][1]);
    for (let i = 1; i < tips.length; i++) { const [px, py] = tips[i - 1], [qx, qy] = tips[i]; g.quadraticCurveTo((px + qx) / 2 - 14, (py + qy) / 2 - 6, qx, qy); }
    g.quadraticCurveTo(20, 90, root[0], root[1] + 14); g.closePath();
    const col = MEMB[kind] || MEMB.smocze;
    if (kind === 'energia') { g.strokeStyle = hsl(190, 100, 65); g.lineWidth = 4; g.shadowColor = hsl(190, 100, 60); g.shadowBlur = 12; g.stroke(); g.fillStyle = hsl(200, 100, 60, .18); g.fill(); }
    else if (kind === 'lodowe') { const gr = g.createLinearGradient(0, 0, 128, 128); gr.addColorStop(0, hsl(190, 90, 92, .95)); gr.addColorStop(1, hsl(205, 80, 60, .8)); g.fillStyle = gr; g.fill(); g.strokeStyle = '#fff'; g.lineWidth = 1; for (let i = 0; i < 18; i++) { const x = 10 + rnd() * 100, y = 10 + rnd() * 100; g.beginPath(); g.moveTo(x, y); g.lineTo(x + 6, y + 10); g.lineTo(x - 2, y + 14); g.stroke(); } }
    else if (kind === 'cienia') { const gr = g.createRadialGradient(4, 44, 4, 4, 44, 130); gr.addColorStop(0, '#0b0710'); gr.addColorStop(.75, '#1a0f2e'); gr.addColorStop(1, '#7c3aed'); g.fillStyle = gr; g.fill(); g.strokeStyle = '#a855f7'; g.lineWidth = 2; g.shadowColor = '#a855f7'; g.shadowBlur = 10; g.stroke(); g.shadowBlur = 0; }
    else { const gr = g.createLinearGradient(0, 0, 128, 128); gr.addColorStop(0, hsl(col[0], col[1], col[2] + 18)); gr.addColorStop(1, hsl(col[0], col[1], col[2] - 10)); g.fillStyle = gr; g.fill(); }
    g.strokeStyle = kind === 'energia' ? hsl(180, 100, 80) : kind === 'demona' ? '#1a0606' : kind === 'lodowe' ? '#e0f2fe' : kind === 'cienia' ? '#4c1d95' : '#e9d5ff'; g.lineWidth = kind === 'energia' ? 2 : 3;
    tips.forEach(([x, y]) => { g.beginPath(); g.moveTo(root[0], root[1]); g.lineTo(x, y); g.stroke(); });
  }
  return c;
}
const ANIMW = { ogniste: 1, galaktyczne: 1, cyber: 1, teczowe: 1, plazmowe: 1, lawowe: 1, zorzowe: 1, glitch: 1 };
function membranePath(g) {
  const tips = [[120, 10], [108, 70], [80, 108], [44, 118]];
  g.beginPath(); g.moveTo(4, 22); g.lineTo(tips[0][0], tips[0][1]);
  for (let i = 1; i < tips.length; i++) { const [px, py] = tips[i - 1], [qx, qy] = tips[i]; g.quadraticCurveTo((px + qx) / 2 - 14, (py + qy) / 2 - 6, qx, qy); }
  g.quadraticCurveTo(20, 90, 4, 58); g.closePath(); return tips;
}
function animWing(kind, c, t) {
  const g = c.getContext('2d'); g.clearRect(0, 0, 128, 128); g.save(); const tips = membranePath(g); g.clip();
  if (kind === 'ogniste') { for (let y = 0; y < 128; y += 4) for (let x = 0; x < 128; x += 4) { const v = Math.sin(x * .08 + t * 3) + Math.sin(y * .1 - t * 5) + Math.sin((x + y) * .05 + t * 2); g.fillStyle = v > 1.2 ? '#fde047' : v > .3 ? '#fb923c' : v > -.6 ? '#dc2626' : '#450a0a'; g.fillRect(x, y, 4, 4); } }
  else if (kind === 'galaktyczne') { const gr = g.createRadialGradient(4, 44, 4, 30, 50, 140); gr.addColorStop(0, '#1e1b4b'); gr.addColorStop(.5, '#4c1d95'); gr.addColorStop(1, '#0c0a1d'); g.fillStyle = gr; g.fillRect(0, 0, 128, 128); for (let i = 0; i < 60; i++) { const x = (HSH(i, 1, 3) * 128), y = HSH(i, 2, 3) * 128, b = Math.sin(t * 3 + i) * .5 + .5; g.fillStyle = `rgba(255,255,255,${b})`; g.fillRect(x, y, 2, 2); } }
  else if (kind === 'cyber') { g.fillStyle = 'rgba(6,20,30,.85)'; g.fillRect(0, 0, 128, 128); g.strokeStyle = 'rgba(34,211,238,.35)'; g.lineWidth = 1; for (let k = 0; k < 128; k += 12) { g.beginPath(); g.moveTo(k, 0); g.lineTo(k, 128); g.moveTo(0, k); g.lineTo(128, k); g.stroke(); } const p = (t * 60) % 160; g.fillStyle = '#67e8f9'; for (let k = 0; k < 128; k += 24) { g.fillRect(p - 20, k, 6, 2); g.fillRect(k, p - 40, 2, 6); } }
  else if (kind === 'plazmowe') { for (let y = 0; y < 128; y += 4) for (let x = 0; x < 128; x += 4) { const v = Math.sin(x * .06 + t) + Math.sin(y * .05 + t * 1.3) + Math.sin(Math.hypot(x - 40, y - 60) * .08 - t * 2); g.fillStyle = hsl(280 + v * 40, 90, 45 + v * 8); g.fillRect(x, y, 4, 4); } }
  else if (kind === 'lawowe') { for (let y = 0; y < 128; y += 4) for (let x = 0; x < 128; x += 4) { const v = Math.sin(x * .07 + t * .8) + Math.sin(y * .06 - t * 1.1) + Math.sin((x + y) * .04 + t * .6); g.fillStyle = v > 1.3 ? '#fef08a' : v > .5 ? '#f97316' : v > -.4 ? '#b91c1c' : '#1c0a0a'; g.fillRect(x, y, 4, 4); } }
  else if (kind === 'zorzowe') { g.fillStyle = '#04111c'; g.fillRect(0, 0, 128, 128); for (let x = 0; x < 128; x += 2) { const y0 = 40 + Math.sin(x * .05 + t * 1.4) * 18; const gr = g.createLinearGradient(0, y0 - 30, 0, y0 + 50); gr.addColorStop(0, 'rgba(0,0,0,0)'); gr.addColorStop(.5, hsl(150 + Math.sin(x * .03 + t) * 40, 90, 55)); gr.addColorStop(1, 'rgba(0,0,0,0)'); g.fillStyle = gr; g.fillRect(x, y0 - 30, 2, 80); } }
  else if (kind === 'glitch') { const r = Math.floor(t * 8); g.fillStyle = '#0b0b14'; g.fillRect(0, 0, 128, 128); for (let y = 0; y < 128; y += 4) { const sh = HSH(0, y, r) > .8 ? (HSH(1, y, r) - .5) * 30 : 0; for (let x = 0; x < 128; x += 4) { const v = HSH(x >> 3, y >> 3, 7); if (v > .7) { g.fillStyle = v > .9 ? '#f0abfc' : '#22d3ee'; g.fillRect(x + sh, y, 4, 4); } } } }
  else { for (let y = 0; y < 128; y += 2) { g.fillStyle = hsl((y * 3 - t * 120) % 360, 90, 60); g.fillRect(0, y, 128, 2); } }
  g.restore();
  g.strokeStyle = kind === 'cyber' || kind === 'glitch' ? '#22d3ee' : kind === 'ogniste' || kind === 'lawowe' ? '#fde047' : kind === 'teczowe' ? '#fff' : kind === 'zorzowe' ? '#6ee7b7' : '#c4b5fd'; g.lineWidth = 3; if (kind === 'cyber') { g.shadowColor = '#22d3ee'; g.shadowBlur = 10; }
  membranePath(g); g.stroke(); tips.forEach(([x, y]) => { g.beginPath(); g.moveTo(4, 44); g.lineTo(x, y); g.stroke(); }); g.shadowBlur = 0;
}
const W3D = { pikselowe: 'vox', diamentowe: 'vox', zlotepix: 'vox', snieznepix: 'vox', krysztalowe: 'cry', ametystowe: 'cry', szmaragdowe: 'cry', rubinowe: 'cry', ostrza: 'bla', ostrzalod: 'bla', szkielet: 'bone', zebatki: 'gear', kule: 'orb' };
const VOXPAL = { pikselowe: [0x7c3aed, 0x8b5cf6, 0xa78bfa, 0xc4b5fd, 0x6d28d9], diamentowe: [0x0891b2, 0x22d3ee, 0x67e8f9, 0xa5f3fc, 0x0e7490], zlotepix: [0xb45309, 0xf59e0b, 0xfcd34d, 0xfde68a, 0xd97706], snieznepix: [0xe0f2fe, 0xf8fafc, 0xbae6fd, 0xffffff, 0x7dd3fc] };
const CRYPAL = { krysztalowe: [0x67e8f9, 0x0e7490, 0xa5f3fc], ametystowe: [0xc084fc, 0x6b21a8, 0xe9d5ff], szmaragdowe: [0x34d399, 0x065f46, 0xa7f3d0], rubinowe: [0xbe123c, 0x4c0519, 0xfb7185] };
function makeVoxelWings(kind) {
  const grp = new T.Group(); grp.position.set(0, 21, -2.6);
  const type = W3D[kind], crystal = type === 'cry';
  const pal = VOXPAL[kind] || VOXPAL.pikselowe;
  const cc = CRYPAL[kind] || CRYPAL.krysztalowe;
  const spin = [];
  const wings = [1, -1].map(side => { const p = new T.Group(); p.scale.x = side; p.position.x = side * 1.2; grp.add(p);
    if (type === 'bone') {
      const bm = lam(0xf5f5f4), dm = lam(0xa8a29e);
      const arm = box(22, 1.6, 1.6, bm); arm.position.set(11, 11, 0); arm.rotation.z = .37; p.add(arm);
      [[21, 13, -1.25, 9], [16, 11, -1.6, 12], [10, 9, -1.75, 11], [5, 7, -1.9, 8]].forEach(([x, y, a, l]) => { const g2 = new T.Group(); g2.position.set(x, y + 2, 0); g2.rotation.z = a; const b = box(l, 1.1, 1.1, bm); b.position.x = l / 2; const j = box(1.8, 1.8, 1.8, dm); g2.add(b, j); p.add(g2); });
      const sh = new T.Shape(); sh.moveTo(1, 7); sh.lineTo(22, 15); sh.quadraticCurveTo(19, 8, 26, 5); sh.quadraticCurveTo(18, 3, 22, -3); sh.quadraticCurveTo(14, 0, 14, -6); sh.quadraticCurveTo(8, 0, 4, -2); sh.lineTo(1, 4);
      const memb = new T.Mesh(new T.ShapeGeometry(sh), new T.MeshBasicMaterial({ color: 0x7f1d1d, transparent: true, opacity: .45, side: T.DoubleSide })); memb.position.z = -.4; p.add(memb);
    } else if (type === 'gear') {
      const met = lam(0x9ca3af, 0x111111), cop = lam(0xb45309, 0x1c0a00);
      for (let i = 0; i < 5; i++) { const pl = box(16 - i * 1.5, 2.4, .8, met); pl.position.set(9 - i * .4, 8 - i * 3.4, 0); pl.rotation.z = .3 - i * .17; p.add(pl); }
      [[4, 6, 3], [10, 2, 2.2], [15, 7, 2.6]].forEach(([x, y, r]) => { const gr = new T.Group(); gr.position.set(x, y, .9); const c = new T.Mesh(new T.CylinderGeometry(r, r, 1, 10), cop); c.rotation.x = Math.PI / 2; gr.add(c); for (let k = 0; k < 8; k++) { const tth = box(1, 1, 1, cop); const a = k / 8 * Math.PI * 2; tth.position.set(Math.cos(a) * (r + .5), Math.sin(a) * (r + .5), 0); gr.add(tth); } p.add(gr); spin.push(gr); });
    } else if (type === 'orb') {
      const gm2 = new T.MeshBasicMaterial({ color: 0xa78bfa }); const gl2 = new T.MeshBasicMaterial({ color: 0x8b5cf6, transparent: true, opacity: .3, blending: T.AdditiveBlending, depthWrite: false });
      for (let i = 0; i < 14; i++) { const u = i / 13; const x = 2 + u * 22, y = 9 + Math.sin(u * Math.PI) * 4 - u * 4; const r = 1.6 - u * .6; const o = new T.Mesh(new T.SphereGeometry(r, 12, 10), gm2); o.position.set(x, y, 0); const gw = new T.Mesh(new T.SphereGeometry(r * 2, 12, 10), gl2); gw.position.copy(o.position); p.add(o, gw); spin.push(o);
        if (i % 2 === 0) for (let k = 1; k < 4; k++) { const d = new T.Mesh(new T.SphereGeometry(r * (1 - k * .2), 10, 8), gm2); d.position.set(x - k * .6, y - k * 3.2, 0); p.add(d); } }
    } else if (type === 'bla') {
      const lod = kind === 'ostrzalod';
      const bm = new T.MeshBasicMaterial({ color: lod ? 0xcffafe : 0xf0abfc }); const gm = new T.MeshBasicMaterial({ color: lod ? 0x22d3ee : 0xd946ef, transparent: true, opacity: .4, blending: T.AdditiveBlending, depthWrite: false });
      for (let i = 0; i < 6; i++) { const len = 12 + i * 2.6 - Math.max(0, i - 3) * 4; const a = .7 - i * .26; const b = new T.Group(); b.rotation.z = a; const blade = box(len, .9, .5, bm); blade.position.x = len / 2 + 1; const glw = box(len + 1, 2.4, 1.2, gm); glw.position.x = len / 2 + 1; b.add(blade, glw); p.add(b); }
    } else if (crystal) {
      const mat = new T.MeshLambertMaterial({ color: cc[0], emissive: cc[1], transparent: true, opacity: .85 });
      const glow = new T.MeshBasicMaterial({ color: cc[2], transparent: true, opacity: .35, blending: T.AdditiveBlending, depthWrite: false });
      [[4, 6, 2.2], [9, 9, 2.8], [14, 11, 3], [19, 12, 2.6], [23, 11, 2], [7, 0, 2.4], [12, 2, 2.8], [17, 3, 2.2], [10, -6, 2], [15, -5, 1.8]].forEach(([x, y, r], i) => { const o = new T.Mesh(new T.OctahedronGeometry(r), mat); o.position.set(x, y, 0); o.scale.y = 1.8; o.rotation.z = -.5 + i * .07; p.add(o); const gl = new T.Mesh(new T.OctahedronGeometry(r * 1.35), glow); gl.position.copy(o.position); gl.scale.y = 1.8; gl.rotation.z = o.rotation.z; p.add(gl); });
    } else {
      const rows = [[1, 5, 9], [2, 4, 11], [3, 2, 13], [4, 0, 14], [5, -2, 13], [6, -4, 11], [7, -6, 8], [8, -8, 5]];
      rows.forEach(([r, y, len], i) => { for (let x = 0; x < len; x++) { if (HSH(x, i, 5) > .93) continue; const b = box(1.9, 1.9, 1.2, lam(pal[(x + i) % pal.length], x === len - 1 ? 0x2e1065 : 0)); b.position.set(1 + x * 2, y + x * .55, 0); p.add(b); } });
    }
    return p; });
  return { obj: grp, update(t) { const f = Math.sin(t * 2.2); wings.forEach((w, i) => { w.rotation.y = (i ? -1 : 1) * (.5 + f * .3); w.rotation.z = (i ? -1 : 1) * .1; }); spin.forEach((o, i) => { if (type === 'gear') o.rotation.z = t * (i % 2 ? -1.5 : 1.5); else o.scale.setScalar(1 + Math.sin(t * 3 + i * .6) * .15); }); } };
}
// małe skrzydełka: te same wzory, ale o połowę mniejsze i machają szybciej
const MINIW = { m_anielskie: ['anielskie', .5], m_demona: ['demona', .48], m_nietoperza: ['nietoperza', .45], m_smocze: ['smocze', .5], m_feniks: ['feniks', .5], m_kruka: ['kruka', .48], m_rozowe: ['rozowe', .48], m_ogniste: ['ogniste', .48], m_galaktyczne: ['galaktyczne', .5], m_cienia: ['cienia', .5], m_lodowe: ['lodowe', .5], m_energia: ['energia', .5], m_motyle: ['motyle', .55, 1.1], m_wrozki: ['wrozki', .55, 1.1], m_krysztalowe: ['krysztalowe', .5], m_pikselowe: ['pikselowe', .5] };
function makeWings(kind) {
  const mini = MINIW[kind];
  if (mini) {
    const o = makeWings(mini[0]), sp = mini[2] || 1.7;
    o.obj.scale.setScalar(mini[1]); o.obj.position.y += 1; o.obj.position.z += .4;
    const up = o.update; o.update = t => up(t * sp);
    return o;
  }
  if (W3D[kind]) return makeVoxelWings(kind);
  const grp = new T.Group(); grp.position.set(0, 21, -2.6);
  const wc = ANIMW[kind] ? canvas(128, 128) : wingCanvas(kind); if (ANIMW[kind]) animWing(kind, wc, 0);
  const tex = new T.CanvasTexture(wc);
  const add = kind === 'energia';
  const solid = !['energia', 'motyle', 'wrozki', 'kosciane', 'lodowe', 'cyber', 'galaktyczne'].includes(kind);
  const mat = new T.MeshBasicMaterial({ map: tex, transparent: true, alphaTest: add || kind === 'wrozki' || kind === 'lodowe' ? .02 : .3, side: T.DoubleSide, blending: add ? T.AdditiveBlending : T.NormalBlending, depthWrite: !add });
  const geo = new T.PlaneGeometry(26, 26); geo.translate(13 - 26 * 4 / 128, -(13 - 26 * 44 / 128), 0);
  const wings = [1, -1].map(side => { const p = new T.Group(); const m = new T.Mesh(geo, mat); p.add(m); p.scale.x = side; p.position.x = side * 1.2; grp.add(p);
    if (solid) { const back = new T.Mesh(geo, mat.clone()); back.material.color = new T.Color(.55, .55, .6); back.scale.set(.82, .82, 1); back.position.z = -.6; p.add(back); }
    return p; });
  return { obj: grp, update(t) { const f = Math.sin(t * (kind === 'motyle' || kind === 'wrozki' ? 6 : kind === 'mechaniczne' ? 1.6 : 2.4)); wings.forEach((w, i) => { w.rotation.y = (i ? -1 : 1) * (.55 + f * .35); w.rotation.z = (i ? -1 : 1) * .12; }); if (kind === 'motyle' || kind === 'wrozki') mat.color.setHSL((t * .08) % 1, .6, .8); if (kind === 'feniks') mat.color.setHSL(.05 + Math.sin(t * 3) * .03, 1, .6 + Math.sin(t * 5) * .08); if (add) mat.opacity = .75 + Math.sin(t * 4) * .25; if (ANIMW[kind]) { animWing(kind, wc, t); tex.needsUpdate = true; } } };
}

/* ---------------- czapki ---------------- */
const lam = (c, e) => new T.MeshLambertMaterial({ color: c, emissive: e || 0x000000 });
const bas = (c, o) => new T.MeshBasicMaterial({ color: c, transparent: o !== undefined, opacity: o === undefined ? 1 : o });
function makeHat(kind) {
  const g = new T.Group(); g.position.set(0, 32, 0); let up = () => {};
  if (kind === 'korona') {
    const gold = lam(0xf5c542, 0x3a2600);
    [[0, 4.4, 9, .9], [0, -4.4, 9, .9], [4.4, 0, .9, 9], [-4.4, 0, .9, 9]].forEach(([x, z, w, d]) => { const b = box(w, 2, d, gold); b.position.set(x, 1, z); g.add(b); });
    [[4.4,4.4],[-4.4,4.4],[4.4,-4.4],[-4.4,-4.4],[0,4.4],[0,-4.4],[4.4,0],[-4.4,0]].forEach(([x,z],i)=>{ const sp = box(1.4, i<4?3.2:2.4, 1.4, gold); sp.position.set(x, i<4?3.4:3, z); g.add(sp); });
    [0xe11d48, 0x2563eb, 0x16a34a, 0xa855f7].forEach((c, i) => { const j = box(1.2, 1.2, .6, bas(c)); const a = i * PI / 2; j.position.set(Math.sin(a) * 4.9, 1, Math.cos(a) * 4.9); j.rotation.y = a; g.add(j); });
  } else if (kind === 'aureola') {
    const ring = new T.Mesh(new T.TorusGeometry(4.6, .55, 8, 40), bas(0xfde68a)); ring.rotation.x = PI / 2; g.add(ring);
    const glow = new T.Mesh(new T.TorusGeometry(4.6, 1.6, 8, 40), new T.MeshBasicMaterial({ color: 0xfacc15, transparent: true, opacity: .25, blending: T.AdditiveBlending, depthWrite: false })); glow.rotation.x = PI / 2; g.add(glow);
    up = t => { g.position.y = 11 + Math.sin(t * 2) * .6; g.rotation.y = t * .6; };
  } else if (kind === 'rogi') {
    [1, -1].forEach(s => { let p = new T.Group(); p.position.set(s * 3, 0, 0); g.add(p); for (let i = 0; i < 4; i++) { const b = box(2.4 - i * .45, 2.2, 2.4 - i * .45, lam(i < 2 ? 0x7f1d1d : 0x1c0a0a)); b.position.y = 1.1; p.add(b); const n = new T.Group(); n.position.y = 2; n.rotation.z = -s * .35; n.rotation.x = -.15; p.add(n); p = n; } });
  } else if (kind === 'kot') {
    [1, -1].forEach(s => { const e = new T.Group(); e.position.set(s * 2.6, 0, .5); e.rotation.z = -s * .25; const o = box(2.6, 3, 1, lam(0x2b2b33)); o.position.y = 1.5; const i = box(1.4, 1.8, .2, lam(0xf9a8d4)); i.position.set(0, 1.3, .55); e.add(o, i); g.add(e); });
  } else if (kind === 'czarodziej') {
    const c = canvas(32, 32), x = c.getContext('2d'); x.fillStyle = '#3b1d8f'; x.fillRect(0, 0, 32, 32); x.fillStyle = '#fde68a'; for (let i = 0; i < 14; i++) x.fillRect(rnd() * 32 | 0, rnd() * 32 | 0, 1, 1);
    const m = new T.MeshLambertMaterial({ map: nearest(new T.CanvasTexture(c)) });
    const brim = new T.Mesh(new T.CylinderGeometry(7, 7, .6, 8), m); brim.position.y = .3; g.add(brim);
    const cone = new T.Mesh(new T.ConeGeometry(4.6, 11, 8), m); cone.position.set(0, 5.8, -.6); cone.rotation.x = -.18; g.add(cone);
    const band = new T.Mesh(new T.CylinderGeometry(4.7, 4.7, 1, 8), lam(0xfacc15)); band.position.y = 1.1; g.add(band);
  } else if (kind === 'sluchawki') {
    const band = new T.Mesh(new T.TorusGeometry(5, .6, 6, 24, PI), lam(0x18181b)); band.position.y = -4; g.add(band);
    const leds = [];
    [1, -1].forEach(s => { const cup = box(1.6, 4, 4, lam(0x27272a)); cup.position.set(s * 4.9, -4, 0); g.add(cup); const l = box(.4, 3, 3, bas(0xa855f7)); l.position.set(s * 5.8, -4, 0); g.add(l); leds.push(l); });
    up = t => leds.forEach(l => l.material.color.setHSL((t * .15) % 1, .9, .6));
  } else if (kind === 'cylinder') {
    const blk = lam(0x111114); const b = new T.Mesh(new T.CylinderGeometry(6.5, 6.5, .6, 24), blk); b.position.y = .3; g.add(b);
    const top = new T.Mesh(new T.CylinderGeometry(4.2, 4.2, 7, 24), blk); top.position.y = 4; g.add(top);
    const band = new T.Mesh(new T.CylinderGeometry(4.3, 4.3, 1.2, 24), lam(0xa855f7)); band.position.y = 1.4; g.add(band);
  } else if (kind === 'mikolaj') {
    const red = lam(0xdc2626), wht = lam(0xf8fafc);
    const brim = box(9.4, 1.8, 9.4, wht); brim.position.y = .9; g.add(brim);
    let p = g; [[8, 3], [6.4, 3], [4.8, 2.6], [3.2, 2.4]].forEach(([w, hh], i) => { const n = new T.Group(); n.position.y = i ? 2.4 : 1.8; n.rotation.x = i ? -.28 : 0; p.add(n); const b = box(w, hh, w, red); b.position.y = hh / 2; n.add(b); p = n; });
    const pom = box(2.2, 2.2, 2.2, wht); pom.position.y = 3; p.add(pom);
  } else if (kind === 'lis') {
    [1, -1].forEach(s => { const e = new T.Group(); e.position.set(s * 2.8, 0, .2); e.rotation.z = -s * .2; const o = box(2.8, 3.6, 1, lam(0xea580c)); o.position.y = 1.8; const tip = box(2.8, 1, 1.02, lam(0x1c1917)); tip.position.y = 3.3; const i = box(1.4, 2, .2, lam(0xfef3c7)); i.position.set(0, 1.4, .55); e.add(o, tip, i); g.add(e); });
  } else if (kind === 'okulary') {
    const fr = lam(0x0a0a0a); const sheen = bas(0x7dd3fc, .0);
    [-2, 2].forEach(x => { const l = box(3.2, 1.6, .5, fr); l.position.set(x, -3.6, 4.3); g.add(l); });
    const br = box(8.6, .6, .5, fr); br.position.set(0, -3.1, 4.3); g.add(br);
    [1, -1].forEach(s => { const arm = box(.4, .5, 8.2, fr); arm.position.set(s * 4.3, -3.1, .2); g.add(arm); });
    const sh = box(1, 1.2, .1, new T.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: .8 })); sh.position.set(-3, -3.6, 4.6); g.add(sh);
    up = t => { const u = (t * .6) % 1; sh.position.x = -3.4 + u * 7; sh.material.opacity = Math.sin(u * PI) * .8; };
  } else if (kind === 'wianek') {
    const cols = [0xf472b6, 0xfde047, 0xffffff, 0xa78bfa, 0xfb7185];
    for (let i = 0; i < 16; i++) { const a = i / 16 * PI * 2, r = 4.8; const f = box(1.6, 1.6, 1.6, i % 2 ? lam(0x16a34a) : bas(cols[(i / 2 | 0) % cols.length])); f.position.set(Math.sin(a) * r, .4 + (i % 2) * .3, Math.cos(a) * r); f.rotation.y = a + .4; g.add(f); }
  } else if (kind === 'wiking') {
    const met = lam(0x9ca3af, 0x111111); const helm = box(9, 3.4, 9, met); helm.position.y = .2; g.add(helm);
    const top = box(6.4, 1.6, 6.4, met); top.position.y = 2.6; g.add(top);
    const band = box(9.2, .8, 9.2, lam(0xb45309)); band.position.y = -1.2; g.add(band);
    [1, -1].forEach(s => { let p = new T.Group(); p.position.set(s * 4.6, 0, 0); p.rotation.z = -s * 1.2; g.add(p); for (let i = 0; i < 4; i++) { const b = box(1.8 - i * .3, 2, 1.8 - i * .3, lam(0xf5f5f4)); b.position.y = 1; p.add(b); const n = new T.Group(); n.position.y = 1.9; n.rotation.z = s * .45; p.add(n); p = n; } });
  } else if (kind === 'dynia') {
    const c = canvas(9, 9), x = c.getContext('2d'); x.fillStyle = '#ea580c'; x.fillRect(0, 0, 9, 9); x.fillStyle = '#c2410c'; for (let i = 1; i < 9; i += 3) x.fillRect(i, 0, 1, 9);
    const side = new T.MeshLambertMaterial({ map: nearest(new T.CanvasTexture(c)) });
    const fc = canvas(9, 9), fx = fc.getContext('2d'); fx.drawImage(c, 0, 0); fx.fillStyle = '#fde047'; fx.fillRect(1, 2, 2, 2); fx.fillRect(6, 2, 2, 2); fx.fillRect(1, 6, 7, 1); fx.fillRect(2, 5, 1, 1); fx.fillRect(4, 5, 1, 1); fx.fillRect(6, 5, 1, 1);
    const face = new T.MeshLambertMaterial({ map: nearest(new T.CanvasTexture(fc)), emissive: 0x3b1300 });
    const pb = box(9.2, 9.2, 9.2, [side, side, side, side, face, side]); pb.position.y = -3.6; g.add(pb);
    const stem = box(1.4, 1.6, 1.4, lam(0x3f6212)); stem.position.y = 1.6; g.add(stem);
    up = t => { face.emissive.setHSL(.08, 1, .12 + Math.max(0, Math.sin(t * 9) * Math.sin(t * 2.3)) * .2); };
  }
  return { obj: g, update: up };
}

/* ---------------- zwierzaki ---------------- */
function faceTex(bg, eye, extra) { const c = canvas(8, 8), x = c.getContext('2d'); x.fillStyle = bg; x.fillRect(0, 0, 8, 8); x.fillStyle = eye; x.fillRect(1, 3, 2, 2); x.fillRect(5, 3, 2, 2); if (extra) extra(x); return nearest(new T.CanvasTexture(c)); }
function makePet(kind) {
  const g = new T.Group(), body = new T.Group(); g.add(body); let up = () => {};
  if (kind === 'duch') {
    const m = new T.MeshLambertMaterial({ color: 0xf1f5f9, transparent: true, opacity: .82, emissive: 0x334155 });
    const fm = m.clone(); fm.map = faceTex('#f1f5f9', '#111', x => { x.fillRect(3, 6, 2, 1); });
    const b = box(5, 5, 5, [m, m, m, m, fm, m]); body.add(b);
    const tails = []; for (let i = 0; i < 4; i++) { const t = box(1.2, 1.6, 4, m); t.position.set(-1.9 + i * 1.25, -3, 0); body.add(t); tails.push(t); }
    up = t => tails.forEach((s, i) => s.position.y = -3 - Math.sin(t * 6 + i) * .4);
  } else if (kind === 'smok') {
    const pm = lam(0x6d28d9, 0x1e0a3c), dm = lam(0x2e1065);
    const b = box(3.4, 3, 6, pm); body.add(b);
    const h = box(3, 3, 3, [pm, pm, pm, pm, new T.MeshLambertMaterial({ map: faceTex('#6d28d9', '#fde047') }), pm]); h.position.set(0, 1.6, 4); body.add(h);
    const tl = box(1.6, 1.6, 4, dm); tl.position.set(0, -.4, -4.6); body.add(tl);
    const wm = new T.MeshBasicMaterial({ color: 0xc4b5fd, side: T.DoubleSide, transparent: true, opacity: .9 });
    const wings = [1, -1].map(s => { const p = new T.Group(); p.position.set(s * 1.7, 1.4, 0); const w = new T.Mesh(new T.PlaneGeometry(6, 4), wm); w.position.x = s * 3; w.rotation.x = -PI / 2; p.add(w); body.add(p); return p; });
    up = t => wings.forEach((w, i) => w.rotation.z = (i ? -1 : 1) * Math.sin(t * 7) * .7);
  } else if (kind === 'slime') {
    const outer = box(5.5, 5.5, 5.5, new T.MeshLambertMaterial({ color: 0x4ade80, transparent: true, opacity: .55 }));
    const core = box(2.6, 2.6, 2.6, new T.MeshLambertMaterial({ map: faceTex('#16a34a', '#052e16') })); core.position.z = .6;
    body.add(outer, core);
    up = t => { const b = Math.abs(Math.sin(t * 3)); body.position.y = b * 3; body.scale.set(1 + (1 - b) * .15, 1 - (1 - b) * .2, 1 + (1 - b) * .15); };
  } else if (kind === 'pszczola') {
    const c = canvas(8, 8), x = c.getContext('2d'); for (let i = 0; i < 8; i++) { x.fillStyle = i % 3 === 2 ? '#1c1917' : '#facc15'; x.fillRect(0, i, 8, 1); }
    const sm = new T.MeshLambertMaterial({ map: nearest(new T.CanvasTexture(c)) }); sm.map.rotation = PI / 2; sm.map.center.set(.5, .5);
    const b = box(4, 4, 5, [sm, sm, sm, sm, new T.MeshLambertMaterial({ map: faceTex('#facc15', '#1c1917') }), sm]); body.add(b);
    const stinger = box(.6, .6, 1.2, lam(0x1c1917)); stinger.position.z = -3; body.add(stinger);
    const wm = new T.MeshBasicMaterial({ color: 0xe0f2fe, transparent: true, opacity: .6, side: T.DoubleSide });
    const ws = [1, -1].map(s => { const w = new T.Mesh(new T.PlaneGeometry(3, 4), wm); w.position.set(s * 1.6, 2.2, -.5); w.rotation.x = -PI / 2; body.add(w); return w; });
    up = t => ws.forEach((w, i) => w.rotation.y = (i ? -1 : 1) * Math.sin(t * 40) * .5);
  } else if (kind === 'aksolotl') {
    const pk = lam(0xf9a8d4, 0x3b0a24);
    const b = box(3, 2.4, 6, pk); body.add(b);
    const h = box(4, 3, 3, [pk, pk, pk, pk, new T.MeshLambertMaterial({ map: faceTex('#f9a8d4', '#1e1b4b', x => { x.fillStyle = '#be185d'; x.fillRect(3, 6, 2, 1); }) }), pk]); h.position.set(0, .4, 4.2); body.add(h);
    [-1, 1].forEach(s => [0, 1, 2].forEach(i => { const gl = box(.6, .6, 1.8, lam(0xdb2777)); gl.position.set(s * (2.3), 1.6 - i * 1, 3.6); gl.rotation.y = s * .6; body.add(gl); }));
    const tail = box(.6, 2.2, 4, lam(0xf472b6)); tail.position.set(0, 0, -4.8); body.add(tail);
    up = t => { tail.rotation.y = Math.sin(t * 4) * .5; };
  }
  else if (kind === 'kot' || kind === 'lisek') {
    const fox = kind === 'lisek', c1 = lam(fox ? 0xea580c : 0x9ca3af), c2 = lam(fox ? 0xfef3c7 : 0x6b7280);
    const b = box(3, 3, 6, c1); body.add(b);
    const h = box(3.6, 3, 3, [c1, c1, c1, c1, new T.MeshLambertMaterial({ map: faceTex(fox ? '#ea580c' : '#9ca3af', fox ? '#1c1917' : '#22c55e', x => { x.fillStyle = fox ? '#fef3c7' : '#f9a8d4'; x.fillRect(3, 6, 2, 2); }) }), c1]); h.position.set(0, 1.2, 4.2); body.add(h);
    [1, -1].forEach(s => { const e = box(1, 1.4, .6, c1); e.position.set(s * 1.2, 3.3, 4.2); body.add(e); });
    const tail = new T.Group(); tail.position.set(0, .8, -3); const tb = box(fox ? 2 : 1, fox ? 2 : 1, 4.4, c1); tb.position.z = -2.2; const tt = box(fox ? 2.05 : 1.05, fox ? 2.05 : 1.05, 1.2, c2); tt.position.z = -4; tail.add(tb, tt); body.add(tail);
    up = t => { tail.rotation.y = Math.sin(t * 3) * .5; tail.rotation.x = .3 + Math.sin(t * 1.5) * .15; h.rotation.y = Math.sin(t * .8) * .25; };
  } else if (kind === 'pingwin') {
    const bk = lam(0x1f2937), wt = lam(0xf8fafc);
    const c = canvas(6, 8), x = c.getContext('2d'); x.fillStyle = '#1f2937'; x.fillRect(0, 0, 6, 8); x.fillStyle = '#f8fafc'; x.fillRect(1, 2, 4, 6); x.fillStyle = '#111'; x.fillRect(1, 1, 1, 1); x.fillRect(4, 1, 1, 1);
    const front = new T.MeshLambertMaterial({ map: nearest(new T.CanvasTexture(c)) });
    const b = box(4, 6, 3.4, [bk, bk, bk, wt, front, bk]); body.add(b);
    const beak = box(1.4, .8, 1.4, lam(0xf59e0b)); beak.position.set(0, 1.6, 2.2); body.add(beak);
    const fl = [1, -1].map(s => { const f = new T.Group(); f.position.set(s * 2.1, 1.6, 0); const m = box(.5, 3.4, 1.6, bk); m.position.y = -1.7; f.add(m); body.add(f); return f; });
    up = t => { fl.forEach((f, i) => f.rotation.z = (i ? -1 : 1) * (.2 + Math.max(0, Math.sin(t * 8)) * .6)); body.rotation.z = Math.sin(t * 4) * .12; };
  } else if (kind === 'sowa') {
    const br = lam(0x92400e), lt = lam(0xd6a76c);
    const c = canvas(8, 8), x = c.getContext('2d'); x.fillStyle = '#b45309'; x.fillRect(0, 0, 8, 8); x.fillStyle = '#fde68a'; x.fillRect(0, 2, 4, 3); x.fillRect(4, 2, 4, 3); x.fillStyle = '#111'; x.fillRect(1, 3, 2, 1); x.fillRect(5, 3, 2, 1); x.fillStyle = '#f59e0b'; x.fillRect(3, 5, 2, 2);
    const head = box(4.4, 4, 4, [br, br, br, br, new T.MeshLambertMaterial({ map: nearest(new T.CanvasTexture(c)) }), br]); head.position.y = 3.6; body.add(head);
    [1, -1].forEach(s => { const t_ = box(1, 1.2, 1, br); t_.position.set(s * 1.6, 2.4, 0); head.add(t_); });
    const b = box(4, 4.4, 3.4, [br, br, br, br, lt, br]); body.add(b);
    const ws = [1, -1].map(s => { const w = new T.Group(); w.position.set(s * 2.1, 1.6, 0); const m = box(.6, 4, 3, lam(0x78350f)); m.position.y = -2; w.add(m); body.add(w); return w; });
    up = t => { head.rotation.y = Math.sin(t * .9) * .9; ws.forEach((w, i) => w.rotation.z = (i ? -1 : 1) * Math.max(0, Math.sin(t * 5)) * .8); };
  } else if (kind === 'dron') {
    const gr = lam(0x374151), lt = bas(0x38bdf8);
    const b = box(4, 1.6, 4, gr); body.add(b); const eye = box(1.6, .8, .2, lt); eye.position.set(0, 0, 2.05); body.add(eye);
    const rot = [[1, 1], [1, -1], [-1, 1], [-1, -1]].map(([sx, sz]) => { const arm = box(.5, .4, 3, gr); arm.position.set(sx * 2.3, .4, sz * 2.3); arm.rotation.y = sx * sz * PI / 4; body.add(arm); const r = new T.Group(); r.position.set(sx * 3.4, 1, sz * 3.4); const bl = box(4, .1, .6, new T.MeshBasicMaterial({ color: 0xe5e7eb, transparent: true, opacity: .7 })); r.add(bl); body.add(r); return r; });
    up = t => { rot.forEach((r, i) => r.rotation.y = t * 30 + i); eye.material.color.setHSL(.55 + Math.sin(t * 2) * .05, 1, .55 + Math.sin(t * 6) * .1); body.rotation.z = Math.sin(t * 1.4) * .08; };
  }
  else if (kind === 'krolik') {
    const w = lam(0xf5f5f4), pk = lam(0xf9a8d4);
    const b = box(3.4, 3.2, 4.4, w); body.add(b);
    const h = box(3, 3, 3, [w, w, w, w, new T.MeshLambertMaterial({ map: faceTex('#f5f5f4', '#1c1917', x => { x.fillStyle = '#f9a8d4'; x.fillRect(3, 5, 2, 1); }) }), w]); h.position.set(0, 1.8, 2.6); body.add(h);
    const ears = [1, -1].map(s => { const e = new T.Group(); e.position.set(s * .8, 3.2, 2.4); const o = box(.9, 3.4, .6, w); o.position.y = 1.7; const i = box(.5, 2.6, .1, pk); i.position.set(0, 1.7, .32); e.add(o, i); body.add(e); return e; });
    const tail = box(1.2, 1.2, 1.2, w); tail.position.set(0, .6, -2.6); body.add(tail);
    up = t => { const hop = Math.max(0, Math.sin(t * 5)); body.position.y = hop * 2.4; ears.forEach((e, i) => e.rotation.x = -.2 - hop * .5 + i * .1); };
  } else if (kind === 'nietoperz') {
    const dk = lam(0x292524);
    const b = box(2.6, 3, 2.4, [dk, dk, dk, dk, new T.MeshLambertMaterial({ map: faceTex('#292524', '#ef4444') }), dk]); body.add(b);
    [1, -1].forEach(s => { const e = box(.7, 1.2, .5, dk); e.position.set(s * .9, 2, 0); body.add(e); });
    const wm = new T.MeshLambertMaterial({ color: 0x1c1917, side: T.DoubleSide });
    const ws = [1, -1].map(s => { const p = new T.Group(); p.position.set(s * 1.3, .6, 0); const sh = new T.Shape(); sh.moveTo(0, 0); sh.lineTo(5, 1.5); sh.lineTo(4, -.5); sh.lineTo(3, .2); sh.lineTo(2, -1); sh.lineTo(1, -.2); sh.lineTo(0, -1.2); const m = new T.Mesh(new T.ShapeGeometry(sh), wm); m.scale.x = s; p.add(m); body.add(p); return p; });
    up = t => { ws.forEach((w, i) => w.rotation.y = (i ? -1 : 1) * Math.sin(t * 14) * .8); body.position.y = Math.sin(t * 3) * 1.4; };
  } else if (kind === 'meduza') {
    const bell = new T.Mesh(new T.SphereGeometry(3, 16, 12, 0, PI * 2, 0, PI / 2), new T.MeshBasicMaterial({ color: 0xc084fc, transparent: true, opacity: .75 })); body.add(bell);
    const core = new T.Mesh(new T.SphereGeometry(1.4, 12, 10), new T.MeshBasicMaterial({ color: 0xf0abfc })); core.position.y = .6; body.add(core);
    const tm = new T.MeshBasicMaterial({ color: 0xe9d5ff, transparent: true, opacity: .7 });
    const ts = []; for (let i = 0; i < 6; i++) { const a = i / 6 * PI * 2; const p = new T.Group(); p.position.set(Math.cos(a) * 2, 0, Math.sin(a) * 2); for (let k = 0; k < 4; k++) { const seg = box(.35, 1.4, .35, tm); seg.position.y = -.8 - k * 1.3; p.add(seg); } body.add(p); ts.push(p); }
    up = t => { const pulse = Math.sin(t * 3); bell.scale.set(1 + pulse * .08, 1 - pulse * .12, 1 + pulse * .08); body.position.y = Math.sin(t * 1.5) * 1.6; ts.forEach((p, i) => { p.rotation.x = Math.sin(t * 3 + i) * .25; p.rotation.z = Math.cos(t * 2.4 + i) * .25; }); };
  } else if (kind === 'kurczak') {
    const yl = lam(0xfde047);
    const b = box(3, 3, 3, [yl, yl, yl, yl, new T.MeshLambertMaterial({ map: faceTex('#fde047', '#1c1917') }), yl]); body.add(b);
    const bk = box(1, .7, .9, lam(0xf97316)); bk.position.set(0, -.2, 1.9); body.add(bk);
    const ws = [1, -1].map(s => { const w = box(.5, 1.6, 2, yl); w.position.set(s * 1.7, 0, 0); body.add(w); return w; });
    const legs = [1, -1].map(s => { const l = box(.4, 1.2, .4, lam(0xf97316)); l.position.set(s * .7, -2, 0); body.add(l); return l; });
    up = t => { ws.forEach((w, i) => w.rotation.z = (i ? -1 : 1) * Math.max(0, Math.sin(t * 10)) * .6); legs.forEach((l, i) => l.rotation.x = Math.sin(t * 8 + i * PI) * .4); body.position.y = Math.abs(Math.sin(t * 8)) * .4; };
  } else if (kind === 'zolw') {
    const sh = new T.Mesh(new T.SphereGeometry(3, 10, 6, 0, PI * 2, 0, PI / 2), lam(0x15803d)); sh.scale.y = .7; body.add(sh);
    const sk = lam(0x84cc16);
    const h = box(1.8, 1.6, 2, [sk, sk, sk, sk, new T.MeshLambertMaterial({ map: faceTex('#84cc16', '#14532d') }), sk]); h.position.set(0, .4, 3.4); body.add(h);
    const ls = [[1.8, 1.8], [-1.8, 1.8], [1.8, -1.8], [-1.8, -1.8]].map(([x, z]) => { const l = box(1.2, .8, 1.4, sk); l.position.set(x, -.3, z); body.add(l); return l; });
    up = t => { ls.forEach((l, i) => l.rotation.y = Math.sin(t * 3 + i * 1.6) * .5); h.rotation.y = Math.sin(t * .7) * .4; };
  } else if (kind === 'feniks') {
    const fm = new T.MeshBasicMaterial({ color: 0xf97316 });
    const b = box(2.2, 2.2, 4, fm); body.add(b);
    const h = box(2, 2, 2, [fm, fm, fm, fm, new T.MeshBasicMaterial({ map: faceTex('#f97316', '#fef08a') }), fm]); h.position.set(0, 1.2, 2.6); body.add(h);
    const crest = box(.5, 1.6, 1.6, new T.MeshBasicMaterial({ color: 0xfde047 })); crest.position.set(0, 2.6, 2.4); body.add(crest);
    const wm = new T.MeshBasicMaterial({ color: 0xfbbf24, side: T.DoubleSide, transparent: true, opacity: .9 });
    const ws = [1, -1].map(s => { const p = new T.Group(); p.position.set(s * 1.1, .8, 0); const w = new T.Mesh(new T.PlaneGeometry(6, 3), wm); w.position.x = s * 3; w.rotation.x = -PI / 2; p.add(w); body.add(p); return p; });
    const tx = TEX.glow('rgba(251,146,60,1)'); const em = []; for (let i = 0; i < 10; i++) { const sp = new T.Sprite(new T.SpriteMaterial({ map: tx, transparent: true, blending: T.AdditiveBlending, depthWrite: false })); sp.scale.setScalar(1.2); body.add(sp); em.push({ sp, ph: rnd() }); }
    up = t => { ws.forEach((w, i) => w.rotation.z = (i ? -1 : 1) * Math.sin(t * 6) * .7); em.forEach(e => { const u = (t * .9 + e.ph) % 1; e.sp.position.set(Math.sin(e.ph * 20) * .8, -u * 3, -2 - u * 5); e.sp.material.opacity = 1 - u; }); };
  } else if (kind === 'ufo') {
    const disc = new T.Mesh(new T.CylinderGeometry(4, 4.6, 1, 20), lam(0x9ca3af, 0x111111)); body.add(disc);
    const dome = new T.Mesh(new T.SphereGeometry(2, 14, 10, 0, PI * 2, 0, PI / 2), new T.MeshBasicMaterial({ color: 0x67e8f9, transparent: true, opacity: .6 })); dome.position.y = .5; body.add(dome);
    const lights = []; for (let i = 0; i < 8; i++) { const a = i / 8 * PI * 2; const l = box(.6, .4, .6, new T.MeshBasicMaterial({ color: 0xfacc15 })); l.position.set(Math.cos(a) * 4.2, -.1, Math.sin(a) * 4.2); body.add(l); lights.push(l); }
    const beam = new T.Mesh(new T.ConeGeometry(3, 9, 16, 1, true), new T.MeshBasicMaterial({ color: 0x86efac, transparent: true, opacity: .18, blending: T.AdditiveBlending, depthWrite: false, side: T.DoubleSide })); beam.position.y = -5; body.add(beam);
    up = t => { body.rotation.y = t * 1.5; lights.forEach((l, i) => l.material.color.setHSL(((t * .5 + i / 8) % 1), .9, .6)); beam.material.opacity = .12 + Math.sin(t * 4) * .06; };
  } else if (kind === 'voidbot') {
    const sh = lam(0x1e1b4b, 0x0b0820);
    const scr = canvas(8, 8), sx = scr.getContext('2d'); const st = nearest(new T.CanvasTexture(scr));
    const face = new T.MeshBasicMaterial({ map: st });
    const b = box(4, 3.4, 3.4, [sh, sh, sh, sh, face, sh]); body.add(b);
    const ant = box(.3, 1.6, .3, sh); ant.position.y = 2.4; body.add(ant); const tip = box(.7, .7, .7, new T.MeshBasicMaterial({ color: 0xa78bfa })); tip.position.y = 3.4; body.add(tip);
    up = t => { sx.fillStyle = '#0c0a1d'; sx.fillRect(0, 0, 8, 8); sx.fillStyle = '#a78bfa'; const bl = (t % 3) < .12; if (bl) { sx.fillRect(1, 4, 2, 1); sx.fillRect(5, 4, 2, 1); } else { sx.fillRect(1, 3, 2, 2); sx.fillRect(5, 3, 2, 2); } sx.fillRect(3, 6, 2, 1); st.needsUpdate = true; tip.material.color.setHSL(.72, .9, .5 + Math.sin(t * 5) * .2); body.rotation.z = Math.sin(t * 1.2) * .1; };
  }
  g.scale.setScalar(1.25);
  return { obj: g, update(t) { g.position.set(11 + Math.sin(t * .7) * 1.5, 26 + Math.sin(t * 2) * 1.2, -3 + Math.cos(t * .7) * 2); g.rotation.y = Math.sin(t * .5) * .4; up(t); } };
}

/* ---------------- efekty wokół gracza ---------------- */
const spriteTex = draw => { const c = canvas(32, 32), x = c.getContext('2d'); draw(x); return new T.CanvasTexture(c); };
const TEX = {
  heart: () => spriteTex(x => { x.fillStyle = '#fb7185'; x.beginPath(); x.moveTo(16, 28); x.bezierCurveTo(-6, 12, 6, -2, 16, 9); x.bezierCurveTo(26, -2, 38, 12, 16, 28); x.fill(); }),
  glow: c => spriteTex(x => { const g = x.createRadialGradient(16, 16, 0, 16, 16, 16); g.addColorStop(0, c); g.addColorStop(.35, c); g.addColorStop(1, 'rgba(0,0,0,0)'); x.fillStyle = g; x.fillRect(0, 0, 32, 32); }),
  flake: () => spriteTex(x => { x.strokeStyle = '#e0f2fe'; x.lineWidth = 2.5; for (let i = 0; i < 3; i++) { x.save(); x.translate(16, 16); x.rotate(i * PI / 3); x.beginPath(); x.moveTo(0, -13); x.lineTo(0, 13); x.stroke(); x.restore(); } }),
  rune: () => spriteTex(x => { x.strokeStyle = '#c4b5fd'; x.lineWidth = 3; x.beginPath(); for (let i = 0; i < 4; i++) { const a = rnd() * 28 + 2, b = rnd() * 28 + 2; i ? x.lineTo(a, b) : x.moveTo(a, b); } x.stroke(); }),
};
function makeFx(kind) {
  const g = new T.Group(); const parts = [];
  const sprite = (tex, add, size) => { const s = new T.Sprite(new T.SpriteMaterial({ map: tex, transparent: true, depthWrite: false, blending: add ? T.AdditiveBlending : T.NormalBlending })); s.scale.set(size, size, 1); g.add(s); return s; };
  if (kind === 'serca') { const tx = TEX.heart(); for (let i = 0; i < 14; i++) parts.push({ s: sprite(tx, false, 2.4), ph: rnd() * 4, a: rnd() * PI * 2, r: 7 + rnd() * 5 }); }
  if (kind === 'ogien') { const tx = TEX.glow('rgba(255,140,40,1)'), tx2 = TEX.glow('rgba(255,60,20,1)'); for (let i = 0; i < 46; i++) parts.push({ s: sprite(i % 2 ? tx : tx2, true, 3 + rnd() * 2), ph: rnd() * 2, a: i / 46 * PI * 2, r: 8 }); }
  if (kind === 'orbity') { const cols = ['rgba(167,139,250,1)', 'rgba(56,189,248,1)', 'rgba(244,114,182,1)']; cols.forEach((c, k) => { const tx = TEX.glow(c); for (let j = 0; j < 9; j++) parts.push({ s: sprite(tx, true, j ? 2.6 - j * .22 : 3.6), k, j }); }); }
  if (kind === 'snieg') { const tx = TEX.flake(); for (let i = 0; i < 40; i++) parts.push({ s: sprite(tx, false, 1.4 + rnd()), ph: rnd() * 6, a: rnd() * PI * 2, r: 5 + rnd() * 10 }); }
  if (kind === 'runy') { for (let i = 0; i < 18; i++) parts.push({ s: sprite(TEX.rune(), true, 2.6), ph: rnd() * 5, a: rnd() * PI * 2 }); }
  if (kind === 'pyl') { const tx = TEX.glow('rgba(253,230,138,1)'), tx2 = TEX.glow('rgba(196,181,253,1)'); for (let i = 0; i < 40; i++) parts.push({ s: sprite(i % 2 ? tx : tx2, true, 1.6), ph: rnd() * 3, x: (rnd() - .5) * 20, y: rnd() * 36, z: (rnd() - .5) * 14 }); }
  if (kind === 'blyskawice') { const tx = TEX.glow('rgba(125,211,252,1)'); for (let k = 0; k < 3; k++) for (let j = 0; j < 14; j++) parts.push({ s: sprite(tx, true, 1.6), k, j }); }
  if (kind === 'babelki') { const tx = spriteTex(x => { x.strokeStyle = 'rgba(186,230,253,.95)'; x.lineWidth = 2.5; x.beginPath(); x.arc(16, 16, 12, 0, PI * 2); x.stroke(); x.fillStyle = 'rgba(255,255,255,.8)'; x.beginPath(); x.arc(11, 11, 3, 0, PI * 2); x.fill(); }); for (let i = 0; i < 24; i++) parts.push({ s: sprite(tx, false, 1.2 + rnd() * 1.6), ph: rnd() * 5, a: rnd() * PI * 2, r: 6 + rnd() * 8 }); }
  if (kind === 'liscie') { const mk = c => spriteTex(x => { x.fillStyle = c; x.beginPath(); x.ellipse(16, 16, 7, 13, .5, 0, PI * 2); x.fill(); x.strokeStyle = 'rgba(0,0,0,.35)'; x.lineWidth = 1.5; x.beginPath(); x.moveTo(10, 26); x.lineTo(22, 6); x.stroke(); }); const txs = ['#f97316', '#eab308', '#dc2626', '#84cc16'].map(mk); for (let i = 0; i < 24; i++) parts.push({ s: sprite(txs[i % 4], false, 2 + rnd()), ph: rnd() * 6, a: rnd() * PI * 2, r: 6 + rnd() * 9 }); }
  if (kind === 'nuty') { const mk = c => spriteTex(x => { x.fillStyle = c; x.beginPath(); x.ellipse(11, 24, 6, 4.5, -.4, 0, PI * 2); x.fill(); x.fillRect(15, 5, 3, 20); x.fillRect(15, 5, 10, 4); }); const txs = ['#f472b6', '#a78bfa', '#38bdf8', '#4ade80'].map(mk); for (let i = 0; i < 14; i++) parts.push({ s: sprite(txs[i % 4], false, 2.6), ph: rnd() * 5, a: rnd() * PI * 2 }); }
  if (kind === 'aura') { for (let i = 0; i < 4; i++) { const m = new T.Mesh(new T.RingGeometry(1, 1.6, 48), new T.MeshBasicMaterial({ color: 0xa78bfa, transparent: true, side: T.DoubleSide, blending: T.AdditiveBlending, depthWrite: false })); m.rotation.x = -PI / 2; g.add(m); parts.push({ m, ph: i / 4 }); } const tx = TEX.glow('rgba(167,139,250,1)'); for (let i = 0; i < 20; i++) parts.push({ s: sprite(tx, true, 1.2), ph: rnd() * 3, a: rnd() * PI * 2 }); }
  if (kind === 'tornado') { const tx = TEX.glow('rgba(196,181,253,1)'); for (let i = 0; i < 70; i++) parts.push({ s: sprite(tx, true, .9 + rnd()), ph: rnd(), a: rnd() * PI * 2 }); }
  if (kind === 'krag') { const c = canvas(128, 128), x = c.getContext('2d'); x.strokeStyle = '#c4b5fd'; x.lineWidth = 3; x.beginPath(); x.arc(64, 64, 58, 0, PI * 2); x.stroke(); x.beginPath(); x.arc(64, 64, 44, 0, PI * 2); x.stroke(); x.font = '14px serif'; x.fillStyle = '#e9d5ff'; for (let i = 0; i < 12; i++) { x.save(); x.translate(64, 64); x.rotate(i / 12 * PI * 2); x.fillText('ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃ'[i], -4, -47); x.restore(); } for (let i = 0; i < 6; i++) { const a = i / 6 * PI * 2; x.beginPath(); x.moveTo(64 + Math.cos(a) * 44, 64 + Math.sin(a) * 44); x.lineTo(64 + Math.cos(a + PI * 2 / 3) * 44, 64 + Math.sin(a + PI * 2 / 3) * 44); x.stroke(); }
    const m = new T.Mesh(new T.PlaneGeometry(26, 26), new T.MeshBasicMaterial({ map: new T.CanvasTexture(c), transparent: true, blending: T.AdditiveBlending, depthWrite: false, side: T.DoubleSide })); m.rotation.x = -PI / 2; m.position.y = .3; g.add(m); parts.push({ m }); const tx = TEX.glow('rgba(167,139,250,1)'); for (let i = 0; i < 16; i++) parts.push({ s: sprite(tx, true, 1.2), ph: rnd(), a: i / 16 * PI * 2 }); }
  if (kind === 'motylki') { const mk = h => spriteTex(x => { x.fillStyle = `hsl(${h},90%,65%)`; x.beginPath(); x.ellipse(10, 12, 8, 9, -.4, 0, PI * 2); x.ellipse(22, 12, 8, 9, .4, 0, PI * 2); x.fill(); x.beginPath(); x.ellipse(11, 22, 5, 6, .3, 0, PI * 2); x.ellipse(21, 22, 5, 6, -.3, 0, PI * 2); x.fill(); x.fillStyle = '#1c1917'; x.fillRect(15, 6, 2, 20); }); const txs = [300, 190, 45, 140].map(mk); for (let i = 0; i < 8; i++) parts.push({ s: sprite(txs[i % 4], false, 2.4), ph: rnd() * 6, r: 9 + rnd() * 4, h: 10 + rnd() * 22 }); }
  if (kind === 'swietliki') { const tx = TEX.glow('rgba(190,242,100,1)'); for (let i = 0; i < 26; i++) parts.push({ s: sprite(tx, true, 1.4), ph: rnd() * 6, x: (rnd() - .5) * 26, y: 2 + rnd() * 34, z: (rnd() - .5) * 20 }); }
  if (kind === 'dziura') { const hole = new T.Mesh(new T.CircleGeometry(3, 32), new T.MeshBasicMaterial({ color: 0x000000 })); const ring = new T.Mesh(new T.RingGeometry(3, 6, 48), new T.MeshBasicMaterial({ color: 0xa855f7, transparent: true, opacity: .55, blending: T.AdditiveBlending, depthWrite: false, side: T.DoubleSide })); const hp = new T.Group(); hp.position.y = 41; hp.add(ring, hole); g.add(hp); parts.push({ hp, ring }); const tx = TEX.glow('rgba(192,132,252,1)'); for (let i = 0; i < 30; i++) parts.push({ s: sprite(tx, true, 1), ph: rnd(), a: rnd() * PI * 2 }); }
  if (kind === 'platki') { const tx = spriteTex(x => { x.fillStyle = '#fbcfe8'; x.beginPath(); x.ellipse(16, 16, 6, 11, .6, 0, PI * 2); x.fill(); x.fillStyle = '#f9a8d4'; x.beginPath(); x.ellipse(15, 18, 3, 7, .6, 0, PI * 2); x.fill(); }); for (let i = 0; i < 30; i++) parts.push({ s: sprite(tx, false, 1.6), ph: rnd() * 6, a: rnd() * PI * 2, r: 5 + rnd() * 10 }); }
  if (kind === 'konfetti') { const cols = ['#f472b6', '#facc15', '#4ade80', '#38bdf8', '#a78bfa', '#fb923c']; const txs = cols.map(c => spriteTex(x => { x.fillStyle = c; x.fillRect(10, 4, 12, 24); })); for (let i = 0; i < 44; i++) parts.push({ s: sprite(txs[i % 6], false, 1.1), ph: rnd() * 2.5, a: rnd() * PI * 2, v: 8 + rnd() * 10 }); }
  if (kind === 'chmurka') { const cm = new T.MeshLambertMaterial({ color: 0x94a3b8, emissive: 0x1e293b }); const cl = new T.Group(); cl.position.y = 40; [[0, 0, 0, 5], [-4, -.5, 0, 3.6], [4, -.5, 0, 3.8], [-1.5, 1.8, 0, 3.2], [2, 1.5, 0, 3]].forEach(([x, y, z, r]) => { const b = box(r * 1.6, r * 1.1, r * 1.3, cm); b.position.set(x, y, z); cl.add(b); }); g.add(cl); parts.push({ cl }); const tx = spriteTex(x => { x.fillStyle = '#7dd3fc'; x.fillRect(14, 4, 4, 24); }); for (let i = 0; i < 26; i++) parts.push({ s: sprite(tx, false, 1.2), ph: rnd(), x: (rnd() - .5) * 12, z: (rnd() - .5) * 6 }); }
  let seed = 0;
  return { obj: g, update(t) {
    if (['tornado', 'krag', 'motylki', 'swietliki', 'dziura', 'platki', 'konfetti', 'chmurka'].includes(kind)) { parts.forEach((p, i) => { const s = p.s;
      if (kind === 'tornado') { const u = (t * .35 + p.ph) % 1; const a = p.a + t * 4 + u * 10; const r = 3 + u * 11; s.position.set(Math.cos(a) * r, u * 40, Math.sin(a) * r); s.material.opacity = Math.sin(u * PI) * .9; }
      if (kind === 'krag') { if (p.m) { p.m.rotation.z = t * .6; p.m.material.opacity = .75 + Math.sin(t * 3) * .25; } else { const u = (t * .4 + p.ph) % 1; s.position.set(Math.cos(p.a + t * .6) * 12, u * 18, Math.sin(p.a + t * .6) * 12); s.material.opacity = 1 - u; } }
      if (kind === 'motylki') { const a = t * .9 + p.ph; s.position.set(Math.cos(a) * p.r, p.h + Math.sin(t * 2 + p.ph) * 3, Math.sin(a) * p.r); s.scale.x = 2.4 * Math.abs(Math.sin(t * 12 + i)); }
      if (kind === 'swietliki') { const tw = Math.max(0, Math.sin(t * 1.6 + p.ph * 3)); s.position.set(p.x + Math.sin(t * .7 + i) * 2, p.y + Math.sin(t * .9 + i * 2) * 2, p.z + Math.cos(t * .6 + i) * 2); s.material.opacity = tw; }
      if (kind === 'dziura') { if (p.hp) { p.hp.rotation.x = -PI / 2; p.ring.rotation.z = t * 2; p.ring.material.opacity = .45 + Math.sin(t * 3) * .15; } else { const u = (t * .4 + p.ph) % 1; const a = p.a + u * 8; const r = 14 * (1 - u); s.position.set(Math.cos(a) * r, 41 - (1 - u) * 5, Math.sin(a) * r); s.material.opacity = u; } }
      if (kind === 'platki') { const u = ((t * .14 + p.ph / 6) % 1); s.position.set(Math.cos(p.a + u * 3) * p.r, 40 - u * 40, Math.sin(p.a + u * 3) * p.r); s.material.rotation = t * 1.5 + i; s.material.opacity = Math.min(1, (1 - u) * 5); }
      if (kind === 'konfetti') { const u = ((t + p.ph) % 2.5) / 2.5; const up2 = u * p.v * 2.5, y = 30 + up2 * 1.2 - u * u * 70; s.position.set(Math.cos(p.a) * u * 16, Math.max(0, y), Math.sin(p.a) * u * 16); s.material.rotation = t * 6 + i; s.material.opacity = Math.min(1, (1 - u) * 3); }
      if (kind === 'chmurka') { if (p.cl) { p.cl.position.y = 40 + Math.sin(t * 1.2) * .6; } else { const u = (t * 1.3 + p.ph) % 1; s.position.set(p.x, 37 - u * 16, p.z); s.material.opacity = 1 - u * .6; } }
    }); return; }
    if (kind === 'blyskawice') { const k2 = Math.floor(t / .12); if (k2 !== seed) { seed = k2; for (let k = 0; k < 3; k++) { const a = rnd() * PI * 2; let x = Math.cos(a) * 9, y = 30 + rnd() * 6, z = Math.sin(a) * 9; parts.filter(p => p.k === k).forEach(p => { p.s.position.set(x, y, z); x += (rnd() - .5) * 3; y -= 2.2; z += (rnd() - .5) * 3; }); } } parts.forEach(p => p.s.material.opacity = (Math.sin(t * 40 + p.k) > -.3 ? 1 : .2)); return; }
    if (kind === 'aura') { parts.forEach((p, i) => { if (p.m) { const u = (t * .5 + p.ph) % 1; p.m.scale.setScalar(2 + u * 9); p.m.position.y = .2; p.m.material.opacity = (1 - u) * .8; } else { const u = (t * .3 + p.ph / 3) % 1; p.s.position.set(Math.cos(p.a) * (6 + u * 2), u * 26, Math.sin(p.a) * (6 + u * 2)); p.s.material.opacity = Math.sin(u * PI); } }); return; }
    parts.forEach((p, i) => { const s = p.s;
      if (kind === 'babelki') { const u = ((t * .22 + p.ph / 5) % 1); s.position.set(Math.cos(p.a) * p.r + Math.sin(t * 2 + i) * .8, u * 38, Math.sin(p.a) * p.r); s.material.opacity = Math.min(1, (1 - u) * 3); }
      if (kind === 'liscie') { const u = ((t * .16 + p.ph / 6) % 1); s.position.set(Math.cos(p.a + u * 2) * p.r + Math.sin(t * 1.5 + i) * 2, 40 - u * 40, Math.sin(p.a + u * 2) * p.r); s.material.rotation = t * 2 + i; s.material.opacity = Math.min(1, (1 - u) * 5); }
      if (kind === 'nuty') { const u = ((t * .25 + p.ph / 5) % 1); const a = p.a + u * PI * 1.5; s.position.set(Math.cos(a) * 9, 8 + u * 28, Math.sin(a) * 9); s.material.opacity = Math.sin(u * PI); s.material.rotation = Math.sin(t * 3 + i) * .3; }
      if (kind === 'serca') { const u = ((t * .35 + p.ph / 4) % 1); s.position.set(Math.cos(p.a) * p.r, 6 + u * 30, Math.sin(p.a) * p.r); s.material.opacity = Math.sin(u * PI); }
      if (kind === 'ogien') { const u = ((t * 1.2 + p.ph) % 2) / 2; const a = p.a + t * .8; s.position.set(Math.cos(a) * (p.r - u * 2), u * 9, Math.sin(a) * (p.r - u * 2)); s.material.opacity = 1 - u; s.scale.setScalar((1 - u) * 4 + .5); }
      if (kind === 'orbity') { const a = t * 1.8 - p.j * .12 + p.k * 2.1, tilt = [.3, -.5, .9][p.k]; const r = 11; const x = Math.cos(a) * r, z = Math.sin(a) * r; s.position.set(x, 18 + z * Math.sin(tilt) * .6 + p.k * 3 - 3, z * Math.cos(tilt)); s.material.opacity = 1 - p.j / 10; }
      if (kind === 'snieg') { const u = ((t * .18 + p.ph / 6) % 1); s.position.set(Math.cos(p.a + t * .2) * p.r, 40 - u * 40, Math.sin(p.a + t * .2) * p.r); s.material.opacity = Math.min(1, (1 - u) * 4); }
      if (kind === 'runy') { const u = ((t * .2 + p.ph / 5) % 1); const a = p.a + u * PI * 2; s.position.set(Math.cos(a) * 10, u * 34, Math.sin(a) * 10); s.material.opacity = Math.sin(u * PI); s.material.color.setHSL(.72 + Math.sin(t + i) * .08, .9, .7); }
      if (kind === 'pyl') { const tw = Math.max(0, Math.sin(t * 3 + p.ph * 7)); s.position.set(p.x, p.y + Math.sin(t + i) * .5, p.z); s.material.opacity = tw; s.scale.setScalar(.6 + tw * 1.6); }
    }); } };
}

/* ---------------- emotki: każda ustawia pozę w czasie t (sekundy, w pętli) ---------------- */
const sm = x => x * x * (3 - 2 * x);
const EMOTES = {
  machanie: (t, p) => { p.armL.rotation.z = 2.7 + Math.sin(t * 9) * .35; p.head.rotation.z = Math.sin(t * 2) * .08; },
  uklon: (t, p, r) => { const b = sm(Math.max(0, Math.sin(t * 1.8))); r.rotation.x = b * .7; p.armR.rotation.x = -b * .4; p.armL.rotation.x = -b * .4; },
  facepalm: (t, p) => { p.armR.rotation.x = -2.5; p.armR.rotation.z = .35; p.head.rotation.x = .35 + Math.sin(t * 6) * .05; p.head.rotation.y = Math.sin(t * 3) * .15; },
  klaskanie: (t, p) => { const k = Math.abs(Math.sin(t * 9)); p.armR.rotation.x = -1.35; p.armL.rotation.x = -1.35; p.armR.rotation.z = .2 + k * .35; p.armL.rotation.z = -.2 - k * .35; },
  robot: (t, p) => { const st = Math.floor(t * 2.5) % 4; p.armR.rotation.x = st % 2 ? -1.57 : 0; p.armL.rotation.x = st % 2 ? 0 : -1.57; p.head.rotation.y = [0, .6, 0, -.6][st]; p.legR.rotation.x = st === 1 ? -.5 : 0; p.legL.rotation.x = st === 3 ? -.5 : 0; },
  krecio: (t, p, r) => { r.rotation.y = t * 7; p.armR.rotation.z = -1.5; p.armL.rotation.z = 1.5; p.head.rotation.x = -.2; },
  salto: (t, p, r) => { const u = (t % 1.6) / 1.6, a = sm(Math.min(1, Math.max(0, (u - .15) / .7))) * Math.PI * 2; r.rotation.x = -a; r.position.z = 16 * Math.sin(a); r.position.y = 16 - 16 * Math.cos(a) + Math.sin(u * Math.PI) * 8; p.legR.rotation.x = p.legL.rotation.x = -Math.sin(a / 2) * 1.2; p.armR.rotation.x = p.armL.rotation.x = -Math.sin(a / 2) * 2; },
  pajacyki: (t, p, r) => { const k = Math.abs(Math.sin(t * 5)); p.armR.rotation.z = -.2 - 2.6 * k; p.armL.rotation.z = .2 + 2.6 * k; p.legR.rotation.z = -.4 * k; p.legL.rotation.z = .4 * k; r.position.y = k * 2; },
  wzruszenie: (t, p) => { const k = sm(Math.abs(Math.sin(t * 1.6))); p.armR.rotation.x = -.7 * k; p.armL.rotation.x = -.7 * k; p.armR.rotation.z = -.6 * k; p.armL.rotation.z = .6 * k; p.head.rotation.z = .25 * k; },
  dab: (t, p, r) => { const k = sm(Math.min(1, (t % 2.4) / .3)) * (t % 2.4 < 2 ? 1 : sm(1 - (t % 2.4 - 2) / .4)); p.head.rotation.x = .55 * k; p.head.rotation.y = -.7 * k; p.armR.rotation.z = -2.3 * k; p.armR.rotation.x = -.4 * k; p.armL.rotation.x = -1.5 * k; p.armL.rotation.z = -.9 * k; r.rotation.z = .08 * k; },
  taniec: (t, p, r) => { const b = Math.sin(t * 8); r.position.y = Math.abs(b) * 1.5; r.rotation.y = Math.sin(t * 2) * .5; p.legR.rotation.x = Math.max(0, b) * -.9; p.legL.rotation.x = Math.max(0, -b) * -.9; p.armR.rotation.z = -1 - b * .8; p.armL.rotation.z = 1 - b * .8; p.head.rotation.z = b * .15; },
  medytacja: (t, p, r) => { r.position.y = 7 + Math.sin(t * 1.5) * 1.5; p.legR.rotation.x = -1.45; p.legL.rotation.x = -1.45; p.legR.rotation.z = .5; p.legL.rotation.z = -.5; p.armR.rotation.z = -.5; p.armL.rotation.z = .5; p.armR.rotation.x = p.armL.rotation.x = -.5; p.head.rotation.x = .15; },
  salut: (t, p) => { const k = sm(Math.min(1, (t % 3) / .4)); p.armR.rotation.x = -2.5 * k; p.armR.rotation.z = -.55 * k; p.head.rotation.x = -.1 * k; },
  pompki: (t, p, r) => { const d = Math.abs(Math.sin(t * 3)); r.rotation.x = 1.42; r.position.y = 2.2 + d * 2.8; r.position.z = -14; p.armR.rotation.x = p.armL.rotation.x = -1.5 - d * .35; p.head.rotation.x = -.6; },
  gitara: (t, p) => { p.armL.rotation.x = -1.1; p.armL.rotation.z = -.5; p.armR.rotation.x = -.5 + Math.sin(t * 14) * .25; p.armR.rotation.z = .5; p.head.rotation.x = Math.abs(Math.sin(t * 4)) * .35; },
  hura: (t, p, r) => { const u = Math.abs(Math.sin(t * 4)); r.position.y = u * 5; p.armR.rotation.z = -2.8 + u * .2; p.armL.rotation.z = 2.8 - u * .2; p.legR.rotation.x = p.legL.rotation.x = -u * .4; p.head.rotation.x = -.25; },
  przysiad: (t, p, r) => { const d = sm(Math.abs(Math.sin(t * 2.2))); r.position.y = -d * 5; p.legR.rotation.x = p.legL.rotation.x = -d * 1.2; r.rotation.x = d * .25; p.armR.rotation.x = p.armL.rotation.x = -1.5 * d; },
  drzemka: (t, p, r) => { r.rotation.x = -1.5; r.position.y = 2.5; r.position.z = 14; p.head.rotation.y = .4; p.armR.rotation.z = -.2; p.armL.rotation.z = .2; r.position.y += Math.sin(t * 1.4) * .4; },
  wskazuje: (t, p) => { p.armR.rotation.x = -1.57; p.armR.rotation.y = .3 * Math.sin(t * 1.5); p.head.rotation.x = Math.sin(t * 6) * .15; },
  serce: (t, p) => { const b = 1 + Math.sin(t * 5) * .06; p.armR.rotation.z = -2.6 * b; p.armL.rotation.z = 2.6 * b; p.armR.rotation.x = p.armL.rotation.x = -.35; },
  boks: (t, p, r) => { const a = Math.sin(t * 9), b = Math.sin(t * 9 + PI); p.armR.rotation.x = -1.2 - Math.max(0, a) * .5; p.armL.rotation.x = -1.2 - Math.max(0, b) * .5; p.armR.rotation.z = .4 - Math.max(0, a) * .3; p.armL.rotation.z = -.4 + Math.max(0, b) * .3; r.position.y = Math.abs(Math.sin(t * 4.5)) * 1; r.rotation.y = Math.sin(t * 9) * .15; },
  karate: (t, p, r) => { const u = (t % 1.6) / 1.6, k = sm(Math.min(1, u * 3)) * (u < .6 ? 1 : sm(Math.max(0, 1 - (u - .6) * 3))); p.legR.rotation.x = -1.6 * k; r.rotation.x = -.25 * k; p.armR.rotation.z = -1.2 * k; p.armL.rotation.z = 1.2 * k; },
  imprezka: (t, p, r) => { const u = Math.abs(Math.sin(t * 6)); p.armR.rotation.z = -2.9 + u * .4; p.armL.rotation.z = 2.9 - u * .4; p.armR.rotation.x = p.armL.rotation.x = -.2 * u; r.position.y = u * 1.4; p.head.rotation.x = u * .2; },
  lasso: (t, p, r) => { p.armR.rotation.z = -2.9; p.armR.rotation.y = t * 9; r.rotation.y = Math.sin(t * 2) * .3; p.legR.rotation.x = Math.sin(t * 4) * .3; p.legL.rotation.x = -Math.sin(t * 4) * .3; },
  plywanie: (t, p, r) => { r.rotation.x = 1.3; r.position.y = 8; r.position.z = -10; p.armR.rotation.x = -3.14 + (t * 5 % (2 * PI)); p.armL.rotation.x = -3.14 + ((t * 5 + PI) % (2 * PI)); p.legR.rotation.x = Math.sin(t * 10) * .3; p.legL.rotation.x = -Math.sin(t * 10) * .3; },
  zwyciestwo: (t, p, r) => { const u = sm(Math.abs(Math.sin(t * 3))); p.armR.rotation.z = -2.9 * u; p.armL.rotation.x = -.6; r.position.y = u * 2; p.head.rotation.x = -.3 * u; },
  placz: (t, p) => { p.armR.rotation.x = p.armL.rotation.x = -2.2; p.armR.rotation.z = .5; p.armL.rotation.z = -.5; p.head.rotation.x = .3; p.head.rotation.z = Math.sin(t * 14) * .05; },
  myslenie: (t, p) => { p.armR.rotation.x = -2; p.armR.rotation.z = .45; p.armL.rotation.x = -.6; p.armL.rotation.z = -.5; p.head.rotation.z = .2 + Math.sin(t) * .05; p.head.rotation.x = -.15; },
  moonwalk: (t, p, r) => { const u = (t % 3) / 3; r.position.z = 8 - u * 16; const w = Math.sin(t * 6); p.legR.rotation.x = Math.max(0, w) * .5; p.legL.rotation.x = Math.max(0, -w) * .5; p.armR.rotation.x = -w * .4; p.armL.rotation.x = w * .4; p.head.rotation.y = -.3; },
  headbang: (t, p, r) => { const b = Math.sin(t * 10); p.head.rotation.x = .2 + b * .45; r.rotation.x = b * .06; p.armL.rotation.x = -1.1; p.armL.rotation.z = -.4; p.armR.rotation.x = -.5 + Math.sin(t * 20) * .25; },
  samolot: (t, p, r) => { p.armR.rotation.z = -1.57; p.armL.rotation.z = 1.57; r.rotation.z = Math.sin(t * 1.8) * .35; r.rotation.y = Math.sin(t * .9) * .6; p.legR.rotation.x = Math.sin(t * 6) * .3; p.legL.rotation.x = -Math.sin(t * 6) * .3; },
  zegnaj: (t, p) => { p.armR.rotation.z = -2.6 - Math.sin(t * 8) * .3; p.armL.rotation.z = 2.6 + Math.sin(t * 8 + 1) * .3; },
  joga: (t, p, r) => { p.legR.rotation.x = -.9; p.legR.rotation.z = .9; p.armR.rotation.z = -2.9; p.armL.rotation.z = 2.9; r.rotation.z = Math.sin(t * 1.2) * .04; },
  biodra: (t, p, r) => { const w = Math.sin(t * 7); p.armR.rotation.x = w * .7; p.armL.rotation.x = w * .7; p.armR.rotation.z = .35 * (w > 0 ? 1 : -1) + .1; p.armL.rotation.z = .35 * (w > 0 ? 1 : -1) - .1; r.position.x = -w * 1.4; r.rotation.z = w * .08; },
  kolysanie: (t, p, r) => { const w = Math.sin(t * 3); p.armR.rotation.z = -2.7 - w * .2; p.armL.rotation.z = 2.7 - w * .2; r.rotation.z = w * .15; r.position.x = w * 1.2; p.head.rotation.z = w * .15; },
  shuffle: (t, p, r) => { const w = Math.sin(t * 10); p.legR.rotation.z = Math.max(0, w) * -.5; p.legL.rotation.z = Math.max(0, -w) * .5; p.legR.rotation.x = Math.max(0, -w) * -.4; p.legL.rotation.x = Math.max(0, w) * -.4; r.position.y = Math.abs(w) * .8; p.armR.rotation.x = -.6 + w * .3; p.armL.rotation.x = -.6 - w * .3; },
  robot2: (t, p, r) => { const k = Math.floor(t * 6), st = k % 6; const j = HSH(k, 1, 3) > .7 ? .08 : 0; p.armR.rotation.x = [-1.57, -1.57, 0, 0, -1.57, -.8][st] + j; p.armL.rotation.x = [0, -1.57, -1.57, -.8, 0, -1.57][st]; p.armR.rotation.z = st > 3 ? -1.2 : 0; p.head.rotation.y = [0, .5, .5, -.5, -.5, 0][st]; r.rotation.y = [0, 0, .3, .3, -.3, 0][st]; },
  fala: (t, p, r) => { const ph = t * 6; p.armL.rotation.z = 1.57 + Math.sin(ph) * .4; p.armR.rotation.z = -1.57 + Math.sin(ph - 2) * .4; p.head.rotation.z = Math.sin(ph - 1) * .2; r.rotation.z = Math.sin(ph - 1) * .05; },
  klaskanie2: (t, p, r) => { const u = Math.abs(Math.sin(t * 5)); p.armR.rotation.z = -2.6 - u * .35; p.armL.rotation.z = 2.6 + u * .35; r.position.y = u * 3; },
  breakdance: (t, p, r) => { r.rotation.order = 'YXZ'; r.rotation.y = t * 7; r.rotation.x = -1.45; r.position.y = 3; p.legR.rotation.x = .6 + Math.sin(t * 7) * .5; p.legL.rotation.x = .6 - Math.sin(t * 7) * .5; p.legR.rotation.z = -.5; p.legL.rotation.z = .5; p.armR.rotation.z = -1.4; p.armL.rotation.z = 1.4; },
  reka: (t, p, r) => { r.rotation.x = PI; r.position.y = 36; p.armR.rotation.x = p.armL.rotation.x = PI; p.armR.rotation.z = -.15; p.armL.rotation.z = .15; p.legR.rotation.x = Math.sin(t * 2) * .35 + .2; p.legL.rotation.x = -Math.sin(t * 2) * .35 - .2; r.rotation.z = Math.sin(t * 1.5) * .04; },
  solowka: (t, p, r) => { r.position.y = -6; p.legR.rotation.x = p.legL.rotation.x = -1.57; r.rotation.x = -.45; p.head.rotation.x = -.5 + Math.abs(Math.sin(t * 6)) * .3; p.armL.rotation.x = -1.2; p.armL.rotation.z = -.6; p.armR.rotation.x = -.5 + Math.sin(t * 16) * .3; p.armR.rotation.z = .5; },
  rycerz: (t, p, r) => { const k = sm(Math.min(1, (t % 4) / .6)); r.position.y = -5 * k; p.legR.rotation.x = -1.57 * k; p.legL.rotation.x = .9 * k; p.head.rotation.x = .5 * k; p.armR.rotation.x = -1.4 * k; p.armR.rotation.z = .4 * k; p.armL.rotation.x = -.5 * k; },
  buziak: (t, p) => { const u = (t % 2) / 2; const k = u < .4 ? sm(u / .4) : u < .6 ? 1 : sm(1 - (u - .6) / .4); p.armR.rotation.x = -2.4 + (u > .4 && u < .7 ? sm((u - .4) / .3) * 1 : 0); p.armR.rotation.z = .5 * k; p.head.rotation.x = -.1 * k; },
  chodztu: (t, p) => { p.armR.rotation.x = -1.4 + Math.abs(Math.sin(t * 6)) * .5; p.armR.rotation.z = .2; p.head.rotation.z = .15; },
  ziewanie: (t, p, r) => { const u = sm(Math.abs(Math.sin(t * .9))); p.armR.rotation.z = -2.9 * u; p.armL.rotation.z = 2.9 * u; p.head.rotation.x = -.45 * u; r.rotation.x = -.06 * u; },
  rozciaganie: (t, p, r) => { const w = Math.sin(t * 1.6); p.armR.rotation.z = -2.8; p.armL.rotation.z = 2.8; r.rotation.z = w * .3; p.head.rotation.z = w * .2; },
  kopanie: (t, p, r) => { const u = Math.sin(t * 3.5); p.armR.rotation.x = -.6 + u * .9; p.armL.rotation.x = -.9 + u * .9; r.rotation.x = .25 + u * .15; p.legR.rotation.x = -.3; },
  wedka: (t, p) => { const u = (t % 3) / 3; const c = u < .25 ? -2.8 * sm(u / .25) : -2.8 + sm(Math.min(1, (u - .25) / .1)) * 1.8; p.armR.rotation.x = c; p.armL.rotation.x = c * .8; p.head.rotation.x = u > .4 ? .25 : -.1; },
  zonglerka: (t, p, r) => { const u = Math.abs(Math.sin(t * 5)); p.legR.rotation.x = -u * 1.1; p.armR.rotation.z = -.5; p.armL.rotation.z = .5; p.head.rotation.x = .35; r.position.y = Math.abs(Math.sin(t * 5 + 1.5)) * .5; },
  kibic: (t, p, r) => { const w = Math.sin(t * 4); p.armR.rotation.z = -2.6 + w * .4; p.armL.rotation.z = 2.6 + w * .4; r.rotation.z = w * .1; r.position.y = Math.abs(Math.sin(t * 8)) * 1.5; },
  kciuk: (t, p) => { const k = sm(Math.min(1, (t % 2.5) / .3)); p.armR.rotation.x = -1.5 * k; p.armR.rotation.z = .1; p.head.rotation.z = -.15 * k; p.head.rotation.x = Math.sin(t * 5) * .05; },
  krab: (t, p, r) => { const w = Math.sin(t * 6); r.position.x = Math.sin(t * 1.5) * 6; p.legR.rotation.z = -.45 - w * .2; p.legL.rotation.z = .45 - w * .2; r.position.y = -2; p.armR.rotation.z = -1.4; p.armL.rotation.z = 1.4; p.armR.rotation.x = p.armL.rotation.x = -.3 + Math.abs(w) * -.6; },
  kurczak: (t, p, r) => { const w = Math.abs(Math.sin(t * 9)); p.armR.rotation.z = -.2 - w * .9; p.armL.rotation.z = .2 + w * .9; p.armR.rotation.x = p.armL.rotation.x = .4; p.head.rotation.x = Math.sin(t * 9) * .3; r.position.y = w * .8; p.legR.rotation.x = Math.sin(t * 4.5) * .4; p.legL.rotation.x = -Math.sin(t * 4.5) * .4; },
  disco: (t, p, r) => { const up = Math.sin(t * 4) > 0; p.armR.rotation.z = up ? -2.6 : -.5; p.armR.rotation.x = up ? 0 : -.6; p.armL.rotation.z = .2; p.armL.rotation.x = -.3; r.rotation.y = up ? -.25 : .25; p.legR.rotation.x = up ? -.2 : 0; r.position.y = Math.abs(Math.sin(t * 8)) * .6; },
  skakanka: (t, p, r) => { const u = t * 6; r.position.y = Math.abs(Math.sin(u)) * 4; p.armR.rotation.x = p.armL.rotation.x = Math.sin(u * 2) * .3; p.armR.rotation.z = -.5; p.armL.rotation.z = .5; p.legR.rotation.x = p.legL.rotation.x = -Math.abs(Math.sin(u)) * .2; },
  surfer: (t, p, r) => { r.rotation.y = 1.2; r.rotation.z = Math.sin(t * 1.4) * .12; r.position.y = -2; p.legR.rotation.z = -.45; p.legL.rotation.z = .45; p.armR.rotation.z = -1.3 + Math.sin(t * 1.4) * .2; p.armL.rotation.z = 1.3 + Math.sin(t * 1.4) * .2; p.head.rotation.y = -1; },
  gwiazda: (t, p, r) => { const u = (t % 2) / 2, a = sm(Math.min(1, u / .7)) * PI * 2; r.rotation.z = a; r.position.y = 16 - 16 * Math.cos(a); r.position.x = 16 * Math.sin(a); p.armR.rotation.z = -2.8; p.armL.rotation.z = 2.8; p.legR.rotation.z = -.6; p.legL.rotation.z = .6; },
  wiatrak: (t, p) => { p.armR.rotation.x = -t * 8; p.armL.rotation.x = -t * 8 + PI; p.armR.rotation.z = -.15; p.armL.rotation.z = .15; },
  zawrot: (t, p, r) => { r.rotation.z = Math.sin(t * 2) * .15; r.rotation.x = Math.cos(t * 2) * .08; p.head.rotation.z = Math.sin(t * 4) * .3; p.head.rotation.x = Math.cos(t * 4) * .2; p.armR.rotation.z = -.4 + Math.sin(t * 3) * .2; p.armL.rotation.z = .4 + Math.cos(t * 3) * .2; },
  tadam: (t, p, r) => { const k = sm(Math.min(1, (t % 2.2) / .35)); p.armR.rotation.z = -2.3 * k; p.armL.rotation.z = 2.3 * k; p.armR.rotation.x = p.armL.rotation.x = -.4 * k; r.position.y = Math.sin(Math.min(1, (t % 2.2) / .35) * PI) * 2; p.legR.rotation.z = -.15 * k; },
  medal: (t, p) => { p.armR.rotation.x = -2.9; p.armR.rotation.z = -.2 + Math.sin(t * 3) * .1; p.armL.rotation.x = -2.9; p.armL.rotation.z = .2 - Math.sin(t * 3) * .1; p.head.rotation.x = -.3; },
  zombie: (t, p, r) => { p.armR.rotation.x = p.armL.rotation.x = -1.57 + Math.sin(t * 2) * .08; const w = Math.sin(t * 3) * .5; p.legR.rotation.x = w; p.legL.rotation.x = -w; r.rotation.z = Math.sin(t * 1.5) * .06; p.head.rotation.z = .2; },
};
EMOTES.hulajnoga = (t, p, r) => { r.position.y = 3.2; p.legR.rotation.x = -.18; p.legL.rotation.x = .18; p.armR.rotation.x = p.armL.rotation.x = -1.05; p.armR.rotation.z = .12; p.armL.rotation.z = -.12; r.rotation.x = -.05; p.head.rotation.x = -.05; };
/* emotki, które grają dalej, kiedy chodzisz (ruszają się tylko ręce i głowa) */
const MOVE = new Set(['machanie', 'klaskanie', 'facepalm', 'dab', 'wskazuje', 'myslenie', 'serce', 'zegnaj', 'imprezka', 'zwyciestwo', 'gitara', 'boks', 'headbang', 'fala', 'salut', 'lasso', 'placz', 'wzruszenie', 'hulajnoga', 'buziak', 'chodztu', 'kciuk', 'tadam', 'medal', 'kibic', 'ziewanie', 'wiatrak', 'disco']);
/* przedmioty do emotek: hulajnoga elektryczna w stylu Void */
function makeScooter() {
  const g = new T.Group(); const dark = lam(0x18181b), grey = lam(0x3f3f46, 0x0a0a0a), acc = new T.MeshBasicMaterial({ color: 0xa855f7 });
  const deck = box(5, 1, 15, dark); deck.position.set(0, 2.2, 0); g.add(deck);
  const grip = box(4.4, .2, 12, lam(0x27272a)); grip.position.set(0, 2.8, -.5); g.add(grip);
  const glow = box(5.4, .3, 15.4, acc); glow.position.set(0, 1.65, 0); g.add(glow);
  const ug = new T.Mesh(new T.PlaneGeometry(9, 19), new T.MeshBasicMaterial({ color: 0xa855f7, transparent: true, opacity: .35, blending: T.AdditiveBlending, depthWrite: false })); ug.rotation.x = -PI / 2; ug.position.y = .15; g.add(ug);
  const wheels = [[7.8], [-7.8]].map(([z]) => { const w = new T.Group(); w.position.set(0, 2, z); const tyre = new T.Mesh(new T.CylinderGeometry(2, 2, 1.6, 16), lam(0x0a0a0a)); tyre.rotation.z = PI / 2; const hub = new T.Mesh(new T.CylinderGeometry(1.1, 1.1, 1.7, 8), acc); hub.rotation.z = PI / 2; w.add(tyre, hub); g.add(w); return w; });
  const stem = box(1, 20, 1, grey); stem.position.set(0, 12, 7.2); stem.rotation.x = -.12; g.add(stem);
  const bar = box(11, .8, .8, grey); bar.position.set(0, 21.8, 8.3); g.add(bar);
  [1, -1].forEach(s => { const h = box(2.2, 1, 1, dark); h.position.set(s * 5.4, 21.8, 8.3); g.add(h); });
  const lamp = box(1.8, 1.2, .5, new T.MeshBasicMaterial({ color: 0xfef9c3 })); lamp.position.set(0, 19.5, 8.4); g.add(lamp);
  const beam = new T.Mesh(new T.ConeGeometry(3, 10, 12, 1, true), new T.MeshBasicMaterial({ color: 0xfef9c3, transparent: true, opacity: .12, blending: T.AdditiveBlending, depthWrite: false, side: T.DoubleSide })); beam.rotation.x = -PI / 2; beam.position.set(0, 19.5, 13.6); g.add(beam);
  const screen = box(2, 1.2, .3, new T.MeshBasicMaterial({ color: 0x22d3ee })); screen.position.set(0, 22.3, 8); screen.rotation.x = -.6; g.add(screen);
  return { obj: g, update(t, walk) { wheels.forEach(w => w.rotation.x = t * (2 + walk * 14)); glow.material.color.setHSL(.78 + Math.sin(t * 2) * .04, .9, .6); ug.material.opacity = .25 + Math.sin(t * 3) * .1; } };
}
const PROPS = { hulajnoga: makeScooter };
const EMOTE_NAMES = [['machanie', 'Machanie'], ['uklon', 'Ukłon'], ['facepalm', 'Facepalm'], ['klaskanie', 'Klaskanie'], ['taniec', 'Taniec'], ['robot', 'Taniec robota'], ['dab', 'Dab'], ['krecio', 'Kręciołek'], ['salto', 'Salto'], ['pajacyki', 'Pajacyki'], ['wzruszenie', 'Wzruszenie ramion'], ['medytacja', 'Lewitacja'], ['salut', 'Salut'], ['pompki', 'Pompki'], ['gitara', 'Gitara powietrzna'], ['zombie', 'Zombie'], ['hura', 'Hura!'], ['imprezka', 'Ręce w górę'], ['zwyciestwo', 'Zwycięstwo'], ['serce', 'Serduszko'], ['zegnaj', 'Pożegnanie'], ['wskazuje', 'Pokazywanie'], ['myslenie', 'Myślenie'], ['placz', 'Płacz'], ['boks', 'Boks'], ['karate', 'Kopnięcie karate'], ['przysiad', 'Przysiady'], ['joga', 'Joga'], ['plywanie', 'Pływanie'], ['moonwalk', 'Moonwalk'], ['headbang', 'Headbanging'], ['lasso', 'Lasso'], ['samolot', 'Samolot'], ['drzemka', 'Drzemka'], ['biodra', 'Wymachiwanie biodrami'], ['kolysanie', 'Kołysanie'], ['shuffle', 'Shuffle'], ['robot2', 'Robot 2.0'], ['fala', 'Fala'], ['klaskanie2', 'Klaskanie nad głową'], ['breakdance', 'Breakdance'], ['reka', 'Stanie na rękach'], ['solowka', 'Solówka na kolanach'], ['rycerz', 'Ukłon rycerza'], ['hulajnoga', 'Hulajnoga elektryczna'], ['buziak', 'Buziak'], ['chodztu', 'Chodź tu'], ['kciuk', 'Kciuk w górę'], ['tadam', 'Ta-dam!'], ['medal', 'Puchar w górę'], ['kibic', 'Kibic'], ['ziewanie', 'Ziewanie'], ['rozciaganie', 'Rozciąganie'], ['zawrot', 'Zawrót głowy'], ['disco', 'Disco'], ['krab', 'Taniec kraba'], ['kurczak', 'Taniec kurczaka'], ['wiatrak', 'Wiatrak'], ['skakanka', 'Skakanka'], ['zonglerka', 'Żonglerka'], ['wedka', 'Łowienie ryb'], ['kopanie', 'Kopanie'], ['surfer', 'Surfer'], ['gwiazda', 'Gwiazda']];
const CATS = [
  { id: 'cape', name: 'Peleryny', items: CAPES.map(([id, n]) => [id, n]) },
  { id: 'wings', name: 'Skrzydła', items: [['anielskie', 'Anielskie'], ['zlote', 'Złote'], ['feniks', 'Feniks'], ['ogniste', 'Ogniste'], ['krysztalowe', 'Kryształowe 3D'], ['pikselowe', 'Pikselowe 3D'], ['galaktyczne', 'Galaktyczne'], ['cyber', 'Cyber'], ['teczowe', 'Tęczowe'], ['natury', 'Natury'], ['nietoperza', 'Nietoperza'], ['plazmowe', 'Plazmowe'], ['lawowe', 'Lawowe'], ['zorzowe', 'Zorzowe'], ['glitch', 'Glitch'], ['sniezne', 'Śnieżne'], ['kruka', 'Kruka'], ['pawie', 'Pawie'], ['rozowe', 'Różowe'], ['diamentowe', 'Diamentowe 3D'], ['ametystowe', 'Ametystowe 3D'], ['ostrza', 'Neonowe ostrza 3D'], ['ostrzalod', 'Lodowe ostrza 3D'], ['szmaragdowe', 'Szmaragdowe 3D'], ['rubinowe', 'Rubinowe 3D'], ['zlotepix', 'Złote pikselowe 3D'], ['snieznepix', 'Śnieżne pikselowe 3D'], ['szkielet', 'Szkielet smoka 3D'], ['zebatki', 'Zębatki 3D'], ['kule', 'Świetliste kule 3D'], ['demona', 'Demona'], ['smocze', 'Smocze'], ['cienia', 'Cienia'], ['lodowe', 'Lodowe'], ['kosciane', 'Kościane'], ['mechaniczne', 'Mechaniczne'], ['motyle', 'Motyle'], ['wrozki', 'Wróżki'], ['energia', 'Energia'], ['m_anielskie', 'Małe anielskie'], ['m_demona', 'Małe demona'], ['m_nietoperza', 'Małe nietoperza'], ['m_smocze', 'Małe smocze'], ['m_feniks', 'Mały feniks'], ['m_kruka', 'Małe kruka'], ['m_rozowe', 'Małe różowe'], ['m_ogniste', 'Małe ogniste'], ['m_galaktyczne', 'Małe galaktyczne'], ['m_cienia', 'Małe cienia'], ['m_lodowe', 'Małe lodowe'], ['m_energia', 'Mała energia'], ['m_motyle', 'Małe motyle'], ['m_wrozki', 'Małe wróżki'], ['m_krysztalowe', 'Małe kryształowe 3D'], ['m_pikselowe', 'Małe pikselowe 3D']] },
  { id: 'hat', name: 'Czapki', items: [['korona', 'Korona'], ['aureola', 'Aureola'], ['rogi', 'Rogi'], ['wiking', 'Hełm wikinga'], ['kot', 'Kocie uszy'], ['lis', 'Lisie uszy'], ['czarodziej', 'Czarodziej'], ['mikolaj', 'Czapka Mikołaja'], ['cylinder', 'Cylinder'], ['sluchawki', 'Słuchawki RGB'], ['okulary', 'Okulary'], ['wianek', 'Wianek'], ['dynia', 'Dynia']] },
  { id: 'pet', name: 'Zwierzaki', items: [['smok', 'Mini smok'], ['duch', 'Duszek'], ['slime', 'Slime'], ['pszczola', 'Pszczółka'], ['aksolotl', 'Aksolotl'], ['kot', 'Kotek'], ['lisek', 'Lisek'], ['pingwin', 'Pingwin'], ['sowa', 'Sowa'], ['dron', 'Dron'], ['krolik', 'Królik'], ['nietoperz', 'Nietoperz'], ['meduza', 'Meduza'], ['kurczak', 'Kurczaczek'], ['zolw', 'Żółw'], ['feniks', 'Feniks'], ['ufo', 'Mini UFO'], ['voidbot', 'Void Bot']] },
  { id: 'emote', name: 'Emotki', items: EMOTE_NAMES },
  { id: 'fx', name: 'Efekty', items: [['orbity', 'Orbity'], ['aura', 'Aura'], ['ogien', 'Pierścień ognia'], ['blyskawice', 'Błyskawice'], ['serca', 'Serduszka'], ['nuty', 'Nuty'], ['snieg', 'Śnieżyca'], ['liscie', 'Jesienne liście'], ['babelki', 'Bąbelki'], ['runy', 'Runy'], ['pyl', 'Gwiezdny pył'], ['tornado', 'Tornado'], ['krag', 'Magiczny krąg'], ['motylki', 'Motylki'], ['swietliki', 'Świetliki'], ['dziura', 'Czarna dziura'], ['platki', 'Płatki wiśni'], ['konfetti', 'Konfetti'], ['chmurka', 'Deszczowa chmurka']] },
];
const build = (cat, id) => cat === 'cape' ? (() => { const c = makeCape(); c.fn = CAPES.find(x => x[0] === id)[2]; return c; })() : cat === 'wings' ? makeWings(id) : cat === 'hat' ? makeHat(id) : cat === 'pet' ? makePet(id) : makeFx(id);

/* ---------------- scena ---------------- */
function scene(canvasEl, w, h) {
  const r = new T.WebGLRenderer({ canvas: canvasEl, antialias: true, alpha: true, preserveDrawingBuffer: true });
  r.setPixelRatio(Math.min(2, devicePixelRatio)); r.setSize(w, h, false);
  const sc = new T.Scene();
  sc.add(new T.AmbientLight(0xffffff, .62)); const d = new T.DirectionalLight(0xffffff, .55); d.position.set(30, 50, 40); sc.add(d); const d2 = new T.DirectionalLight(0xb794f6, .35); d2.position.set(-40, 20, -30); sc.add(d2);
  const cam = new T.PerspectiveCamera(32, w / h, 1, 500);
  const pl = makePlayer(); const spin = new T.Group(); spin.add(pl.root); sc.add(spin);
  return { r, sc, cam, pl, spin, worn: {} };
}
const wear = (S, cat, id) => { if (cat === 'emote') { S.emote = id; if (S.prop) { S.spin.remove(S.prop.obj); S.prop = null; } if (id && PROPS[id]) { S.prop = PROPS[id](); S.spin.add(S.prop.obj); } return; } if (S.worn[cat]) { S.worn[cat].obj.parent.remove(S.worn[cat].obj); delete S.worn[cat]; } if (id) { const o = build(cat, id); S.worn[cat] = o; if (cat === 'hat') { o.obj.position.y -= 24; S.pl.head.add(o.obj); } else if (cat === 'cape' || cat === 'wings') S.pl.root.add(o.obj); else S.spin.add(o.obj); } };
const tick = (S, t, walk) => {
  const R0 = S.pl.root; R0.position.set(0, 0, 0); R0.rotation.order = 'XYZ'; R0.rotation.set(0, 0, 0);
  [S.pl.head, S.pl.armR, S.pl.armL, S.pl.legR, S.pl.legL].forEach(o => o.rotation.set(0, 0, 0));
  if (S.emote && EMOTES[S.emote]) {
    EMOTES[S.emote](t, S.pl, R0);
    if (walk > .01 && MOVE.has(S.emote) && S.emote !== 'hulajnoga') { const w = Math.sin(t * 6) * .6 * walk; S.pl.legR.rotation.x = w; S.pl.legL.rotation.x = -w; R0.position.y += Math.abs(Math.sin(t * 6)) * .6 * walk; }
    if (S.prop) S.prop.update(t, walk);
    if (S.emote === 'hulajnoga') { R0.rotation.z = Math.sin(t * 1.3) * .03 * (1 + walk); S.spin.position.z = 0; }
    Object.values(S.worn).forEach(o => o.update && o.update(t, walk * .6)); return; }
  const sw = Math.sin(t * 6) * .6 * walk;
  S.pl.legR.rotation.x = sw; S.pl.legL.rotation.x = -sw; S.pl.armR.rotation.x = -sw; S.pl.armL.rotation.x = sw;
  S.pl.armR.rotation.z = .05 + Math.sin(t * 1.3) * .03; S.pl.armL.rotation.z = -.05 - Math.sin(t * 1.3) * .03;
  S.pl.head.rotation.y = Math.sin(t * .6) * .15;
  Object.values(S.worn).forEach(o => o.update && o.update(t, walk));
};

/* ---------------- podglądy 3D (ekran główny i kosmetyki) ---------------- */
const equip = { cape: 'pustka', wings: null, hat: null, pet: null, fx: null, emote: null };
const viewers = [];
function viewer(el, w, h, cam, look, startYaw) {
  const S = scene(el, w, h); S.cam.position.set(...cam); S.cam.lookAt(...look);
  Object.entries(equip).forEach(([c, id]) => wear(S, c, id));
  const v = { S, el, yaw: startYaw, drag: null, idle: 3, walking: false, walkAmt: 0 };
  el.addEventListener('pointerdown', e => { v.drag = e.clientX; v.idle = 0; el.setPointerCapture(e.pointerId); });
  el.addEventListener('pointermove', e => { if (v.drag !== null) { v.yaw += (e.clientX - v.drag) * .012; v.drag = e.clientX; } });
  ['pointerup', 'pointercancel'].forEach(n => el.addEventListener(n, () => { v.drag = null; }));
  viewers.push(v); return v;
}
const site = document.getElementById('site3d');
const vSite = site ? viewer(site, 460, 520, [0, 20, 118], [0, 18, 0], PI * .8) : null;
if (vSite) vSite.solo = true;
if (vSite) {
  const nowEl = document.getElementById('wsNow');
  const looks = [['wings', 'krysztalowe', 'Skrzydła Kryształowe 3D'], ['hat', 'korona', 'Korona'], ['pet', 'ufo', 'Mini UFO'], ['fx', 'tornado', 'Tornado'], ['wings', 'feniks', 'Skrzydła Feniks'], ['cape', 'portal', 'Peleryna Portal'], ['pet', 'meduza', 'Meduza'], ['wings', 'zebatki', 'Skrzydła Zębatki 3D'], ['hat', 'wiking', 'Hełm wikinga'], ['fx', 'krag', 'Magiczny krąg'], ['wings', 'ostrza', 'Neonowe ostrza 3D'], ['emote', 'taniec', 'Emotka: Taniec'], ['emote', null, 'Bez emotki']];
  let li = 0; const step = () => { const [c2, id, n] = looks[li % looks.length]; wear(vSite.S, c2, id); if (nowEl) nowEl.textContent = n; li++; };
  step(); step(); setInterval(step, 2600);
}

const reduce = matchMedia('(prefers-reduced-motion:reduce)').matches;
const t0 = performance.now(); let last = t0;
function frame(now) {
  const t = reduce ? 1.3 : (now - t0) / 1000, dt = Math.min(.05, (now - last) / 1000); last = now;
  viewers.forEach(v => {
    const r = v.el.getBoundingClientRect();
    if (r.bottom < 0 || r.top > innerHeight || document.hidden) return;
    if (v.drag === null) { v.idle += dt; if (v.idle > 2.5) v.yaw += dt * .45; }
    v.S.spin.rotation.y = v.yaw; tick(v.S, t, 0); v.S.r.render(v.S.sc, v.S.cam);
  });
  requestAnimationFrame(frame);
}
requestAnimationFrame(frame);
}
