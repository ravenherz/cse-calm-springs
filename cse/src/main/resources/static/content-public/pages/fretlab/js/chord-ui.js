(function () {
  function midisFromPcsAscending(pcs, base) {
    const b = base != null ? base : 48;
    let last = -1;
    return pcs.map((pc) => {
      let m = b + (((pc % 12) + 12) % 12);
      while (m <= last) m += 12;
      last = m;
      return m;
    });
  }

  function permutePcs(arr) {
    if (arr.length <= 1) return [arr.slice()];
    const out = [];
    for (let i = 0; i < arr.length; i++) {
      const rest = arr.slice(0, i).concat(arr.slice(i + 1));
      const tails = permutePcs(rest);
      for (let j = 0; j < tails.length; j++) out.push([arr[i]].concat(tails[j]));
    }
    return out;
  }

  /** All unique pitch classes in the analyzed chord (no tone dropping). */
  function shapePcsFromChord(r) {
    if (!r || !r.matched || r.rootPc == null) return [];
    const root = ((r.rootPc % 12) + 12) % 12;
    const seen = {};
    const pcs = [];
    function add(pc) {
      pc = ((pc % 12) + 12) % 12;
      if (seen[pc]) return;
      seen[pc] = true;
      pcs.push(pc);
    }
    if (r.notes && r.notes.length) {
      r.notes.forEach((nt) => add(nt.pc));
    } else if (r.intervals && r.intervals.length) {
      r.intervals.forEach((iv) => add(root + iv));
    }
    if (!seen[root]) pcs.unshift(root);
    return pcs.length >= 2 ? pcs : [];
  }

  function voicingFingerprint(notes) {
    return notes.map((nt) => nt.s + ':' + nt.f).sort().join('|');
  }

  function voicingHasAllPcs(notes, midis, pcs) {
    const have = {};
    notes.forEach((nt) => {
      have[(((midis[nt.s] + nt.f) % 12) + 12) % 12] = true;
    });
    for (let i = 0; i < pcs.length; i++) {
      if (!have[pcs[i]]) return false;
    }
    return true;
  }

  /** Stacks that keep every chord tone — full perms when small; rotations when larger. */
  function chordToneStacks(pcs, rootPc) {
    const stacks = [];
    const stackKeys = {};
    function addStack(st) {
      if (st.length !== pcs.length) return;
      const key = st.join(',');
      if (stackKeys[key]) return;
      stackKeys[key] = true;
      stacks.push(st);
    }
    const others = pcs.filter((p) => p !== rootPc);
    if (pcs.length <= 4) {
      permutePcs(others).forEach((rest) => addStack([rootPc].concat(rest)));
      others.forEach((bass) => {
        const rest = pcs.filter((p) => p !== bass);
        permutePcs(rest).forEach((tail) => addStack([bass].concat(tail)));
      });
      return stacks;
    }
    const byInterval = others.slice().sort((a, b) =>
      ((a - rootPc + 12) % 12) - ((b - rootPc + 12) % 12)
    );
    const closed = [rootPc].concat(byInterval);
    const rev = [rootPc].concat(byInterval.slice().reverse());
    [closed, rev].forEach((ord) => {
      for (let i = 0; i < ord.length; i++) {
        addStack(ord.slice(i).concat(ord.slice(0, i)));
      }
    });
    return stacks;
  }

  /**
   * Compact closed-stack voicing for a PC list (bass = pcs[0]).
   * When `minRootMidi` is set, only accept shapes whose bass/root MIDI is
   * strictly higher — used so scale-chord sequences climb in pitch.
   * Prefers the nearest higher root, then compactness.
   */
  function placeChordPcs(pcs, midis, fretCount, minRootMidi, opts) {
    if (!pcs || !pcs.length || !midis || midis.length < pcs.length) return null;
    const Q = window.FretQuartal;
    if (!Q || typeof Q.collectVoicings !== 'function') return null;
    opts = opts || {};
    // maxFrettedSpan: max (hi−lo) among fretted notes only (opens ignored).
    // 4 ⇒ at most 5 fret positions (same rule as chord applicatures).
    const maxFrettedSpan = opts.maxFrettedSpan != null && Number.isFinite(opts.maxFrettedSpan)
      ? opts.maxFrettedSpan
      : null;
    const searchSpan = maxFrettedSpan != null
      ? Math.max(maxFrettedSpan, 12)
      : Math.max(4, pcs.length + 2);
    const maxFret = opts.maxFret != null && Number.isFinite(opts.maxFret) ? opts.maxFret : null;
    const floor = minRootMidi != null && Number.isFinite(minRootMidi) ? minRootMidi : null;
    let best = null;

    function fretsOk(notes) {
      for (let i = 0; i < notes.length; i++) {
        if (notes[i].f < 0 || notes[i].f > fretCount) return false;
      }
      return true;
    }

    function frettedSpanOk(notes) {
      if (maxFrettedSpan == null) return true;
      let lo = 99;
      let hi = 0;
      let any = false;
      for (let i = 0; i < notes.length; i++) {
        const f = notes[i].f;
        if (f <= 0) continue;
        any = true;
        if (f < lo) lo = f;
        if (f > hi) hi = f;
      }
      if (!any) return true;
      return hi - lo <= maxFrettedSpan;
    }

    function consider(found, score) {
      if (!found || !found.notes || !found.notes.length || !fretsOk(found.notes)) return;
      if (!frettedSpanOk(found.notes)) return;
      const rootMidi = found.rootMidi != null ? found.rootMidi : found.notes[0].m;
      if (floor != null && !(rootMidi > floor)) return;
      const sc = score != null ? score : found.score;
      if (!best) {
        best = { notes: found.notes, rootMidi: rootMidi, score: sc };
        return;
      }
      if (floor != null) {
        if (rootMidi < best.rootMidi || (rootMidi === best.rootMidi && sc < best.score)) {
          best = { notes: found.notes, rootMidi: rootMidi, score: sc };
        }
        return;
      }
      if (sc < best.score) best = { notes: found.notes, rootMidi: rootMidi, score: sc };
    }

    for (let start = 0; start <= midis.length - pcs.length; start++) {
      const candidates = Q.collectVoicings(midis, fretCount, pcs, start, searchSpan, 24, maxFret);
      for (let i = 0; i < candidates.length; i++) {
        const found = candidates[i];
        consider(found, found.score);
        if (typeof Q.shiftVoicing !== 'function') continue;
        for (let oct = 1; oct <= 3; oct++) {
          const shifted = Q.shiftVoicing(found, oct);
          if (shifted) consider(shifted, found.score + oct * 1000);
        }
      }
    }

    if (!best && floor != null) return placeChordPcs(pcs, midis, fretCount, null, opts);
    return best ? best.notes.map((nt) => ({ s: nt.s, f: nt.f, m: nt.m })) : null;
  }

  function encodeVoicing(notes) {
    return notes.map((nt) => nt.s + ':' + nt.f).join(';');
  }

  function decodeVoicing(str) {
    if (!str) return null;
    const notes = [];
    str.split(';').forEach((part) => {
      const bits = part.split(':');
      if (bits.length !== 2) return;
      const s = parseInt(bits[0], 10);
      const f = parseInt(bits[1], 10);
      if (Number.isFinite(s) && Number.isFinite(f)) notes.push({ s: s, f: f });
    });
    return notes.length ? notes : null;
  }

  /** Classic vertical chord diagram (low string left → high right). */
  function chordDiagramSvg(notes, stringCount, rootPc, midis) {
    return miniNeckSvg(notes, stringCount, rootPc, midis, null);
  }

  /**
   * Compact applicature SVG for Analyzer chips and Theory inline figures.
   * opts.labels: 'degrees' | 'names' | falsy — short text on fretted dots.
   * opts.className — extra SVG class (default sd-chord-diag).
   */
  function miniNeckSvg(notes, stringCount, rootPc, midis, opts) {
    if (!notes || !notes.length) return '';
    opts = opts || {};
    const labelMode = opts.labels || '';
    const nStr = stringCount;
    const byString = {};
    notes.forEach((nt) => { byString[nt.s] = nt.f; });

    const fretted = notes.map((nt) => nt.f).filter((f) => f > 0);
    let base = 1;
    let rows = 5;
    if (fretted.length) {
      const lo = Math.min.apply(null, fretted);
      const hi = Math.max.apply(null, fretted);
      if (lo > 1) base = lo;
      rows = Math.max(5, hi - base + 1);
    }

    const showLab = labelMode === 'degrees' || labelMode === 'names';
    const padL = base > 1 ? 14 : 6;
    const padR = 6;
    const padT = showLab ? 16 : 14;
    const padB = 6;
    const cellW = showLab ? 16 : 12;
    const cellH = showLab ? 16 : 12;
    const gridW = (nStr - 1) * cellW;
    const gridH = rows * cellH;
    const w = padL + gridW + padR;
    const h = padT + gridH + padB;
    const xAt = (s) => padL + s * cellW;
    const yAt = (row) => padT + row * cellH;
    const DEG = ['1', '♭2', '2', '♭3', '3', '4', '♯4', '5', '♭6', '6', '♭7', '7'];
    const nm = opts.names || null;

    const cls = 'sd-chord-diag' + (opts.className ? ' ' + opts.className : '') + (showLab ? ' theory-mini-neck' : '');
    const parts = [];
    parts.push('<svg class="' + cls + '" viewBox="0 0 ' + w + ' ' + h + '" width="' + w + '" height="' + h + '" aria-hidden="true">');

    if (base === 1) {
      parts.push('<rect class="sd-cd-nut" x="' + padL + '" y="' + (padT - 2.5) + '" width="' + gridW + '" height="3" rx="0.5"/>');
    } else {
      parts.push('<line class="sd-cd-fret" x1="' + padL + '" y1="' + padT + '" x2="' + (padL + gridW) + '" y2="' + padT + '"/>');
      parts.push('<text class="sd-cd-pos" x="' + (padL - 3) + '" y="' + (padT + cellH * 0.65) + '">' + base + '</text>');
    }

    for (let r = 1; r <= rows; r++) {
      const y = yAt(r);
      parts.push('<line class="sd-cd-fret" x1="' + padL + '" y1="' + y + '" x2="' + (padL + gridW) + '" y2="' + y + '"/>');
    }
    for (let s = 0; s < nStr; s++) {
      const x = xAt(s);
      parts.push('<line class="sd-cd-str" x1="' + x + '" y1="' + padT + '" x2="' + x + '" y2="' + (padT + gridH) + '"/>');
    }

    for (let s = 0; s < nStr; s++) {
      const x = xAt(s);
      if (byString[s] == null) {
        parts.push('<text class="sd-cd-mute" x="' + x + '" y="' + (padT - 5) + '">×</text>');
        continue;
      }
      const f = byString[s];
      const pc = midis ? (((midis[s] + f) % 12) + 12) % 12 : null;
      const isRoot = pc != null && rootPc != null && pc === rootPc;
      let lab = '';
      if (showLab && pc != null && rootPc != null) {
        if (labelMode === 'names' && nm) lab = nm[pc] || '';
        else lab = DEG[(pc - rootPc + 12) % 12] || '';
      }
      if (f === 0) {
        parts.push('<circle class="sd-cd-open' + (isRoot ? ' root' : '') + '" cx="' + x + '" cy="' + (padT - 6) + '" r="' + (showLab ? 5 : 3.2) + '" fill="none"/>');
        if (lab) {
          parts.push('<text class="sd-cd-lab open' + (isRoot ? ' root' : '') + '" x="' + x + '" y="' + (padT - 4) + '">' + lab + '</text>');
        }
        continue;
      }
      if (f < base || f > base + rows - 1) continue;
      const row = f - base;
      const cy = padT + row * cellH + cellH / 2;
      const rDot = showLab ? 6.2 : 3.6;
      parts.push('<circle class="sd-cd-dot' + (isRoot ? ' root' : '') + '" cx="' + x + '" cy="' + cy + '" r="' + rDot + '"/>');
      if (lab) {
        parts.push('<text class="sd-cd-lab' + (isRoot ? ' root' : '') + '" x="' + x + '" y="' + (cy + 3.2) + '">' + lab + '</text>');
      }
    }

    parts.push('</svg>');
    return parts.join('');
  }

  /**
   * Resolve Theory staff note descriptors to letter/accidental/octave/step/midi.
   * notes: [{ name: 'C'|'Eb'|…, octave?: number }] — octave defaults to 4,
   * then auto-ascends so each note sits at/above the previous staff step.
   */
  function resolveStaffNotes(notes) {
    if (!notes || !notes.length) return [];
    const M = (typeof window !== 'undefined' && window.Music) ? window.Music : null;
    const parse = M && M.parseSpell ? M.parseSpell.bind(M) : null;
    if (!parse) return [];
    const NAT_PC = [0, 2, 4, 5, 7, 9, 11];
    const out = [];
    let lastStep = null;
    for (let i = 0; i < notes.length; i++) {
      const raw = notes[i];
      if (!raw || !raw.name) continue;
      const sp = parse(String(raw.name).replace(/♭/g, 'b').replace(/♯/g, '#'));
      if (!sp) continue;
      let oct = raw.octave != null && Number.isFinite(raw.octave) ? (raw.octave | 0) : 4;
      let step = sp.letterIdx + oct * 7;
      if (raw.octave == null && lastStep != null) {
        while (step <= lastStep) {
          oct += 1;
          step = sp.letterIdx + oct * 7;
        }
      }
      lastStep = step;
      const midi = (oct + 1) * 12 + NAT_PC[sp.letterIdx] + sp.accidental;
      out.push({
        letterIdx: sp.letterIdx,
        accidental: sp.accidental,
        octave: oct,
        step: step,
        midi: midi
      });
    }
    return out;
  }

  /** MIDI list for staff figures (same pitch height as staffSvg). */
  function staffNotesToMidi(notes) {
    return resolveStaffNotes(notes).map((n) => n.midi);
  }

  /**
   * Minimal treble-staff SVG for Theory figures (intervals / spelling).
   * opts.layout: 'harmonic' (stacked) | 'melodic' (spaced).
   */
  function staffSvg(notes, opts) {
    if (!notes || !notes.length) return '';
    opts = opts || {};
    const layout = opts.layout === 'melodic' ? 'melodic' : 'harmonic';
    const parsed = resolveStaffNotes(notes);
    if (!parsed.length) return '';

    const LINE_GAP = 8;
    const STEP_H = LINE_GAP / 2;
    const bottomStep = 2 + 4 * 7; // E4
    const topStep = bottomStep + 8; // F5
    let minStep = parsed[0].step;
    let maxStep = parsed[0].step;
    for (let i = 1; i < parsed.length; i++) {
      if (parsed[i].step < minStep) minStep = parsed[i].step;
      if (parsed[i].step > maxStep) maxStep = parsed[i].step;
    }
    const padExtraTop = Math.max(0, maxStep - topStep) * STEP_H + 4;
    const padExtraBot = Math.max(0, bottomStep - minStep) * STEP_H + 4;
    const padT = 10 + padExtraTop;
    const padB = 8 + padExtraBot;
    const padL = 6;
    const padR = 8;
    const clefW = 22;
    const staffH = 4 * LINE_GAP;
    const yBottom = padT + staffH;
    const yAt = (step) => yBottom - (step - bottomStep) * STEP_H;

    const noteXs = [];
    if (layout === 'melodic') {
      let x = padL + clefW + 14;
      for (let i = 0; i < parsed.length; i++) {
        noteXs.push(x);
        x += parsed[i].accidental ? 22 : 16;
      }
    } else {
      const baseX = padL + clefW + 18;
      const sorted = parsed.map((n, i) => ({ i: i, step: n.step })).sort((a, b) => a.step - b.step);
      const xOf = {};
      let cluster = baseX;
      for (let i = 0; i < sorted.length; i++) {
        if (i > 0 && sorted[i].step - sorted[i - 1].step <= 1) cluster = baseX + 7;
        else cluster = baseX;
        xOf[sorted[i].i] = cluster;
      }
      for (let i = 0; i < parsed.length; i++) noteXs.push(xOf[i] != null ? xOf[i] : baseX);
    }

    let maxX = padL + clefW;
    for (let i = 0; i < noteXs.length; i++) {
      const ax = noteXs[i] + (parsed[i].accidental ? 4 : 10);
      if (ax > maxX) maxX = ax;
    }
    const w = Math.max(72, maxX + padR);
    const h = padT + staffH + padB;
    const cls = 'sd-staff' + (opts.className ? ' ' + opts.className : '');
    const parts = [];
    parts.push('<svg class="' + cls + '" viewBox="0 0 ' + w + ' ' + h + '" width="' + w + '" height="' + h + '" aria-hidden="true">');

    for (let li = 0; li < 5; li++) {
      const y = padT + li * LINE_GAP;
      parts.push('<line class="sd-staff-line" x1="' + padL + '" y1="' + y + '" x2="' + (w - padR) + '" y2="' + y + '"/>');
    }

    // Compact treble-clef silhouette (G-clef hint).
    const cx = padL + 10;
    const cy = padT + 2.5 * LINE_GAP;
    parts.push(
      '<path class="sd-staff-clef" d="' +
      'M' + (cx + 2) + ' ' + (cy + 14) +
      'c0 4-3.5 6-6 4.5s-3-5-1.5-8c2-4 7-7 7-12 0-5-3.5-8-7-8s-6.5 3.2-6 7.5c.4 3.5 3.5 5.5 6.5 5' +
      'c0-6 1.2-10 4.2-10 2.8 0 4.3 2.6 4.3 5.8 0 5.5-4.5 9.2-7.2 13.2-1.6 2.4-1.2 5.2 1.2 6.2 3.2 1.3 7-1.5 7-5.8z' +
      '"/>'
    );

    function ledgerAt(step, x) {
      if (step < bottomStep) {
        for (let s = bottomStep - 2; s >= step; s -= 2) {
          if ((s - bottomStep) % 2 !== 0) continue;
          const y = yAt(s);
          parts.push('<line class="sd-staff-ledger" x1="' + (x - 7) + '" y1="' + y + '" x2="' + (x + 7) + '" y2="' + y + '"/>');
        }
      } else if (step > topStep) {
        for (let s = topStep + 2; s <= step; s += 2) {
          if ((s - bottomStep) % 2 !== 0) continue;
          const y = yAt(s);
          parts.push('<line class="sd-staff-ledger" x1="' + (x - 7) + '" y1="' + y + '" x2="' + (x + 7) + '" y2="' + y + '"/>');
        }
      }
    }

    for (let i = 0; i < parsed.length; i++) {
      const n = parsed[i];
      const x = noteXs[i];
      const y = yAt(n.step);
      ledgerAt(n.step, x);
      if (n.accidental) {
        const glyph = n.accidental > 0 ? '♯'.repeat(Math.min(2, n.accidental))
          : '♭'.repeat(Math.min(2, -n.accidental));
        parts.push('<text class="sd-staff-acc" x="' + (x - 9) + '" y="' + (y + 3.5) + '">' + glyph + '</text>');
      }
      parts.push('<ellipse class="sd-staff-head" cx="' + x + '" cy="' + y + '" rx="5.2" ry="3.8" transform="rotate(-18 ' + x + ' ' + y + ')"/>');
    }

    parts.push('</svg>');
    return parts.join('');
  }

  function create(deps) {
    const $ = deps.$;
    const t = deps.t;
    const M = deps.M;
    const state = deps.state;
    const ui = deps.ui;
    const names = deps.names;
    const degreeRoman = deps.degreeRoman;
    const tuningMidis = deps.tuningMidis;
    const scaleDisplayName = deps.scaleDisplayName;
    const renderScaleClusters = deps.renderScaleClusters;
    const applySharedScale = deps.applySharedScale;
    const render = deps.render;
    const saveState = deps.saveState;
    const playSimultaneous = deps.playSimultaneous;
    const playChordSimultaneous = deps.playChordSimultaneous;
    const openTheoryArticle = deps.openTheoryArticle;
    const showTip = deps.showTip;
    const hideTip = deps.hideTip;
    const positionTip = deps.positionTip;
    const tipVisible = deps.tipVisible;
    const tipRow = deps.tipRow;

    let chordScalesCache = { key: '', value: null };

    function proposalKeyPc(r) {
      if (ui.scalePropFollow || !r) return r ? r.rootPc : 0;
      return ui.scalePropKey;
    }

    function chordScaleOpts(r, keyPc) {
      return { names: names(), compoundIntervals: state.compoundIntervals, scaleRoot: keyPc != null ? keyPc : proposalKeyPc(r) };
    }

    function getChordScales(r, keyPc) {
      if (!r || !r.matched) return null;
      const opts = chordScaleOpts(r, keyPc);
      const nm = opts.names || [];
      const key = [
        r.rootPc,
        (r.intervals || []).join(','),
        opts.scaleRoot,
        opts.compoundIntervals ? 1 : 0,
        nm.join('|')
      ].join('#');
      if (chordScalesCache.key === key) return chordScalesCache.value;
      const value = M.chordScales(r.rootPc, r.intervals, opts);
      chordScalesCache = { key: key, value: value };
      return value;
    }

    function alterationHooksHtml(alts, rootPc, interactive) {
      if (!alts || !alts.length) return '';
      const hookFn = interactive && window.FretTheory && window.FretTheory.hookForAlteration;
      return alts.map((tok) => {
        const hook = hookFn ? hookFn(tok) : null;
        if (!hook || !Number.isFinite(rootPc)) {
          return '<span class="cr-alteration">' + tok + '</span>';
        }
        return '<button type="button" class="cr-alt-hook" data-root="' + rootPc +
          '" data-family="' + hook.family +
          '" data-mode="' + hook.modeIndex +
          '" title="' + t('chords.altHookTitle', { alt: tok }) + '">' + tok + '</button>';
      }).join('');
    }

    function parenHooksHtml(parens, rootPc, interactive) {
      const list = chordDisplayParens(parens);
      if (!list.length) return '';
      const hookFn = interactive && window.FretTheory && window.FretTheory.hookForAlteration;
      const inner = list.map((tok) => {
        const hook = hookFn ? hookFn(tok) : null;
        if (!hook || !Number.isFinite(rootPc)) return tok;
        return '<button type="button" class="cr-alt-hook" data-root="' + rootPc +
          '" data-family="' + hook.family +
          '" data-mode="' + hook.modeIndex +
          '" title="' + t('chords.altHookTitle', { alt: tok }) + '">' + tok + '</button>';
      }).join(',');
      return '<span class="cr-alteration">(' + inner + ')</span>';
    }

    function chordDisplayParens(parens) {
      const list = Array.isArray(parens) ? parens : [];
      return state.showNo5 ? list.slice() : list.filter((p) => p !== 'no5');
    }

    function chordDisplaySuffix(r) {
      const p = chordDisplayParens(r.parens);
      const parenStr = p.length ? '(' + p.join(',') + ')' : '';
      return (r.quality || '') + (r.extension || '') + (r.alterations || []).join('') + parenStr;
    }

    function chordSymbolStackHtml(r, opts) {
      if (!r) return '';
      const interactive = !(opts && opts.hooks === false);
      const raised =
        (r.extension ? '<span class="cr-extension">' + r.extension + '</span>' : '') +
        alterationHooksHtml(r.alterations, r.rootPc, interactive) +
        parenHooksHtml(r.parens, r.rootPc, interactive);
      return '<span class="cr-root">' + r.rootName + '</span>' +
        (r.quality ? '<span class="cr-quality">' + r.quality + '</span>' : '') +
        raised;
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
      const p = chordDisplayParens(r.parens);
      const paren = p.length ? '(' + p.join(',') + ')' : '';
      const alteration = (alts + paren) || dash;
      return tipRow(t('tip.root'), r.rootName || dash, 'tr') +
        tipRow(t('tip.quality'), tipQuality(r)) +
        tipRow(t('tip.extension'), r.extension || dash) +
        tipRow(t('tip.alteration'), alteration) +
        tipRow(t('tip.bass'), r.slash && r.bassName ? r.bassName : dash);
    }

    /**
     * Alternative closed applicatures for the analyzed chord (all chord tones,
     * frets 0–11 only — no +12 octave copies). Max fret span 4 (5 fret positions)
     * so 6–7-fret stretches are dropped as unplayable.
     */
    function collectChordApplicatures(r, limit) {
      const Q = window.FretQuartal;
      if (!Q || typeof Q.collectVoicings !== 'function') return [];
      const pcs = shapePcsFromChord(r);
      if (pcs.length < 2) return [];
      const midis = tuningMidis();
      if (pcs.length > midis.length) return [];
      const N = state.fretCount;
      const maxOut = limit != null ? limit : 10;
      const maxSpan = 4;
      const stacks = chordToneStacks(pcs, r.rootPc);
      const found = [];
      const seen = {};
      stacks.forEach((stack) => {
        for (let start = 0; start <= midis.length - stack.length; start++) {
          const hits = Q.collectVoicings(midis, N, stack, start, maxSpan, 6, 11);
          hits.forEach((hit) => {
            if (hit.bassFret >= 12) return;
            let lo = 99;
            let hi = 0;
            hit.notes.forEach((nt) => {
              if (nt.f <= 0) return;
              if (nt.f < lo) lo = nt.f;
              if (nt.f > hi) hi = nt.f;
            });
            if (lo <= hi && hi - lo > maxSpan) return;
            if (!voicingHasAllPcs(hit.notes, midis, pcs)) return;
            const fp = voicingFingerprint(hit.notes);
            if (seen[fp]) return;
            seen[fp] = true;
            found.push({
              score: hit.score || 0,
              notes: hit.notes,
              rootInBass: (((midis[hit.notes[0].s] + hit.notes[0].f) % 12) + 12) % 12 === r.rootPc
            });
          });
        }
      });
      found.sort((a, b) => (b.rootInBass - a.rootInBass) || (a.score - b.score));
      return found.slice(0, maxOut);
    }

    function applyChordVoicing(notes, rootPc) {
      if (!notes || !notes.length) return;
      const midis = tuningMidis();
      state.chordNotes = notes.map((nt) => ({ s: nt.s, f: nt.f, id: ++ui.noteSeq }));
      const rootNote = notes.find((nt) => (((midis[nt.s] + nt.f) % 12) + 12) % 12 === rootPc);
      state.pinRoot = rootNote ? { s: rootNote.s, f: rootNote.f } : { s: notes[0].s, f: notes[0].f };
      ui.scaleSel = null;
      render();
    }

    function chordShapesSectionHtml(r) {
      if (!r || !r.matched) return '';
      const apps = collectChordApplicatures(r, 10);
      const midis = tuningMidis();
      const playLabel = t('scaleDetails.playChord');
      const body = apps.length
        ? '<div class="cr-shapes-row">' +
          apps.map((app) => {
            const diagram = chordDiagramSvg(app.notes, midis.length, r.rootPc, midis);
            return '<div class="cr-shape-chip" data-voicing="' + encodeVoicing(app.notes) + '" data-root="' + r.rootPc + '">' +
              (diagram || '') +
              '<span class="cr-shape-actions">' +
                '<button type="button" class="cr-shape-apply" title="' + t('chords.shapeApply') + '">' + t('chords.shapeApply') + '</button>' +
                '<button type="button" class="cr-chord-play cr-shape-play" title="' + playLabel + '" aria-label="' + playLabel + '">▶</button>' +
              '</span>' +
            '</div>';
          }).join('') +
          '</div>'
        : '<p class="cr-shapes-empty">' + t('chords.shapesEmpty') + '</p>';
      return '<section class="cr-section cr-shapes">' +
        '<details class="cr-shapes-spoiler"' + (ui.shapesSpoilerOpen ? ' open' : '') + '>' +
          '<summary class="cr-shapes-summary">' + t('chords.shapes') +
            (apps.length ? ' <span class="cr-shapes-count">(' + apps.length + ')</span>' : '') +
          '</summary>' +
          '<div class="cr-shapes-body">' + body + '</div>' +
        '</details>' +
      '</section>';
    }

    function updateChordLive() {
      const live = $('#chord-live');
      if (!live) return;
      if (!ui.chordResult) {
        live.textContent = t('controls.chords.liveEmpty');
        return;
      }
      const r = ui.chordResult;
      if (!r.matched) {
        live.textContent = t('controls.chords.liveUnmatched');
        return;
      }
      let name = r.rootName + chordDisplaySuffix(r);
      if (r.slash) name += '/' + r.bassName;
      live.textContent = name;
    }

    function renderChordResult() {
      const el = $('#chord-result');
      if (!ui.chordResult) {
        el.innerHTML = '<div class="cr-empty">' + t('chords.empty') + '</div>';
        updateChordLive();
        return;
      }
      const r = ui.chordResult;
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
        alterationHooksHtml(r.alterations, r.rootPc) +
        parenHooksHtml(r.parens, r.rootPc);
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
        altHtml = '<div class="cr-alt">' + t('chords.alsoHeardAs') + r.alternatives.map((a) => a.rootName + chordDisplaySuffix(a)).join('  ·  ') + '</div>';
      }
      let scalesHtml = '';
      if (r.matched) {
        if (ui.scalePropFollow) ui.scalePropKey = r.rootPc;
        const keyPc = proposalKeyPc(r);
        const sc = getChordScales(r, keyPc);
        const nm = names();
        const keyOpts = nm.map((nn, i) => '<option value="' + i + '"' + (i === keyPc ? ' selected' : '') + '>' + nn + '</option>').join('');
        const resetBtn = ui.scalePropFollow ? '' : '<button type="button" class="cr-scale-key-reset" title="' + t('chords.useChordRoot') + '">↺</button>';
        const keyCtl =
          '<label class="cr-scale-key-wrap">' + t('chords.key') + ' ' +
          '<select class="cr-scale-key" aria-label="' + t('chords.scaleProposalKeyAria') + '">' + keyOpts + '</select>' +
          resetBtn +
          '</label>';
        const chip = (e, extra) => {
          const active = ui.scaleSel && ui.scaleSel.rootPc === keyPc && ui.scaleSel.name === e.name ? ' active' : '';
          const pinned = state.scaleKey === keyPc && state.scaleFamily === e.family && state.modeIndex === e.modeIndex;
          const pcs = Array.from(e.pcs).sort((a, b) => a - b).join(',');
          const disp = scaleDisplayName(e.family, e.modeIndex);
          return '<button type="button" class="cr-scale-chip' + extra + active + (pinned ? ' cr-scale-wired' : '') + '" data-r="' + keyPc + '" data-n="' + e.name + '" data-pcs="' + pcs + '" data-fk="' + e.family + '" data-m="' + e.modeIndex + '">' +
            '<span class="cr-scale-label">' + disp + e.note + '</span>' +
            '<span class="cr-scale-pin' + (pinned ? ' on' : '') + '" data-pin="1" title="' + t('chords.useOnExplore') + '">✦</span>' +
            '</button>';
        };
        const fullBlock = sc.full.length
          ? '<div class="cr-scale-block"><div class="cr-scale-block-label">' + t('chords.fullMatch') + '</div>' + renderScaleClusters(sc.full, chip, '') + '</div>'
          : '';
        const partBlock = sc.partial.length
          ? '<div class="cr-scale-block cr-scale-block-partial"><div class="cr-scale-block-label">' + t('chords.partial') + '</div>' + renderScaleClusters(sc.partial, chip, ' cr-scale-partial') + '</div>'
          : '';
        const hint = ui.scaleSel
          ? '<span class="cr-scale-hint">' + t('chords.ghostHint', { root: sc.rootName, scale: scaleDisplayName(ui.scaleSel.family, ui.scaleSel.modeIndex) }) + '</span>'
          : '';
        const empty = (!sc.full.length && !sc.partial.length)
          ? '<div class="cr-scales-empty">' + t('chords.noScales', { root: sc.rootName }) + '</div>'
          : '';
        scalesHtml =
          '<section class="cr-section cr-scales">' +
          '<div class="cr-scales-title">' +
            '<span class="cr-section-label">' + t('chords.scalesThatFit') + '</span>' +
            '<b>' + r.rootName + chordDisplaySuffix(r) + '</b>' +
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
            '<div class="cr-head">' +
              '<div class="cr-name-row">' +
                '<div class="cr-name">' + symbol + '</div>' +
                '<button type="button" class="cr-chord-play" title="' + t('scaleDetails.playChord') + '" aria-label="' + t('scaleDetails.playChord') + '">▶</button>' +
              '</div>' +
              '<div class="cr-sub">' + sub + '</div>' +
            '</div>' +
            altHtml +
          '</section>' +
          '<section class="cr-section cr-tones">' +
            '<div class="cr-section-label">' + t('chords.notes') + '</div>' +
            '<div class="cr-chips">' + chips + '</div>' +
          '</section>' +
          chordShapesSectionHtml(r) +
          scalesHtml +
        '</div>';
      updateChordLive();
    }

    function bindChordResultEvents() {
      $('#chord-result').addEventListener('change', (e) => {
        if (!e.target.classList || !e.target.classList.contains('cr-scale-key')) return;
        ui.scalePropFollow = false;
        ui.scalePropKey = parseInt(e.target.value, 10);
        ui.scaleSel = null;
        ui.chordScaleFocus = null;
        render();
      });

      $('#chord-result').addEventListener('click', (e) => {
        const playBtn = e.target.closest ? e.target.closest('.cr-chord-play') : null;
        if (playBtn) {
          e.preventDefault();
          e.stopPropagation();
          const shapeChip = playBtn.closest('.cr-shape-chip');
          if (shapeChip) {
            const voicing = decodeVoicing(shapeChip.dataset.voicing);
            const midis = tuningMidis();
            if (voicing && voicing.length) {
              playSimultaneous(voicing.map((nt) => midis[nt.s] + nt.f));
            }
            return;
          }
          playChordSimultaneous();
          return;
        }
        const applyBtn = e.target.closest ? e.target.closest('.cr-shape-apply') : null;
        if (applyBtn) {
          e.preventDefault();
          e.stopPropagation();
          const shapeChip = applyBtn.closest('.cr-shape-chip');
          if (!shapeChip) return;
          const voicing = decodeVoicing(shapeChip.dataset.voicing);
          const rootPc = parseInt(shapeChip.dataset.root, 10);
          if (voicing) applyChordVoicing(voicing, rootPc);
          return;
        }
        const shapeChip = e.target.closest ? e.target.closest('.cr-shape-chip') : null;
        if (shapeChip && !e.target.closest('.cr-shape-actions')) {
          e.preventDefault();
          const voicing = decodeVoicing(shapeChip.dataset.voicing);
          const rootPc = parseInt(shapeChip.dataset.root, 10);
          if (voicing) applyChordVoicing(voicing, rootPc);
          return;
        }
        const reset = e.target.closest ? e.target.closest('.cr-scale-key-reset') : null;
        if (reset) {
          e.preventDefault();
          ui.scalePropFollow = true;
          ui.scaleSel = null;
          ui.chordScaleFocus = null;
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
          ui.chordScaleFocus = { rootPc: rootPc, family: family, modeIndex: modeIndex, name: chip.dataset.n };
          applySharedScale(rootPc, family, modeIndex);
          return;
        }
        const altHook = e.target.closest ? e.target.closest('.cr-alt-hook') : null;
        if (altHook) {
          e.preventDefault();
          e.stopPropagation();
          const rootPc = parseInt(altHook.dataset.root, 10);
          const family = altHook.dataset.family;
          const modeIndex = parseInt(altHook.dataset.mode, 10);
          if (!M.SCALES[family] || !Number.isFinite(rootPc)) return;
          const ordered = M.scaleOrdered(family, modeIndex);
          const name = names()[rootPc] + ' ' + scaleDisplayName(family, modeIndex);
          ui.scaleSel = {
            rootPc: rootPc,
            name: name,
            ivs: ordered.slice(),
            family: family,
            modeIndex: modeIndex
          };
          ui.chordScaleFocus = { rootPc: rootPc, family: family, modeIndex: modeIndex, name: name };
          applySharedScale(rootPc, family, modeIndex);
          return;
        }
        const chip = e.target.closest ? e.target.closest('.cr-scale-chip') : null;
        if (!chip) return;
        const r = parseInt(chip.dataset.r, 10);
        const n = chip.dataset.n;
        if (ui.scaleSel && ui.scaleSel.rootPc === r && ui.scaleSel.name === n) {
          ui.scaleSel = null;
        } else {
          ui.scaleSel = {
            rootPc: r,
            name: n,
            ivs: chip.dataset.pcs.split(',').map(Number),
            family: chip.dataset.fk,
            modeIndex: parseInt(chip.dataset.m, 10)
          };
        }
        render();
      });

      $('#chord-result').addEventListener('toggle', (e) => {
        const det = e.target.closest ? e.target.closest('.cr-shapes-spoiler') : null;
        if (!det || e.target !== det) return;
        ui.shapesSpoilerOpen = !!det.open;
        saveState();
      }, true);

      $('#chord-result').addEventListener('mouseover', (e) => {
        const nameEl = e.target.closest ? e.target.closest('.cr-name') : null;
        if (!nameEl || !ui.chordResult) return;
        showTip(chordSymbolTipHtml(ui.chordResult), e.clientX, e.clientY);
      });
      $('#chord-result').addEventListener('mouseout', (e) => {
        const nameEl = e.target.closest ? e.target.closest('.cr-name') : null;
        if (!nameEl) return;
        const next = e.relatedTarget;
        if (next && nameEl.contains(next)) return;
        hideTip();
      });
      $('#chord-result').addEventListener('mousemove', (e) => {
        if (!tipVisible()) return;
        if (!(e.target.closest && e.target.closest('.cr-name'))) return;
        positionTip(e.clientX, e.clientY);
      });
    }

    return {
      renderChordResult: renderChordResult,
      bindChordResultEvents: bindChordResultEvents,
      getChordScales: getChordScales,
      proposalKeyPc: proposalKeyPc,
      chordDisplayParens: chordDisplayParens,
      chordDisplaySuffix: chordDisplaySuffix,
      chordSymbolStackHtml: chordSymbolStackHtml,
      chordSymbolTipHtml: chordSymbolTipHtml,
      chordShapesSectionHtml: chordShapesSectionHtml,
      collectChordApplicatures: collectChordApplicatures,
      applyChordVoicing: applyChordVoicing,
      placeChordPcs: placeChordPcs,
      chordDiagramSvg: chordDiagramSvg,
      miniNeckSvg: miniNeckSvg,
      staffSvg: staffSvg,
      staffNotesToMidi: staffNotesToMidi,
      encodeVoicing: encodeVoicing,
      decodeVoicing: decodeVoicing,
      midisFromPcsAscending: midisFromPcsAscending
    };
  }

  window.FretChordUi = {
    placeChordPcs: placeChordPcs,
    chordDiagramSvg: chordDiagramSvg,
    miniNeckSvg: miniNeckSvg,
    staffSvg: staffSvg,
    staffNotesToMidi: staffNotesToMidi,
    encodeVoicing: encodeVoicing,
    decodeVoicing: decodeVoicing,
    midisFromPcsAscending: midisFromPcsAscending,
    create: create
  };
})();
