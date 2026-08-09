(function (root) {
  const M = root.Music;
  if (!M) throw new Error('Music required');

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
    else if (h[7]) c.fifth = 7;
    else if (h[8]) c.fifth = 8;
    else if (h[6] && c.third === 4 && !c.seventh && !c.sixth) c.fifth = 0;
    else if (h[6]) c.fifth = 6;
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
      c.ext.sh11 = h[6] && (c.fifth === 7 || c.fifth === 0);
    }
    c.ok = false;
    if (shell) c.ok = true;
    else if (c.seventh === 10 || c.seventh === 11) c.ok = c.third !== 0 || c.sus !== '' || c.fifth !== 0;
    else if (c.third !== 0) c.ok = true;
    else c.ok = c.sus !== '' || c.fifth === 7;
    return c;
  }

  function t(path, vars) {
    return (root.FretI18n && root.FretI18n.t(path, vars)) || path;
  }

  function describeChord(c) {
    const num = c.ext.n13 ? '13' : (c.ext.n11 ? '11' : (c.ext.n9 ? '9' : '7'));
    const numWord = c.ext.n13 ? '13th' : (c.ext.n11 ? '11th' : (c.ext.n9 ? '9th' : '7th'));
    let quality = '';
    let extension = '';
    const alterations = [];
    const parens = [];
    let name = '';
    if (c.dim7) {
      extension = 'dim7';
      name = 'diminished 7th';
    } else if (c.dim) {
      extension = 'dim';
      name = 'diminished';
    } else if (c.aug) {
      extension = 'aug';
      name = 'augmented';
    } else if (c.sixth) {
      if (c.third === 3) quality = 'm';
      extension = '6' + (c.ext.n9 ? '/9' : '');
      name = (quality ? 'minor' : 'major') + ' 6th' + (c.ext.n9 ? ' with 9th' : '');
      if (c.ext.n11) parens.push('add11');
      if (c.ext.b9) parens.push('b9');
      if (c.ext.sh11) parens.push('#11');
      if (c.ext.b13) parens.push('b13');
    } else if (c.seventh === 11) {
      if (c.third === 0) {
        extension = 'maj' + num;
        parens.push('no3');
        name = 'major ' + numWord + ' (no 3rd)';
      } else {
        if (c.third === 3) quality = 'm';
        extension = (quality ? 'Maj' : 'maj') + num;
        name = (quality ? 'minor-major ' : 'major ') + numWord;
      }
    } else if (c.seventh === 10) {
      if (c.sus) {
        extension = num + 'sus' + c.sus;
        name = 'suspended dominant ' + numWord;
      } else if (c.third === 0) {
        extension = num;
        parens.push('no3');
        name = numWord + ' (no 3rd)';
      } else {
        if (c.third === 3) quality = 'm';
        extension = num;
        name = (quality ? 'minor ' : 'dominant ') + numWord;
      }
    } else if (c.third !== 0) {
      if (c.third === 3) quality = 'm';
      name = quality ? 'minor' : 'major';
      if (c.ext.n9 && c.ext.n11) {
        extension = 'add11';
        name += ' added 11th';
      } else if (c.ext.n11) {
        extension = 'add4';
        name += ' added 4th';
      } else if (c.ext.n9) {
        extension = 'add9';
        name += ' added 9th';
      } else if (c.ext.sh11) {
        extension = 'add#11';
        name += ' added sharp 11th';
      }
      if (c.ext.b9) parens.push('b9');
      if (c.ext.sh9) parens.push('#9');
      if (c.ext.sh11 && (c.ext.n9 || c.ext.n11)) parens.push('#11');
      if (c.fifth === 6) { alterations.push('b5'); name += ' flat 5'; }
      else if (c.fifth === 8) { alterations.push('#5'); name += ' sharp 5'; }
    } else if (c.sus) {
      extension = 'sus' + c.sus;
      name = 'suspended ' + (c.sus === '4' ? '4th' : '2nd');
    } else {
      extension = '5';
      name = 'power chord';
    }
    if (c.seventh === 10 || c.seventh === 11) {
      if (c.fifth === 6) { alterations.push('b5'); name += ' flat 5'; }
      else if (c.fifth === 8) { alterations.push('#5'); name += ' sharp 5'; }
      if (c.ext.b9) { alterations.push('b9'); name += ' flat 9'; }
      if (c.ext.sh9) { alterations.push('#9'); name += ' sharp 9'; }
      if (c.ext.sh11) { alterations.push('#11'); name += ' sharp 11'; }
      if (c.ext.b13) { alterations.push('b13'); name += ' flat 13'; }
    }
    if (c.sixth || c.seventh === 10 || c.seventh === 11) {
      if (c.fifth === 0) parens.push('no5');
    } else if (c.third !== 0 && !c.dim && !c.aug && c.fifth === 0 && !c.ext.sh11) {
      parens.push('no5');
    }
    const parenStr = parens.length ? '(' + parens.join(',') + ')' : '';
    const suffix = quality + extension + alterations.join('') + parenStr;
    return {
      quality: quality,
      extension: extension,
      alterations: alterations,
      parens: parens,
      suffix: suffix,
      name: name
    };
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

  function roleLabel(iv, c, compoundIntervals) {
    if (iv === 0) return 'R';
    if (c.dim7 && iv === 9) return 'bb7';
    if (iv === 2) return (compoundIntervals && c.ext.n9) ? '9' : '2';
    if (iv === 5) return (compoundIntervals && c.ext.n11 && (c.seventh || c.sixth)) ? '11' : '4';
    if (iv === 9) return (compoundIntervals && c.ext.n13 && c.seventh) ? '13' : '6';
    if (iv === 1) return (compoundIntervals && c.ext.b9) ? 'b9' : 'b2';
    if (iv === 3) return (compoundIntervals && c.ext.sh9) ? '#9' : 'b3';
    if (iv === 6) {
      if (c.fifth === 6) return 'b5';
      if (c.ext.sh11) return compoundIntervals ? '#11' : '#4';
      return 'b5';
    }
    if (iv === 8) return c.fifth === 8 ? '#5' : ((compoundIntervals && c.ext.b13) ? 'b13' : 'b6');
    return M.DEGREES[iv];
  }

  function chordRoleText(iv, c, compoundIntervals) {
    return c ? roleLabel(iv, c, compoundIntervals) : M.DEGREES[iv];
  }

  function chordIntervalText(iv, c, compoundIntervals) {
    if (!compoundIntervals) return M.QUALITIES[iv];
    const role = chordRoleText(iv, c, compoundIntervals);
    if (role === '9' || role === 'b9' || role === '#9') return role === 'b9' ? 'm9' : (role === '#9' ? 'A9' : 'M9');
    if (role === '11' || role === '#11') return role === '#11' ? 'A11' : 'P11';
    if (role === '13' || role === 'b13') return role === 'b13' ? 'm13' : 'M13';
    return M.QUALITIES[iv];
  }

  function chordIntervalFull(iv, c, compoundIntervals) {
    if (!compoundIntervals) return M.qualityFull(iv);
    const role = chordRoleText(iv, c, compoundIntervals);
    const keyed = t('tip.intervalFull.' + role);
    if (keyed && keyed !== 'tip.intervalFull.' + role) return keyed;
    return M.qualityFull(iv);
  }

  function analyzeChord(midis, forcedRoot, opts) {
    if (!midis.length) return null;
    const names = opts.names;
    const compound = !!opts.compoundIntervals;
    const pcs = Array.from(new Set(midis.map((m) => ((m % 12) + 12) % 12))).sort((a, b) => a - b);
    const bassPc = Math.min.apply(null, midis) % 12;
    const readings = [];
    pcs.forEach((r) => {
      const I = pcs.map((p) => (p - r + 12) % 12).sort((a, b) => a - b);
      const c = interpretChord(I);
      if (!c.ok) return;
      readings.push({ root: r, I: I, c: c, score: chordScore(c) });
    });
    const mkNotes = (rootPc) => pcs.map((pc) => ({ pc: pc, name: names[pc] }));
    const labelFor = (rootPc, c) => {
      const lf = {};
      pcs.forEach((pc) => {
        const iv = (pc - rootPc + 12) % 12;
        lf[pc] = c ? roleLabel(iv, c, compound) : M.DEGREES[iv];
      });
      return lf;
    };
    const pack = (rootPc, desc, c, intervals, slash, alternatives) => ({
      matched: !!c,
      rootPc: rootPc,
      rootName: names[rootPc],
      quality: desc.quality || '',
      extension: desc.extension || '',
      alterations: desc.alterations || [],
      parens: desc.parens || [],
      suffix: desc.suffix || '',
      name: desc.name,
      intervals: intervals,
      exact: !!c,
      bassPc: bassPc,
      bassName: names[bassPc],
      inversion: false,
      inversionName: '',
      slash: slash,
      notes: mkNotes(rootPc),
      labelFor: labelFor(rootPc, c),
      c: c,
      alternatives: alternatives
    });
    const unmatched = (rootPc) => pack(rootPc, {
      quality: '', extension: '', alterations: [], parens: [], suffix: '', name: t('chords.noStandardChord')
    }, null, [], rootPc !== bassPc, []);
    if (forcedRoot != null) {
      const forced = readings.find((r) => r.root === forcedRoot) || null;
      if (!forced) return unmatched(forcedRoot);
      const desc = describeChord(forced.c);
      const alternatives = readings.filter((r) => r !== forced).slice(0, 2).map((r) => ({ rootName: names[r.root], suffix: describeChord(r.c).suffix }));
      return pack(forced.root, desc, forced.c, forced.I, forced.root !== bassPc, alternatives);
    }
    readings.sort((a, b) => b.score - a.score || ((a.root === bassPc ? -1 : (b.root === bassPc ? 1 : a.root - b.root))));
    const best = readings[0] || null;
    const bassReading = readings.find((r) => r.root === bassPc) || null;
    if (!best) {
      return unmatched(bassPc);
    }
    const exoticBass = bassReading && (bassReading.c.dim || bassReading.c.dim7 || bassReading.c.aug || bassReading.c.fifth === 6 || bassReading.c.fifth === 8);
    let primary = best;
    if (bassReading && (best.root === bassPc || !exoticBass)) {
      primary = bassReading;
    }
    const desc = describeChord(primary.c);
    const alternatives = readings.filter((r) => r !== primary).slice(0, 2).map((r) => ({ rootName: names[r.root], suffix: describeChord(r.c).suffix }));
    return pack(primary.root, desc, primary.c, primary.I, primary.root !== bassPc, alternatives);
  }

  function allScaleKeys() {
    const out = [];
    M.SCALE_CATS.forEach((cat) => cat.families.forEach((fk) => out.push(fk)));
    return out;
  }

  function chordScales(rootPc, intervals, opts) {
    const scaleRoot = opts.scaleRoot != null ? opts.scaleRoot : rootPc;
    const toScale = (iv) => (rootPc + iv - scaleRoot + 12) % 12;
    const c = interpretChord(intervals);
    const shell = essentialIntervals(c);
    const full = [];
    const partial = [];
    const seen = {};
    const inScale = (pcs, iv) => pcs.has(toScale(iv));
    const missing = (pcs) => {
      const ivs = intervals.filter((iv) => !shell.has(iv) && !inScale(pcs, iv));
      return ivs.length ? t('chords.missingPrefix') + ivs.map((iv) => roleLabel(iv, c, opts.compoundIntervals)).join(', ') : '';
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
        const entry = { name: nm, note: '', pcs: pcs, family: fk, modeIndex: m };
        if (intervals.every((iv) => inScale(pcs, iv))) {
          full.push(entry);
        } else if (Array.from(shell).every((iv) => inScale(pcs, iv))) {
          entry.note = missing(pcs);
          partial.push(entry);
        }
      }
    });
    return { full: full, partial: partial, rootName: opts.names[scaleRoot], scaleRoot: scaleRoot };
  }

  M.interpretChord = interpretChord;
  M.describeChord = describeChord;
  M.chordScore = chordScore;
  M.essentialIntervals = essentialIntervals;
  M.roleLabel = roleLabel;
  M.chordRoleText = chordRoleText;
  M.chordIntervalText = chordIntervalText;
  M.chordIntervalFull = chordIntervalFull;
  M.analyzeChord = analyzeChord;
  M.chordScales = chordScales;
})(typeof self !== 'undefined' ? self : this);
