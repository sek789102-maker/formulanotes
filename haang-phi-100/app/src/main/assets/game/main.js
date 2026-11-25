'use strict';
const DAY_LEN = 150, NIGHT_LEN = 110, MAX_DAY = 100;
const BOSS_DAYS = [20, 40, 60, 80, 100];
const $ = id => document.getElementById(id);
const store = { get(k, d) { try { const v = localStorage.getItem(k); return v == null ? d : JSON.parse(v); } catch (e) { return d; } }, set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch (e) { } }, del(k) { try { localStorage.removeItem(k); } catch (e) { } } };
QUALITY = store.get('hp100_q', 1);

const G = {
  phase: 'title', day: 1, t: 0, paused: false, hp: 100, hunger: 100, sanity: 100, stand: 100, battery: 100,
  inv: {}, kills: 0, nightKills: 0, ppos: new THREE.Vector3(), yaw: 0, pitch: 0, running: false, lightOn: false,
  animals: [], ghosts: [], pickups: [], arrows: [], queue: [], boss: null, raining: false, shrineUsed: false,
  reload: 0, riceCd: 0, shake: 0, lure: 0, hurtFlash: 0, bob: 0, stepT: 0, warned: false, time: 0, insaneT: 5,
  crouch: false,
};
const START_INV = () => ({ arrows: 20, wood: 3, meat: 2, fruit: 1, herb: 1, mush: 0, honey: 0, rice: 3, amulet: 1 });
const INV_ICON = { arrows: '🏹', wood: '🪵', meat: '🍖', fruit: '🍈', herb: '🌿', mush: '🍄', honey: '🍯', rice: '🍚', amulet: '📿' };
const INV_NAME = { arrows: 'ลูกดอก', wood: 'ไม้', meat: 'เนื้อย่าง', fruit: 'ผลไม้', herb: 'สมุนไพร', mush: 'เห็ด', honey: 'น้ำผึ้ง', rice: 'ข้าวสารเสก', amulet: 'พระเครื่อง' };
let crossbow, spot, flashFlare;

// ===== init =====
function init() {
  buildTextures(QUALITY);
  buildRenderer($('c'));
  buildWorld();
  crossbow = buildCrossbow(); camera.add(crossbow); crossbow.visible = false;
  spot = new THREE.SpotLight(0xfff2d8, 0, 70, 0.36, 0.55, 0); spot.position.set(0.12, -0.12, -0.3); spot.target.position.set(0.0, -0.1, -12); camera.add(spot); camera.add(spot.target);
  flashFlare = glowSprite(0xfff4d0, 0.25, 0, crossbow); flashFlare.position.set(0.05, -0.04, -0.47);
  window.addEventListener('resize', onResize); onResize();
  setupInput(); setupUI();
  $('loading').classList.add('hide'); showTitle();
  requestAnimationFrame(loop);
}
function onResize() { renderer.setSize(window.innerWidth, window.innerHeight); camera.aspect = window.innerWidth / window.innerHeight; camera.updateProjectionMatrix(); }

// ===== save =====
function saveGame() { store.set('hp100_save', { day: G.day, hp: G.hp, hunger: G.hunger, sanity: G.sanity, stand: G.stand, battery: G.battery, inv: G.inv, kills: G.kills }); }
function loadGame() { const s = store.get('hp100_save', null); if (!s) return false; Object.assign(G, { day: s.day, hp: s.hp, hunger: s.hunger, sanity: s.sanity, stand: s.stand, battery: s.battery, inv: Object.assign(START_INV(), s.inv), kills: s.kills || 0 }); return true; }
function newGame() { Object.assign(G, { day: 1, hp: 100, hunger: 85, sanity: 100, stand: 100, battery: 100, inv: START_INV(), kills: 0 }); store.del('hp100_save'); saveGame(); }

// ===== phases =====
function clearEntities() {
  G.animals.forEach(a => a.remove()); G.animals = []; G.ghosts.forEach(g => g.remove()); G.ghosts = []; G.pickups.forEach(p => scene.remove(p.g)); G.pickups = [];
  G.arrows.forEach(a => scene.remove(a.m)); G.arrows = []; G.boss = null; $('bossBar').classList.add('hide');
}
function randGround(minR, maxR) {
  for (let k = 0; k < 40; k++) { const a = Math.random() * TAU, r = rr(minR, maxR), x = Math.cos(a) * r, z = Math.sin(a) * r; if (Math.hypot(x - POND.x, z - POND.z) < POND.r + 1) continue; let bad = false; for (const c of nearColliders(x, z)) if (Math.hypot(x - c.x, z - c.z) < c.r + 0.8) { bad = true; break; } if (!bad) return [x, z]; }
  return [rr(-20, 20), rr(10, 30)];
}
function startDay() {
  clearEntities(); G.phase = 'day'; G.t = 0; G.warned = false; G.shrineUsed = false; G.lightOn = false; G.raining = Math.random() < 0.18;
  G.ppos.set(0.3, groundY(0.3, 3.2) + EYE_H, 3.2); G.yaw = 0; G.pitch = -0.05;
  // animals
  const n = 13 + Math.min(6, G.day / 10 | 0);
  for (let i = 0; i < n; i++) { const r = Math.random(), boarP = 0.12 + Math.min(0.15, G.day * 0.003); const type = r < boarP ? 'boar' : r < boarP + 0.22 ? 'deer' : r < boarP + 0.5 ? 'rabbit' : 'chicken'; const [x, z] = randGround(25, 150); G.animals.push(new Animal(type, x, z)); }
  const pk = { wood: 22, fruit: 12, herb: 8, mush: 9, honey: 3, bag: 2 };
  for (const k in pk) for (let i = 0; i < pk[k]; i++) { const [x, z] = randGround(k === 'wood' ? 8 : 15, k === 'bag' ? 160 : 145); addPickup(k, x, z); }
  setControls('day'); crossbow.visible = true;
  bigText(`วันที่ ${G.day}`, G.raining ? 'ฝนตกพรำ... ออกล่าสัตว์และหาเสบียงก่อนค่ำ' : 'ออกล่าสัตว์ หาไม้ หาอาหาร ก่อนตะวันตกดิน');
  fade(0);
}
function addPickup(type, x, z) { const g = buildPickup(type); g.position.set(x, groundY(x, z), z); g.rotation.y = Math.random() * TAU; scene.add(g); G.pickups.push({ type, g }); }

