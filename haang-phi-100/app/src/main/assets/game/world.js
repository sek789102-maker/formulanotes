'use strict';
// ===== world construction =====
const STAND_Y = 8.4, EYE_H = 1.55;
const POND = { x: 48, z: -34, r: 15, lvl: -0.9 };
const SHRINE = { x: -36, z: 28 };
const TAKHIAN = { x: -92, z: -78 };
const WORLD_R = 175;
const W = { uTime: { value: 0 }, wind: { value: 1 } };
let renderer, scene, camera, QUALITY = 1;
const COLL = []; const CGRID = new Map();
function addCollider(x, z, r, h = 14) { const o = { x, z, r, h, gy: terrainH(x, z) }; COLL.push(o); const k = ((x / 10) | 0) + ',' + ((z / 10) | 0); const cx = Math.floor(x / 10), cz = Math.floor(z / 10); for (let i = -1; i <= 1; i++) for (let j = -1; j <= 1; j++) { const kk = (cx + i) + ',' + (cz + j); if (!CGRID.has(kk)) CGRID.set(kk, []); CGRID.get(kk).push(o); } }
function nearColliders(x, z) { return CGRID.get(Math.floor(x / 10) + ',' + Math.floor(z / 10)) || []; }

function terrainH(x, z) {
  let h = fbm(x * 0.011, z * 0.011, 4) * 8 + fbm(x * 0.06 + 40, z * 0.06, 2) * 0.9;
  const d = Math.hypot(x, z);
  h *= smooth(6, 34, d);
  const dp = Math.hypot(x - POND.x, z - POND.z);
  if (dp < POND.r + 10) h = lerp(h, -2.6 + (dp / POND.r) * 1.6, 1 - smooth(POND.r - 5, POND.r + 10, dp));
  const ds = Math.hypot(x - SHRINE.x, z - SHRINE.z); h = lerp(h, terrainBase(SHRINE.x, SHRINE.z), 1 - smooth(3, 9, ds));
  h += smooth(150, 205, d) * 30;
  return h;
}
function terrainBase(x, z) { return fbm(x * 0.011, z * 0.011, 4) * 8 * smooth(6, 34, Math.hypot(x, z)); }
const groundY = terrainH;

function mergeGeos(list) {
  const gs = list.map(g => g.index ? g.toNonIndexed() : g);
  let n = 0; gs.forEach(g => n += g.attributes.position.count);
  const pos = new Float32Array(n * 3), nor = new Float32Array(n * 3), uv = new Float32Array(n * 2), col = new Float32Array(n * 3);
  let o = 0;
  gs.forEach(g => {
    const c = g.attributes.position.count;
    pos.set(g.attributes.position.array, o * 3);
    if (!g.attributes.normal) g.computeVertexNormals();
    nor.set(g.attributes.normal.array, o * 3);
    if (g.attributes.uv) uv.set(g.attributes.uv.array, o * 2);
    if (g.attributes.color) col.set(g.attributes.color.array, o * 3); else col.fill(1, o * 3, (o + c) * 3);
    o += c;
  });
  const m = new THREE.BufferGeometry();
  m.setAttribute('position', new THREE.BufferAttribute(pos, 3)); m.setAttribute('normal', new THREE.BufferAttribute(nor, 3));
  m.setAttribute('uv', new THREE.BufferAttribute(uv, 2)); m.setAttribute('color', new THREE.BufferAttribute(col, 3));
  return m;
}
function tint(g, r, gg, b) { const n = g.attributes.position.count, a = new Float32Array(n * 3); for (let i = 0; i < n; i++) { a[i * 3] = r; a[i * 3 + 1] = gg; a[i * 3 + 2] = b; } g.setAttribute('color', new THREE.BufferAttribute(a, 3)); return g; }
function blob(r, detail, jag) {
  const g = new THREE.IcosahedronGeometry(r, detail), p = g.attributes.position, v = new THREE.Vector3();
  const s = Math.random() * 100;
  for (let i = 0; i < p.count; i++) { v.fromBufferAttribute(p, i); const n = 1 + jag * noise2(v.x * 1.3 + s, v.y * 1.3 + v.z * 0.7); v.multiplyScalar(n); v.y *= 0.7; p.setXYZ(i, v.x, v.y, v.z); }
  g.computeVertexNormals(); return g;
}
function swayMat(mat, amp) {
  mat.onBeforeCompile = sh => {
    sh.uniforms.uTime = W.uTime; sh.uniforms.uWind = W.wind;
    sh.vertexShader = 'uniform float uTime; uniform float uWind;\n' + sh.vertexShader.replace('#include <begin_vertex>',
      `#include <begin_vertex>
      #ifdef USE_INSTANCING
      float ph = instanceMatrix[3].x * 0.37 + instanceMatrix[3].z * 0.23;
      #else
      float ph = 0.0;
      #endif
      float sw = sin(uTime * 1.7 + ph) * 0.6 + sin(uTime * 3.1 + ph * 1.7) * 0.4;
      transformed.x += sw * ${amp.toFixed(3)} * max(position.y, 0.0) * uWind;
      transformed.z += cos(uTime * 1.3 + ph) * ${(amp * 0.6).toFixed(3)} * max(position.y, 0.0) * uWind;`);
  };
  return mat;
}
function instanced(geo, mat, mats, shadow) {
  const m = new THREE.InstancedMesh(geo, mat, mats.length);
  mats.forEach((mm, i) => m.setMatrixAt(i, mm));
  m.instanceMatrix.needsUpdate = true; m.castShadow = !!shadow; m.receiveShadow = QUALITY > 0;
  m.frustumCulled = false; scene.add(m); return m;
}
const _m4 = new THREE.Matrix4(), _q = new THREE.Quaternion(), _v = new THREE.Vector3(), _s = new THREE.Vector3(), _e = new THREE.Euler();
function mtx(x, y, z, ry, sx, sy, sz, rx = 0, rz = 0) { _e.set(rx, ry, rz); _q.setFromEuler(_e); return new THREE.Matrix4().compose(new THREE.Vector3(x, y, z), _q.clone(), new THREE.Vector3(sx, sy == null ? sx : sy, sz == null ? sx : sz)); }

