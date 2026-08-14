(function () {
  /**
   * Curriculum catalogue. Copy lives in i18n (`theory.sections.*`, `theory.articles.*`).
   * Body may be a string[], or [{ h, p: string[] }] for sectioned prose.
   */
  const SECTIONS = [
    {
      id: 'foundations',
      articles: [
        {
          id: 'pitchAndOctave',
          related: ['intervals', 'spellingAndEnharmonics'],
          ctas: [
            {
              labelKey: 'theory.articles.pitchAndOctave.ctaNames',
              lesson: { root: 0, family: 'major', mode: 0, layer: 'none', showNames: true, showDegrees: false, showIntervals: false }
            }
          ]
        },
        {
          id: 'intervals',
          related: ['pitchAndOctave', 'building', 'spellingAndEnharmonics'],
          ctas: [
            {
              labelKey: 'theory.articles.intervals.ctaDegrees',
              lesson: { root: 0, family: 'major', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false }
            },
            {
              labelKey: 'theory.articles.intervals.ctaIntervals',
              lesson: { root: 0, family: 'major', mode: 0, layer: 'none', showNames: true, showDegrees: false, showIntervals: true }
            },
            { labelKey: 'theory.articles.intervals.ctaM3', lesson: { tab: 'chords', chordPcs: [0, 4], chordRoot: 0 } },
            { labelKey: 'theory.articles.intervals.ctaM3min', lesson: { tab: 'chords', chordPcs: [0, 3], chordRoot: 0 } }
          ]
        },
        {
          id: 'spellingAndEnharmonics',
          related: ['intervals', 'modes'],
          ctas: [
            {
              labelKey: 'theory.articles.spellingAndEnharmonics.ctaFit',
              lesson: { root: 6, family: 'major', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false, noteSpell: 'fit' }
            },
            {
              labelKey: 'theory.articles.spellingAndEnharmonics.ctaFlats',
              lesson: { root: 6, family: 'major', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false, noteSpell: 'flat' }
            }
          ]
        }
      ]
    },
    {
      id: 'chords',
      articles: [
        {
          id: 'building',
          related: ['intervals', 'inversionsAndSlash', 'extensionsAndAlts', 'chords'],
          ctas: [
            { labelKey: 'theory.articles.building.ctaMajor', lesson: { tab: 'chords', chordPcs: [0, 4, 7], chordRoot: 0 } },
            { labelKey: 'theory.articles.building.ctaMinor', lesson: { tab: 'chords', chordPcs: [0, 3, 7], chordRoot: 0 } },
            { labelKey: 'theory.articles.building.ctaDim', lesson: { tab: 'chords', chordPcs: [0, 3, 6], chordRoot: 0 } },
            { labelKey: 'theory.articles.building.ctaAug', lesson: { tab: 'chords', chordPcs: [0, 4, 8], chordRoot: 0 } },
            { labelKey: 'theory.articles.building.ctaMaj7', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 11], chordRoot: 0 } },
            { labelKey: 'theory.articles.building.ctaDom7', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 10], chordRoot: 0 } },
            { labelKey: 'theory.articles.building.ctaHalfDim', lesson: { tab: 'chords', chordPcs: [0, 3, 6, 10], chordRoot: 0 } }
          ]
        },
        {
          id: 'inversionsAndSlash',
          related: ['building', 'chords', 'voiceLeading'],
          ctas: [
            { labelKey: 'theory.articles.inversionsAndSlash.ctaRoot', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 11], chordRoot: 0 } },
            { labelKey: 'theory.articles.inversionsAndSlash.ctaFirst', lesson: { tab: 'chords', chordPcs: [4, 7, 11, 0], chordRoot: 0 } },
            { labelKey: 'theory.articles.inversionsAndSlash.ctaSlash', lesson: { tab: 'chords', chordPcs: [4, 0, 7], chordRoot: 0 } }
          ]
        },
        {
          id: 'extensionsAndAlts',
          related: ['building', 'chords', 'melodicMinor', 'harmonicMinor'],
          ctas: [
            { labelKey: 'theory.articles.extensionsAndAlts.ctaMaj9', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 11, 2], chordRoot: 0 } },
            { labelKey: 'theory.articles.extensionsAndAlts.ctaDom9', lesson: { tab: 'chords', chordPcs: [7, 11, 2, 5, 9], chordRoot: 7 } },
            { labelKey: 'theory.articles.extensionsAndAlts.ctaLyd', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 11, 6], chordRoot: 0 } },
            { labelKey: 'theory.articles.extensionsAndAlts.ctaAlt', lesson: { tab: 'chords', chordPcs: [7, 11, 1, 5, 8], chordRoot: 7 } }
          ]
        },
        {
          id: 'chords',
          related: ['building', 'extensionsAndAlts', 'inversionsAndSlash'],
          ctas: [
            { labelKey: 'theory.articles.chords.ctaMaj7', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 11], chordRoot: 0 } },
            { labelKey: 'theory.articles.chords.ctaDom7', lesson: { tab: 'chords', chordPcs: [7, 11, 2, 5], chordRoot: 7 } },
            { labelKey: 'theory.articles.chords.ctaHalfDim', lesson: { tab: 'chords', chordPcs: [11, 2, 5, 9], chordRoot: 11 } }
          ]
        }
      ]
    },
    {
      id: 'harmony',
      articles: [
        {
          id: 'harmony',
          related: ['building', 'functionalHarmony', 'cadencesAndTurnarounds'],
          ctas: [
            { labelKey: 'theory.articles.harmony.ctaIonian', lesson: { root: 0, family: 'major', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.harmony.ctaIi', lesson: { tab: 'chords', chordPcs: [2, 5, 9, 0], chordRoot: 2 } },
            { labelKey: 'theory.articles.harmony.ctaV', lesson: { tab: 'chords', chordPcs: [7, 11, 2, 5], chordRoot: 7 } },
            { labelKey: 'theory.articles.harmony.ctaI', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 11], chordRoot: 0 } },
            { labelKey: 'theory.articles.harmony.ctaDrill', lesson: { tab: 'drills' } }
          ]
        },
        {
          id: 'functionalHarmony',
          related: ['harmony', 'cadencesAndTurnarounds', 'secondaryDominants'],
          ctas: [
            { labelKey: 'theory.articles.functionalHarmony.ctaV', lesson: { tab: 'chords', chordPcs: [7, 11, 2, 5], chordRoot: 7 } },
            { labelKey: 'theory.articles.functionalHarmony.ctaI', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 11], chordRoot: 0 } },
            { labelKey: 'theory.articles.functionalHarmony.ctaIV', lesson: { tab: 'chords', chordPcs: [5, 9, 0, 4], chordRoot: 5 } }
          ]
        },
        {
          id: 'cadencesAndTurnarounds',
          related: ['functionalHarmony', 'harmony', 'secondaryDominants'],
          ctas: [
            { labelKey: 'theory.articles.cadencesAndTurnarounds.ctaAuth', lesson: { tab: 'chords', chordPcs: [7, 11, 2], chordRoot: 7 } },
            { labelKey: 'theory.articles.cadencesAndTurnarounds.ctaPlagal', lesson: { tab: 'chords', chordPcs: [5, 9, 0], chordRoot: 5 } },
            { labelKey: 'theory.articles.cadencesAndTurnarounds.ctaDecep', lesson: { tab: 'chords', chordPcs: [9, 0, 4, 7], chordRoot: 9 } },
            { labelKey: 'theory.articles.cadencesAndTurnarounds.ctaTurn', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 11], chordRoot: 0 } }
          ]
        },
        {
          id: 'secondaryDominants',
          related: ['functionalHarmony', 'cadencesAndTurnarounds', 'harmony'],
          ctas: [
            { labelKey: 'theory.articles.secondaryDominants.ctaVV', lesson: { tab: 'chords', chordPcs: [2, 6, 9, 0], chordRoot: 2 } },
            { labelKey: 'theory.articles.secondaryDominants.ctaV', lesson: { tab: 'chords', chordPcs: [7, 11, 2, 5], chordRoot: 7 } },
            { labelKey: 'theory.articles.secondaryDominants.ctaI', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 11], chordRoot: 0 } }
          ]
        },
        {
          id: 'minorHarmony',
          related: ['harmony', 'harmonicMinor', 'melodicMinor', 'functionalHarmony'],
          ctas: [
            { labelKey: 'theory.articles.minorHarmony.ctaAeolian', lesson: { root: 9, family: 'major', mode: 5, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.minorHarmony.ctaHarm', lesson: { root: 9, family: 'harmonicMinor', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.minorHarmony.ctaV', lesson: { tab: 'chords', chordPcs: [4, 8, 11, 2], chordRoot: 4 } },
            { labelKey: 'theory.articles.minorHarmony.ctaIm', lesson: { tab: 'chords', chordPcs: [9, 0, 4, 7], chordRoot: 9 } }
          ]
        },
        {
          id: 'voiceLeading',
          related: ['inversionsAndSlash', 'functionalHarmony', 'harmony'],
          ctas: [
            { labelKey: 'theory.articles.voiceLeading.ctaV', lesson: { tab: 'chords', chordPcs: [7, 11, 2, 5], chordRoot: 7 } },
            { labelKey: 'theory.articles.voiceLeading.ctaI', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 11], chordRoot: 0 } }
          ]
        }
      ]
    },
    {
      id: 'scales',
      articles: [
        {
          id: 'modes',
          related: ['intervals', 'melodicMinor', 'harmonicMinor', 'harmony'],
          ctas: [
            { labelKey: 'theory.articles.modes.ctaIonian', lesson: { root: 0, family: 'major', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.modes.ctaDorian', lesson: { root: 2, family: 'major', mode: 1, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.modes.ctaPhrygian', lesson: { root: 4, family: 'major', mode: 2, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.modes.ctaLydian', lesson: { root: 5, family: 'major', mode: 3, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.modes.ctaMixo', lesson: { root: 7, family: 'major', mode: 4, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } }
          ]
        },
        {
          id: 'melodicMinor',
          related: ['modes', 'extensionsAndAlts', 'minorHarmony'],
          ctas: [
            { labelKey: 'theory.articles.melodicMinor.ctaMm', lesson: { root: 0, family: 'melodicMinor', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.melodicMinor.ctaLydDom', lesson: { root: 5, family: 'melodicMinor', mode: 3, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.melodicMinor.ctaAlt', lesson: { root: 7, family: 'melodicMinor', mode: 6, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } }
          ]
        },
        {
          id: 'harmonicMinor',
          related: ['modes', 'minorHarmony', 'extensionsAndAlts'],
          ctas: [
            { labelKey: 'theory.articles.harmonicMinor.ctaHm', lesson: { root: 9, family: 'harmonicMinor', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.harmonicMinor.ctaPhryDom', lesson: { root: 4, family: 'harmonicMinor', mode: 4, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } }
          ]
        },
        {
          id: 'pentatonicAndBlues',
          related: ['modes', 'readingTheNeck'],
          ctas: [
            { labelKey: 'theory.articles.pentatonicAndBlues.ctaMinP', lesson: { root: 9, family: 'minorPentatonic', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.pentatonicAndBlues.ctaMajP', lesson: { root: 0, family: 'majorPentatonic', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.pentatonicAndBlues.ctaBlues', lesson: { root: 9, family: 'blues', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } }
          ]
        },
        {
          id: 'symmetricScales',
          related: ['modes', 'extensionsAndAlts'],
          ctas: [
            { labelKey: 'theory.articles.symmetricScales.ctaWt', lesson: { root: 0, family: 'wholeTone', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.symmetricScales.ctaDim', lesson: { root: 0, family: 'diminished', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.symmetricScales.ctaHw', lesson: { root: 0, family: 'diminishedHW', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } }
          ]
        },
        {
          id: 'bebopAndChromatic',
          related: ['modes', 'melodicMinor', 'readingTheNeck'],
          ctas: [
            { labelKey: 'theory.articles.bebopAndChromatic.ctaDom', lesson: { root: 7, family: 'bebopDominant', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.bebopAndChromatic.ctaMaj', lesson: { root: 0, family: 'bebopMajor', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } }
          ]
        }
      ]
    },
    {
      id: 'guitar',
      articles: [
        {
          id: 'systems',
          related: ['modes', 'quartalHarmony', 'readingTheNeck'],
          ctas: [
            { labelKey: 'theory.articles.systems.ctaCaged', lesson: { root: 0, family: 'major', mode: 0, layer: 'caged', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.systems.ctaNps', lesson: { root: 0, family: 'major', mode: 0, layer: 'nps', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.systems.ctaBerklee', lesson: { root: 0, family: 'major', mode: 0, layer: 'berklee', showNames: true, showDegrees: true, showIntervals: false } }
          ]
        },
        {
          id: 'quartalHarmony',
          related: ['building', 'systems', 'modes'],
          ctas: [
            { labelKey: 'theory.articles.quartalHarmony.ctaLayer', lesson: { root: 0, family: 'major', mode: 1, layer: 'quartal', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.quartalHarmony.ctaQuartal', lesson: { tab: 'chords', chordPcs: [0, 5, 10], chordRoot: 0 } },
            { labelKey: 'theory.articles.quartalHarmony.ctaTertian', lesson: { tab: 'chords', chordPcs: [0, 4, 7], chordRoot: 0 } }
          ]
        },
        {
          id: 'readingTheNeck',
          related: ['intervals', 'systems', 'chords', 'harmony'],
          ctas: [
            { labelKey: 'theory.articles.readingTheNeck.ctaExplore', lesson: { root: 0, family: 'major', mode: 0, layer: 'none', showNames: true, showDegrees: true, showIntervals: false } },
            { labelKey: 'theory.articles.readingTheNeck.ctaDrills', lesson: { tab: 'drills' } },
            { labelKey: 'theory.articles.readingTheNeck.ctaMaj7', lesson: { tab: 'chords', chordPcs: [0, 4, 7, 11], chordRoot: 0 } },
            { labelKey: 'theory.articles.readingTheNeck.ctaSlash', lesson: { tab: 'chords', chordPcs: [4, 0, 7], chordRoot: 0 } }
          ]
        }
      ]
    }
  ];

  const ARTICLES = [];
  SECTIONS.forEach((sec) => {
    sec.articles.forEach((a) => ARTICLES.push(a));
  });

  const ABOUT_ID = 'about';

  /** Chord alteration token → Explore mode (root stays the chord root). */
  const ALT_HOOKS = {
    '#11': { family: 'major', modeIndex: 3, article: 'extensionsAndAlts' },
    'b9': { family: 'harmonicMinor', modeIndex: 4, article: 'extensionsAndAlts' },
    '#9': { family: 'harmonicMinor', modeIndex: 5, article: 'extensionsAndAlts' },
    'b13': { family: 'melodicMinor', modeIndex: 4, article: 'extensionsAndAlts' },
    '#5': { family: 'melodicMinor', modeIndex: 2, article: 'extensionsAndAlts' },
    'b5': { family: 'major', modeIndex: 6, article: 'extensionsAndAlts' }
  };

  function normalizeAltToken(tok) {
    return String(tok == null ? '' : tok)
      .replace(/♯/g, '#')
      .replace(/♭/g, 'b')
      .replace(/\s+/g, '');
  }

  function hookForAlteration(tok) {
    const key = normalizeAltToken(tok);
    return ALT_HOOKS[key] || null;
  }

  function readmeMarkdown() {
    return (window.FretReadme && typeof window.FretReadme.markdown === 'string')
      ? window.FretReadme.markdown
      : '';
  }

  function create(deps) {
    const $ = deps.$;
    const t = deps.t;
    const esc = deps.esc;
    const openLesson = deps.openLesson;
    const applyDemo = deps.applyDemo || function () {};
    const onNavigate = deps.onNavigate || function () {};
    const placeChordPcs = deps.placeChordPcs || function () { return null; };
    const miniNeckSvg = deps.miniNeckSvg || function () { return ''; };
    const staffSvg = deps.staffSvg || function () { return ''; };
    const staffNotesToMidi = deps.staffNotesToMidi || function () { return []; };
    const decodeVoicing = deps.decodeVoicing || function () { return null; };
    const playSimultaneous = deps.playSimultaneous || function () {};
    const playMidi = deps.playMidi || function () {};
    const tuningMidis = deps.tuningMidis || function () { return []; };
    const names = deps.names || function () { return []; };
    const state = deps.state || {};
    const panel = $('#theory-panel');
    const nav = $('#theory-nav');
    const body = $('#theory-body');
    let articleId = ARTICLES[0] ? ARTICLES[0].id : null;
    let bound = false;
    let demoSyncedFor = null;
    let activeFigCta = null;
    let figPlayTimers = [];

    function clearFigPlayTimers() {
      figPlayTimers.forEach(clearTimeout);
      figPlayTimers = [];
    }

    function playFigureMidis(midiList, arpeggio) {
      clearFigPlayTimers();
      if (!midiList || !midiList.length) return;
      if (arpeggio && midiList.length > 1 && playMidi) {
        midiList.forEach((m, i) => {
          figPlayTimers.push(setTimeout(() => playMidi(m), i * 280));
        });
        return;
      }
      playSimultaneous(midiList);
    }

    function articleById(id) {
      for (let i = 0; i < ARTICLES.length; i++) {
        if (ARTICLES[i].id === id) return ARTICLES[i];
      }
      return null;
    }

    function boardableLesson(lesson) {
      if (!lesson || typeof lesson !== 'object') return null;
      if (lesson.tab === 'drills') return null;
      return lesson;
    }

    function demoLessonFor(art) {
      if (!art) return null;
      if (art.demo) return boardableLesson(art.demo);
      const ctas = art.ctas || [];
      for (let i = 0; i < ctas.length; i++) {
        const lesson = boardableLesson(ctas[i].lesson);
        if (lesson) return lesson;
      }
      return null;
    }

    function syncDemo(force) {
      if (!force && demoSyncedFor === articleId) return;
      demoSyncedFor = articleId;
      if (articleId === ABOUT_ID) {
        activeFigCta = null;
        applyDemo(null);
        return;
      }
      const art = articleById(articleId);
      const lesson = demoLessonFor(art);
      if (lesson && art && art.ctas) {
        const idx = art.ctas.findIndex((c) => c.lesson === lesson ||
          (c.lesson && lesson && c.lesson.tab === lesson.tab &&
            JSON.stringify(c.lesson.chordPcs || null) === JSON.stringify(lesson.chordPcs || null) &&
            c.lesson.chordRoot === lesson.chordRoot &&
            c.lesson.root === lesson.root &&
            c.lesson.family === lesson.family &&
            c.lesson.mode === lesson.mode));
        activeFigCta = idx >= 0 ? idx : 0;
      } else {
        activeFigCta = null;
      }
      applyDemo(lesson);
    }

    function renderFigures(figures, art) {
      if (!Array.isArray(figures) || !figures.length) return '';
      const midis = tuningMidis();
      const nStr = midis.length || state.stringCount || 6;
      const fretCount = state.fretCount || 22;
      const nm = names();
      const playLabel = t('theory.figurePlay');
      const parts = figures.map((fig, fi) => {
        if (!fig || typeof fig !== 'object') return '';
        let svg = '';
        let midiList = [];
        let arpeggio = false;
        let kind = '';
        if (fig.type === 'miniNeck') {
          kind = ' theory-figure-neck';
          const pcs = Array.isArray(fig.chordPcs) ? fig.chordPcs : [];
          let notes = null;
          if (fig.voicing) notes = decodeVoicing(String(fig.voicing));
          if ((!notes || !notes.length) && pcs.length) {
            const span = fig.maxFrettedSpan != null && Number.isFinite(fig.maxFrettedSpan)
              ? fig.maxFrettedSpan
              : 4;
            notes = placeChordPcs(pcs, midis, fretCount, null, { maxFrettedSpan: span });
          }
          if (!notes || !notes.length) return '';
          const root = fig.chordRoot != null
            ? (((fig.chordRoot % 12) + 12) % 12)
            : (pcs.length ? (((pcs[0] % 12) + 12) % 12) : null);
          midiList = notes.map((nt) => {
            if (nt.m != null && Number.isFinite(nt.m)) return nt.m;
            return midis[nt.s] + nt.f;
          }).filter((m) => Number.isFinite(m));
          svg = miniNeckSvg(notes, nStr, root, midis, {
            labels: fig.labels || 'degrees',
            names: nm,
            className: 'theory-fig-svg'
          });
        } else if (fig.type === 'staff') {
          kind = ' theory-figure-staff';
          arpeggio = fig.layout === 'melodic';
          midiList = staffNotesToMidi(fig.notes) || [];
          svg = staffSvg(fig.notes, {
            layout: fig.layout || 'harmonic',
            className: 'theory-fig-svg'
          });
        } else if (fig.type === 'formula' || fig.type === 'romans') {
          kind = ' theory-figure-strip';
          const tokens = Array.isArray(fig.tokens) ? fig.tokens : [];
          if (!tokens.length) return '';
          const chip = fig.type === 'romans' ? 'theory-strip-roman' : 'theory-strip-token';
          const join = fig.type === 'romans' ? '→' : '·';
          svg = '<div class="theory-strip theory-strip-' + fig.type + '" aria-hidden="true">' +
            tokens.map((tok, i) =>
              (i ? '<span class="theory-strip-join">' + join + '</span>' : '') +
              '<span class="' + chip + '">' + esc(String(tok)) + '</span>'
            ).join('') +
            '</div>';
        } else {
          return '';
        }
        if (!svg) return '';
        const caption = fig.caption ? String(fig.caption) : '';
        const ctaIdx = fig.demoCta != null && Number.isFinite(fig.demoCta) ? (fig.demoCta | 0) : -1;
        const active = ctaIdx >= 0 && ctaIdx === activeFigCta ? ' on' : '';
        const aria = caption || t('theory.figureAria');
        const hitOpen = ctaIdx >= 0
          ? '<button type="button" class="theory-figure-hit" data-theory-fig-cta="' + ctaIdx + '"' +
            ' title="' + esc(aria) + '" aria-label="' + esc(aria) + '">'
          : '<div class="theory-figure-hit" role="img" aria-label="' + esc(aria) + '">';
        const hitClose = ctaIdx >= 0 ? '</button>' : '</div>';
        const playBtn = midiList.length
          ? '<button type="button" class="theory-figure-play" data-theory-fig-play' +
            (arpeggio ? ' data-fig-arpeggio="1"' : '') +
            ' data-fig-midis="' + midiList.join(',') + '"' +
            ' title="' + esc(playLabel) + '" aria-label="' + esc(playLabel) + '">▶</button>'
          : '';
        return '<div class="theory-figure' + kind + (ctaIdx < 0 ? ' static' : '') + active + '"' +
          ' data-theory-fig="' + fi + '">' +
          hitOpen +
          svg +
          (caption ? '<span class="theory-figure-cap">' + esc(caption) + '</span>' : '') +
          hitClose +
          playBtn +
          '</div>';
      }).filter(Boolean);
      if (!parts.length) return '';
      return '<div class="theory-figures" role="group">' + parts.join('') + '</div>';
    }

    function renderProse(id, art) {
      const raw = t('theory.articles.' + id + '.body');
      if (!raw) return '';
      if (typeof raw === 'string' && raw !== 'theory.articles.' + id + '.body') {
        return '<p>' + esc(raw) + '</p>';
      }
      if (!Array.isArray(raw)) return '';
      let html = '';
      raw.forEach((block) => {
        if (typeof block === 'string' && block) {
          html += '<p>' + esc(block) + '</p>';
          return;
        }
        if (!block || typeof block !== 'object') return;
        if (block.h) html += '<h4 class="theory-prose-h">' + esc(block.h) + '</h4>';
        const paras = Array.isArray(block.p) ? block.p : (typeof block.p === 'string' ? [block.p] : []);
        paras.forEach((p) => {
          if (typeof p === 'string' && p) html += '<p>' + esc(p) + '</p>';
        });
        if (block.figures) html += renderFigures(block.figures, art);
      });
      return html;
    }

    function navButton(id, title, blurb, extraClass) {
      const on = id === articleId;
      return '<button type="button" class="theory-nav-item' + (extraClass ? ' ' + extraClass : '') + (on ? ' on' : '') + '" data-theory-open="' + esc(id) + '"' +
        (on ? ' aria-current="page"' : '') + '>' +
        '<span class="theory-nav-title">' + esc(title) + '</span>' +
        '<span class="theory-nav-blurb">' + esc(blurb) + '</span>' +
        '</button>';
    }

    function renderNav() {
      if (!nav) return;
      nav.setAttribute('aria-label', t('theory.navAria'));
      let lessons = '';
      SECTIONS.forEach((sec) => {
        lessons +=
          '<div class="theory-nav-section" role="group" aria-label="' + esc(t('theory.sections.' + sec.id)) + '">' +
          '<div class="theory-nav-section-label">' + esc(t('theory.sections.' + sec.id)) + '</div>' +
          sec.articles.map((a) =>
            navButton(a.id, t('theory.articles.' + a.id + '.title'), t('theory.articles.' + a.id + '.blurb'))
          ).join('') +
          '</div>';
      });
      const about = navButton(ABOUT_ID, t('theory.about.title'), t('theory.about.blurb'), 'theory-nav-about');
      nav.innerHTML = lessons + about;
    }

    function renderEmpty() {
      if (!body) return;
      body.innerHTML =
        '<div class="theory-empty">' +
        '<p class="theory-intro">' + esc(t('theory.intro')) + '</p>' +
        '</div>';
      applyDemo(null);
    }

    function renderAbout() {
      if (!body) return;
      const md = readmeMarkdown();
      if (!md) {
        body.innerHTML = '<p class="about-status about-status-error">' + esc(t('about.loadError')) + '</p>';
        demoSyncedFor = ABOUT_ID;
        applyDemo(null);
        return;
      }
      const html = window.FretAbout && typeof window.FretAbout.markdownToHtml === 'function'
        ? window.FretAbout.markdownToHtml(md)
        : esc(md);
      body.innerHTML =
        '<article class="theory-article theory-about">' +
        '<h3 class="theory-article-title">' + esc(t('theory.about.title')) + '</h3>' +
        '<p class="theory-about-hint">' + esc(t('about.hint')) + '</p>' +
        '<div class="about-prose">' + html + '</div>' +
        '</article>';
      demoSyncedFor = ABOUT_ID;
      applyDemo(null);
    }

    function renderArticle(id) {
      if (!body) return;
      if (id === ABOUT_ID) {
        renderAbout();
        return;
      }
      const art = articleById(id);
      if (!art) {
        articleId = null;
        renderEmpty();
        return;
      }
      if (demoSyncedFor !== id) syncDemo(true);
      const title = t('theory.articles.' + id + '.title');
      const prose = renderProse(id, art);
      const ctas = (art.ctas || []).map((c, i) => {
        const lesson = c.lesson || {};
        const full = lesson.tab === 'drills';
        const on = i === activeFigCta ? ' on' : '';
        return '<button type="button" class="theory-cta' + (full ? ' theory-cta-full' : '') + on + '" data-theory-cta="' + i + '">' +
          esc(t(c.labelKey)) + '</button>';
      }).join('');
      const related = (art.related || []).filter((rid) => articleById(rid)).map((rid) =>
        '<button type="button" class="theory-related-btn" data-theory-open="' + esc(rid) + '">' +
        esc(t('theory.articles.' + rid + '.title')) +
        '</button>'
      ).join('');
      body.innerHTML =
        '<article class="theory-article">' +
        '<h3 class="theory-article-title">' + esc(title) + '</h3>' +
        '<div class="theory-prose">' + prose + '</div>' +
        (ctas ? '<div class="theory-ctas"><span class="theory-ctas-label">' + esc(t('theory.openOnNeck')) + '</span>' + ctas + '</div>' : '') +
        (related ? '<div class="theory-related"><span class="theory-related-label">' + esc(t('theory.seeAlso')) + '</span>' + related + '</div>' : '') +
        '</article>';
    }

    function renderTheory() {
      if (!panel) return;
      renderNav();
      if (articleId) renderArticle(articleId);
      else renderEmpty();
    }

    function bind() {
      if (bound) return;
      bound = true;
      const onClick = (e) => {
        const openBtn = e.target.closest('[data-theory-open]');
        if (openBtn && panel && panel.contains(openBtn)) {
          articleId = openBtn.getAttribute('data-theory-open');
          demoSyncedFor = null;
          renderTheory();
          onNavigate();
          return;
        }
        const playBtn = e.target.closest('[data-theory-fig-play]');
        if (playBtn && body && body.contains(playBtn)) {
          e.preventDefault();
          e.stopPropagation();
          const midiList = String(playBtn.getAttribute('data-fig-midis') || '')
            .split(',')
            .map(Number)
            .filter((n) => Number.isFinite(n));
          playFigureMidis(midiList, playBtn.getAttribute('data-fig-arpeggio') === '1');
          return;
        }
        const cta = e.target.closest('[data-theory-cta]');
        if (cta && body && body.contains(cta) && articleId && articleId !== ABOUT_ID) {
          const art = articleById(articleId);
          const idx = parseInt(cta.getAttribute('data-theory-cta'), 10);
          if (!art || isNaN(idx) || !art.ctas[idx]) return;
          const lesson = art.ctas[idx].lesson;
          if (lesson && lesson.tab === 'drills') {
            openLesson(lesson);
            return;
          }
          activeFigCta = idx;
          applyDemo(lesson);
          demoSyncedFor = articleId;
          renderArticle(articleId);
          boardWrapScroll();
          return;
        }
        const fig = e.target.closest('[data-theory-fig-cta]');
        if (fig && body && body.contains(fig) && articleId && articleId !== ABOUT_ID) {
          const art = articleById(articleId);
          const idx = parseInt(fig.getAttribute('data-theory-fig-cta'), 10);
          if (!art || isNaN(idx) || !art.ctas || !art.ctas[idx]) return;
          const lesson = art.ctas[idx].lesson;
          if (!lesson || lesson.tab === 'drills') return;
          activeFigCta = idx;
          applyDemo(lesson);
          demoSyncedFor = articleId;
          renderArticle(articleId);
          boardWrapScroll();
        }
      };
      if (panel) panel.addEventListener('click', onClick);
    }

    function boardWrapScroll() {
      const wrap = $('#board-wrap');
      if (!wrap || wrap.classList.contains('hidden')) return false;
      requestAnimationFrame(() => {
        wrap.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      });
      return true;
    }

    return {
      renderTheory: renderTheory,
      bind: bind,
      openArticle: function (id) {
        if (id === ABOUT_ID || articleById(id)) articleId = id;
        else if (ARTICLES[0]) articleId = ARTICLES[0].id;
        demoSyncedFor = null;
        activeFigCta = null;
        renderTheory();
        onNavigate();
      },
      invalidateDemo: function () {
        demoSyncedFor = null;
        activeFigCta = null;
      },
      getArticleId: function () {
        return articleId;
      },
      articles: ARTICLES,
      sections: SECTIONS
    };
  }

  window.FretTheory = {
    create: create,
    ARTICLES: ARTICLES,
    SECTIONS: SECTIONS,
    ABOUT_ID: ABOUT_ID,
    ALT_HOOKS: ALT_HOOKS,
    hookForAlteration: hookForAlteration,
    normalizeAltToken: normalizeAltToken
  };
})();
