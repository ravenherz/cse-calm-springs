(function (root) {
  function chordCase(name, pcs, expectSymbol, extra) {
    extra = extra || {};
    return {
      name: name,
      bucket: extra.bucket || 'lock',
      run: function (M, h) {
        const midis = extra.midis || h.midisFromPcsLowToHigh(pcs);
        const r = M.analyzeChord(midis, extra.forcedRoot != null ? extra.forcedRoot : null, { names: M.SHARP });
        h.eq(!!r && r.matched, true, 'matched');
        const sym = h.chordSymbol(r);
        h.eq(sym, expectSymbol, 'symbol');
        if (extra.slash != null) h.eq(r.slash, extra.slash, 'slash');
        if (extra.bassName) h.eq(r.bassName, extra.bassName, 'bass');
        if (extra.rootPc != null) h.eq(r.rootPc, extra.rootPc, 'rootPc');
        if (extra.labelFor) {
          Object.keys(extra.labelFor).forEach((pc) => {
            h.eq(r.labelFor[pc], extra.labelFor[pc], 'labelFor[' + pc + ']');
          });
        }
      }
    };
  }

  const MAJOR_MODES = [
    [0, 2, 4, 5, 7, 9, 11],
    [0, 2, 3, 5, 7, 9, 10],
    [0, 1, 3, 5, 7, 8, 10],
    [0, 2, 4, 6, 7, 9, 11],
    [0, 2, 4, 5, 7, 9, 10],
    [0, 2, 3, 5, 7, 8, 10],
    [0, 1, 3, 5, 6, 8, 10]
  ];

  const suites = [
    {
      name: 'Music — scales',
      cases: [
        {
          name: 'scaleOrdered major modes 0–6',
          bucket: 'lock',
          run: function (M, h) {
            for (let i = 0; i < 7; i++) h.eq(M.scaleOrdered('major', i), MAJOR_MODES[i], 'mode ' + i);
          }
        },
        {
          name: 'scaleOrdered blues / wholeTone',
          bucket: 'lock',
          run: function (M, h) {
            h.eq(M.scaleOrdered('blues', 0), [0, 3, 5, 6, 7, 10]);
            h.eq(M.scaleOrdered('wholeTone', 0), [0, 2, 4, 6, 8, 10]);
          }
        },
        {
          name: 'fitScaleNames C Phrygian unique letters',
          bucket: 'lock',
          run: function (M, h) {
            const r = M.fitScaleNames(0, M.scaleOrdered('major', 2));
            h.eq(!!r && !!r.names, true, 'has names');
            h.eq(
              [0, 1, 3, 5, 7, 8, 10].map((pc) => r.names[pc]),
              ['C', 'Db', 'Eb', 'F', 'G', 'Ab', 'Bb']
            );
          }
        },
        {
          name: 'fitScaleNames C Lydian uses F# not Gb',
          bucket: 'lock',
          run: function (M, h) {
            const r = M.fitScaleNames(0, M.scaleOrdered('major', 3));
            h.eq(
              [0, 2, 4, 6, 7, 9, 11].map((pc) => r.names[pc]),
              ['C', 'D', 'E', 'F#', 'G', 'A', 'B']
            );
          }
        },
        {
          name: 'fitScaleNames C major pentatonic skips letters',
          bucket: 'lock',
          run: function (M, h) {
            const r = M.fitScaleNames(0, M.scaleOrdered('majorPentatonic', 0));
            h.eq(
              [0, 2, 4, 7, 9].map((pc) => r.names[pc]),
              ['C', 'D', 'E', 'G', 'A']
            );
          }
        },
        {
          name: 'fitScaleNames F# Ionian anchors on F# not Gb',
          bucket: 'known',
          passNote: 'want F# G# A# B C# D# E#; fitScaleNames currently returns names:null',
          run: function (M, h) {
            const r = M.fitScaleNames(6, M.scaleOrdered('major', 0));
            h.eq(!!r && !!r.names, true, 'has names');
            h.eq(
              [6, 8, 10, 0, 1, 3, 5].map((pc) => r.names[pc]),
              ['F#', 'G#', 'A#', 'B', 'C#', 'D#', 'E#']
            );
          }
        },
        {
          name: 'fitScaleNames blues cannot uniquify letters',
          bucket: 'lock',
          run: function (M, h) {
            const r = M.fitScaleNames(0, M.scaleOrdered('blues', 0));
            h.eq(r.names, null);
            h.eq(typeof r.preferFlat, 'boolean');
          }
        },
        {
          name: 'scalePcs major ionian membership',
          bucket: 'lock',
          run: function (M, h) {
            const pcs = M.scalePcs('major', 0);
            h.eq(pcs.has(0) && pcs.has(4) && pcs.has(7) && !pcs.has(1), true);
          }
        },
        {
          name: 'parentScale modal families → relative mode 0',
          bucket: 'lock',
          run: function (M, h) {
            h.eq(M.parentScale('major', 0, 0), null, 'Ionian has no parent');
            h.eq(M.parentScale('major', 1, 2), { rootPc: 0, family: 'major', modeIndex: 0 }, 'D Dorian → C Ionian');
            h.eq(M.parentScale('major', 2, 4), { rootPc: 0, family: 'major', modeIndex: 0 }, 'E Phrygian → C Ionian');
            h.eq(M.parentScale('major', 5, 9), { rootPc: 0, family: 'major', modeIndex: 0 }, 'A Aeolian → C Ionian');
            h.eq(M.parentScale('major', 4, 7), { rootPc: 0, family: 'major', modeIndex: 0 }, 'G Mixolydian → C Ionian');
            h.eq(M.parentScale('melodicMinor', 1, 2), { rootPc: 0, family: 'melodicMinor', modeIndex: 0 }, 'D Dorian b2 → C Melodic Minor');
            h.eq(M.parentScale('harmonicMinor', 4, 7), { rootPc: 0, family: 'harmonicMinor', modeIndex: 0 }, 'G Phrygian Dominant → C Harmonic Minor');
            h.eq(M.parentScale('blues', 0, 0), null, 'non-modal family');
          }
        },
        {
          name: 'scaleChords C Ionian triads',
          bucket: 'lock',
          run: function (M, h) {
            const ch = M.scaleChords(0, M.scaleOrdered('major', 0), 3);
            h.eq(ch.length, 7);
            h.eq(ch.map((c) => c.pcs), [
              [0, 4, 7],
              [2, 5, 9],
              [4, 7, 11],
              [5, 9, 0],
              [7, 11, 2],
              [9, 0, 4],
              [11, 2, 5]
            ]);
            h.eq(ch.map((c) => c.rootPc), [0, 2, 4, 5, 7, 9, 11]);
          }
        },
        {
          name: 'scaleChords D Dorian same relative set',
          bucket: 'lock',
          run: function (M, h) {
            const ch = M.scaleChords(2, M.scaleOrdered('major', 1), 3);
            h.eq(ch.length, 7);
            h.eq(ch[0].pcs, [2, 5, 9], 'i = Dm');
            h.eq(ch[6].pcs, [0, 4, 7], 'VII = C');
          }
        },
        {
          name: 'scaleChords C Ionian sevenths',
          bucket: 'lock',
          run: function (M, h) {
            const ch = M.scaleChords(0, M.scaleOrdered('major', 0), 4);
            h.eq(ch.length, 7);
            h.eq(ch.map((c) => c.pcs), [
              [0, 4, 7, 11],
              [2, 5, 9, 0],
              [4, 7, 11, 2],
              [5, 9, 0, 4],
              [7, 11, 2, 5],
              [9, 0, 4, 7],
              [11, 2, 5, 9]
            ]);
          }
        },
        {
          name: 'scaleChords skips tiny scales',
          bucket: 'lock',
          run: function (M, h) {
            h.eq(M.scaleChords(0, [0, 3, 7], 3).length, 0);
            h.eq(M.scaleChords(0, M.scaleOrdered('majorPentatonic', 0), 4).length, 0);
          }
        },
        {
          name: 'rotateScale mode 0 equals base',
          bucket: 'lock',
          run: function (M, h) {
            Object.keys(M.SCALES).forEach((fk) => {
              const fam = M.SCALES[fk];
              if (!fam.modes) return;
              h.eq(M.rotateScale(fk, 0), fam.base.slice(), fk);
            });
          }
        },
        {
          name: 'SCALE_CATS covers every SCALES key once',
          bucket: 'lock',
          run: function (M, h) {
            const seen = {};
            M.SCALE_CATS.forEach((cat) => {
              cat.families.forEach((fk) => {
                if (seen[fk]) throw new Error('duplicate family ' + fk);
                seen[fk] = true;
                if (!M.SCALES[fk]) throw new Error('missing SCALES.' + fk);
              });
            });
            Object.keys(M.SCALES).forEach((fk) => {
              if (!seen[fk]) throw new Error('unlisted family ' + fk);
            });
          }
        }
      ]
    },
    {
      name: 'Chords — lock',
      cases: [
        chordCase('C major triad', [0, 4, 7], 'C'),
        chordCase('A minor triad', [9, 0, 4], 'Am'),
        chordCase('Csus4', [0, 5, 7], 'Csus4'),
        chordCase('C5 power', [0, 7], 'C5'),
        chordCase('C7', [0, 4, 7, 10], 'C7'),
        chordCase('Cmaj7', [0, 4, 7, 11], 'Cmaj7'),
        chordCase('Cm7', [0, 3, 7, 10], 'Cm7'),
        chordCase('Cm7b5', [0, 3, 6, 10], 'Cm7b5'),
        chordCase('Cdim7', [0, 3, 6, 9], 'Cdim7'),
        chordCase('C6', [0, 4, 7, 9], 'C6'),
        chordCase('C6/9', [0, 4, 7, 9, 2], 'C6/9'),
        chordCase('E–G–C → C/E', [4, 7, 0], 'C/E', { slash: true, bassName: 'E' }),
        chordCase('A–C#–E → A (not slash)', [9, 1, 4], 'A', { slash: false }),
        chordCase('C E G forced root E → Em#5/C', [0, 4, 7], 'Em#5/C', {
          midis: [48, 52, 55],
          forcedRoot: 4,
          slash: true,
          bassName: 'C'
        }),
        chordCase('C7b9', [0, 4, 7, 10, 1], 'C7b9'),
        chordCase('C7#11 (with P5)', [0, 4, 7, 10, 6], 'C7#11'),
        chordCase('Cmaj7#5', [0, 4, 8, 11], 'Cmaj7#5'),
        chordCase('C E F# G → Cadd#11', [0, 4, 6, 7], 'Cadd#11'),
        chordCase('C E F# → Cadd#11 (not Cb5)', [0, 4, 6], 'Cadd#11'),
        chordCase('Cmaj9#11 (no5, not maj9b5)', [0, 2, 4, 6, 11], 'Cmaj9#11(no5)'),
        chordCase('Cmaj7#11 (no5)', [0, 4, 6, 11], 'Cmaj7#11(no5)'),
        chordCase('Cmaj9#11 (with P5)', [0, 2, 4, 6, 7, 11], 'Cmaj9#11'),
        {
          name: 'Ab m + b9 + no5 → Abm(b9,no5)',
          bucket: 'lock',
          run: function (M, h) {
            const r = M.analyzeChord([44, 45, 47], null, { names: M.FLAT });
            h.eq(h.chordSymbol(r), 'Abm(b9,no5)');
            h.eq(r.quality, 'm');
            h.eq(r.extension, '');
            h.eq(r.parens, ['b9', 'no5']);
          }
        },
        {
          name: 'Bbmaj7#5 parts: root / ext / alt',
          bucket: 'lock',
          run: function (M, h) {
            const r = M.analyzeChord([46, 50, 54, 57], null, { names: M.FLAT });
            h.eq(h.chordSymbol(r), 'Bbmaj7#5');
            h.eq(r.quality, '');
            h.eq(r.extension, 'maj7');
            h.eq(r.alterations, ['#5']);
          }
        },
        {
          name: 'Cm7b5 parts: quality m + ext 7 + alt b5',
          bucket: 'lock',
          run: function (M, h) {
            const r = M.analyzeChord(h.midisFromPcsLowToHigh([0, 3, 6, 10]), null, { names: M.SHARP });
            h.eq(h.chordSymbol(r), 'Cm7b5');
            h.eq(r.quality, 'm');
            h.eq(r.extension, '7');
            h.eq(r.alterations, ['b5']);
          }
        },
        {
          name: 'C13 labelFor uses compound 9/13',
          bucket: 'lock',
          run: function (M, h) {
            const midis = h.midisFromPcsLowToHigh([0, 4, 7, 10, 2, 9]);
            const r = M.analyzeChord(midis, null, { names: M.SHARP, compoundIntervals: true });
            h.eq(h.chordSymbol(r), 'C13', 'symbol');
            h.eq(r.labelFor[2], '9', 'labelFor 9');
            h.eq(r.labelFor[9], '13', 'labelFor 13');
          }
        },
        {
          name: 'chordScales Cmaj7 includes Ionian and Lydian',
          bucket: 'lock',
          run: function (M, h) {
            const r = M.analyzeChord(h.midisFromPcsLowToHigh([0, 4, 7, 11]), null, { names: M.SHARP });
            h.eq(!!r && r.matched, true, 'matched');
            const sc = M.chordScales(r.rootPc, r.intervals, {
              names: M.SHARP,
              compoundIntervals: true,
              scaleRoot: r.rootPc
            });
            const keys = sc.full.map((e) => e.family + ':' + e.modeIndex);
            h.eq(keys.indexOf('major:0') >= 0, true, 'C Ionian');
            h.eq(keys.indexOf('major:3') >= 0, true, 'C Lydian');
          }
        },
        {
          name: 'chordScales Dm7 includes Dorian',
          bucket: 'lock',
          run: function (M, h) {
            const r = M.analyzeChord(h.midisFromPcsLowToHigh([2, 5, 9, 0]), 2, { names: M.SHARP });
            h.eq(!!r && r.matched, true, 'matched');
            const sc = M.chordScales(r.rootPc, r.intervals, {
              names: M.SHARP,
              compoundIntervals: true,
              scaleRoot: r.rootPc
            });
            const keys = sc.full.map((e) => e.family + ':' + e.modeIndex);
            h.eq(keys.indexOf('major:1') >= 0, true, 'D Dorian');
          }
        }
      ]
    },
    {
      name: 'Deep links',
      cases: [
        {
          name: 'build/parse C Lydian + CAGED',
          bucket: 'lock',
          run: function (M, h) {
            const DL = window.FretDeepLink;
            h.eq(!!DL, true, 'FretDeepLink loaded');
            const hash = DL.build({ ver: 1, r: 0, f: 'major', m: 3, l: 'caged' });
            h.eq(hash.indexOf('m=3') >= 0, true, 'mode in hash');
            h.eq(hash.indexOf('l=caged') >= 0, true, 'layer in hash');
            const parsed = DL.parseQueryString(hash);
            h.eq(parsed.r, 0, 'root');
            h.eq(parsed.f, 'major', 'family');
            h.eq(parsed.m, 3, 'mode');
            h.eq(parsed.l, 'caged', 'layer');
          }
        },
        {
          name: 'default C major explore is not shareable',
          bucket: 'lock',
          run: function (M, h) {
            const DL = window.FretDeepLink;
            h.eq(DL.hasShareable({ ver: 1, t: 'explore', r: 0, f: 'major', m: 0 }), false, 'default');
            h.eq(DL.build({ ver: 1, t: 'explore', r: 0, f: 'major', m: 0 }), '', 'empty hash');
          }
        },
        {
          name: 'chord pcs round-trip',
          bucket: 'lock',
          run: function (M, h) {
            const DL = window.FretDeepLink;
            const hash = DL.build({ ver: 1, t: 'chords', pcs: [0, 4, 7, 11], cr: 0 });
            const parsed = DL.parseQueryString(hash);
            h.eq(parsed.t, 'chords', 'tab');
            h.eq(parsed.pcs.join(','), '0,4,7,11', 'pcs');
            h.eq(parsed.cr, 0, 'cr');
          }
        }
      ]
    },
    {
      name: 'Chords — known debt',
      cases: [
        {
          name: 'C–F–Bb quartal trichord stays unmatched',
          bucket: 'known',
          passNote: 'want unmatched (quartal stack); engine forces Fsus4/C',
          run: function (M, h) {
            const r = M.analyzeChord(h.midisFromPcsLowToHigh([0, 5, 10]), null, { names: M.SHARP });
            h.eq(!!(r && r.matched), false, 'matched');
          }
        },
        {
          name: 'C7alt (no5) reads as C7alt',
          bucket: 'known',
          passNote: 'want C7alt; unforced root currently prefers C#mMaj13/C',
          run: function (M, h) {
            const r = M.analyzeChord(h.midisFromPcsLowToHigh([0, 4, 10, 1, 6, 8]), null, { names: M.SHARP });
            h.eq(h.chordSymbol(r), 'C7alt');
          }
        }
      ]
    },
    {
      name: 'Geometry',
      cases: [
        {
          name: 'x / bandCenter / fretFromX for 15 frets',
          bucket: 'lock',
          run: function (M, h) {
            const G = window.FretGeom;
            h.eq(!!G, true, 'FretGeom loaded');
            h.eq(G.x(0, 15), 10, 'nut x');
            h.eq(G.x(15, 15), 100, 'last fret x');
            h.eq(G.bandCenter(0, 15), 7, 'open band');
            h.eq(G.bandCenter(1, 15), 13, 'fret 1 band');
            h.eq(G.fretFromX(G.x(3, 15) - 0.01, 15), 3, 'just left of fret 3');
            h.eq(G.fretFromX(99, 15), 15, 'near end');
          }
        },
        {
          name: 'yArea / yPx bass at bottom',
          bucket: 'lock',
          run: function (M, h) {
            const G = window.FretGeom;
            const area = G.yArea(6);
            h.eq(area.n, 6);
            h.eq(area.step, 42);
            h.eq(G.yPx(0, area) > G.yPx(5, area), true, 'bass below treble');
            h.eq(G.yPx(0, area), 277, 'bass y');
          }
        },
        {
          name: 'hexA alpha channel',
          bucket: 'lock',
          run: function (M, h) {
            h.eq(window.FretGeom.hexA('#ff0000', 0.5), 'rgba(255,0,0,0.5)');
          }
        }
      ]
    },
    {
      name: 'CAGED',
      cases: [
        {
          name: 'SHAPE_DEFS CAGED order',
          bucket: 'lock',
          run: function (M, h) {
            const C = window.FretCaged;
            h.eq(!!C, true, 'FretCaged loaded');
            h.eq(C.SHAPE_DEFS.map((s) => s.key), ['C', 'A', 'G', 'E', 'D']);
          }
        },
        {
          name: 'C Ionian standard tuning one box per shape',
          bucket: 'lock',
          run: function (M, h) {
            const C = window.FretCaged;
            const midis = [40, 45, 50, 55, 59, 64];
            const data = C.computeData({
              M: M,
              midis: midis,
              fretCount: 15,
              root: 0,
              scaleFamily: 'major',
              modeIndex: 0,
              showCagedShapes: true,
              shapes: { C: true, A: true, G: true, E: true, D: true },
              cagedRepeats: false,
              fullChart: false,
              highlightDegree: -1,
              showCharOriginals: false,
              tab: 'explore'
            });
            h.eq(data.boxes.map((b) => b.key + ':' + b.lo + '-' + b.hi), [
              'C:0-4', 'A:2-6', 'G:4-8', 'E:7-11', 'D:9-13'
            ]);
            h.eq(data.markers.length > 0, true, 'has markers');
            h.eq(data.pcs.has(0) && data.pcs.has(4) && data.pcs.has(7), true, 'C major pcs');
          }
        }
      ]
    },
    {
      name: 'Quartal',
      cases: [
        {
          name: 'clampHeight / heightOptions / defaultStartString',
          bucket: 'lock',
          run: function (M, h) {
            const Q = window.FretQuartal;
            h.eq(!!Q, true, 'FretQuartal loaded');
            h.eq(Q.clampHeight(2, 6), 3);
            h.eq(Q.clampHeight(9, 6), 6);
            h.eq(Q.heightOptions(6), [3, 4, 5, 6]);
            h.eq(Q.defaultStartString(6, 3), 1);
            h.eq(Q.clampStartString(9, 6, 3), 3);
          }
        },
        {
          name: 'C Ionian height-3 voicings (no repeats)',
          bucket: 'lock',
          run: function (M, h) {
            const Q = window.FretQuartal;
            const midis = [40, 45, 50, 55, 59, 64];
            const r = Q.computeVoicings({
              M: M,
              midis: midis,
              fretCount: 15,
              root: 0,
              scaleFamily: 'major',
              modeIndex: 0,
              height: 3,
              startString: 1,
              repeats: false
            });
            h.eq(r.height, 3);
            h.eq(r.startString, 1);
            h.eq(r.voicings.length, 7);
            h.eq(r.voicings.map((v) => v.bassRel), [9, 11, 0, 2, 4, 5, 7]);
            r.voicings.forEach((v, i) => {
              h.eq(v.notes.length, 3, 'voicing ' + i + ' notes');
              h.eq(v.notes[0].m < v.notes[1].m && v.notes[1].m < v.notes[2].m, true, 'ascending ' + i);
            });
          }
        }
      ]
    },
    {
      name: 'i18n',
      cases: [
        {
          name: 'ru leaf keys are a subset of en',
          bucket: 'lock',
          run: function (M, h) {
            const I18n = window.FretI18n;
            h.eq(!!I18n && typeof I18n.vocab === 'function', true, 'FretI18n.vocab');
            const en = I18n.vocab('en');
            const ru = I18n.vocab('ru');
            h.eq(!!en && !!ru, true, 'both vocabs registered');
            function leafPaths(obj, prefix, out) {
              Object.keys(obj).forEach((k) => {
                const p = prefix ? prefix + '.' + k : k;
                const v = obj[k];
                if (v && typeof v === 'object' && !Array.isArray(v)) leafPaths(v, p, out);
                else out.push(p);
              });
            }
            const enLeaves = [];
            const ruLeaves = [];
            leafPaths(en, '', enLeaves);
            leafPaths(ru, '', ruLeaves);
            h.eq(enLeaves.length > 100, true, 'en has leaves');
            h.eq(ruLeaves.length > 0, true, 'ru has leaves');
            const enSet = {};
            enLeaves.forEach((p) => { enSet[p] = true; });
            const orphans = ruLeaves.filter((p) => !enSet[p]);
            h.eq(orphans, [], 'ru-only keys');
          }
        }
      ]
    }
  ];

  root.TheoryFixtures = { suites: suites };
})(typeof self !== 'undefined' ? self : this);