const SKY = {};
function buildRenderer(canvas) {
  renderer = new THREE.WebGLRenderer({ canvas, antialias: QUALITY > 0, powerPreference: 'high-performance' });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, [0.75, 1.25, 2][QUALITY]));
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.0;
  renderer.shadowMap.enabled = QUALITY > 0; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  scene = new THREE.Scene();
  scene.fog = new THREE.FogExp2(0x9fb8a8, 0.012);
  camera = new THREE.PerspectiveCamera(70, window.innerWidth / window.innerHeight, 0.08, 420);
  scene.add(camera);
}

function buildSky() {
  const mat = new THREE.ShaderMaterial({
    side: THREE.BackSide, depthWrite: false, fog: false,
    uniforms: { top: { value: new THREE.Color(0x3a78c8) }, hor: { value: new THREE.Color(0xb8d4e0) }, bot: { value: new THREE.Color(0x2a3020) }, sunDir: { value: new THREE.Vector3(0, 1, 0) }, sunCol: { value: new THREE.Color(1, 0.9, 0.7) }, sunS: { value: 1 }, moonDir: { value: new THREE.Vector3(0, 1, 0) }, moonS: { value: 0 } },
    vertexShader: 'varying vec3 vD; void main(){ vD = normalize(position); gl_Position = projectionMatrix * modelViewMatrix * vec4(position,1.0); gl_Position.z = gl_Position.w; }',
    fragmentShader: `uniform vec3 top,hor,bot,sunCol,sunDir,moonDir; uniform float sunS,moonS; varying vec3 vD;
      void main(){ float h = vD.y; vec3 c = h>0.0 ? mix(hor, top, pow(clamp(h,0.0,1.0),0.55)) : mix(hor, bot, clamp(-h*4.0,0.0,1.0));
      float sd = max(dot(normalize(vD), sunDir),0.0); c += sunCol * (pow(sd, 900.0)*6.0 + pow(sd, 12.0)*0.35 + pow(sd,3.0)*0.12) * sunS;
      float md = max(dot(normalize(vD), moonDir),0.0); c += vec3(0.85,0.9,1.0) * (smoothstep(0.9993,0.9996,md)*1.6 + pow(md,60.0)*0.25 + pow(md,6.0)*0.04) * moonS;
      gl_FragColor = vec4(c,1.0);
      #include <tonemapping_fragment>
      #include <colorspace_fragment>
      }`
  });
  SKY.mat = mat; SKY.mesh = new THREE.Mesh(new THREE.SphereGeometry(400, 32, 16), mat); SKY.mesh.renderOrder = -10; scene.add(SKY.mesh);
  // stars
  const n = 1400, p = new Float32Array(n * 3);
  for (let i = 0; i < n; i++) { const u = Math.random() * TAU, v = Math.random() * 0.95 + 0.05, r = 390; const s = Math.sqrt(1 - v * v); p[i * 3] = Math.cos(u) * s * r; p[i * 3 + 1] = v * r; p[i * 3 + 2] = Math.sin(u) * s * r; }
  const sg = new THREE.BufferGeometry(); sg.setAttribute('position', new THREE.BufferAttribute(p, 3));
  SKY.stars = new THREE.Points(sg, new THREE.PointsMaterial({ color: 0xffffff, size: 1.3, sizeAttenuation: false, transparent: true, opacity: 0, fog: false, depthWrite: false }));
  scene.add(SKY.stars);
  // lights
  SKY.hemi = new THREE.HemisphereLight(0xcfe6ff, 0x3a3020, 1.0); scene.add(SKY.hemi);
  SKY.sun = new THREE.DirectionalLight(0xfff0d8, 3); scene.add(SKY.sun); scene.add(SKY.sun.target);
  if (QUALITY > 0) {
    SKY.sun.castShadow = true; const sm = QUALITY === 2 ? 2048 : 1024; SKY.sun.shadow.mapSize.set(sm, sm);
    const sc = SKY.sun.shadow.camera; sc.left = -45; sc.right = 45; sc.top = 45; sc.bottom = -45; sc.near = 1; sc.far = 220; SKY.sun.shadow.bias = -0.0008; SKY.sun.shadow.normalBias = 0.04;
  }
}
const ENV_DAY = { top: 0x3a7ccc, hor: 0xc4dce6, bot: 0x3a4a30, fog: 0xa9c2b4, den: 0.011, hemi: 1.15, hs: 0xcfe6ff, hg: 0x4a4028, sun: 3.2, sc: 0xfff0d8, star: 0, moon: 0, exp: 1.0, sunS: 1 };
const ENV_DUSK = { top: 0x2a3a72, hor: 0xff9050, bot: 0x2a2018, fog: 0x7a5a48, den: 0.016, hemi: 0.6, hs: 0xffb080, hg: 0x302018, sun: 1.6, sc: 0xff9a50, star: 0.2, moon: 0, exp: 1.05, sunS: 1.4 };
const ENV_NIGHT = { top: 0x03060e, hor: 0x0c1626, bot: 0x020304, fog: 0x060b12, den: 0.032, hemi: 0.32, hs: 0x5a7ab8, hg: 0x0a0c10, sun: 0.45, sc: 0x9ab4ff, star: 1, moon: 1, exp: 1.25, sunS: 0 };
const _c1 = new THREE.Color(), _c2 = new THREE.Color();
function mixEnv(a, b, t) { const o = {}; for (const k in a) { if (k === 'top' || k === 'hor' || k === 'bot' || k === 'fog' || k === 'hs' || k === 'hg' || k === 'sc') { _c1.setHex(a[k]); _c2.setHex(b[k]); o[k] = _c1.clone().lerp(_c2, t); } else o[k] = lerp(a[k], b[k], t); } return o; }
function toCol(v) { return v.isColor ? v : new THREE.Color(v); }
function applyEnv(e, sunDir, moonDir, focus) {
  const u = SKY.mat.uniforms; u.top.value.copy(toCol(e.top)); u.hor.value.copy(toCol(e.hor)); u.bot.value.copy(toCol(e.bot));
  u.sunDir.value.copy(sunDir); u.sunCol.value.copy(toCol(e.sc)); u.sunS.value = e.sunS; u.moonDir.value.copy(moonDir); u.moonS.value = e.moon;
  scene.fog.color.copy(toCol(e.fog)); scene.fog.density = e.den;
  SKY.hemi.intensity = e.hemi; SKY.hemi.color.copy(toCol(e.hs)); SKY.hemi.groundColor.copy(toCol(e.hg));
  SKY.sun.intensity = e.sun; SKY.sun.color.copy(toCol(e.sc));
  const L = e.moon > 0.5 ? moonDir : sunDir;
  SKY.sun.position.set(focus.x + L.x * 90, focus.y + L.y * 90, focus.z + L.z * 90); SKY.sun.target.position.copy(focus);
  SKY.stars.material.opacity = e.star; SKY.stars.position.copy(camera.position);
  SKY.mesh.position.copy(camera.position);
  renderer.toneMappingExposure = e.exp;
}

