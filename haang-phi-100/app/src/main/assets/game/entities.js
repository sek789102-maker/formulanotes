'use strict';
// ===== shared helpers for models =====
const lam = (c, o = {}) => new THREE.MeshLambertMaterial(Object.assign({ color: c }, o));
const basic = (c, o = {}) => new THREE.MeshBasicMaterial(Object.assign({ color: c }, o));
function mesh(geo, mat, x = 0, y = 0, z = 0, parent) { const m = new THREE.Mesh(geo, mat); m.position.set(x, y, z); m.castShadow = QUALITY > 0; if (parent) parent.add(m); return m; }
function glowSprite(color, size, opacity = 1, parent) { const s = new THREE.Sprite(new THREE.SpriteMaterial({ map: TEX.glow, color, blending: THREE.AdditiveBlending, depthWrite: false, transparent: true, opacity })); s.scale.set(size, size, 1); if (parent) parent.add(s); return s; }
function eyes(parent, x, y, z, sep, size, color) { const m = basic(color, { fog: false }); const g = new THREE.SphereGeometry(size, 6, 5); const a = mesh(g, m, x - sep, y, z, parent), b = mesh(g, m, x + sep, y, z, parent); glowSprite(color, size * 9, 0.7, a); glowSprite(color, size * 9, 0.7, b); return [a, b]; }
function limb(len, r0, r1, mat) { const g = new THREE.CylinderGeometry(r1, r0, len, 6); g.translate(0, -len / 2, 0); return new THREE.Mesh(g, mat); }

// ===== particles =====
const PARTS = [];
function spawnParts(pos, n, color, speed, life, size, grav = 0, additive = true) {
  for (let i = 0; i < n; i++) {
    const s = new THREE.Sprite(new THREE.SpriteMaterial({ map: TEX.glow, color, transparent: true, depthWrite: false, blending: additive ? THREE.AdditiveBlending : THREE.NormalBlending }));
    s.position.copy(pos); const sz = size * rr(0.6, 1.3); s.scale.set(sz, sz, 1); scene.add(s);
    const v = new THREE.Vector3(rr(-1, 1), rr(-0.4, 1), rr(-1, 1)).normalize().multiplyScalar(speed * rr(0.3, 1));
    PARTS.push({ s, v, life: life * rr(0.6, 1.2), max: life, grav, sz });
  }
}
function updateParts(dt) {
  for (let i = PARTS.length - 1; i >= 0; i--) { const p = PARTS[i]; p.life -= dt; if (p.life <= 0) { scene.remove(p.s); p.s.material.dispose(); PARTS.splice(i, 1); continue; } p.v.y -= p.grav * dt; p.s.position.addScaledVector(p.v, dt); const k = p.life / p.max; p.s.material.opacity = k; p.s.scale.setScalar(p.sz * (0.5 + k * 0.5)); }
}

