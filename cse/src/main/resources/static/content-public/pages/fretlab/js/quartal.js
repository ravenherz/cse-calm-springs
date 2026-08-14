(function (root) {
  const COLORS = ['#20c997', '#22b8cf', '#748ffc', '#845ef7', '#e64980', '#f59f00', '#94d82d'];
  const MIN_HEIGHT = 3;

  function nextFourth(pc, scalePcs) {
    for (let i = 0; i < 3; i++) {
      const prefer = [5, 6, 4][i];
      const target = (pc + prefer) % 12;
      if (scalePcs.has(target)) return target;
    }
    return null;
  }

  function stackFrom(bassPc, scalePcs, height) {
    const stack = [bassPc];
    let cur = bassPc;
    for (let i = 1; i < height; i++) {
      const next = nextFourth(cur, scalePcs);
      if (next == null) return null;
      stack.push(next);
      cur = next;
    }
    return stack;
  }

  function clampHeight(height, stringCount) {
    const n = Math.max(1, stringCount | 0);
    const minH = Math.min(MIN_HEIGHT, n);
    let h = height | 0;
    if (h < minH) h = minH;
    if (h > n) h = n;
    return h;
  }

  function heightOptions(stringCount) {
    const n = Math.max(1, stringCount | 0);
    const minH = Math.min(MIN_HEIGHT, n);
    const out = [];
    for (let h = minH; h <= n; h++) out.push(h);
    return out;
  }

  function defaultStartString(stringCount, height) {
    const n = stringCount | 0;
    const h = clampHeight(height, n);
    if (n < h) return 0;
    const core = Math.max(0, n - 6);
    const mid = core + 1;
    if (mid + h - 1 < n) return mid;
    return 0;
  }

  function clampStartString(start, stringCount, height) {
    const n = stringCount | 0;
    const h = clampHeight(height, n);
    const maxStart = Math.max(0, n - h);
    let s = start | 0;
    if (s < 0) s = 0;
    if (s > maxStart) s = maxStart;
    return s;
  }

  function maxSpanFor(height) {
    return Math.max(2, (height | 0) - 1);
  }

  function bestVoicing(midis, fretCount, stack, startString, maxSpan) {
    const all = collectVoicings(midis, fretCount, stack, startString, maxSpan, 1);
    return all.length ? all[0] : null;
  }

  /**
   * All compact ascending voicings for a fixed PC stack on consecutive strings,
   * ranked by compactness (low frets, small span). Caps at `limit`.
   * Optional `maxFret` limits the fretting range (e.g. 11 to skip octave copies).
   */
  function collectVoicings(midis, fretCount, stack, startString, maxSpan, limit, maxFret) {
    const nNotes = stack.length;
    if (nNotes < 1 || startString < 0 || startString + nNotes > midis.length) return [];
    const strings = [];
    for (let i = 0; i < nNotes; i++) strings.push(startString + i);
    const found = [];
    const maxKeep = limit != null ? limit : 12;
    const fHi = maxFret != null ? Math.min(fretCount, maxFret) : fretCount;

    function search(idx, frets, lo, hi) {
      if (idx === nNotes) {
        const notes = [];
        for (let i = 0; i < nNotes; i++) {
          const s = strings[i];
          const f = frets[i];
          notes.push({ m: midis[s] + f, s: s, f: f });
        }
        for (let i = 1; i < nNotes; i++) {
          if (!(notes[i - 1].m < notes[i].m)) return;
        }
        const span = hi - lo;
        let sum = 0;
        for (let i = 0; i < nNotes; i++) sum += frets[i];
        const score = frets[0] * 100 + span * 10 + sum;
        found.push({
          score: score,
          notes: notes,
          rootMidi: notes[0].m,
          bassFret: frets[0],
          startString: startString
        });
        return;
      }
      const s = strings[idx];
      const want = stack[idx];
      for (let f = 0; f <= fHi; f++) {
        if (((midis[s] + f) % 12 + 12) % 12 !== want) continue;
        const nLo = idx === 0 ? f : Math.min(lo, f);
        const nHi = idx === 0 ? f : Math.max(hi, f);
        if (nHi - nLo > maxSpan) continue;
        frets[idx] = f;
        search(idx + 1, frets, nLo, nHi);
      }
    }

    search(0, new Array(nNotes), 0, 0);
    found.sort((a, b) => a.score - b.score);
    return found.slice(0, maxKeep);
  }

  function shiftVoicing(found, oct) {
    const notes = found.notes.map((nt) => ({
      m: nt.m + oct * 12,
      s: nt.s,
      f: nt.f + oct * 12
    }));
    for (let i = 0; i < notes.length; i++) {
      if (notes[i].f < 0) return null;
    }
    return {
      notes: notes,
      rootMidi: found.rootMidi + oct * 12,
      bassFret: found.bassFret + oct * 12
    };
  }

  function computeVoicings(opts) {
    const M = opts.M;
    const midis = opts.midis;
    const n = midis.length;
    const N = opts.fretCount;
    const root = opts.root;
    const height = clampHeight(opts.height, n);
    const start = clampStartString(opts.startString, n, height);
    const repeats = opts.repeats !== false;
    if (n < height) return { voicings: [], startString: start, height: height, repeats: repeats };

    const ordered = M.scaleOrdered(opts.scaleFamily, opts.modeIndex);
    const scalePcs = new Set(ordered.map((v) => (root + v) % 12));
    const span = maxSpanFor(height);
    const voicings = [];
    const usedBass = {};

    ordered.forEach((v, si) => {
      const bass = (root + v) % 12;
      if (usedBass[bass]) return;
      const stack = stackFrom(bass, scalePcs, height);
      if (!stack) return;
      usedBass[bass] = true;
      const found = bestVoicing(midis, N, stack, start, span);
      if (!found) return;
      const color = COLORS[si % COLORS.length];
      const bassRel = (stack[0] - root + 12) % 12;
      const maxOct = repeats ? Math.floor((N - found.bassFret) / 12) : 0;
      for (let oct = 0; oct <= maxOct; oct++) {
        const shifted = shiftVoicing(found, oct);
        if (!shifted) continue;
        let maxF = 0;
        for (let i = 0; i < shifted.notes.length; i++) {
          if (shifted.notes[i].f > maxF) maxF = shifted.notes[i].f;
        }
        if (maxF > N) continue;
        voicings.push({
          bassRel: bassRel,
          color: color,
          notes: shifted.notes,
          rootMidi: shifted.rootMidi,
          bassFret: shifted.bassFret,
          startString: start
        });
      }
    });

    voicings.sort((a, b) => (a.bassFret - b.bassFret) || (a.bassRel - b.bassRel));
    return { voicings: voicings, startString: start, height: height, repeats: repeats };
  }

  function computeBoxes(opts) {
    const r = computeVoicings(opts);
    return { boxes: [], voicings: r.voicings, startString: r.startString, height: r.height };
  }

  root.FretQuartal = {
    computeVoicings: computeVoicings,
    computeBoxes: computeBoxes,
    bestVoicing: bestVoicing,
    collectVoicings: collectVoicings,
    shiftVoicing: shiftVoicing,
    defaultStartString: defaultStartString,
    clampStartString: clampStartString,
    clampHeight: clampHeight,
    heightOptions: heightOptions,
    MIN_HEIGHT: MIN_HEIGHT,
    COLORS: COLORS
  };
})(typeof window !== 'undefined' ? window : this);