const MAT = {};
function buildTerrain() {
  const seg = [90, 150, 200][QUALITY];
  const g = new THREE.PlaneGeometry(420, 420, seg, seg); g.rotateX(-Math.PI / 2);
  const p = g.attributes.position, col = new Float32Array(p.count * 3);
  for (let i = 0; i < p.count; i++) {
    const x = p.getX(i), z = p.getZ(i), h = terrainH(x, z); p.setY(i, h);
    const n = fbm(x * 0.04, z * 0.04, 3), m = fbm(x * 0.15 + 9, z * 0.15, 2);
    let r = 0.95 + m * 0.15, gg = 1.0 + n * 0.25, b = 0.85;
    const grassy = smooth(-0.1, 0.4, n); r = lerp(r, 0.78, grassy); gg = lerp(gg, 1.15, grassy); b = lerp(b, 0.6, grassy);
    const dp = Math.hypot(x - POND.x, z - POND.z); const mud = 1 - smooth(POND.r - 2, POND.r + 4, dp); r = lerp(r, 0.55, mud); gg = lerp(gg, 0.5, mud); b = lerp(b, 0.4, mud);
    const dc = Math.hypot(x, z); const path = 1 - smooth(3, 8, dc); r = lerp(r, 1.05, path); gg = lerp(gg, 0.9, path); b = lerp(b, 0.75, path);
    col[i * 3] = r; col[i * 3 + 1] = gg; col[i * 3 + 2] = b;
  }
  g.setAttribute('color', new THREE.BufferAttribute(col, 3)); g.computeVertexNormals();
  MAT.ground = new THREE.MeshLambertMaterial({ map: TEX.ground, vertexColors: true });
  const m = new THREE.Mesh(g, MAT.ground); m.receiveShadow = QUALITY > 0; scene.add(m);
}

function buildWater() {
  const c = cvs(256), g = c.getContext('2d'); const img = g.createImageData(256, 256);
  for (let y = 0; y < 256; y++) for (let x = 0; x < 256; x++) { const nx = Math.sin(x / 256 * TAU * 4) * 0.5 + fbm(x * 0.05, y * 0.05, 3), ny = Math.cos(y / 256 * TAU * 3) * 0.5 + fbm(x * 0.05 + 7, y * 0.05, 3); const i = (y * 256 + x) * 4; img.data[i] = 128 + nx * 60; img.data[i + 1] = 128 + ny * 60; img.data[i + 2] = 255; img.data[i + 3] = 255; }
  g.putImageData(img, 0, 0); const nt = new THREE.CanvasTexture(c); nt.wrapS = nt.wrapT = THREE.RepeatWrapping; nt.repeat.set(6, 6);
  MAT.water = new THREE.MeshPhongMaterial({ color: 0x0d2a2c, specular: 0x8aa8b0, shininess: 120, normalMap: nt, normalScale: new THREE.Vector2(0.6, 0.6), transparent: true, opacity: 0.88 });
  const w = new THREE.Mesh(new THREE.CircleGeometry(POND.r + 3, 48), MAT.water); w.rotation.x = -Math.PI / 2; w.position.set(POND.x, POND.lvl, POND.z); scene.add(w); MAT.waterTex = nt;
  // lily pads
  const pads = []; for (let i = 0; i < 26; i++) { const a = Math.random() * TAU, r = rr(3, POND.r - 2); pads.push(mtx(POND.x + Math.cos(a) * r, POND.lvl + 0.02, POND.z + Math.sin(a) * r, Math.random() * TAU, rr(0.4, 0.8), 1, rr(0.4, 0.8))); }
  const pg = new THREE.CircleGeometry(1, 12, 0.3, TAU - 0.3); pg.rotateX(-Math.PI / 2);
  instanced(pg, new THREE.MeshLambertMaterial({ color: 0x3a6a24, side: THREE.DoubleSide }), pads);
  // lotus flowers
  const lot = []; for (let i = 0; i < 8; i++) { const a = Math.random() * TAU, r = rr(3, POND.r - 3); lot.push(mtx(POND.x + Math.cos(a) * r, POND.lvl + 0.15, POND.z + Math.sin(a) * r, 0, 0.22)); }
  instanced(new THREE.ConeGeometry(1, 1.2, 6, 1, true), new THREE.MeshLambertMaterial({ color: 0xf2a0c0, side: THREE.DoubleSide, emissive: 0x401020 }), lot);
}

function canopyGeo(n, spread, jag) {
  const parts = [];
  for (let i = 0; i < n; i++) { const b = blob(rr(0.7, 1.1), 1, jag); const a = (i / n) * TAU + Math.random(); const r = i === 0 ? 0 : spread * rr(0.5, 1); b.translate(Math.cos(a) * r, rr(-0.25, 0.35) * (i ? 1 : 0) + (i === 0 ? 0.3 : 0), Math.sin(a) * r); const sh = rr(0.75, 1.05); tint(b, sh * 0.9, sh, sh * 0.85); parts.push(b); }
  return mergeGeos(parts);
}
function crossPlanes(w, h, n) { const parts = []; for (let i = 0; i < n; i++) { const p = new THREE.PlaneGeometry(w, h); p.translate(0, h / 2, 0); p.rotateY(i * Math.PI / n); parts.push(p); } return mergeGeos(parts); }