// ===== animals =====
const ANIMALS = {
  chicken: { name: 'ไก่ป่า', hp: 10, speed: 5.5, meat: 1, r: 0.35, cy: 0.35, flee: 11 },
  rabbit: { name: 'กระต่ายป่า', hp: 10, speed: 7, meat: 1, r: 0.32, cy: 0.25, flee: 12 },
  deer: { name: 'เก้ง', hp: 20, speed: 9, meat: 3, r: 0.6, cy: 0.75, flee: 20 },
  boar: { name: 'หมูป่า', hp: 45, speed: 6.8, meat: 5, r: 0.75, cy: 0.55, flee: 0, aggro: true },
};
function buildAnimal(type) {
  const g = new THREE.Group(); const legs = []; let body;
  if (type === 'chicken') {
    const bm = lam(0xa0401a), dk = lam(0x1a2a1a);
    body = mesh(new THREE.SphereGeometry(0.22, 10, 8), bm, 0, 0.36, 0, g); body.scale.set(1, 0.9, 1.3);
    const n = mesh(new THREE.SphereGeometry(0.1, 8, 6), lam(0xd08030), 0, 0.55, 0.2, g);
    mesh(new THREE.BoxGeometry(0.03, 0.08, 0.1), lam(0xd01010), 0, 0.66, 0.2, g);
    mesh(new THREE.ConeGeometry(0.03, 0.08, 4), lam(0xd0b040), 0, 0.55, 0.32, g).rotation.x = Math.PI / 2;
    const tail = mesh(new THREE.TorusGeometry(0.16, 0.04, 5, 8, Math.PI), dk, 0, 0.5, -0.25, g); tail.rotation.y = Math.PI / 2;
    for (const sx of [-0.07, 0.07]) { const l = limb(0.22, 0.015, 0.02, lam(0x8a7a40)); l.position.set(sx, 0.24, 0); g.add(l); legs.push(l); }
  } else if (type === 'rabbit') {
    const bm = lam(0x7a6450);
    body = mesh(new THREE.SphereGeometry(0.22, 10, 8), bm, 0, 0.22, 0, g); body.scale.set(0.9, 0.85, 1.25);
    mesh(new THREE.SphereGeometry(0.13, 8, 6), bm, 0, 0.36, 0.24, g);
    for (const sx of [-0.05, 0.05]) { const e = mesh(new THREE.CapsuleGeometry(0.03, 0.16, 3, 6), bm, sx, 0.54, 0.2, g); e.rotation.x = -0.3; }
    mesh(new THREE.SphereGeometry(0.06, 6, 5), lam(0xeeeeee), 0, 0.25, -0.27, g);
    for (const [sx, sz] of [[-0.1, 0.12], [0.1, 0.12], [-0.12, -0.12], [0.12, -0.12]]) { const l = limb(0.12, 0.03, 0.04, bm); l.position.set(sx, 0.12, sz); g.add(l); legs.push(l); }
  } else if (type === 'deer') {
    const bm = lam(0x9a5a2a), bl = lam(0xe8d8c0);
    body = mesh(new THREE.CapsuleGeometry(0.25, 0.7, 4, 10), bm, 0, 0.82, 0, g); body.rotation.x = Math.PI / 2;
    const neck = mesh(new THREE.CylinderGeometry(0.09, 0.13, 0.5, 7), bm, 0, 1.12, 0.45, g); neck.rotation.x = 0.5;
    const head = mesh(new THREE.ConeGeometry(0.12, 0.38, 7), bm, 0, 1.34, 0.65, g); head.rotation.x = Math.PI / 2 + 0.3;
    for (const sx of [-0.06, 0.06]) { mesh(new THREE.ConeGeometry(0.015, 0.18, 4), lam(0x3a2a1a), sx, 1.48, 0.55, g); mesh(new THREE.ConeGeometry(0.05, 0.14, 4), bm, sx * 2, 1.43, 0.5, g).rotation.z = sx * 12; }
    mesh(new THREE.SphereGeometry(0.07, 6, 5), bl, 0, 0.92, -0.52, g);
    for (const [sx, sz] of [[-0.14, 0.32], [0.14, 0.32], [-0.14, -0.32], [0.14, -0.32]]) { const l = limb(0.68, 0.03, 0.06, bm); l.position.set(sx, 0.7, sz); g.add(l); legs.push(l); }
  } else if (type === 'boar') {
    const bm = lam(0x2e2622), sn = lam(0x5a4a42);
    body = mesh(new THREE.CapsuleGeometry(0.4, 0.8, 4, 10), bm, 0, 0.62, 0, g); body.rotation.x = Math.PI / 2; body.scale.set(1, 1, 1.05);
    mesh(new THREE.SphereGeometry(0.42, 10, 8), bm, 0, 0.72, 0.35, g).scale.set(1, 1.05, 0.9); // hump
    const head = mesh(new THREE.ConeGeometry(0.28, 0.6, 8), bm, 0, 0.6, 0.85, g); head.rotation.x = Math.PI / 2 + 0.15;
    mesh(new THREE.CylinderGeometry(0.1, 0.1, 0.08, 8), sn, 0, 0.54, 1.14, g).rotation.x = Math.PI / 2;
    for (const sx of [-0.1, 0.1]) { const t = mesh(new THREE.ConeGeometry(0.025, 0.16, 4), lam(0xf0e8d0), sx, 0.56, 1.05, g); t.rotation.x = -0.7; mesh(new THREE.ConeGeometry(0.06, 0.14, 4), bm, sx * 1.8, 0.86, 0.75, g); }
    // mane bristles
    for (let i = 0; i < 8; i++) mesh(new THREE.ConeGeometry(0.05, 0.18, 3), lam(0x161210), 0, 1.0 - i * 0.02, 0.5 - i * 0.12, g);
    for (const [sx, sz] of [[-0.2, 0.4], [0.2, 0.4], [-0.2, -0.4], [0.2, -0.4]]) { const l = limb(0.4, 0.06, 0.09, bm); l.position.set(sx, 0.4, sz); g.add(l); legs.push(l); }
  }
  return { g, legs, body };
}
class Animal {
  constructor(type, x, z) {
    this.type = type; this.cfg = ANIMALS[type]; const m = buildAnimal(type); this.g = m.g; this.legs = m.legs; this.hp = this.cfg.hp;
    this.g.position.set(x, groundY(x, z), z); this.g.rotation.y = Math.random() * TAU; scene.add(this.g);
    this.state = 'wander'; this.t = rr(1, 4); this.dir = Math.random() * TAU; this.speed = 0; this.phase = 0; this.dead = false; this.harvested = false; this.atkCd = 0; this.charging = false;
  }
  center() { return _v.set(this.g.position.x, this.g.position.y + this.cfg.cy, this.g.position.z); }
  hit(dmg, from) {
    if (this.dead) return; this.hp -= dmg; spawnParts(this.center().clone(), 8, 0x880000, 2.5, 0.5, 0.18, 6, false); AU.hitFlesh();
    if (this.type === 'boar') { AU.squeal(); this.charging = true; this.state = 'charge'; }
    else { this.state = 'flee'; this.t = rr(4, 7); }
    if (this.hp <= 0) { this.dead = true; this.g.rotation.z = Math.PI / 2; this.g.position.y += 0.05; if (this.type === 'chicken') AU.cluck(); }
  }
  update(dt, P, G) {
    if (this.dead) return;
    const pos = this.g.position, dx = P.x - pos.x, dz = P.z - pos.z, d = Math.hypot(dx, dz);
    this.atkCd -= dt; this.t -= dt;
    if (this.state === 'wander') {
      if (this.t < 0) { this.t = rr(2, 6); this.dir = Math.random() * TAU; this.speed = Math.random() < 0.35 ? 0 : this.cfg.speed * 0.18; }
      if (this.cfg.flee && d < this.cfg.flee * (G.crouch ? 0.6 : 1) * (G.running ? 1.5 : 1)) { this.state = 'flee'; this.t = rr(4, 7); if (this.type === 'chicken') AU.cluck(); }
      if (this.type === 'boar' && d < 9) { this.state = 'charge'; }
    } else if (this.state === 'flee') {
      this.dir = Math.atan2(-dx, -dz) + Math.sin(this.t * 3) * 0.4; this.speed = this.cfg.speed; if (this.t < 0 && d > this.cfg.flee * 1.5) { this.state = 'wander'; }
    } else if (this.state === 'charge') {
      this.dir = Math.atan2(dx, dz); this.speed = this.cfg.speed;
      if (d < 1.4 && this.atkCd <= 0) { this.atkCd = 1.6; G.hurtPlayer(15, 'หมูป่าขวิด!'); this.state = 'retreat'; this.t = 1.5; }
      if (d > 40) { this.state = 'wander'; }
    } else if (this.state === 'retreat') {
      this.dir = Math.atan2(-dx, -dz); this.speed = this.cfg.speed * 0.6; if (this.t < 0) this.state = 'charge';
    }
    // move
    let nx = pos.x + Math.sin(this.dir) * this.speed * dt, nz = pos.z + Math.cos(this.dir) * this.speed * dt;
    for (const c of nearColliders(nx, nz)) { const ex = nx - c.x, ez = nz - c.z, ed = Math.hypot(ex, ez), mr = c.r + this.cfg.r * 0.6; if (ed < mr && ed > 0.001) { nx = c.x + ex / ed * mr; nz = c.z + ez / ed * mr; this.dir += 0.5; } }
    const R = Math.hypot(nx, nz); if (R > WORLD_R - 10) { nx *= (WORLD_R - 10) / R; nz *= (WORLD_R - 10) / R; this.dir += Math.PI; }
    if (Math.hypot(nx - POND.x, nz - POND.z) < POND.r - 1) { this.dir += Math.PI * 0.5; nx = pos.x; nz = pos.z; }
    pos.x = nx; pos.z = nz; pos.y = groundY(nx, nz);
    let ry = this.g.rotation.y, tr = this.dir; let df = ((tr - ry + Math.PI) % TAU + TAU) % TAU - Math.PI; this.g.rotation.y = ry + df * Math.min(1, dt * 8);
    this.phase += dt * this.speed * (this.type === 'rabbit' ? 2.5 : 3);
    this.legs.forEach((l, i) => l.rotation.x = Math.sin(this.phase + (i % 2 ? Math.PI : 0) + (i > 1 ? Math.PI / 2 : 0)) * (this.speed > 0.1 ? 0.6 : 0));
    if (this.type === 'rabbit' && this.speed > 1) this.g.position.y += Math.abs(Math.sin(this.phase)) * 0.25;
  }
  remove() { scene.remove(this.g); }
}

