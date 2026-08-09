(function () {
  const M = window.Music;
  const G = window.FretGeom;
  const SHAPE_DEFS = window.FretCaged.SHAPE_DEFS;
  const t = (path, vars) => (window.FretI18n && window.FretI18n.t(path, vars)) || path;
  const $ = (s) => document.querySelector(s);
  const neck = $('#neck');
  const strLabels = $('#string-labels');
  const numLabels = $('#string-numbers');
  const boardWrap = $('#board-wrap');
  const tip = $('#tip');
  const ctxMenu = $('#ctx-menu');

  const INLAY_FRETS = [3, 5, 7, 9, 12, 15, 17, 19, 21, 24];
  const CHROMATIC_ROMAN = ['I', 'bII', 'II', 'bIII', 'III', 'IV', 'bV', 'V', 'bVI', 'VI', 'bVII', 'VII'];
  function degreeRoman(rel) {
    return CHROMATIC_ROMAN[((rel % 12) + 12) % 12];
  }
  const FORMULA = ['1', '♭2', '2', '♭3', '3', '4', '♯4', '5', '♭6', '6', '♭7', '7'];

  function theoryInfo(modeName, familyKey) {
    const raw = window.FretI18n && window.FretI18n.raw;
    if (!raw) return null;
    if (modeName) {
      const byMode = raw('modeInfo.' + modeName);
      if (byMode && typeof byMode === 'object') return byMode;
    }
    if (familyKey) {
      const byFam = raw('scaleInfo.' + familyKey);
      if (byFam && typeof byFam === 'object') return byFam;
    }
    return null;
  }
  const DEFAULT_TUNING_ID = 'std6';
  const DEFAULT_FRET_COUNT = 15;

  const state = {
    tab: 'explore',
    stringCount: 6,
    tuningId: 'std6',
    customPcs: [4, 9, 2, 7, 11, 4],
    fretCount: 15,
    flats: false,
    sound: true,
    showNames: true,
    showDegrees: true,
    showIntervals: false,
    compoundIntervals: true,
    fullChart: false,
    cagedKey: 0,
    shapes: { C: true, A: true, G: true, E: true, D: true },
    showCagedShapes: true,
    showChordTones: true,
    showRootLines: true,
    scaleKey: 0,
    scaleFamily: 'major',
    scaleCategory: 'church',
    modeIndex: 0,
    bpm: 120,
    groove: 'downsideUp',
    chordNotes: [],
    pinRoot: null,
    showIntervalGhost: false,
    profiles: [],
    profileSeq: 0,
    activeProfileId: null
  };

  let profiles;
  const freshShapes = new Set();
  let chordResult = null;
  let scaleSel = null;
  let chordScaleFocus = null;
  let scalePropFollow = true;
  let scalePropKey = 0;
  let noteSeq = 0;
  let ghostPlus = null;

  function proposalKeyPc(r) {
    if (scalePropFollow || !r) return r ? r.rootPc : 0;
    return scalePropKey;
  }

  function chordScaleOpts(r, keyPc) {
    return { names: names(), compoundIntervals: state.compoundIntervals, scaleRoot: keyPc != null ? keyPc : proposalKeyPc(r) };
  }

  function names() {
    const key = state.flats ? 'noteNames.flat' : 'noteNames.sharp';
    const arr = window.FretI18n && window.FretI18n.raw(key);
    if (Array.isArray(arr) && arr.length === 12) return arr;
    return state.flats ? M.FLAT : M.SHARP;
  }

  function midiLabel(midi) {
    const m = midi | 0;
    const pc = ((m % 12) + 12) % 12;
    return names()[pc] + (Math.floor(m / 12) - 1);
  }

  const audio = window.FretAudio.create({
    getSound: () => state.sound,
    getBpm: () => state.bpm,
    getGroove: () => state.groove,
    getRootPc: () => rootPc(),
    getNeck: () => neck,
    midiToFreq: M.midiToFreq
  });
  const playMidi = audio.playMidi;
  const playSequence = audio.playSequence;

  function activeProfile() {
    return state.activeProfileId ? state.profiles.find((p) => p.id === state.activeProfileId) || null : null;
  }

  function syncBoardSource() {
    const p = activeProfile();
    if (p) {
      state.stringCount = p.tuning.length;
      state.fretCount = p.fretCount;
    } else {
      const t = M.TUNINGS.find((x) => x.id === DEFAULT_TUNING_ID);
      state.stringCount = t ? t.strings : 6;
      state.fretCount = DEFAULT_FRET_COUNT;
    }
  }

  function tuningMidis() {
    const p = activeProfile();
    if (p) return p.tuning.map((t) => profiles.strToMidi(t));
    const t = M.TUNINGS.find((x) => x.id === DEFAULT_TUNING_ID);
    return t ? t.midi.slice() : [40, 45, 50, 55, 59, 64];
  }

  function rootPc() {
    return state.scaleKey;
  }

  function setExploreRoot(pc) {
    state.scaleKey = pc;
    state.cagedKey = pc;
  }

  function catForFamily(fk) {
    for (let i = 0; i < M.SCALE_CATS.length; i++) {
      if (M.SCALE_CATS[i].families.indexOf(fk) >= 0) return M.SCALE_CATS[i].id;
    }
    return null;
  }

  function scaleCatLabel(id, kind) {
    const key = 'scaleCats.' + id + '.' + (kind || 'label');
    const val = t(key);
    return val === key ? id : val;
  }

  function scaleFamilyLabel(fk) {
    const key = 'scales.' + fk + '.label';
    const val = t(key);
    if (val !== key) return val;
    return (M.SCALES[fk] && M.SCALES[fk].label) || fk;
  }

  function scaleFamilyShort(fk) {
    const key = 'scales.' + fk + '.short';
    const val = t(key);
    if (val !== key) return val;
    return scaleFamilyLabel(fk).split(/[\s(/]/)[0];
  }

  function scaleModeLabel(fk, modeIndex) {
    const modes = window.FretI18n && window.FretI18n.raw('scales.' + fk + '.modes');
    if (Array.isArray(modes) && modes[modeIndex] != null) return modes[modeIndex];
    const fam = M.SCALES[fk];
    if (fam && fam.modes && fam.modes[modeIndex] != null) return fam.modes[modeIndex];
    return scaleFamilyLabel(fk);
  }

  function scaleDisplayName(fk, modeIndex) {
    const fam = M.SCALES[fk];
    if (fam && fam.modes) return scaleModeLabel(fk, modeIndex | 0);
    return scaleFamilyLabel(fk);
  }

  function clusterScaleEntries(entries) {
    const buckets = {};
    entries.forEach((e) => {
      const id = catForFamily(e.family) || 'other';
      if (!buckets[id]) buckets[id] = [];
      buckets[id].push(e);
    });
    const groups = [];
    M.SCALE_CATS.forEach((cat) => {
      if (!buckets[cat.id] || !buckets[cat.id].length) return;
      groups.push({ id: cat.id, label: scaleCatLabel(cat.id, 'short'), entries: buckets[cat.id] });
    });
    if (buckets.other && buckets.other.length) {
      groups.push({ id: 'other', label: scaleCatLabel('other', 'short'), entries: buckets.other });
    }
    return groups;
  }

  function renderScaleClusters(entries, chipFn, chipExtra) {
    return clusterScaleEntries(entries).map((g) =>
      '<div class="cr-scale-group">' +
        '<div class="cr-scale-group-label">' + g.label + '</div>' +
        '<div class="cr-scales-row">' + g.entries.map((e) => chipFn(e, chipExtra)).join('') + '</div>' +
      '</div>'
    ).join('');
  }

  function applySharedScale(rootPc, family, modeIndex) {
    if (!M.SCALES[family]) return;
    const maxMode = (M.SCALES[family].modes || []).length - 1;
    setExploreRoot(rootPc);
    state.scaleFamily = family;
    state.modeIndex = Math.max(0, Math.min(maxMode < 0 ? 0 : maxMode, modeIndex | 0));
    const cat = catForFamily(family);
    if (cat) state.scaleCategory = cat;
    populateSelects();
    saveState();
    render();
  }

  function allScaleKeys() {
    const out = [];
    M.SCALE_CATS.forEach((cat) => cat.families.forEach((fk) => out.push(fk)));
    return out;
  }

  const STORE_KEY = 'fretboard-lab-v1';

  function serializeState() {
    return {
      tab: state.tab,
      stringCount: state.stringCount,
      tuningId: state.tuningId,
      customPcs: state.customPcs,
      fretCount: state.fretCount,
      flats: state.flats,
      sound: state.sound,
      showNames: state.showNames,
      showDegrees: state.showDegrees,
      showIntervals: state.showIntervals,
      compoundIntervals: state.compoundIntervals,
      fullChart: state.fullChart,
      cagedKey: state.cagedKey,
      shapes: state.shapes,
      showCagedShapes: state.showCagedShapes,
      showChordTones: state.showChordTones,
      showRootLines: state.showRootLines,
      scaleKey: state.scaleKey,
      scaleFamily: state.scaleFamily,
      scaleCategory: state.scaleCategory,
      modeIndex: state.modeIndex,
      bpm: state.bpm,
      groove: state.groove,
      chordNotes: state.chordNotes.map((nt) => ({ s: nt.s, f: nt.f })),
      pinRoot: state.pinRoot ? { s: state.pinRoot.s, f: state.pinRoot.f } : null,
      showIntervalGhost: state.showIntervalGhost,
      profiles: state.profiles,
      profileSeq: state.profileSeq,
      activeProfileId: state.activeProfileId
    };
  }

  function saveState() {
    try {
      localStorage.setItem(STORE_KEY, JSON.stringify(serializeState()));
      return true;
    } catch (e) { return false; }
  }

  function loadState() {
    let s;
    try {
      s = JSON.parse(localStorage.getItem(STORE_KEY));
    } catch (e) { s = null; }
    if (!s) return;
    sanitizeState(s);
  }

  function sanitizeState(s) {
    s = s && typeof s === 'object' ? s : {};
    let tab = s.tab;
    if (tab === 'caged') {
      tab = 'explore';
      if (s.showCagedShapes == null) s.showCagedShapes = true;
    } else if (tab === 'scales') {
      tab = 'explore';
      if (s.showCagedShapes == null) s.showCagedShapes = false;
    }
    state.tab = ['explore', 'chords', 'profiles'].indexOf(tab) >= 0 ? tab : 'explore';
    state.stringCount = [4, 5, 6, 7, 8, 9, 10].indexOf(s.stringCount) >= 0 ? s.stringCount : state.stringCount;
    state.fretCount = [12, 15, 22, 24].indexOf(s.fretCount) >= 0 ? s.fretCount : state.fretCount;
    if (Array.isArray(s.chordNotes)) {
      state.chordNotes = s.chordNotes
        .filter((nt) => nt && Number.isFinite(nt.s) && Number.isFinite(nt.f))
        .map((nt) => ({
          s: Math.max(0, Math.min(state.stringCount - 1, nt.s | 0)),
          f: Math.max(0, Math.min(state.fretCount, nt.f | 0)),
          id: ++noteSeq
        }));
      state.pinRoot = (s.pinRoot && Number.isFinite(s.pinRoot.s) && Number.isFinite(s.pinRoot.f))
        ? { s: Math.max(0, Math.min(state.stringCount - 1, s.pinRoot.s | 0)), f: Math.max(0, Math.min(state.fretCount, s.pinRoot.f | 0)) }
        : null;
      if (state.pinRoot && !state.chordNotes.some((nt) => nt.s === state.pinRoot.s && nt.f === state.pinRoot.f)) {
        state.pinRoot = null;
      }
    } else {
      state.pinRoot = null;
    }
    state.showIntervalGhost = !!s.showIntervalGhost;
    state.flats = !!s.flats;
    state.sound = s.sound !== false;
    state.showNames = s.showNames !== false;
    if (s.showDegrees != null) {
      state.showDegrees = !!s.showDegrees;
      state.showIntervals = !!s.showIntervals;
    } else if (s.showSteps != null) {
      state.showDegrees = !!s.showSteps;
      state.showIntervals = !!s.showIntervals;
    } else {
      state.showDegrees = s.showIntervals !== false;
      state.showIntervals = false;
    }
    state.fullChart = !!s.fullChart;
    state.compoundIntervals = s.compoundIntervals !== false;
    state.cagedKey = Math.max(0, Math.min(11, s.cagedKey | 0));
    if (s.shapes && typeof s.shapes === 'object') {
      Object.keys(state.shapes).forEach((k) => { state.shapes[k] = s.shapes[k] !== false; });
    }
    state.showCagedShapes = s.showCagedShapes !== false;
    state.showChordTones = s.showChordTones !== false;
    state.showRootLines = s.showRootLines !== false;
    state.scaleKey = Math.max(0, Math.min(11, (s.scaleKey != null ? s.scaleKey : s.cagedKey) | 0));
    state.cagedKey = state.scaleKey;
    if (M.SCALES[s.scaleFamily]) state.scaleFamily = s.scaleFamily;
    state.scaleCategory = catForFamily(state.scaleFamily) || 'church';
    const maxMode = (M.SCALES[state.scaleFamily].modes || []).length - 1;
    state.modeIndex = Math.max(0, Math.min(maxMode, s.modeIndex | 0));
    state.bpm = Math.max(40, Math.min(800, s.bpm | 0 || 120));
    if (['downsideUp', 'upsideDown', 'bothFromDown', 'bothFromUp', 'lowestRoot', 'upperRoot'].indexOf(s.groove) >= 0) {
      state.groove = s.groove;
    }
    if (Array.isArray(s.customPcs) && s.customPcs.length) {
      state.customPcs = s.customPcs.map((v) => (((v % 12) + 12) % 12));
    }
    state.tuningId = s.tuningId || state.tuningId;
    if (state.tuningId !== 'custom') {
      const t = M.TUNINGS.find((x) => x.id === state.tuningId);
      if (!t || t.strings !== state.stringCount) {
        const fb = M.TUNINGS.find((x) => x.strings === state.stringCount && x.id !== 'custom');
        state.tuningId = fb ? fb.id : 'std6';
      }
    } else {
      while (state.customPcs.length < state.stringCount) state.customPcs.unshift(7);
      while (state.customPcs.length > state.stringCount) state.customPcs.shift();
    }
    state.profiles = [];
    state.profileSeq = 0;
    if (Array.isArray(s.profiles)) {
      state.profiles = s.profiles
        .filter((p) => p && typeof p === 'object')
        .map((p, i) => ({
          id: (p.id | 0) || (Date.now() + i),
          name: String(p.name || '').slice(0, 60),
          construction: window.FretProfiles.CONSTRUCTION_OPTS.some((c) => c.v === p.construction) ? p.construction : 'electric',
          role: window.FretProfiles.ROLE_OPTS.some((r) => r.v === p.role) ? p.role : 'guitar',
          scaleLen: Math.max(20, Math.min(40, parseFloat(p.scaleLen) || window.FretProfiles.ROLE_DEFAULTS.guitar.scaleLen)),
          fretCount: Math.max(15, Math.min(24, parseInt(p.fretCount, 10) || window.FretProfiles.ROLE_DEFAULTS[p.role].fretCount)),
          tuning: (Array.isArray(p.tuning) ? p.tuning : [])
            .filter((t) => t && Number.isFinite(t.n) && Number.isFinite(t.o))
            .slice(0, 10)
            .map((t) => ({ n: ((t.n % 12) + 12) % 12, o: Math.max(0, Math.min(6, t.o | 0)) })),
          photo: typeof p.photo === 'string' && p.photo.length < 1500000 ? p.photo : '',
          desc: String(p.desc || '').slice(0, 255)
        }));
      const maxId = state.profiles.reduce((m, p) => Math.max(m, p.id), 0);
      state.profileSeq = Math.max(s.profileSeq | 0, maxId);
      state.activeProfileId = s.activeProfileId && state.profiles.some((p) => p.id === s.activeProfileId) ? s.activeProfileId : null;
      state.profiles.forEach((p) => {
        if (!p.tuning.length) p.tuning = window.FretProfiles.ROLE_DEFAULTS[p.role].tuning.map((m) => ({ n: m % 12, o: Math.floor(m / 12) - 1 }));
      });
    }
  }

  function defaultGuitarSub() {
    const tun = M.TUNINGS.find((x) => x.id === DEFAULT_TUNING_ID);
    return (tun ? tun.label : '') + ' · ' + t('app.guitarChip.fretsSuffix', { count: DEFAULT_FRET_COUNT });
  }

  function profileTuningSub(p) {
    return p.tuning.map((tn) => names()[tn.n] + tn.o).join(' ') + ' · ' + t('app.guitarChip.fretsSuffix', { count: p.fretCount });
  }

  function guitarThumbHtml(photo, cls) {
    const src = photo || window.FretProfiles.DEFAULT_GUITAR_PHOTO;
    return '<img class="' + cls + (photo ? '' : ' guitar-thumb-default') + '" src="' + src + '" alt="">';
  }

  function guitarMenuItem(opts) {
    return '<button type="button" class="guitar-menu-item' + (opts.on ? ' on' : '') + '" data-pid="' + esc(opts.pid) + '" role="option" aria-selected="' + (opts.on ? 'true' : 'false') + '">' +
      guitarThumbHtml(opts.photo, 'guitar-menu-thumb') +
      '<span class="guitar-menu-text">' +
      '<span class="guitar-menu-name">' + esc(opts.name) + '</span>' +
      '<span class="guitar-menu-sub">' + esc(opts.sub) + '</span>' +
      (opts.meta ? '<span class="guitar-menu-meta">' + esc(opts.meta) + '</span>' : '') +
      '</span>' +
      '<span class="guitar-menu-check" aria-hidden="true">✓</span>' +
      '</button>';
  }

  function renderGuitarChip() {
    const el = $('#ctl-guitar');
    if (!el) return;
    const wasOpen = el.open;
    const p = activeProfile();
    let name, sub, photo;
    if (p) {
      el.classList.add('on');
      name = p.name;
      sub = profileTuningSub(p);
      photo = p.photo || '';
    } else {
      el.classList.remove('on');
      name = t('app.guitarChip.defaultName');
      sub = defaultGuitarSub();
      photo = '';
    }
    const summaryThumb = guitarThumbHtml(photo, 'guitar-chip-thumb');
    const items = [];
    items.push(guitarMenuItem({
      pid: '',
      on: !p,
      name: t('app.guitarChip.defaultName'),
      sub: defaultGuitarSub(),
      meta: t('app.guitarChip.standardHint'),
      photo: ''
    }));
    state.profiles.forEach((pr) => {
      items.push(guitarMenuItem({
        pid: String(pr.id),
        on: !!(p && p.id === pr.id),
        name: pr.name,
        sub: profileTuningSub(pr),
        meta: t('profiles.construction.' + pr.construction) + ' · ' + t('profiles.roles.' + pr.role),
        photo: pr.photo || ''
      }));
    });
    const emptyHint = state.profiles.length
      ? ''
      : '<div class="guitar-menu-hint">' + esc(t('app.guitarChip.emptyProfiles')) + '</div>';
    el.innerHTML =
      '<summary class="guitar-chip' + (p ? ' on' : '') + '" aria-label="' + esc(t('app.guitarChip.aria')) + '">' +
      summaryThumb +
      '<span class="guitar-chip-text">' +
      '<span class="guitar-chip-name">' + esc(name) + '</span>' +
      '<span class="guitar-chip-sub">' + esc(sub) + '</span>' +
      '</span>' +
      '<span class="guitar-chip-caret" aria-hidden="true">▾</span>' +
      '</summary>' +
      '<div class="guitar-menu" role="listbox" aria-label="' + esc(t('app.guitarChip.aria')) + '">' +
      items.join('') +
      emptyHint +
      '<button type="button" class="guitar-menu-action" data-guitar-manage="1">' + esc(t('app.guitarChip.manage')) + '</button>' +
      '</div>';
    if (wasOpen) el.open = true;
  }

  function bindGuitarPicker() {
    const el = $('#ctl-guitar');
    if (!el || el.dataset.bound === '1') return;
    el.dataset.bound = '1';
    el.addEventListener('click', (e) => {
      const manage = e.target.closest('[data-guitar-manage]');
      if (manage && el.contains(manage)) {
        e.preventDefault();
        el.open = false;
        state.tab = 'profiles';
        applyTabUI();
        saveState();
        render();
        return;
      }
      const item = e.target.closest('.guitar-menu-item');
      if (!item || !el.contains(item)) return;
      e.preventDefault();
      const raw = item.getAttribute('data-pid');
      const next = raw === '' || raw == null ? null : parseInt(raw, 10);
      if (next != null && (isNaN(next) || !state.profiles.some((pr) => pr.id === next))) return;
      if (state.activeProfileId === next) {
        el.open = false;
        return;
      }
      state.activeProfileId = next;
      el.open = false;
      saveState();
      render();
    });
    document.addEventListener('click', (e) => {
      if (el.open && !el.contains(e.target)) el.open = false;
    });
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && el.open) el.open = false;
    });
  }

  function syncControls() {
    renderGuitarChip();
    const langSel = $('#ctl-lang');
    if (langSel) langSel.value = window.FretI18n.lang();
    $('#ctl-flats').value = state.flats ? '1' : '0';
    $('#ctl-names').checked = state.showNames;
    $('#ctl-degrees').checked = state.showDegrees;
    $('#ctl-intervals').checked = state.showIntervals;
    $('#ctl-full').checked = state.fullChart;
    $('#ctl-sound').checked = state.sound;
    $('#ctl-chordtones').checked = state.showChordTones;
    $('#ctl-rootlines').checked = state.showRootLines;
    $('#ctl-caged-layer').checked = state.showCagedShapes;
    $('#ctl-compound').value = state.compoundIntervals ? '1' : '0';
    $('#ctl-bpm').value = String(state.bpm);
    $('#ctl-groove').value = state.groove;
  }

  function applyTabUI() {
    document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t.dataset.tab === state.tab));
    $('#ctl-main-controls').classList.toggle('hidden', state.tab === 'profiles');
    $('#ctl-explore').classList.toggle('hidden', state.tab !== 'explore');
    $('#ctl-chords').classList.toggle('hidden', state.tab !== 'chords');
    $('#ctl-playback').classList.toggle('hidden', state.tab === 'profiles');
    $('#ctl-chord-opts').classList.toggle('hidden', state.tab !== 'chords');
    $('#ctl-caged-shapes').classList.toggle('hidden', state.tab !== 'explore');
    document.querySelectorAll('.ctl-shapes-layer').forEach((el) => {
      el.classList.toggle('hidden', !state.showCagedShapes);
    });
    $('#ctl-playback-controls').classList.toggle('hidden', !state.sound || state.tab === 'profiles');
    $('#board-wrap').classList.toggle('hidden', state.tab === 'profiles');
    $('#legend').classList.toggle('hidden', state.tab === 'profiles');
    $('#tipbox').classList.toggle('hidden', state.tab === 'profiles');
    if (state.tab === 'profiles') $('#scale-details').classList.add('hidden');
    else if (state.tab !== 'chords') $('#scale-details').classList.remove('hidden');
    $('#chord-result').classList.toggle('hidden', state.tab !== 'chords');
    $('#profiles-panel').classList.toggle('hidden', state.tab !== 'profiles');
  }

  const x = (i) => G.x(i, state.fretCount);
  const bandCenter = (i) => G.bandCenter(i, state.fretCount);
  const leftBound = (i) => G.leftBound(i, state.fretCount);
  const rightBound = (i) => G.rightBound(i, state.fretCount);
  const hexA = G.hexA;
  const yArea = () => G.yArea(state.stringCount);
  const yPx = G.yPx;
  const fretFromX = (pct) => G.fretFromX(pct, state.fretCount);

  function positionTip(x, y) {
    const r = tip.getBoundingClientRect();
    const maxX = Math.max(8, window.innerWidth - r.width - 8);
    const maxY = Math.max(8, window.innerHeight - r.height - 8);
    tip.style.left = Math.max(8, Math.min(x + 14, maxX)) + 'px';
    tip.style.top = Math.max(8, Math.min(y + 20, maxY)) + 'px';
  }

  function showTip(html, x, y) {
    tip.innerHTML = html;
    tip.style.display = 'block';
    positionTip(x, y);
  }

  function tipRow(key, val, valCls) {
    return '<div class="tip-row"><span class="tip-k">' + key + '</span><span class="tip-v' + (valCls ? ' ' + valCls : '') + '">' + val + '</span></div>';
  }

  function tipHtml(label) {
    const dash = t('tip.emDash');
    const deg = label.tipDegree || dash;
    const iv = label.tipIntervalHtml || label.tipInterval || dash;
    return tipRow(t('tip.note'), label.octaveName || label.name || dash) +
      tipRow(t('tip.fret'), label.fretText || dash) +
      tipRow(t('tip.degree'), deg, label.root ? 'tr' : '') +
      tipRow(t('tip.interval'), iv, 'ti');
  }

  function tipQuality(r) {
    if (r.quality) return r.quality;
    const dash = t('tip.emDash');
    if (!r.matched || !r.c) return dash;
    if (r.c.dim || r.c.dim7 || r.c.aug || r.c.sus) return dash;
    if (r.c.third === 4) return t('tip.qualityMajor');
    return dash;
  }

  function chordSymbolTipHtml(r) {
    const dash = t('tip.emDash');
    const alts = (r.alterations || []).join('');
    const paren = r.parens && r.parens.length ? '(' + r.parens.join(',') + ')' : '';
    const alteration = (alts + paren) || dash;
    return tipRow(t('tip.root'), r.rootName || dash, 'tr') +
      tipRow(t('tip.quality'), tipQuality(r)) +
      tipRow(t('tip.extension'), r.extension || dash) +
      tipRow(t('tip.alteration'), alteration) +
      tipRow(t('tip.bass'), r.slash && r.bassName ? r.bassName : dash);
  }

  function hideTip() {
    tip.style.display = 'none';
  }

  function positionCtx(x, y) {
    const r = ctxMenu.getBoundingClientRect();
    const maxX = Math.max(8, window.innerWidth - r.width - 8);
    const maxY = Math.max(8, window.innerHeight - r.height - 8);
    ctxMenu.style.left = Math.max(8, Math.min(x, maxX)) + 'px';
    ctxMenu.style.top = Math.max(8, Math.min(y, maxY)) + 'px';
  }

  function closeCtx() {
    ctxMenu.style.display = 'none';
  }

  function openChordMenu(x, y, nt) {
    const pinned = state.pinRoot && state.pinRoot.s === nt.s && state.pinRoot.f === nt.f;
    ctxMenu.innerHTML =
      '<button type="button" class="ctx-item" data-act="pin">' +
      '<span class="ctx-check">' + (pinned ? '\u2726' : '') + '</span>' +
      '<span>' + (pinned ? t('ctx.unpinRoot') : t('ctx.pinAsRoot')) + '</span>' +
      '</button>' +
      '<button type="button" class="ctx-item" data-act="iv">' +
      '<span class="ctx-check">' + (state.showIntervalGhost ? '\u2713' : '') + '</span>' +
      '<span>' + t('ctx.relativeDegreesIntervals') + '</span>' +
      '</button>';
    ctxMenu.dataset.s = nt.s;
    ctxMenu.dataset.f = nt.f;
    ctxMenu.style.display = 'block';
    positionCtx(x, y);
  }

  ctxMenu.addEventListener('click', (e) => {
    const btn = e.target.closest ? e.target.closest('.ctx-item') : null;
    if (!btn) return;
    const s = parseInt(ctxMenu.dataset.s, 10);
    const f = parseInt(ctxMenu.dataset.f, 10);
    if (btn.dataset.act === 'pin') {
      if (state.pinRoot && state.pinRoot.s === s && state.pinRoot.f === f) state.pinRoot = null;
      else state.pinRoot = { s: s, f: f };
      render();
    } else if (btn.dataset.act === 'iv') {
      state.showIntervalGhost = !state.showIntervalGhost;
      render();
    }
    closeCtx();
  });

  function relText(semis, degreeOverride, join) {
    return M.formatRel(semis, false, state.showIntervals, degreeOverride, join);
  }

  function makeMarker(pc, fret, stringIdx, midis, label, cls, color, colorR) {
    const m = document.createElement('div');
    m.className = 'marker ' + cls;
    if (color) {
      m.style.setProperty('--mc', color);
      m.style.setProperty('--mcL', color);
      if (cls.indexOf('root') < 0) m.classList.add('shaped');
    }
    if (colorR) m.style.setProperty('--mcR', colorR);
    const left = bandCenter(fret);
    m.style.left = left + '%';
    m.style.top = label.top + 'px';
    m.style.zIndex = 10;
    m.dataset.sf = stringIdx + ':' + fret;

    const nameTxt = label.name;
    const degTxt = label.degree;
    const intTxt = label.interval;

    m.innerHTML =
      '<span class="mtop">' +
      (nameTxt ? '<span class="mn">' + nameTxt + '</span>' : '') +
      (degTxt ? '<span class="ms">' + degTxt + '</span>' : '') +
      '</span>' +
      (intTxt ? '<span class="ml"></span><span class="mi">' + intTxt + '</span>' : '');

    const midi = midis[stringIdx] + fret;
    m.addEventListener('click', (e) => {
      e.stopPropagation();
      if (state.tab === 'chords') {
        const i = state.chordNotes.findIndex((nt) => nt.s === stringIdx && nt.f === fret);
        if (i >= 0) state.chordNotes.splice(i, 1);
        if (state.pinRoot && state.pinRoot.s === stringIdx && state.pinRoot.f === fret) state.pinRoot = null;
        render();
        return;
      }
      playMidi(midi);
    });
    m.addEventListener('mouseenter', (e) => {
      showTip(tipHtml(label), e.clientX, e.clientY);
    });
    m.addEventListener('mouseleave', hideTip);
    neck.appendChild(m);
    return m;
  }

  function pinRootPc(midis) {
    if (!state.pinRoot) return null;
    const m = midis[state.pinRoot.s];
    if (m == null) return null;
    return (((m + state.pinRoot.f) % 12) + 12) % 12;
  }

  function gridAt(pct, y) {
    const area = yArea();
    const midis = tuningMidis();
    const s = Math.max(0, Math.min(midis.length - 1, Math.round(midis.length - 0.5 - (y - area.topPad) / area.step)));
    const f = fretFromX(pct);
    return { s: s, f: f, area: area };
  }

  function makeGhostPlus() {
    ghostPlus = document.createElement('div');
    ghostPlus.className = 'marker ghost-plus';
    ghostPlus.innerHTML = '<span class="place-plus">+</span>';
    ghostPlus.style.display = 'none';
    neck.appendChild(ghostPlus);
  }

  function placeGhostPlus(pct, y) {
    if (!ghostPlus) return;
    const g = gridAt(pct, y);
    const occupied = state.chordNotes.some((nt) => nt.s === g.s && nt.f === g.f);
    if (occupied) {
      ghostPlus.style.display = 'none';
      return;
    }
    ghostPlus.style.left = bandCenter(g.f) + '%';
    ghostPlus.style.top = yPx(g.s, g.area) + 'px';
    ghostPlus.style.display = 'block';
  }

  function makeChordNote(nt, midis, area) {
    const midi = midis[nt.s] + nt.f;
    const pc = ((midi % 12) + 12) % 12;
    const rootPc = chordResult ? chordResult.rootPc : -1;
    const isRoot = pc === rootPc;
    const isPinned = state.pinRoot && state.pinRoot.s === nt.s && state.pinRoot.f === nt.f;
    const rel = rootPc >= 0 ? (pc - rootPc + 12) % 12 : -1;
    const cdesc = chordResult && chordResult.matched ? chordResult.c : null;
    const role = rel >= 0 ? M.chordRoleText(rel, cdesc, state.compoundIntervals) : '';
    const ivAbbr = rel >= 0 ? M.chordIntervalText(rel, cdesc, state.compoundIntervals) : '';
    const lbl = {
      name: state.showNames ? names()[pc] : '',
      degree: rel >= 0 && state.showDegrees ? degreeRoman(rel) : '',
      interval: rel >= 0 && state.showIntervals ? ivAbbr : '',
      tipDegree: rel >= 0 ? (role || degreeRoman(rel)) : '',
      tipInterval: ivAbbr,
      tipIntervalHtml: rel >= 0 ? M.chordIntervalFull(rel, cdesc, state.compoundIntervals) : '',
      octaveName: midiLabel(midi),
      fretText: nt.f === 0 ? t('tip.fretOpen') : String(nt.f),
      root: isRoot,
      top: yPx(nt.s, area)
    };
    const m = makeMarker(pc, nt.f, nt.s, midis, lbl, isRoot ? 'root' : 'scale', null);
    if (isPinned) {
      const star = document.createElement('span');
      star.className = 'pin-star';
      star.textContent = '*';
      m.appendChild(star);
    }
    m.addEventListener('contextmenu', (e) => {
      e.preventDefault();
      e.stopPropagation();
      openChordMenu(e.clientX, e.clientY, nt);
    });
  }

  function renderScaleGhost(midis, area) {
    if (!scaleSel || !scaleSel.ivs) return;
    const N = state.fretCount;
    const rootPc = scaleSel.rootPc;
    const ivs = scaleSel.ivs;
    const abs = new Set(ivs.map((v) => (rootPc + v) % 12));
    midis.forEach((base, s) => {
      for (let f = 0; f <= N; f++) {
        const pc = (((base + f) % 12) + 12) % 12;
        const rel = (pc - rootPc + 12) % 12;
        const inScale = abs.has(pc);
        const name = state.showNames ? names()[pc] : '';
        const degree = state.showDegrees ? degreeRoman(rel) : '';
        const interval = state.showIntervals ? relText(rel) : '';
        if (!inScale && !name && !degree && !interval) continue;
        const m = document.createElement('div');
        m.className = 'marker ' + (inScale ? (pc === rootPc ? 'root' : 'scale') : 'plain') + ' soft ghost-scale';
        m.style.left = bandCenter(f) + '%';
        m.style.top = yPx(s, area) + 'px';
        m.style.zIndex = 3;
        m.style.pointerEvents = 'none';
        m.style.cursor = 'default';
        m.innerHTML =
          '<span class="mtop">' +
          (name ? '<span class="mn">' + name + '</span>' : '') +
          (degree ? '<span class="ms">' + degree + '</span>' : '') +
          '</span>' +
          (interval ? '<span class="ml"></span><span class="mi">' + interval + '</span>' : '');
        neck.appendChild(m);
      }
    });
  }

  function renderIntervalGhost(midis, area) {
    if (!state.showIntervalGhost || (!state.showDegrees && !state.showIntervals)) return;
    const rootPc = chordResult ? chordResult.rootPc : null;
    if (rootPc == null) return;
    const N = state.fretCount;
    midis.forEach((base, s) => {
      for (let f = 0; f <= N; f++) {
        const pc = (((base + f) % 12) + 12) % 12;
        const rel = (pc - rootPc + 12) % 12;
        const degree = state.showDegrees ? degreeRoman(rel) : '';
        const interval = relText(rel);
        if (!degree && !interval) continue;
        const m = document.createElement('div');
        m.className = 'marker plain soft ghost-interval';
        m.style.left = bandCenter(f) + '%';
        m.style.top = yPx(s, area) + 'px';
        m.style.zIndex = 2;
        m.style.pointerEvents = 'none';
        m.style.cursor = 'default';
        m.innerHTML =
          '<span class="mtop">' +
          (degree ? '<span class="ms">' + degree + '</span>' : '') +
          '</span>' +
          (interval ? '<span class="ml"></span><span class="mi">' + interval + '</span>' : '');
        neck.appendChild(m);
      }
    });
  }

  function renderChordNotes(midis, area) {
    const n = midis.length;
    const N = state.fretCount;
    const valid = state.chordNotes.filter((nt) => nt.s >= 0 && nt.s < n && nt.f >= 0 && nt.f <= N);
    chordResult = M.analyzeChord(valid.map((nt) => midis[nt.s] + nt.f), pinRootPc(midis), { names: names(), compoundIntervals: state.compoundIntervals });
    if (chordResult && chordResult.matched && scalePropFollow) scalePropKey = chordResult.rootPc;
    if (!chordResult || !chordResult.matched) {
      scaleSel = null;
      chordScaleFocus = null;
    } else if (scaleSel) {
      const keyPc = proposalKeyPc(chordResult);
      const sc = M.chordScales(chordResult.rootPc, chordResult.intervals, chordScaleOpts(chordResult, keyPc));
      const ok = scaleSel.rootPc === keyPc && sc.full.concat(sc.partial).some((e) => e.name === scaleSel.name && e.family === scaleSel.family);
      if (!ok) scaleSel = null;
    }
    const cnt = $('#chord-count');
    if (cnt) cnt.textContent = t('controls.chords.noteCount', { count: valid.length });
    valid.forEach((nt) => {
      makeChordNote(nt, midis, area);
    });
    makeGhostPlus();
  }

  function playChord() {
    if (!state.chordNotes.length) return;
    const midis = tuningMidis();
    const notes = state.chordNotes
      .filter((nt) => nt.s >= 0 && nt.s < midis.length && nt.f >= 0 && nt.f <= state.fretCount)
      .map((nt) => ({ s: nt.s, f: nt.f, m: midis[nt.s] + nt.f }));
    playSequence(notes, chordResult ? chordResult.rootPc : null);
  }

  function drawBoard() {
    const area = yArea();
    const midis = tuningMidis();
    const n = midis.length;
    const N = state.fretCount;
    const nm = names();
    neck.style.height = area.height + 'px';
    neck.innerHTML = '';
    strLabels.innerHTML = '';
    numLabels.innerHTML = '';

    midis.forEach((m, i) => {
      const lbl = document.createElement('div');
      lbl.className = 'string-label';
      lbl.textContent = midiLabel(m);
      lbl.style.top = yPx(i, area) + 'px';
      lbl.addEventListener('click', () => playMidi(m));
      strLabels.appendChild(lbl);

      const num = document.createElement('div');
      num.className = 'string-num';
      num.textContent = n - i;
      num.style.top = yPx(i, area) + 'px';
      numLabels.appendChild(num);

      const line = document.createElement('div');
      line.className = 'string-line';
      const h = 1.5 + (n - 1 - i) * 0.55;
      line.style.height = h + 'px';
      line.style.top = (yPx(i, area) - h / 2) + 'px';
      const bronze = [205, 127, 50];
      const silver = [201, 206, 214];
      const tt = i / (n - 1);
      const col = bronze.map((v, k) => Math.round(v + (silver[k] - v) * tt));
      line.style.backgroundColor = 'rgb(' + col.join(',') + ')';
      if (i <= n - 3) {
        const t = n > 3 ? i / (n - 3) : 0;
        const period = 10 - (10 - 3.5) * t;
        line.style.backgroundImage = 'repeating-linear-gradient(45deg, var(--bg) 0 1.5px, transparent 1.5px ' + period + 'px)';
      }
      neck.appendChild(line);
    });

    for (let i = 0; i <= N; i++) {
      const fl = document.createElement('div');
      fl.className = i === 0 ? 'fret-line nut' : 'fret-line';
      fl.style.left = x(i) + '%';
      fl.style.top = area.topPad + 'px';
      fl.style.height = n * area.step + 'px';
      neck.appendChild(fl);
    }

    INLAY_FRETS.forEach((f) => {
      if (f > N) return;
      const double = f === 12 || f === 24;
      const d = document.createElement('div');
      d.className = 'inlay' + (double ? ' double' : '');
      d.style.left = bandCenter(f) + '%';
      d.style.top = (area.topPad + (n + 0.75) * area.step) + 'px';
      neck.appendChild(d);
    });

    for (let i = 0; i <= N; i++) {
      const fl = document.createElement('div');
      fl.className = 'fret-num';
      fl.textContent = i;
      fl.style.left = bandCenter(i) + '%';
      neck.appendChild(fl);
    }

    const data = computeData();
    const labelOpts = {
      rootPc: rootPc(),
      ordered: data.ordered,
      pcs: data.pcs,
      nm: nm,
      midis: midis,
      area: area
    };

    data.markers.forEach((mk) => {
      const lbl = markerLabels(mk, labelOpts);
      lbl.top = yPx(mk.stringIdx, area);
      const el = makeMarker(mk.pc, mk.fret, mk.stringIdx, midis, lbl, mk.cls, mk.color, mk.colorR);
      if (mk.shapes) el.dataset.shapes = mk.shapes.join(' ');
    });

    data.boxes.forEach((box) => {
      const b = document.createElement('div');
      b.className = 'caged-box';
      b.dataset.shape = box.key;
      if (freshShapes.has(box.key)) b.classList.add('box-fade-in');
      const l = leftBound(box.visLo), r = rightBound(box.visHi);
      const yTop = Math.min(yPx(box.minS, area), yPx(box.maxS, area));
      const yBot = Math.max(yPx(box.minS, area), yPx(box.maxS, area));
      b.style.left = l + '%';
      b.style.width = (r - l) + '%';
      b.style.top = (yTop - 15 - area.step / 2) + 'px';
      b.style.height = (yBot - yTop + 30 + area.step) + 'px';
      b.style.borderColor = box.color;
      b.style.background = hexA(box.color, 0.08);
      b.style.zIndex = 1;
      const top = document.createElement('div');
      top.className = 'box-top';
      const playBtn = document.createElement('div');
      playBtn.className = 'box-play';
      playBtn.innerHTML = '&#9654;';
      playBtn.style.background = box.color;
      playBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        playSequence(box.notes);
      });
      top.appendChild(playBtn);
      const lab = document.createElement('div');
      lab.className = 'box-label';
      lab.textContent = t('controls.shapes.boxLabel', { key: box.key });
      lab.style.background = box.color;
      top.appendChild(lab);
      b.appendChild(top);
      b.addEventListener('click', (e) => {
        e.stopPropagation();
        playMidi(box.rootMidi);
      });
      neck.appendChild(b);
    });

    if (state.showRootLines && data.boxes.length) {
      const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      svg.setAttribute('viewBox', '0 0 100 100');
      svg.setAttribute('preserveAspectRatio', 'none');
      svg.setAttribute('class', 'shape-lines');
      data.boxes.forEach((box) => {
        if (!box.roots || box.roots.length < 2) return;
        const pts = box.roots
          .slice()
          .sort((a, b) => a.s - b.s)
          .map((r) => bandCenter(r.f).toFixed(3) + ',' + ((yPx(r.s, area) / area.height) * 100).toFixed(3))
          .join(' ');
        const poly = document.createElementNS('http://www.w3.org/2000/svg', 'polyline');
        poly.setAttribute('points', pts);
        poly.setAttribute('stroke', box.color);
        poly.setAttribute('stroke-width', '1.5');
        poly.setAttribute('vector-effect', 'non-scaling-stroke');
        poly.setAttribute('fill', 'none');
        poly.setAttribute('stroke-linecap', 'round');
        svg.appendChild(poly);
      });
      if (svg.childNodes.length) neck.appendChild(svg);
    }

    if (state.tab === 'chords') {
      renderChordNotes(midis, area);
      renderIntervalGhost(midis, area);
      renderScaleGhost(midis, area);
    }
  }

  function markerLabels(mk, o) {
    const rootPc = o.rootPc;
    const intIdx = (mk.pc - rootPc + 12) % 12;
    const inScale = o.pcs.has(mk.pc);
    let degree = '';
    let interval = '';
    let name = '';
    const midiVal = o.midis[mk.stringIdx] + mk.fret;
    if (state.showNames) name = o.nm[mk.pc];
    const octaveName = midiLabel(midiVal);
    if (state.showDegrees) degree = degreeRoman(intIdx);
    interval = relText(intIdx);
    return {
      name: name,
      degree: degree,
      interval: interval,
      tipDegree: degreeRoman(intIdx),
      tipInterval: M.QUALITIES[intIdx],
      tipIntervalHtml: M.qualityFull(intIdx),
      octaveName: octaveName,
      fretText: mk.fret === 0 ? t('tip.fretOpen') : String(mk.fret),
      root: mk.pc === rootPc && inScale
    };
  }

  function computeData() {
    return window.FretCaged.computeData({
      M: M,
      midis: tuningMidis(),
      fretCount: state.fretCount,
      root: rootPc(),
      tab: state.tab,
      showCagedShapes: state.tab === 'explore' && state.showCagedShapes,
      scaleFamily: state.scaleFamily,
      modeIndex: state.modeIndex,
      shapes: state.shapes,
      showChordTones: state.showChordTones,
      fullChart: state.fullChart
    });
  }

  function render() {
    hideTip();
    syncBoardSource();
    renderGuitarChip();
    if (state.tab === 'profiles') {
      profiles.renderProfiles();
      saveState();
      return;
    }
    drawBoard();
    renderLegend();
    renderTipbox();
    renderChordResult();
    renderScaleDetails();
    saveState();
    freshShapes.clear();
  }

  function renderChordResult() {
    const el = $('#chord-result');
    if (!chordResult) {
      el.innerHTML = '<div class="cr-empty">' + t('chords.empty') + '</div>';
      return;
    }
    const r = chordResult;
    const chips = r.notes.map((nt) => {
      const iv = (nt.pc - r.rootPc + 12) % 12;
      const inChord = r.matched && r.intervals.indexOf(iv) >= 0;
      const cls = nt.pc === r.rootPc ? 'cr-root' : (inChord ? 'cr-in' : 'cr-out');
      const role = r.labelFor ? (r.labelFor[nt.pc] || M.DEGREES[iv]) : M.DEGREES[iv];
      const parts = [];
      if (state.showDegrees) parts.push(degreeRoman(iv));
      if (state.showIntervals) parts.push(M.chordIntervalText(iv, r.c, state.compoundIntervals));
      if (!parts.length) parts.push(role);
      return '<span class="cr-chip ' + cls + '"><b>' + nt.name + '</b><i>' + parts.join(' · ') + '</i></span>';
    }).join('');
    const raised =
      (r.extension ? '<span class="cr-extension">' + r.extension + '</span>' : '') +
      (r.alterations && r.alterations.length ? '<span class="cr-alteration">' + r.alterations.join('') + '</span>' : '') +
      (r.parens && r.parens.length ? '<span class="cr-alteration">(' + r.parens.join(',') + ')</span>' : '');
    let symbol =
      '<span class="cr-root">' + r.rootName + '</span>' +
      (r.quality ? '<span class="cr-quality">' + r.quality + '</span>' : '') +
      raised;
    let sub = r.name;
    if (r.matched) {
      if (r.slash) {
        symbol += '<span class="cr-bass">/' + r.bassName + '</span>';
        sub += t('chords.slashChord', { bass: r.bassName });
      } else {
        sub += t('chords.rootPosition');
      }
    }
    let altHtml = '';
    if (r.alternatives.length) {
      altHtml = '<div class="cr-alt">' + t('chords.alsoHeardAs') + r.alternatives.map((a) => a.rootName + a.suffix).join('  ·  ') + '</div>';
    }
    let scalesHtml = '';
    if (r.matched) {
      if (scalePropFollow) scalePropKey = r.rootPc;
      const keyPc = proposalKeyPc(r);
      const sc = M.chordScales(r.rootPc, r.intervals, chordScaleOpts(r, keyPc));
      const nm = names();
      const keyOpts = nm.map((nn, i) => '<option value="' + i + '"' + (i === keyPc ? ' selected' : '') + '>' + nn + '</option>').join('');
      const resetBtn = scalePropFollow ? '' : '<button type="button" class="cr-scale-key-reset" title="' + t('chords.useChordRoot') + '">↺</button>';
      const keyCtl =
        '<label class="cr-scale-key-wrap">' + t('chords.key') + ' ' +
        '<select class="cr-scale-key" aria-label="' + t('chords.scaleProposalKeyAria') + '">' + keyOpts + '</select>' +
        resetBtn +
        '</label>';
      const chip = (e, extra) => {
        const active = scaleSel && scaleSel.rootPc === keyPc && scaleSel.name === e.name ? ' active' : '';
        const pinned = state.scaleKey === keyPc && state.scaleFamily === e.family && state.modeIndex === e.modeIndex;
        const pcs = Array.from(e.pcs).sort((a, b) => a - b).join(',');
        const disp = scaleDisplayName(e.family, e.modeIndex);
        return '<button type="button" class="cr-scale-chip' + extra + active + (pinned ? ' cr-scale-wired' : '') + '" data-r="' + keyPc + '" data-n="' + e.name + '" data-pcs="' + pcs + '" data-fk="' + e.family + '" data-m="' + e.modeIndex + '">' +
          '<span class="cr-scale-label">' + sc.rootName + ' ' + disp + e.note + '</span>' +
          '<span class="cr-scale-pin' + (pinned ? ' on' : '') + '" data-pin="1" title="' + t('chords.useOnExplore') + '">✦</span>' +
          '</button>';
      };
      const fullBlock = sc.full.length
        ? '<div class="cr-scale-block"><div class="cr-scale-block-label">' + t('chords.fullMatch') + '</div>' + renderScaleClusters(sc.full, chip, '') + '</div>'
        : '';
      const partBlock = sc.partial.length
        ? '<div class="cr-scale-block cr-scale-block-partial"><div class="cr-scale-block-label">' + t('chords.partial') + '</div>' + renderScaleClusters(sc.partial, chip, ' cr-scale-partial') + '</div>'
        : '';
      const hint = scaleSel
        ? '<span class="cr-scale-hint">' + t('chords.ghostHint', { root: sc.rootName, scale: scaleDisplayName(scaleSel.family, scaleSel.modeIndex) }) + '</span>'
        : '';
      const empty = (!sc.full.length && !sc.partial.length)
        ? '<div class="cr-scales-empty">' + t('chords.noScales', { root: sc.rootName }) + '</div>'
        : '';
      scalesHtml =
        '<section class="cr-section cr-scales">' +
        '<div class="cr-scales-title">' +
          '<span class="cr-section-label">' + t('chords.scalesThatFit') + '</span>' +
          '<b>' + r.rootName + r.suffix + '</b>' +
          keyCtl +
          hint +
        '</div>' +
        '<p class="cr-scale-guide">' + t('chords.guide') + '</p>' +
        fullBlock +
        partBlock +
        empty +
        '</section>';
    }
    el.innerHTML =
      '<div class="cr-stack">' +
        '<section class="cr-section cr-symbol">' +
          '<div class="cr-head"><div class="cr-name">' + symbol + '</div><div class="cr-sub">' + sub + '</div></div>' +
          altHtml +
        '</section>' +
        '<section class="cr-section cr-tones">' +
          '<div class="cr-section-label">' + t('chords.notes') + '</div>' +
          '<div class="cr-chips">' + chips + '</div>' +
        '</section>' +
        scalesHtml +
      '</div>';
  }

  function renderLegend() {
    const el = $('#legend');
    if (state.tab === 'explore') {
      const desc = scaleDisplayName(state.scaleFamily, state.modeIndex);
      const layer = state.showCagedShapes ? t('legend.layerCaged') : t('legend.layerFullScale');
      el.innerHTML =
        '<h3>' + t('legend.exploreTitle', { root: names()[state.scaleKey], scale: desc, layer: layer }) + '</h3>' +
        '<div class="legend-items">' +
        legendItem(t('legend.swatchRoot'), 'gold', t('legend.swatchRootDesc')) +
        legendItem(t('legend.swatchChord'), '#7dd3fc', t('legend.swatchChordDesc')) +
        legendItem(t('legend.swatchScale'), '#3b6fd4', t('legend.swatchScaleDesc')) +
        (state.fullChart && !state.showCagedShapes ? legendItem(t('legend.swatchPlain'), 'rgba(255,255,255,.25)', t('legend.swatchPlainDesc')) : '') +
        '</div>' +
        (state.showCagedShapes
          ? '<p class="hint">' + t('legend.hintCaged') + '</p>'
          : '<p class="hint">' + t('legend.hintFullScale') + '</p>');
    } else if (state.tab === 'chords') {
      el.innerHTML =
        '<h3>' + t('legend.chordsTitle') + '</h3>' +
        '<div class="legend-items">' +
        '<span class="legend-item"><i class="swatch swatch-place"><span>+</span></i><b>' + t('legend.placeNote') + '</b> — ' + t('legend.placeNoteDesc') + '</span>' +
        '</div>' +
        '<p class="hint">' + t('legend.hintChords') + '</p>';
    }
  }

  function legendItem(text, color, desc) {
    return '<span class="legend-item"><i class="swatch" style="background:' + color + '"></i><b>' + text + '</b> — ' + desc + '</span>';
  }

  function renderTipbox() {
    const el = $('#tipbox');
    if (state.tab === 'explore') {
      const fam = M.SCALES[state.scaleFamily];
      const desc = scaleDisplayName(state.scaleFamily, state.modeIndex);
      const layer = state.showCagedShapes ? t('tipbox.layerCagedOn') : t('tipbox.layerFullScale');
      const modePart = fam.modes
        ? t('tipbox.exploreModePart', { n: state.modeIndex + 1, parentKey: state.scaleKeyName(), familyShort: scaleFamilyShort(state.scaleFamily) })
        : '';
      const body = t('tipbox.exploreBody', { root: names()[state.scaleKey], scale: desc, modePart: modePart, layer: layer });
      el.innerHTML = '<strong>' + t('tipbox.exploreStrong') + '</strong> — <span>' + body + '</span>';
    } else if (state.tab === 'chords') {
      el.innerHTML = '<strong>' + t('tipbox.chordsStrong') + '</strong> — <span>' + t('tipbox.chordsBody') + '</span>';
    }
  }

  function scaleWheelSvg(nm, root, ordered) {
    const scaleSet = new Set(ordered.map((v) => (root + v) % 12));
    const chordSet = new Set([ordered[0], ordered[2], ordered[4]].map((v) => (root + v) % 12));
    const cx = 100, cy = 100, R = 78;
    const parts = [];
    for (let pc = 0; pc < 12; pc++) {
      const ang = (pc * 30 - 90) * Math.PI / 180;
      const x = cx + R * Math.cos(ang);
      const y = cy + R * Math.sin(ang);
      let cls = 'w-off';
      if (pc === root) cls = 'w-root';
      else if (chordSet.has(pc)) cls = 'w-chord';
      else if (scaleSet.has(pc)) cls = 'w-scale';
      parts.push('<circle class="' + cls + '" cx="' + x.toFixed(2) + '" cy="' + y.toFixed(2) + '" r="15"/>');
      parts.push('<text x="' + x.toFixed(2) + '" y="' + (y + 4).toFixed(2) + '">' + nm[pc] + '</text>');
    }
    return '<svg viewBox="0 0 200 200" role="img" aria-label="' + t('scaleDetails.wheelAria') + '">' + parts.join('') + '</svg>';
  }

  function resolveScaleDetailsTarget() {
    if (state.tab === 'profiles') return null;
    if (state.tab === 'chords') {
      if (scaleSel && scaleSel.family && M.SCALES[scaleSel.family]) {
        return { root: scaleSel.rootPc, family: scaleSel.family, modeIndex: scaleSel.modeIndex | 0 };
      }
      if (chordScaleFocus && M.SCALES[chordScaleFocus.family]) {
        return { root: chordScaleFocus.rootPc, family: chordScaleFocus.family, modeIndex: chordScaleFocus.modeIndex | 0 };
      }
      return null;
    }
    return {
      root: state.scaleKey,
      family: state.scaleFamily,
      modeIndex: state.modeIndex
    };
  }

  function renderScaleDetails() {
    const el = $('#scale-details');
    const target = resolveScaleDetailsTarget();
    if (!target) {
      el.classList.add('hidden');
      el.innerHTML = '';
      return;
    }
    const fam = M.SCALES[target.family];
    if (!fam) {
      el.classList.add('hidden');
      el.innerHTML = '';
      return;
    }
    el.classList.remove('hidden');
    const nm = names();
    const root = target.root;
    const modeIndex = target.modeIndex;
    const ordered = M.scaleOrdered(target.family, modeIndex);
    const modeName = fam.modes ? fam.modes[modeIndex] : null;
    const displayName = scaleDisplayName(target.family, modeIndex);

    const theory = theoryInfo(modeName, target.family);
    const mood = theory ? theory.mood : null;
    const desc = theory ? theory.desc : '';

    const title = nm[root] + ' ' + displayName;
    let parent = '';
    if (modeName) {
      const baseOrdered = M.scaleOrdered(target.family, 0);
      const parentPc = (root + 12 - baseOrdered[0]) % 12;
      parent = t('scaleDetails.modeOf', { n: modeIndex + 1, parent: nm[parentPc] + ' ' + scaleFamilyShort(target.family) });
    }

    const len = ordered.length;
    const steps = [];
    for (let i = 0; i < len; i++) {
      const b = i + 1 < len ? ordered[i + 1] : ordered[0] + 12;
      steps.push(b - ordered[i]);
    }
    const stepHtml = steps.map((d) => {
      const lab = d === 1 ? t('scaleDetails.stepHalf') : d === 2 ? t('scaleDetails.stepWhole') : String(d);
      const cls = d === 1 ? 'sd-step H' : d === 2 ? 'sd-step W' : 'sd-step';
      return '<span class="' + cls + '">' + lab + '</span>';
    }).join('<span class="sd-step-join">·</span>');

    const degHtml = ordered.map((v, i) => {
      const pc = (root + v) % 12;
      const cls = i === 0 ? 'sd-root' : (i === 2 || i === 4 ? 'sd-chord' : 'sd-scale');
      return '<div class="sd-deg ' + cls + '">' +
        '<span class="sd-deg-num">' + degreeRoman(v) + '</span>' +
        '<span class="sd-deg-note">' + nm[pc] + '</span>' +
        '<span class="sd-deg-int">' + M.QUALITIES[v] + '</span>' +
        '</div>';
    }).join('');

    const formula = ordered.map((v) => FORMULA[v]).join(' · ');
    const charTxt = theory && theory.char ? theory.char : '';

    el.innerHTML =
      '<div class="sd-head"><h3>' + title + '</h3>' +
      (parent ? '<span class="sd-parent">' + parent + '</span>' : '') +
      '</div>' +
      '<div class="sd-body">' +
        '<div class="sd-wheel">' + scaleWheelSvg(nm, root, ordered) + '</div>' +
        '<div class="sd-main">' +
          (mood ? '<p class="sd-mood">' + mood.join('  ·  ') + '</p>' : '') +
          '<p class="sd-desc">' + desc + '</p>' +
          (charTxt ? '<p class="sd-char">' + t('scaleDetails.characteristic') + '<b>' + charTxt + '</b></p>' : '') +
          '<p class="sd-formula">' + formula + '</p>' +
          '<div class="sd-steps">' + stepHtml + '</div>' +
          '<div class="sd-degrees">' + degHtml + '</div>' +
        '</div>' +
      '</div>';
  }

  state.scaleKeyName = function () {
    const ordered = M.scaleOrdered(state.scaleFamily, 0);
    const keyPc = (state.scaleKey + 12 - ordered[0]) % 12;
    return names()[keyPc];
  };

  function populateSelects() {
    const nm = names();
    const rootSel = $('#ctl-explore-root');
    rootSel.innerHTML = nm.map((nn, i) => '<option value="' + i + '"' + (i === state.scaleKey ? ' selected' : '') + '>' + nn + '</option>').join('');
    $('#ctl-explore-family').innerHTML = familyOptions();
    renderScaleCats();
    populateModeSelect();
    populateShapeButtons();
  }

  function familyOptions() {
    const cat = M.SCALE_CATS.find((c) => c.id === state.scaleCategory);
    const keys = (cat && cat.families.indexOf(state.scaleFamily) >= 0) ? cat.families : allScaleKeys();
    return keys.map((k) => '<option value="' + k + '"' + (k === state.scaleFamily ? ' selected' : '') + '>' + scaleFamilyLabel(k) + '</option>').join('');
  }

  function renderScaleCats() {
    const wrap = $('#explore-cats');
    wrap.innerHTML = '';
    M.SCALE_CATS.forEach((cat) => {
      const b = document.createElement('button');
      b.className = 'cat-tab' + (cat.id === state.scaleCategory ? ' active' : '');
      b.dataset.cat = cat.id;
      b.textContent = scaleCatLabel(cat.id, 'label');
      b.addEventListener('click', () => {
        if (state.scaleCategory === cat.id) return;
        state.scaleCategory = cat.id;
        if (cat.families.indexOf(state.scaleFamily) < 0) {
          state.scaleFamily = cat.families[0];
          state.modeIndex = 0;
        }
        populateSelects();
        render();
      });
      wrap.appendChild(b);
    });
  }

  function populateModeSelect() {
    const fam = M.SCALES[state.scaleFamily];
    if (!fam) return;
    const sel = $('#ctl-explore-mode');
    if (!fam.modes) {
      sel.innerHTML = '<option>—</option>';
      sel.disabled = true;
      return;
    }
    sel.disabled = false;
    sel.innerHTML = fam.modes.map((_, i) => '<option value="' + i + '"' + (i === state.modeIndex ? ' selected' : '') + '>' + scaleModeLabel(state.scaleFamily, i) + '</option>').join('');
  }

  function populateShapeButtons() {
    const wrap = $('#shape-buttons');
    wrap.innerHTML = '';
    SHAPE_DEFS.forEach((def) => {
      const b = document.createElement('button');
      b.className = 'shape-btn' + (state.shapes[def.key] ? ' on' : '');
      b.style.setProperty('--sc', def.color);
      b.textContent = def.key;
      b.title = t('controls.shapes.shapeTitle', { key: def.key });
      b.addEventListener('click', () => {
        const turningOn = !state.shapes[def.key];
        state.shapes[def.key] = turningOn;
        populateShapeButtons();
        if (turningOn) {
          freshShapes.add(def.key);
          render();
        } else {
          const outEls = [];
          neck.querySelectorAll('.caged-box[data-shape="' + def.key + '"]').forEach((el) => outEls.push(el));
          neck.querySelectorAll('.marker[data-shapes~="' + def.key + '"]').forEach((el) => outEls.push(el));
          if (outEls.length) {
            const overlay = document.createElement('div');
            overlay.className = 'fade-overlay';
            outEls.forEach((el) => {
              const r = el.getBoundingClientRect();
              el.style.position = 'fixed';
              el.style.left = r.left + 'px';
              el.style.top = r.top + 'px';
              el.style.width = r.width + 'px';
              el.style.height = r.height + 'px';
              el.style.margin = '0';
              el.style.transform = 'none';
              el.classList.remove('box-fade-in');
              el.classList.add(el.classList.contains('caged-box') ? 'box-fade-out' : 'marker-fade-out');
              overlay.appendChild(el);
            });
            document.body.appendChild(overlay);
            setTimeout(() => { overlay.remove(); }, 220);
          }
          render();
        }
      });
      wrap.appendChild(b);
    });
  }

  function esc(s) {
    return String(s).replace(/[&<>"']/g, (ch) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[ch]));
  }

  function bindEvents() {
    $('.tabs').addEventListener('click', (e) => {
      const b = e.target.closest('.tab');
      if (!b) return;
      state.tab = b.dataset.tab;
      applyTabUI();
      render();
    });


    $('#ctl-names').addEventListener('change', (e) => { state.showNames = e.target.checked; render(); });
    $('#ctl-degrees').addEventListener('change', (e) => { state.showDegrees = e.target.checked; render(); });
    $('#ctl-intervals').addEventListener('change', (e) => { state.showIntervals = e.target.checked; render(); });
    $('#ctl-compound').addEventListener('change', (e) => { state.compoundIntervals = e.target.value === '1'; render(); });
    $('#ctl-full').addEventListener('change', (e) => { state.fullChart = e.target.checked; render(); });
    $('#ctl-sound').addEventListener('change', (e) => { state.sound = e.target.checked; saveState(); applyTabUI(); });

    $('#ctl-bpm').addEventListener('change', (e) => {
      state.bpm = Math.max(40, Math.min(800, parseInt(e.target.value, 10) || 120));
      e.target.value = String(state.bpm);
      saveState();
    });
    $('#ctl-groove').addEventListener('change', (e) => { state.groove = e.target.value; saveState(); });
    $('#ctl-flats').addEventListener('change', (e) => {
      state.flats = e.target.value === '1';
      populateSelects();
      render();
    });

    $('#ctl-explore-root').addEventListener('change', (e) => {
      setExploreRoot(parseInt(e.target.value, 10));
      populateSelects();
      render();
    });
    $('#ctl-explore-family').addEventListener('change', (e) => {
      state.scaleFamily = e.target.value;
      state.modeIndex = 0;
      populateSelects();
      render();
    });
    $('#ctl-explore-mode').addEventListener('change', (e) => {
      state.modeIndex = parseInt(e.target.value, 10);
      populateSelects();
      render();
    });
    $('#ctl-caged-layer').addEventListener('change', (e) => {
      state.showCagedShapes = e.target.checked;
      applyTabUI();
      render();
    });
    $('#ctl-chordtones').addEventListener('change', (e) => { state.showChordTones = e.target.checked; render(); });
    $('#ctl-rootlines').addEventListener('change', (e) => { state.showRootLines = e.target.checked; render(); });

    neck.addEventListener('click', (e) => {
      if (state.tab !== 'chords') return;
      const r = neck.getBoundingClientRect();
      const pct = ((e.clientX - r.left) / r.width) * 100;
      const y = e.clientY - r.top;
      const g = gridAt(pct, y);
      const i = state.chordNotes.findIndex((nt) => nt.s === g.s);
      if (state.pinRoot && i >= 0 && state.pinRoot.s === state.chordNotes[i].s && state.pinRoot.f === state.chordNotes[i].f) {
        state.pinRoot = null;
      }
      if (i >= 0) state.chordNotes[i] = { s: g.s, f: g.f, id: state.chordNotes[i].id };
      else state.chordNotes.push({ s: g.s, f: g.f, id: ++noteSeq });
      render();
    });

    neck.addEventListener('mousemove', (e) => {
      if (state.tab !== 'chords') return;
      const r = neck.getBoundingClientRect();
      const pct = ((e.clientX - r.left) / r.width) * 100;
      const y = e.clientY - r.top;
      placeGhostPlus(pct, y);
    });

    neck.addEventListener('mouseleave', () => {
      if (ghostPlus) ghostPlus.style.display = 'none';
    });

    $('#btn-chord-clear').addEventListener('click', () => {
      state.chordNotes = [];
      state.pinRoot = null;
      scalePropFollow = true;
      scaleSel = null;
      chordScaleFocus = null;
      render();
    });

    $('#btn-chord-play').addEventListener('click', () => {
      playChord();
    });

    $('#chord-result').addEventListener('change', (e) => {
      if (!e.target.classList || !e.target.classList.contains('cr-scale-key')) return;
      scalePropFollow = false;
      scalePropKey = parseInt(e.target.value, 10);
      scaleSel = null;
      chordScaleFocus = null;
      render();
    });

    $('#chord-result').addEventListener('click', (e) => {
      const reset = e.target.closest ? e.target.closest('.cr-scale-key-reset') : null;
      if (reset) {
        e.preventDefault();
        scalePropFollow = true;
        scaleSel = null;
        chordScaleFocus = null;
        render();
        return;
      }
      const pin = e.target.closest ? e.target.closest('.cr-scale-pin') : null;
      if (pin) {
        e.preventDefault();
        e.stopPropagation();
        const chip = pin.closest('.cr-scale-chip');
        if (!chip || !chip.dataset.fk) return;
        const rootPc = parseInt(chip.dataset.r, 10);
        const family = chip.dataset.fk;
        const modeIndex = parseInt(chip.dataset.m, 10);
        chordScaleFocus = { rootPc: rootPc, family: family, modeIndex: modeIndex, name: chip.dataset.n };
        applySharedScale(rootPc, family, modeIndex);
        return;
      }
      const chip = e.target.closest ? e.target.closest('.cr-scale-chip') : null;
      if (!chip) return;
      const r = parseInt(chip.dataset.r, 10);
      const n = chip.dataset.n;
      if (scaleSel && scaleSel.rootPc === r && scaleSel.name === n) {
        scaleSel = null;
      } else {
        scaleSel = {
          rootPc: r,
          name: n,
          ivs: chip.dataset.pcs.split(',').map(Number),
          family: chip.dataset.fk,
          modeIndex: parseInt(chip.dataset.m, 10)
        };
      }
      render();
    });

    $('#chord-result').addEventListener('mouseover', (e) => {
      const nameEl = e.target.closest ? e.target.closest('.cr-name') : null;
      if (!nameEl || !chordResult) return;
      showTip(chordSymbolTipHtml(chordResult), e.clientX, e.clientY);
    });
    $('#chord-result').addEventListener('mouseout', (e) => {
      const nameEl = e.target.closest ? e.target.closest('.cr-name') : null;
      if (!nameEl) return;
      const next = e.relatedTarget;
      if (next && nameEl.contains(next)) return;
      hideTip();
    });
    $('#chord-result').addEventListener('mousemove', (e) => {
      if (tip.style.display === 'none') return;
      if (!(e.target.closest && e.target.closest('.cr-name'))) return;
      positionTip(e.clientX, e.clientY);
    });

    boardWrap.addEventListener('mousemove', (e) => {
      if (tip.style.display !== 'none') {
        positionTip(e.clientX, e.clientY);
      }
    });

    profiles.bind();
    bindGuitarPicker();

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') closeCtx();
    });
    document.addEventListener('click', (e) => {
      if (ctxMenu && !ctxMenu.contains(e.target)) closeCtx();
    });
    document.addEventListener('contextmenu', (e) => {
      if (ctxMenu && !ctxMenu.contains(e.target)) closeCtx();
    });
    window.addEventListener('scroll', closeCtx, true);
    window.addEventListener('resize', closeCtx);
  }

  function init() {
    profiles = window.FretProfiles.create({
      $: $,
      state: state,
      names: names,
      saveState: saveState,
      render: render,
      syncControls: syncControls,
      applyTabUI: applyTabUI,
      esc: esc,
      serializeState: serializeState,
      sanitizeState: sanitizeState
    });
    loadState();
    syncControls();
    applyTabUI();
    populateSelects();
    bindEvents();
    SHAPE_DEFS.forEach((d) => freshShapes.add(d.key));
    render();
  }

  function bindLangPicker() {
    const langSel = document.getElementById('ctl-lang');
    if (!langSel || langSel.dataset.bound === '1') return;
    langSel.dataset.bound = '1';
    langSel.value = window.FretI18n.lang();
    const refreshUi = () => {
      langSel.value = window.FretI18n.lang();
      syncControls();
      populateSelects();
      render();
    };
    langSel.addEventListener('change', () => {
      const code = langSel.value === 'ru' ? 'ru' : 'en';
      const prev = window.FretI18n.lang();
      if (!window.FretI18n.applyLang(code)) {
        langSel.value = prev;
        return;
      }
      refreshUi();
    });
  }

  function storedTheme() {
    try {
      const t = localStorage.getItem('fretboard-lab-theme');
      if (t === 'light' || t === 'dark') return t;
    } catch (e) { /* ignore */ }
    return 'dark';
  }

  function applyTheme(theme) {
    const t = theme === 'light' ? 'light' : 'dark';
    try { localStorage.setItem('fretboard-lab-theme', t); } catch (e) { /* ignore */ }
    document.documentElement.setAttribute('data-theme', t);
    const link = document.getElementById('theme-css');
    if (link) link.href = 'css/' + t + '.css';
  }

  function bindThemePicker() {
    const themeSel = document.getElementById('ctl-theme');
    if (!themeSel || themeSel.dataset.bound === '1') return;
    themeSel.dataset.bound = '1';
    themeSel.value = storedTheme();
    themeSel.addEventListener('change', () => applyTheme(themeSel.value));
  }

  function bootUi() {
    window.FretI18n.applyDom(document);
    init();
    bindThemePicker();
    bindLangPicker();
  }

  const wantLang = window.FretI18n.storedLang() || 'en';
  if (!window.FretI18n.applyLang(wantLang)) window.FretI18n.applyLang('en');
  bootUi();
})();