function buildVegetation() {
  srand(4242);
  const dens = [0.55, 1, 1.5][QUALITY];
  const tallT = [], tallC = [], midT = [], midC = [], bamboo = [], banana = [], fern = [], grass = [], rocks = [], logs = [];
  const ok = (x, z, clear) => { const d = Math.hypot(x, z); if (d < clear || d > 200) return false; if (Math.hypot(x - POND.x, z - POND.z) < POND.r + 2) return false; if (Math.hypot(x - SHRINE.x, z - SHRINE.z) < 7) return false; if (Math.hypot(x - TAKHIAN.x, z - TAKHIAN.z) < 10) return false; return true; };
  // tall emergent trees (yang na) - grid jitter
  for (let gx = -200; gx < 200; gx += 15) for (let gz = -200; gz < 200; gz += 15) {
    if (rnd() > 0.62) continue; const x = gx + rnd() * 12, z = gz + rnd() * 12; if (!ok(x, z, 14)) continue;
    const h = 16 + rnd() * 12, s = 0.8 + rnd() * 0.7, y = groundY(x, z) - 0.3;
    tallT.push(mtx(x, y, z, rnd() * TAU, s, h, s)); { const cs = 4.5 + rnd() * 3; tallC.push(mtx(x, y + h - 1, z, rnd() * TAU, cs, cs * (0.6 + rnd() * 0.2), cs * (0.85 + rnd() * 0.3))); } addCollider(x, z, 0.55 * s + 0.15);
  }
  // mid trees
  const nMid = 420 * dens | 0;
  for (let i = 0; i < nMid; i++) {
    const a = rnd() * TAU, r = 10 + Math.sqrt(rnd()) * 190, x = Math.cos(a) * r, z = Math.sin(a) * r; if (!ok(x, z, 11)) continue;
    const h = 5 + rnd() * 7, s = 0.5 + rnd() * 0.5, y = groundY(x, z) - 0.2;
    midT.push(mtx(x, y, z, rnd() * TAU, s, h, s, (rnd() - .5) * 0.12, (rnd() - .5) * 0.12)); { const cs = 2.2 + rnd() * 2; midC.push(mtx(x, y + h - 0.3, z, rnd() * TAU, cs, cs * (0.7 + rnd() * 0.25), cs * (0.85 + rnd() * 0.3))); } addCollider(x, z, 0.32 * s + 0.12);
  }
  // bamboo clumps
  for (let i = 0; i < 70 * dens; i++) { const a = rnd() * TAU, r = 18 + rnd() * 170, x = Math.cos(a) * r, z = Math.sin(a) * r; if (!ok(x, z, 14)) continue; bamboo.push(mtx(x, groundY(x, z) - 0.2, z, rnd() * TAU, 0.8 + rnd() * 0.5)); addCollider(x, z, 1.1); }
  // bananas
  for (let i = 0; i < 70 * dens; i++) { const a = rnd() * TAU, r = 9 + rnd() * 160, x = Math.cos(a) * r, z = Math.sin(a) * r; if (!ok(x, z, 9)) continue; banana.push(mtx(x, groundY(x, z) - 0.1, z, rnd() * TAU, 0.8 + rnd() * 0.6)); }
  // ferns & grass
  for (let i = 0; i < 1600 * dens; i++) { const a = rnd() * TAU, r = 4 + Math.sqrt(rnd()) * 190, x = Math.cos(a) * r, z = Math.sin(a) * r; if (!ok(x, z, 4)) continue; const s = 0.7 + rnd() * 0.9; fern.push(mtx(x, groundY(x, z) - 0.05, z, rnd() * TAU, s * 1.3, s, s * 1.3)); }
  for (let i = 0; i < 2600 * dens; i++) { const a = rnd() * TAU, r = 3 + Math.sqrt(rnd()) * 190, x = Math.cos(a) * r, z = Math.sin(a) * r; if (Math.hypot(x - SHRINE.x, z - SHRINE.z) < 3) continue; if (Math.hypot(x - POND.x, z - POND.z) < POND.r - 2) continue; const s = 0.5 + rnd() * 0.7; grass.push(mtx(x, groundY(x, z) - 0.05, z, rnd() * TAU, s * 1.2, s, s * 1.2)); }
  for (let i = 0; i < 110 * dens; i++) { const a = rnd() * TAU, r = 8 + rnd() * 185, x = Math.cos(a) * r, z = Math.sin(a) * r; if (!ok(x, z, 8)) continue; const s = 0.3 + rnd() * rnd() * 1.8; rocks.push(mtx(x, groundY(x, z) - s * 0.3, z, rnd() * TAU, s, s * (0.5 + rnd() * 0.4), s, rnd(), rnd())); if (s > 0.9) addCollider(x, z, s * 0.8, s); }
  for (let i = 0; i < 26 * dens; i++) { const a = rnd() * TAU, r = 14 + rnd() * 160, x = Math.cos(a) * r, z = Math.sin(a) * r; if (!ok(x, z, 12)) continue; const L = 4 + rnd() * 6, ry = rnd() * TAU; logs.push(mtx(x, groundY(x, z) + 0.25, z, ry, 0.45 + rnd() * 0.25, L, 0.45 + rnd() * 0.25, 0, Math.PI / 2)); addCollider(x, z, 0.8, 1); addCollider(x + Math.cos(ry) * L * 0.35, z - Math.sin(ry) * L * 0.35, 0.6, 1); addCollider(x - Math.cos(ry) * L * 0.35, z + Math.sin(ry) * L * 0.35, 0.6, 1); }

  // materials
  MAT.barkPale = new THREE.MeshLambertMaterial({ map: TEX.barkPale }); TEX.barkPale.repeat.set(2, 6);
  MAT.bark = new THREE.MeshLambertMaterial({ map: TEX.bark }); TEX.bark.repeat.set(2, 3);
  MAT.leaf = swayMat(new THREE.MeshLambertMaterial({ map: TEX.leaf, alphaTest: 0.45, side: THREE.DoubleSide, vertexColors: true, color: 0xb8d0a0 }), 0.01);
  MAT.leafDark = swayMat(new THREE.MeshLambertMaterial({ map: TEX.leaf, alphaTest: 0.45, side: THREE.DoubleSide, vertexColors: true, color: 0x8aa878 }), 0.015);
  MAT.fern = swayMat(new THREE.MeshLambertMaterial({ map: TEX.fern, alphaTest: 0.5, side: THREE.DoubleSide }), 0.06);
  MAT.grass = swayMat(new THREE.MeshLambertMaterial({ map: TEX.grass, alphaTest: 0.5, side: THREE.DoubleSide, color: 0xc8d8a0 }), 0.1);
  MAT.wideLeaf = swayMat(new THREE.MeshLambertMaterial({ map: TEX.wideLeaf, alphaTest: 0.5, side: THREE.DoubleSide }), 0.03);
  MAT.rock = new THREE.MeshLambertMaterial({ color: 0x8a8a7a, map: TEX.barkPale });
  const trunkG = new THREE.CylinderGeometry(0.33, 0.55, 1, 9, 1, true); trunkG.translate(0, 0.5, 0);
  // flare base
  { const p = trunkG.attributes.position; for (let i = 0; i < p.count; i++) if (p.getY(i) < 0.01) { p.setX(i, p.getX(i) * 1.7); p.setZ(i, p.getZ(i) * 1.7); } trunkG.computeVertexNormals(); }
  instanced(trunkG, MAT.barkPale, tallT, true);
  instanced(canopyGeo(6, 1.0, 0.35), MAT.leaf, tallC, true);
  const t2 = new THREE.CylinderGeometry(0.25, 0.4, 1, 7, 1, true); t2.translate(0, 0.5, 0);
  instanced(t2, MAT.bark, midT, QUALITY === 2);
  instanced(canopyGeo(4, 0.8, 0.4), MAT.leafDark, midC, true);
  // bamboo
  { const parts = []; for (let i = 0; i < 12; i++) { const h = rr(7, 12), c = new THREE.CylinderGeometry(0.06, 0.08, h, 5, 1, true); c.translate(0, h / 2, 0); c.rotateZ(rr(-0.25, 0.25)); c.rotateX(rr(-0.25, 0.25)); c.translate(rr(-0.6, 0.6), 0, rr(-0.6, 0.6)); tint(c, 0.55, 0.65, 0.25); parts.push(c); }
    const bmat = new THREE.MeshLambertMaterial({ color: 0xc8c070, vertexColors: true }); instanced(mergeGeos(parts), bmat, bamboo, QUALITY === 2);
    const lp = []; for (let i = 0; i < 10; i++) { const b = blob(rr(1, 1.6), 1, 0.5); b.scale(1, 1.6, 1); b.translate(rr(-1.5, 1.5), rr(6, 10), rr(-1.5, 1.5)); tint(b, 0.9, 1.05, 0.6); lp.push(b); }
    instanced(mergeGeos(lp), MAT.leaf, bamboo, true); }
  // banana
  { const parts = []; const tr = new THREE.CylinderGeometry(0.12, 0.18, 2.2, 7); tr.translate(0, 1.1, 0); tint(tr, 0.5, 0.6, 0.3); parts.push(tr);
    for (let i = 0; i < 7; i++) { const l = new THREE.PlaneGeometry(0.9, 2.6, 1, 4); const p = l.attributes.position; for (let k = 0; k < p.count; k++) { const yy = p.getY(k) + 1.3; p.setZ(k, -yy * yy * 0.12); p.setY(k, yy); } l.rotateX(-0.6 - Math.random() * 0.5); l.rotateY(i / 7 * TAU + Math.random() * 0.3); l.translate(0, 2.1, 0); parts.push(l); }
    instanced(mergeGeos(parts), MAT.wideLeaf, banana, QUALITY === 2); }
  instanced(crossPlanes(1.6, 1.1, 3), MAT.fern, fern);
  instanced(crossPlanes(1.1, 0.9, 2), MAT.grass, grass);
  { const rg = new THREE.DodecahedronGeometry(1, 1), p = rg.attributes.position; for (let i = 0; i < p.count; i++) { _v.fromBufferAttribute(p, i); _v.multiplyScalar(1 + noise2(_v.x * 2, _v.z * 2 + _v.y) * 0.2); p.setXYZ(i, _v.x, _v.y, _v.z); } rg.computeVertexNormals(); instanced(rg, MAT.rock, rocks, true); }
  { const lg = new THREE.CylinderGeometry(1, 1, 1, 9); instanced(lg, MAT.bark, logs, true); }
}

