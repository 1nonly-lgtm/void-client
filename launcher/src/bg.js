/* Tło launchera 2.0 (jedno dla wszystkich): zorza, zaćmienie z jasną krawędzią, gwiazdy, warstwy gór we mgle i unoszące się iskry. */
{
const cv = document.getElementById('bgcv'); const g = cv.getContext('2d');
let W = 1280, H = 720, EX = 905, EY = 255, ER = 150, layers = [], stars = [], motes = [];
const R = (s => () => (s = (s * 16807) % 2147483647) / 2147483647)(5);
/* góry z klocków: trzy warstwy, każda gładko opada we mgłę */
const ridge = (seed, step, base, amp) => { base = Math.round(base); const r = (s => () => (s = (s * 16807) % 2147483647) / 2147483647)(seed); const pts = []; let h = base; for (let x = 0; x <= W + step * 4; x += step) { h += (r() - .5) * amp; h = Math.max(base - amp * 2, Math.min(base + amp * 1.2, h)); pts.push(Math.round(h / 6) * 6); } return pts; };
function resize() {
  W = window.innerWidth || 1180; H = window.innerHeight || 720;
  const DPR = Math.min(1.5, devicePixelRatio || 1);
  cv.width = Math.round(W * DPR); cv.height = Math.round(H * DPR); g.setTransform(DPR, 0, 0, DPR, 0, 0);
  const k = H / 720;
  layers = [
    { pts: ridge(3, 48, 585 * k, 30), step: 48, col: [34, 18, 64], spd: 2, fog: .3 },
    { pts: ridge(9, 36, 630 * k, 26), step: 36, col: [20, 11, 40], spd: 5, fog: .2 },
    { pts: ridge(17, 28, 676 * k, 20), step: 28, col: [10, 6, 20], spd: 9, fog: .0 },
  ];
  EX = W * .66; EY = H * .34; ER = Math.min(150, H * .2); // zaćmienie za postacią
  stars = Array.from({ length: Math.round(W * H / 4200) }, () => ({ x: R() * W, y: R() * H * .75, r: R() < .08 ? 1.5 : R() < .4 ? 1 : .6, p: R() * 6, s: .4 + R() * 1.6, d: .2 + R() * .8 }));
  motes = Array.from({ length: 40 }, () => ({ x: R() * W, y: R() * H, r: .8 + R() * 1.8, v: 6 + R() * 16, p: R() * 6, h: R() < .5 ? 270 : 320 }));
}
let shoot = null, nextShoot = 3;
const reduce = matchMedia('(prefers-reduced-motion:reduce)').matches;
const still = () => reduce || !document.body.classList.contains('anim');
const t0 = performance.now();
resize(); window.addEventListener('resize', () => { resize(); if (still()) draw(6); });
const AC = document.createElement('canvas'); AC.width = 256; AC.height = 144; const ag = AC.getContext('2d');
function aurora(t) {
  const sx = 256 / W; ag.clearRect(0, 0, 256, 144); ag.globalCompositeOperation = 'lighter';
  [[300, 120, .9, 0], [190, 170, .7, 2.1], [320, 90, .55, 4.2]].forEach(([hue, y0, a, ph]) => {
    for (let k = 0; k < 3; k++) {
      ag.beginPath();
      for (let x = -40; x <= W + 40; x += 20) { const y = y0 + Math.sin(x * .004 + t * .18 + ph) * 46 + Math.sin(x * .011 - t * .27 + ph * 2) * 18 + k * 10; x === -40 ? ag.moveTo(x * sx, y * sx) : ag.lineTo(x * sx, y * sx); }
      const gr = ag.createLinearGradient(0, 0, 256, 0);
      gr.addColorStop(0, `hsla(${hue},90%,60%,0)`); gr.addColorStop(.3, `hsla(${hue},90%,62%,${.09 * a})`); gr.addColorStop(.6, `hsla(${hue + 30},90%,65%,${.16 * a})`); gr.addColorStop(1, `hsla(${hue},90%,60%,0)`);
      ag.strokeStyle = gr; ag.lineWidth = (60 - k * 16) * sx; ag.stroke();
    }
  });
  g.save(); g.globalCompositeOperation = 'lighter'; g.imageSmoothingEnabled = true; g.imageSmoothingQuality = 'high'; g.drawImage(AC, 0, 0, W, H); g.restore();
}
function eclipse(t) {
  const y = EY + Math.sin(t * .35) * 6;
  // poświata
  const halo = g.createRadialGradient(EX, y, ER * .9, EX, y, ER * 2.6);
  halo.addColorStop(0, 'rgba(192,132,252,.38)'); halo.addColorStop(.35, 'rgba(147,51,234,.14)'); halo.addColorStop(1, 'rgba(147,51,234,0)');
  g.fillStyle = halo; g.fillRect(EX - ER * 2.7, y - ER * 2.7, ER * 5.4, ER * 5.4);
  // jasna krawędź (sierp światła u góry z lewej)
  g.save(); g.shadowColor = 'rgba(233,213,255,.95)'; g.shadowBlur = 40;
  const rim = g.createLinearGradient(EX - ER, y - ER, EX + ER, y + ER);
  rim.addColorStop(0, '#fdf4ff'); rim.addColorStop(.35, '#d8b4fe'); rim.addColorStop(.7, 'rgba(168,85,247,.25)'); rim.addColorStop(1, 'rgba(168,85,247,0)');
  g.beginPath(); g.arc(EX, y, ER + 2.5, 0, Math.PI * 2); g.fillStyle = rim; g.fill(); g.restore();
  // ciemna tarcza
  const disk = g.createRadialGradient(EX + ER * .25, y + ER * .25, ER * .2, EX, y, ER);
  disk.addColorStop(0, '#0d0818'); disk.addColorStop(1, '#170d2c');
  g.beginPath(); g.arc(EX, y, ER, 0, Math.PI * 2); g.fillStyle = disk; g.fill();
  // subtelny pierścień
  g.save(); g.translate(EX, y); g.rotate(-.22);
  const rg = g.createLinearGradient(-ER * 2, 0, ER * 2, 0); rg.addColorStop(0, 'rgba(216,180,254,0)'); rg.addColorStop(.5, 'rgba(216,180,254,.55)'); rg.addColorStop(1, 'rgba(216,180,254,0)');
  g.strokeStyle = rg; g.lineWidth = 1.5; g.beginPath(); g.ellipse(0, 0, ER * 1.9, ER * .34, 0, -.08, Math.PI + .08); g.stroke();
  const a = t * .5; g.fillStyle = '#f5d0fe'; g.shadowColor = '#e879f9'; g.shadowBlur = 12;
  const px = Math.cos(a) * ER * 1.9, py = Math.sin(a) * ER * .34; if (Math.sin(a) > -.1) { g.beginPath(); g.arc(px, py, 3, 0, 7); g.fill(); }
  g.restore();
}
function mountains(t) {
  layers.forEach((L, i) => {
    const off = (t * L.spd) % L.step, k0 = Math.floor(t * L.spd / L.step);
    g.beginPath(); g.moveTo(-L.step, H);
    for (let j = 0; j < L.pts.length - 2; j++) { const x = j * L.step - off, y = L.pts[(j + k0) % L.pts.length]; g.lineTo(x, y); g.lineTo(x + L.step, y); }
    g.lineTo(W + L.step, H); g.closePath();
    const [r, gg, b] = L.col; const gr = g.createLinearGradient(0, H * .75, 0, H);
    gr.addColorStop(0, `rgb(${r},${gg},${b})`); gr.addColorStop(1, `rgb(${r * .5 | 0},${gg * .5 | 0},${b * .6 | 0})`);
    g.fillStyle = gr; g.fill();
    if (i === 0) { g.strokeStyle = 'rgba(196,181,253,.25)'; g.lineWidth = 1.5; g.stroke(); }
    if (L.fog) { const f = g.createLinearGradient(0, H * .78 + i * 40, 0, H * .97); f.addColorStop(0, 'rgba(124,58,237,0)'); f.addColorStop(1, `rgba(76,29,149,${L.fog})`); g.fillStyle = f; g.fillRect(0, H * .72, W, H * .28); }
  });
}
function frame(now) {
  if (!document.hidden && !still()) draw((now - t0) / 1000);
  requestAnimationFrame(frame);
}
let drewStill = false;
setInterval(() => { if (still() && !drewStill) { draw(6); drewStill = true; } if (!still()) drewStill = false; }, 500);
function draw(t) {
  g.clearRect(0, 0, W, H);
  stars.forEach(s => { const x = (s.x - t * s.d * 3 + W) % W; g.globalAlpha = .2 + .8 * Math.abs(Math.sin(t * s.s * .5 + s.p)); g.fillStyle = s.r > 1.2 ? '#fdf4ff' : '#e9d5ff'; g.beginPath(); g.arc(x, s.y, s.r, 0, 7); g.fill(); });
  g.globalAlpha = 1;
  aurora(t);
  if (t > nextShoot && !shoot) shoot = { x: W * .35 + R() * W * .55, y: 40 + R() * 120, t };
  if (shoot) { const u = (t - shoot.t) / 1.2; if (u > 1) { shoot = null; nextShoot = t + 5 + R() * 6; } else { const e = u * u, x = shoot.x - e * 380, y = shoot.y + e * 190, al = Math.sin(u * Math.PI); const gr = g.createLinearGradient(x, y, x + 140, y - 70); gr.addColorStop(0, `rgba(255,255,255,${al})`); gr.addColorStop(1, 'rgba(216,180,254,0)'); g.strokeStyle = gr; g.lineWidth = 1.8; g.beginPath(); g.moveTo(x, y); g.lineTo(x + 140, y - 70); g.stroke(); } }
  eclipse(t);
  mountains(t);
  g.save(); g.globalCompositeOperation = 'lighter';
  motes.forEach(m => { const y = (m.y - t * m.v + H * 2) % (H + 40) - 20, x = m.x + Math.sin(t * .6 + m.p) * 14; const al = .35 + .35 * Math.sin(t * 2 + m.p); const gr = g.createRadialGradient(x, y, 0, x, y, m.r * 4); gr.addColorStop(0, `hsla(${m.h},90%,75%,${al})`); gr.addColorStop(1, `hsla(${m.h},90%,60%,0)`); g.fillStyle = gr; g.fillRect(x - m.r * 4, y - m.r * 4, m.r * 8, m.r * 8); });
  g.restore();
}
draw(6);
requestAnimationFrame(frame);
}