function endDay(early) {
  if (G.phase !== 'day') return;
  G.phase = 'dusk'; fade(1);
  const far = Math.hypot(G.ppos.x, G.ppos.z);
  setTimeout(() => {
    if (!early && far > 30) { G.hp -= 10; G.sanity -= 10; toast('วิ่งฝ่าความมืดกลับห้าง... บาดเจ็บ -10'); }
    startNight();
  }, 1300);
}
function startNight() {
  clearEntities(); G.phase = 'night'; G.t = 0; G.nightKills = 0; G.lightOn = true; G.raining = G.raining || Math.random() < 0.12;
  G.ppos.set(0, STAND_Y + EYE_H, 0); G.pitch = -0.1;
  // spawn schedule
  const n = Math.min(4 + Math.floor(G.day * 0.45), 46); const pool = Object.keys(GHOSTS).filter(k => GHOSTS[k].day <= G.day);
  G.queue = [];
  for (let i = 0; i < n; i++) { let tot = 0; pool.forEach(k => tot += GHOSTS[k].w); let r = Math.random() * tot, type = pool[0]; for (const k of pool) { r -= GHOSTS[k].w; if (r <= 0) { type = k; break; } } G.queue.push({ t: 4 + (i / n) * (NIGHT_LEN - 30) + rr(-2, 2), type }); }
  if (G.day === 1) G.queue = [{ t: 6, type: 'krasue' }, { t: 22, type: 'krasue' }, { t: 40, type: 'krasue' }, { t: 60, type: 'krasue' }];
  if (BOSS_DAYS.includes(G.day)) { G.queue.push({ t: 20, type: 'takhian' }); if (G.day === 100) { G.queue.push({ t: 50, type: 'takhian' }); G.queue.push({ t: 30, type: 'pret' }); } }
  G.queue.sort((a, b) => a.t - b.t);
  setControls('night');
  const boss = BOSS_DAYS.includes(G.day);
  bigText(`คืนที่ ${G.day}`, boss ? '⚠️ คืนนี้ลมเย็นผิดปกติ... นางตะเคียนกำลังมา' : 'ขึ้นห้างแล้ว เปิดไฟฉาย เฝ้าระวังรอบทิศ');
  fade(0);
  if (G.day === 1) setTimeout(() => toast('ลากหน้าจอเพื่อหันมอง · กดยิงเมื่อเล็งโดนผี'), 3000);
}
function endNight() {
  if (G.phase !== 'night') return;
  G.phase = 'dawn'; G.ghosts.forEach(g => { if (!g.dead) { g.dead = true; g.dying = 1; } });
  bigText('ไก่ขัน... รุ่งสางแล้ว', `รอดชีวิตคืนที่ ${G.day} · ปราบผีได้ ${G.nightKills} ตน`);
  setTimeout(() => {
    fade(1);
    setTimeout(() => {
      if (G.day >= MAX_DAY) { win(); return; }
      G.day++; G.battery = Math.min(100, G.battery + 45); G.sanity = Math.min(100, G.sanity + 25); G.stand = Math.min(100, G.stand + 3);
      saveGame(); startDay();
    }, 1300);
  }, 3500);
}
function gameOver(reason) {
  if (G.phase === 'over') return; G.phase = 'over'; setControls('none'); AU.scream('phrai', 0, 0.5);
  panel(`<h2>💀 จบชีวิต</h2><p>${reason}</p><p>รอดมาได้ ${G.day - 1} วัน · ปราบผีทั้งหมด ${G.kills} ตน</p>
  <div><button class="btn" onclick="retryDay()">ลองวันที่ ${G.day} ใหม่</button><button class="btn g" onclick="toTitle()">กลับหน้าหลัก</button></div>`);
}
function win() {
  G.phase = 'win'; setControls('none'); store.del('hp100_save');
  panel(`<h2>🏆 รอดครบ 100 วัน!</h2><p>ท่านคือนายพรานผู้กล้าแห่งพงไพร ผ่านคืนอันน่าสะพรึงมาได้ครบร้อยคืน</p><p>ปราบผีทั้งหมด ${G.kills} ตน</p><button class="btn" onclick="toTitle()">กลับหน้าหลัก</button>`);
  fade(0);
}
window.retryDay = () => { hidePanel(); if (!loadGame()) newGame(); fade(1); setTimeout(startDay, 600); };
window.toTitle = () => { hidePanel(); clearEntities(); showTitle(); };

// ===== combat API used by entities =====
G.hurtPlayer = (d, msg, silent) => {
  if (G.phase !== 'night' && G.phase !== 'day') return;
  if (G.stand <= 0 && G.phase === 'night') d *= 1.5;
  G.hp -= d; if (!silent) { G.hurtFlash = 1; G.shake = Math.max(G.shake, 0.35); AU.hurt(); if (msg) toast(msg); if (navigator.vibrate) try { navigator.vibrate(60); } catch (e) { } }
  else G.hurtFlash = Math.max(G.hurtFlash, 0.25);
};
G.damageStand = d => { const was = G.stand; G.stand = Math.max(0, G.stand - d); AU.creak(); if (was > 0 && G.stand <= 0) { bigText('ห้างพัง!', 'รีบซ่อมด้วยไม้ — ผีทำร้ายรุนแรงขึ้น'); } };
G.toast = m => toast(m);
const _fw = new THREE.Vector3(), _tp = new THREE.Vector3();
G.lookDot = p => { camera.getWorldDirection(_fw); _tp.copy(p).sub(camera.position).normalize(); return _fw.dot(_tp); };
G.inFlashlight = p => { if (!G.lightOn || G.battery <= 0) return false; const d = camera.position.distanceTo(p); return d < 48 && G.lookDot(p) > Math.cos(spot.angle * 0.85); };
G.spawnGhost = (type, a, d) => { const g = new Ghost(type, a, d, G.day); G.ghosts.push(g); if (GHOSTS[type].boss) { G.boss = g; $('bossBar').classList.remove('hide'); $('bossName').textContent = '👹 ' + GHOSTS[type].name; } return g; };
G.onGhostKilled = g => {
  G.kills++; G.nightKills++;
  if (g.cfg.boss) { G.inv.amulet++; G.boss = null; $('bossBar').classList.add('hide'); bigText('ปราบนางตะเคียนสำเร็จ!', 'ได้รับพระเครื่อง 📿 +1'); G.sanity = Math.min(100, G.sanity + 30); }
  else if (g.type === 'pret') { toast('ปราบเปรตได้! ได้รับข้าวสารเสก +2'); G.inv.rice += 2; }
  else toast('ปราบ' + g.cfg.name + 'ได้');
  if (Math.random() < 0.15) { G.inv.rice++; toast('ได้ข้าวสารเสก +1'); }
};

