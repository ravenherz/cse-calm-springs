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
          name: 'scalePcs major ionian membership',
          bucket: 'lock',
          run: function (M, h) {
            const pcs = M.scalePcs('major', 0);
            h.eq(pcs.has(0) && pcs.has(4) && pcs.has(7) && !pcs.has(1), true);
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
        }
      ]
    }
  ];

  root.TheoryFixtures = { suites: suites };
})(typeof self !== 'undefined' ? self : this);