const STAND = {};
function buildBigTree() {
  const g = new THREE.Group(); scene.add(g);
  const bm = new THREE.MeshLambertMaterial({ map: TEX.barkPale.clone() }); bm.map.repeat.set(3, 3); bm.map.needsUpdate = true;
  const trunk = new THREE.Mesh(new THREE.CylinderGeometry(1.05, 1.55, STAND_Y + 0.2, 18, 4), bm); trunk.position.y = (STAND_Y + 0.2) / 2 - 0.3; trunk.castShadow = true; trunk.receiveShadow = true; g.add(trunk);
  // buttress roots
  const sh = new THREE.Shape(); sh.moveTo(0, 0); sh.lineTo(3.2, 0); sh.quadraticCurveTo(1.2, 0.6, 0.9, 3.6); sh.lineTo(0, 3.8); sh.lineTo(0, 0);
  const rg = new THREE.ExtrudeGeometry(sh, { depth: 0.32, bevelEnabled: false }); rg.translate(0, 0, -0.16);
  for (let i = 0; i < 6; i++) { const r = new THREE.Mesh(rg, bm); const a = i / 6 * TAU + 0.3; r.rotation.y = a; r.position.set(Math.cos(a) * 0.9, -0.3, -Math.sin(a) * 0.9); r.scale.set(rr(0.8, 1.2), rr(0.8, 1.1), 1); r.castShadow = true; g.add(r); }
  // limbs
  const limbG = new THREE.CylinderGeometry(0.32, 0.6, 1, 10, 1, true); limbG.translate(0, 0.5, 0);
  const canopy = [];
  for (let i = 0; i < 4; i++) {
    const a = i / 4 * TAU + Math.PI / 4, L = 12 + Math.random() * 3, tilt = 0.72 + Math.random() * 0.1;
    const l = new THREE.Mesh(limbG, bm); l.scale.set(1, L, 1); l.position.set(Math.cos(a) * 1.0, STAND_Y - 1.6, Math.sin(a) * 1.0);
    l.rotation.order = 'YXZ'; l.rotation.y = -a + Math.PI / 2; l.rotation.x = tilt; l.castShadow = true; g.add(l);
    const tip = new THREE.Vector3(0, L, 0).applyEuler(l.rotation).add(l.position); canopy.push(tip);
    // sub-branch
    const sb = new THREE.Mesh(limbG, bm); sb.scale.set(0.5, 6, 0.5); sb.position.copy(new THREE.Vector3(0, L * 0.55, 0).applyEuler(l.rotation).add(l.position)); sb.rotation.set(rr(-.8, .8), rr(0, TAU), rr(-.8, .8)); g.add(sb);
    canopy.push(new THREE.Vector3(0, 6, 0).applyEuler(sb.rotation).add(sb.position));
  }
  const cg = canopyGeo(7, 1.1, 0.35); const cm = []; canopy.forEach(t => cm.push(mtx(t.x, t.y + 1, t.z, Math.random() * TAU, rr(5, 7), rr(2.6, 3.4), rr(5, 7)))); cm.push(mtx(0, STAND_Y + 16, 0, 0, 8, 3.5, 8));
  instanced(cg, MAT.leaf, cm, true);
  // vines
  const vm = new THREE.MeshLambertMaterial({ color: 0x3a4a1a });
  for (let i = 0; i < 9; i++) { const t = canopy[i % canopy.length]; const len = rr(6, t.y - 1); const v = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.04, len, 4), vm); v.position.set(t.x * rr(0.5, 0.9), t.y - len / 2, t.z * rr(0.5, 0.9)); g.add(v); }
  // platform
  const wm = new THREE.MeshLambertMaterial({ map: TEX.wood }); const bam = new THREE.MeshLambertMaterial({ color: 0xa89050 });
  const S = 2.3; STAND.half = S;
  for (const z of [-1.6, 1.6]) { const b = new THREE.Mesh(new THREE.BoxGeometry(5.4, 0.22, 0.22), wm); b.position.set(0, STAND_Y - 0.18, z); b.castShadow = true; g.add(b); }
  for (let i = 0; i < 12; i++) { const p = new THREE.Mesh(new THREE.BoxGeometry(0.36, 0.07, S * 2 + rr(-0.1, 0.3)), wm); p.position.set(-S + 0.2 + i * 0.39, STAND_Y - 0.03, rr(-0.08, 0.08)); p.rotation.y = rr(-0.03, 0.03); p.receiveShadow = true; p.castShadow = true; g.add(p); }
  // posts & rails
  const postG = new THREE.CylinderGeometry(0.07, 0.08, 3.0, 6);
  for (const [x, z] of [[-S, -S], [S, -S], [S, S], [-S, S]]) { const p = new THREE.Mesh(postG, bam); p.position.set(x, STAND_Y + 1.3, z); p.castShadow = true; g.add(p); }
  const railG = new THREE.CylinderGeometry(0.045, 0.045, S * 2, 6); railG.rotateZ(Math.PI / 2);
  for (const h of [0.5, 0.95]) for (let s = 0; s < 4; s++) { const r = new THREE.Mesh(railG, bam); r.position.set(s === 0 ? 0 : s === 1 ? S : s === 2 ? 0 : -S, STAND_Y + h, s === 0 ? -S : s === 1 ? 0 : s === 2 ? S : 0); r.rotation.y = s % 2 ? Math.PI / 2 : 0; g.add(r); }
  // roof
  const tm = new THREE.MeshLambertMaterial({ map: TEX.thatch, side: THREE.DoubleSide }); TEX.thatch.repeat.set(3, 2);
  for (const sd of [-1, 1]) { const r = new THREE.Mesh(new THREE.PlaneGeometry(S * 2 + 1.4, 2.0), tm); r.position.set(0, STAND_Y + 3.0, sd * 0.85); r.rotation.x = -Math.PI / 2 + sd * 0.6; r.castShadow = true; g.add(r); }
  // ladder rungs
  for (let y = 0.6; y < STAND_Y - 0.3; y += 0.5) { const r = new THREE.Mesh(new THREE.BoxGeometry(0.7, 0.07, 0.1), wm); r.position.set(0, y, 1.55 - y * 0.05); g.add(r); }
  // lantern
  const lan = new THREE.Group(); lan.position.set(S - 0.05, STAND_Y + 2.3, 0); g.add(lan);
  const lg = new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.12, 0.25, 8), new THREE.MeshBasicMaterial({ color: 0xffb050 })); lan.add(lg);
  const lgs = new THREE.Sprite(new THREE.SpriteMaterial({ map: TEX.glow, color: 0xffa040, blending: THREE.AdditiveBlending, depthWrite: false, transparent: true, opacity: 0.4 })); lgs.scale.set(0.8, 0.8, 1); lan.add(lgs);
  STAND.lanternLight = new THREE.PointLight(0xff9a40, 0, 16, 2); lan.add(STAND.lanternLight); STAND.lanternGlow = lgs; STAND.lanternMesh = lg;
  // items on stand: rice sack, water jar
  const jar = new THREE.Mesh(new THREE.SphereGeometry(0.22, 10, 8), new THREE.MeshLambertMaterial({ color: 0x6a3a1a })); jar.position.set(-S + 0.4, STAND_Y + 0.2, S - 0.4); g.add(jar);
  const sack = new THREE.Mesh(new THREE.SphereGeometry(0.28, 8, 6), new THREE.MeshLambertMaterial({ color: 0xd8c8a0 })); sack.scale.y = 0.7; sack.position.set(-S + 0.45, STAND_Y + 0.15, -S + 0.45); g.add(sack);
  // sai sin (holy thread) around the stand
  const th = new THREE.Line(new THREE.BufferGeometry().setFromPoints([[-S, -S], [S, -S], [S, S], [-S, S], [-S, -S]].map(([x, z]) => new THREE.Vector3(x, STAND_Y + 1.0, z))), new THREE.LineBasicMaterial({ color: 0xf0ece0 })); g.add(th);
  // campfire below
  const cf = new THREE.Group(); cf.position.set(3.8, groundY(3.8, 2.5), 2.5); g.add(cf); STAND.fire = cf;
  const stm = new THREE.MeshLambertMaterial({ color: 0x5a5850 });
  for (let i = 0; i < 9; i++) { const s = new THREE.Mesh(new THREE.DodecahedronGeometry(0.18), stm); const a = i / 9 * TAU; s.position.set(Math.cos(a) * 0.6, 0.08, Math.sin(a) * 0.6); cf.add(s); }
  for (let i = 0; i < 4; i++) { const l = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.07, 1, 6), new THREE.MeshLambertMaterial({ color: 0x2a1a10 })); l.rotation.z = Math.PI / 2 - 0.4; l.rotation.y = i / 4 * TAU; l.position.y = 0.2; cf.add(l); }
  STAND.flames = [];
  for (let i = 0; i < 3; i++) { const f = new THREE.Sprite(new THREE.SpriteMaterial({ map: TEX.glow, color: i ? 0xff7a20 : 0xffd080, blending: THREE.AdditiveBlending, depthWrite: false, transparent: true })); f.position.y = 0.4 + i * 0.2; f.scale.set(0.9 - i * 0.2, 1.2 - i * 0.2, 1); cf.add(f); STAND.flames.push(f); }
  STAND.fireLight = new THREE.PointLight(0xff7a30, 12, 14, 2); STAND.fireLight.position.y = 0.8; cf.add(STAND.fireLight);
  addCollider(0, 0, 2.0, STAND_Y - 0.6); addCollider(cf.position.x, cf.position.z, 0.8, 0.5);
  STAND.group = g;
}