// ===== actions =====
function fire() {
  if (G.paused || (G.phase !== 'day' && G.phase !== 'night')) return;
  if (G.reload > 0) return;
  if (G.inv.arrows <= 0) { toast('ลูกดอกหมด! ใช้ไม้ทำลูกดอกในย่าม'); return; }
  G.inv.arrows--; G.reload = 0.75; AU.twang();
  const m = arrowMesh(); camera.getWorldDirection(_fw);
  const start = new THREE.Vector3(0.12, -0.08, -0.5).applyMatrix4(camera.matrixWorld);
  m.position.copy(start); scene.add(m);
  G.arrows.push({ m, v: _fw.clone().multiplyScalar(80), life: 3, stuck: false });
  crossbow.userData.arrow.visible = false; crossbow.position.z = -0.36;
  // alert nearby animals
  if (G.phase === 'day') G.animals.forEach(a => { if (!a.dead && a.g.position.distanceTo(G.ppos) < 30 && a.cfg.flee) { a.state = 'flee'; a.t = rr(3, 6); } });
}
function updateArrows(dt) {
  for (let i = G.arrows.length - 1; i >= 0; i--) {
    const A = G.arrows[i]; A.life -= dt;
    if (A.stuck) { if (A.life < 0 || G.phase !== A.phase) { scene.remove(A.m); G.arrows.splice(i, 1); } else if (G.phase === 'day' && A.m.position.distanceTo(G.ppos) < 2.2) { G.inv.arrows++; scene.remove(A.m); G.arrows.splice(i, 1); toast('เก็บลูกดอกคืน +1'); AU.pickup(); } continue; }
    if (A.life < 0) { scene.remove(A.m); G.arrows.splice(i, 1); continue; }
    A.v.y -= 3.5 * dt; const step = A.v.length() * dt, n = Math.ceil(step / 0.3); let hit = false;
    for (let s = 0; s < n && !hit; s++) {
      A.m.position.addScaledVector(A.v, dt / n); const p = A.m.position;
      if (G.phase === 'night') { for (const g of G.ghosts) { if (!g.dead && g.testPoint(p)) { if (g.hit(12, 'arrow', G)) { hit = true; flashCross(); scene.remove(A.m); G.arrows.splice(i, 1); break; } } } if (hit) break; }
      else if (G.phase === 'day') { for (const a of G.animals) { if (!a.dead && a.center().distanceTo(p) < a.cfg.r + 0.15) { a.hit(12, G.ppos); hit = true; flashCross(); A.stuck = true; A.life = 0.01; break; } } }
      if (hit) break;
      const gy = groundY(p.x, p.z); if (p.y < gy + 0.03) { p.y = gy + 0.05; A.stuck = true; A.life = 30; A.phase = G.phase; AU.thud(); hit = true; break; }
      for (const c of nearColliders(p.x, p.z)) if (p.y < c.gy + c.h && Math.hypot(p.x - c.x, p.z - c.z) < c.r) { A.stuck = true; A.life = 30; A.phase = G.phase; AU.thud(); hit = true; break; }
    }
    if (!hit || A.stuck) { if (!A.stuck) A.m.lookAt(_tp.copy(A.m.position).sub(A.v)); }
  }
}
function flashCross() { const c = $('cross'); c.classList.add('hit'); setTimeout(() => c.classList.remove('hit'), 200); }
function throwRice() {
  if (G.phase !== 'night' || G.paused) return; if (G.inv.rice <= 0) { toast('ข้าวสารเสกหมด — ไปไหว้ศาลพระภูมิตอนกลางวัน'); return; } if (G.riceCd > 0) return;
  G.inv.rice--; G.riceCd = 1; AU.rice(); camera.getWorldDirection(_fw);
  let best = null, bd = 1e9; const cp = camera.position;
  for (const g of G.ghosts) { if (g.dead) continue; const c = g.hitCenter(new THREE.Vector3()); if (g.type === 'pret') c.y += 7; const d = c.distanceTo(cp); if (d > 38) continue; const dot = _tp.copy(c).sub(cp).normalize().dot(_fw); if (dot > 0.95 && d < bd) { bd = d; best = c; } }
  const center = best || cp.clone().addScaledVector(_fw, 12);
  for (let k = 0; k < 10; k++) { const p = cp.clone().lerp(center, k / 10); spawnParts(p, 2, 0xffffff, 0.8, 0.4, 0.15); }
  spawnParts(center, 40, 0xfffbe0, 6, 0.9, 0.3, 2);
  let n = 0; G.ghosts.forEach(g => { if (g.dead) return; const c = g.hitCenter(new THREE.Vector3()); if (g.type === 'pret') c.y += 7; if (c.distanceTo(center) < 6) { g.hit(40, 'rice', G); g.stun = 1; n++; } });
  toast(n ? `ข้าวสารเสกโดนผี ${n} ตน!` : 'สาดข้าวสารเสก!');
}
function useAmulet() {
  if (G.phase !== 'night' || G.paused) return; if (G.inv.amulet <= 0) { toast('ไม่มีพระเครื่อง'); return; }
  G.inv.amulet--; AU.chant(); G.sanity = Math.min(100, G.sanity + 30); bigText('📿 พุทธคุณคุ้มครอง', 'ผีรอบห้างถูกแสงพระเผาไหม้');
  const fl = $('dmg'); fl.style.background = 'radial-gradient(ellipse at center,rgba(255,220,120,.7),rgba(255,180,60,.2))'; fl.style.opacity = 1; setTimeout(() => { fl.style.opacity = 0; setTimeout(() => fl.style.background = '', 600); }, 300);
  G.ghosts.forEach(g => { if (!g.dead && g.g.position.distanceTo(G.ppos) < 75) { g.hit(110, 'amulet', G); g.stun = 4; } });
}
function toggleLight() { if (G.battery <= 0) { toast('ถ่านไฟฉายหมด!'); G.lightOn = false; return; } G.lightOn = !G.lightOn; AU.tone('square', 1200, 900, 0.04, 0.05); }
function repairStand() {
  if (G.inv.wood <= 0) { toast('ไม่มีไม้สำหรับซ่อม'); return; } if (G.stand >= 100) { toast('ห้างยังแข็งแรงดี'); return; }
  G.inv.wood--; G.stand = Math.min(100, G.stand + 14); AU.thud(); setTimeout(() => AU.thud(), 180); toast('ซ่อมห้าง +14');
}
function eatBest() { for (const k of ['meat', 'honey', 'fruit', 'mush']) if (G.inv[k] > 0) { eat(k); return; } toast('ไม่มีอาหาร!'); }
function eat(k) {
  if (G.inv[k] <= 0) return; G.inv[k]--; AU.pickup();
  if (k === 'meat') { G.hunger = Math.min(100, G.hunger + 24); G.hp = Math.min(100, G.hp + 4); toast('กินเนื้อย่าง อิ่มท้อง +24'); }
  else if (k === 'fruit') { G.hunger = Math.min(100, G.hunger + 10); G.sanity = Math.min(100, G.sanity + 3); toast('กินผลไม้ป่า +10'); }
  else if (k === 'honey') { G.hunger = Math.min(100, G.hunger + 14); G.sanity = Math.min(100, G.sanity + 12); toast('กินน้ำผึ้งป่า หวานชื่นใจ'); }
  else if (k === 'mush') { if (Math.random() < 0.25) { G.hp -= 12; G.sanity -= 15; toast('🤢 เห็ดเมา! ตาลาย สติหลุด'); } else { G.hunger = Math.min(100, G.hunger + 9); toast('กินเห็ดป่า +9'); } }
  else if (k === 'herb') { G.hp = Math.min(100, G.hp + 25); toast('ตำสมุนไพรพอกแผล ชีวิต +25'); }
}
function craftArrows() { if (G.inv.wood <= 0) { toast('ต้องมีไม้'); return; } G.inv.wood--; G.inv.arrows += 4; AU.thud(); toast('เหลาลูกดอกได้ 4 ดอก'); }
function act() {
  const a = G.action; if (!a) return;
  if (a.k === 'stand') endDay(true);
  else if (a.k === 'carcass') { const an = a.o; G.inv.meat += an.cfg.meat; an.harvested = true; an.remove(); G.animals.splice(G.animals.indexOf(an), 1); AU.pickup(); toast(`ชำแหละ${an.cfg.name} ได้เนื้อ +${an.cfg.meat}`); }
  else if (a.k === 'shrine') {
    if (G.shrineUsed) { toast('วันนี้ไหว้ไปแล้ว'); return; }
    const off = G.inv.fruit > 0 ? 'fruit' : G.inv.meat > 0 ? 'meat' : G.inv.honey > 0 ? 'honey' : null; if (!off) { toast('ต้องมีผลไม้ เนื้อ หรือน้ำผึ้งเพื่อถวาย'); return; }
    G.inv[off]--; G.shrineUsed = true; G.inv.rice += 3; G.sanity = Math.min(100, G.sanity + 15); AU.chant(); bigText('🙏 ไหว้ศาลพระภูมิเจ้าที่', `ถวาย${INV_NAME[off]} · ได้ข้าวสารเสก +3 · จิตใจสงบ`);
    if (Math.random() < 0.12) { G.inv.amulet++; toast('เจ้าที่ประทานพระเครื่อง 📿 +1'); }
  }
  else if (a.k === 'takhian') { if (G.inv.fruit > 0) { G.inv.fruit--; G.sanity = Math.min(100, G.sanity + 10); toast('วางผลไม้ไหว้แม่ตะเคียน... ลมเย็นพัดผ่าน'); AU.scream('phrai', 0, 0.06); } else toast('ต้นตะเคียนผูกผ้าสามสี... อย่าลบหลู่'); }
}

