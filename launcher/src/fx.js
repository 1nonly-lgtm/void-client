'use strict';
// Fioletowe wybuchy i iskry launchera.
// Dwie warstwy: "back" (za interfejsem – wybuchy w tle) i "front" (nad interfejsem – kliknięcia, przycisk GRAJ).
// Pętla rysowania działa tylko wtedy, gdy coś jest na ekranie, więc w spoczynku nie zjada procesora.

(() => {
  const PALETTE = ['#ffffff', '#f5e9ff', '#f0abfc', '#e879f9', '#c084fc', '#a855f7', '#9333ea', '#7c3aed'];
  const reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const enabled = () => !reduce && document.body.classList.contains('anim') && !document.hidden;

  function layer(cls) {
    const c = document.createElement('canvas');
    c.className = 'fx ' + cls;
    c.setAttribute('aria-hidden', 'true');
    document.body.appendChild(c);
    return { c, ctx: c.getContext('2d'), parts: [], rings: [], flashes: [] };
  }
  const back = layer('fx-back');
  const front = layer('fx-front');
  let W = 0, H = 0, DPR = 1;

  function resize() {
    DPR = Math.min(2, window.devicePixelRatio || 1);
    W = window.innerWidth; H = window.innerHeight;
    for (const L of [back, front]) {
      L.c.width = Math.round(W * DPR);
      L.c.height = Math.round(H * DPR);
      L.ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
      L.ctx.imageSmoothingEnabled = false;
    }
  }
  resize();
  window.addEventListener('resize', resize);

  const rnd = (a, b) => a + Math.random() * (b - a);
  const pick = arr => arr[(Math.random() * arr.length) | 0];

  // ───────── tworzenie efektów ─────────

  function spark(L, x, y, o = {}) {
    const ang = o.ang ?? rnd(0, Math.PI * 2);
    const sp = o.speed ?? rnd(1, 4);
    L.parts.push({
      x, y,
      vx: Math.cos(ang) * sp, vy: Math.sin(ang) * sp,
      g: o.g ?? 0.06, drag: o.drag ?? 0.965,
      size: o.size ?? rnd(2, 5),
      life: 0, max: o.life ?? rnd(35, 70),
      color: o.color ?? pick(PALETTE),
      kind: o.kind ?? (Math.random() < 0.75 ? 'px' : 'star'),
      spin: rnd(-0.2, 0.2), rot: rnd(0, Math.PI)
    });
  }

  function ring(L, x, y, r, o = {}) {
    L.rings.push({ x, y, r: o.r0 ?? 4, to: r, life: 0, max: o.life ?? 38, w: o.w ?? 3, color: o.color ?? '#d8b4fe' });
  }

  function flash(L, x, y, r, o = {}) {
    L.flashes.push({ x, y, r, life: 0, max: o.life ?? 22, a: o.a ?? 0.55 });
  }

  /** Mały wybuch – kliknięcie przycisku. */
  function pop(x, y, power = 1) {
    if (!enabled()) return;
    const n = Math.round(14 * power);
    for (let i = 0; i < n; i++) spark(front, x, y, { speed: rnd(1.2, 3.6) * power, life: rnd(22, 42), size: rnd(2, 4) });
    ring(front, x, y, 26 * power, { life: 20, w: 2 });
    flash(front, x, y, 28 * power, { life: 12, a: 0.45 });
    run();
  }

  /** Duży wybuch – przycisk GRAJ, start gry. */
  function boom(x, y, power = 1) {
    if (!enabled()) return;
    flash(front, x, y, 220 * power, { life: 30, a: 0.75 });
    ring(front, x, y, 170 * power, { life: 34, w: 5, color: '#f0abfc' });
    ring(front, x, y, 260 * power, { life: 48, w: 2, color: '#a855f7', r0: 20 });
    for (let i = 0; i < 90 * power; i++) {
      spark(front, x, y, { speed: rnd(2, 10) * power, life: rnd(40, 90), size: rnd(2, 6), g: 0.09, drag: 0.955 });
    }
    // iskry jak cząsteczki portalu Endu – wolno unoszą się w górę
    for (let i = 0; i < 26; i++) {
      spark(front, x + rnd(-70, 70), y + rnd(-20, 20), { ang: rnd(-Math.PI * 0.85, -Math.PI * 0.15), speed: rnd(0.4, 1.4), g: -0.012, drag: 0.99, life: rnd(80, 140), size: rnd(2, 3), kind: 'px', color: pick(['#e9d5ff', '#c084fc', '#f0abfc']) });
    }
    // spowolnione echo wybuchu w tle
    flash(back, x, y, 420 * power, { life: 60, a: 0.35 });
    ring(back, x, y, 520 * power, { life: 70, w: 2, color: '#7c3aed', r0: 60 });
    shake(power);
    run();
  }

  /** Cichy wybuch w tle co kilka sekund. */
  function ambient(x, y) {
    if (!enabled()) return;
    const p = rnd(0.5, 1);
    flash(back, x, y, 120 * p, { life: 45, a: 0.42 });
    ring(back, x, y, 80 * p, { life: 55, w: 1.5, color: '#a855f7' });
    ring(back, x, y, 140 * p, { life: 70, w: 1, color: '#f0abfc', r0: 10 });
    for (let i = 0; i < 40 * p; i++) spark(back, x, y, { speed: rnd(0.6, 3.2), life: rnd(55, 100), size: rnd(2, 4), g: 0.015, drag: 0.975 });
    run();
  }

  function shake(power) {
    const main = document.querySelector('main');
    if (!main || !main.animate) return;
    const a = 5 * power;
    main.animate([
      { transform: 'translate(0,0)' },
      { transform: `translate(${-a}px,${a * 0.6}px)` },
      { transform: `translate(${a * 0.8}px,${-a * 0.5}px)` },
      { transform: `translate(${-a * 0.4}px,${-a * 0.3}px)` },
      { transform: 'translate(0,0)' }
    ], { duration: 320, easing: 'ease-out' });
  }

  // ───────── rysowanie ─────────

  function step(L) {
    const ctx = L.ctx;
    ctx.clearRect(0, 0, W, H);
    ctx.globalCompositeOperation = 'lighter';

    for (let i = L.flashes.length - 1; i >= 0; i--) {
      const f = L.flashes[i];
      f.life++;
      const t = f.life / f.max;
      if (t >= 1) { L.flashes.splice(i, 1); continue; }
      const a = f.a * (1 - t) * (1 - t);
      const g = ctx.createRadialGradient(f.x, f.y, 0, f.x, f.y, f.r * (0.6 + t * 0.6));
      g.addColorStop(0, `rgba(250,232,255,${a})`);
      g.addColorStop(0.35, `rgba(192,132,252,${a * 0.6})`);
      g.addColorStop(1, 'rgba(124,58,237,0)');
      ctx.fillStyle = g;
      ctx.beginPath();
      ctx.arc(f.x, f.y, f.r * (0.6 + t * 0.6), 0, Math.PI * 2);
      ctx.fill();
    }

    for (let i = L.rings.length - 1; i >= 0; i--) {
      const r = L.rings[i];
      r.life++;
      const t = r.life / r.max;
      if (t >= 1) { L.rings.splice(i, 1); continue; }
      const e = 1 - Math.pow(1 - t, 3);
      ctx.globalAlpha = (1 - t) * 0.9;
      ctx.strokeStyle = r.color;
      ctx.lineWidth = r.w * (1 - t * 0.7);
      ctx.beginPath();
      ctx.arc(r.x, r.y, r.r + (r.to - r.r) * e, 0, Math.PI * 2);
      ctx.stroke();
    }

    for (let i = L.parts.length - 1; i >= 0; i--) {
      const p = L.parts[i];
      p.life++;
      if (p.life >= p.max) { L.parts.splice(i, 1); continue; }
      p.vx *= p.drag; p.vy = p.vy * p.drag + p.g;
      p.x += p.vx; p.y += p.vy;
      p.rot += p.spin;
      const t = p.life / p.max;
      const s = p.size * (1 - t * 0.6);
      ctx.globalAlpha = t < 0.1 ? t * 10 : (1 - t);
      ctx.fillStyle = p.color;
      if (p.kind === 'star') {
        // mała czteroramienna gwiazdka
        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(p.rot);
        ctx.fillRect(-s * 1.4, -s * 0.25, s * 2.8, s * 0.5);
        ctx.fillRect(-s * 0.25, -s * 1.4, s * 0.5, s * 2.8);
        ctx.restore();
      } else {
        // pikselowa cząsteczka jak w Minecrafcie (ostre krawędzie)
        const q = Math.max(1, Math.round(s));
        ctx.fillRect(Math.round(p.x - q / 2), Math.round(p.y - q / 2), q, q);
      }
    }
    ctx.globalAlpha = 1;
    ctx.globalCompositeOperation = 'source-over';
    return L.parts.length + L.rings.length + L.flashes.length;
  }

  let raf = 0;
  function frame() {
    raf = 0;
    const left = step(back) + step(front);
    if (left > 0) raf = requestAnimationFrame(frame);
  }
  function run() { if (!raf) raf = requestAnimationFrame(frame); }

  // ───────── podpięcie pod interfejs ─────────

  function center(el) {
    const r = el.getBoundingClientRect();
    return [r.left + r.width / 2, r.top + r.height / 2];
  }

  document.addEventListener('pointerdown', e => {
    if (e.button !== 0) return;
    const el = e.target.closest('button, .profile, a');
    if (!el || el.disabled) return;
    if (el.id === 'playBtn') return; // GRAJ ma własny, duży wybuch
    pop(e.clientX, e.clientY, el.classList.contains('nav') ? 0.8 : el.classList.contains('switch') ? 0.7 : 1);
  }, { passive: true });

  const play = document.getElementById('playBtn');
  if (play) {
    // iskry sypiące się z krawędzi przycisku GRAJ, gdy kursor jest nad nim
    let hoverTimer = 0;
    play.addEventListener('pointerenter', () => {
      if (hoverTimer) return;
      hoverTimer = setInterval(() => {
        if (!enabled() || play.disabled) return;
        const r = play.getBoundingClientRect();
        // jak żar nad portalem: iskry unoszą się znad przycisku
        const x = rnd(r.left + 8, r.right - 8);
        spark(front, x, rnd(r.top - 2, r.top + 10), { ang: rnd(-2.2, -0.9), speed: rnd(0.5, 1.6), g: -0.015, drag: 0.98, life: rnd(30, 55), size: rnd(2, 4), kind: Math.random() < 0.8 ? 'px' : 'star', color: pick(['#ffffff', '#f5e9ff', '#f0abfc', '#e879f9', '#c084fc']) });
        run();
      }, 35);
    });
    play.addEventListener('pointerleave', () => { clearInterval(hoverTimer); hoverTimer = 0; });
  }

  // wybuchy w tle: co kilka sekund, raczej w pustych miejscach (wokół księżyca i u góry)
  function scheduleAmbient() {
    setTimeout(() => {
      if (enabled() && document.getElementById('tab-play')?.classList.contains('active')) {
        const moon = document.querySelector('.moon');
        let x, y;
        if (moon && Math.random() < 0.55) {
          const [mx, my] = center(moon);
          const a = rnd(0, Math.PI * 2), d = rnd(120, 190);
          x = mx + Math.cos(a) * d; y = my + Math.sin(a) * d * 0.7;
        } else {
          x = rnd(W * 0.12, W * 0.7); y = rnd(H * 0.1, H * 0.42);
        }
        ambient(x, y);
      }
      scheduleAmbient();
    }, rnd(3200, 6500));
  }
  scheduleAmbient();

  // powitanie: wybuch z księżyca po otwarciu launchera
  window.addEventListener('load', () => {
    setTimeout(() => {
      const moon = document.querySelector('.moon');
      if (!moon) return;
      const [x, y] = center(moon);
      if (!enabled()) return;
      flash(back, x, y, 300, { life: 50, a: 0.5 });
      ring(back, x, y, 340, { life: 60, w: 3, color: '#c084fc', r0: 50 });
      for (let i = 0; i < 60; i++) spark(back, x, y, { speed: rnd(1, 5), life: rnd(50, 100), size: rnd(2, 4), g: 0.02, drag: 0.97 });
      run();
    }, 450);
  });

  // poświata podążająca za kursorem (zmienne CSS, bez stylów inline w HTML)
  let mx = -999, my = -999, glowRaf = 0;
  document.addEventListener('pointermove', e => {
    mx = e.clientX; my = e.clientY;
    if (glowRaf) return;
    glowRaf = requestAnimationFrame(() => {
      glowRaf = 0;
      document.documentElement.style.setProperty('--mx', mx + 'px');
      document.documentElement.style.setProperty('--my', my + 'px');
    });
  }, { passive: true });

  window.voidFx = { pop, boom, ambient, layers: [back, front] };
})();
