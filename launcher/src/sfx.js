'use strict';
// Dźwięki launchera – generowane na bieżąco (Web Audio), bez plików.
// Ciche, krótkie i miękkie: kliknięcia, przełączniki, start gry, sukces i błąd.
// Włącza/wyłącza je opcja „Dźwięki w launcherze” (klasa .sfx na <body>).

(() => {
  let ctx = null, master = null, verb = null;
  const on = () => document.body.classList.contains('sfx') && !document.hidden;

  function init() {
    if (ctx) return true;
    try {
      ctx = new (window.AudioContext || window.webkitAudioContext)();
    } catch { return false; }
    master = ctx.createGain();
    master.gain.value = 0.55;
    // delikatny "pogłos" z krótkiego echa – brzmi przestrzennie, jak w pustce
    verb = ctx.createDelay(0.5);
    verb.delayTime.value = 0.11;
    const fb = ctx.createGain(); fb.gain.value = 0.28;
    const wet = ctx.createGain(); wet.gain.value = 0.22;
    const lp = ctx.createBiquadFilter(); lp.type = 'lowpass'; lp.frequency.value = 2600;
    verb.connect(lp); lp.connect(fb); fb.connect(verb); lp.connect(wet); wet.connect(master);
    master.connect(ctx.destination);
    return true;
  }

  function ready() {
    if (!on() || !init()) return false;
    if (ctx.state === 'suspended') ctx.resume();
    return true;
  }

  /** Jeden miękki ton: sinus z szybkim narastaniem i łagodnym wygaszeniem. */
  function tone(freq, { at = 0, dur = 0.12, vol = 0.08, type = 'sine', to = null, echo = true } = {}) {
    const t = ctx.currentTime + at;
    const o = ctx.createOscillator();
    const g = ctx.createGain();
    o.type = type;
    o.frequency.setValueAtTime(freq, t);
    if (to) o.frequency.exponentialRampToValueAtTime(to, t + dur);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(vol, t + 0.006);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g); g.connect(master);
    if (echo) g.connect(verb);
    o.start(t); o.stop(t + dur + 0.02);
  }

  /** Szum przepuszczony przez filtr – "whoosh". */
  function whoosh({ at = 0, dur = 0.5, vol = 0.05, from = 300, to = 3000 } = {}) {
    const t = ctx.currentTime + at;
    const len = Math.ceil(ctx.sampleRate * dur);
    const buf = ctx.createBuffer(1, len, ctx.sampleRate);
    const d = buf.getChannelData(0);
    for (let i = 0; i < len; i++) d[i] = Math.random() * 2 - 1;
    const src = ctx.createBufferSource(); src.buffer = buf;
    const f = ctx.createBiquadFilter(); f.type = 'bandpass'; f.Q.value = 1.4;
    f.frequency.setValueAtTime(from, t); f.frequency.exponentialRampToValueAtTime(to, t + dur);
    const g = ctx.createGain();
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(vol, t + dur * 0.4);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    src.connect(f); f.connect(g); g.connect(master); g.connect(verb);
    src.start(t); src.stop(t + dur);
  }

  const sfx = {
    // zwykłe kliknięcie: krótki, miękki "tik" z lekkim spadkiem wysokości
    click() {
      if (!ready()) return;
      const p = 1 + (Math.random() - 0.5) * 0.06; // minimalnie różny za każdym razem
      tone(1250 * p, { dur: 0.07, vol: 0.06, to: 820 * p });
      tone(2500 * p, { dur: 0.035, vol: 0.015, echo: false });
    },
    // zakładka w menu bocznym: niższy, okrągły "pop"
    nav() {
      if (!ready()) return;
      tone(520, { dur: 0.11, vol: 0.07, to: 760 });
      tone(1040, { at: 0.02, dur: 0.08, vol: 0.02 });
    },
    // przełącznik: w górę przy włączeniu, w dół przy wyłączeniu
    toggle(state) {
      if (!ready()) return;
      if (state) { tone(660, { dur: 0.08, vol: 0.06 }); tone(990, { at: 0.06, dur: 0.12, vol: 0.06 }); }
      else { tone(880, { dur: 0.08, vol: 0.05 }); tone(587, { at: 0.06, dur: 0.12, vol: 0.05 }); }
    },
    // start gry: narastający szum i rozłożony akord jak otwarcie portalu
    play() {
      if (!ready()) return;
      whoosh({ dur: 0.7, vol: 0.06, from: 250, to: 4200 });
      tone(110, { dur: 0.6, vol: 0.09, to: 55, type: 'triangle' });
      [523.25, 659.25, 783.99, 1046.5, 1318.5].forEach((f, i) => tone(f, { at: 0.05 + i * 0.055, dur: 0.55, vol: 0.05 }));
    },
    // sukces (instalacja, dodanie pliku, logowanie): dwa wesołe dzwoneczki
    success() {
      if (!ready()) return;
      tone(784, { dur: 0.16, vol: 0.06 });
      tone(1175, { at: 0.09, dur: 0.28, vol: 0.06 });
      tone(2350, { at: 0.09, dur: 0.12, vol: 0.012, echo: false });
    },
    // błąd: niski, miękki podwójny dźwięk – bez piszczenia
    error() {
      if (!ready()) return;
      tone(330, { dur: 0.12, vol: 0.07, type: 'triangle' });
      tone(262, { at: 0.13, dur: 0.2, vol: 0.07, type: 'triangle' });
    }
  };
  window.voidSfx = sfx;

  document.addEventListener('click', e => {
    const el = e.target.closest('button, .profile, a');
    if (!el || el.disabled || el.id === 'playBtn') return; // GRAJ ma własny dźwięk
    if (el.classList.contains('switch')) return sfx.toggle(el.classList.contains('on'));
    if (el.classList.contains('nav')) return sfx.nav();
    sfx.click();
  });
})();
