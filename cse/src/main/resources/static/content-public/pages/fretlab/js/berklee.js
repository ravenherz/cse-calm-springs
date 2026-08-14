(function (root) {
  const COLORS = ['#ff6b6b', '#ffa94d', '#51cf66', '#4dabf7', '#b197fc', '#f06595', '#20c997'];
  /** Position span in frets (Berklee 4-fret hand + stretch room), same width as CAGED boxes. */
  const WINDOW = 5;

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

  function hasAllSteps(midis, fretCount, scalePcs, lo, hi) {
    for (const pc of scalePcs) {
      let found = false;
      for (let s = 0; s < midis.length && !found; s++) {
        for (let fr = lo; fr <= hi && !found; fr++) {
          if (((midis[s] + fr) % 12 + 12) % 12 === pc) found = true;
        }
      }
      if (!found) return false;
    }
    return true;
  }

  function pruneDuplicates(notes) {
    const span = {};
    for (const nt of notes) {
      if (!span[nt.s]) span[nt.s] = { lo: nt.f, hi: nt.f };
      else {
        if (nt.f < span[nt.s].lo) span[nt.s].lo = nt.f;
        if (nt.f > span[nt.s].hi) span[nt.s].hi = nt.f;
      }
    }
    const strDist = {};
    for (const s in span) strDist[s] = span[s].hi - span[s].lo;
    const byMidi = {};
    for (const nt of notes) {
      (byMidi[nt.m] = byMidi[nt.m] || []).push(nt);
    }
    const keep = new Set();
    const out = [];
    Object.keys(byMidi).forEach((m) => {
      const grp = byMidi[m];
      if (grp.length === 1) {
        keep.add(grp[0].s + ':' + grp[0].f);
        out.push(grp[0]);
        return;
      }
      grp.sort((a, b) => (strDist[a.s] - strDist[b.s]) || (a.s - b.s));
      keep.add(grp[0].s + ':' + grp[0].f);
      out.push(grp[0]);
    });
    return { notes: out, keep: keep };
  }

  function buildPosition(midis, fretCount, scalePcs, lo) {
    const hi = lo + WINDOW - 1;
    if (lo < 0 || hi > fretCount) return null;
    if (!hasAllSteps(midis, fretCount, scalePcs, lo, hi)) return null;
    const n = midis.length;
    const notes = [];
    for (let s = 0; s < n; s++) {
      for (let fr = lo; fr <= hi; fr++) {
        const pc = ((midis[s] + fr) % 12 + 12) % 12;
        if (!scalePcs.has(pc)) continue;
        notes.push({ s: s, f: fr, m: midis[s] + fr, pc: pc });
      }
    }
    if (!notes.length) return null;
    notes.sort((a, b) => a.m - b.m);
    const pruned = pruneDuplicates(notes);
    let visLo = hi;
    let visHi = lo;
    for (let i = 0; i < pruned.notes.length; i++) {
      if (pruned.notes[i].f < visLo) visLo = pruned.notes[i].f;
      if (pruned.notes[i].f > visHi) visHi = pruned.notes[i].f;
    }
    return {
      notes: pruned.notes,
      keep: pruned.keep,
      lo: lo,
      hi: hi,
      visLo: visLo,
      visHi: visHi,
      minS: 0,
      maxS: n - 1,
      rootMidi: pruned.notes[0].m,
      startFret: lo
    };
  }

  function computePatterns(opts) {
    const M = opts.M;
    const midis = opts.midis;
    const N = opts.fretCount;
    const root = ((opts.root % 12) + 12) % 12;
    const repeats = opts.repeats !== false;
    const forms = opts.forms || null;
    const ordered = M.scaleOrdered(opts.scaleFamily, opts.modeIndex) || [];
    const patterns = [];
    const defs = formDefs(ordered, root);
    if (!midis || !midis.length || !ordered.length) {
      return { patterns: patterns, repeats: repeats, forms: defs };
    }

    const scalePcs = new Set(ordered.map((v) => (root + v) % 12));
    const bass = midis[0];
    const seenStart = {};

    ordered.forEach((v, si) => {
      const startPc = (root + v) % 12;
      if (seenStart[startPc]) return;
      seenStart[startPc] = true;
      const degreeRel = (startPc - root + 12) % 12;
      const formKey = String(degreeRel);
      if (forms && forms[formKey] === false) return;

      const baseFret = ((startPc - (bass % 12) + 12) % 12);
      const color = COLORS[si % COLORS.length];
      for (let f = baseFret; f <= N; f += 12) {
        const pos = buildPosition(midis, N, scalePcs, f);
        if (!pos) continue;
        patterns.push({
          key: formKey,
          degreeIndex: si,
          degreeRel: degreeRel,
          color: color,
          notes: pos.notes,
          keep: pos.keep,
          lo: pos.lo,
          hi: pos.hi,
          visLo: pos.visLo,
          visHi: pos.visHi,
          minS: pos.minS,
          maxS: pos.maxS,
          rootMidi: pos.rootMidi,
          startFret: pos.startFret
        });
        if (!repeats) break;
      }
    });

    patterns.sort((a, b) => (a.startFret - b.startFret) || (a.degreeRel - b.degreeRel));
    return { patterns: patterns, repeats: repeats, forms: defs };
  }

  root.FretBerklee = {
    computePatterns: computePatterns,
    formDefs: formDefs,
    WINDOW: WINDOW,
    COLORS: COLORS
  };
})(typeof window !== 'undefined' ? window : this);
