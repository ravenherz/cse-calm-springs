(function (root, factory) {
  const NS = factory();
  root.Music = NS;
  if (typeof module !== 'undefined' && module.exports) module.exports = NS;
})(typeof self !== 'undefined' ? self : this, function () {
  const SHARP = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];
  const FLAT = ['C', 'Db', 'D', 'Eb', 'E', 'F', 'Gb', 'G', 'Ab', 'A', 'Bb', 'B'];
  const INTERVALS = ['R', 'b2', '2', 'b3', '3', '4', 'b5', '5', 'b6', '6', 'b7', '7'];
  const CHORD_SYMBOL = ['1', 'b2', '2', 'b3', '3', '4', '#4', '5', 'b6', '6', 'b7', '7'];

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
    { id: 'church', label: 'Church Modes', families: ['major'] },
    { id: 'melodic', label: 'Melodic Minor Modes', families: ['melodicMinor'] },
    { id: 'harmonic', label: 'Harmonic Minor Modes', families: ['harmonicMinor'] },
    { id: 'harmMajor', label: 'Harmonic Major Modes', families: ['harmonicMajor'] },
    { id: 'doubleHarm', label: 'Double Harmonic Modes', families: ['doubleHarmonic'] },
    { id: 'neapolitan', label: 'Neapolitan Modes', families: ['neapolitanMajor', 'neapolitanMinor'] },
    { id: 'symmetric', label: 'Symmetric & Limited Transposition', families: ['wholeTone', 'augmented', 'diminished', 'diminishedHW', 'chromatic', 'messiaen3', 'messiaen4', 'messiaen5', 'messiaen6', 'messiaen7'] },
    { id: 'penta', label: 'Pentatonic', families: ['majorPentatonic', 'minorPentatonic', 'suspendedPentatonic', 'dominantPentatonic', 'hirajoshi', 'iwato', 'insen', 'pelog', 'marwa'] },
    { id: 'hex', label: 'Hexatonic & Blues', families: ['blues', 'bluesMajor', 'hexatonicMajor', 'hexatonicMinor', 'prometheus'] },
    { id: 'bebop', label: 'Bebop', families: ['bebopDominant', 'bebopMajor', 'bebopMinor', 'bebopDorian', 'bebopLocrian'] },
    { id: 'jazz', label: 'Jazz & Dominant', families: ['dominant', 'minorSixDiminished', 'majorSixDiminished'] },
    { id: 'world', label: 'World & Exotic', families: ['persian', 'enigmatic', 'spanishPhrygian', 'todi', 'purvi', 'phrygianNatural6', 'mixolydianAugmented', 'lydianMinor', 'hungarianMajor', 'locrianNatural2'] }
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

  function midiToFreq(m) {
    return 440 * Math.pow(2, (m - 69) / 12);
  }

  function midiName(m, flat) {
    const pc = ((m % 12) + 12) % 12;
    const names = flat ? FLAT : SHARP;
    return names[pc];
  }

  function fretFraction(i) {
    return i;
  }

  return {
    SHARP: SHARP,
    FLAT: FLAT,
    INTERVALS: INTERVALS,
    CHORD_SYMBOL: CHORD_SYMBOL,
    TUNINGS: TUNINGS,
    SCALES: SCALES,
    SCALE_CATS: SCALE_CATS,
    rotateScale: rotateScale,
    scaleOrdered: scaleOrdered,
    scalePcs: scalePcs,
    midiToFreq: midiToFreq,
    midiName: midiName,
    fretFraction: fretFraction
  };
});
