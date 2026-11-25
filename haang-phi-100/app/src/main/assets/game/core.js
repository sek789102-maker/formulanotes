'use strict';
// ===== utilities =====
const TAU = Math.PI * 2;
const clamp = (v, a, b) => v < a ? a : v > b ? b : v;
const lerp = (a, b, t) => a + (b - a) * t;
const smooth = (a, b, x) => { const t = clamp((x - a) / (b - a), 0, 1); return t * t * (3 - 2 * t); };
let _seed = 1337;
function srand(s) { _seed = s >>> 0; }
function rnd() { _seed = (_seed * 1664525 + 1013904223) >>> 0; return _seed / 4294967296; }
const rr = (a, b) => a + Math.random() * (b - a);
const pick = arr => arr[(Math.random() * arr.length) | 0];

// gradient noise 2D
const PERM = new Uint8Array(512);
(function () { const p = []; for (let i = 0; i < 256; i++) p[i] = i; srand(9001); for (let i = 255; i > 0; i--) { const j = (rnd() * (i + 1)) | 0; [p[i], p[j]] = [p[j], p[i]]; } for (let i = 0; i < 512; i++) PERM[i] = p[i & 255]; })();
function grad(h, x, y) { const a = (h & 7) * 0.785398; return Math.cos(a) * x + Math.sin(a) * y; }
function noise2(x, y) {
  const xi = Math.floor(x), yi = Math.floor(y), xf = x - xi, yf = y - yi;
  const X = xi & 255, Y = yi & 255;
  const u = xf * xf * xf * (xf * (xf * 6 - 15) + 10), v = yf * yf * yf * (yf * (yf * 6 - 15) + 10);
  const aa = PERM[PERM[X] + Y], ab = PERM[PERM[X] + Y + 1], ba = PERM[PERM[X + 1] + Y], bb = PERM[PERM[X + 1] + Y + 1];
  const x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1, yf), u);
  const x2 = lerp(grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1), u);
  return lerp(x1, x2, v) * 1.4;
}
function fbm(x, y, o = 4) { let s = 0, a = 0.5, f = 1; for (let i = 0; i < o; i++) { s += a * noise2(x * f, y * f); f *= 2.03; a *= 0.5; } return s; }