function buildShrine() {
  const g = new THREE.Group(); const y0 = groundY(SHRINE.x, SHRINE.z); g.position.set(SHRINE.x, y0, SHRINE.z); g.rotation.y = 0.8; scene.add(g);
  const white = new THREE.MeshLambertMaterial({ color: 0xe8e2d4 }), red = new THREE.MeshLambertMaterial({ color: 0x9a1a14 }), gold = new THREE.MeshPhongMaterial({ color: 0xd4a020, emissive: 0x302000, shininess: 80, specular: 0xffe080 });
  const base = new THREE.Mesh(new THREE.BoxGeometry(1.2, 0.3, 1.2), white); base.position.y = 0.15; g.add(base);
  const post = new THREE.Mesh(new THREE.BoxGeometry(0.28, 1.5, 0.28), white); post.position.y = 1.0; g.add(post);
  const plat = new THREE.Mesh(new THREE.BoxGeometry(1.3, 0.1, 1.3), red); plat.position.y = 1.8; g.add(plat);
  const house = new THREE.Mesh(new THREE.BoxGeometry(0.7, 0.6, 0.6), red); house.position.y = 2.15; g.add(house);
  for (const sd of [-1, 1]) { const r = new THREE.Mesh(new THREE.BoxGeometry(0.95, 0.04, 0.55), gold); r.position.set(0, 2.62, sd * 0.2); r.rotation.x = sd * 0.75; g.add(r); }
  const chofa = new THREE.Mesh(new THREE.ConeGeometry(0.05, 0.3, 5), gold); chofa.position.set(0, 2.9, 0); g.add(chofa);
  for (const sx of [-0.45, 0.45]) { const c = new THREE.Mesh(new THREE.ConeGeometry(0.03, 0.25, 4), gold); c.position.set(sx, 2.65, 0); c.rotation.z = -sx * 1.5; g.add(c); }
  const door = new THREE.Mesh(new THREE.PlaneGeometry(0.25, 0.4), new THREE.MeshBasicMaterial({ color: 0x1a0a04 })); door.position.set(0, 2.1, 0.31); g.add(door);
  // garlands & figurines
  const gar = new THREE.Mesh(new THREE.TorusGeometry(0.15, 0.04, 6, 14), new THREE.MeshLambertMaterial({ color: 0xffe0f0, emissive: 0x202000 })); gar.position.set(0.3, 1.9, 0.5); gar.rotation.x = 1.3; g.add(gar);
  for (let i = 0; i < 4; i++) { const f = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.05, 0.18, 6), i % 2 ? new THREE.MeshLambertMaterial({ color: 0x3a2a1a }) : new THREE.MeshLambertMaterial({ color: 0xf0e8d0 })); f.position.set(-0.5 + i * 0.1, 1.94, 0.5); g.add(f); }
  // zebra figures
  for (let i = 0; i < 2; i++) { const z = new THREE.Mesh(new THREE.BoxGeometry(0.1, 0.12, 0.2), new THREE.MeshLambertMaterial({ color: 0xdddddd })); z.position.set(0.4 + i * 0.15, 0.36, 0.5); g.add(z); }
  // incense glow
  const inc = new THREE.Sprite(new THREE.SpriteMaterial({ map: TEX.glow, color: 0xff5020, blending: THREE.AdditiveBlending, depthWrite: false, transparent: true })); inc.position.set(0, 2.0, 0.55); inc.scale.set(0.15, 0.15, 1); g.add(inc);
  addCollider(SHRINE.x, SHRINE.z, 1.0, 3);
  return g;
}

