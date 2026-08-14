(function (root) {
  const COLORS = ['#ff6b6b', '#ffa94d', '#51cf66', '#4dabf7', '#b197fc', '#f06595', '#20c997'];
  const COUNT_OPTIONS = [3, 4];
  const MIN_COUNT = 3;
  const MAX_COUNT = 4;
  const DEFAULT_COUNT = 3;

  function clampCount(n) {
    const v = parseInt(n, 10);
    if (!isFinite(v)) return DEFAULT_COUNT;
    return Math.max(MIN_COUNT, Math.min(MAX_COUNT, v));
  }

  function placeOnString(openMidi, fretCount, pcs, minFret) {
    const frets = [];
    let minF = Math.max(0, minFret | 0);
    for (let i = 0; i < pcs.length; i++) {
      const want = pcs[i];
      let found = -1;
      for (let f = minF; f <= fretCount; f++) {
        if (((openMidi + f) % 12 + 12) % 12 === want) {
          found = f;
          break;
        }
      }
      if (found < 0) return null;
      frets.push(found);
      minF = found + 1;
    }
    return frets;
  }

  function buildPattern(midis, fretCount, ordered, root, startIndex, notesPerString) {
    const n = midis.length;
    const len = ordered.length;
    const per = clampCount(notesPerString);
    if (n < 1 || len < 1) return null;
    const notes = [];
    let idx = startIndex % len;
    let prevMidi = -Infinity;

    for (let s = 0; s < n; s++) {
      const pcs = [];
      for (let k = 0; k < per; k++) {
        pcs.push((root + ordered[(idx + k) % len]) % 12);
      }
      const minFret = prevMidi === -Infinity ? 0 : Math.max(0, (prevMidi + 1) - midis[s]);
      const frets = placeOnString(midis[s], fretCount, pcs, minFret);
      if (!frets) return null;
      for (let k = 0; k < per; k++) {
        const f = frets[k];
        const m = midis[s] + f;
        notes.push({ s: s, f: f, m: m, pc: pcs[k] });
        prevMidi = m;
      }
      idx = (idx + per) % len;
    }

    let lo = notes[0].f;
    let hi = notes[0].f;
    for (let i = 1; i < notes.length; i++) {
      if (notes[i].f < lo) lo = notes[i].f;
      if (notes[i].f > hi) hi = notes[i].f;
    }
    return {
      notes: notes,
      lo: lo,
      hi: hi,
      minS: 0,
      maxS: n - 1,
      rootMidi: notes[0].m,
      startFret: notes[0].f
    };
  }

  function shiftPattern(pat, oct) {
    const notes = pat.notes.map((nt) => ({
      s: nt.s,
      f: nt.f + oct * 12,
      m: nt.m + oct * 12,
      pc: nt.pc
    }));
    for (let i = 0; i < notes.length; i++) {
      if (notes[i].f < 0) return null;
    }
    return {
      notes: notes,
      lo: pat.lo + oct * 12,
      hi: pat.hi + oct * 12,
      minS: pat.minS,
      maxS: pat.maxS,
      rootMidi: pat.rootMidi + oct * 12,
      startFret: pat.startFret + oct * 12
    };
  }

  function formDefs(ordered, root) {
    const defs = [];
    const seen = {};
    const r = ((root % 12) + 12) % 12;
    (ordered || []).forEach((v, si) => {
      const startPc = (r + v) % 12;
      if (seen[startPc]) return;
      seen[startPc] = true;
      const degreeRel = (startPc - r + 12) % 12;
      defs.push({
        key: String(degreeRel),
        degreeRel: degreeRel,
        degreeIndex: si,
        color: COLORS[si % COLORS.length]
      });
    });
    return defs;
  }

  function computePatterns(opts) {
    const M = opts.M;
    const midis = opts.midis;
    const N = opts.fretCount;
    const root = ((opts.root % 12) + 12) % 12;
    const repeats = opts.repeats !== false;
    const forms = opts.forms || null;
    const per = clampCount(opts.notesPerString);
    const ordered = M.scaleOrdered(opts.scaleFamily, opts.modeIndex) || [];
    const patterns = [];
    if (!midis || !midis.length || !ordered.length) {
      return { patterns: patterns, repeats: repeats, notesPerString: per, forms: formDefs(ordered, root) };
    }

    const seenStart = {};
    ordered.forEach((v, si) => {
      const startPc = (root + v) % 12;
      if (seenStart[startPc]) return;
      seenStart[startPc] = true;
      const degreeRel = (startPc - root + 12) % 12;
      const formKey = String(degreeRel);
      if (forms && forms[formKey] === false) return;
      const base = buildPattern(midis, N, ordered, root, si, per);
      if (!base) return;
      const color = COLORS[si % COLORS.length];
      const maxOct = repeats ? Math.floor((N - base.startFret) / 12) : 0;
      for (let oct = 0; oct <= maxOct; oct++) {
        const shifted = shiftPattern(base, oct);
        if (!shifted) continue;
        if (shifted.hi > N) continue;
        patterns.push({
          key: formKey,
          degreeIndex: si,
          degreeRel: degreeRel,
          color: color,
          notes: shifted.notes,
          lo: shifted.lo,
          hi: shifted.hi,
          visLo: shifted.lo,
          visHi: shifted.hi,
          minS: shifted.minS,
          maxS: shifted.maxS,
          rootMidi: shifted.rootMidi,
          startFret: shifted.startFret
        });
      }
    });

    patterns.sort((a, b) => (a.startFret - b.startFret) || (a.degreeRel - b.degreeRel));
    return { patterns: patterns, repeats: repeats, notesPerString: per, forms: formDefs(ordered, root) };
  }

  root.FretNps = {
    computePatterns: computePatterns,
    formDefs: formDefs,
    clampCount: clampCount,
    COUNT_OPTIONS: COUNT_OPTIONS,
    MIN_COUNT: MIN_COUNT,
    MAX_COUNT: MAX_COUNT,
    DEFAULT_COUNT: DEFAULT_COUNT,
    COLORS: COLORS
  };
})(typeof window !== 'undefined' ? window : this);