// ===== canvas textures =====
function cvs(w, h) { const c = document.createElement('canvas'); c.width = w; c.height = h || w; return c; }
function tex(c, rep) {
  const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace;
  if (rep) { t.wrapS = t.wrapT = THREE.RepeatWrapping; t.repeat.set(rep, rep); }
  t.anisotropy = 4; return t;
}
function noiseFill(ctx, w, h, base, vari, scale, oct) {
  const img = ctx.createImageData(w, h), d = img.data;
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    // tileable noise via 4-corner blend
    const fx = x / w, fy = y / h;
    const n = (fbm(x * scale, y * scale, oct) * (1 - fx) * (1 - fy) + fbm((x - w) * scale, y * scale, oct) * fx * (1 - fy) +
      fbm(x * scale, (y - h) * scale, oct) * (1 - fx) * fy + fbm((x - w) * scale, (y - h) * scale, oct) * fx * fy);
    const i = (y * w + x) * 4;
    d[i] = clamp(base[0] + n * vari[0], 0, 255); d[i + 1] = clamp(base[1] + n * vari[1], 0, 255); d[i + 2] = clamp(base[2] + n * vari[2], 0, 255); d[i + 3] = 255;
  }
  ctx.putImageData(img, 0, 0);
}
const TEX = {};
function buildTextures(q) {
  const S = q === 0 ? 256 : 512;
  // forest floor: dirt + leaf litter + grass patches
  let c = cvs(S), g = c.getContext('2d');
  noiseFill(g, S, S, [78, 70, 42], [60, 55, 35], 8 / S, 5);
  srand(11);
  for (let i = 0; i < S * 3; i++) { // leaf litter
    const x = rnd() * S, y = rnd() * S, r = 1.5 + rnd() * 4;
    const k = rnd();
    g.fillStyle = k < 0.35 ? `rgba(${110 + rnd() * 60|0},${70 + rnd() * 30|0},${30|0},0.55)` : k < 0.7 ? `rgba(${60 + rnd() * 30|0},${80 + rnd() * 50|0},${25},0.5)` : `rgba(30,26,18,0.4)`;
    g.save(); g.translate(x, y); g.rotate(rnd() * TAU); g.beginPath(); g.ellipse(0, 0, r, r * 0.45, 0, 0, TAU); g.fill(); g.restore();
  }
  for (let i = 0; i < S * 4; i++) { // grass strokes
    const x = rnd() * S, y = rnd() * S; g.strokeStyle = `rgba(${50 + rnd() * 40|0},${90 + rnd() * 60|0},${30},0.5)`; g.lineWidth = 1;
    g.beginPath(); g.moveTo(x, y); g.lineTo(x + (rnd() - .5) * 4, y - 3 - rnd() * 5); g.stroke();
  }
  TEX.ground = tex(c, 70);
  // bark
  c = cvs(256, 512); g = c.getContext('2d');
  noiseFill(g, 256, 512, [86, 70, 56], [40, 34, 28], 0.03, 4);
  for (let i = 0; i < 160; i++) { const x = rnd() * 256; g.strokeStyle = `rgba(20,14,10,${0.25 + rnd() * 0.4})`; g.lineWidth = 1 + rnd() * 3; g.beginPath(); g.moveTo(x, 0); let yy = 0, xx = x; while (yy < 512) { yy += 10 + rnd() * 30; xx += (rnd() - .5) * 8; g.lineTo(xx, yy); } g.stroke(); }
  for (let i = 0; i < 60; i++) { g.fillStyle = `rgba(${90 + rnd() * 40|0},${110 + rnd() * 40|0},60,0.25)`; g.beginPath(); g.arc(rnd() * 256, rnd() * 512, 4 + rnd() * 14, 0, TAU); g.fill(); } // moss
  TEX.bark = tex(c); TEX.bark.wrapS = TEX.bark.wrapT = THREE.RepeatWrapping;
  // pale bark (yang na)
  c = cvs(256, 512); g = c.getContext('2d');
  noiseFill(g, 256, 512, [140, 128, 110], [40, 36, 30], 0.025, 4);
  for (let i = 0; i < 90; i++) { g.fillStyle = `rgba(${60 + rnd() * 30|0},${55 + rnd() * 20|0},40,${0.2 + rnd() * 0.3})`; g.beginPath(); g.ellipse(rnd() * 256, rnd() * 512, 3 + rnd() * 10, 6 + rnd() * 25, 0, 0, TAU); g.fill(); }
  for (let i = 0; i < 50; i++) { g.fillStyle = `rgba(${70 + rnd() * 30|0},${110 + rnd() * 50|0},50,0.3)`; g.beginPath(); g.arc(rnd() * 256, rnd() * 512, 5 + rnd() * 18, 0, TAU); g.fill(); }
  TEX.barkPale = tex(c); TEX.barkPale.wrapS = TEX.barkPale.wrapT = THREE.RepeatWrapping;
  // leaf cluster (alpha)
  c = cvs(256); g = c.getContext('2d');
  for (let i = 0; i < 230; i++) {
    const a = rnd() * TAU, r = Math.sqrt(rnd()) * 110, x = 128 + Math.cos(a) * r, y = 128 + Math.sin(a) * r;
    const l = 30 + rnd() * 50;
    g.fillStyle = `rgb(${l * 0.6 + rnd() * 20|0},${l + 40 + rnd() * 50|0},${l * 0.35|0})`;
    g.save(); g.translate(x, y); g.rotate(rnd() * TAU); g.beginPath(); g.ellipse(0, 0, 13 + rnd() * 6, 5 + rnd() * 2, 0, 0, TAU); g.fill();
    g.strokeStyle = 'rgba(20,40,10,0.5)'; g.lineWidth = 1; g.beginPath(); g.moveTo(-12, 0); g.lineTo(12, 0); g.stroke(); g.restore();
  }
  TEX.leaf = tex(c);
  // fern (alpha)
  c = cvs(256); g = c.getContext('2d');
  for (let f = 0; f < 7; f++) {
    const ang = -Math.PI / 2 + (f - 3) * 0.33; g.save(); g.translate(128, 256); g.rotate(ang + Math.PI / 2);
    const L = 170 + rnd() * 60; g.strokeStyle = '#3d5a1c'; g.lineWidth = 3; g.beginPath(); g.moveTo(0, 0); g.quadraticCurveTo(10, -L * 0.6, 25, -L); g.stroke();
    for (let t = 0.1; t < 1; t += 0.045) {
      const px = 25 * t * t, py = -L * t, w = 26 * (1 - t) + 5;
      g.fillStyle = `rgb(${40 + rnd() * 30|0},${100 + rnd() * 50|0},${30})`;
      g.beginPath(); g.ellipse(px - w * 0.5, py, w * 0.55, 3.5, -0.4, 0, TAU); g.fill();
      g.beginPath(); g.ellipse(px + w * 0.5, py, w * 0.55, 3.5, 0.4, 0, TAU); g.fill();
    }
    g.restore();
  }
  TEX.fern = tex(c);
  // tall grass (alpha)
  c = cvs(256); g = c.getContext('2d');
  for (let i = 0; i < 90; i++) {
    const x = 20 + rnd() * 216, h = 120 + rnd() * 130, bend = (rnd() - .5) * 60;
    g.strokeStyle = `rgb(${60 + rnd() * 50|0},${110 + rnd() * 60|0},${30 + rnd() * 20|0})`; g.lineWidth = 2 + rnd() * 2.5;
    g.beginPath(); g.moveTo(x, 256); g.quadraticCurveTo(x + bend * 0.3, 256 - h * 0.6, x + bend, 256 - h); g.stroke();
  }
  TEX.grass = tex(c);
  // banana / wide leaf
  c = cvs(128, 256); g = c.getContext('2d');
  g.fillStyle = '#3f7a24'; g.beginPath(); g.moveTo(64, 0); g.bezierCurveTo(128, 60, 120, 200, 64, 256); g.bezierCurveTo(8, 200, 0, 60, 64, 0); g.fill();
  g.strokeStyle = '#a8c870'; g.lineWidth = 3; g.beginPath(); g.moveTo(64, 0); g.lineTo(64, 256); g.stroke();
  g.strokeStyle = 'rgba(30,60,15,0.6)'; g.lineWidth = 1; for (let y = 10; y < 250; y += 7) { g.beginPath(); g.moveTo(64, y); g.lineTo(10, y + 18); g.moveTo(64, y); g.lineTo(118, y + 18); g.stroke(); }
  for (let i = 0; i < 6; i++) { g.clearRect(rnd() > .5 ? 0 : 90, 40 + rnd() * 180, 40, 3); }
  TEX.wideLeaf = tex(c);
  // wood planks
  c = cvs(256); g = c.getContext('2d');
  noiseFill(g, 256, 256, [120, 86, 52], [40, 30, 20], 0.02, 3);
  for (let i = 0; i < 70; i++) { const y = rnd() * 256; g.strokeStyle = `rgba(50,30,15,${0.2 + rnd() * 0.3})`; g.lineWidth = 1; g.beginPath(); g.moveTo(0, y); g.bezierCurveTo(80, y + (rnd() - .5) * 8, 170, y + (rnd() - .5) * 8, 256, y); g.stroke(); }
  TEX.wood = tex(c); TEX.wood.wrapS = TEX.wood.wrapT = THREE.RepeatWrapping;
  // thatch
  c = cvs(256); g = c.getContext('2d'); g.fillStyle = '#6b5a32'; g.fillRect(0, 0, 256, 256);
  for (let i = 0; i < 900; i++) { const x = rnd() * 256, y = rnd() * 256; g.strokeStyle = `rgb(${120 + rnd() * 60|0},${100 + rnd() * 50|0},${50 + rnd() * 30|0})`; g.lineWidth = 1 + rnd(); g.beginPath(); g.moveTo(x, y); g.lineTo(x + (rnd() - .5) * 6, y + 20 + rnd() * 20); g.stroke(); }
  TEX.thatch = tex(c); TEX.thatch.wrapS = TEX.thatch.wrapT = THREE.RepeatWrapping;
  // glow radial
  c = cvs(128); g = c.getContext('2d'); let gr = g.createRadialGradient(64, 64, 0, 64, 64, 64);
  gr.addColorStop(0, 'rgba(255,255,255,1)'); gr.addColorStop(0.25, 'rgba(255,255,255,0.6)'); gr.addColorStop(1, 'rgba(255,255,255,0)'); g.fillStyle = gr; g.fillRect(0, 0, 128, 128);
  TEX.glow = tex(c);
  // soft fog puff
  c = cvs(128); g = c.getContext('2d'); gr = g.createRadialGradient(64, 64, 0, 64, 64, 64);
  gr.addColorStop(0, 'rgba(255,255,255,0.5)'); gr.addColorStop(1, 'rgba(255,255,255,0)'); g.fillStyle = gr; g.fillRect(0, 0, 128, 128);
  TEX.puff = tex(c);
  // tiger fur
  c = cvs(256); g = c.getContext('2d'); noiseFill(g, 256, 256, [196, 110, 36], [40, 30, 15], 0.05, 3);
  g.fillStyle = 'rgba(240,225,200,0.8)'; g.fillRect(0, 190, 256, 66);
  for (let i = 0; i < 26; i++) { const x = i * 10 + rnd() * 6; g.fillStyle = '#140c06'; g.beginPath(); g.moveTo(x, 0); g.bezierCurveTo(x + 8, 60, x - 6, 120, x + 4, 180 + rnd() * 30); g.lineTo(x + 2, 180); g.bezierCurveTo(x - 10, 110, x + 4, 60, x - 3, 0); g.fill(); }
  TEX.tiger = tex(c);
  // woven bamboo (kradong)
  c = cvs(128); g = c.getContext('2d'); g.fillStyle = '#9a7a3a'; g.fillRect(0, 0, 128, 128);
  for (let i = 0; i < 16; i++) for (let j = 0; j < 16; j++) { g.fillStyle = (i + j) % 2 ? '#c8a560' : '#7a5a26'; g.fillRect(i * 8, j * 8, (i + j) % 2 ? 8 : 6, (i + j) % 2 ? 6 : 8); }
  TEX.woven = tex(c);
  // hair strands (alpha)
  c = cvs(128, 256); g = c.getContext('2d');
  for (let i = 0; i < 260; i++) { const x = rnd() * 128; g.strokeStyle = `rgba(${8 + rnd() * 14|0},${6 + rnd() * 10|0},${6 + rnd() * 10|0},${0.7 + rnd() * 0.3})`; g.lineWidth = 1 + rnd() * 1.5; g.beginPath(); g.moveTo(x, 0); g.bezierCurveTo(x + (rnd() - .5) * 20, 80, x + (rnd() - .5) * 30, 160, x + (rnd() - .5) * 30, 180 + rnd() * 76); g.stroke(); }
  TEX.hair = tex(c);
  // ribbons (pha prae 3 colours)
  c = cvs(64); g = c.getContext('2d'); ['#d81b60', '#1e88e5', '#43a047', '#fdd835', '#d81b60', '#8e24aa'].forEach((col, i) => { g.fillStyle = col; g.fillRect(0, i * 11, 64, 11); });
  TEX.ribbon = tex(c);
  TEX.faces = {};
}
// ghost faces
function faceTex(kind) {
  if (TEX.faces[kind]) return TEX.faces[kind];
  const c = cvs(256), g = c.getContext('2d');
  const skin = { krasue: '#d9c4b0', phrai: '#e8e8ea', takhian: '#e2d2b8', pop: '#7d7868', pret: '#3b3430', krahang: '#6a4a36' }[kind] || '#ccc';
  let gr = g.createRadialGradient(128, 120, 10, 128, 128, 128); gr.addColorStop(0, skin); gr.addColorStop(0.85, skin); gr.addColorStop(1, 'rgba(0,0,0,0)');
  g.fillStyle = gr; g.beginPath(); g.ellipse(128, 128, 95, 122, 0, 0, TAU); g.fill();
  // shadows
  g.fillStyle = 'rgba(40,20,30,0.35)'; g.beginPath(); g.ellipse(128, 200, 60, 30, 0, 0, TAU); g.fill();
  // eyes sockets
  const ey = kind === 'pret' ? 110 : 112;
  for (const sx of [-1, 1]) {
    gr = g.createRadialGradient(128 + sx * 36, ey, 2, 128 + sx * 36, ey, 34); gr.addColorStop(0, '#000'); gr.addColorStop(0.6, 'rgba(20,0,0,0.85)'); gr.addColorStop(1, 'rgba(40,10,20,0)');
    g.fillStyle = gr; g.beginPath(); g.ellipse(128 + sx * 36, ey, 30, 24, 0, 0, TAU); g.fill();
    const eyeCol = kind === 'phrai' ? '#ffffff' : kind === 'takhian' ? '#aaff88' : '#ff2a1a';
    g.fillStyle = eyeCol; g.shadowColor = eyeCol; g.shadowBlur = 14; g.beginPath(); g.arc(128 + sx * 34, ey + 2, kind === 'pop' ? 7 : 5, 0, TAU); g.fill(); g.shadowBlur = 0;
  }
  // nose
  g.strokeStyle = 'rgba(60,30,30,0.5)'; g.lineWidth = 3; g.beginPath(); g.moveTo(128, 120); g.lineTo(122, 160); g.lineTo(134, 162); g.stroke();
  // mouth
  if (kind === 'pret') { g.fillStyle = '#000'; g.beginPath(); g.arc(128, 190, 4, 0, TAU); g.fill(); }
  else {
    g.fillStyle = '#1a0005'; g.beginPath(); g.ellipse(128, 192, kind === 'krasue' ? 34 : 26, kind === 'phrai' ? 26 : 16, 0, 0, TAU); g.fill();
    g.fillStyle = '#e8e0c8'; for (let i = -3; i <= 3; i++) { g.beginPath(); g.moveTo(128 + i * 8 - 3, 180); g.lineTo(128 + i * 8 + 3, 180); g.lineTo(128 + i * 8, 190 + (i % 2 ? 6 : 2)); g.fill(); }
    if (kind === 'krasue' || kind === 'pop' || kind === 'krahang') { g.fillStyle = 'rgba(150,0,0,0.85)'; g.fillRect(110, 200, 6, 40); g.fillRect(140, 204, 4, 30); g.beginPath(); g.ellipse(128, 206, 30, 6, 0, 0, TAU); g.fill(); }
  }
  // cracks / veins
  g.strokeStyle = kind === 'phrai' ? 'rgba(80,80,140,0.35)' : 'rgba(80,0,0,0.35)'; g.lineWidth = 1.2;
  for (let i = 0; i < 12; i++) { let x = 50 + Math.random() * 156, y = 40 + Math.random() * 170; g.beginPath(); g.moveTo(x, y); for (let k = 0; k < 4; k++) { x += (Math.random() - .5) * 20; y += (Math.random() - .5) * 20; g.lineTo(x, y); } g.stroke(); }
  return (TEX.faces[kind] = tex(c));
}