// ===== ghosts =====
const GHOSTS = {
  krasue: { name: 'กระสือ', hp: 22, dmg: 8, san: 4, r: 0.55, cy: 0, day: 1, w: 5 },
  pop: { name: 'ผีปอบ', hp: 45, dmg: 7, san: 2, r: 0.55, cy: 1.0, day: 2, w: 4 },
  khamot: { name: 'ผีโขมด', hp: 10, dmg: 0, san: 0, r: 0.5, cy: 0, day: 3, w: 3 },
  saming: { name: 'เสือสมิง', hp: 75, dmg: 14, san: 3, r: 0.8, cy: 0.7, day: 4, w: 3 },
  phrai: { name: 'ผีพราย', hp: 35, dmg: 2, san: 5, r: 0.55, cy: 1.0, day: 6, w: 3 },
  krahang: { name: 'ผีกระหัง', hp: 42, dmg: 12, san: 3, r: 0.85, cy: 0.9, day: 8, w: 3 },
  pret: { name: 'เปรต', hp: 190, dmg: 20, san: 6, r: 1.0, cy: 0, day: 14, w: 1.2 },
  takhian: { name: 'นางตะเคียน', hp: 420, dmg: 12, san: 8, r: 0.7, cy: 1.0, day: 999, w: 0, boss: true },
};
function hairCurtain(parent, r, len, y, open) {
  const g = new THREE.CylinderGeometry(r, r * 1.35, len, 14, 3, true, open ? 0.6 : 0, open ? TAU - 1.2 : TAU); g.translate(0, -len / 2, 0);
  const m = new THREE.Mesh(g, new THREE.MeshLambertMaterial({ map: TEX.hair, alphaTest: 0.35, side: THREE.DoubleSide, color: 0x555555 })); m.position.y = y; parent.add(m); return m;
}
function faceMesh(kind, parent, w, h, y, z) { const m = new THREE.Mesh(new THREE.PlaneGeometry(w, h), new THREE.MeshLambertMaterial({ map: faceTex(kind), transparent: true, emissive: 0x222222, emissiveMap: faceTex(kind) })); m.position.set(0, y, z); parent.add(m); return m; }
function buildGhost(type) {
  const g = new THREE.Group(); const P = { anim: [] };
  if (type === 'krasue') {
    const head = new THREE.Group(); g.add(head); P.head = head;
    mesh(new THREE.SphereGeometry(0.24, 14, 12), lam(0xc8b4a0), 0, 0, 0, head).scale.set(0.95, 1.15, 1);
    faceMesh('krasue', head, 0.42, 0.52, 0, 0.2);
    const hair = hairCurtain(head, 0.25, 0.9, 0.18, true);
    mesh(new THREE.SphereGeometry(0.26, 12, 8, 0, TAU, 0, Math.PI / 2), lam(0x0a0808), 0, 0.04, -0.02, head);
    // entrails: glowing chains
    P.chains = [];
    const em = new THREE.MeshBasicMaterial({ color: 0xc02818 }), em2 = new THREE.MeshBasicMaterial({ color: 0x80ff60 });
    for (let c = 0; c < 5; c++) {
      let par = new THREE.Group(); par.position.set(rr(-0.08, 0.08), -0.3, rr(-0.08, 0.06)); head.add(par); const chain = [];
      const n = c === 0 ? 3 : 7;
      for (let i = 0; i < n; i++) { const seg = new THREE.Group(); seg.position.y = i ? -0.16 : 0; par.add(seg); const s = mesh(new THREE.SphereGeometry(c === 0 ? 0.12 : 0.045 + Math.random() * 0.02, 6, 5), c === 0 ? em : (i % 3 === 0 ? em2 : em), 0, -0.08, 0, seg); s.scale.y = 1.8; chain.push(seg); par = seg; }
      P.chains.push(chain);
    }
    glowSprite(0x60ff50, 1.4, 0.35, head).position.y = -0.6; glowSprite(0xff3020, 0.9, 0.4, head).position.y = -0.4;
    P.light = new THREE.PointLight(0x70ff60, 3, 7, 2); P.light.position.y = -0.5; head.add(P.light);
  } else if (type === 'pop') {
    const sk = lam(0x5a5648), cl = lam(0x2a2018);
    const torso = mesh(new THREE.CapsuleGeometry(0.2, 0.55, 4, 8), sk, 0, 1.15, 0, g); torso.rotation.x = 0.45; P.torso = torso;
    mesh(new THREE.CylinderGeometry(0.26, 0.32, 0.4, 8), cl, 0, 0.82, -0.05, g);
    const head = new THREE.Group(); head.position.set(0, 1.55, 0.25); g.add(head); P.head = head;
    mesh(new THREE.SphereGeometry(0.17, 10, 8), sk, 0, 0, 0, head); faceMesh('pop', head, 0.3, 0.36, 0, 0.14); hairCurtain(head, 0.17, 0.5, 0.12, true);
    P.arms = []; for (const sx of [-0.28, 0.28]) { const a = limb(0.85, 0.04, 0.07, sk); a.position.set(sx, 1.4, 0.15); a.rotation.x = -0.6; g.add(a); P.arms.push(a); mesh(new THREE.ConeGeometry(0.06, 0.15, 4), lam(0x201810), 0, -0.9, 0, a); }
    P.legs = []; for (const sx of [-0.12, 0.12]) { const l = limb(0.8, 0.05, 0.08, sk); l.position.set(sx, 0.8, 0); g.add(l); P.legs.push(l); }
    eyes(head, 0, 0.02, 0.16, 0.065, 0.018, 0xff2010);
  } else if (type === 'saming') {
    const fur = new THREE.MeshLambertMaterial({ map: TEX.tiger });
    const body = mesh(new THREE.CapsuleGeometry(0.38, 1.1, 4, 10), fur, 0, 0.85, 0, g); body.rotation.x = Math.PI / 2; P.body = body;
    const head = new THREE.Group(); head.position.set(0, 1.05, 0.95); g.add(head); P.head = head;
    mesh(new THREE.SphereGeometry(0.3, 12, 10), fur, 0, 0, 0, head).scale.set(1.1, 0.95, 1);
    mesh(new THREE.BoxGeometry(0.28, 0.18, 0.25), fur, 0, -0.08, 0.22, head);
    mesh(new THREE.SphereGeometry(0.04, 6, 5), lam(0x201010), 0, -0.02, 0.36, head);
    for (const sx of [-0.18, 0.18]) mesh(new THREE.ConeGeometry(0.08, 0.14, 4), fur, sx, 0.25, -0.02, head);
    for (const sx of [-0.06, 0.06]) mesh(new THREE.ConeGeometry(0.018, 0.12, 4), lam(0xf0f0e0), sx, -0.22, 0.3, head).rotation.x = Math.PI;
    eyes(head, 0, 0.07, 0.25, 0.11, 0.035, 0xffd000);
    P.legs = []; for (const [sx, sz] of [[-0.22, 0.6], [0.22, 0.6], [-0.22, -0.55], [0.22, -0.55]]) { const l = limb(0.75, 0.07, 0.12, fur); l.position.set(sx, 0.75, sz); g.add(l); P.legs.push(l); }
    const tail = limb(1.0, 0.03, 0.06, fur); tail.position.set(0, 0.95, -0.95); tail.rotation.x = -2.2; g.add(tail); P.tail = tail;
    glowSprite(0xffa000, 2.5, 0.12, g).position.set(0, 0.9, 0.3);
  } else if (type === 'khamot') {
    P.orbs = []; for (let i = 0; i < 3; i++) { const o = new THREE.Group(); g.add(o); mesh(new THREE.SphereGeometry(0.12, 8, 6), basic(0xfff0a0, { fog: false }), 0, 0, 0, o); glowSprite(0xffb030, 1.6, 0.7, o); glowSprite(0xff6010, 3.2, 0.25, o); P.orbs.push(o); }
    P.light = new THREE.PointLight(0xffa040, 4, 10, 2); g.add(P.light);
  } else if (type === 'phrai') {
    P.mats = [];
    const dm = new THREE.MeshLambertMaterial({ color: 0xe8ecf0, transparent: true, opacity: 0.1, side: THREE.DoubleSide, emissive: 0x404858, depthWrite: false }); P.mats.push(dm);
    const dress = mesh(new THREE.ConeGeometry(0.5, 1.7, 12, 3, true), dm, 0, 0.85, 0, g);
    const sm = new THREE.MeshLambertMaterial({ color: 0xf0f0f2, transparent: true, opacity: 0.1, emissive: 0x303038 }); P.mats.push(sm);
    const head = new THREE.Group(); head.position.y = 1.85; g.add(head); P.head = head;
    mesh(new THREE.SphereGeometry(0.15, 10, 8), sm, 0, 0, 0, head);
    const fm = faceMesh('phrai', head, 0.28, 0.34, 0, 0.13); fm.material.transparent = true; fm.material.opacity = 0.1; P.mats.push(fm.material);
    const hair = hairCurtain(head, 0.17, 1.4, 0.15, true); hair.material.transparent = true; hair.material.opacity = 0.1; P.mats.push(hair.material);
    P.arms = []; for (const sx of [-0.3, 0.3]) { const a = limb(0.75, 0.03, 0.05, sm); a.position.set(sx, 1.6, 0.05); a.rotation.x = -1.3; g.add(a); P.arms.push(a); }
    P.glow = glowSprite(0xc0d0ff, 3, 0.05, g); P.glow.position.y = 1.1;
  } else if (type === 'krahang') {
    const sk = lam(0x5a3a28), cl = lam(0x7a1a10);
    mesh(new THREE.CapsuleGeometry(0.22, 0.6, 4, 8), sk, 0, 1.0, 0, g);
    mesh(new THREE.CylinderGeometry(0.24, 0.28, 0.25, 8), cl, 0, 0.62, 0, g);
    const head = new THREE.Group(); head.position.y = 1.6; g.add(head); P.head = head;
    mesh(new THREE.SphereGeometry(0.17, 10, 8), sk, 0, 0, 0, head); faceMesh('krahang', head, 0.3, 0.34, 0, 0.14);
    mesh(new THREE.SphereGeometry(0.18, 10, 6, 0, TAU, 0, Math.PI / 2.2), lam(0x100808), 0, 0.03, 0, head);
    eyes(head, 0, 0.03, 0.16, 0.065, 0.02, 0xff3010);
    P.wings = []; const wm = new THREE.MeshLambertMaterial({ map: TEX.woven, side: THREE.DoubleSide });
    for (const sd of [-1, 1]) { const piv = new THREE.Group(); piv.position.set(sd * 0.25, 1.25, 0); g.add(piv); const w = mesh(new THREE.CylinderGeometry(0.75, 0.75, 0.05, 20), wm, sd * 0.8, 0, 0, piv); w.rotation.z = Math.PI / 2; const rim = mesh(new THREE.TorusGeometry(0.75, 0.04, 4, 20), lam(0x5a4010), sd * 0.8, 0, 0, piv); rim.rotation.y = Math.PI / 2; P.wings.push({ piv, sd }); }
    const pestle = mesh(new THREE.CylinderGeometry(0.07, 0.09, 1.5, 8), lam(0x4a3220), 0, 0.3, -0.35, g); pestle.rotation.x = 1.0; P.pestle = pestle;
    for (const sx of [-0.1, 0.1]) { const l = limb(0.6, 0.05, 0.07, sk); l.position.set(sx, 0.55, 0); l.rotation.x = 0.4; g.add(l); }
    glowSprite(0xff4020, 2.5, 0.15, g).position.y = 1.1;
  } else if (type === 'pret') {
    const sk = lam(0x2e2622);
    const H = 9;
    const legs = []; for (const sx of [-0.25, 0.25]) { const l = limb(4.2, 0.07, 0.1, sk); l.position.set(sx, 4.2, 0); g.add(l); legs.push(l); } P.legs = legs;
    const torso = mesh(new THREE.CapsuleGeometry(0.28, 1.6, 4, 8), sk, 0, 5.3, 0, g);
    for (let i = 0; i < 6; i++) { const rb = mesh(new THREE.TorusGeometry(0.29, 0.02, 3, 10, Math.PI), lam(0x4a4038), 0, 5.0 + i * 0.18, 0.02, g); rb.rotation.y = -Math.PI / 2; rb.rotation.z = Math.PI / 2; }
    const neck = mesh(new THREE.CylinderGeometry(0.07, 0.1, 2.2, 6), sk, 0, 7.3, 0, g);
    const head = new THREE.Group(); head.position.y = 8.6; g.add(head); P.head = head;
    mesh(new THREE.SphereGeometry(0.32, 10, 8), sk, 0, 0, 0, head).scale.set(0.9, 1.15, 1);
    faceMesh('pret', head, 0.52, 0.62, 0, 0.27); hairCurtain(head, 0.3, 1.2, 0.2, true);
    eyes(head, 0, 0.06, 0.29, 0.1, 0.03, 0xff3020);
    P.arms = []; for (const sx of [-0.4, 0.4]) { const a = limb(3.6, 0.05, 0.08, sk); a.position.set(sx, 6.1, 0); a.rotation.z = sx > 0 ? 0.15 : -0.15; g.add(a); P.arms.push(a); }
    glowSprite(0x800000, 4, 0.15, head);
  } else if (type === 'takhian') {
    const sk = lam(0xe0d0b8, { emissive: 0x203018 });
    const dm = lam(0x1f6a3a, { emissive: 0x0a2a14, side: THREE.DoubleSide }), gold = new THREE.MeshPhongMaterial({ color: 0xd4a020, emissive: 0x403000, shininess: 80 });
    const skirt = mesh(new THREE.ConeGeometry(0.55, 1.5, 14, 2, true), dm, 0, 0.75, 0, g);
    mesh(new THREE.CylinderGeometry(0.17, 0.22, 0.6, 10), dm, 0, 1.7, 0, g);
    const sabai = mesh(new THREE.CylinderGeometry(0.19, 0.23, 0.32, 10), lam(0xa01848, { emissive: 0x200008 }), 0, 1.85, 0, g); sabai.rotation.z = 0.35;
    mesh(new THREE.TorusGeometry(0.2, 0.03, 5, 14), gold, 0, 1.45, 0, g).rotation.x = Math.PI / 2;
    const head = new THREE.Group(); head.position.y = 2.2; g.add(head); P.head = head;
    mesh(new THREE.SphereGeometry(0.15, 10, 8), sk, 0, 0, 0, head); faceMesh('takhian', head, 0.28, 0.34, 0, 0.13);
    hairCurtain(head, 0.17, 1.6, 0.15, true);
    mesh(new THREE.ConeGeometry(0.1, 0.35, 8), gold, 0, 0.3, -0.02, head);
    P.arms = []; for (const sx of [-0.25, 0.25]) { const a = limb(0.75, 0.035, 0.05, sk); a.position.set(sx, 2.0, 0); a.rotation.x = -1.0; g.add(a); P.arms.push(a); }
    P.aura = glowSprite(0x40ff80, 5, 0.25, g); P.aura.position.y = 1.4;
    P.light = new THREE.PointLight(0x50ff80, 8, 14, 2); P.light.position.y = 1.5; g.add(P.light);
  }
  return { g, P };
}