function buildTakhian() {
  const g = new THREE.Group(); g.position.set(TAKHIAN.x, groundY(TAKHIAN.x, TAKHIAN.z) - 0.5, TAKHIAN.z); scene.add(g);
  const bm = new THREE.MeshLambertMaterial({ map: TEX.bark.clone(), color: 0xc8b8a8 }); bm.map.repeat.set(4, 6); bm.map.needsUpdate = true;
  const t = new THREE.Mesh(new THREE.CylinderGeometry(1.4, 2.4, 30, 16, 6), bm); t.position.y = 15; t.castShadow = true; g.add(t);
  const p = t.geometry.attributes.position; for (let i = 0; i < p.count; i++) { const y = p.getY(i); p.setX(i, p.getX(i) * (1 + Math.sin(y * 0.7) * 0.08)); } t.geometry.computeVertexNormals();
  const rm = new THREE.MeshLambertMaterial({ map: TEX.ribbon, side: THREE.DoubleSide, emissive: 0x100008 });
  for (let i = 0; i < 4; i++) { const r = new THREE.Mesh(new THREE.CylinderGeometry(2.38 - i * 0.12, 2.42 - i * 0.12, 0.6, 18, 1, true), rm); r.position.y = 1.2 + i * 0.9; g.add(r); }
  const cg = canopyGeo(8, 1.2, 0.35); instanced(cg, MAT.leafDark, [mtx(TAKHIAN.x, g.position.y + 31, TAKHIAN.z, 0, 10, 4, 10), mtx(TAKHIAN.x + 6, g.position.y + 26, TAKHIAN.z + 3, 1, 6, 3, 6)], true);
  // offerings: thai dresses on hanger, flower garlands
  const dm = new THREE.MeshLambertMaterial({ color: 0xc01860, side: THREE.DoubleSide });
  for (let i = 0; i < 3; i++) { const d = new THREE.Mesh(new THREE.ConeGeometry(0.4, 1.3, 8, 1, true), i === 1 ? new THREE.MeshLambertMaterial({ color: 0x1a8a40, side: THREE.DoubleSide }) : dm); const a = 0.4 + i * 0.6; d.position.set(Math.cos(a) * 2.9, 1.1, Math.sin(a) * 2.9); g.add(d); }
  addCollider(TAKHIAN.x, TAKHIAN.z, 2.6, 30);
  return g;
}