// ===== audio (procedural) =====
const AU = {
  ctx: null, master: null, amb: {}, ready: false, vol: 0.8,
  init() {
    if (this.ctx) { if (this.ctx.state !== 'running') this.ctx.resume(); return; }
    const AC = window.AudioContext || window.webkitAudioContext; if (!AC) return;
    this.ctx = new AC(); this.master = this.ctx.createGain(); this.master.gain.value = this.vol;
    this.comp = this.ctx.createDynamicsCompressor(); this.master.connect(this.comp); this.comp.connect(this.ctx.destination);
    // noise buffer
    const len = this.ctx.sampleRate * 2, b = this.ctx.createBuffer(1, len, this.ctx.sampleRate), d = b.getChannelData(0);
    for (let i = 0; i < len; i++) d[i] = Math.random() * 2 - 1; this.noise = b;
    // pink-ish brown noise buffer
    const b2 = this.ctx.createBuffer(1, len, this.ctx.sampleRate), d2 = b2.getChannelData(0); let last = 0;
    for (let i = 0; i < len; i++) { last = (last + 0.02 * (Math.random() * 2 - 1)) / 1.02; d2[i] = last * 3.5; } this.brown = b2;
    this.ready = true; this.startAmbience();
  },
  t() { return this.ctx.currentTime; },
  gain(v, dest) { const g = this.ctx.createGain(); g.gain.value = v; g.connect(dest || this.master); return g; },
  noiseSrc(buf, loop) { const s = this.ctx.createBufferSource(); s.buffer = buf || this.noise; s.loop = !!loop; return s; },
  pan(x, dest) { if (!this.ctx.createStereoPanner) return dest || this.master; const p = this.ctx.createStereoPanner(); p.pan.value = clamp(x, -1, 1); p.connect(dest || this.master); return p; },
  startAmbience() {
    const c = this.ctx;
    // wind / forest bed
    const w = this.noiseSrc(this.brown, true), wf = c.createBiquadFilter(); wf.type = 'lowpass'; wf.frequency.value = 500;
    const wg = this.gain(0.0); w.connect(wf); wf.connect(wg); w.start(); this.amb.wind = wg;
    // cicadas (day) - band noise amplitude modulated
    const ci = this.noiseSrc(this.noise, true), cf = c.createBiquadFilter(); cf.type = 'bandpass'; cf.frequency.value = 5200; cf.Q.value = 6;
    const cg = this.gain(0); const am = c.createGain(); am.gain.value = 0.5; const lfo = c.createOscillator(); lfo.frequency.value = 38; const lg = c.createGain(); lg.gain.value = 0.5; lfo.connect(lg); lg.connect(am.gain); lfo.start();
    ci.connect(cf); cf.connect(am); am.connect(cg); ci.start(); this.amb.cicada = cg;
    // crickets (night)
    const cr = c.createOscillator(); cr.frequency.value = 4400; const crA = c.createGain(); crA.gain.value = 0; const crL = c.createOscillator(); crL.type = 'square'; crL.frequency.value = 22;
    const crLG = c.createGain(); crLG.gain.value = 0.5; crL.connect(crLG); crLG.connect(crA.gain); const crg = this.gain(0); cr.connect(crA); crA.connect(crg); cr.start(); crL.start(); this.amb.cricket = crg;
    // second cricket
    const cr2 = c.createOscillator(); cr2.frequency.value = 3900; const c2A = c.createGain(); c2A.gain.value = 0; const c2L = c.createOscillator(); c2L.type = 'square'; c2L.frequency.value = 17;
    const c2LG = c.createGain(); c2LG.gain.value = 0.5; c2L.connect(c2LG); c2LG.connect(c2A.gain); cr2.connect(c2A); c2A.connect(crg); cr2.start(); c2L.start();
    // drone (night dread)
    const dr = c.createOscillator(); dr.type = 'sawtooth'; dr.frequency.value = 55; const dr2 = c.createOscillator(); dr2.type = 'sawtooth'; dr2.frequency.value = 55.7;
    const df = c.createBiquadFilter(); df.type = 'lowpass'; df.frequency.value = 180; const dg = this.gain(0); dr.connect(df); dr2.connect(df); df.connect(dg); dr.start(); dr2.start(); this.amb.drone = dg;
    // heartbeat timer handled in update
    this.hbT = 0; this.birdT = 2; this.owlT = 8; this.geckoT = 20;
  },
  setAmb(day01, dread, sanityLow) { // day01: 1 day, 0 night
    if (!this.ready) return; const t = this.t();
    this.amb.wind.gain.setTargetAtTime(0.10 + 0.05 * (1 - day01), t, 0.8);
    this.amb.cicada.gain.setTargetAtTime(0.022 * day01, t, 1);
    this.amb.cricket.gain.setTargetAtTime(0.012 * (1 - day01) * (1 - dread * 0.7), t, 1);
    this.amb.drone.gain.setTargetAtTime(0.06 * dread + 0.05 * sanityLow, t, 1.5);
  },
  update(dt, day01, sanity) {
    if (!this.ready) return;
    if (day01 > 0.5) { this.birdT -= dt; if (this.birdT < 0) { this.birdT = rr(1.5, 6); this.bird(); } }
    else { this.owlT -= dt; if (this.owlT < 0) { this.owlT = rr(10, 25); Math.random() < 0.5 ? this.owl() : this.gecko(); } }
    if (sanity < 35) { this.hbT -= dt; if (this.hbT < 0) { this.hbT = lerp(0.45, 1.0, sanity / 35); this.heart(); } }
  },
  env(g, t, a, peak, d) { g.gain.setValueAtTime(0.0001, t); g.gain.exponentialRampToValueAtTime(peak, t + a); g.gain.exponentialRampToValueAtTime(0.0001, t + a + d); },
  tone(type, f0, f1, dur, vol, pan, delay = 0) {
    if (!this.ready) return; const t = this.t() + delay, o = this.ctx.createOscillator(); o.type = type; o.frequency.setValueAtTime(f0, t);
    if (f1) o.frequency.exponentialRampToValueAtTime(f1, t + dur); const g = this.ctx.createGain(); this.env(g, t, 0.01, vol, dur); o.connect(g); g.connect(this.pan(pan || 0)); o.start(t); o.stop(t + dur + 0.05);
  },
  burst(freq, q, dur, vol, type = 'bandpass', pan = 0, delay = 0) {
    if (!this.ready) return; const t = this.t() + delay, s = this.noiseSrc(); const f = this.ctx.createBiquadFilter(); f.type = type; f.frequency.value = freq; f.Q.value = q;
    const g = this.ctx.createGain(); this.env(g, t, 0.005, vol, dur); s.connect(f); f.connect(g); g.connect(this.pan(pan)); s.start(t, Math.random()); s.stop(t + dur + 0.05);
  },
  bird() { const p = rr(-0.8, 0.8), b = rr(2200, 3600), n = 2 + (Math.random() * 4 | 0); for (let i = 0; i < n; i++) this.tone('sine', b * rr(0.9, 1.2), b * rr(1.2, 1.6), 0.09, 0.03, p, i * 0.13); },
  owl() { const p = rr(-1, 1); this.tone('sine', 420, 380, 0.35, 0.05, p); this.tone('sine', 400, 360, 0.6, 0.05, p, 0.5); },
  gecko() { const p = rr(-1, 1); for (let i = 0; i < 5; i++) { this.tone('square', 520, 300, 0.12, 0.02, p, i * 0.5); this.tone('square', 260, 180, 0.15, 0.02, p, i * 0.5 + 0.18); } },
  heart() { this.tone('sine', 70, 40, 0.15, 0.35); this.tone('sine', 65, 38, 0.15, 0.25, 0, 0.18); },
  twang() { this.tone('triangle', 180, 90, 0.25, 0.25); this.burst(2500, 1, 0.08, 0.2, 'highpass'); },
  thud() { this.burst(400, 1, 0.12, 0.4, 'lowpass'); },
  hitFlesh() { this.burst(900, 2, 0.1, 0.35); this.tone('sine', 140, 60, 0.1, 0.2); },
  step(run) { this.burst(run ? 700 : 500, 0.8, 0.06, run ? 0.08 : 0.05, 'lowpass', rr(-0.2, 0.2)); },
  pickup() { this.tone('sine', 660, 990, 0.12, 0.08); this.tone('sine', 990, 1320, 0.12, 0.06, 0, 0.08); },
  rice() { for (let i = 0; i < 14; i++) this.burst(rr(3000, 7000), 3, 0.04, 0.08, 'bandpass', rr(-.5, .5), i * 0.02); this.tone('sine', 880, 1760, 0.6, 0.06); },
  chant() { // amulet: deep om + bell
    this.tone('sine', 110, 108, 2.2, 0.3); this.tone('sine', 165, 163, 2.2, 0.15); [1320, 1980, 2640].forEach((f, i) => this.tone('sine', f, f * 0.99, 2.5, 0.08 / (i + 1)));
  },
  scream(kind, pan = 0, vol = 0.25) {
    if (!this.ready) return; const c = this.ctx, t = this.t();
    if (kind === 'saming') { // roar
      const s = this.noiseSrc(); const f = c.createBiquadFilter(); f.type = 'lowpass'; f.frequency.setValueAtTime(900, t); f.frequency.exponentialRampToValueAtTime(200, t + 1.4);
      const o = c.createOscillator(); o.type = 'sawtooth'; o.frequency.setValueAtTime(110, t); o.frequency.exponentialRampToValueAtTime(60, t + 1.4);
      const g = c.createGain(); this.env(g, t, 0.08, vol * 1.6, 1.3); s.connect(f); o.connect(f); f.connect(g); g.connect(this.pan(pan)); s.start(t); s.stop(t + 1.6); o.start(t); o.stop(t + 1.6); return;
    }
    if (kind === 'krasue') { // high cackle
      for (let i = 0; i < 7; i++) { const f = rr(900, 1300); this.tone('sawtooth', f, f * 0.7, 0.11, vol * 0.25, pan, i * 0.14); }
      return;
    }
    if (kind === 'pret') { // very high thin whistle wail
      const o = c.createOscillator(); o.type = 'sine'; o.frequency.setValueAtTime(1800, t); o.frequency.linearRampToValueAtTime(2400, t + 1.5); o.frequency.linearRampToValueAtTime(1500, t + 3);
      const v = c.createOscillator(); v.frequency.value = 6; const vg = c.createGain(); vg.gain.value = 40; v.connect(vg); vg.connect(o.frequency);
      const g = c.createGain(); this.env(g, t, 0.4, vol * 0.4, 2.6); o.connect(g); g.connect(this.pan(pan)); o.start(t); v.start(t); o.stop(t + 3.1); v.stop(t + 3.1); return;
    }
    // wail (phrai/takhian/pop/krahang)
    const base = kind === 'pop' ? 180 : kind === 'krahang' ? 240 : 520;
    const o = c.createOscillator(); o.type = kind === 'pop' ? 'sawtooth' : 'triangle'; o.frequency.setValueAtTime(base, t); o.frequency.linearRampToValueAtTime(base * 1.6, t + 0.6); o.frequency.linearRampToValueAtTime(base * 0.6, t + 2);
    const v = c.createOscillator(); v.frequency.value = 7; const vg = c.createGain(); vg.gain.value = base * 0.06; v.connect(vg); vg.connect(o.frequency);
    const f = c.createBiquadFilter(); f.type = 'bandpass'; f.frequency.value = base * 2; f.Q.value = 1.2;
    const g = c.createGain(); this.env(g, t, 0.2, vol * 0.7, 1.9); o.connect(f); f.connect(g); g.connect(this.pan(pan)); o.start(t); v.start(t); o.stop(t + 2.2); v.stop(t + 2.2);
  },
  squeal() { this.tone('sawtooth', 900, 500, 0.4, 0.12); this.tone('square', 700, 400, 0.3, 0.06, 0, 0.1); },
  cluck() { for (let i = 0; i < 3; i++) this.tone('square', 700, 500, 0.06, 0.04, 0, i * 0.09); },
  hurt() { this.tone('sawtooth', 220, 110, 0.25, 0.15); this.burst(300, 1, 0.2, 0.25, 'lowpass'); },
  ghostDie() { this.tone('sine', 600, 80, 1.2, 0.18); this.burst(1500, 0.5, 0.8, 0.12, 'bandpass'); },
  creak() { this.tone('sawtooth', 90, 70, 0.4, 0.06); this.burst(250, 4, 0.3, 0.1); },
};