class Ghost {
  constructor(type, angle, dist, day) {
    this.type = type; this.cfg = GHOSTS[type]; const b = buildGhost(type); this.g = b.g; this.P = b.P;
    const sc = 1 + day * 0.013; this.maxHp = this.hp = this.cfg.hp * sc; this.dmgMul = 1 + day * 0.008;
    const x = Math.cos(angle) * dist, z = Math.sin(angle) * dist; this.g.position.set(x, groundY(x, z), z);
    this.fly = type === 'krasue' || type === 'khamot' || type === 'krahang';
    if (type === 'krasue') this.g.position.y += rr(3, 8); if (type === 'khamot') this.g.position.y += rr(3, 9); if (type === 'krahang') this.g.position.y += rr(10, 16);
    if (type === 'takhian') this.g.position.y += STAND_Y * 0.5;
    scene.add(this.g); this.state = 'approach'; this.t = 0; this.cd = rr(0.5, 1.5); this.phase = Math.random() * 10; this.dead = false; this.dying = 0; this.stun = 0;
    this.orbitA = angle; this.orbitR = dist; this.lit = 0; this.vel = new THREE.Vector3(); this.climbY = 0; this.scr = rr(3, 8); this.sumT = 10; this.tpT = 4;
    if (type === 'saming') AU.scream('saming', 0, 0.35);
  }
  hitCenter(out) { const p = this.g.position; out.set(p.x, p.y + this.cfg.cy, p.z); return out; }
  testPoint(pt) { // returns true if pt inside hit volume
    const p = this.g.position;
    if (this.type === 'pret') { const dx = pt.x - p.x, dz = pt.z - p.z; return Math.hypot(dx, dz) < 0.75 && pt.y > p.y + 3.5 && pt.y < p.y + 9.2; }
    if (this.type === 'khamot') { for (const o of this.P.orbs) { const w = o.getWorldPosition(_v); if (w.distanceTo(pt) < 0.55) return true; } return false; }
    if (this.type === 'saming') { const c = _v.set(p.x, p.y + 0.85, p.z); return c.distanceTo(pt) < 1.0; }
    const yb = this.type === 'krasue' ? -0.6 : 0, yt = this.type === 'krasue' ? 0.3 : (this.type === 'takhian' ? 2.4 : 1.9);
    const dx = pt.x - p.x, dz = pt.z - p.z; return Math.hypot(dx, dz) < this.cfg.r + 0.1 && pt.y > p.y + yb && pt.y < p.y + yt;
  }
  canBeHit() { return !(this.type === 'phrai' && this.lit < 0.35); }
  hit(dmg, kind, G) {
    if (this.dead) return false;
    if (kind === 'arrow' && !this.canBeHit()) return false;
    let m = 1; if (kind === 'rice' && (this.type === 'phrai' || this.type === 'krasue' || this.type === 'khamot' || this.type === 'pop')) m = 1.8;
    if (kind === 'arrow' && this.type === 'pret') m = 0.8;
    this.hp -= dmg * m; const c = this.hitCenter(new THREE.Vector3());
    spawnParts(this.type === 'pret' ? this.g.position.clone().add(new THREE.Vector3(0, 7, 0)) : c, 6, this.type === 'saming' || this.type === 'pop' ? 0x700000 : 0x90ff90, 2, 0.4, 0.2, 2, this.type !== 'saming');
    if (this.type === 'phrai' || this.type === 'khamot') { } else if (Math.random() < 0.5) AU.scream(this.type, 0, 0.12);
    if (this.hp <= 0) { this.die(G); }
    else if (kind === 'arrow' && (this.type === 'krasue' || this.type === 'krahang')) { this.state = 'retreat'; this.t = 1.2; }
    return true;
  }
  die(G) { this.dead = true; this.dying = 1; AU.ghostDie(); const c = this.hitCenter(new THREE.Vector3()); if (this.type === 'pret') c.y += 6; spawnParts(c, 30, this.type === 'takhian' ? 0x60ff90 : 0xc0ffd0, 4, 1.2, 0.35, -1); G.onGhostKilled(this); }
  remove() { scene.remove(this.g); }
  update(dt, G) {
    const p = this.g.position, PL = G.ppos; this.phase += dt;
    if (this.dying > 0) { this.dying -= dt * 1.2; this.g.scale.setScalar(Math.max(0.01, this.dying)); p.y += dt * 2; this.g.traverse(o => { if (o.material) { o.material.transparent = true; o.material.opacity = Math.max(0, this.dying); } }); return this.dying > 0; }
    if (this.stun > 0) { this.stun -= dt; this.animate(dt, 0.2); return true; }
    const dx = PL.x - p.x, dz = PL.z - p.z, dh = Math.hypot(dx, dz), dy = PL.y - p.y; this.cd -= dt; this.t -= dt;
    const face = () => { this.g.rotation.y = Math.atan2(dx, dz); };
    const T = this.type;
    if (T === 'krasue') {
      let tx = PL.x, ty = PL.y + 0.2, tz = PL.z;
      if (this.state === 'retreat') { const a = Math.atan2(p.z, p.x); tx = Math.cos(a) * 14; tz = Math.sin(a) * 14; ty = STAND_Y + 6; if (this.t < 0) this.state = 'approach'; }
      _v.set(tx - p.x, ty - p.y, tz - p.z); const d = _v.length(); _v.normalize();
      const side = new THREE.Vector3(-_v.z, 0, _v.x).multiplyScalar(Math.sin(this.phase * 2.3) * 2.5);
      this.vel.lerp(_v.multiplyScalar(4.8 + G.day * 0.03).add(side), Math.min(1, dt * 2)); p.addScaledVector(this.vel, dt);
      p.y += Math.sin(this.phase * 3) * dt * 0.8;
      this.g.rotation.y = Math.atan2(dx, dz);
      if (this.state !== 'retreat' && _v.set(PL.x - p.x, PL.y - p.y, PL.z - p.z).length() < 1.5) { G.hurtPlayer(this.cfg.dmg * this.dmgMul, 'กระสือโจมตี!'); G.sanity -= this.cfg.san; AU.scream('krasue', 0, 0.3); this.state = 'retreat'; this.t = 2.5; }
      this.scr -= dt; if (this.scr < 0) { this.scr = rr(6, 12); AU.scream('krasue', clamp(-dx / 20, -1, 1), 0.12); }
    } else if (T === 'pop' || T === 'saming') {
      const sp = T === 'saming' ? 7.5 : 2.3, climb = T === 'saming' ? 2.3 : 1.0;
      if (this.state === 'approach') {
        const a = Math.atan2(p.z, p.x), tx = Math.cos(a) * 2.1, tz = Math.sin(a) * 2.1; const ex = tx - p.x, ez = tz - p.z, ed = Math.hypot(ex, ez);
        if (ed < 0.3) { this.state = 'climb'; if (T === 'saming') AU.scream('saming', 0, 0.4); else AU.scream('pop', 0, 0.2); }
        else { p.x += ex / ed * sp * dt; p.z += ez / ed * sp * dt; p.y = groundY(p.x, p.z); this.g.rotation.y = Math.atan2(ex, ez); }
        this.animate(dt, sp);
      } else if (this.state === 'climb') {
        p.y += climb * dt; this.g.rotation.y = Math.atan2(-p.x, -p.z); this.g.rotation.x = -0.9;
        if (p.y >= STAND_Y - 0.1) { p.y = STAND_Y; const a = Math.atan2(p.z, p.x); p.x = Math.cos(a) * 2.6; p.z = Math.sin(a) * 2.6; this.state = 'attack'; this.g.rotation.x = 0; this.cd = 0.6; }
        this.animate(dt, climb * 2);
      } else if (this.state === 'attack') {
        face(); this.g.rotation.x = 0;
        if (this.cd <= 0) { this.cd = T === 'saming' ? 1.1 : 1.6; this.lunge = 0.3; G.hurtPlayer(this.cfg.dmg * this.dmgMul, T === 'saming' ? 'เสือสมิงตะปบ!' : 'ผีปอบข่วน!'); G.damageStand(T === 'pop' ? 6 : 3); G.sanity -= this.cfg.san; if (T === 'saming' && Math.random() < 0.4) AU.scream('saming', 0, 0.3); }
        this.animate(dt, 3);
      }
    } else if (T === 'khamot') {
      this.orbitA += dt * 0.12; this.orbitR = Math.max(6, this.orbitR - dt * 0.6);
      p.x = lerp(p.x, Math.cos(this.orbitA) * this.orbitR, dt); p.z = lerp(p.z, Math.sin(this.orbitA) * this.orbitR, dt); p.y = lerp(p.y, STAND_Y + 1 + Math.sin(this.phase * 0.7) * 3, dt);
      if (G.lookDot(p) > 0.97) { G.sanity -= dt * 3.5; G.lure = Math.max(G.lure, 0.8); }
      if (this.orbitR <= 6.1) { this.t -= 0; if (Math.random() < dt * 0.15) { G.sanity -= 10; G.toast('ผีโขมดเผาสติ!'); this.orbitR = 25; } }
    } else if (T === 'phrai') {
      // lit?
      const inLight = G.inFlashlight(_v.set(p.x, p.y + 1.1, p.z));
      this.lit = lerp(this.lit, inLight ? 1 : 0, Math.min(1, dt * 5));
      const o = 0.04 + this.lit * 0.75; this.P.mats.forEach(m => m.opacity = o); this.P.glow.material.opacity = 0.05 + this.lit * 0.3;
      const tr = 3.2, a = Math.atan2(p.z, p.x); const tx = Math.cos(a) * tr, tz = Math.sin(a) * tr; const ex = tx - p.x, ez = tz - p.z, ed = Math.hypot(ex, ez);
      const sp = inLight ? 0.6 : 2.2;
      if (ed > 0.2) { p.x += ex / ed * sp * dt; p.z += ez / ed * sp * dt; }
      const gy = groundY(p.x, p.z); const targetY = ed < 12 ? STAND_Y - 0.5 : gy + 0.2; p.y = lerp(p.y, targetY + Math.sin(this.phase) * 0.2, dt * 0.6);
      face();
      const d3 = _v.set(PL.x - p.x, PL.y - p.y - 1.5, PL.z - p.z).length();
      if (d3 < 4.5) { G.sanity -= this.cfg.san * dt; G.hurtPlayer(this.cfg.dmg * dt * this.dmgMul, null, true); }
      this.tpT -= dt; if (this.tpT < 0 && !inLight) { this.tpT = rr(6, 11); const na = a + rr(-1.6, 1.6), nr = Math.max(8, Math.hypot(p.x, p.z) - rr(0, 4)); p.set(Math.cos(na) * nr, groundY(Math.cos(na) * nr, Math.sin(na) * nr) + 0.2, Math.sin(na) * nr); AU.scream('phrai', clamp(Math.cos(na), -1, 1), 0.15); }
      this.P.arms.forEach((ar, i) => ar.rotation.x = -1.3 + Math.sin(this.phase * 2 + i) * 0.15);
    } else if (T === 'krahang') {
      if (this.state === 'approach' || this.state === 'retreat') {
        this.orbitA += dt * 0.35; this.orbitR = lerp(this.orbitR, 16, dt * 0.3); const tx = Math.cos(this.orbitA) * this.orbitR, tz = Math.sin(this.orbitA) * this.orbitR, ty = STAND_Y + 7 + Math.sin(this.phase) * 2;
        p.x = lerp(p.x, tx, dt * 1.5); p.z = lerp(p.z, tz, dt * 1.5); p.y = lerp(p.y, ty, dt); this.g.rotation.y = this.orbitA + Math.PI;
        if (this.t < 0 && this.orbitR < 20) { this.state = 'swoop'; AU.scream('krahang', 0, 0.25); }
      } else if (this.state === 'swoop') {
        _v.set(PL.x - p.x, PL.y - 1 - p.y, PL.z - p.z); const d = _v.length(); _v.normalize(); p.addScaledVector(_v, dt * 11); face();
        if (d < 1.8) { G.hurtPlayer(this.cfg.dmg * this.dmgMul, 'ผีกระหังโฉบ!'); G.damageStand(2); this.state = 'retreat'; this.t = rr(5, 8); this.orbitA = Math.atan2(p.z, p.x); this.orbitR = 4; }
      }
      if (this.state === 'retreat' && this.t > 0) { } else if (this.state === 'retreat') this.state = 'approach';
      this.P.wings.forEach(w => w.piv.rotation.z = w.sd * Math.sin(this.phase * 9) * 0.6);
    } else if (T === 'pret') {
      if (dh > 4.2) { p.x += dx / dh * 1.1 * dt; p.z += dz / dh * 1.1 * dt; p.y = groundY(p.x, p.z) - 0.2; this.animate(dt, 1.1); }
      else if (this.cd <= 0) { this.cd = 2.3; G.hurtPlayer(this.cfg.dmg * this.dmgMul, 'เปรตฟาด!'); G.damageStand(5); this.P.arms.forEach(a => a.rotation.x = -1.4); }
      if (this.cd < 1.6) this.P.arms.forEach(a => a.rotation.x = lerp(a.rotation.x, Math.sin(this.phase) * 0.1, dt * 3));
      face(); this.scr -= dt; if (this.scr < 0) { this.scr = rr(9, 14); AU.scream('pret', clamp(-dx / dh, -1, 1), 0.4); if (dh < 45) { G.sanity -= this.cfg.san; G.toast('เสียงเปรตร้องโหยหวน... สติลดลง'); } }
    } else if (T === 'takhian') {
      p.y = lerp(p.y, STAND_Y + 0.3 + Math.sin(this.phase * 1.4) * 0.4, dt * 2); face();
      this.tpT -= dt;
      if (this.tpT < 0) { this.tpT = rr(3.5, 5.5); const cur = Math.hypot(p.x, p.z), nr = Math.max(5, cur - rr(3, 8)), na = Math.atan2(p.z, p.x) + rr(-1.2, 1.2); spawnParts(this.hitCenter(new THREE.Vector3()), 14, 0x50ff80, 3, 0.6, 0.3); p.x = Math.cos(na) * nr; p.z = Math.sin(na) * nr; }
      if (dh < 5.5 && this.cd <= 0) { this.cd = 1.5; G.hurtPlayer(this.cfg.dmg * this.dmgMul, 'นางตะเคียนบีบคอ!'); G.sanity -= 4; if (Math.random() < 0.35) { const na = Math.random() * TAU; p.x = Math.cos(na) * 22; p.z = Math.sin(na) * 22; } }
      this.scr -= dt; if (this.scr < 0) { this.scr = rr(8, 11); AU.scream('takhian', 0, 0.45); G.sanity -= this.cfg.san; G.toast('นางตะเคียนกรีดร้อง!'); G.shake = 0.5; }
      this.sumT -= dt; if (this.sumT < 0) { this.sumT = 14; const a = Math.atan2(p.z, p.x); G.spawnGhost('krasue', a + 0.3, Math.hypot(p.x, p.z) + 2); G.spawnGhost('krasue', a - 0.3, Math.hypot(p.x, p.z) + 2); }
      this.P.aura.material.opacity = 0.2 + Math.sin(this.phase * 4) * 0.08; this.P.arms.forEach((ar, i) => ar.rotation.x = -1.0 + Math.sin(this.phase * 2 + i) * 0.2);
    }
    if (T === 'krasue') this.animate(dt, 0);
    return true;
  }
  animate(dt, sp) {
    const P = this.P, t = this.phase;
    if (P.chains) P.chains.forEach((ch, ci) => ch.forEach((seg, i) => { seg.rotation.x = Math.sin(t * 3 + i * 0.6 + ci) * 0.09 + Math.min(0.12, this.vel.length() * 0.015); seg.rotation.z = Math.cos(t * 2.4 + i * 0.5 + ci * 2) * 0.07; }));
    if (P.legs) P.legs.forEach((l, i) => l.rotation.x = Math.sin(t * sp * 1.6 + (i % 2 ? Math.PI : 0) + (i > 1 ? Math.PI / 2 : 0)) * (sp > 0.3 ? 0.6 : 0.05));
    if (P.arms && this.type === 'pop') P.arms.forEach((a, i) => a.rotation.x = -0.6 + Math.sin(t * sp * 1.6 + (i ? 0 : Math.PI)) * 0.5 - (this.lunge > 0 ? 1 : 0));
    if (this.lunge > 0) this.lunge -= dt;
    if (P.tail) P.tail.rotation.z = Math.sin(t * 3) * 0.3;
    if (P.head && this.type === 'pop') P.head.rotation.z = Math.sin(t * 1.3) * 0.25;
    if (this.type === 'khamot') P.orbs.forEach((o, i) => { o.position.set(Math.sin(t * 0.9 + i * 2.1) * 1.2, Math.sin(t * 1.3 + i) * 0.6, Math.cos(t * 0.7 + i * 2.1) * 1.2); o.scale.setScalar(0.8 + Math.sin(t * 7 + i * 3) * 0.25); });
  }
}
// hook khamot animate into update
const _gup = Ghost.prototype.update; Ghost.prototype.update = function (dt, G) { const r = _gup.call(this, dt, G); if (this.type === 'khamot' && !this.dead) this.animate(dt, 0); return r; };

