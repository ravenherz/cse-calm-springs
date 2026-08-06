(function () {
  const M = window.Music;
  const $ = (s) => document.querySelector(s);
  const neck = $('#neck');
  const strLabels = $('#string-labels');
  const numLabels = $('#string-numbers');
  const boardWrap = $('#board-wrap');
  const tip = $('#tip');

  const SHAPE_DEFS = [
    { key: 'C', color: '#ff6b6b', pos2: 4, roots: '5th & 2nd strings', rootStrings: [1, 4] },
    { key: 'A', color: '#ffa94d', pos2: 1, roots: '5th & 3rd strings', rootStrings: [1, 3] },
    { key: 'G', color: '#51cf66', pos2: 3, roots: '6th, 3rd & 1st strings', rootStrings: [0, 3, 5] },
    { key: 'E', color: '#4dabf7', pos2: 0, roots: '6th & 1st strings', rootStrings: [0, 2, 5] },
    { key: 'D', color: '#b197fc', pos2: 2, roots: '4th & 2nd strings', rootStrings: [2, 4] }
  ];

  const INLAY_FRETS = [3, 5, 7, 9, 12, 15, 17, 19, 21, 24];
  const ROMAN = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI', 'XII'];
  const SCALE_STRINGS = {
    major: 'C A G E D',
    harmonicMinor: 'C A G E D',
    melodicMinor: 'C A G E D',
    majorPentatonic: 'C A G E D',
    minorPentatonic: 'C A G E D',
    blues: 'C A G E D'
  };

  const FORMULA = ['1', '♭2', '2', '♭3', '3', '4', '♯4', '5', '♭6', '6', '♭7', '7'];

  const MODE_INFO = {
    'Ionian': { mood: ['bright', 'happy', 'pure', 'stable'], char: 'perfect 4th + major 7th', desc: 'The major scale itself. No real tension to resolve — the sound of home and a happy ending.' },
    'Dorian': { mood: ['jazzy', 'soulful', 'hopeful', 'bluesy'], char: 'minor 3rd + major 6th', desc: 'A minor scale with a bright 6th. Santana solos, Irish folk, and modal jazz — sadness that still smiles.' },
    'Phrygian': { mood: ['dark', 'exotic', 'tense', 'Spanish'], char: 'minor 2nd (♭2)', desc: 'The ♭2 gives it a distinctly Spanish / Andalusian flavor. Flamenco and heavy-metal riffs live here.' },
    'Lydian': { mood: ['dreamy', 'ethereal', 'mystical', 'floating'], char: 'augmented 4th (♯4)', desc: 'The ♯4 makes everything feel airy and unresolved — film scores, dream-pop shimmer, classic Sci-Fi wonder.' },
    'Mixolydian': { mood: ['bluesy', 'rock', 'streetwise', 'laid-back'], char: 'minor 7th (♭7)', desc: 'The major scale with a bluesy flattened 7th — the sound of rock, blues, and Irish folk.' },
    'Aeolian': { mood: ['sad', 'melancholic', 'brooding', 'serious'], char: '♭3, ♭6, ♭7', desc: 'The natural minor scale — Western music\u2019s default for sadness, drama and dark emotion.' },
    'Locrian': { mood: ['unsettling', 'dissonant', 'scary', 'unstable'], char: 'diminished 5th (♭5)', desc: 'The tonic triad is diminished, so nothing ever truly resolves — dread, chaos, and deliberate \u201cwrongness\u201d.' },
    'Harmonic Minor': { mood: ['dark', 'exotic', 'haunting', 'Eastern-European'], char: '♭6 + major 7th', desc: 'The raised 7th over a ♭6 creates a signature leap — klezmer, gypsy, metal and classical minor melodies.' },
    'Locrian #6': { mood: ['dark', 'mysterious', 'unsettled'], char: '♭2, ♭5, ♯6', desc: 'Locrian with a raised 6th — shadowy and unstable.' },
    'Ionian #5': { mood: ['tense', 'suspended', 'bright but unresolved'], char: 'raised 5th (♯5)', desc: 'A major scale with a sharpened 5th — chords wobble between major and augmented.' },
    'Dorian #4': { mood: ['jazzy', 'quirky', 'bright'], char: 'raised 4th (♯4) + ♭3', desc: 'Dorian with a raised 4th — a jazzy, slightly exotic minor sound.' },
    'Phrygian Major': { mood: ['Spanish', 'flamenco', 'Arabic', 'metal'], char: '♭2 + major 3rd', desc: 'Also called Phrygian Dominant — the Andalusian sound of flamenco, gypsy jazz and Middle-Eastern metal.' },
    'Lydian #9': { mood: ['tense', 'dissonant', 'altered'], char: 'raised 9th (♯9) + ♯4', desc: 'An odd, chromatic-sounding major mode.' },
    'Altered bb7': { mood: ['dominant', 'dissonant', 'chaotic'], char: '♭3, ♭5, ♭♭7', desc: 'An altered dominant scale full of chromatic tension.' },
    'Melodic Minor': { mood: ['jazz', 'bittersweet', 'lyrical'], char: '♭3 + major 6th + 7th', desc: 'The jazz minor — a minor scale that rises with a major sound, a favorite for soloing over minor chords.' },
    'Dorian b2': { mood: ['mysterious', 'neoclassical', 'haunting'], char: '♭2 + ♭3', desc: 'Dorian with a flattened 2nd — dark and exotic.' },
    'Lydian Augmented': { mood: ['dreamy', 'airy', 'cinematic'], char: '♯4 + ♯5', desc: 'Lydian with a raised 5th — floating, majestic film-score material.' },
    'Lydian Dominant': { mood: ['bright', 'bluesy', 'fusion'], char: '♯4 + ♭7', desc: 'A dominant 7 chord with a ♯11 — the jazz-fusion and bebop dominant sound.' },
    'Mixolydian b6': { mood: ['sad-dominant', 'song-like', 'minor-leaning'], char: '♭7 + ♭6', desc: 'A dominant scale that bends toward minor — melancholic.' },
    'Locrian #2': { mood: ['dark', 'dissonant', 'jazz'], char: '♯2 + ♭5', desc: 'Locrian with a raised 2nd — dark and unstable.' },
    'Super Locrian': { mood: ['tense', 'extreme', 'free', 'altered'], char: '♭9 ♯9 ♭5 ♭13', desc: 'The altered scale — every note is a tension note, built for playing over altered dominant chords.' },
    'Phrygian Dominant': { mood: ['Spanish', 'flamenco', 'Arabic', 'metal'], char: '♭2 + major 3rd', desc: 'The Andalusian sound of flamenco, gypsy jazz and Middle-Eastern metal — a dominant chord with a flat 2nd.' },
    'Harmonic Major': { mood: ['bright', 'unsettled', 'classical'], char: '♭6 + major 7th', desc: 'The major scale with a flattened 6th — Ravel and jazz harmony love its dual major/minor colors.' },
    'Dorian b5': { mood: ['dark', 'jazzy', 'tense'], char: '♭5 + major 6th', desc: 'Dorian with a flattened 5th — a minor sound with a diminished core.' },
    'Phrygian b4': { mood: ['exotic', 'dark', 'mystical'], char: '♭2 + ♭4', desc: 'Phrygian with a flattened 4th — an unusual, ancient-sounding color.' },
    'Lydian b3': { mood: ['unstable', 'dreamy', 'uneasy'], char: '♯4 + ♭3', desc: 'Lydian bent toward minor — the tonic wavers between major and minor.' },
    'Mixolydian b2': { mood: ['dark', 'dominant', 'tense'], char: '♭2 + ♭7', desc: 'A dominant sound with a Phrygian lean — tense and dramatic.' },
    'Lydian Augmented #2': { mood: ['dense', 'altered', 'dissonant'], char: '♯2 ♯4 ♯5', desc: 'An extreme Lydian variant with three raised notes.' },
    'Locrian bb7': { mood: ['dark', 'unstable', 'diminished'], char: '♭5 + ♭♭7', desc: 'Locrian with a doubly-flattened 7th — a fully diminished flavor.' },
    'Double Harmonic Major': { mood: ['exotic', 'mystical', 'Arabic', 'Jewish'], char: 'two augmented 2nds', desc: 'The famous "double harmonic" sound — Jewish prayer, Arabic maqam, gypsy music.' },
    'Lydian #2 #6': { mood: ['bright', 'odd', 'shimmering'], char: '♯2 + ♯6 + ♯4', desc: 'An exotic Lydian variant with raised 2nd and 6th.' },
    'Ultraphrygian': { mood: ['dark', 'dissonant', 'extreme'], char: '♭2 + ♭3 + ♭4', desc: 'Phrygian with a flattened 4th too — very dark and unusual.' },
    'Hungarian Minor': { mood: ['haunting', 'gypsy', 'eastern-European'], char: '♭3 + ♯4 + ♯7', desc: 'The gypsy minor scale — two augmented 2nds, one minor 3rd, very dramatic.' },
    'Oriental': { mood: ['exotic', 'tense', 'mysterious'], char: '♭2 + ♯4', desc: 'A double-harmonic variant with a raised 4th — snake-charmer exotica.' },
    'Ionian b2 b5': { mood: ['unstable', 'dissonant', 'chromatic'], char: '♭2 + ♭5 + ♭6', desc: 'A major scale with two flattened notes — uneasy and rare.' },
    'Locrian b3 bb7': { mood: ['extreme', 'dark', 'diminished'], char: '♭3 + ♭5 + ♭♭7', desc: 'Doubly flattened — even darker than ordinary Locrian.' },
    'Neapolitan Major': { mood: ['romantic', 'dramatic', 'classical'], char: '♭2 + major 7th', desc: 'The Neapolitan sound — a major scale with a flat 2nd, used for dramatic classical cadences.' },
    'Leading Whole Tone': { mood: ['shimmering', 'unstable', 'floating'], char: '♭2 + whole-tone run', desc: 'Mostly whole steps over a flat 2nd — a dreamy, unstable Neapolitan mode.' },
    'Lydian #5 b7': { mood: ['fusion', 'tense', 'bright'], char: '♯4 + ♯5 + ♭7', desc: 'A dominant Lydian variant with a raised 5th.' },
    'Lydian b6 b7': { mood: ['minor-leaning', 'airy', 'song-like'], char: '♯4 + ♭6 + ♭7', desc: 'Lydian bent minor — a haunting raised-4th color.' },
    'Major Locrian': { mood: ['dark', 'unsettled', 'rare'], char: '♭2 + ♭5', desc: 'Locrian with a major 3rd — an exotic diminished-major clash.' },
    'Ionian #4 b6': { mood: ['tense', 'chromatic', 'unstable'], char: '♯4 + ♭6', desc: 'Major with raised 4th and flattened 6th — odd and rare.' },
    'Phrygian #2 #4': { mood: ['dark', 'exotic', 'chromatic'], char: '♯2 + ♯4', desc: 'Phrygian with raised 2nd and 4th — exotic and unstable.' },
    'Neapolitan Minor': { mood: ['dark', 'classical', 'dramatic'], char: '♭2 + ♭3 + ♭7', desc: 'The Neapolitan minor — a dark classical mode with a flat 2nd.' },
    'Phrygian #3': { mood: ['Spanish', 'major-minor', 'exotic'], char: '♭2 + major 3rd', desc: 'A Phrygian-dominant-like sound — flamenco and Arabic color.' },
    'Mixolydian #2': { mood: ['tense', 'dominant', 'chromatic'], char: '♯2 + ♭7', desc: 'A dominant mode with a sharpened 2nd.' },
    'Lydian #2 b6': { mood: ['dark', 'exotic', 'unsettled'], char: '♯2 + ♯4 + ♭6', desc: 'Lydian bent minor with a raised 2nd — dark and rare.' }
  };

  const SCALE_INFO = {
    majorPentatonic: { mood: ['bright', 'simple', 'safe'], char: 'no semitones', desc: 'Five notes with no half-steps — almost impossible to sound wrong. Pop, folk, country and blues.' },
    minorPentatonic: { mood: ['bluesy', 'rock', 'earthy'], char: 'no semitones', desc: 'The go-to scale for rock and blues guitar solos.' },
    blues: { mood: ['gritty', 'wailing', 'expressive'], char: 'adds the ♭5 \u201cblue note\u201d', desc: 'Minor pentatonic with a flattened 5th — the note that bends and wails.' },
    dominant: { mood: ['bluesy', 'rock', 'laid-back'], char: 'minor 7th (♭7)', desc: 'The Mixolydian sound — a dominant scale for bluesy rock.' },
    wholeTone: { mood: ['floating', 'dreamlike', 'ambiguous'], char: 'six equal whole steps', desc: 'No tonal center at all — Debussy and jazz use it for a hazy, unsettled shimmer.' },
    diminished: { mood: ['tense', 'dissonant', 'symmetrical'], char: 'alternating W / H', desc: 'Alternating whole and half steps — an 8-note scale full of built-in tension.' },
    augmented: { mood: ['dreamy', 'floating', 'magical'], char: 'two augmented triads', desc: 'Two augmented triads stacked — Debussy and Ravel use it for a hazy, glowing sound.' },
    diminishedHW: { mood: ['tense', 'symmetrical', 'dominant'], char: 'alternating H / W', desc: 'The half-whole diminished — the classic dominant-altered scale.' },
    chromatic: { mood: ['dense', 'total', '12-tone'], char: 'every half step', desc: 'All twelve notes — total chromatic color, atonal and free.' },
    messiaen3: { mood: ['modal', 'mystical', 'ritualistic'], char: '2-1-1 repeating', desc: 'Messiaen\u2019s Mode 3 — a symmetrical 9-note scale with a liturgical shimmer.' },
    messiaen4: { mood: ['symmetrical', 'modern', 'eerie'], char: '2-1-3-1-1 pattern', desc: 'Messiaen Mode 4 — an 8-note symmetrical scale.' },
    messiaen5: { mood: ['ethereal', 'modern', 'detached'], char: '1-4-1-1-1 pattern', desc: 'Messiaen Mode 5 — a sparse 6-note symmetric scale.' },
    messiaen6: { mood: ['colorful', 'modern', 'fragmented'], char: '2-2-1-1-2-2-1 pattern', desc: 'Messiaen Mode 6 — two interlocking tritone shapes.' },
    messiaen7: { mood: ['dense', 'total', 'modern'], char: '1-1-1-1-1-1-1-2-1', desc: 'Messiaen\u2019s Mode 7 — an almost-chromatic scale of 10 notes.' },
    suspendedPentatonic: { mood: ['ancient', 'open', 'modal', 'calm'], char: 'no 3rd', desc: 'The Egyptian / Yo scale — a pentatonic with a suspended, open sound.' },
    dominantPentatonic: { mood: ['bluesy', 'earthy', 'simple'], char: '♭7, no 3rd', desc: 'Pentatonic built on the dominant notes — a blues-friendly five-note set.' },
    hirajoshi: { mood: ['zen', 'Japanese', 'minimal'], char: 'two minor 2nds', desc: 'A classic koto tuning scale — sparse and evocative.' },
    iwato: { mood: ['Japanese', 'dark', 'minimal'], char: '♭2 + ♭5', desc: 'A dark Japanese scale with flat 2nd and flat 5th.' },
    insen: { mood: ['Japanese', 'melancholy', 'clean'], char: 'minor 2nd + tritone', desc: 'Another traditional Japanese scale — sparse and elegant.' },
    pelog: { mood: ['Balinese', 'exotic', 'gamelan'], char: 'wide leaps', desc: 'A Javanese/Balinese gamelan scale of seven tones.' },
    marwa: { mood: ['Arabic', 'exotic', 'serious'], char: '♭2 + major 3rd', desc: 'An Arabic maqam scale — dark and dignified.' },
    bluesMajor: { mood: ['soulful', 'cheerful', 'bluesy'], char: 'adds the ♭3 to major pentatonic', desc: 'The major blues scale — a bright pentatonic with a passing blue note.' },
    hexatonicMajor: { mood: ['bright', 'wide', 'incomplete'], char: '♭7 + major 7th', desc: 'A six-note major scale with a wide gap between 5th and 7th.' },
    hexatonicMinor: { mood: ['minor', 'wide', 'folk'], char: 'two 3-semitone leaps', desc: 'A six-note minor scale with big expressive leaps.' },
    prometheus: { mood: ['mysterious', 'orchestral', 'mystic'], char: '♭2 + ♯4 + ♭6', desc: 'The Scriabin "mystic" chord scale — mystical and brooding.' },
    bebopDominant: { mood: ['jazzy', 'driving', 'swinging'], char: 'adds a passing ♭7', desc: 'Dominant with a chromatic passing note — bebop phrasing flows smoothly.' },
    bebopMajor: { mood: ['jazzy', 'smooth', 'classic'], char: 'adds a passing ♯5', desc: 'Major with a passing chromatic — the classic bebop scale.' },
    bebopMinor: { mood: ['jazzy', 'dark', 'flowing'], char: 'adds a passing 2', desc: 'Minor with a chromatic passing note between 3rd and 5th.' },
    bebopDorian: { mood: ['funky', 'jazzy', 'soulful'], char: 'adds a passing 3', desc: 'Dorian with a passing chromatic — funky and fluid.' },
    bebopLocrian: { mood: ['dark', 'tense', 'jazz'], char: 'adds a passing 2', desc: 'Locrian with a passing note — tense and chromatic.' },
    minorSixDiminished: { mood: ['dark', 'smooth', 'classical', 'jazz'], char: '♭6 + major 7th', desc: 'The Barry Harris minor scale — a minor sound with a sharp 7th.' },
    majorSixDiminished: { mood: ['bright', 'swinging', 'classical'], char: '♭3 + major 7th', desc: 'The Barry Harris major scale — major with a passing chromatic 3rd.' },
    persian: { mood: ['Arabic', 'exotic', 'serious'], char: '♭2 + two augmented 2nds', desc: 'The Persian scale — exotic and regal, used in traditional Persian music.' },
    enigmatic: { mood: ['mysterious', 'dark', 'orchestral'], char: '♭2 + ♯4 + ♭7 + major 7th', desc: 'A theatrical scale from Italian opera — dark, chromatic and grand.' },
    spanishPhrygian: { mood: ['flamenco', 'Spanish', 'fiery'], char: '8 notes with ♭2 + ♭3 + ♭7', desc: 'The flamenco scale — Phrygian with a raised 3rd and a ♭7.' },
    todi: { mood: ['Indian', 'dark', 'meditative'], char: '♭2 + ♭3 + ♯4', desc: 'A Hindustani raga scale — devotional and serious.' },
    purvi: { mood: ['Indian', 'tense', 'dramatic'], char: '♭2 + ♯4', desc: 'A raga scale built on double-harmonic color.' },
    phrygianNatural6: { mood: ['dark', 'folk', 'exotic'], char: '♭2 + natural 6th', desc: 'Phrygian with a raised 6th — a folk Andalusian sound.' },
    mixolydianAugmented: { mood: ['fusion', 'dramatic', 'dominant'], char: '♯5 + ♭7', desc: 'A dominant scale with a raised 5th — bright and tense.' },
    lydianMinor: { mood: ['dreamy', 'minor', 'floating'], char: '♯4 + ♭3', desc: 'Lydian bent toward minor — airy and unresolved.' },
    hungarianMajor: { mood: ['gypsy', 'exotic', 'dramatic'], char: 'two augmented 2nds', desc: 'The Hungarian gypsy major — dark, fiery and folk-inflected.' },
    locrianNatural2: { mood: ['dark', 'unsettled', 'modern'], char: '♭3 + ♭5 with natural 2', desc: 'Locrian with a natural 2nd — dissonant but less sharp.' }
  };

  const state = {
    tab: 'caged',
    stringCount: 6,
    tuningId: 'std6',
    customPcs: [4, 9, 2, 7, 11, 4],
    fretCount: 15,
    flats: false,
    sound: true,
    showNames: true,
    showIntervals: true,
    showSteps: true,
    fullChart: false,
    cagedKey: 0,
    shapes: { C: true, A: true, G: true, E: true, D: true },
    showChordTones: true,
    showRootLines: true,
    scaleKey: 9,
    scaleFamily: 'major',
    scaleCategory: 'church',
    modeIndex: 0,
    bpm: 120,
    groove: 'downsideUp',
    chordNotes: []
  };

  let audioCtx = null;
  const freshShapes = new Set();
  let chordResult = null;
  let scaleSel = null;
  let noteSeq = 0;
  let ghostPlus = null;

  function names() {
    return state.flats ? M.FLAT : M.SHARP;
  }

  function tuningMidis() {
    if (state.tuningId === 'custom') return customMidi(state.customPcs);
    const t = M.TUNINGS.find((x) => x.id === state.tuningId);
    return t.midi.slice();
  }

  function customMidi(pcs) {
    const out = [40 + ((pcs[0] + 12 - 4) % 12)];
    for (let i = 1; i < pcs.length; i++) {
      let d = (pcs[i] - pcs[i - 1] + 12) % 12;
      if (d === 0) d = 12;
      out.push(out[i - 1] + d);
    }
    return out;
  }

  function rootPc() {
    return state.tab === 'caged' ? state.cagedKey : state.scaleKey;
  }

  function catForFamily(fk) {
    for (let i = 0; i < M.SCALE_CATS.length; i++) {
      if (M.SCALE_CATS[i].families.indexOf(fk) >= 0) return M.SCALE_CATS[i].id;
    }
    return null;
  }

  function allScaleKeys() {
    const out = [];
    M.SCALE_CATS.forEach((cat) => cat.families.forEach((fk) => out.push(fk)));
    return out;
  }

  const STORE_KEY = 'fretboard-lab-v1';

  function saveState() {
    try {
      localStorage.setItem(STORE_KEY, JSON.stringify({
        tab: state.tab,
        stringCount: state.stringCount,
        tuningId: state.tuningId,
        customPcs: state.customPcs,
        fretCount: state.fretCount,
        flats: state.flats,
        sound: state.sound,
        showNames: state.showNames,
        showIntervals: state.showIntervals,
        showSteps: state.showSteps,
        fullChart: state.fullChart,
        cagedKey: state.cagedKey,
        shapes: state.shapes,
        showChordTones: state.showChordTones,
        showRootLines: state.showRootLines,
        scaleKey: state.scaleKey,
        scaleFamily: state.scaleFamily,
        scaleCategory: state.scaleCategory,
        modeIndex: state.modeIndex,
        bpm: state.bpm,
        groove: state.groove,
        chordNotes: state.chordNotes.map((nt) => ({ s: nt.s, f: nt.f }))
      }));
    } catch (e) { /* storage unavailable */ }
  }

  function loadState() {
    let s;
    try {
      s = JSON.parse(localStorage.getItem(STORE_KEY));
    } catch (e) { s = null; }
    if (!s) return;
    state.tab = ['caged', 'scales', 'chords'].indexOf(s.tab) >= 0 ? s.tab : 'caged';
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
    }
    state.flats = !!s.flats;
    state.sound = s.sound !== false;
    state.showNames = s.showNames !== false;
    state.showIntervals = s.showIntervals !== false;
    state.showSteps = !!s.showSteps;
    state.fullChart = !!s.fullChart;
    state.cagedKey = Math.max(0, Math.min(11, s.cagedKey | 0));
    if (s.shapes && typeof s.shapes === 'object') {
      Object.keys(state.shapes).forEach((k) => { state.shapes[k] = s.shapes[k] !== false; });
    }
    state.showChordTones = s.showChordTones !== false;
    state.showRootLines = s.showRootLines !== false;
    state.scaleKey = Math.max(0, Math.min(11, s.scaleKey | 0));
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
  }

  function syncControls() {
    $('#ctl-strings').value = String(state.stringCount);
    $('#ctl-frets').value = String(state.fretCount);
    $('#ctl-flats').value = state.flats ? '1' : '0';
    $('#ctl-names').checked = state.showNames;
    $('#ctl-intervals').checked = state.showIntervals;
    $('#ctl-steps').checked = state.showSteps;
    $('#ctl-full').checked = state.fullChart;
    $('#ctl-sound').checked = state.sound;
    $('#ctl-chordtones').checked = state.showChordTones;
    $('#ctl-rootlines').checked = state.showRootLines;
    $('#ctl-bpm').value = String(state.bpm);
    $('#ctl-groove').value = state.groove;
  }

  function applyTabUI() {
    document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t.dataset.tab === state.tab));
    $('#ctl-caged').classList.toggle('hidden', state.tab !== 'caged');
    $('#ctl-scales').classList.toggle('hidden', state.tab !== 'scales');
    $('#ctl-chords').classList.toggle('hidden', state.tab !== 'chords');
    $('#ctl-playback').classList.toggle('hidden', state.tab === 'scales');
    $('#scale-details').classList.toggle('hidden', state.tab === 'chords');
    $('#chord-result').classList.toggle('hidden', state.tab !== 'chords');
  }

  function playMidi(m) {
    if (!state.sound) return;
    try {
      audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
      if (audioCtx.state === 'suspended') audioCtx.resume();
      const t = audioCtx.currentTime;
      const o = audioCtx.createOscillator();
      const g = audioCtx.createGain();
      const lp = audioCtx.createBiquadFilter();
      o.type = 'triangle';
      o.frequency.value = M.midiToFreq(m);
      lp.type = 'lowpass';
      lp.frequency.value = 3200;
      g.gain.setValueAtTime(0.0001, t);
      g.gain.exponentialRampToValueAtTime(0.45, t + 0.012);
      g.gain.exponentialRampToValueAtTime(0.0001, t + 1.1);
      o.connect(lp);
      lp.connect(g);
      g.connect(audioCtx.destination);
      o.start(t);
      o.stop(t + 1.2);
    } catch (e) { /* audio unavailable */ }
  }

  let seqTimers = [];

  function seqMark(sf, on) {
    neck.querySelectorAll('.marker[data-sf="' + sf + '"]').forEach((el) => el.classList.toggle('playing', on));
  }

  function orderNotes(notes, rootOverride) {
    const root = rootOverride != null ? rootOverride : rootPc();
    const up = notes.slice().sort((a, b) => a.m - b.m);
    const down = up.slice().reverse();
    const g = state.groove;
    if (g === 'upsideDown') return down;
    if (g === 'bothFromDown') return up.concat(down.slice(1));
    if (g === 'bothFromUp') return down.concat(up.slice(1));
    if (g === 'lowestRoot' || g === 'upperRoot') {
      const base = g === 'lowestRoot' ? up : down;
      const idx = base.findIndex((n) => (((n.m % 12) + 12) % 12) === root);
      return idx > 0 ? base.slice(idx) : base;
    }
    return up;
  }

  function playSequence(notes, rootOverride) {
    if (!state.sound || !notes.length) return;
    const gap = 60000 / state.bpm;
    seqTimers.forEach(clearTimeout);
    seqTimers = [];
    neck.querySelectorAll('.marker.playing').forEach((el) => el.classList.remove('playing'));
    const markMs = Math.min(380, gap * 0.75);
    orderNotes(notes, rootOverride).forEach((note, i) => {
      seqTimers.push(setTimeout(() => {
        playMidi(note.m);
        seqMark(note.s + ':' + note.f, true);
        seqTimers.push(setTimeout(() => seqMark(note.s + ':' + note.f, false), markMs));
      }, i * gap));
    });
  }

  function x(i) {
    const headstock = 10;
    const span = 100 - headstock;
    return headstock + (i / state.fretCount) * span;
  }

  function bandCenter(i) {
    if (i === 0) return x(0) - (bandCenter(1) - x(0));
    return (x(i - 1) + x(i)) / 2;
  }

  function leftBound(i) {
    return x(i - 1) - 0.2 * (x(1) - x(0));
  }

  function rightBound(i) {
    return Math.min(100, x(i) + 0.2 * (x(1) - x(0)));
  }

  function hexA(hex, a) {
    const n = parseInt(hex.slice(1), 16);
    return 'rgba(' + ((n >> 16) & 255) + ',' + ((n >> 8) & 255) + ',' + (n & 255) + ',' + a + ')';
  }

  function showTip(html, leftPct, topPx) {
    tip.innerHTML = html;
    tip.style.left = leftPct + '%';
    tip.style.top = topPx + 'px';
    tip.style.display = 'block';
  }

  function hideTip() {
    tip.style.display = 'none';
  }

  function makeMarker(pc, fret, stringIdx, midis, label, cls, color, colorR) {
    const m = document.createElement('div');
    m.className = 'marker ' + cls;
    if (color) {
      m.style.setProperty('--mc', color);
      m.style.setProperty('--mcL', color);
    }
    if (colorR) m.style.setProperty('--mcR', colorR);
    const left = bandCenter(fret);
    m.style.left = left + '%';
    m.style.top = label.top + 'px';
    m.style.zIndex = 10;
    m.dataset.sf = stringIdx + ':' + fret;

    const nameTxt = label.name;
    const intTxt = label.interval;
    const stepTxt = label.step;

    m.innerHTML =
      '<span class="mtop">' +
      '<span class="mn">' + nameTxt + '</span>' +
      (stepTxt ? '<span class="ms">' + stepTxt + '</span>' : '') +
      '</span>' +
      (intTxt ? '<span class="ml"></span><span class="mi">' + intTxt + '</span>' : '');

    const midi = midis[stringIdx] + fret;
    m.addEventListener('click', (e) => {
      e.stopPropagation();
      if (state.tab === 'chords') {
        const i = state.chordNotes.findIndex((nt) => nt.s === stringIdx && nt.f === fret);
        if (i >= 0) state.chordNotes.splice(i, 1);
        render();
        return;
      }
      playMidi(midi);
    });
    m.addEventListener('mouseenter', () => {
      showTip(
        '<b>' + (label.octaveName || nameTxt) + '</b>' +
        '<br>' + (label.fretText || 'fret ' + fret) +
        (label.interval ? '<br><span class="ti">' + label.interval + '</span>' : '') +
        (label.step ? '<br>scale degree ' + label.step : '') +
        (label.root ? '<br><span class="tr">root</span>' : '') +
        '<br>midi ' + midi,
        left,
        label.top - 52
      );
    });
    m.addEventListener('mouseleave', hideTip);
    neck.appendChild(m);
    return m;
  }

  function yArea() {
    const topPad = 46;
    const bottomPad = 16 + 42;
    const n = state.stringCount;
    return {
      topPad: topPad,
      height: topPad + n * 42 + bottomPad,
      step: 42,
      n: n
    };
  }

  function yPx(idx, area) {
    return area.topPad + (area.n - idx - 0.5) * area.step;
  }

  function fretFromX(pct) {
    for (let i = 0; i <= state.fretCount; i++) {
      if (pct < x(i)) return i;
    }
    return state.fretCount;
  }

  function interpretChord(I) {
    const has = (iv) => I.indexOf(iv) >= 0;
    const h = [];
    for (let i = 0; i < 12; i++) h.push(has(i));
    const c = { third: 0, seventh: 0, fifth: 0, sus: '', sixth: false, dim7: false, dim: false, aug: false, ext: {} };
    if (h[4]) c.third = 4;
    else if (h[3]) c.third = 3;
    if (h[11]) c.seventh = 11;
    else if (h[10]) c.seventh = 10;
    let shell = '';
    if (!c.seventh) {
      if (h[3] && h[6] && h[9] && !h[7] && !h[4]) { c.dim7 = true; shell = 'dim7'; }
      else if (h[9] && c.third !== 0) { c.sixth = true; shell = '6'; }
      else if (h[3] && h[6] && !h[7] && !h[4]) { c.dim = true; shell = 'dim'; }
      else if (h[4] && h[8] && !h[7]) { c.aug = true; shell = 'aug'; }
    }
    if (c.third === 0 && !shell) {
      if (h[5] && h[7]) c.sus = '4';
      else if (h[2] && h[7]) c.sus = '2';
    }
    if (c.dim7 || c.dim) c.fifth = 6;
    else if (h[6] && !h[7]) c.fifth = 6;
    else if (h[8] && !h[7]) c.fifth = 8;
    else if (h[7]) c.fifth = 7;
    if (c.seventh === 10 || c.seventh === 11) {
      c.ext.b9 = h[1];
      c.ext.n9 = h[2] && c.sus !== '2';
      c.ext.sh9 = h[3] && c.third === 4;
      c.ext.n11 = h[5] && c.sus !== '4';
      c.ext.sh11 = h[6] && c.fifth === 7;
      c.ext.b13 = h[8] && c.fifth === 7;
      c.ext.n13 = h[9] && c.fifth !== 6 && c.fifth !== 8;
    } else if (c.sixth) {
      c.ext.n9 = h[2];
      c.ext.n11 = h[5] && c.third !== 0;
      c.ext.b9 = h[1];
      c.ext.sh11 = h[6] && c.fifth === 7;
      c.ext.b13 = h[8] && c.fifth === 7;
    } else if (c.third !== 0) {
      c.ext.n9 = h[2];
      c.ext.n11 = h[5];
      c.ext.b9 = h[1];
      c.ext.sh9 = h[3] && c.third === 4;
    }
    c.ok = false;
    if (shell) c.ok = true;
    else if (c.seventh === 10 || c.seventh === 11) c.ok = c.third !== 0 || c.sus !== '' || c.fifth !== 0;
    else if (c.third !== 0) c.ok = true;
    else c.ok = c.sus !== '' || c.fifth === 7;
    return c;
  }

  function describeChord(c) {
    const num = c.ext.n13 ? '13' : (c.ext.n11 ? '11' : (c.ext.n9 ? '9' : '7'));
    const numWord = c.ext.n13 ? '13th' : (c.ext.n11 ? '11th' : (c.ext.n9 ? '9th' : '7th'));
    let suffix = '';
    let name = '';
    if (c.dim7) {
      suffix = 'dim7';
      name = 'diminished 7th';
    } else if (c.dim) {
      suffix = 'dim';
      name = 'diminished';
    } else if (c.aug) {
      suffix = 'aug';
      name = 'augmented';
    } else if (c.sixth) {
      const m = c.third === 3;
      suffix = (m ? 'm6' : '6');
      if (c.ext.n9) suffix += '/9';
      name = (m ? 'minor' : 'major') + ' 6th' + (c.ext.n9 ? ' with 9th' : '');
      const extra = [];
      if (c.ext.n11) extra.push('add11');
      if (c.ext.b9) extra.push('b9');
      if (c.ext.sh11) extra.push('#11');
      if (c.ext.b13) extra.push('b13');
      if (extra.length) suffix += '(' + extra.join(',') + ')';
    } else if (c.seventh === 11) {
      const m = c.third === 3;
      if (c.third === 0) {
        suffix = 'maj' + num + '(no3)';
        name = 'major ' + numWord + ' (no 3rd)';
      } else {
        suffix = (m ? 'mMaj' : 'maj') + num;
        name = (m ? 'minor-major ' : 'major ') + numWord;
      }
    } else if (c.seventh === 10) {
      if (c.sus) {
        suffix = num + 'sus' + c.sus;
        name = 'suspended dominant ' + numWord;
      } else if (c.third === 0) {
        suffix = num + '(no3)';
        name = numWord + ' (no 3rd)';
      } else {
        const m = c.third === 3;
        suffix = (m ? 'm' : '') + num;
        name = (m ? 'minor ' : 'dominant ') + numWord;
      }
    } else if (c.third !== 0) {
      const m = c.third === 3;
      suffix = m ? 'm' : '';
      name = m ? 'minor' : 'major';
      if (c.ext.n9 && c.ext.n11) {
        suffix += 'add11';
        name += ' added 11th';
      } else if (c.ext.n11) {
        suffix += 'add4';
        name += ' added 4th';
      } else if (c.ext.n9) {
        suffix += 'add9';
        name += ' added 9th';
      }
      const extra = [];
      if (c.ext.b9) extra.push('b9');
      if (c.ext.sh9) extra.push('#9');
      if (extra.length) suffix += '(' + extra.join(',') + ')';
      if (c.fifth === 6) { suffix += 'b5'; name += ' flat 5'; }
      else if (c.fifth === 8) { suffix += '#5'; name += ' sharp 5'; }
    } else if (c.sus) {
      suffix = 'sus' + c.sus;
      name = 'suspended ' + (c.sus === '4' ? '4th' : '2nd');
    } else {
      suffix = '5';
      name = 'power chord';
    }
    if (c.seventh === 10 || c.seventh === 11) {
      if (c.fifth === 6) { suffix += 'b5'; name += ' flat 5'; }
      else if (c.fifth === 8) { suffix += '#5'; name += ' sharp 5'; }
      if (c.ext.b9) { suffix += 'b9'; name += ' flat 9'; }
      if (c.ext.sh9) { suffix += '#9'; name += ' sharp 9'; }
      if (c.ext.sh11) { suffix += '#11'; name += ' sharp 11'; }
      if (c.ext.b13) { suffix += 'b13'; name += ' flat 13'; }
    }
    if (c.sixth || c.seventh === 10 || c.seventh === 11) {
      if (c.fifth === 0) suffix += '(no5)';
    } else if (c.third !== 0 && !c.dim && !c.aug && c.fifth === 0) {
      suffix += '(no5)';
    }
    return { suffix: suffix, name: name };
  }

  function chordScore(c) {
    let s = 3;
    if (c.third === 3 || c.third === 4) s += 4;
    else if (c.sus) s += 3;
    if (c.seventh === 10 || c.seventh === 11) s += 4;
    else if (c.sixth) s += 2;
    if (c.fifth === 7) s += 3;
    else if (c.fifth === 6 || c.fifth === 8) s += 1;
    if (c.dim7) s += 5;
    else if (c.dim) s += 2;
    if (c.ext.b9) s += 1;
    if (c.ext.n9) s += 1;
    if (c.ext.sh9) s += 1;
    if (c.ext.n11) s += 1;
    if (c.ext.sh11) s += 1;
    if (c.ext.b13) s += 1;
    if (c.ext.n13) s += 1;
    return s;
  }

  function essentialIntervals(c) {
    const s = new Set([0]);
    if (c.dim7) { s.add(3); s.add(6); s.add(9); return s; }
    if (c.dim) { s.add(3); s.add(6); return s; }
    if (c.aug) { s.add(4); s.add(8); return s; }
    if (c.sus) s.add(c.sus === '4' ? 5 : 2);
    else if (c.third) s.add(c.third);
    if (c.fifth) s.add(c.fifth);
    if (c.sixth) s.add(9);
    else if (c.seventh) s.add(c.seventh);
    return s;
  }

  function chordScales(rootPc, intervals) {
    const c = interpretChord(intervals);
    const shell = essentialIntervals(c);
    const full = [];
    const partial = [];
    const seen = {};
    const missing = (pcs) => {
      const ivs = intervals.filter((iv) => !shell.has(iv) && !pcs.has(iv));
      return ivs.length ? ' — missing ' + ivs.map((iv) => roleLabel(iv, c)).join(', ') : '';
    };
    allScaleKeys().forEach((fk) => {
      const fam = M.SCALES[fk];
      const n = fam.modes ? fam.modes.length : 1;
      for (let m = 0; m < n; m++) {
        const pcs = M.scalePcs(fk, m);
        const sig = Array.from(pcs).sort((a, b) => a - b).join(',');
        if (seen[sig]) continue;
        seen[sig] = true;
        const nm = fam.modes ? fam.modes[m] : fam.label;
        if (intervals.every((iv) => pcs.has(iv))) {
          full.push({ name: nm, note: '', pcs: pcs });
        } else if (Array.from(shell).every((iv) => pcs.has(iv))) {
          partial.push({ name: nm, note: missing(pcs), pcs: pcs });
        }
      }
    });
    return { full: full.slice(0, 8), partial: partial.slice(0, 6), fullMore: Math.max(0, full.length - 8), partialMore: Math.max(0, partial.length - 6), rootName: names()[rootPc] };
  }

  function roleLabel(iv, c) {
    if (iv === 0) return 'R';
    if (c.dim7 && iv === 9) return 'bb7';
    if (iv === 2) return c.ext.n9 ? '9' : '2';
    if (iv === 5) return (c.ext.n11 && (c.seventh || c.sixth)) ? '11' : '4';
    if (iv === 9) return (c.ext.n13 && c.seventh) ? '13' : '6';
    if (iv === 1) return c.ext.b9 ? 'b9' : 'b2';
    if (iv === 3) return c.ext.sh9 ? '#9' : 'b3';
    if (iv === 6) return c.ext.sh11 ? '#11' : 'b5';
    if (iv === 8) return c.fifth === 8 ? '#5' : (c.ext.b13 ? 'b13' : 'b6');
    return M.INTERVALS[iv];
  }

  function analyzeChord(midis) {
    if (!midis.length) return null;
    const pcs = Array.from(new Set(midis.map((m) => ((m % 12) + 12) % 12))).sort((a, b) => a - b);
    const bassPc = Math.min.apply(null, midis) % 12;
    const readings = [];
    pcs.forEach((r) => {
      const I = pcs.map((p) => (p - r + 12) % 12).sort((a, b) => a - b);
      const c = interpretChord(I);
      if (!c.ok) return;
      readings.push({ root: r, I: I, c: c, score: chordScore(c) });
    });
    readings.sort((a, b) => b.score - a.score || ((a.root === bassPc ? -1 : (b.root === bassPc ? 1 : a.root - b.root))));
    const best = readings[0] || null;
    const bassReading = readings.find((r) => r.root === bassPc) || null;
    const mkNotes = (rootPc) => pcs.map((pc) => ({ pc: pc, name: names()[pc] }));
    const labelFor = (rootPc, c) => {
      const lf = {};
      pcs.forEach((pc) => {
        const iv = (pc - rootPc + 12) % 12;
        lf[pc] = c ? roleLabel(iv, c) : M.INTERVALS[iv];
      });
      return lf;
    };
    if (!best) {
      return {
        matched: false,
        rootPc: bassPc,
        rootName: names()[bassPc],
        suffix: '',
        name: 'No standard chord found',
        intervals: [],
        exact: false,
        bassPc: bassPc,
        bassName: names()[bassPc],
        inversion: false,
        inversionName: '',
        slash: false,
        notes: mkNotes(bassPc),
        labelFor: labelFor(bassPc, null),
        alternatives: []
      };
    }
    const exoticBass = bassReading && (bassReading.c.dim || bassReading.c.dim7 || bassReading.c.aug || bassReading.c.fifth === 6 || bassReading.c.fifth === 8);
    let primary = best;
    if (bassReading && (best.root === bassPc || !exoticBass)) {
      primary = bassReading;
    }
    const desc = describeChord(primary.c);
    const slash = primary.root !== bassPc;
    const alternatives = readings.filter((r) => r !== primary).slice(0, 2).map((r) => ({ rootName: names()[r.root], suffix: describeChord(r.c).suffix }));
    return {
      matched: true,
      rootPc: primary.root,
      rootName: names()[primary.root],
      suffix: desc.suffix,
      name: desc.name,
      intervals: primary.I,
      exact: true,
      bassPc: bassPc,
      bassName: names()[bassPc],
      inversion: false,
      inversionName: '',
      slash: slash,
      notes: mkNotes(primary.root),
      labelFor: labelFor(primary.root, primary.c),
      alternatives: alternatives
    };
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
    ghostPlus.className = 'marker plain ghost-plus';
    ghostPlus.innerHTML = '<span class="cplus">+</span>';
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
    const lbl = {
      name: names()[pc],
      interval: rootPc >= 0 && state.showIntervals ? (chordResult.labelFor ? (chordResult.labelFor[pc] || M.INTERVALS[(pc - rootPc + 12) % 12]) : M.INTERVALS[(pc - rootPc + 12) % 12]) : '',
      step: '',
      octaveName: M.midiName(midi, state.flats) + (Math.floor(midi / 12) - 1),
      fretText: nt.f === 0 ? 'open string' : 'fret ' + nt.f,
      root: isRoot,
      top: yPx(nt.s, area)
    };
    makeMarker(pc, nt.f, nt.s, midis, lbl, isRoot ? 'root' : 'scale', null);
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
        if (!abs.has(pc)) continue;
        const rel = (pc - rootPc + 12) % 12;
        const deg = ivs.indexOf(rel);
        const name = state.showNames ? names()[pc] : '';
        const interval = state.showIntervals ? M.INTERVALS[rel] : '';
        const step = state.showSteps && deg >= 0 ? ROMAN[deg] : '';
        const m = document.createElement('div');
        m.className = 'marker plain soft ghost-scale';
        m.style.left = bandCenter(f) + '%';
        m.style.top = yPx(s, area) + 'px';
        m.style.zIndex = 3;
        m.style.pointerEvents = 'none';
        m.style.cursor = 'default';
        m.innerHTML =
          '<span class="mtop">' +
          '<span class="mn">' + name + '</span>' +
          (step ? '<span class="ms">' + step + '</span>' : '') +
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
    chordResult = analyzeChord(valid.map((nt) => midis[nt.s] + nt.f));
    if (scaleSel) {
      const sc = chordResult && chordResult.matched ? chordScales(chordResult.rootPc, chordResult.intervals) : null;
      const ok = sc && scaleSel.rootPc === chordResult.rootPc && sc.full.concat(sc.partial).some((e) => e.name === scaleSel.name);
      if (!ok) scaleSel = null;
    }
    const cnt = $('#chord-count');
    if (cnt) cnt.textContent = valid.length + (valid.length === 1 ? ' note' : ' notes');
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
      lbl.textContent = M.midiName(m, state.flats) + (Math.floor(m / 12) - 1);
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
      lab.textContent = box.key + ' shape';
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
      renderScaleGhost(midis, area);
    }
  }

  function markerLabels(mk, o) {
    const rootPc = o.rootPc;
    const intIdx = (mk.pc - rootPc + 12) % 12;
    const inScale = o.pcs.has(mk.pc);
    let interval = '';
    let step = '';
    let name = o.nm[mk.pc];
    const midiVal = o.midis[mk.stringIdx] + mk.fret;
    if (state.showNames) {
      name = o.nm[mk.pc];
    } else {
      name = '';
    }
    const octaveName = M.midiName(midiVal, state.flats) + (Math.floor(midiVal / 12) - 1);
    if (state.showIntervals) interval = M.INTERVALS[intIdx];
    if (state.showSteps && inScale) {
      const deg = o.ordered.findIndex((v) => (v + o.rootPc) % 12 === mk.pc);
      step = deg >= 0 ? ROMAN[deg] : '';
    }
    return {
      name: name,
      interval: interval,
      step: step,
      octaveName: octaveName,
      fretText: mk.fret === 0 ? 'open string' : 'fret ' + mk.fret,
      root: mk.pc === rootPc && inScale
    };
  }

  function computeData() {
    const midis = tuningMidis();
    const n = midis.length;
    const N = state.fretCount;
    const root = rootPc();
    const markers = [];
    const boxes = [];
    const taken = {};
    const overlap = {};
    let boxSeq = 0;
    const boxKeep = {};

    const ordered = M.scaleOrdered(state.scaleFamily, state.modeIndex);
    if (state.tab === 'caged') {
      const scalePcs = new Set(ordered.map((v) => (root + v) % 12));
      const chordPcs = [(root + ordered[0]) % 12, (root + ordered[2]) % 12, (root + ordered[4]) % 12];
      const shapes = SHAPE_DEFS.filter((s) => state.shapes[s.key]);
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
        const hits = overlap[keyId];
        const parts = keyId.split(':');
        const s = parseInt(parts[0], 10);
        const fr = parseInt(parts[1], 10);
        const pc = ((midis[s] + fr) % 12 + 12) % 12;
        taken[keyId] = true;
        let cls = 'scale';
        if (pc === root) cls = 'root';
        else if (state.showChordTones && chordPcs.indexOf(pc) >= 0) cls = 'chord';
        const kept = hits.some((h) => boxKeep[h.id] && boxKeep[h.id].has(keyId));
        if (!kept) {
          markers.push({
            pc: pc, fret: fr, stringIdx: s, cls: cls + ' soft',
            color: hits[0].color, shapes: hits.map((h) => h.key)
          });
          return;
        }
        if (cls === 'chord' && hits.length >= 2) {
          hits.sort((a, b) => a.lo - b.lo);
          markers.push({
            pc: pc, fret: fr, stringIdx: s, cls: 'chord split',
            color: hits[0].color, colorR: hits[hits.length - 1].color,
            shapes: hits.map((h) => h.key)
          });
        } else {
          markers.push({
            pc: pc, fret: fr, stringIdx: s, cls: cls, color: hits[0].color,
            shapes: hits.map((h) => h.key)
          });
        }
      });

      if (state.fullChart) {
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
              cls = pc === root ? 'root' : (state.showChordTones && chordPcs.indexOf(pc) >= 0 ? 'chord' : 'scale');
            }
            if (!inBox) cls += ' soft';
            markers.push({ pc: pc, fret: fr, stringIdx: s, cls: cls });
          }
        }
      }
    } else if (state.tab === 'chords') {
      return { markers: [], boxes: [], ordered: [], pcs: new Set() };
    } else {
      const ordered = M.scaleOrdered(state.scaleFamily, state.modeIndex);
      const pcs = new Set(ordered.map((v) => (root + v) % 12));
      const triad = new Set([ordered[0], ordered[2], ordered[4]].map((v) => (root + v) % 12));
      for (let s = 0; s < n; s++) {
        for (let fr = 0; fr <= N; fr++) {
          const pc = ((midis[s] + fr) % 12 + 12) % 12;
          const inScale = pcs.has(pc);
          if (!inScale && !state.fullChart) continue;
          let cls = 'plain';
          if (inScale) {
            cls = pc === root ? 'root' : (state.showChordTones && triad.has(pc) ? 'chord' : 'scale');
          }
          markers.push({ pc: pc, fret: fr, stringIdx: s, cls: cls });
        }
      }
      return { markers: markers, boxes: boxes, ordered: ordered, pcs: pcs };
    }

    const pcs = new Set(ordered.map((v) => (root + v) % 12));
    return { markers: markers, boxes: boxes, ordered: ordered, pcs: pcs };
  }

  function render() {
    hideTip();
    drawBoard();
    renderLegend();
    renderTipbox();
    renderScaleDetails();
    renderChordResult();
    saveState();
    freshShapes.clear();
  }

  function renderChordResult() {
    const el = $('#chord-result');
    if (!chordResult) {
      el.innerHTML = '<div class="cr-empty">No notes yet — click the fretboard to place notes and detect a chord.</div>';
      return;
    }
    const r = chordResult;
    const chips = r.notes.map((nt) => {
      const iv = (nt.pc - r.rootPc + 12) % 12;
      const inChord = r.matched && r.intervals.indexOf(iv) >= 0;
      const cls = nt.pc === r.rootPc ? 'cr-root' : (inChord ? 'cr-in' : 'cr-out');
      const lbl = r.labelFor ? (r.labelFor[nt.pc] || M.INTERVALS[iv]) : M.INTERVALS[iv];
      return '<span class="cr-chip ' + cls + '"><b>' + nt.name + '</b><i>' + lbl + '</i></span>';
    }).join('');
    let symbol = r.rootName + r.suffix;
    let sub = r.name;
    if (r.matched) {
      if (r.slash) {
        symbol += '/' + r.bassName;
        sub += ' · slash chord — ' + r.bassName + ' in the bass';
      } else {
        sub += ' · root position';
      }
    }
    let altHtml = '';
    if (r.alternatives.length) {
      altHtml = '<div class="cr-alt">Also heard as: ' + r.alternatives.map((a) => a.rootName + a.suffix).join('  ·  ') + '</div>';
    }
    let scalesHtml = '';
    if (r.matched) {
      const sc = chordScales(r.rootPc, r.intervals);
      const chip = (e, extra) => {
        const active = scaleSel && scaleSel.rootPc === r.rootPc && scaleSel.name === e.name ? ' active' : '';
        const pcs = Array.from(e.pcs).sort((a, b) => a - b).join(',');
        return '<button type="button" class="cr-scale-chip' + extra + active + '" data-r="' + r.rootPc + '" data-n="' + e.name + '" data-pcs="' + pcs + '">' + sc.rootName + ' ' + e.name + e.note + '</button>';
      };
      let fullRow = sc.full.map((e) => chip(e, '')).join('');
      if (sc.fullMore) fullRow += '<span class="cr-scale-chip cr-scale-more">+' + sc.fullMore + ' more</span>';
      let partRow = sc.partial.map((e) => chip(e, ' cr-scale-partial')).join('');
      if (sc.partialMore) partRow += '<span class="cr-scale-chip cr-scale-partial cr-scale-more">+' + sc.partialMore + ' more</span>';
      const hint = scaleSel ? '<span class="cr-scale-hint">ghost notes: ' + sc.rootName + ' ' + scaleSel.name + ' — click again to hide</span>' : '';
      if (fullRow || partRow) {
        scalesHtml =
          '<div class="cr-scales">' +
          '<div class="cr-scales-title">Scales that fit <b>' + r.rootName + r.suffix + '</b>' + hint + '</div>' +
          (fullRow ? '<div class="cr-scales-row">' + fullRow + '</div>' : '') +
          (partRow ? '<div class="cr-scales-row cr-scales-partials">' + partRow + '</div>' : '') +
          '</div>';
      }
    }
    el.innerHTML =
      '<div class="cr-head"><div class="cr-name">' + symbol + '</div><div class="cr-sub">' + sub + '</div></div>' +
      '<div class="cr-chips">' + chips + '</div>' +
      altHtml +
      scalesHtml;
  }

  function renderLegend() {
    const el = $('#legend');
    if (state.tab === 'caged') {
      const fam = M.SCALES[state.scaleFamily];
      const modeName = fam.modes ? fam.modes[state.modeIndex] : '';
      const desc = modeName ? modeName : fam.label;
      el.innerHTML =
        '<h3>Legend — CAGED System · ' + names()[state.cagedKey] + ' ' + desc + '</h3>' +
        '<div class="legend-items">' +
        legendItem('root', 'gold', 'Root note') +
        legendItem('chord', '#7dd3fc', 'Chord tone (1 · 3 · 5)') +
        legendItem('scale', '#3b6fd4', 'Scale note') +
        '</div>' +
        '<p class="hint">The 5 shapes (C · A · G · E · D) are movable forms that always appear up the neck in the order C → A → G → E → D → C. Each is a 5-fret window anchored to its root string (C: 5th &amp; 2nd, A: 5th &amp; 3rd, G: 6th/3rd/1st, E: 6th &amp; 1st, D: 4th &amp; 2nd), and the root note always sits on the 2nd fret of the box. Adjacent shapes overlap, and every box contains all scale steps. Click a shape button to toggle it. Turn on “All notes” to see every note on the neck. Hover a note for details.</p>';
    } else if (state.tab === 'chords') {
      el.innerHTML =
        '<h3>Legend — Chord Analyzer</h3>' +
        '<div class="legend-items">' +
        '<span class="legend-item"><i class="swatch swatch-plus">+</i><b>Placed note</b> — appears exactly where you click</span>' +
        '</div>' +
        '<p class="hint">Move the mouse over the neck — a gold + snaps to the nearest string/fret; click to place a note there, click a placed note to remove it. The chord is built from third, seventh, then extensions (9 / 11 / 13, with ♭9 ♯9 ♯11 ♭13 alterations). The lowest note is usually the root, but when the notes read more strongly as a chord over a different bass note the result is shown as a slash chord (e.g. Bbmaj7#5/F#). The result updates live below the fretboard — click any proposed scale to preview it as ghost notes across the whole neck (click again to hide; only one scale at a time).</p>';
    } else {
      const fam = M.SCALES[state.scaleFamily];
      const modeName = fam.modes ? fam.modes[state.modeIndex] : '';
      const desc = modeName ? modeName : fam.label;
      el.innerHTML =
        '<h3>Legend — ' + names()[state.scaleKey] + ' ' + desc + '</h3>' +
        '<div class="legend-items">' +
        legendItem('root', 'gold', 'Root / tonic') +
        legendItem('chord', '#7dd3fc', 'Triad tones (1 · 3 · 5)') +
        legendItem('scale', '#3b6fd4', 'Scale tone') +
        (state.fullChart ? legendItem('plain', '#666', 'Other notes') : '') +
        '</div>' +
        '<p class="hint">Intervals are shown relative to the root. Steps show the scale degree (1–' + fam.base.length + '). Turn on “All notes” to see every note on the neck.</p>';
    }
  }

  function legendItem(text, color, desc) {
    return '<span class="legend-item"><i class="swatch" style="background:' + color + '"></i><b>' + text + '</b> — ' + desc + '</span>';
  }

  function renderTipbox() {
    const el = $('#tipbox');
    if (state.tab === 'caged') {
      const fam = M.SCALES[state.scaleFamily];
      const modeName = fam.modes ? fam.modes[state.modeIndex] : '';
      const desc = modeName || fam.label;
      el.innerHTML = '<strong>CAGED system</strong> — <span>root ' + names()[state.cagedKey] + ' ' + desc + ' (' +
        SCALE_STRINGS.major + ' shapes). Shapes move up the neck in C-A-G-E-D order; each box is a 5-fret window with the root on its 2nd fret. Chord tones 1·3·5 are emphasized; the root is gold. Markers show note name, interval (relative to root) and scale-degree step.</span>';
    } else if (state.tab === 'chords') {
      el.innerHTML = '<strong>Chord analyzer</strong> — <span>place notes by clicking the fretboard; click a placed note again to remove it. The app names the chord and shows each note\u2019s role relative to its root.</span>';
    } else {
      const fam = M.SCALES[state.scaleFamily];
      const modeName = fam.modes ? fam.modes[state.modeIndex] : null;
      let txt;
      if (modeName) {
        txt = 'root ' + names()[state.scaleKey] + ' · ' + modeName + ' (mode ' + (state.modeIndex + 1) + ' of ' + state.scaleKeyName() + ' ' + fam.label.split(' ')[0] + ')';
      } else {
        txt = 'root ' + names()[state.scaleKey] + ' · ' + fam.label;
      }
      el.innerHTML = '<strong>' + (modeName || fam.label) + '</strong> — <span>' + txt + '</span>';
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
    return '<svg viewBox="0 0 200 200" role="img" aria-label="scale wheel">' + parts.join('') + '</svg>';
  }

  function renderScaleDetails() {
    const el = $('#scale-details');
    const fam = M.SCALES[state.scaleFamily];
    const nm = names();
    const root = state.tab === 'caged' ? state.cagedKey : state.scaleKey;
    const ordered = M.scaleOrdered(state.scaleFamily, state.modeIndex);
    const modeName = fam.modes ? fam.modes[state.modeIndex] : null;

    const theory = modeName ? MODE_INFO[modeName] : SCALE_INFO[state.scaleFamily];
    const mood = theory ? theory.mood : null;
    const desc = theory ? theory.desc : '';

    const title = nm[root] + ' ' + (modeName || fam.label);
    let parent = '';
    if (modeName) {
      const baseOrdered = M.scaleOrdered(state.scaleFamily, 0);
      const parentPc = (root + 12 - baseOrdered[0]) % 12;
      parent = 'mode ' + (state.modeIndex + 1) + ' of ' + nm[parentPc] + ' ' + fam.label.split(' ')[0];
    }

    const len = ordered.length;
    const steps = [];
    for (let i = 0; i < len; i++) {
      const b = i + 1 < len ? ordered[i + 1] : ordered[0] + 12;
      steps.push(b - ordered[i]);
    }
    const stepHtml = steps.map((d) => {
      const lab = d === 1 ? 'H' : d === 2 ? 'W' : String(d);
      const cls = d === 1 ? 'sd-step H' : d === 2 ? 'sd-step W' : 'sd-step';
      return '<span class="' + cls + '">' + lab + '</span>';
    }).join('<span class="sd-step-join">·</span>');

    const degHtml = ordered.map((v, i) => {
      const pc = (root + v) % 12;
      const cls = i === 0 ? 'sd-root' : (i === 2 || i === 4 ? 'sd-chord' : 'sd-scale');
      return '<div class="sd-deg ' + cls + '">' +
        '<span class="sd-deg-num">' + ROMAN[i] + '</span>' +
        '<span class="sd-deg-note">' + nm[pc] + '</span>' +
        '<span class="sd-deg-int">' + M.INTERVALS[v] + '</span>' +
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
          (charTxt ? '<p class="sd-char">Characteristic: <b>' + charTxt + '</b></p>' : '') +
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
    const keySel = $('#ctl-caged-key');
    keySel.innerHTML = nm.map((nn, i) => '<option value="' + i + '"' + (i === state.cagedKey ? ' selected' : '') + '>' + nn + '</option>').join('');
    const rootSel = $('#ctl-scale-root');
    rootSel.innerHTML = nm.map((nn, i) => '<option value="' + i + '"' + (i === state.scaleKey ? ' selected' : '') + '>' + nn + '</option>').join('');

    const famSel = $('#ctl-scale-family');
    famSel.innerHTML = familyOptions();
    const cagedFamSel = $('#ctl-caged-family');
    cagedFamSel.innerHTML = familyOptions();

    renderScaleCats();
    populateModeSelect();
    populateShapeButtons();
  }

  function familyOptions() {
    const cat = M.SCALE_CATS.find((c) => c.id === state.scaleCategory);
    const keys = (cat && cat.families.indexOf(state.scaleFamily) >= 0) ? cat.families : allScaleKeys();
    return keys.map((k) => '<option value="' + k + '"' + (k === state.scaleFamily ? ' selected' : '') + '>' + M.SCALES[k].label + '</option>').join('');
  }

  function renderScaleCats() {
    ['#scale-cats', '#caged-cats'].forEach((id) => {
      const wrap = $(id);
      wrap.innerHTML = '';
      M.SCALE_CATS.forEach((cat) => {
        const b = document.createElement('button');
        b.className = 'cat-tab' + (cat.id === state.scaleCategory ? ' active' : '');
        b.dataset.cat = cat.id;
        b.textContent = cat.label;
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
    });
  }

  function populateModeSelect() {
    const fam = M.SCALES[state.scaleFamily];
    ['#ctl-scale-mode', '#ctl-caged-mode'].forEach((selId) => {
      const sel = $(selId);
      if (!fam.modes) {
        sel.innerHTML = '<option>—</option>';
        sel.disabled = true;
        return;
      }
      sel.disabled = false;
      sel.innerHTML = fam.modes.map((mname, i) => '<option value="' + i + '"' + (i === state.modeIndex ? ' selected' : '') + '>' + mname + '</option>').join('');
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
      b.title = def.key + ' shape';
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

  function populateTunings() {
    const sel = $('#ctl-tuning');
    const list = M.TUNINGS.filter((t) => t.id === 'custom' || t.strings === state.stringCount);
    sel.innerHTML = list.map((t) => '<option value="' + t.id + '"' + (t.id === state.tuningId ? ' selected' : '') + '>' + t.label + '</option>').join('');
  }

  function renderCustomPanel() {
    const panel = $('#ctl-custom');
    const wrap = $('#custom-selects');
    if (state.tuningId !== 'custom') { panel.classList.add('hidden'); return; }
    panel.classList.remove('hidden');
    wrap.innerHTML = '';
    const nm = names();
    for (let i = 0; i < state.customPcs.length; i++) {
      const col = document.createElement('div');
      col.className = 'custom-col';
      const lab = document.createElement('span');
      lab.textContent = 'str ' + (state.customPcs.length - i);
      const sel = document.createElement('select');
      sel.innerHTML = nm.map((nn, p) => '<option value="' + p + '"' + (p === state.customPcs[i] ? ' selected' : '') + '>' + nn + '</option>').join('');
      sel.addEventListener('change', () => {
        state.customPcs[i] = parseInt(sel.value, 10);
        render();
      });
      col.appendChild(lab);
      col.appendChild(sel);
      wrap.appendChild(col);
    }
  }

  function switchTuning(id) {
    state.tuningId = id;
    if (id === 'custom') {
      if (state.customPcs.length !== state.stringCount) {
        const pad = [7, 2, 9, 4, 11, 6, 1, 8, 3, 10];
        while (state.customPcs.length < state.stringCount) state.customPcs.unshift(pad[state.customPcs.length]);
        while (state.customPcs.length > state.stringCount) state.customPcs.shift();
      }
    }
  }

  function bindEvents() {
    $('.tabs').addEventListener('click', (e) => {
      const b = e.target.closest('.tab');
      if (!b) return;
      state.tab = b.dataset.tab;
      applyTabUI();
      render();
    });

    $('#ctl-strings').addEventListener('change', (e) => {
      state.stringCount = parseInt(e.target.value, 10);
      const t = M.TUNINGS.find((x) => x.id === state.tuningId && x.strings === state.stringCount);
      if (state.tuningId !== 'custom') {
        const fallback = M.TUNINGS.find((x) => x.strings === state.stringCount && x.id !== 'custom');
        state.tuningId = t ? t.id : fallback.id;
      } else {
        while (state.customPcs.length < state.stringCount) state.customPcs.unshift(7);
        while (state.customPcs.length > state.stringCount) state.customPcs.shift();
      }
      populateTunings();
      renderCustomPanel();
      render();
    });

    $('#ctl-tuning').addEventListener('change', (e) => {
      switchTuning(e.target.value);
      renderCustomPanel();
      render();
    });

    $('#ctl-frets').addEventListener('change', (e) => {
      state.fretCount = parseInt(e.target.value, 10);
      render();
    });

    $('#ctl-names').addEventListener('change', (e) => { state.showNames = e.target.checked; render(); });
    $('#ctl-intervals').addEventListener('change', (e) => { state.showIntervals = e.target.checked; render(); });
    $('#ctl-steps').addEventListener('change', (e) => { state.showSteps = e.target.checked; render(); });
    $('#ctl-full').addEventListener('change', (e) => { state.fullChart = e.target.checked; render(); });
    $('#ctl-sound').addEventListener('change', (e) => { state.sound = e.target.checked; saveState(); });

    $('#ctl-bpm').addEventListener('change', (e) => {
      state.bpm = Math.max(40, Math.min(800, parseInt(e.target.value, 10) || 120));
      e.target.value = String(state.bpm);
      saveState();
    });
    $('#ctl-groove').addEventListener('change', (e) => { state.groove = e.target.value; saveState(); });
    $('#ctl-flats').addEventListener('change', (e) => {
      state.flats = e.target.value === '1';
      populateSelects();
      renderCustomPanel();
      render();
    });

    $('#ctl-caged-key').addEventListener('change', (e) => {
      state.cagedKey = parseInt(e.target.value, 10);
      render();
    });
    $('#ctl-caged-family').addEventListener('change', (e) => {
      state.scaleFamily = e.target.value;
      state.modeIndex = 0;
      populateModeSelect();
      render();
    });
    $('#ctl-caged-mode').addEventListener('change', (e) => {
      state.modeIndex = parseInt(e.target.value, 10);
      render();
    });
    $('#ctl-chordtones').addEventListener('change', (e) => { state.showChordTones = e.target.checked; render(); });
    $('#ctl-rootlines').addEventListener('change', (e) => { state.showRootLines = e.target.checked; render(); });

    $('#ctl-scale-root').addEventListener('change', (e) => {
      state.scaleKey = parseInt(e.target.value, 10);
      render();
    });
    $('#ctl-scale-family').addEventListener('change', (e) => {
      state.scaleFamily = e.target.value;
      state.modeIndex = 0;
      populateModeSelect();
      render();
    });
    $('#ctl-scale-mode').addEventListener('change', (e) => {
      state.modeIndex = parseInt(e.target.value, 10);
      render();
    });

    $('#btn-custom-reset').addEventListener('click', () => {
      const std = [40, 45, 50, 55, 59, 64];
      const base = M.TUNINGS.find((t) => t.strings === state.stringCount && t.id.indexOf('std') === 0);
      const arr = (base ? base.midi : std).map((m) => ((m % 12) + 12) % 12);
      while (arr.length < state.stringCount) arr.unshift((((std[0]) % 12) + 12) % 12);
      state.customPcs = arr.slice(0, state.stringCount);
      renderCustomPanel();
      render();
    });

    neck.addEventListener('click', (e) => {
      if (state.tab !== 'chords') return;
      const r = neck.getBoundingClientRect();
      const pct = ((e.clientX - r.left) / r.width) * 100;
      const y = e.clientY - r.top;
      const g = gridAt(pct, y);
      const i = state.chordNotes.findIndex((nt) => nt.s === g.s);
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
      render();
    });

    $('#btn-chord-play').addEventListener('click', () => {
      playChord();
    });

    $('#chord-result').addEventListener('click', (e) => {
      const chip = e.target.closest ? e.target.closest('.cr-scale-chip') : null;
      if (!chip || chip.classList.contains('cr-scale-more')) return;
      const r = parseInt(chip.dataset.r, 10);
      const n = chip.dataset.n;
      if (scaleSel && scaleSel.rootPc === r && scaleSel.name === n) {
        scaleSel = null;
      } else {
        scaleSel = { rootPc: r, name: n, ivs: chip.dataset.pcs.split(',').map(Number) };
      }
      render();
    });

    boardWrap.addEventListener('mousemove', (e) => {
      if (tip.style.display !== 'none') {
        const r = boardWrap.getBoundingClientRect();
        tip.style.left = Math.min(Math.max(e.clientX - r.left + 14, 0), r.width - 160) + 'px';
        tip.style.top = (e.clientY - r.top + 20) + 'px';
      }
    });
  }

  function init() {
    loadState();
    syncControls();
    applyTabUI();
    populateSelects();
    populateTunings();
    renderCustomPanel();
    bindEvents();
    SHAPE_DEFS.forEach((d) => freshShapes.add(d.key));
    render();
  }

  init();
})();