// ===== input =====
const INP = { keys: {}, joy: null, look: null, jx: 0, jy: 0, mouseLock: false };
function setupInput() {
  const cv = $('c'), joy = $('joy'), joyK = $('joyK');
  const onDown = e => {
    if (G.phase !== 'day' && G.phase !== 'night') return; AU.init();
    if (e.pointerType === 'mouse') { if (!INP.mouseLock && cv.requestPointerLock) cv.requestPointerLock(); else fire(); return; }
    if (e.clientX < window.innerWidth * 0.42 && !INP.joy) { INP.joy = { id: e.pointerId, x: e.clientX, y: e.clientY }; joy.style.display = 'block'; joy.style.left = e.clientX + 'px'; joy.style.top = e.clientY + 'px'; joyK.style.transform = ''; $('joyHint').style.display = 'none'; }
    else if (!INP.look) INP.look = { id: e.pointerId, x: e.clientX, y: e.clientY };
  };
  cv.addEventListener('pointerdown', onDown);
  window.addEventListener('pointermove', e => {
    if (INP.mouseLock && e.pointerType === 'mouse') { G.yaw -= e.movementX * 0.0024; G.pitch = clamp(G.pitch - e.movementY * 0.0024, -1.35, 1.35); return; }
    if (INP.joy && e.pointerId === INP.joy.id) { let dx = e.clientX - INP.joy.x, dy = e.clientY - INP.joy.y; const d = Math.hypot(dx, dy), R = 55; if (d > R) { dx *= R / d; dy *= R / d; } INP.jx = dx / R; INP.jy = dy / R; joyK.style.transform = `translate(${dx}px,${dy}px)`; }
    else if (INP.look && e.pointerId === INP.look.id) { const s = 5.2 / Math.max(window.innerWidth, 600); G.yaw -= (e.clientX - INP.look.x) * s; G.pitch = clamp(G.pitch - (e.clientY - INP.look.y) * s, -1.35, 1.35); INP.look.x = e.clientX; INP.look.y = e.clientY; }
  });
  const onUp = e => { if (INP.joy && e.pointerId === INP.joy.id) { INP.joy = null; INP.jx = INP.jy = 0; joy.style.display = 'none'; } if (INP.look && e.pointerId === INP.look.id) INP.look = null; };
  window.addEventListener('pointerup', onUp); window.addEventListener('pointercancel', onUp);
  document.addEventListener('pointerlockchange', () => { INP.mouseLock = document.pointerLockElement === cv; });
  window.addEventListener('keydown', e => {
    INP.keys[e.code] = true; if (G.phase !== 'day' && G.phase !== 'night') return;
    if (e.code === 'Space') fire(); if (e.code === 'KeyF') toggleLight(); if (e.code === 'KeyR') throwRice(); if (e.code === 'KeyQ') useAmulet(); if (e.code === 'KeyE') act(); if (e.code === 'KeyT') repairStand(); if (e.code === 'KeyB') openBag(); if (e.code === 'KeyG') eatBest(); if (e.code === 'Escape') pause();
  });
  window.addEventListener('keyup', e => { INP.keys[e.code] = false; });
  const btn = (id, fn, look) => { const b = $(id); b.addEventListener('pointerdown', e => { e.preventDefault(); e.stopPropagation(); AU.init(); fn(); if (look && !INP.look && e.pointerType !== 'mouse') INP.look = { id: e.pointerId, x: e.clientX, y: e.clientY }; }); };
  btn('bFire', fire, true); btn('bRice', throwRice, true); btn('bLight', toggleLight); btn('bAmu', useAmulet); btn('bAct', act); btn('bRepair', repairStand); btn('bEatN', eatBest); btn('bBag', openBag); btn('bPause', pause);
  btn('bRun', () => { G.running = !G.running; $('bRun').classList.toggle('on', G.running); });
  document.addEventListener('contextmenu', e => e.preventDefault());
}
function setControls(mode) {
  const show = (id, v) => $(id).classList.toggle('hide', !v);
  $('hud').classList.toggle('hide', mode === 'none'); $('ctl').classList.toggle('hide', mode === 'none');
  show('bRun', mode === 'day'); show('bRice', mode === 'night'); show('bLight', mode === 'night'); show('bAmu', mode === 'night'); show('bRepair', mode === 'night');
  show('home', mode === 'day'); show('joyHint', mode === 'day'); show('bAct', false);
  G.running = false; $('bRun').classList.remove('on');
}

