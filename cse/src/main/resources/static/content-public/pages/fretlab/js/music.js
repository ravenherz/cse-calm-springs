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

  const SCALES = {
    major: {
      label: 'Major (Church Modes)',
      modes: ['Ionian', 'Dorian', 'Phrygian', 'Lydian', 'Mixolydian', 'Aeolian', 'Locrian'],
      base: [0, 2, 4, 5, 7, 9, 11]
    },
    harmonicMinor: {
      label: 'Harmonic Minor',
      modes: ['Harmonic Minor', 'Locrian #6', 'Ionian #5', 'Dorian #4', 'Phrygian Major', 'Lydian #9', 'Altered bb7'],
      base: [0, 2, 3, 5, 7, 8, 11]
    },
    melodicMinor: {
      label: 'Melodic Minor',
      modes: ['Melodic Minor', 'Dorian b2', 'Lydian Augmented', 'Lydian Dominant', 'Mixolydian b6', 'Locrian #2', 'Super Locrian'],
      base: [0, 2, 3, 5, 7, 9, 11]
    },
    majorPentatonic: { label: 'Major Pentatonic', base: [0, 2, 4, 7, 9] },
    minorPentatonic: { label: 'Minor Pentatonic', base: [0, 3, 5, 7, 10] },
    blues: { label: 'Blues (hexatonic)', base: [0, 3, 5, 6, 7, 10] },
    dominant: { label: 'Mixolydian (Dominant)', base: [0, 2, 4, 5, 7, 9, 10] },
    wholeTone: { label: 'Whole Tone', base: [0, 2, 4, 6, 8, 10] },
    diminished: { label: 'Diminished (whole-half)', base: [0, 2, 3, 5, 6, 8, 9, 11] }
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
    rotateScale: rotateScale,
    scaleOrdered: scaleOrdered,
    scalePcs: scalePcs,
    midiToFreq: midiToFreq,
    midiName: midiName,
    fretFraction: fretFraction
  };
});
