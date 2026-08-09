(function (root) {
  const SHAPE_DEFS = [
    { key: 'C', color: '#ff6b6b', pos2: 4, roots: '5th & 2nd strings', rootStrings: [1, 4] },
    { key: 'A', color: '#ffa94d', pos2: 1, roots: '5th & 3rd strings', rootStrings: [1, 3] },
    { key: 'G', color: '#51cf66', pos2: 3, roots: '6th, 3rd & 1st strings', rootStrings: [0, 3, 5] },
    { key: 'E', color: '#4dabf7', pos2: 0, roots: '6th & 1st strings', rootStrings: [0, 2, 5] },
    { key: 'D', color: '#b197fc', pos2: 2, roots: '4th & 2nd strings', rootStrings: [2, 4] }
  ];

  function computeData(opts) {
    const M = opts.M;
    const midis = opts.midis;
    const n = midis.length;
    const N = opts.fretCount;
    const root = opts.root;
    const markers = [];
    const boxes = [];
    const taken = {};
    const overlap = {};
    let boxSeq = 0;
    const boxKeep = {};

    const ordered = M.scaleOrdered(opts.scaleFamily, opts.modeIndex);
    if (opts.showCagedShapes) {
      const scalePcs = new Set(ordered.map((v) => (root + v) % 12));
      const chordPcs = [(root + ordered[0]) % 12, (root + ordered[2]) % 12, (root + ordered[4]) % 12];
      const shapes = SHAPE_DEFS.filter((s) => opts.shapes[s.key]);
      const core = Math.max(0, n - 6);

      const hasAllSteps = (lo, hi) => {
        for (const pc of scalePcs) {
          let found = false;
          for (let s = 0; s < n && !found; s++) {
            for (let fr = lo; fr <= hi && !found; fr++) {
              if (((midis[s] + fr) % 12 + 12) % 12 === pc) found = true;
            }
          }
          if (!found) return false;
        }
        return true;
      };

      const pruneDuplicates = (notes) => {
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
      };

      const addShape = (def, pos2, f) => {
        const lo = f - 1;
        const hi = lo + 4;
        if (lo < 0 || hi > N) return;
        if (!hasAllSteps(lo, hi)) return;
        const minS = 0;
        const maxS = n - 1;
        const notes = [];
        const bid = boxSeq++;
        for (let s = minS; s <= maxS; s++) {
          for (let fr = lo; fr <= hi; fr++) {
            const pc = ((midis[s] + fr) % 12 + 12) % 12;
            if (!scalePcs.has(pc)) continue;
            const keyId = s + ':' + fr;
            if (!overlap[keyId]) overlap[keyId] = [];
            overlap[keyId].push({ color: def.color, lo: lo, key: def.key, id: bid });
            notes.push({ m: midis[s] + fr, s: s, f: fr });
          }
        }
        notes.sort((a, b) => a.m - b.m);
        const pruned = pruneDuplicates(notes);
        boxKeep[bid] = pruned.keep;
        const rootNotes = [];
        def.rootStrings.forEach((rs) => {
          const s = rs + core;
          if (s < 0 || s >= n) return;
          let fr = ((root - midis[s]) % 12 + 12) % 12;
          while (fr < lo) fr += 12;
          if (fr <= hi) rootNotes.push({ s: s, f: fr });
        });
        let visLo = hi, visHi = lo;
        for (const nt of pruned.notes) {
          if (nt.f < visLo) visLo = nt.f;
          if (nt.f > visHi) visHi = nt.f;
        }
        boxes.push({
          key: def.key,
          color: def.color,
          lo: lo,
          hi: hi,
          visLo: visLo,
          visHi: visHi,
          roots: rootNotes,
          minS: minS,
          maxS: maxS,
          notes: pruned.notes,
          keep: pruned.keep,
          rootMidi: midis[pos2] + f
        });
      };

      shapes.forEach((def) => {
        const pos2 = core + def.pos2;
        if (pos2 >= n) return;
        const base = ((root - midis[pos2] % 12) % 12 + 12) % 12;
        for (let f = base; f <= N; f += 12) {
          addShape(def, pos2, f);
        }
      });

      Object.keys(overlap).forEach((keyId) => {
        const hits = overlap[keyId].slice().sort((a, b) => (a.lo - b.lo) || (a.id - b.id));
        const parts = keyId.split(':');
        const s = parseInt(parts[0], 10);
        const fr = parseInt(parts[1], 10);
        const pc = ((midis[s] + fr) % 12 + 12) % 12;
        taken[keyId] = true;
        let cls = 'scale';
        if (pc === root) cls = 'root';
        else if (opts.showChordTones && chordPcs.indexOf(pc) >= 0) cls = 'chord';
        const kept = hits.some((h) => boxKeep[h.id] && boxKeep[h.id].has(keyId));
        const shapeKeys = hits.map((h) => h.key);
        if (cls === 'root') {
          markers.push({
            pc: pc, fret: fr, stringIdx: s, cls: kept ? 'root' : 'root soft',
            shapes: shapeKeys
          });
          return;
        }
        const soft = kept ? '' : ' soft';
        if (hits.length >= 2) {
          markers.push({
            pc: pc, fret: fr, stringIdx: s, cls: cls + ' split' + soft,
            color: hits[0].color, colorR: hits[hits.length - 1].color,
            shapes: shapeKeys
          });
        } else {
          markers.push({
            pc: pc, fret: fr, stringIdx: s, cls: cls + soft,
            color: hits[0].color, shapes: shapeKeys
          });
        }
      });

      if (opts.fullChart) {
        const boxRanges = boxes.map((b) => ({ lo: b.visLo, hi: b.visHi }));
        for (let s = 0; s < n; s++) {
          for (let fr = 0; fr <= N; fr++) {
            const keyId = s + ':' + fr;
            if (taken[keyId]) continue;
            const pc = ((midis[s] + fr) % 12 + 12) % 12;
            const inScale = scalePcs.has(pc);
            const inBox = boxRanges.some((r) => fr >= r.lo && fr <= r.hi);
            let cls = 'plain';
            if (inScale) {
              cls = pc === root ? 'root' : (opts.showChordTones && chordPcs.indexOf(pc) >= 0 ? 'chord' : 'scale');
            }
            if (!inBox) cls += ' soft';
            markers.push({ pc: pc, fret: fr, stringIdx: s, cls: cls });
          }
        }
      }
    } else if (opts.tab === 'chords') {
      return { markers: [], boxes: [], ordered: [], pcs: new Set() };
    } else {
      const pcs = new Set(ordered.map((v) => (root + v) % 12));
      const triad = new Set([ordered[0], ordered[2], ordered[4]].map((v) => (root + v) % 12));
      for (let s = 0; s < n; s++) {
        for (let fr = 0; fr <= N; fr++) {
          const pc = ((midis[s] + fr) % 12 + 12) % 12;
          const inScale = pcs.has(pc);
          if (!inScale && !opts.fullChart) continue;
          let cls = 'plain';
          if (inScale) {
            cls = pc === root ? 'root' : (opts.showChordTones && triad.has(pc) ? 'chord' : 'scale');
          }
          markers.push({ pc: pc, fret: fr, stringIdx: s, cls: cls });
        }
      }
      return { markers: markers, boxes: boxes, ordered: ordered, pcs: pcs };
    }

    const pcs = new Set(ordered.map((v) => (root + v) % 12));
    return { markers: markers, boxes: boxes, ordered: ordered, pcs: pcs };
  }

  root.FretCaged = {
    SHAPE_DEFS: SHAPE_DEFS,
    computeData: computeData
  };
})(typeof self !== 'undefined' ? self : this);