// ===== UI =====
function toast(m) { const d = document.createElement('div'); d.textContent = m; const t = $('toast'); t.appendChild(d); while (t.children.length > 4) t.removeChild(t.firstChild); setTimeout(() => d.remove(), 2700); }
let bigTO; function bigText(a, b) { const e = $('big'); e.innerHTML = a + (b ? `<small>${b}</small>` : ''); e.style.opacity = 1; clearTimeout(bigTO); bigTO = setTimeout(() => e.style.opacity = 0, 3200); }
function fade(v) { $('fade').style.opacity = v; }
function panel(html) { $('card').innerHTML = html; $('panel').classList.remove('hide'); }
function hidePanel() { $('panel').classList.add('hide'); }
window.hidePanel = hidePanel;
function pause() {
  if (G.phase !== 'day' && G.phase !== 'night') return; G.paused = true; if (document.exitPointerLock) document.exitPointerLock();
  panel(`<h2>⏸ พักเกม</h2><p>วันที่ ${G.day} · ${G.phase === 'day' ? 'กลางวัน' : 'กลางคืน'} · ปราบผี ${G.kills} ตน</p>
  <p>เสียง: <input type="range" min="0" max="100" value="${AU.vol * 100}" oninput="setVol(this.value)" style="width:50%;touch-action:auto"></p>
  <div><button class="btn" onclick="resume()">เล่นต่อ</button><button class="btn g" onclick="showHelp(true)">วิธีเล่น</button><button class="btn g" onclick="toTitle()">ออกหน้าหลัก</button></div>
  <p style="font-size:12px;opacity:.6">เกมบันทึกอัตโนมัติทุกเช้า</p>`);
}
window.setVol = v => { AU.vol = v / 100; if (AU.master) AU.master.gain.value = AU.vol; store.set('hp100_vol', AU.vol); };
window.resume = () => { hidePanel(); G.paused = false; };
function openBag() {
  if (G.phase !== 'day' && G.phase !== 'night') return; G.paused = true;
  const it = (k, desc, btns) => `<div class="item">${INV_ICON[k]} <b>${INV_NAME[k]}</b> × ${G.inv[k]}<br><span style="opacity:.75">${desc}</span><br>${btns || ''}</div>`;
  const eb = k => G.inv[k] > 0 ? `<button class="btn" onclick="bagEat('${k}')">กิน/ใช้</button>` : '';
  panel(`<h2>🎒 ย่ามนายพราน</h2><div class="grid">
   ${it('meat', 'ความหิว +24 ชีวิต +4', eb('meat'))}${it('fruit', 'ความหิว +10 · ใช้ถวายศาลได้', eb('fruit'))}${it('honey', 'ความหิว +14 สติ +12', eb('honey'))}
   ${it('mush', 'ความหิว +9 (มีโอกาสเป็นเห็ดเมา!)', eb('mush'))}${it('herb', 'ชีวิต +25', eb('herb'))}
   ${it('wood', 'ซ่อมห้าง หรือเหลาลูกดอก', G.inv.wood > 0 ? `<button class="btn" onclick="bagCraft()">เหลาลูกดอก (+4)</button><button class="btn g" onclick="bagRepair()">ซ่อมห้าง</button>` : '')}
   ${it('arrows', 'อาวุธหลัก ยิงด้วยหน้าไม้')}${it('rice', 'สาดใส่ผี ทำลายเป็นวงกว้าง (กลางคืน)')}${it('amulet', 'ทำลายผีรอบห้างทั้งหมด (กลางคืน)')}
  </div><p>❤️ ${G.hp | 0} · 🍖 ${G.hunger | 0} · 🧠 ${G.sanity | 0} · 🛖 ${G.stand | 0} · 🔦 ${G.battery | 0}</p><button class="btn" onclick="resume()">ปิด</button>`);
}
window.bagEat = k => { eat(k); openBag(); }; window.bagCraft = () => { craftArrows(); openBag(); };
window.bagRepair = () => { if (G.phase === 'day' && Math.hypot(G.ppos.x, G.ppos.z) > 6) { toast('ต้องอยู่ใกล้ต้นไม้ห้างจึงซ่อมได้'); return; } repairStand(); openBag(); };