// ===== pickups =====
const PICK = {
  wood: { name: 'กิ่งไม้', icon: '🪵' }, fruit: { name: 'ผลไม้ป่า', icon: '🍈' }, herb: { name: 'สมุนไพร', icon: '🌿' }, mush: { name: 'เห็ดป่า', icon: '🍄' }, bag: { name: 'ย่ามนายพรานเก่า', icon: '🎒' }, honey: { name: 'รังผึ้ง', icon: '🍯' },
};
function buildPickup(type) {
  const g = new THREE.Group();
  if (type === 'wood') { const m = lam(0x6a4a2a); for (let i = 0; i < 4; i++) { const s = mesh(new THREE.CylinderGeometry(0.04, 0.05, 1.1, 5), m, rr(-0.15, 0.15), 0.06 + i * 0.03, rr(-0.1, 0.1), g); s.rotation.z = Math.PI / 2; s.rotation.y = rr(-0.4, 0.4); } }
  else if (type === 'fruit') { const m = lam(0xd8a020, { emissive: 0x201000 }); for (let i = 0; i < 4; i++) mesh(new THREE.SphereGeometry(0.1, 8, 6), i % 2 ? m : lam(0x9aba30), rr(-0.15, 0.15), 0.1, rr(-0.15, 0.15), g); }
  else if (type === 'herb') { const m = lam(0x40a040, { emissive: 0x082008, side: THREE.DoubleSide }); for (let i = 0; i < 6; i++) { const l = mesh(new THREE.PlaneGeometry(0.12, 0.4), m, 0, 0.2, 0, g); l.rotation.y = i; l.rotation.x = 0.3; } mesh(new THREE.SphereGeometry(0.04, 6, 4), lam(0xffffff, { emissive: 0x404040 }), 0, 0.42, 0, g); }
  else if (type === 'mush') { for (let i = 0; i < 3; i++) { const x = rr(-0.12, 0.12), z = rr(-0.12, 0.12); mesh(new THREE.CylinderGeometry(0.025, 0.03, 0.12, 5), lam(0xe8e0d0), x, 0.06, z, g); mesh(new THREE.SphereGeometry(0.08, 8, 5, 0, TAU, 0, Math.PI / 2), lam(0x9a5a30), x, 0.11, z, g); } }
  else if (type === 'bag') { mesh(new THREE.BoxGeometry(0.4, 0.3, 0.2), lam(0x4a3a20), 0, 0.15, 0, g).rotation.y = 0.4; mesh(new THREE.TorusGeometry(0.15, 0.02, 4, 10, Math.PI), lam(0x2a1a10), 0, 0.3, 0, g); }
  else if (type === 'honey') { mesh(new THREE.SphereGeometry(0.2, 8, 6), lam(0xb08020, { emissive: 0x302000 }), 0, 0.2, 0, g).scale.set(1, 1.3, 0.8); }
  const sp = glowSprite(type === 'herb' ? 0xa0ffa0 : 0xfff0b0, 0.7, 0.35, g); sp.position.y = 0.3; g.userData.spark = sp;
  return g;
}

