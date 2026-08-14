(function () {
  const M = window.Music;
  const G = window.FretGeom;
  const SHAPE_DEFS = window.FretCaged.SHAPE_DEFS;
  const t = (path, vars) => (window.FretI18n && window.FretI18n.t(path, vars)) || path;
  const $ = (s) => document.querySelector(s);
  const neck = $('#neck');
  const boardWrap = $('#board-wrap');

  const CHROMATIC_ROMAN = ['I', 'bII', 'II', 'bIII', 'III', 'IV', 'bV', 'V', 'bVI', 'VI', 'bVII', 'VII'];
  const CHROMATIC_ROMAN_SHARP = { 1: '#I', 3: '#II', 6: '#IV', 8: '#V', 10: '#VI' };
  function degreeRoman(rel, familyKey, modeIndex) {
    rel = ((rel % 12) + 12) % 12;
    const fk = familyKey != null ? familyKey : (state.tab === 'explore' ? state.scaleFamily : null);
    const mi = modeIndex != null ? modeIndex : (state.tab === 'explore' ? state.modeIndex : 0);
    if (fk != null) {
      const alts = M.characteristicAlts(fk, mi);
      if (alts[rel] === 'up' && CHROMATIC_ROMAN_SHARP[rel]) return CHROMATIC_ROMAN_SHARP[rel];
    }
    return CHROMATIC_ROMAN[rel];
  }

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
    fretCount: 15,
    flats: false,
    noteSpell: 'fit',
    sound: true,
    showNames: true,
    showDegrees: true,
    showIntervals: false,
    compoundIntervals: true,
    showNo5: true,
    fullChart: false,
    showPiano: true,
    showLegend: false,
    shapes: { C: true, A: true, G: true, E: true, D: true },
    showCagedShapes: true,
    showQuartal: false,
    showNps: false,
    showBerklee: false,
    quartalHeight: 3,
    quartalStartString: 1,
    quartalRepeats: true,
    npsCount: 3,
    npsRepeats: true,
    npsForms: {},
    berkleeRepeats: true,
    berkleeForms: {},
    cagedRepeats: true,
    showCharTips: true,
    showCharOriginals: true,
    highlightDegree: 0,
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

  /** Cross-module view state shared by board-view / chord-ui / scale-details. */
  const ui = {
    chordResult: null,
    scaleSel: null,
    chordScaleFocus: null,
    scalePropFollow: true,
    scalePropKey: 0,
    noteSeq: 0,
    shapesSpoilerOpen: false,
    scaleChordSpoilers: { chords: false, chords7: false },
    /** On Theory tab: 'explore' | 'chords' | null (About / no demo). */
    theoryBoardMode: null
  };

  let profiles;
  let chordUi;
  let scaleDetails;
  let boardView;
  let pianoView;
  let theory;
  let drills;
  const freshShapes = new Set();

  function chromaticNames(spell) {
    const key = spell === 'flat' ? 'noteNames.flat' : 'noteNames.sharp';
    const arr = window.FretI18n && window.FretI18n.raw(key);
    if (Array.isArray(arr) && arr.length === 12) return arr.slice();
    return (spell === 'flat' ? M.FLAT : M.SHARP).slice();
  }

  function localizeSpell(en) {
    if (!en) return en;
    const m = /^([A-G])(#*|b*)$/.exec(en);
    if (!m) return en;
    const letterIdx = M.LETTERS.indexOf(m[1]);
    if (letterIdx < 0) return en;
    const natPc = [0, 2, 4, 5, 7, 9, 11][letterIdx];
    const natural = chromaticNames('sharp')[natPc];
    return natural + m[2];
  }

  function names() {
    if (state.noteSpell !== 'fit') return chromaticNames(state.noteSpell);
    const target = scaleDetails ? scaleDetails.resolveTarget() : null;
    if (!target || !M.SCALES[target.family]) return chromaticNames('sharp');
    const fitted = M.fitScaleNames(target.root, M.scaleOrdered(target.family, target.modeIndex));
    const out = chromaticNames(fitted.preferFlat ? 'flat' : 'sharp');
    if (fitted.names) {
      for (let pc = 0; pc < 12; pc++) {
        if (fitted.names[pc]) out[pc] = localizeSpell(fitted.names[pc]);
      }
    }
    return out;
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
  const playSequenceRaw = audio.playSequence;
  const playSimultaneous = audio.playSimultaneous;
  const playBlocks = audio.playBlocks;

  function playSequence(notes, rootOverride) {
    if (scaleDetails) scaleDetails.clearPlaying();
    playSequenceRaw(notes, rootOverride);
  }

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
    if (window.FretQuartal) {
      state.quartalHeight = window.FretQuartal.clampHeight(state.quartalHeight, state.stringCount);
      state.quartalStartString = window.FretQuartal.clampStartString(
        state.quartalStartString, state.stringCount, state.quartalHeight
      );
    }
    pruneChordNotesToBoard();
    if (enforceCagedHonesty()) {
      renderLayerPicker();
      const shapes = $('#ctl-caged-shapes');
      if (shapes) shapes.classList.add('hidden');
    }
  }

  /** @returns {'ok'|'approx'|'blocked'} */
  function cagedHonestyMode() {
    const n = state.stringCount | 0;
    if (n < 6) return 'blocked';
    if (n > 6) return 'approx';
    return 'ok';
  }

  /** Clear CAGED when the active guitar cannot host it. @returns {boolean} whether layer was cleared */
  function enforceCagedHonesty() {
    if (!state.showCagedShapes) return false;
    if (cagedHonestyMode() !== 'blocked') return false;
    state.showCagedShapes = false;
    return true;
  }

  function cagedLayerDescKey() {
    const mode = cagedHonestyMode();
    if (mode === 'blocked') return 'controls.explore.layerCagedDescBlocked';
    if (mode === 'approx') return 'controls.explore.layerCagedDescApprox';
    return 'controls.explore.layerCagedDesc';
  }

  function pruneChordNotesToBoard() {
    const n = state.stringCount;
    const N = state.fretCount;
    state.chordNotes = state.chordNotes.filter((nt) =>
      nt && Number.isFinite(nt.s) && Number.isFinite(nt.f) && nt.s >= 0 && nt.s < n && nt.f >= 0 && nt.f <= N
    );
    if (state.pinRoot) {
      const pin = state.pinRoot;
      const onBoard = pin.s >= 0 && pin.s < n && pin.f >= 0 && pin.f <= N &&
        state.chordNotes.some((nt) => nt.s === pin.s && nt.f === pin.f);
      if (!onBoard) state.pinRoot = null;
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
      const fk = e.family || 'other';
      if (!buckets[fk]) buckets[fk] = [];
      buckets[fk].push(e);
    });
    const groups = [];
    const seen = {};
    M.SCALE_CATS.forEach((cat) => {
      cat.families.forEach((fk) => {
        if (!buckets[fk] || !buckets[fk].length || seen[fk]) return;
        seen[fk] = true;
        groups.push({ id: fk, label: scaleFamilyLabel(fk), entries: buckets[fk] });
      });
    });
    Object.keys(buckets).forEach((fk) => {
      if (seen[fk] || !buckets[fk].length) return;
      groups.push({ id: fk, label: scaleFamilyLabel(fk), entries: buckets[fk] });
    });
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

  const STORE_KEY = 'fretboard-lab-v1';

  function serializeState() {
    return {
      tab: state.tab,
      stringCount: state.stringCount,
      fretCount: state.fretCount,
      flats: state.noteSpell === 'flat',
      noteSpell: state.noteSpell,
      sound: state.sound,
      showNames: drillsDisplaySnap ? drillsDisplaySnap.showNames : state.showNames,
      showDegrees: drillsDisplaySnap ? drillsDisplaySnap.showDegrees : state.showDegrees,
      showIntervals: drillsDisplaySnap ? drillsDisplaySnap.showIntervals : state.showIntervals,
      compoundIntervals: state.compoundIntervals,
      showNo5: state.showNo5,
      fullChart: state.fullChart,
      showPiano: state.showPiano,
      showLegend: state.showLegend,
      shapes: state.shapes,
      showCagedShapes: state.showCagedShapes,
      showQuartal: state.showQuartal,
      showNps: state.showNps,
      showBerklee: state.showBerklee,
      quartalHeight: state.quartalHeight,
      quartalStartString: state.quartalStartString,
      quartalRepeats: state.quartalRepeats,
      npsCount: state.npsCount,
      npsRepeats: state.npsRepeats,
      npsForms: state.npsForms,
      berkleeRepeats: state.berkleeRepeats,
      berkleeForms: state.berkleeForms,
      cagedRepeats: state.cagedRepeats,
      showCharTips: state.showCharTips,
      showCharOriginals: state.showCharOriginals,
      highlightDegree: state.highlightDegree,
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
      activeProfileId: state.activeProfileId,
      shapesSpoilerOpen: !!ui.shapesSpoilerOpen,
      scaleChordSpoilers: {
        chords: !!ui.scaleChordSpoilers.chords,
        chords7: !!ui.scaleChordSpoilers.chords7
      }
    };
  }

  function saveState() {
    try {
      localStorage.setItem(STORE_KEY, JSON.stringify(serializeState()));
      return true;
    } catch (e) { return false; }
  }

  let saveTimer = null;
  let deepLinkWriteSuppressed = false;
  let pendingTheoryArticle = null;

  function scheduleSave() {
    if (saveTimer) clearTimeout(saveTimer);
    saveTimer = setTimeout(() => {
      saveTimer = null;
      saveState();
      writeDeepLink();
    }, 220);
  }

  function applyLayerId(layer) {
    if (layer === 'caged' && cagedHonestyMode() === 'blocked') layer = 'none';
    state.showCagedShapes = layer === 'caged';
    state.showQuartal = layer === 'quartal';
    state.showNps = layer === 'nps';
    state.showBerklee = layer === 'berklee';
  }

  function applyDeepLink(slice) {
    const DL = window.FretDeepLink;
    if (!DL || !DL.hasShareable(slice)) return false;

    if (slice.names != null) state.showNames = !!slice.names;
    if (slice.degrees != null) state.showDegrees = !!slice.degrees;
    if (slice.intervals != null) state.showIntervals = !!slice.intervals;

    if (slice.l != null) applyLayerId(slice.l);

    if (slice.f != null || slice.r != null || slice.m != null) {
      const fam = slice.f != null ? slice.f : state.scaleFamily;
      if (M.SCALES[fam]) {
        const maxMode = (M.SCALES[fam].modes || []).length - 1;
        const root = slice.r != null ? slice.r : state.scaleKey;
        const mode = slice.m != null ? slice.m : state.modeIndex;
        setExploreRoot(root);
        state.scaleFamily = fam;
        state.modeIndex = Math.max(0, Math.min(maxMode < 0 ? 0 : maxMode, mode | 0));
        const cat = catForFamily(fam);
        if (cat) state.scaleCategory = cat;
      }
    }

    if (slice.spell === 'fit' || slice.spell === 'flat' || slice.spell === 'sharp') {
      state.noteSpell = slice.spell;
      state.flats = slice.spell === 'flat';
    }

    pendingTheoryArticle = null;
    // Share links are self-contained: without explicit t/a/pcs/vox, land on Explore
    // (do not keep a leftover Theory/Drills tab from localStorage).
    const tab = slice.t && ['explore', 'chords', 'drills', 'theory'].indexOf(slice.t) >= 0
      ? slice.t
      : (slice.a ? 'theory' : (slice.pcs || slice.vox ? 'chords' : 'explore'));

    if (tab === 'drills') {
      state.tab = 'drills';
      state.chordNotes = [];
      state.pinRoot = null;
      ui.chordResult = null;
      ui.theoryBoardMode = null;
      return true;
    }

    let placed = false;
    if (slice.vox && chordUi && typeof chordUi.decodeVoicing === 'function') {
      const notes = chordUi.decodeVoicing(slice.vox);
      if (notes && notes.length) {
        state.chordNotes = notes.map((nt) => ({
          s: Math.max(0, Math.min(state.stringCount - 1, nt.s | 0)),
          f: Math.max(0, Math.min(state.fretCount, nt.f | 0)),
          id: ++ui.noteSeq
        }));
        placed = true;
        if (slice.cr != null) {
          const midis = tuningMidis();
          const hit = state.chordNotes.find((nt) => (((midis[nt.s] + nt.f) % 12) + 12) % 12 === slice.cr);
          state.pinRoot = hit ? { s: hit.s, f: hit.f } : null;
        }
      }
    }
    if (!placed && slice.pcs && slice.pcs.length) {
      const rootPcVal = slice.cr != null ? slice.cr : slice.pcs[0];
      placed = !!placeChordNotes(slice.pcs, rootPcVal);
    }

    if (tab === 'theory') {
      state.tab = 'theory';
      if (slice.a) pendingTheoryArticle = slice.a;
      if (placed) {
        ui.theoryBoardMode = 'chords';
      } else {
        // Article-only links should not keep leftover Analyzer notes from localStorage.
        state.chordNotes = [];
        state.pinRoot = null;
        ui.chordResult = null;
        ui.theoryBoardMode = (slice.f != null || slice.r != null || slice.l != null)
          ? 'explore'
          : null;
      }
      return true;
    }

    if (placed || tab === 'chords') {
      state.tab = 'chords';
      ui.theoryBoardMode = null;
      if (!placed) {
        state.chordNotes = [];
        state.pinRoot = null;
        ui.chordResult = null;
      }
      return true;
    }

    state.tab = 'explore';
    state.chordNotes = [];
    state.pinRoot = null;
    ui.chordResult = null;
    ui.theoryBoardMode = null;
    return true;
  }

  function applyDeepLinkFromLocation() {
    const DL = window.FretDeepLink;
    if (!DL) return false;
    const slice = DL.parseLocation();
    if (!slice) return false;
    deepLinkWriteSuppressed = true;
    const ok = applyDeepLink(slice);
    deepLinkWriteSuppressed = false;
    return ok;
  }

  function writeDeepLink() {
    if (deepLinkWriteSuppressed) return;
    const DL = window.FretDeepLink;
    if (!DL || typeof history === 'undefined' || !history.replaceState) return;
    try {
      const slice = DL.sliceFromState(state, {
        articleId: theory && typeof theory.getArticleId === 'function' ? theory.getArticleId() : null,
        encodeVoicing: chordUi && chordUi.encodeVoicing ? (notes) => chordUi.encodeVoicing(notes) : null,
        tuningMidis: tuningMidis
      });
      const hash = DL.build(slice);
      const next = location.pathname + location.search + (hash || '');
      const cur = location.pathname + location.search + (location.hash || '');
      if (next !== cur) history.replaceState(null, '', next);
    } catch (e) { /* ignore */ }
  }

  function bindDeepLink() {
    if (typeof window === 'undefined') return;
    window.addEventListener('hashchange', () => {
      const DL = window.FretDeepLink;
      if (!DL) return;
      const slice = DL.parseLocation();
      if (!slice) return;
      deepLinkWriteSuppressed = true;
      applyDeepLink(slice);
      deepLinkWriteSuppressed = false;
      if (pendingTheoryArticle && theory) {
        theory.openArticle(pendingTheoryArticle);
        pendingTheoryArticle = null;
      }
      syncControls();
      populateSelects();
      renderLayerPicker();
      applyTabUI();
      render();
      scheduleSave();
    });
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
    } else if (tab === 'about') {
      tab = 'theory';
    }
    state.tab = ['explore', 'chords', 'drills', 'theory', 'profiles'].indexOf(tab) >= 0 ? tab : 'explore';
    state.stringCount = [4, 5, 6, 7, 8, 9, 10].indexOf(s.stringCount) >= 0 ? s.stringCount : state.stringCount;
    {
      const fc = s.fretCount | 0;
      if (fc >= 12 && fc <= 24) state.fretCount = fc;
    }
    if (Array.isArray(s.chordNotes)) {
      state.chordNotes = s.chordNotes
        .filter((nt) => nt && Number.isFinite(nt.s) && Number.isFinite(nt.f))
        .map((nt) => ({
          s: Math.max(0, Math.min(state.stringCount - 1, nt.s | 0)),
          f: Math.max(0, Math.min(state.fretCount, nt.f | 0)),
          id: ++ui.noteSeq
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
    if (s.noteSpell === 'fit' || s.noteSpell === 'flat' || s.noteSpell === 'sharp') {
      state.noteSpell = s.noteSpell;
    } else {
      state.noteSpell = s.flats ? 'flat' : 'sharp';
    }
    state.flats = state.noteSpell === 'flat';
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
    state.showPiano = s.showPiano !== false;
    state.showLegend = !!s.showLegend;
    state.compoundIntervals = s.compoundIntervals !== false;
    state.showNo5 = s.showNo5 !== false;
    if (s.shapes && typeof s.shapes === 'object') {
      Object.keys(state.shapes).forEach((k) => { state.shapes[k] = s.shapes[k] !== false; });
    }
    state.showCagedShapes = s.showCagedShapes !== false;
    state.showQuartal = !!s.showQuartal;
    state.showNps = !!s.showNps;
    state.showBerklee = !!s.showBerklee;
    {
      const layers = [state.showCagedShapes, state.showQuartal, state.showNps, state.showBerklee].filter(Boolean).length;
      if (layers > 1) {
        if (state.showCagedShapes) { state.showQuartal = false; state.showNps = false; state.showBerklee = false; }
        else if (state.showBerklee) { state.showQuartal = false; state.showNps = false; }
        else if (state.showNps) { state.showQuartal = false; }
      }
    }
    state.npsCount = window.FretNps ? window.FretNps.clampCount(s.npsCount) : 3;
    state.npsRepeats = s.npsRepeats !== false;
    if (s.npsForms && typeof s.npsForms === 'object') {
      state.npsForms = {};
      Object.keys(s.npsForms).forEach((k) => { state.npsForms[k] = s.npsForms[k] !== false; });
    }
    state.berkleeRepeats = s.berkleeRepeats !== false;
    if (s.berkleeForms && typeof s.berkleeForms === 'object') {
      state.berkleeForms = {};
      Object.keys(s.berkleeForms).forEach((k) => { state.berkleeForms[k] = s.berkleeForms[k] !== false; });
    }
    if (window.FretQuartal) {
      state.quartalHeight = window.FretQuartal.clampHeight(s.quartalHeight, state.stringCount);
      const defStart = window.FretQuartal.defaultStartString(state.stringCount, state.quartalHeight);
      const rawStart = Number.isFinite(s.quartalStartString) ? (s.quartalStartString | 0) : defStart;
      state.quartalStartString = window.FretQuartal.clampStartString(rawStart, state.stringCount, state.quartalHeight);
    } else {
      const n = state.stringCount;
      const minH = Math.min(3, n);
      state.quartalHeight = Math.max(minH, Math.min(n, s.quartalHeight | 0 || 3));
      state.quartalStartString = Math.max(0, Math.min(n - state.quartalHeight, s.quartalStartString | 0));
    }
    state.quartalRepeats = s.quartalRepeats !== false;
    state.cagedRepeats = s.cagedRepeats !== false;
    state.showCharTips = s.showCharTips !== false;
    state.showCharOriginals = s.showCharOriginals !== false;
    if (s.highlightDegree === -1 || s.highlightDegree === null) {
      state.highlightDegree = -1;
    } else if (Number.isFinite(s.highlightDegree)) {
      state.highlightDegree = ((s.highlightDegree % 12) + 12) % 12;
    } else if (s.highlightRoots === false) {
      state.highlightDegree = -1;
    } else {
      state.highlightDegree = 0;
    }
    state.showRootLines = s.showRootLines !== false;
    state.scaleKey = Math.max(0, Math.min(11, (s.scaleKey != null ? s.scaleKey : s.cagedKey) | 0));
    if (M.SCALES[s.scaleFamily]) state.scaleFamily = s.scaleFamily;
    state.scaleCategory = catForFamily(state.scaleFamily) || 'church';
    const maxMode = (M.SCALES[state.scaleFamily].modes || []).length - 1;
    state.modeIndex = Math.max(0, Math.min(maxMode, s.modeIndex | 0));
    state.bpm = Math.max(40, Math.min(300, s.bpm | 0 || 120));
    if (['downsideUp', 'upsideDown', 'bothFromDown', 'bothFromUp', 'lowestRoot', 'upperRoot'].indexOf(s.groove) >= 0) {
      state.groove = s.groove;
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
    ui.shapesSpoilerOpen = !!s.shapesSpoilerOpen;
    const spoilers = s.scaleChordSpoilers && typeof s.scaleChordSpoilers === 'object' ? s.scaleChordSpoilers : {};
    ui.scaleChordSpoilers.chords = !!spoilers.chords;
    ui.scaleChordSpoilers.chords7 = !!spoilers.chords7;
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
      syncBoardSource();
      applyTabUI();
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

  function exploreLayerId() {
    if (state.showCagedShapes) return 'caged';
    if (state.showQuartal) return 'quartal';
    if (state.showNps) return 'nps';
    if (state.showBerklee) return 'berklee';
    return 'none';
  }

  function exploreLayerDefs() {
    const cagedBlocked = cagedHonestyMode() === 'blocked';
    return [
      { id: 'none', nameKey: 'controls.explore.layerNone', descKey: 'controls.explore.layerNoneDesc' },
      {
        id: 'caged',
        nameKey: 'controls.explore.cagedShapes',
        descKey: cagedLayerDescKey(),
        disabled: cagedBlocked
      },
      { id: 'quartal', nameKey: 'controls.explore.quartalChords', descKey: 'controls.explore.layerQuartalDesc' },
      { id: 'nps', nameKey: 'controls.explore.nps', descKey: 'controls.explore.layerNpsDesc' },
      { id: 'berklee', nameKey: 'controls.explore.berklee', descKey: 'controls.explore.layerBerkleeDesc' }
    ];
  }

  function setExploreLayer(layer) {
    if (layer === 'caged' && cagedHonestyMode() === 'blocked') return;
    state.showCagedShapes = layer === 'caged';
    state.showQuartal = layer === 'quartal';
    state.showNps = layer === 'nps';
    state.showBerklee = layer === 'berklee';
    renderLayerPicker();
    applyTabUI();
    render();
  }

  function renderLayerPicker() {
    const el = $('#ctl-explore-layers');
    if (!el) return;
    const wasOpen = el.open;
    const cur = exploreLayerId();
    const defs = exploreLayerDefs();
    const selected = defs.find((d) => d.id === cur) || defs[0];
    const items = defs.map((d) => {
      const on = d.id === cur;
      const disabled = !!d.disabled;
      return '<button type="button" class="layer-picker-item' + (on ? ' on' : '') + (disabled ? ' disabled' : '') +
        '" data-layer="' + d.id + '" role="option" aria-selected="' + (on ? 'true' : 'false') + '"' +
        (disabled ? ' aria-disabled="true" disabled' : '') + '>' +
        '<span class="layer-picker-check" aria-hidden="true">' + (on ? '✓' : '') + '</span>' +
        '<span class="layer-picker-item-text">' +
        '<span class="layer-picker-item-name">' + esc(t(d.nameKey)) + '</span>' +
        '<span class="layer-picker-item-desc">' + esc(t(d.descKey)) + '</span>' +
        '</span>' +
        '</button>';
    }).join('');
    el.innerHTML =
      '<summary class="layer-picker-summary">' +
      '<span class="layer-picker-chip-text">' +
      '<span class="layer-picker-name">' + esc(t(selected.nameKey)) + '</span>' +
      '<span class="layer-picker-sub">' + esc(t(selected.descKey)) + '</span>' +
      '</span>' +
      '<span class="layer-picker-caret" aria-hidden="true">▾</span>' +
      '</summary>' +
      '<div class="layer-picker-menu" role="listbox" aria-label="' + esc(t('controls.explore.layersAria')) + '">' +
      items +
      '</div>';
    el.open = wasOpen;
    el.setAttribute('aria-label', t('controls.explore.layersAria'));
  }

  function bindLayerPicker() {
    const wrap = $('#ctl-explore-layers-wrap');
    const el = $('#ctl-explore-layers');
    if (!el || el.dataset.bound === '1') return;
    el.dataset.bound = '1';
    el.addEventListener('click', (e) => {
      const btn = e.target.closest('.layer-picker-item');
      if (!btn || !el.contains(btn)) return;
      e.preventDefault();
      const layer = btn.dataset.layer || 'none';
      el.open = false;
      if (layer === exploreLayerId()) return;
      setExploreLayer(layer);
    });
    document.addEventListener('click', (e) => {
      if (!el.open) return;
      if (el.contains(e.target)) return;
      if (wrap && wrap.contains(e.target)) return;
      el.open = false;
    });
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && el.open) el.open = false;
    });
  }

  function syncKbPlaceControls() {
    const sel = $('#ctl-kb-string');
    const fret = $('#ctl-kb-fret');
    if (!sel || !fret || !boardView) return;
    const n = state.stringCount | 0;
    const N = state.fretCount | 0;
    boardView.setKbCursor(boardView.getKbCursor().s, boardView.getKbCursor().f, { silent: true });
    const cur = boardView.getKbCursor();
    if (sel.options.length !== n) {
      sel.innerHTML = '';
      for (let s = 0; s < n; s++) {
        const opt = document.createElement('option');
        opt.value = String(s);
        opt.textContent = t('controls.chords.kbStringOpt', { n: n - s });
        sel.appendChild(opt);
      }
    } else {
      for (let s = 0; s < n; s++) {
        if (sel.options[s]) sel.options[s].textContent = t('controls.chords.kbStringOpt', { n: n - s });
      }
    }
    fret.min = '0';
    fret.max = String(N);
    sel.value = String(cur.s);
    fret.value = String(cur.f);
    if (boardView.syncNeckAria) boardView.syncNeckAria();
  }

  function syncControls() {
    renderGuitarChip();
    const langSel = $('#ctl-lang');
    if (langSel) langSel.value = window.FretI18n.lang();
    $('#ctl-flats').value = state.noteSpell === 'fit' ? '2' : (state.noteSpell === 'flat' ? '1' : '0');
    // While Drills locks labels off, keep the switches showing the user's prefs (snap).
    const disp = drillsDisplaySnap || state;
    $('#ctl-names').checked = disp.showNames;
    $('#ctl-degrees').checked = disp.showDegrees;
    $('#ctl-intervals').checked = disp.showIntervals;
    $('#ctl-full').checked = state.fullChart;
    $('#ctl-sound').checked = state.sound;
    const cagedRepeats = $('#ctl-caged-repeats');
    if (cagedRepeats) cagedRepeats.checked = state.cagedRepeats;
    const charTips = $('#ctl-char-tips');
    if (charTips) charTips.checked = state.showCharTips;
    const charOrig = $('#ctl-char-originals');
    if (charOrig) charOrig.checked = state.showCharOriginals;
    $('#ctl-rootlines').checked = state.showRootLines;
    renderLayerPicker();
    const quartalRepeats = $('#ctl-quartal-repeats');
    if (quartalRepeats) quartalRepeats.checked = state.quartalRepeats;
    const npsRepeats = $('#ctl-nps-repeats');
    if (npsRepeats) npsRepeats.checked = state.npsRepeats;
    syncNpsCountSelect();
    const berkleeRepeats = $('#ctl-berklee-repeats');
    if (berkleeRepeats) berkleeRepeats.checked = state.berkleeRepeats;
    populateQuartalControls();
    $('#ctl-compound').value = state.compoundIntervals ? '1' : '0';
    const showNo5 = $('#ctl-show-no5');
    if (showNo5) showNo5.checked = state.showNo5;
    $('#ctl-bpm').value = String(state.bpm);
    $('#ctl-groove').value = state.groove;
    syncKbPlaceControls();
  }

  function syncNpsCountSelect() {
    const sel = $('#ctl-nps-count');
    if (!sel) return;
    if (window.FretNps) state.npsCount = window.FretNps.clampCount(state.npsCount);
    else state.npsCount = 3;
    const opts = (window.FretNps && window.FretNps.COUNT_OPTIONS) || [3, 4];
    const cur = String(state.npsCount);
    if (sel.options.length !== opts.length) {
      sel.innerHTML = opts.map((n) => '<option value="' + n + '">' + n + '</option>').join('');
    }
    sel.value = cur;
  }

  function populateQuartalHeightSelect() {
    const sel = $('#ctl-quartal-height');
    if (!sel || !window.FretQuartal) return;
    const n = state.stringCount;
    state.quartalHeight = window.FretQuartal.clampHeight(state.quartalHeight, n);
    const opts = window.FretQuartal.heightOptions(n);
    const cur = String(state.quartalHeight);
    sel.innerHTML = '';
    opts.forEach((h) => {
      const opt = document.createElement('option');
      opt.value = String(h);
      opt.textContent = String(h);
      sel.appendChild(opt);
    });
    sel.value = cur;
    if (sel.value !== cur) sel.value = String(state.quartalHeight);
  }

  function populateQuartalStartSelect() {
    const sel = $('#ctl-quartal-start');
    if (!sel || !window.FretQuartal) return;
    const n = state.stringCount;
    const h = state.quartalHeight;
    state.quartalStartString = window.FretQuartal.clampStartString(state.quartalStartString, n, h);
    const maxStart = Math.max(0, n - h);
    const cur = String(state.quartalStartString);
    sel.innerHTML = '';
    for (let eng = 0; eng <= maxStart; eng++) {
      const playerNum = n - eng;
      const opt = document.createElement('option');
      opt.value = String(eng);
      opt.textContent = t('controls.quartal.startStringOpt', { n: playerNum });
      sel.appendChild(opt);
    }
    sel.value = cur;
    if (sel.value !== cur) sel.value = String(maxStart >= 0 ? Math.min(state.quartalStartString, maxStart) : 0);
  }

  function populateQuartalControls() {
    populateQuartalHeightSelect();
    populateQuartalStartSelect();
  }

  function boardlessTab() {
    return state.tab === 'profiles';
  }

  function theoryShowsBoard() {
    return state.tab === 'theory' && !!ui.theoryBoardMode;
  }

  function placeChordNotes(pcs, rootPc, voicingNotes) {
    const midis = tuningMidis();
    let notes = voicingNotes;
    if (!notes || !notes.length) notes = chordUi.placeChordPcs(pcs, midis, state.fretCount);
    if (!notes || !notes.length) return false;
    state.chordNotes = notes.map((nt) => ({ s: nt.s, f: nt.f, id: ++ui.noteSeq }));
    const rootNote = notes.find((nt) => (((midis[nt.s] + nt.f) % 12) + 12) % 12 === rootPc);
    state.pinRoot = rootNote ? { s: rootNote.s, f: rootNote.f } : { s: notes[0].s, f: notes[0].f };
    ui.scaleSel = null;
    return true;
  }

  function openScaleChordInAnalyzer(pcs, rootPc, voicingNotes) {
    if (!placeChordNotes(pcs, rootPc, voicingNotes)) return false;
    state.tab = 'chords';
    applyTabUI();
      render();
    requestAnimationFrame(() => {
      if (boardWrap) boardWrap.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    return true;
  }

  /** Apply a Theory lesson to the companion board without leaving the Theory tab. */
  function applyTheoryDemo(spec) {
    const lesson = spec && typeof spec === 'object' ? spec : null;
    if (!lesson || lesson.tab === 'drills') {
      ui.theoryBoardMode = null;
      state.chordNotes = [];
      state.pinRoot = null;
      ui.chordResult = null;
      applyTabUI();
      if (state.tab === 'theory') {
        scheduleSave();
      }
      return;
    }

    if (lesson.showNames != null) state.showNames = !!lesson.showNames;
    if (lesson.showDegrees != null) state.showDegrees = !!lesson.showDegrees;
    if (lesson.showIntervals != null) state.showIntervals = !!lesson.showIntervals;

    if (lesson.layer != null) {
      applyLayerId(lesson.layer);
    } else if (lesson.tab !== 'chords') {
      state.showCagedShapes = false;
      state.showQuartal = false;
      state.showNps = false;
      state.showBerklee = false;
    }

    if (lesson.family != null || lesson.root != null || lesson.mode != null) {
      const fam = lesson.family != null ? lesson.family : state.scaleFamily;
      if (M.SCALES[fam]) {
        const maxMode = (M.SCALES[fam].modes || []).length - 1;
        const root = lesson.root != null ? (((lesson.root % 12) + 12) % 12) : state.scaleKey;
        const mode = lesson.mode != null ? lesson.mode : state.modeIndex;
        setExploreRoot(root);
        state.scaleFamily = fam;
        state.modeIndex = Math.max(0, Math.min(maxMode < 0 ? 0 : maxMode, mode | 0));
        const cat = catForFamily(fam);
        if (cat) state.scaleCategory = cat;
      }
    }

    if (lesson.noteSpell === 'fit' || lesson.noteSpell === 'flat' || lesson.noteSpell === 'sharp') {
      state.noteSpell = lesson.noteSpell;
    }

    if (lesson.tab === 'chords' && Array.isArray(lesson.chordPcs) && lesson.chordPcs.length) {
      const rootPcVal = lesson.chordRoot != null
        ? (((lesson.chordRoot % 12) + 12) % 12)
        : (lesson.chordPcs[0] % 12);
      if (!placeChordNotes(lesson.chordPcs, rootPcVal)) {
        ui.theoryBoardMode = null;
      } else {
        ui.theoryBoardMode = 'chords';
      }
    } else {
      state.chordNotes = [];
      state.pinRoot = null;
      ui.chordResult = null;
      ui.theoryBoardMode = 'explore';
    }

    populateSelects();
    renderLayerPicker();
    applyTabUI();
    syncControls();
    if (boardView) boardView.drawBoard();
    if (pianoView) pianoView.renderPiano();
    scheduleSave();
  }

  function openTheoryArticle(id) {
    state.tab = 'theory';
    applyTabUI();
    if (theory && typeof theory.openArticle === 'function') theory.openArticle(id);
    else render();
    scheduleSave();
  }

  function openTheoryLesson(spec) {
    const lesson = spec && typeof spec === 'object' ? spec : {};
    if (lesson.showNames != null) state.showNames = !!lesson.showNames;
    if (lesson.showDegrees != null) state.showDegrees = !!lesson.showDegrees;
    if (lesson.showIntervals != null) state.showIntervals = !!lesson.showIntervals;

    if (lesson.layer != null) {
      applyLayerId(lesson.layer);
    }

    if (lesson.family != null || lesson.root != null || lesson.mode != null) {
      const fam = lesson.family != null ? lesson.family : state.scaleFamily;
      if (M.SCALES[fam]) {
        const maxMode = (M.SCALES[fam].modes || []).length - 1;
        const root = lesson.root != null ? (((lesson.root % 12) + 12) % 12) : state.scaleKey;
        const mode = lesson.mode != null ? lesson.mode : state.modeIndex;
        setExploreRoot(root);
        state.scaleFamily = fam;
        state.modeIndex = Math.max(0, Math.min(maxMode < 0 ? 0 : maxMode, mode | 0));
        const cat = catForFamily(fam);
        if (cat) state.scaleCategory = cat;
      }
    }

    if (lesson.noteSpell === 'fit' || lesson.noteSpell === 'flat' || lesson.noteSpell === 'sharp') {
      state.noteSpell = lesson.noteSpell;
    }

    if (lesson.tab === 'drills') {
      state.tab = 'drills';
      applyTabUI();
      syncControls();
      saveState();
      render();
      requestAnimationFrame(() => {
        if (boardWrap) boardWrap.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
      return;
    }

    if (lesson.tab === 'chords' && Array.isArray(lesson.chordPcs) && lesson.chordPcs.length) {
      const rootPcVal = lesson.chordRoot != null ? (((lesson.chordRoot % 12) + 12) % 12) : (lesson.chordPcs[0] % 12);
      openScaleChordInAnalyzer(lesson.chordPcs, rootPcVal);
      return;
    }

    state.tab = 'explore';
    populateSelects();
    renderLayerPicker();
    applyTabUI();
    syncControls();
    saveState();
    render();
    requestAnimationFrame(() => {
      if (boardWrap) boardWrap.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  }

  let drillsDisplaySnap = null;

  function lockDrillsDisplay() {
    const cluster = $('#ctl-display');
    if (cluster) {
      cluster.classList.add('ctl-locked');
      cluster.setAttribute('aria-disabled', 'true');
      cluster.title = t('drills.displayLocked');
      cluster.querySelectorAll('input, select').forEach((el) => { el.disabled = true; });
    }
    if (!drillsDisplaySnap) {
      drillsDisplaySnap = {
        showNames: state.showNames,
        showDegrees: state.showDegrees,
        showIntervals: state.showIntervals
      };
    }
    state.showNames = false;
    state.showDegrees = false;
    state.showIntervals = false;
  }

  /** Restore user's Display prefs for drawing without leaving Drills / clearing the snap. */
  function peekDrillsDisplay() {
    if (!drillsDisplaySnap) return;
    state.showNames = drillsDisplaySnap.showNames;
    state.showDegrees = drillsDisplaySnap.showDegrees;
    state.showIntervals = drillsDisplaySnap.showIntervals;
  }

  /** Force labels off again while still on Drills (after Show answer peek). */
  function relockDrillsDisplay() {
    if (state.tab !== 'drills' || !drillsDisplaySnap) return;
    state.showNames = false;
    state.showDegrees = false;
    state.showIntervals = false;
  }

  function unlockDrillsDisplay() {
    const cluster = $('#ctl-display');
    if (cluster) {
      cluster.classList.remove('ctl-locked');
      cluster.removeAttribute('aria-disabled');
      cluster.removeAttribute('title');
      cluster.querySelectorAll('input, select').forEach((el) => { el.disabled = false; });
    }
    if (drillsDisplaySnap) {
      state.showNames = drillsDisplaySnap.showNames;
      state.showDegrees = drillsDisplaySnap.showDegrees;
      state.showIntervals = drillsDisplaySnap.showIntervals;
      drillsDisplaySnap = null;
    }
  }

  function applyTabUI() {
    document.querySelectorAll('.tab').forEach((t) => {
      const on = t.dataset.tab === state.tab;
      t.classList.toggle('active', on);
      t.setAttribute('aria-selected', on ? 'true' : 'false');
      t.tabIndex = on ? 0 : -1;
    });
    if (state.tab !== 'drills' && drills && typeof drills.active === 'function' && drills.active()) {
      drills.end({ silent: true });
    }
    if (state.tab === 'drills') lockDrillsDisplay();
    else unlockDrillsDisplay();
    if (state.tab !== 'theory') {
      ui.theoryBoardMode = null;
      if (theory && typeof theory.invalidateDemo === 'function') theory.invalidateDemo();
    }
    const hideBoard = boardlessTab() || (state.tab === 'theory' && !ui.theoryBoardMode);
    const theoryLive = theoryShowsBoard();
    $('#ctl-main-controls').classList.toggle('hidden', hideBoard);
    $('#ctl-explore').classList.toggle('hidden', state.tab !== 'explore');
    $('#ctl-chords').classList.toggle('hidden', state.tab !== 'chords');
    if (state.tab !== 'chords' && boardView && boardView.clearKbReveal) boardView.clearKbReveal();
    $('#ctl-playback').classList.toggle('hidden', hideBoard || state.tab === 'drills' || state.tab === 'theory');
    $('#ctl-chord-opts').classList.toggle('hidden', state.tab !== 'chords');
    $('#ctl-caged-shapes').classList.toggle('hidden', state.tab !== 'explore');
    document.querySelectorAll('.ctl-shapes-layer').forEach((el) => {
      el.classList.toggle('hidden', !state.showCagedShapes || state.tab === 'theory');
    });
    const qStrip = $('#ctl-quartal-shapes');
    if (qStrip) qStrip.classList.toggle('hidden', state.tab !== 'explore' || !state.showQuartal);
    const npsStrip = $('#ctl-nps-shapes');
    if (npsStrip) npsStrip.classList.toggle('hidden', state.tab !== 'explore' || !state.showNps);
    const berkleeStrip = $('#ctl-berklee-shapes');
    if (berkleeStrip) berkleeStrip.classList.toggle('hidden', state.tab !== 'explore' || !state.showBerklee);
    $('#ctl-playback-controls').classList.toggle('hidden', !state.sound || hideBoard || state.tab === 'drills' || state.tab === 'theory');
    $('#board-wrap').classList.toggle('hidden', hideBoard);
    $('#legend').classList.toggle('hidden', hideBoard || state.tab === 'drills' || state.tab === 'theory');
    $('#tipbox').classList.toggle('hidden', hideBoard || state.tab === 'drills' || state.tab === 'theory');
    if (hideBoard || state.tab === 'chords' || state.tab === 'drills' || state.tab === 'theory') $('#scale-details').classList.add('hidden');
    else $('#scale-details').classList.remove('hidden');
    $('#chord-result').classList.toggle('hidden', state.tab !== 'chords');
    $('#profiles-panel').classList.toggle('hidden', state.tab !== 'profiles');
    const theoryPanel = $('#theory-panel');
    if (theoryPanel) {
      theoryPanel.classList.toggle('hidden', state.tab !== 'theory');
      theoryPanel.classList.toggle('theory-with-board', theoryLive);
    }
    const drillsPanel = $('#drills-panel');
    const drillBar = $('#drill-bar');
    if (state.tab !== 'drills') {
      if (drillsPanel) drillsPanel.classList.add('hidden');
      if (drillBar) drillBar.classList.add('hidden');
    }
    syncControls();
  }

  function render() {
    boardView.hideTip();
    syncBoardSource();
    renderGuitarChip();
    if (state.tab === 'profiles') {
      profiles.renderProfiles();
      scheduleSave();
      return;
    }
    if (state.tab === 'theory') {
      theory.renderTheory();
      if (ui.theoryBoardMode) {
        boardView.drawBoard();
        pianoView.renderPiano();
      }
      scheduleSave();
      return;
    }
    if (state.tab === 'drills') {
      drills.renderDrills();
      boardView.drawBoard();
      pianoView.renderPiano();
      scheduleSave();
      return;
    }
    boardView.drawBoard();
    pianoView.renderPiano();
    renderLegend();
    renderTipbox();
    chordUi.renderChordResult();
    scaleDetails.renderScaleDetails();
    scheduleSave();
    freshShapes.clear();
  }

  function exploreLayerLabel(prefix) {
      const parts = [];
    if (state.showCagedShapes) {
      const approx = cagedHonestyMode() === 'approx';
      if (prefix === 'tipbox') {
        parts.push(t(approx ? 'tipbox.layerCagedOnApprox' : 'tipbox.layerCagedOn'));
      } else {
        parts.push(t(approx ? 'legend.layerCagedApprox' : 'legend.layerCaged'));
      }
    }
    if (state.showQuartal) parts.push(t(prefix === 'tipbox' ? 'tipbox.layerQuartalOn' : 'legend.layerQuartal'));
    if (state.showNps) parts.push(t(prefix === 'tipbox' ? 'tipbox.layerNpsOn' : 'legend.layerNps'));
    if (state.showBerklee) parts.push(t(prefix === 'tipbox' ? 'tipbox.layerBerkleeOn' : 'legend.layerBerklee'));
    if (!parts.length) return t(prefix === 'tipbox' ? 'tipbox.layerFullScale' : 'legend.layerFullScale');
    return parts.join(' · ');
  }

  function renderLegend() {
    const el = $('#legend');
    if (!el) return;
    el.classList.toggle('legend-collapsed', !state.showLegend);

    const head =
      '<div class="legend-head">' +
      '<span class="legend-title">' + t('legend.title') + '</span>' +
      '<label class="switch" title="' + t('legend.toggle') + '">' +
      '<input type="checkbox" aria-label="' + t('legend.toggle') + '"' +
      (state.showLegend ? ' checked' : '') + '>' +
      '<span class="switch-track" aria-hidden="true"></span>' +
      '</label>' +
      '</div>';

    if (!state.showLegend) {
      el.innerHTML = head;
      return;
    }

    let body = '';
    if (state.tab === 'explore') {
      const desc = scaleDisplayName(state.scaleFamily, state.modeIndex);
      const layer = exploreLayerLabel('legend');
      let hint = t('legend.hintFullScale');
      if (state.showCagedShapes) {
        hint = t(cagedHonestyMode() === 'approx' ? 'legend.hintCagedApprox' : 'legend.hintCaged');
      } else if (state.showQuartal) hint = t('legend.hintQuartal');
      else if (state.showNps) hint = t('legend.hintNps');
      else if (state.showBerklee) hint = t('legend.hintBerklee');
      body =
        '<h3>' + t('legend.exploreTitle', { root: names()[state.scaleKey], scale: desc, layer: layer }) + '</h3>' +
        '<div class="legend-items">' +
        legendItem(t('legend.swatchRoot'), 'gold', t('legend.swatchRootDesc')) +
        legendItem(t('legend.swatchScale'), '#3b6fd4', t('legend.swatchScaleDesc')) +
        (state.fullChart && !state.showCagedShapes && !state.showQuartal && !state.showNps && !state.showBerklee ? legendItem(t('legend.swatchPlain'), 'rgba(255,255,255,.25)', t('legend.swatchPlainDesc')) : '') +
        '</div>' +
        '<p class="hint">' + hint + '</p>' +
        '<p class="legend-learn"><button type="button" class="legend-learn-btn" data-theory-article="systems">' + t('legend.learnSystems') + '</button>' +
        ' <button type="button" class="legend-learn-btn" data-theory-article="modes">' + t('legend.learnModes') + '</button></p>';
    } else if (state.tab === 'chords') {
      body =
        '<h3>' + t('legend.chordsTitle') + '</h3>' +
        '<div class="legend-items">' +
        '<span class="legend-item"><i class="swatch swatch-place"><span>+</span></i><b>' + t('legend.placeNote') + '</b> — ' + t('legend.placeNoteDesc') + '</span>' +
        '</div>' +
        '<p class="hint">' + t('legend.hintChords') + '</p>' +
        '<p class="legend-learn"><button type="button" class="legend-learn-btn" data-theory-article="building">' + t('legend.learnBuilding') + '</button>' +
        ' <button type="button" class="legend-learn-btn" data-theory-article="extensionsAndAlts">' + t('scaleDetails.learnExtensions') + '</button>' +
        ' <button type="button" class="legend-learn-btn" data-theory-article="chords">' + t('legend.learnChords') + '</button></p>';
    }
    el.innerHTML = head + (body ? '<div class="legend-body">' + body + '</div>' : '');
  }

  function legendItem(text, color, desc) {
    return '<span class="legend-item"><i class="swatch" style="background:' + color + '"></i><b>' + text + '</b> — ' + desc + '</span>';
  }

  function renderTipbox() {
    const el = $('#tipbox');
    if (state.tab === 'explore') {
      const fam = M.SCALES[state.scaleFamily];
      const desc = scaleDisplayName(state.scaleFamily, state.modeIndex);
      const layer = exploreLayerLabel('tipbox');
      const modePart = fam.modes
        ? t('tipbox.exploreModePart', { n: state.modeIndex + 1, parentKey: state.scaleKeyName(), familyShort: scaleFamilyShort(state.scaleFamily) })
        : '';
      const body = t('tipbox.exploreBody', { root: names()[state.scaleKey], scale: desc, modePart: modePart, layer: layer });
      const famObj = M.SCALES[state.scaleFamily];
      const modeName = famObj && famObj.modes ? famObj.modes[state.modeIndex] : null;
      const info = theoryInfo(modeName, state.scaleFamily);
      const why = info && info.desc
        ? ' <button type="button" class="tip-learn" data-theory-article="modes">' + t('tipbox.learnModes') + '</button>'
        : '';
      el.innerHTML = '<strong>' + t('tipbox.exploreStrong') + '</strong> — <span>' + body + '</span>' + why;
    } else if (state.tab === 'chords') {
      el.innerHTML = '<strong>' + t('tipbox.chordsStrong') + '</strong> — <span>' + t('tipbox.chordsBody') + '</span>' +
        ' <button type="button" class="tip-learn" data-theory-article="building">' + t('tipbox.learnBuilding') + '</button>';
    }
  }

  state.scaleKeyName = function () {
    const p = M.parentScale(state.scaleFamily, state.modeIndex, state.scaleKey);
    return names()[p ? p.rootPc : state.scaleKey];
  };

  function populateHighlightDegreeSelect() {
    const sel = $('#ctl-highlight-degree');
    if (!sel) return;
    const ordered = M.scaleOrdered(state.scaleFamily, state.modeIndex) || [0];
    if (state.highlightDegree >= 0 && ordered.indexOf(state.highlightDegree) < 0) {
      state.highlightDegree = ordered.indexOf(0) >= 0 ? 0 : ordered[0];
    }
    let html = '<option value="-1"' + (state.highlightDegree < 0 ? ' selected' : '') + '>' + t('controls.shapes.highlightNone') + '</option>';
    ordered.forEach((v) => {
      html += '<option value="' + v + '"' + (state.highlightDegree === v ? ' selected' : '') + '>' + degreeRoman(v) + '</option>';
    });
    sel.innerHTML = html;
  }

  function populateSelects() {
    const nm = names();
    const rootSel = $('#ctl-explore-root');
    rootSel.innerHTML = nm.map((nn, i) => '<option value="' + i + '"' + (i === state.scaleKey ? ' selected' : '') + '>' + nn + '</option>').join('');
    $('#ctl-explore-family').innerHTML = familyOptions();
    renderScaleCats();
    populateModeSelect();
    populateHighlightDegreeSelect();
    populateShapeButtons();
    populateNpsFormButtons();
    populateBerkleeFormButtons();
    refreshExploreSearchChrome();
  }

  let scaleSearchIndex = null;
  let scaleSearchActive = -1;

  function refreshExploreSearchChrome() {
    const input = $('#ctl-explore-search');
    if (!input) return;
    input.setAttribute('aria-label', t('controls.explore.searchAria'));
    scaleSearchIndex = null;
  }

  function buildScaleSearchIndex() {
    const out = [];
    M.SCALE_CATS.forEach((cat) => {
      const catLabel = scaleCatLabel(cat.id, 'label');
      (cat.families || []).forEach((fk) => {
        const fam = M.SCALES[fk];
        if (!fam) return;
        const famLabel = scaleFamilyLabel(fk);
        const famEn = fam.label || fk;
        if (fam.modes && fam.modes.length) {
          fam.modes.forEach((enName, mi) => {
            const label = scaleModeLabel(fk, mi);
            const hay = [label, enName, famLabel, famEn, fk, catLabel, cat.id].join('\n').toLowerCase();
            out.push({
              family: fk,
              modeIndex: mi,
              category: cat.id,
              label: label,
              meta: catLabel + ' · ' + famLabel,
              hay: hay
            });
          });
        } else {
          const hay = [famLabel, famEn, fk, catLabel, cat.id].join('\n').toLowerCase();
          out.push({
            family: fk,
            modeIndex: 0,
            category: cat.id,
            label: famLabel,
            meta: catLabel,
            hay: hay
          });
        }
      });
    });
    return out;
  }

  function scaleSearchIndexData() {
    if (!scaleSearchIndex) scaleSearchIndex = buildScaleSearchIndex();
    return scaleSearchIndex;
  }

  function normalizeSearchQuery(q) {
    return String(q || '').trim().toLowerCase().replace(/\s+/g, ' ');
  }

  function filterScaleSearch(query) {
    const q = normalizeSearchQuery(query);
    if (!q) return [];
    const parts = q.split(' ').filter(Boolean);
    const hits = [];
    scaleSearchIndexData().forEach((entry) => {
      let ok = true;
      for (let i = 0; i < parts.length; i++) {
        if (entry.hay.indexOf(parts[i]) < 0) { ok = false; break; }
      }
      if (!ok) return;
      const starts = entry.label.toLowerCase().indexOf(q) === 0 || entry.hay.indexOf('\n' + q) >= 0;
      hits.push({ entry: entry, starts: starts });
    });
    hits.sort((a, b) => (b.starts - a.starts) || a.entry.label.localeCompare(b.entry.label));
    return hits.slice(0, 12).map((h) => h.entry);
  }

  function closeExploreSearch() {
    const list = $('#explore-search-results');
    const input = $('#ctl-explore-search');
    if (list) {
      list.hidden = true;
      list.innerHTML = '';
    }
    if (input) input.setAttribute('aria-expanded', 'false');
    scaleSearchActive = -1;
  }

  function renderExploreSearchResults(query) {
    const list = $('#explore-search-results');
    const input = $('#ctl-explore-search');
    if (!list || !input) return;
    const q = normalizeSearchQuery(query);
    if (!q) {
      closeExploreSearch();
      return;
    }
    const hits = filterScaleSearch(q);
    scaleSearchActive = hits.length ? 0 : -1;
    if (!hits.length) {
      list.innerHTML = '<li class="explore-search-empty" role="presentation">' + t('controls.explore.searchNoResults') + '</li>';
    } else {
      list.innerHTML = hits.map((hit, i) =>
        '<li role="presentation">' +
          '<button type="button" class="explore-search-option' + (i === 0 ? ' active' : '') + '"' +
          ' role="option" data-idx="' + i + '"' +
          ' data-family="' + hit.family + '" data-mode="' + hit.modeIndex + '"' +
          ' id="explore-search-opt-' + i + '">' +
            '<span class="explore-search-option-label">' + esc(hit.label) + '</span>' +
            '<span class="explore-search-option-meta">' + esc(hit.meta) + '</span>' +
          '</button>' +
        '</li>'
      ).join('');
    }
    list.hidden = false;
    input.setAttribute('aria-expanded', 'true');
    if (scaleSearchActive >= 0) {
      input.setAttribute('aria-activedescendant', 'explore-search-opt-' + scaleSearchActive);
    } else {
      input.removeAttribute('aria-activedescendant');
    }
  }

  function setExploreSearchActive(idx) {
    const list = $('#explore-search-results');
    if (!list || list.hidden) return;
    const opts = list.querySelectorAll('.explore-search-option');
    if (!opts.length) return;
    const n = opts.length;
    scaleSearchActive = ((idx % n) + n) % n;
    opts.forEach((el, i) => el.classList.toggle('active', i === scaleSearchActive));
    const input = $('#ctl-explore-search');
    if (input) input.setAttribute('aria-activedescendant', 'explore-search-opt-' + scaleSearchActive);
    const active = opts[scaleSearchActive];
    if (active && active.scrollIntoView) active.scrollIntoView({ block: 'nearest' });
  }

  function jumpToScaleSearchHit(btn) {
    if (!btn) return;
    const family = btn.dataset.family;
    const modeIndex = parseInt(btn.dataset.mode, 10) || 0;
    if (!M.SCALES[family]) return;
    const input = $('#ctl-explore-search');
    if (input) input.value = '';
    closeExploreSearch();
    applySharedScale(state.scaleKey, family, modeIndex);
    flashCtl($('#ctl-explore-family'));
    if (M.SCALES[family].modes) flashCtl($('#ctl-explore-mode'));
  }

  function bindExploreSearch() {
    const input = $('#ctl-explore-search');
    const list = $('#explore-search-results');
    if (!input || !list || input.dataset.bound === '1') return;
    input.dataset.bound = '1';
    refreshExploreSearchChrome();

    input.addEventListener('input', () => {
      renderExploreSearchResults(input.value);
    });
    input.addEventListener('focus', () => {
      if (normalizeSearchQuery(input.value)) renderExploreSearchResults(input.value);
    });
    input.addEventListener('keydown', (e) => {
      const open = list && !list.hidden;
      if (e.key === 'ArrowDown') {
        if (!open) renderExploreSearchResults(input.value);
        else setExploreSearchActive(scaleSearchActive + 1);
        e.preventDefault();
      } else if (e.key === 'ArrowUp') {
        if (open) setExploreSearchActive(scaleSearchActive - 1);
        e.preventDefault();
      } else if (e.key === 'Enter') {
        if (!open) return;
        const opts = list.querySelectorAll('.explore-search-option');
        const btn = opts[scaleSearchActive] || opts[0];
        if (btn) {
          e.preventDefault();
          jumpToScaleSearchHit(btn);
        }
      } else if (e.key === 'Escape') {
        if (open) {
          e.preventDefault();
          closeExploreSearch();
        } else if (input.value) {
          input.value = '';
        }
      }
    });
    list.addEventListener('mousedown', (e) => {
      const btn = e.target.closest ? e.target.closest('.explore-search-option') : null;
      if (!btn) return;
      e.preventDefault();
      jumpToScaleSearchHit(btn);
    });
    document.addEventListener('click', (e) => {
      if (!e.target.closest || e.target.closest('.explore-search')) return;
      closeExploreSearch();
    });
  }

  function familyOptions() {
    const cat = M.SCALE_CATS.find((c) => c.id === state.scaleCategory);
    const keys = (cat && cat.families.indexOf(state.scaleFamily) >= 0) ? cat.families : M.allScaleKeys();
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
    const modeBtns = [$('#btn-shift-mode-prev'), $('#btn-shift-mode-next')];
      if (!fam.modes) {
        sel.innerHTML = '<option>—</option>';
        sel.disabled = true;
      modeBtns.forEach((b) => { if (b) b.disabled = true; });
        return;
      }
      sel.disabled = false;
    modeBtns.forEach((b) => { if (b) b.disabled = false; });
    sel.innerHTML = fam.modes.map((_, i) => '<option value="' + i + '"' + (i === state.modeIndex ? ' selected' : '') + '>' + scaleModeLabel(state.scaleFamily, i) + '</option>').join('');
  }

  function flashCtl(el) {
    if (!el) return;
    el.classList.remove('ctl-flash');
    void el.offsetWidth;
    el.classList.add('ctl-flash');
    const done = () => {
      el.classList.remove('ctl-flash');
      el.removeEventListener('animationend', done);
    };
    el.addEventListener('animationend', done);
  }

  function shiftScale(delta) {
    setExploreRoot((state.scaleKey + delta + 12) % 12);
    populateSelects();
    flashCtl($('#ctl-explore-root'));
    flashCtl($(delta < 0 ? '#btn-shift-scale-left' : '#btn-shift-scale-right'));
    render();
  }

  function shiftMode(delta) {
    const fam = M.SCALES[state.scaleFamily];
    if (!fam || !fam.modes || !fam.modes.length) return;
    const n = fam.modes.length;
    state.modeIndex = (state.modeIndex + delta + n) % n;
    populateSelects();
    flashCtl($('#ctl-explore-mode'));
    flashCtl($(delta < 0 ? '#btn-shift-mode-prev' : '#btn-shift-mode-next'));
    render();
  }

  /** Shift↑↓ — move the treble (upper) end. delta +1 = toward higher strings. */
  function shiftQuartalUpper(delta) {
    if (!state.showQuartal || !window.FretQuartal || !delta) return false;
    const n = state.stringCount;
    const start = state.quartalStartString;
    const maxH = n - start;
    const minH = Math.min(window.FretQuartal.MIN_HEIGHT, n);
    let h = state.quartalHeight + delta;
    if (h < minH) h = minH;
    if (h > maxH) h = maxH;
    h = window.FretQuartal.clampHeight(h, n);
    if (h === state.quartalHeight) return false;
    state.quartalHeight = h;
    state.quartalStartString = window.FretQuartal.clampStartString(start, n, h);
    populateQuartalControls();
    flashCtl($('#ctl-quartal-height'));
    render();
    return true;
  }

  /** Ctrl/⌘↑↓ — move the bass (bottom) end. delta +1 = toward higher strings. */
  function shiftQuartalBottom(delta) {
    if (!state.showQuartal || !window.FretQuartal || !delta) return false;
    const n = state.stringCount;
    const end = state.quartalStartString + state.quartalHeight - 1;
    const minH = Math.min(window.FretQuartal.MIN_HEIGHT, n);
    let start = state.quartalStartString + delta;
    let h = end - start + 1;
    if (h < minH) {
      start = end - minH + 1;
      h = minH;
    }
    if (start < 0) {
      start = 0;
      h = end - start + 1;
    }
    if (start + h - 1 >= n) {
      h = n - start;
    }
    h = window.FretQuartal.clampHeight(h, n);
    start = window.FretQuartal.clampStartString(start, n, h);
    if (start === state.quartalStartString && h === state.quartalHeight) return false;
    const startChanged = start !== state.quartalStartString;
    const heightChanged = h !== state.quartalHeight;
    state.quartalStartString = start;
    state.quartalHeight = h;
    populateQuartalControls();
    if (startChanged) flashCtl($('#ctl-quartal-start'));
    if (heightChanged) flashCtl($('#ctl-quartal-height'));
    render();
    return true;
  }

  function syncNpsForms() {
    if (!window.FretNps) return;
    const ordered = M.scaleOrdered(state.scaleFamily, state.modeIndex) || [];
    const defs = window.FretNps.formDefs(ordered, state.scaleKey);
    const next = {};
    defs.forEach((d) => {
      next[d.key] = state.npsForms[d.key] !== false;
    });
    state.npsForms = next;
    return defs;
  }

  function populateNpsFormButtons() {
    const wrap = $('#nps-form-buttons');
    if (!wrap || !window.FretNps) return;
    const defs = syncNpsForms();
    wrap.innerHTML = '';
    defs.forEach((def) => {
      const on = state.npsForms[def.key] !== false;
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'shape-btn' + (on ? ' on' : '');
      b.style.setProperty('--sc', def.color);
      b.textContent = degreeRoman(def.degreeRel);
      b.title = t('controls.nps.formTitle', { degree: degreeRoman(def.degreeRel) });
      b.addEventListener('click', () => {
        state.npsForms[def.key] = !on;
        populateNpsFormButtons();
        render();
      });
      wrap.appendChild(b);
    });
  }

  function syncBerkleeForms() {
    if (!window.FretBerklee) return;
    const ordered = M.scaleOrdered(state.scaleFamily, state.modeIndex) || [];
    const defs = window.FretBerklee.formDefs(ordered, state.scaleKey);
    const next = {};
    defs.forEach((d) => {
      next[d.key] = state.berkleeForms[d.key] !== false;
    });
    state.berkleeForms = next;
    return defs;
  }

  function populateBerkleeFormButtons() {
    const wrap = $('#berklee-form-buttons');
    if (!wrap || !window.FretBerklee) return;
    const defs = syncBerkleeForms();
    wrap.innerHTML = '';
    defs.forEach((def) => {
      const on = state.berkleeForms[def.key] !== false;
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'shape-btn' + (on ? ' on' : '');
      b.style.setProperty('--sc', def.color);
      b.textContent = degreeRoman(def.degreeRel);
      b.title = t('controls.berklee.formTitle', { degree: degreeRoman(def.degreeRel) });
      b.addEventListener('click', () => {
        state.berkleeForms[def.key] = !on;
        populateBerkleeFormButtons();
        render();
      });
      wrap.appendChild(b);
    });
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
    bindExploreSearch();
    const legendEl = $('#legend');
    if (legendEl && !legendEl.dataset.bound) {
      legendEl.dataset.bound = '1';
      legendEl.addEventListener('change', (e) => {
        const input = e.target;
        if (!input || input.type !== 'checkbox' || !input.closest('.legend-head .switch')) return;
        state.showLegend = !!input.checked;
        render();
      });
      legendEl.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-theory-article]');
        if (!btn || !legendEl.contains(btn)) return;
        e.preventDefault();
        openTheoryArticle(btn.getAttribute('data-theory-article'));
      });
    }
    const tipboxEl = $('#tipbox');
    if (tipboxEl && !tipboxEl.dataset.theoryBound) {
      tipboxEl.dataset.theoryBound = '1';
      tipboxEl.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-theory-article]');
        if (!btn || !tipboxEl.contains(btn)) return;
        e.preventDefault();
        openTheoryArticle(btn.getAttribute('data-theory-article'));
      });
    }
    $('.tabs').addEventListener('click', (e) => {
      const b = e.target.closest('.tab');
      if (!b) return;
      state.tab = b.dataset.tab;
      applyTabUI();
      render();
    });
    $('.tabs').addEventListener('keydown', (e) => {
      const tabs = Array.prototype.slice.call(document.querySelectorAll('.tabs .tab'));
      if (!tabs.length) return;
      const i = tabs.indexOf(document.activeElement);
      if (i < 0) return;
      let next = -1;
      if (e.key === 'ArrowRight' || e.key === 'ArrowDown') next = (i + 1) % tabs.length;
      else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') next = (i - 1 + tabs.length) % tabs.length;
      else if (e.key === 'Home') next = 0;
      else if (e.key === 'End') next = tabs.length - 1;
      if (next < 0) return;
      e.preventDefault();
      state.tab = tabs[next].dataset.tab;
      applyTabUI();
      tabs[next].focus();
      render();
    });
    $('#ctl-names').addEventListener('change', (e) => { state.showNames = e.target.checked; render(); });
    $('#ctl-degrees').addEventListener('change', (e) => { state.showDegrees = e.target.checked; render(); });
    $('#ctl-intervals').addEventListener('change', (e) => { state.showIntervals = e.target.checked; render(); });
    $('#ctl-compound').addEventListener('change', (e) => { state.compoundIntervals = e.target.value === '1'; render(); });
    $('#ctl-show-no5').addEventListener('change', (e) => { state.showNo5 = e.target.checked; render(); });
    $('#ctl-full').addEventListener('change', (e) => { state.fullChart = e.target.checked; render(); });
    $('#ctl-sound').addEventListener('change', (e) => { state.sound = e.target.checked; saveState(); applyTabUI(); });

    $('#ctl-bpm').addEventListener('change', (e) => {
      state.bpm = Math.max(40, Math.min(300, parseInt(e.target.value, 10) || 120));
      e.target.value = String(state.bpm);
      saveState();
    });
    $('#ctl-groove').addEventListener('change', (e) => { state.groove = e.target.value; saveState(); });
    $('#ctl-flats').addEventListener('change', (e) => {
      const v = e.target.value;
      state.noteSpell = v === '2' ? 'fit' : (v === '1' ? 'flat' : 'sharp');
      state.flats = state.noteSpell === 'flat';
      populateSelects();
      render();
    });

    $('#ctl-explore-root').addEventListener('change', (e) => {
      setExploreRoot(parseInt(e.target.value, 10));
      populateSelects();
      render();
    });
    $('#btn-shift-scale-left').addEventListener('click', () => shiftScale(-1));
    $('#btn-shift-scale-right').addEventListener('click', () => shiftScale(1));
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
    $('#btn-shift-mode-prev').addEventListener('click', () => shiftMode(-1));
    $('#btn-shift-mode-next').addEventListener('click', () => shiftMode(1));
    document.addEventListener('keydown', (e) => {
      if (state.tab !== 'explore') return;
      const t = e.target;
      if (t && (t.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(t.tagName))) return;
      const overlay = $('#modal-overlay');
      if (overlay && !overlay.classList.contains('hidden')) return;
      const isUp = e.key === 'ArrowUp';
      const isDown = e.key === 'ArrowDown';
      if ((isUp || isDown) && state.showQuartal) {
        const dir = isUp ? 1 : -1;
        if (e.shiftKey && !e.altKey && !e.ctrlKey && !e.metaKey) {
          e.preventDefault();
          shiftQuartalUpper(dir);
          return;
        }
        if ((e.ctrlKey || e.metaKey) && !e.altKey && !e.shiftKey) {
          e.preventDefault();
          shiftQuartalBottom(dir);
          return;
        }
      }
      if (e.altKey || e.ctrlKey || e.metaKey) return;
      if (e.key === 'ArrowLeft') { e.preventDefault(); shiftScale(-1); }
      else if (e.key === 'ArrowRight') { e.preventDefault(); shiftScale(1); }
      else if (e.key === 'ArrowUp') { e.preventDefault(); shiftMode(-1); }
      else if (e.key === 'ArrowDown') { e.preventDefault(); shiftMode(1); }
    });
    $('#ctl-quartal-height').addEventListener('change', (e) => {
      const raw = parseInt(e.target.value, 10);
      state.quartalHeight = window.FretQuartal
        ? window.FretQuartal.clampHeight(raw, state.stringCount)
        : Math.max(3, Math.min(state.stringCount, raw || 3));
      if (window.FretQuartal) {
        state.quartalStartString = window.FretQuartal.clampStartString(
          state.quartalStartString, state.stringCount, state.quartalHeight
        );
      }
      populateQuartalStartSelect();
      render();
    });
    $('#ctl-quartal-start').addEventListener('change', (e) => {
      state.quartalStartString = parseInt(e.target.value, 10) || 0;
      if (window.FretQuartal) {
        state.quartalStartString = window.FretQuartal.clampStartString(
          state.quartalStartString, state.stringCount, state.quartalHeight
        );
      }
      render();
    });
    $('#ctl-quartal-repeats').addEventListener('change', (e) => {
      state.quartalRepeats = e.target.checked;
      render();
    });
    const npsRepeatsCtl = $('#ctl-nps-repeats');
    if (npsRepeatsCtl) npsRepeatsCtl.addEventListener('change', (e) => {
      state.npsRepeats = e.target.checked;
      render();
    });
    const npsCountCtl = $('#ctl-nps-count');
    if (npsCountCtl) npsCountCtl.addEventListener('change', (e) => {
      state.npsCount = window.FretNps
        ? window.FretNps.clampCount(e.target.value)
        : 3;
      render();
    });
    const berkleeRepeatsCtl = $('#ctl-berklee-repeats');
    if (berkleeRepeatsCtl) berkleeRepeatsCtl.addEventListener('change', (e) => {
      state.berkleeRepeats = e.target.checked;
      render();
    });
    $('#ctl-caged-repeats').addEventListener('change', (e) => { state.cagedRepeats = e.target.checked; render(); });
    $('#ctl-char-tips').addEventListener('change', (e) => { state.showCharTips = e.target.checked; render(); });
    $('#ctl-char-originals').addEventListener('change', (e) => { state.showCharOriginals = e.target.checked; render(); });
    $('#ctl-highlight-degree').addEventListener('change', (e) => {
      const v = parseInt(e.target.value, 10);
      state.highlightDegree = v === -1 ? -1 : (((v % 12) + 12) % 12);
      render();
    });
    $('#ctl-rootlines').addEventListener('change', (e) => { state.showRootLines = e.target.checked; render(); });

    $('#btn-chord-clear').addEventListener('click', () => {
      state.chordNotes = [];
      state.pinRoot = null;
      ui.scalePropFollow = true;
      ui.scaleSel = null;
      ui.chordScaleFocus = null;
      render();
    });

    $('#btn-chord-play').addEventListener('click', () => {
      boardView.playChord();
    });

    const kbString = $('#ctl-kb-string');
    const kbFret = $('#ctl-kb-fret');
    if (kbString) {
      kbString.addEventListener('change', () => {
        const s = parseInt(kbString.value, 10);
        const f = kbFret ? parseInt(kbFret.value, 10) : boardView.getKbCursor().f;
        boardView.setKbCursor(s, f, { fromControls: true, silent: true });
        syncKbPlaceControls();
      });
    }
    if (kbFret) {
      const applyKbFret = () => {
        const raw = kbFret.value;
        if (raw === '' || raw === '-') return;
        let f = parseInt(raw, 10);
        if (!Number.isFinite(f)) return;
        const s = kbString ? parseInt(kbString.value, 10) : boardView.getKbCursor().s;
        boardView.setKbCursor(s, f, { fromControls: true, silent: true });
        syncKbPlaceControls();
      };
      kbFret.addEventListener('change', applyKbFret);
      kbFret.addEventListener('input', applyKbFret);
    }
    const btnKbPlace = $('#btn-kb-place');
    if (btnKbPlace) {
      btnKbPlace.addEventListener('click', () => {
        const c = boardView.getKbCursor();
        boardView.placeAt(c.s, c.f);
      });
    }
    const btnKbRemove = $('#btn-kb-remove');
    if (btnKbRemove) {
      btnKbRemove.addEventListener('click', () => {
        const c = boardView.getKbCursor();
        boardView.removeAt(c.s);
      });
    }
    if (boardView.setKbCursorListener) {
      boardView.setKbCursorListener(() => {
        syncKbPlaceControls();
      });
    }

    boardView.bindNeckEvents();
    pianoView.bindPianoEvents();
    chordUi.bindChordResultEvents();
    scaleDetails.bindScaleDetailsEvents();
    profiles.bind();
    theory.bind();
    drills.bind();
    bindGuitarPicker();
    bindLayerPicker();

    window.addEventListener('beforeunload', () => {
      if (saveTimer) {
        clearTimeout(saveTimer);
        saveTimer = null;
        saveState();
      }
    });
  }

  function createModules() {
    chordUi = window.FretChordUi.create({
      $: $,
      t: t,
      M: M,
      state: state,
      ui: ui,
      names: names,
      degreeRoman: degreeRoman,
      tuningMidis: tuningMidis,
      scaleDisplayName: scaleDisplayName,
      renderScaleClusters: renderScaleClusters,
      applySharedScale: applySharedScale,
      render: render,
      saveState: saveState,
      playSimultaneous: playSimultaneous,
      playChordSimultaneous: () => boardView.playChordSimultaneous(),
      openTheoryArticle: openTheoryArticle,
      showTip: (html, x, y) => boardView.showTip(html, x, y),
      hideTip: () => boardView.hideTip(),
      positionTip: (x, y) => boardView.positionTip(x, y),
      tipVisible: () => boardView.tipVisible(),
      tipRow: (key, val, valCls) => boardView.tipRow(key, val, valCls)
    });

    scaleDetails = window.FretScaleDetails.create({
      $: $,
      t: t,
      M: M,
      state: state,
      ui: ui,
      names: names,
      theoryInfo: theoryInfo,
      degreeRoman: degreeRoman,
      tuningMidis: tuningMidis,
      scaleDisplayName: scaleDisplayName,
      placeChordPcs: chordUi.placeChordPcs,
      chordDiagramSvg: chordUi.chordDiagramSvg,
      chordDisplaySuffix: chordUi.chordDisplaySuffix,
      chordSymbolStackHtml: chordUi.chordSymbolStackHtml,
      encodeVoicing: chordUi.encodeVoicing,
      decodeVoicing: chordUi.decodeVoicing,
      midisFromPcsAscending: chordUi.midisFromPcsAscending,
      playBlocks: playBlocks,
      playSimultaneous: playSimultaneous,
      applySharedScale: applySharedScale,
      openScaleChordInAnalyzer: openScaleChordInAnalyzer,
      openTheoryArticle: openTheoryArticle,
      saveState: saveState
    });

    boardView = window.FretBoardView.create({
      $: $,
      t: t,
      M: M,
      G: G,
      state: state,
      ui: ui,
      names: names,
      degreeRoman: degreeRoman,
      tuningMidis: tuningMidis,
      rootPc: rootPc,
      freshShapes: freshShapes,
      playMidi: playMidi,
      playSequence: playSequence,
      playSimultaneous: playSimultaneous,
      render: render,
      getChordScales: (r, keyPc) => chordUi.getChordScales(r, keyPc),
      proposalKeyPc: (r) => chordUi.proposalKeyPc(r),
      getDrillMode: () => (drills && drills.mode ? drills.mode() : null),
      drillNotesLocked: () => (drills && drills.notesLocked ? drills.notesLocked() : false),
      drillAnswerRevealed: () => (drills && drills.answerRevealed ? drills.answerRevealed() : false),
      getTheoryBoardMode: () => (state.tab === 'theory' ? ui.theoryBoardMode : null),
      onDrillNeckClick: (s, f, opts) => (drills && drills.onNeckClick ? drills.onNeckClick(s, f, opts) : false)
    });

    pianoView = window.FretPianoView.create({
      $: $,
      t: t,
      M: M,
      state: state,
      names: names,
      degreeRoman: degreeRoman,
      rootPc: rootPc,
      tuningMidis: tuningMidis,
      playMidi: playMidi,
      render: render,
      showTip: (html, x, y) => boardView.showTip(html, x, y),
      hideTip: () => boardView.hideTip(),
      positionTip: (x, y) => boardView.positionTip(x, y),
      tipVisible: () => boardView.tipVisible(),
      tipRow: (key, val, valCls) => boardView.tipRow(key, val, valCls)
    });

    theory = window.FretTheory.create({
      $: $,
      t: t,
      esc: esc,
      openLesson: openTheoryLesson,
      applyDemo: applyTheoryDemo,
      onNavigate: () => scheduleSave(),
      placeChordPcs: (...args) => chordUi.placeChordPcs(...args),
      miniNeckSvg: (...args) => chordUi.miniNeckSvg(...args),
      staffSvg: (...args) => chordUi.staffSvg(...args),
      staffNotesToMidi: (...args) => chordUi.staffNotesToMidi(...args),
      decodeVoicing: (...args) => chordUi.decodeVoicing(...args),
      playSimultaneous: playSimultaneous,
      playMidi: playMidi,
      tuningMidis: tuningMidis,
      names: names,
      state: state
    });

    drills = window.FretDrills.create({
      $: $,
      t: t,
      M: M,
      state: state,
      ui: ui,
      names: names,
      esc: esc,
      tuningMidis: tuningMidis,
      placeChordPcs: (...args) => chordUi.placeChordPcs(...args),
      midisFromPcsAscending: (...args) => chordUi.midisFromPcsAscending(...args),
      playMidi: playMidi,
      playSimultaneous: playSimultaneous,
      render: render,
      applyTabUI: applyTabUI,
      saveState: saveState,
      setExploreRoot: setExploreRoot,
      populateSelects: populateSelects,
      degreeRoman: degreeRoman,
      peekDrillsDisplay: peekDrillsDisplay,
      relockDrillsDisplay: relockDrillsDisplay
    });

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
  }

  function init() {
    createModules();
    loadState();
    applyDeepLinkFromLocation();
    syncControls();
    applyTabUI();
    populateSelects();
    bindEvents();
    bindDeepLink();
    SHAPE_DEFS.forEach((d) => freshShapes.add(d.key));
    if (pendingTheoryArticle && theory) {
      theory.openArticle(pendingTheoryArticle);
      pendingTheoryArticle = null;
    }
    render();
    writeDeepLink();
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
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', t === 'light' ? '#eef1f6' : '#0e1016');
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