const HELP = `<h2>📜 วิธีเล่น</h2>
<p><b>เป้าหมาย:</b> เอาชีวิตรอดในป่าลึกให้ครบ <b>100 วัน</b></p>
<p>☀️ <b>กลางวัน</b> — ลงจากห้างไปล่าสัตว์ (ไก่ป่า กระต่าย เก้ง หมูป่า) ยิงแล้วเดินไปกด <b>ชำแหละ</b> เก็บไม้ ผลไม้ สมุนไพร เห็ด รังผึ้ง ย่ามนายพรานเก่า · ลูกดอกที่ยิงพลาดเดินไปเก็บคืนได้ · ระวัง <b>หมูป่า</b> จะพุ่งชน!</p>
<p>🙏 <b>ศาลพระภูมิ</b> — ถวายผลไม้/เนื้อวันละครั้ง ได้ <b>ข้าวสารเสก</b> · ดูลูกศรนำทางด้านบนเพื่อกลับห้าง เดินไปที่บันไดแล้วกด <b>ขึ้นห้าง</b></p>
<p>🌙 <b>กลางคืน</b> — นั่งบนห้าง ลากจอเพื่อหันมอง เปิด <b>ไฟฉาย</b> ยิงหน้าไม้ใส่ผี · <b>ข้าวสาร</b> สาดโดนเป็นวงกว้าง · <b>พระ</b> ทำลายผีรอบห้าง · ใช้ไม้ <b>ซ่อมห้าง</b> เมื่อผีปีนขึ้นมาพัง</p>
<p>🧠 <b>สติ</b> ลดลงเมื่อเจอผี ถ้าหมดจะเสียชีวิตทีละน้อย · 🍖 ความหิวหมดก็เสียชีวิต · ไฟฉายใช้ถ่าน ได้คืนบางส่วนทุกเช้าและในย่ามนายพราน</p>
<p>🎮 <b>ควบคุม:</b> ซ้ายจอ = จอยเดิน · ขวาจอ = หันมอง · ปุ่มยิงลากเล็งได้ (คีย์บอร์ด: WASD, เมาส์, Space ยิง, F ไฟ, R ข้าวสาร, Q พระ, E ใช้งาน, T ซ่อม, G กิน, B ย่าม)</p>`;
const LORE = `<h2>👻 ตำนานผีแห่งพงไพร</h2>
<p><b>กระสือ</b> (คืนที่ 1+) — หัวหญิงสาวลอยได้ ห้อยไส้เรืองแสง บินโฉบกัดแล้วถอย ยิงง่ายแต่ว่องไว</p>
<p><b>ผีปอบ</b> (2+) — ร่างผอมแห้งตาแดง เดินมาปีนต้นไม้ ข่วนทั้งคนและพังห้าง</p>
<p><b>ผีโขมด</b> (3+) — ดวงไฟลอยวนในป่า หากจ้องนานสติจะหลุด ยิงให้ดับ</p>
<p><b>เสือสมิง</b> (4+) — เสือที่สิงร่างคน วิ่งเร็วและปีนต้นไม้ได้ไว ตะปบแรงมาก ฆ่าก่อนถึงโคนต้น!</p>
<p><b>ผีพราย</b> (6+) — หญิงชุดขาวผมยาว มองไม่เห็นในความมืด <b>ต้องส่องไฟฉาย</b> ลูกดอกจึงจะโดน ข้าวสารเสกได้ผลดี</p>
<p><b>ผีกระหัง</b> (8+) — ชายใช้กระด้งเป็นปีก สากตำข้าวเป็นหาง บินวนแล้วโฉบใส่</p>
<p><b>เปรต</b> (14+) — สูงเท่าต้นตาล ปากเท่ารูเข็ม เสียงร้องหวีดแหลมกัดกินสติ ทนทานมาก</p>
<p><b>นางตะเคียน</b> (คืนที่ 20, 40, 60, 80, 100) — วิญญาณสิงต้นตะเคียนผูกผ้าสามสี หายตัวเข้าใกล้ กรีดร้อง และเรียกกระสือ ปราบได้รับพระเครื่อง</p>`;
window.showHelp = back => panel(HELP + `<button class="btn" onclick="${back ? 'pause()' : 'hidePanel()'}">ตกลง</button>`);
window.pause = pause;
function setupUI() {
  AU.vol = store.get('hp100_vol', 0.8);
  $('tNew').onclick = () => { AU.init(); const s = store.get('hp100_save', null); if (s && s.day > 1) { panel(`<h2>เริ่มเกมใหม่?</h2><p>เซฟเดิม (วันที่ ${s.day}) จะถูกลบ</p><button class="btn" onclick="hidePanel();confirmNew()">เริ่มใหม่</button><button class="btn g" onclick="hidePanel()">ยกเลิก</button>`); return; } confirmNew(); };
  window.confirmNew = () => { newGame(); beginPlay(); };
  $('tCont').onclick = () => { AU.init(); if (loadGame()) beginPlay(); };
  $('tHelp').onclick = () => panel(HELP + `<button class="btn" onclick="hidePanel()">ตกลง</button>`);
  $('tGhosts').onclick = () => panel(LORE + `<button class="btn" onclick="hidePanel()">ตกลง</button>`);
  document.querySelectorAll('#qual b').forEach(b => { b.classList.toggle('on', +b.dataset.q === QUALITY); b.onclick = () => { store.set('hp100_q', +b.dataset.q); location.reload(); }; });
  window.onAppPause = () => { if (G.phase === 'day' || G.phase === 'night') { if (!G.paused) pause(); } if (AU.ctx) AU.ctx.suspend(); };
  window.onAppResume = () => { if (AU.ctx) AU.ctx.resume(); };
  window.onBackButton = () => { if (!$('panel').classList.contains('hide')) { if (G.paused) resume(); else hidePanel(); return true; } if (G.phase === 'day' || G.phase === 'night') { pause(); return true; } return false; };
  document.addEventListener('visibilitychange', () => { if (document.hidden) window.onAppPause(); });
}
function showTitle() { G.phase = 'title'; G.paused = false; setControls('none'); crossbow.visible = false; $('title').classList.remove('hide'); const s = store.get('hp100_save', null); $('tCont').disabled = !s; $('tCont').textContent = s ? `เล่นต่อ (วันที่ ${s.day})` : 'เล่นต่อ'; fade(0); }
function beginPlay() { $('title').classList.add('hide'); fade(1); setTimeout(() => { G.paused = false; startDay(); if (G.day === 1) setTimeout(() => showHelp(false), 600); }, 900); }

// ===== HUD =====
let hudT = 0;
function updateHUD(dt) {
  hudT -= dt; if (hudT > 0) return; hudT = 0.1;
  $('dayLbl').textContent = `วันที่ ${G.day} / ${MAX_DAY}`;
  const left = (G.phase === 'night' ? NIGHT_LEN : DAY_LEN) - G.t; const mm = Math.max(0, left) | 0;
  $('phaseLbl').textContent = G.phase === 'night' ? `🌙 กลางคืน · รุ่งสางใน ${mm} วิ` : G.phase === 'day' ? `☀️ กลางวัน · ค่ำใน ${mm} วิ${G.raining ? ' · 🌧' : ''}` : G.phase === 'dawn' ? '🌅 รุ่งสาง' : '🌆 พลบค่ำ';
  const bar = (id, v) => { $(id).firstElementChild.style.width = clamp(v, 0, 100) + '%'; };
  bar('bHp', G.hp); bar('bHun', G.hunger); bar('bSan', G.sanity); bar('bStd', G.stand); bar('bBat', G.battery);
  $('inv').innerHTML = ['arrows', 'wood', 'meat', 'fruit', 'herb', 'rice', 'amulet'].map(k => `<span>${INV_ICON[k]}${G.inv[k]}</span>`).join('');
  $('nRice').textContent = G.inv.rice; $('nAmu').textContent = G.inv.amulet; $('bLight').classList.toggle('on', G.lightOn);
  if (G.phase === 'day') {
    const dx = -G.ppos.x, dz = -G.ppos.z, d = Math.hypot(dx, dz); const ang = Math.atan2(dx, dz) - G.yaw - Math.PI; // angle relative to facing (-z forward)
    $('homeArrow').style.transform = `rotate(${-ang * 180 / Math.PI}deg)`; $('homeTxt').textContent = `ห้าง ${d | 0} ม.`;
  }
  if (G.boss) $('bossBar').querySelector('i').style.width = clamp(G.boss.hp / G.boss.maxHp * 100, 0, 100) + '%';
  const a = G.action; $('bAct').classList.toggle('hide', !a); if (a) $('actTxt').textContent = a.label;
}