// ===== crossbow view model =====
function buildCrossbow() {
  const g = new THREE.Group(); const wm = new THREE.MeshLambertMaterial({ map: TEX.wood, emissive: 0x000000 }), dk = new THREE.MeshLambertMaterial({ color: 0x2a1e14, emissive: 0x000000 });
  const stock = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.07, 0.7), wm); stock.position.z = -0.15; g.add(stock);
  const grip = new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.14, 0.07), wm); grip.position.set(0, -0.08, 0.1); grip.rotation.x = 0.3; g.add(grip);
  const lg = new THREE.TorusGeometry(0.42, 0.015, 5, 16, 1.3); const limbs = new THREE.Mesh(lg, dk); limbs.rotation.set(Math.PI / 2, 0, Math.PI / 2 + Math.PI - 0.65 + Math.PI); limbs.position.set(0, 0.02, -0.08 - 0.42 * 0.0); g.add(limbs);
  // limbs as two angled boxes (clearer)
  g.remove(limbs);
  for (const sd of [-1, 1]) { const l = new THREE.Mesh(new THREE.BoxGeometry(0.36, 0.025, 0.035), dk); l.position.set(sd * 0.18, 0.03, -0.46 + 0.05); l.rotation.y = sd * 0.28; g.add(l); }
  const strG = new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(-0.34, 0.03, -0.31), new THREE.Vector3(0, 0.03, -0.05), new THREE.Vector3(0.34, 0.03, -0.31)]);
  const str = new THREE.Line(strG, new THREE.LineBasicMaterial({ color: 0xd8d0b8 })); g.add(str); g.userData.str = str;
  const arrow = new THREE.Group(); const sh = new THREE.Mesh(new THREE.CylinderGeometry(0.007, 0.007, 0.5, 4), lam(0xc8b080)); sh.rotation.x = Math.PI / 2; arrow.add(sh);
  const tip = new THREE.Mesh(new THREE.ConeGeometry(0.014, 0.05, 4), lam(0x888888)); tip.rotation.x = -Math.PI / 2; tip.position.z = -0.27; arrow.add(tip);
  arrow.position.set(0, 0.055, -0.3); g.add(arrow); g.userData.arrow = arrow;
  // taped flashlight
  const fl = new THREE.Mesh(new THREE.CylinderGeometry(0.022, 0.026, 0.2, 8), new THREE.MeshLambertMaterial({ color: 0x303030 })); fl.rotation.x = Math.PI / 2; fl.position.set(0.05, -0.04, -0.35); g.add(fl);
  const lens = new THREE.Mesh(new THREE.CircleGeometry(0.022, 10), new THREE.MeshBasicMaterial({ color: 0x777766 })); lens.position.set(0.05, -0.04, -0.451); lens.rotation.y = Math.PI; g.add(lens); g.userData.lens = lens;
  g.userData.mats = [wm, dk];
  g.scale.setScalar(0.62); g.position.set(0.17, -0.17, -0.42); g.rotation.set(0.05, -0.05, 0);
  g.traverse(o => { if (o.material) { o.material.fog = false; } o.renderOrder = 5; o.castShadow = false; });
  return g;
}
function arrowMesh() {
  const g = new THREE.Group(); const sh = new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.012, 0.7, 4), lam(0xc8b080)); sh.rotation.x = Math.PI / 2; g.add(sh);
  const f = new THREE.Mesh(new THREE.PlaneGeometry(0.06, 0.12), lam(0xe8e8e8, { side: THREE.DoubleSide })); f.position.z = 0.3; f.rotation.y = Math.PI / 2; g.add(f);
  const f2 = f.clone(); f2.rotation.set(0, Math.PI / 2, Math.PI / 2); g.add(f2);
  return g;
}