const FX = {};
function buildAtmos() {
  // fireflies
  const n = [60, 140, 220][QUALITY]; const p = new Float32Array(n * 3); FX.ffSeed = [];
  for (let i = 0; i < n; i++) { FX.ffSeed.push([Math.random() * TAU, rr(5, 45), rr(0.5, 5), Math.random() * 10]); }
  const g = new THREE.BufferGeometry(); g.setAttribute('position', new THREE.BufferAttribute(p, 3));
  FX.ff = new THREE.Points(g, new THREE.PointsMaterial({ map: TEX.glow, color: 0xd8ff70, size: 0.35, transparent: true, opacity: 0, blending: THREE.AdditiveBlending, depthWrite: false })); FX.ff.frustumCulled = false; scene.add(FX.ff);
  // ground mist sprites
  FX.mist = []; const mm = new THREE.SpriteMaterial({ map: TEX.puff, color: 0x9aa8b8, transparent: true, opacity: 0, depthWrite: false, fog: true });
  for (let i = 0; i < [8, 18, 28][QUALITY]; i++) { const s = new THREE.Sprite(mm); const a = Math.random() * TAU, r = rr(8, 60); s.position.set(Math.cos(a) * r, 0, Math.sin(a) * r); s.position.y = groundY(s.position.x, s.position.z) + rr(0.8, 2); s.scale.set(rr(14, 24), rr(4, 6), 1); s.userData.v = rr(-0.3, 0.3); scene.add(s); FX.mist.push(s); }
  FX.mistMat = mm;
  // sun rays dust (day) - floating motes
  const dn = 120, dp = new Float32Array(dn * 3); for (let i = 0; i < dn; i++) { dp[i * 3] = rr(-20, 20); dp[i * 3 + 1] = rr(0.5, 8); dp[i * 3 + 2] = rr(-20, 20); }
  const dg = new THREE.BufferGeometry(); dg.setAttribute('position', new THREE.BufferAttribute(dp, 3));
  FX.dust = new THREE.Points(dg, new THREE.PointsMaterial({ map: TEX.glow, color: 0xfff0c0, size: 0.07, transparent: true, opacity: 0.0, blending: THREE.AdditiveBlending, depthWrite: false })); FX.dust.frustumCulled = false; scene.add(FX.dust);
  // rain
  const rn = [400, 900, 1500][QUALITY], rp = new Float32Array(rn * 6); for (let i = 0; i < rn; i++) { const x = rr(-30, 30), y = rr(0, 30), z = rr(-30, 30); rp.set([x, y, z, x + 0.05, y - 0.6, z], i * 6); }
  const rg = new THREE.BufferGeometry(); rg.setAttribute('position', new THREE.BufferAttribute(rp, 3));
  FX.rain = new THREE.LineSegments(rg, new THREE.LineBasicMaterial({ color: 0x9ab0c0, transparent: true, opacity: 0, depthWrite: false })); FX.rain.frustumCulled = false; scene.add(FX.rain);
}
function updateAtmos(dt, t, night01, center, raining) {
  const p = FX.ff.geometry.attributes.position;
  for (let i = 0; i < FX.ffSeed.length; i++) { const s = FX.ffSeed[i]; const a = s[0] + t * 0.05 * (i % 2 ? 1 : -1), r = s[1]; const x = Math.cos(a) * r + Math.sin(t * 0.7 + s[3]) * 1.5, z = Math.sin(a) * r + Math.cos(t * 0.5 + s[3]) * 1.5; p.setXYZ(i, x, groundY(x, z) + s[2] + Math.sin(t * 1.3 + s[3]) * 0.4, z); }
  p.needsUpdate = true; FX.ff.material.opacity = night01 * (0.6 + 0.4 * Math.sin(t * 3)) * (raining ? 0.2 : 1);
  FX.mistMat.opacity = night01 * 0.28 + 0.05;
  for (const s of FX.mist) { s.position.x += s.userData.v * dt; if (Math.abs(s.position.x) > 70) s.userData.v *= -1; }
  FX.dust.position.set(center.x, 0, center.z); FX.dust.material.opacity = (1 - night01) * 0.5;
  FX.rain.material.opacity = raining ? 0.35 : 0; if (raining) { FX.rain.position.set(center.x, center.y - 10 - ((t * 22) % 30) + 30, center.z); if (FX.rain.position.y > center.y + 5) FX.rain.position.y -= 30; }
  W.wind.value = raining ? 2.2 : 1;
  if (MAT.waterTex) { MAT.waterTex.offset.x = t * 0.01; MAT.waterTex.offset.y = t * 0.007; }
  // fire flicker
  STAND.flames.forEach((f, i) => { const k = 0.8 + Math.sin(t * (9 + i * 3)) * 0.12 + Math.random() * 0.1; f.scale.set((0.9 - i * 0.2) * k, (1.2 - i * 0.2) * k * 1.2, 1); f.material.opacity = 0.5 + night01 * 0.5; });
  STAND.fireLight.intensity = (4 + night01 * 18) * (0.85 + Math.random() * 0.15) * (raining ? 0.5 : 1);
  STAND.lanternLight.intensity = night01 * 0.8 * (0.92 + Math.random() * 0.08); STAND.lanternGlow.material.opacity = 0.1 + night01 * 0.35;
}

function buildWorld() {
  buildSky(); buildTerrain(); buildWater(); buildVegetation(); buildBigTree(); FX.shrine = buildShrine(); FX.takhian = buildTakhian(); buildAtmos();
}