// ===== main loop =====
let last = performance.now(), fpsN = 0, fpsT = 0;
const _fwd = new THREE.Vector3(), _rt = new THREE.Vector3(), _env = {};
function loop(now) {
  requestAnimationFrame(loop);
  let dt = Math.min(0.05, (now - last) / 1000); last = now; fpsN++; fpsT += dt; if (fpsT > 1) { $('fps').textContent = fpsN + ' fps'; fpsN = 0; fpsT = 0; }
  G.time += dt; W.uTime.value = G.time;
  if (G.phase === 'title') { titleCam(dt); render(); return; }
  if (!G.paused) step(dt);
  render();
}
function titleCam(dt) {
  const a = G.time * 0.05; camera.position.set(Math.cos(a) * 22, 6 + Math.sin(G.time * 0.1) * 1.5, Math.sin(a) * 22); camera.lookAt(0, STAND_Y + 1, 0);
  const sd = new THREE.Vector3(-0.6, 0.12, -0.5).normalize(), md = new THREE.Vector3(0.4, 0.6, 0.3).normalize();
  applyEnv(mixEnv(ENV_DUSK, ENV_NIGHT, 0.55), sd, md, new THREE.Vector3(0, 0, 0)); spot.intensity = 0;
  updateAtmos(dt, G.time, 0.8, camera.position, false); AU.setAmb(0.2, 0.3, 0);
}
function step(dt) {
  G.t += dt; const night = G.phase === 'night' || G.phase === 'dawn';
  G.reload -= dt; G.riceCd -= dt; if (G.reload <= 0 && crossbow) { crossbow.userData.arrow.visible = G.inv.arrows > 0; crossbow.position.z = lerp(crossbow.position.z, -0.42, dt * 10); }
  // movement
  let mx = INP.jx, my = INP.jy; const K = INP.keys; if (K.KeyW) my = -1; if (K.KeyS) my = 1; if (K.KeyA) mx = -1; if (K.KeyD) mx = 1;
  if (K.ArrowLeft) G.yaw += dt * 2; if (K.ArrowRight) G.yaw -= dt * 2;
  const run = (G.running || K.ShiftLeft) && G.phase === 'day';
  const mag = Math.min(1, Math.hypot(mx, my));
  if ((G.phase === 'day' || G.phase === 'night') && mag > 0.08) {
    const sp = G.phase === 'night' ? 1.6 : run ? 6.2 : 3.3; _fwd.set(-Math.sin(G.yaw), 0, -Math.cos(G.yaw)); _rt.set(Math.cos(G.yaw), 0, -Math.sin(G.yaw));
    let nx = G.ppos.x + (_fwd.x * -my + _rt.x * mx) * sp * dt, nz = G.ppos.z + (_fwd.z * -my + _rt.z * mx) * sp * dt;
    if (G.phase === 'day') {
      for (const c of nearColliders(nx, nz)) { const ex = nx - c.x, ez = nz - c.z, ed = Math.hypot(ex, ez), mr = c.r + 0.35; if (ed < mr && ed > 1e-4) { nx = c.x + ex / ed * mr; nz = c.z + ez / ed * mr; } }
      const R = Math.hypot(nx, nz); if (R > WORLD_R) { nx *= WORLD_R / R; nz *= WORLD_R / R; }
      const dp = Math.hypot(nx - POND.x, nz - POND.z); if (dp < POND.r - 3) { nx = POND.x + (nx - POND.x) / dp * (POND.r - 3); nz = POND.z + (nz - POND.z) / dp * (POND.r - 3); }
      G.ppos.x = nx; G.ppos.z = nz; G.bob += dt * sp * 2.2 * mag; G.stepT -= dt * sp * mag; if (G.stepT < 0) { G.stepT = 1.6; AU.step(run); }
      G.hunger -= dt * (run ? 0.06 : 0) * mag;
    } else { G.ppos.x = clamp(nx, -1.9, 1.9); G.ppos.z = clamp(nz, -1.9, 1.9); G.bob += dt * 3 * mag; }
  }
  if (G.phase === 'day') G.ppos.y = lerp(G.ppos.y, groundY(G.ppos.x, G.ppos.z) + EYE_H, Math.min(1, dt * 12)); else if (night) G.ppos.y = STAND_Y + EYE_H;
  // camera
  G.shake = Math.max(0, G.shake - dt); const sh = G.shake * 0.08;
  let pitchOff = 0, yawOff = 0;
  if (G.sanity < 30) { const k = (30 - G.sanity) / 30; yawOff = Math.sin(G.time * 0.7) * 0.03 * k; pitchOff = Math.sin(G.time * 1.1) * 0.02 * k; camera.fov = 70 + Math.sin(G.time * 1.6) * 4 * k; camera.updateProjectionMatrix(); } else if (camera.fov !== 70) { camera.fov = 70; camera.updateProjectionMatrix(); }
  camera.position.set(G.ppos.x + rr(-sh, sh), G.ppos.y + Math.sin(G.bob) * 0.05 + rr(-sh, sh), G.ppos.z + rr(-sh, sh));
  camera.rotation.set(G.pitch + pitchOff, G.yaw + yawOff, 0, 'YXZ');
  crossbow.position.y = -0.17 + Math.sin(G.bob * 0.5) * 0.008; crossbow.position.x = 0.17 + Math.cos(G.bob * 0.5) * 0.006;
  // environment
  const sunDir = new THREE.Vector3(), moonDir = new THREE.Vector3(); let env, night01 = 0;
  if (G.phase === 'day' || G.phase === 'dusk') {
    const p = clamp(G.t / DAY_LEN, 0, 1); const el = lerp(0.35, Math.PI - 0.2, p); sunDir.set(Math.cos(el) * 0.8, Math.sin(el) * 0.9 + 0.05, 0.45).normalize();
    env = p < 0.05 ? mixEnv(ENV_DUSK, ENV_DAY, 0.55 + 0.45 * p / 0.05) : p > 0.8 ? mixEnv(ENV_DAY, ENV_DUSK, (p - 0.8) / 0.2) : mixEnv(ENV_DAY, ENV_DAY, 0);
    if (G.phase === 'dusk') env = mixEnv(ENV_DUSK, ENV_NIGHT, 0.5);
    moonDir.set(0, -1, 0); night01 = p > 0.9 ? (p - 0.9) * 3 : 0;
  } else {
    const p = G.t / NIGHT_LEN; moonDir.set(Math.cos(lerp(0.4, 2.6, p)) * 0.7, 0.55 + Math.sin(p * Math.PI) * 0.3, -0.45).normalize(); sunDir.set(-1, -0.3, 0).normalize();
    env = p < 0.06 ? mixEnv(ENV_DUSK, ENV_NIGHT, p / 0.06) : G.phase === 'dawn' ? mixEnv(ENV_NIGHT, ENV_DUSK, clamp((G.t - NIGHT_LEN) / 4, 0, 0.7)) : mixEnv(ENV_NIGHT, ENV_NIGHT, 0);
    if (G.phase === 'dawn') { sunDir.set(-0.9, 0.08, 0.3).normalize(); }
    night01 = 1;
  }
  if (G.raining) { env.den *= 1.5; env.sun *= 0.5; env.hemi *= 0.8; env.sunS *= 0.3; }
  applyEnv(env, sunDir, moonDir, G.phase === 'day' ? G.ppos : new THREE.Vector3(0, STAND_Y, 0));
  updateAtmos(dt, G.time, night01, G.ppos, G.raining);
  // flashlight
  if (night && G.lightOn && G.battery > 0) { G.battery = Math.max(0, G.battery - dt * 0.75); spot.intensity = G.battery < 10 ? (Math.random() < 0.1 ? 0 : 1.4) : 2.4; flashFlare.material.opacity = 0.9; if (G.battery <= 0) { G.lightOn = false; toast('ถ่านไฟฉายหมด!'); } } else { spot.intensity = 0; flashFlare.material.opacity = 0; }
  crossbow.userData.mats.forEach(m => m.emissive.setHex(night ? 0x1a1410 : 0x000000));
  // stats
  if (G.phase === 'day' || G.phase === 'night') {
    G.hunger -= dt * 0.055; if (G.hunger <= 0) { G.hunger = 0; G.hp -= dt * 0.6; }
    if (G.phase === 'day') G.sanity = Math.min(100, G.sanity + dt * 0.12); else G.sanity -= dt * 0.02;
    if (G.hunger > 60 && G.hp < 100) G.hp = Math.min(100, G.hp + dt * 0.05);
    G.sanity = clamp(G.sanity, 0, 100); if (G.sanity <= 0) G.hp -= dt * 1.5;
    if (G.hp <= 0) { G.hp = 0; const why = G.hunger <= 0 ? 'อดอาหารจนหมดแรง...' : G.sanity <= 0 ? 'สติแตก หลงเข้าป่าลึกไม่มีวันกลับ...' : G.phase === 'night' ? 'ถูกผีป่าคร่าชีวิตบนห้าง...' : 'บาดเจ็บสาหัสกลางป่า...'; gameOver(why); return; }
  }
  // phase logic
  G.action = null;
  if (G.phase === 'day') {
    if (G.t > DAY_LEN - 20 && !G.warned) { G.warned = true; bigText('ตะวันใกล้ลับฟ้า!', 'รีบกลับขึ้นห้างก่อนมืด'); AU.owl(); }
    if (G.t > DAY_LEN) endDay(false);
    G.animals.forEach(a => a.update(dt, G.ppos, G));
    G.crouch = false;
    // context actions
    const pp = G.ppos;
    if (Math.hypot(pp.x, pp.z) < 4.2) G.action = { k: 'stand', label: 'ขึ้นห้าง' };
    if (Math.hypot(pp.x - SHRINE.x, pp.z - SHRINE.z) < 3.5) G.action = { k: 'shrine', label: G.shrineUsed ? 'ไหว้แล้ว' : 'ไหว้ศาล' };
    const dtk = Math.hypot(pp.x - TAKHIAN.x, pp.z - TAKHIAN.z); if (dtk < 6) { G.action = { k: 'takhian', label: 'ไหว้ตะเคียน' }; G.sanity -= dt * 0.4; if (Math.random() < dt * 0.1) toast('รู้สึกเหมือนมีใครจ้องมอง...'); }
    for (const a of G.animals) if (a.dead && !a.harvested && a.g.position.distanceTo(_tp.set(pp.x, pp.y - EYE_H, pp.z)) < 2.4) { G.action = { k: 'carcass', o: a, label: 'ชำแหละ' }; break; }
    // pickups
    for (let i = G.pickups.length - 1; i >= 0; i--) {
      const P = G.pickups[i]; P.g.userData.spark.material.opacity = 0.25 + Math.sin(G.time * 3 + i) * 0.15;
      if (Math.hypot(P.g.position.x - pp.x, P.g.position.z - pp.z) < 1.6) { collect(P.type); scene.remove(P.g); G.pickups.splice(i, 1); }
    }
    $('bRepair').classList.toggle('hide', Math.hypot(pp.x, pp.z) > 6);
  } else if (G.phase === 'night') {
    while (G.queue.length && G.queue[0].t <= G.t) { const q = G.queue.shift(); let a = Math.random() * TAU; if (q.type === 'takhian') a = Math.atan2(TAKHIAN.z, TAKHIAN.x); G.spawnGhost(q.type, a, q.type === 'khamot' ? rr(24, 30) : q.type === 'takhian' ? 34 : rr(40, 62)); }
    if (G.t > NIGHT_LEN) endNight();
    // hallucination whispers
    if (G.sanity < 30) { G.insaneT -= dt; if (G.insaneT < 0) { G.insaneT = rr(4, 9); AU.scream(pick(['phrai', 'krasue']), rr(-1, 1), 0.08); } }
  }
  for (let i = G.ghosts.length - 1; i >= 0; i--) { if (!G.ghosts[i].update(dt, G)) { G.ghosts[i].remove(); G.ghosts.splice(i, 1); } }
  if (G.phase === 'dawn') { /* allow dissolve */ }
  updateArrows(dt); updateParts(dt);
  // overlays
  G.hurtFlash = Math.max(0, G.hurtFlash - dt * 1.8); $('dmg').style.opacity = G.hurtFlash;
  $('insane').style.opacity = clamp((40 - G.sanity) / 40, 0, 1) * (0.7 + Math.sin(G.time * 2) * 0.3);
  // audio
  const dread = night ? clamp(G.ghosts.filter(g => !g.dead).length / 6, 0.2, 1) : 0;
  AU.setAmb(night ? 0 : 1, dread, G.sanity < 35 ? 1 : 0); AU.update(dt, night ? 0 : 1, G.sanity);
  updateHUD(dt);
}
function collect(type) {
  AU.pickup();
  if (type === 'wood') { G.inv.wood += 1; toast('🪵 เก็บกิ่งไม้ +1'); }
  else if (type === 'fruit') { G.inv.fruit += 1; toast('🍈 เก็บผลไม้ป่า +1'); }
  else if (type === 'herb') { G.inv.herb += 1; toast('🌿 เก็บสมุนไพร +1'); }
  else if (type === 'mush') { G.inv.mush += 1; toast('🍄 เก็บเห็ดป่า +1'); }
  else if (type === 'honey') { G.inv.honey += 1; G.hp -= 3; toast('🍯 ได้รังผึ้ง! (โดนผึ้งต่อย -3)'); }
  else if (type === 'bag') {
    const r = Math.random(); let msg = '🎒 ย่ามนายพรานเก่า: ';
    const a = 4 + (Math.random() * 6 | 0); G.inv.arrows += a; msg += `ลูกดอก +${a}`;
    if (r < 0.6) { G.battery = Math.min(100, G.battery + 40); msg += ' · ถ่านไฟฉาย +40'; }
    if (r > 0.5) { G.inv.rice += 2; msg += ' · ข้าวสารเสก +2'; }
    if (r > 0.85) { G.inv.amulet += 1; msg += ' · พระเครื่อง +1'; }
    toast(msg);
  }
}
function render() { renderer.render(scene, camera); }

window.addEventListener('load', () => setTimeout(() => { try { init(); } catch (e) { $('loading').textContent = 'เกิดข้อผิดพลาด: ' + e.message; console.error(e); } }, 50));
