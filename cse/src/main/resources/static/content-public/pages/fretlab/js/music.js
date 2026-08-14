(function (root, factory) {
  const NS = factory();
  root.Music = NS;
  if (typeof module !== 'undefined' && module.exports) module.exports = NS;
})(typeof self !== 'undefined' ? self : this, function () {
  const SHARP = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];
  const FLAT = ['C', 'Db', 'D', 'Eb', 'E', 'F', 'Gb', 'G', 'Ab', 'A', 'Bb', 'B'];
  const DEGREES = ['R', 'b2', '2', 'b3', '3', '4', 'b5', '5', 'b6', '6', 'b7', '7'];
  const QUALITIES = ['P1', 'm2', 'M2', 'm3', 'M3', 'P4', 'TT', 'P5', 'm6', 'M6', 'm7', 'M7'];
  const QUALITY_NAMES = [
    'Perfect unison',
    'Minor 2nd',
    'Major 2nd',
    'Minor 3rd',
    'Major 3rd',
    'Perfect 4th',
    'Tritone',
    'Perfect 5th',
    'Minor 6th',
    'Major 6th',
    'Minor 7th',
    'Major 7th'
  ];
  const INTERVALS = DEGREES;
  function qualityFull(semis) {
    const i = ((semis % 12) + 12) % 12;
    let full = QUALITY_NAMES[i];
    try {
      const g = typeof self !== 'undefined' ? self : (typeof window !== 'undefined' ? window : null);
      const arr = g && g.FretI18n && typeof g.FretI18n.raw === 'function' ? g.FretI18n.raw('tip.qualityNames') : null;
      if (Array.isArray(arr) && arr[i]) full = arr[i];
    } catch (e) { /* keep English */ }
    return QUALITIES[i] + ' <span class="tip-full">(' + full + ')</span>';
  }
  const CHORD_SYMBOL = ['1', 'b2', '2', 'b3', '3', '4', '#4', '5', 'b6', '6', 'b7', '7'];
  function relLabel(semis, degreeOverride) {
    const i = ((semis % 12) + 12) % 12;
    return {
      degree: degreeOverride != null ? degreeOverride : DEGREES[i],
      quality: QUALITIES[i]
    };
  }
  function formatRel(semis, showDegree, showQuality, degreeOverride, join) {
    const r = relLabel(semis, degreeOverride);
    const parts = [];
    if (showDegree && r.degree) parts.push(r.degree);
    if (showQuality) parts.push(r.quality);
    return parts.join(join != null ? join : '<br>');
  }

  const TUNINGS = [
    { id: 'std6', label: 'Standard (EADGBE)', strings: 6, midi: [40, 45, 50, 55, 59, 64] },
    { id: 'dropd6', label: 'Drop D', strings: 6, midi: [38, 45, 50, 55, 59, 64] },
    { id: 'opend6', label: 'Open D', strings: 6, midi: [38, 45, 50, 54, 57, 62] },
    { id: 'openg6', label: 'Open G', strings: 6, midi: [38, 43, 50, 55, 59, 62] },
    { id: 'opene6', label: 'Open E', strings: 6, midi: [40, 47, 52, 56, 59, 64] },
    { id: 'openc6', label: 'Open C', strings: 6, midi: [36, 43, 48, 52, 55, 60] },
    { id: 'dadgad6', label: 'DADGAD', strings: 6, midi: [38, 45, 50, 55, 57, 62] },
    { id: 'fourths6', label: 'All Fourths', strings: 6, midi: [40, 45, 50, 55, 60, 65] },
    { id: 'std7', label: 'Standard 7-string (BEADGBE)', strings: 7, midi: [35, 40, 45, 50, 55, 59, 64] },
    { id: 'dropa7', label: 'Drop A (7-string)', strings: 7, midi: [33, 40, 45, 50, 55, 59, 64] },
    { id: 'std8', label: 'Standard 8-string (F#BEADGBE)', strings: 8, midi: [30, 35, 40, 45, 50, 55, 59, 64] },
    { id: 'std9', label: 'Standard 9-string (C#F#BEADGBE)', strings: 9, midi: [25, 30, 35, 40, 45, 50, 55, 59, 64] },
    { id: 'std10', label: 'Standard 10-string (BC#F#BEADGBE)', strings: 10, midi: [23, 25, 30, 35, 40, 45, 50, 55, 59, 64] },
    { id: 'std5', label: 'Standard 5-string (ADGBE)', strings: 5, midi: [45, 50, 55, 59, 64] },
    { id: 'uk4', label: 'Standard 4-string (DGBE)', strings: 4, midi: [50, 55, 59, 64] },
    { id: 'custom', label: 'Custom…', strings: null, midi: null }
  ];

  const SCALE_CATS = [
    { id: 'church', families: ['major'] },
    { id: 'melodic', families: ['melodicMinor'] },
    { id: 'harmonic', families: ['harmonicMinor'] },
    { id: 'harmMajor', families: ['harmonicMajor'] },
    { id: 'doubleHarm', families: ['doubleHarmonic'] },
    { id: 'neapolitan', families: ['neapolitanMajor', 'neapolitanMinor'] },
    { id: 'symmetric', families: ['wholeTone', 'augmented', 'diminished', 'diminishedHW', 'chromatic', 'messiaen3', 'messiaen4', 'messiaen5', 'messiaen6', 'messiaen7'] },
    { id: 'penta', families: ['majorPentatonic', 'minorPentatonic', 'suspendedPentatonic', 'dominantPentatonic', 'hirajoshi', 'iwato', 'insen', 'pelog', 'marwa'] },
    { id: 'hex', families: ['blues', 'bluesMajor', 'hexatonicMajor', 'hexatonicMinor', 'prometheus'] },
    { id: 'bebop', families: ['bebopDominant', 'bebopMajor', 'bebopMinor', 'bebopDorian', 'bebopLocrian'] },
    { id: 'jazz', families: ['dominant', 'minorSixDiminished', 'majorSixDiminished'] },
    { id: 'world', families: ['persian', 'enigmatic', 'spanishPhrygian', 'todi', 'purvi', 'phrygianNatural6', 'mixolydianAugmented', 'lydianMinor', 'hungarianMajor', 'locrianNatural2'] }
  ];

  const SCALES = {
    major: {
      label: 'Major (Church Modes)',
      modes: ['Ionian', 'Dorian', 'Phrygian', 'Lydian', 'Mixolydian', 'Aeolian', 'Locrian'],
      base: [0, 2, 4, 5, 7, 9, 11]
    },
    melodicMinor: {
      label: 'Melodic Minor',
      modes: ['Melodic Minor', 'Dorian b2', 'Lydian Augmented', 'Lydian Dominant', 'Mixolydian b6', 'Locrian #2', 'Super Locrian'],
      base: [0, 2, 3, 5, 7, 9, 11]
    },
    harmonicMinor: {
      label: 'Harmonic Minor',
      modes: ['Harmonic Minor', 'Locrian #6', 'Ionian #5', 'Dorian #4', 'Phrygian Dominant', 'Lydian #9', 'Altered bb7'],
      base: [0, 2, 3, 5, 7, 8, 11]
    },
    harmonicMajor: {
      label: 'Harmonic Major',
      modes: ['Harmonic Major', 'Dorian b5', 'Phrygian b4', 'Lydian b3', 'Mixolydian b2', 'Lydian Augmented #2', 'Locrian bb7'],
      base: [0, 2, 4, 5, 7, 8, 11]
    },
    doubleHarmonic: {
      label: 'Double Harmonic',
      modes: ['Double Harmonic Major', 'Lydian #2 #6', 'Ultraphrygian', 'Hungarian Minor', 'Oriental', 'Ionian b2 b5', 'Locrian b3 bb7'],
      base: [0, 1, 4, 5, 7, 8, 11]
    },
    neapolitanMajor: {
      label: 'Neapolitan Major',
      modes: ['Neapolitan Major', 'Leading Whole Tone', 'Lydian #5 b7', 'Lydian b6 b7', 'Major Locrian', 'Ionian #4 b6', 'Phrygian #2 #4'],
      base: [0, 1, 3, 5, 7, 9, 11]
    },
    neapolitanMinor: {
      label: 'Neapolitan Minor',
      modes: ['Neapolitan Minor', 'Lydian Dominant', 'Mixolydian', 'Locrian #2', 'Phrygian #3', 'Mixolydian #2', 'Lydian #2 b6'],
      base: [0, 1, 3, 5, 7, 8, 11]
    },
    wholeTone: { label: 'Whole Tone', base: [0, 2, 4, 6, 8, 10] },
    augmented: { label: 'Augmented', base: [0, 3, 4, 7, 8, 11] },
    diminished: { label: 'Diminished (whole-half)', base: [0, 2, 3, 5, 6, 8, 9, 11] },
    diminishedHW: { label: 'Diminished (half-whole)', base: [0, 1, 3, 4, 6, 7, 9, 10] },
    chromatic: { label: 'Chromatic', base: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11] },
    messiaen3: { label: 'Messiaen Mode 3', base: [0, 2, 3, 4, 6, 7, 8, 10, 11] },
    messiaen4: { label: 'Messiaen Mode 4', base: [0, 1, 2, 5, 6, 7, 8, 11] },
    messiaen5: { label: 'Messiaen Mode 5', base: [0, 1, 5, 6, 7, 8] },
    messiaen6: { label: 'Messiaen Mode 6', base: [0, 2, 4, 5, 6, 8, 10, 11] },
    messiaen7: { label: 'Messiaen Mode 7', base: [0, 1, 2, 3, 4, 5, 6, 7, 9, 10] },
    majorPentatonic: { label: 'Major Pentatonic', base: [0, 2, 4, 7, 9] },
    minorPentatonic: { label: 'Minor Pentatonic', base: [0, 3, 5, 7, 10] },
    suspendedPentatonic: { label: 'Suspended (Egyptian / Yo)', base: [0, 2, 5, 7, 9] },
    dominantPentatonic: { label: 'Dominant Pentatonic', base: [0, 2, 4, 7, 10] },
    hirajoshi: { label: 'Hirajoshi', base: [0, 2, 3, 7, 8] },
    iwato: { label: 'Iwato', base: [0, 1, 5, 6, 10] },
    insen: { label: 'In-Sen', base: [0, 1, 5, 7, 10] },
    pelog: { label: 'Pelog', base: [0, 1, 3, 7, 8] },
    marwa: { label: 'Marwa', base: [0, 1, 4, 6, 9] },
    blues: { label: 'Blues (hexatonic)', base: [0, 3, 5, 6, 7, 10] },
    bluesMajor: { label: 'Major Blues', base: [0, 2, 3, 4, 7, 9] },
    hexatonicMajor: { label: 'Hexatonic Major', base: [0, 2, 4, 5, 7, 11] },
    hexatonicMinor: { label: 'Hexatonic Minor', base: [0, 3, 5, 7, 8, 11] },
    prometheus: { label: 'Prometheus (Mystic)', base: [0, 1, 4, 6, 9, 10] },
    bebopDominant: { label: 'Bebop Dominant', base: [0, 2, 4, 5, 7, 9, 10, 11] },
    bebopMajor: { label: 'Bebop Major', base: [0, 2, 4, 5, 7, 8, 9, 11] },
    bebopMinor: { label: 'Bebop Minor', base: [0, 2, 3, 4, 5, 7, 9, 10] },
    bebopDorian: { label: 'Bebop Dorian', base: [0, 2, 3, 5, 7, 9, 10, 11] },
    bebopLocrian: { label: 'Bebop Locrian', base: [0, 1, 2, 3, 5, 6, 8, 10] },
    dominant: { label: 'Mixolydian (Dominant)', base: [0, 2, 4, 5, 7, 9, 10] },
    minorSixDiminished: { label: 'Minor Six Diminished', base: [0, 3, 5, 7, 8, 10, 11] },
    majorSixDiminished: { label: 'Major Six Diminished', base: [0, 2, 4, 5, 7, 8, 10, 11] },
    persian: { label: 'Persian', base: [0, 1, 4, 5, 6, 8, 10] },
    enigmatic: { label: 'Enigmatic', base: [0, 1, 4, 6, 8, 10, 11] },
    spanishPhrygian: { label: 'Spanish Phrygian (Flamenco)', base: [0, 1, 3, 4, 5, 7, 8, 10] },
    todi: { label: 'Todi', base: [0, 1, 3, 6, 7, 8, 11] },
    purvi: { label: 'Purvi', base: [0, 1, 4, 6, 8, 11] },
    phrygianNatural6: { label: 'Phrygian Natural 6', base: [0, 1, 3, 5, 7, 8, 10] },
    mixolydianAugmented: { label: 'Mixolydian Augmented', base: [0, 2, 4, 5, 7, 8, 10] },
    lydianMinor: { label: 'Lydian Minor', base: [0, 2, 4, 6, 7, 8, 10] },
    hungarianMajor: { label: 'Hungarian Major', base: [0, 3, 4, 6, 7, 9, 10] },
    locrianNatural2: { label: 'Locrian Natural 2', base: [0, 2, 3, 5, 6, 8, 10] }
  };

  function rotateScale(familyKey, mode) {
    const b = SCALES[familyKey].base;
    const len = b.length;
    const out = [];
    for (let j = 0; j < len; j++) {
      const idx = (mode + j) % len;
      const v = idx >= mode ? b[idx] - b[mode] : (b[idx] + 12) - b[mode];
      out.push(v);
    }
    return out;
  }

  function scaleOrdered(familyKey, modeIndex) {
    const fam = SCALES[familyKey];
    if (fam.modes) return rotateScale(familyKey, modeIndex);
    return fam.base.slice();
  }

  function scalePcs(familyKey, modeIndex) {
    const ordered = scaleOrdered(familyKey, modeIndex);
    const set = new Set();
    for (const v of ordered) set.add(v % 12);
    return set;
  }

  /**
   * Parent (relative) scale for a mode, when defined.
   * Any modal family (modes[]) except mode 0 → relative mode-0 tonic.
   * e.g. D Dorian → C Ionian; D Dorian b2 → C Melodic Minor.
   */
  function parentScale(familyKey, modeIndex, rootPc) {
    const fam = SCALES[familyKey];
    if (!fam || !fam.modes || !fam.base) return null;
    const mi = modeIndex | 0;
    if (mi < 1 || mi >= fam.modes.length || mi >= fam.base.length) return null;
    const root = ((rootPc % 12) + 12) % 12;
    const parentRoot = (root - fam.base[mi] + 120) % 12;
    return { rootPc: parentRoot, family: familyKey, modeIndex: 0 };
  }

  /**
   * Diatonic chords by stacking thirds through the scale (every other degree).
   * height 3 = triad (default), 4 = seventh. Returns [] when the scale is too
   * small for unique stacks (need ≥ 5 tones for triads).
   */
  function scaleChords(rootPc, orderedIvs, height) {
    const h = height != null ? height : 3;
    const ordered = orderedIvs || [];
    const n = ordered.length;
    if (h < 3 || n < h + 2) return [];
    const root = ((rootPc % 12) + 12) % 12;
    const out = [];
    for (let i = 0; i < n; i++) {
      const pcs = [];
      const seen = {};
      let ok = true;
      for (let k = 0; k < h; k++) {
        const step = ordered[(i + k * 2) % n];
        const pc = (root + (step % 12) + 12) % 12;
        if (seen[pc]) { ok = false; break; }
        seen[pc] = true;
        pcs.push(pc);
      }
      if (!ok || pcs.length !== h) continue;
      out.push({ degreeIndex: i, rootPc: pcs[0], pcs: pcs });
    }
    return out;
  }

  function characteristicPairs(familyKey, modeIndex) {
    const fam = SCALES[familyKey];
    if (!fam) return [];
    const current = scaleOrdered(familyKey, modeIndex);
    let parent;
    if (fam.modes) parent = scaleOrdered(familyKey, 0);
    else if (current.length === 7) parent = SCALES.major.base.slice();
    else return [];
    if (!parent || parent.length !== current.length) return [];
    const pairs = [];
    for (let i = 0; i < current.length; i++) {
      const d = current[i] - parent[i];
      if (d === 0) continue;
      pairs.push({
        alt: current[i] % 12,
        original: parent[i] % 12,
        dir: d < 0 ? 'down' : 'up'
      });
    }
    return pairs;
  }

  function characteristicAlts(familyKey, modeIndex) {
    const marks = {};
    characteristicPairs(familyKey, modeIndex).forEach((p) => { marks[p.alt] = p.dir; });
    return marks;
  }

  function midiToFreq(m) {
    return 440 * Math.pow(2, (m - 69) / 12);
  }

  function midiName(m, flat) {
    const pc = ((m % 12) + 12) % 12;
    const names = flat ? FLAT : SHARP;
    return names[pc];
  }

  const LETTERS = ['C', 'D', 'E', 'F', 'G', 'A', 'B'];
  const NAT_PC = [0, 2, 4, 5, 7, 9, 11];
  // Preferred tonic spelling per pitch class (common major-key names).
  const TONIC_SPELL = ['C', 'Db', 'D', 'Eb', 'E', 'F', 'F#', 'G', 'Ab', 'A', 'Bb', 'B'];

  function parseSpell(en) {
    const m = /^([A-G])(#*|b*)$/.exec(en);
    if (!m) return null;
    const letterIdx = LETTERS.indexOf(m[1]);
    if (letterIdx < 0) return null;
    const accidental = m[2].charAt(0) === '#' ? m[2].length : (m[2].charAt(0) === 'b' ? -m[2].length : 0);
    return { letterIdx: letterIdx, accidental: accidental };
  }

  function accidentalFor(pc, letterIdx) {
    let delta = (((pc - NAT_PC[letterIdx]) % 12) + 12) % 12;
    if (delta > 6) delta -= 12;
    return delta;
  }

  function formatSpell(letterIdx, accidental) {
    const L = LETTERS[letterIdx];
    if (accidental === 0) return L;
    if (accidental > 0) return L + '#'.repeat(accidental);
    return L + 'b'.repeat(-accidental);
  }

  function scoreFitAssignment(assigned) {
    let score = 0;
    let sharps = 0;
    let flats = 0;
    for (let i = 0; i < assigned.length; i++) {
      const a = assigned[i].accidental;
      const abs = Math.abs(a);
      if (abs >= 3) score += 1000;
      else if (abs === 2) score += 12;
      else if (abs === 1) score += 1;
      if (a > 0) sharps++;
      if (a < 0) flats++;
      if (i > 0) {
        const step = (assigned[i].letterIdx - assigned[i - 1].letterIdx + 7) % 7;
        if (step === 0) score += 50;
        else if (step === 1) score -= 0.15;
        else score += (step - 1) * 0.05;
      }
    }
    if (sharps && flats) score += 3;
    if (assigned.length && assigned[0].accidental !== 0) score += 0.4;
    return score;
  }

  /**
   * Spell scale tones with unique staff letters when possible (e.g. C Phrygian →
   * C Db Eb F G Ab Bb). Returns English names for scale PCs plus a fallback
   * preference for off-scale / failed fits. Null `names` entries mean "use fallback".
   */
  function fitScaleNames(rootPc, orderedIvs) {
    const root = ((rootPc % 12) + 12) % 12;
    const ordered = orderedIvs || [];
    const uniq = [];
    const seen = new Set();
    for (let i = 0; i < ordered.length; i++) {
      const pc = (root + (ordered[i] % 12) + 12) % 12;
      if (seen.has(pc)) continue;
      seen.add(pc);
      uniq.push(pc);
    }
    const fallbackFlat = preferFlatFallback(root, uniq);
    if (uniq.length === 0 || uniq.length > 7) {
      return { names: null, preferFlat: fallbackFlat };
    }

    const tonic = parseSpell(TONIC_SPELL[root]);
    if (!tonic || accidentalFor(root, tonic.letterIdx) !== tonic.accidental) {
      return { names: null, preferFlat: fallbackFlat };
    }

    let best = null;
    let bestScore = Infinity;

    function search(i, usedMask, assigned) {
      if (i === uniq.length) {
        const score = scoreFitAssignment(assigned);
        if (score < bestScore) {
          bestScore = score;
          best = assigned.map((a) => ({ pc: a.pc, letterIdx: a.letterIdx, accidental: a.accidental }));
        }
        return;
      }
      const pc = uniq[i];
      if (i === 0) {
        assigned.push({ pc: pc, letterIdx: tonic.letterIdx, accidental: tonic.accidental });
        search(1, usedMask | (1 << tonic.letterIdx), assigned);
        assigned.pop();
        return;
      }
      for (let L = 0; L < 7; L++) {
        if (usedMask & (1 << L)) continue;
        const accidental = accidentalFor(pc, L);
        if (Math.abs(accidental) > 2) continue;
        assigned.push({ pc: pc, letterIdx: L, accidental: accidental });
        search(i + 1, usedMask | (1 << L), assigned);
        assigned.pop();
      }
    }

    search(0, 0, []);

    if (!best || bestScore >= 100) {
      return { names: null, preferFlat: fallbackFlat };
    }

    let fitCost = 0;
    for (let i = 0; i < best.length; i++) fitCost += Math.abs(best[i].accidental);
    const altCost = Math.min(tableAccidentalCost(uniq, FLAT), tableAccidentalCost(uniq, SHARP));
    // Prefer chromatic flat/sharp tables when unique-letter spelling needs more ink
    // (e.g. C blues → C D# E# F# G A# loses to C Eb F Gb G Bb).
    if (fitCost > altCost) {
      return { names: null, preferFlat: fallbackFlat };
    }

    const names = new Array(12).fill(null);
    let sharpAcc = 0;
    let flatAcc = 0;
    for (let i = 0; i < best.length; i++) {
      const a = best[i];
      names[a.pc] = formatSpell(a.letterIdx, a.accidental);
      if (a.accidental > 0) sharpAcc++;
      if (a.accidental < 0) flatAcc++;
    }
    const preferFlat = flatAcc > sharpAcc ? true : (sharpAcc > flatAcc ? false : fallbackFlat);
    return { names: names, preferFlat: preferFlat };
  }

  function tableAccidentalCost(pcs, table) {
    let cost = 0;
    for (let i = 0; i < pcs.length; i++) {
      const n = table[pcs[i]];
      for (let j = 0; j < n.length; j++) {
        if (n.charAt(j) === '#' || n.charAt(j) === 'b') cost++;
      }
    }
    return cost;
  }

  function preferFlatFallback(rootPc, scalePcs) {
    let flatHits = 0;
    let sharpHits = 0;
    for (let i = 0; i < scalePcs.length; i++) {
      const pc = scalePcs[i];
      if (FLAT[pc].indexOf('b') >= 0) flatHits++;
      if (SHARP[pc].indexOf('#') >= 0) sharpHits++;
    }
    if (flatHits !== sharpHits) return flatHits > sharpHits;
    return false;
  }

  function allScaleKeys() {
    const out = [];
    SCALE_CATS.forEach((cat) => cat.families.forEach((fk) => out.push(fk)));
    return out;
  }

  return {
    SHARP: SHARP,
    FLAT: FLAT,
    LETTERS: LETTERS,
    DEGREES: DEGREES,
    QUALITIES: QUALITIES,
    QUALITY_NAMES: QUALITY_NAMES,
    INTERVALS: INTERVALS,
    relLabel: relLabel,
    formatRel: formatRel,
    qualityFull: qualityFull,
    CHORD_SYMBOL: CHORD_SYMBOL,
    TUNINGS: TUNINGS,
    SCALES: SCALES,
    SCALE_CATS: SCALE_CATS,
    allScaleKeys: allScaleKeys,
    rotateScale: rotateScale,
    scaleOrdered: scaleOrdered,
    scalePcs: scalePcs,
    parentScale: parentScale,
    scaleChords: scaleChords,
    characteristicPairs: characteristicPairs,
    characteristicAlts: characteristicAlts,
    fitScaleNames: fitScaleNames,
    parseSpell: parseSpell,
    formatSpell: formatSpell,
    midiToFreq: midiToFreq,
    midiName: midiName
  };
});
