(function () {
  const TYPES = [
    { id: 'findDegree', titleKey: 'drills.types.findDegree.title', blurbKey: 'drills.types.findDegree.blurb' },
    { id: 'buildCadence', titleKey: 'drills.types.buildCadence.title', blurbKey: 'drills.types.buildCadence.blurb' },
    { id: 'nameChord', titleKey: 'drills.types.nameChord.title', blurbKey: 'drills.types.nameChord.blurb' }
  ];

  const FORMULA = ['1', '♭2', '2', '♭3', '3', '4', '♯4', '5', '♭6', '6', '♭7', '7'];

  const CADENCE = [
    { roman: 'ii', pcs: [2, 5, 9, 0], rootPc: 2 },
    { roman: 'V', pcs: [7, 11, 2, 5], rootPc: 7 },
    { roman: 'I', pcs: [0, 4, 7, 11], rootPc: 0 }
  ];

  const NAME_POOL = [
    // maj7 / m7 / 7 family (shared tones → alternate roots)
    { pcs: [0, 4, 7, 11], rootPc: 0 },   // Cmaj7
    { pcs: [2, 5, 9, 0], rootPc: 2 },     // Dm7
    { pcs: [4, 7, 11, 2], rootPc: 4 },    // Em7
    { pcs: [5, 9, 0, 4], rootPc: 5 },     // Fmaj7
    { pcs: [7, 11, 2, 5], rootPc: 7 },    // G7
    { pcs: [9, 0, 4, 7], rootPc: 9 },     // Am7
    { pcs: [11, 2, 5, 9], rootPc: 11 },   // Bm7b5
    // triads + 6 / sus
    { pcs: [0, 4, 7], rootPc: 0 },        // C
    { pcs: [2, 5, 9], rootPc: 2 },        // Dm
    { pcs: [4, 7, 11], rootPc: 4 },       // Em
    { pcs: [5, 9, 0], rootPc: 5 },        // F
    { pcs: [7, 11, 2], rootPc: 7 },       // G
    { pcs: [9, 0, 4], rootPc: 9 },        // Am
    { pcs: [0, 4, 7, 9], rootPc: 0 },     // C6
    { pcs: [0, 5, 7], rootPc: 0 },        // Csus4
    { pcs: [7, 0, 2, 5], rootPc: 7 },     // G7sus
    // color / tension
    { pcs: [0, 4, 7, 10], rootPc: 0 },    // C7
    { pcs: [5, 9, 0, 2], rootPc: 5 },     // Fmaj9 shell
    { pcs: [0, 3, 7, 10], rootPc: 0 },    // Cm7
    { pcs: [2, 5, 8, 0], rootPc: 2 },     // Dm7b5
    { pcs: [4, 7, 10, 1], rootPc: 4 },    // Em7b5
    { pcs: [7, 10, 2, 5], rootPc: 7 },    // Gm7
    { pcs: [0, 3, 6, 9], rootPc: 0 },     // Cdim7
    { pcs: [0, 4, 8], rootPc: 0 },        // Caug
    { pcs: [5, 9, 0, 3], rootPc: 5 },     // F7
    { pcs: [10, 2, 5, 8], rootPc: 10 },   // Bb7
    { pcs: [1, 5, 8, 0], rootPc: 1 },     // Dbmaj7
    { pcs: [6, 10, 1, 4], rootPc: 6 },    // F#m7b5
    { pcs: [8, 0, 3, 6], rootPc: 8 },     // Ab7
    { pcs: [3, 7, 10, 2], rootPc: 3 }     // Ebmaj7
  ];

  function chordQualityOf(sym) {
    return String(sym || '').replace(/^[A-G][#b♯♭]?/, '');
  }

  const DISTRACTOR_SUFFIXES = ['', 'm', '7', 'maj7', 'm7', 'm7b5', 'dim', 'aug', '6', 'm6', 'sus4', '9', 'm9'];

  function randInt(n) {
    return Math.floor(Math.random() * n);
  }

  function shuffle(arr) {
    const a = arr.slice();
    for (let i = a.length - 1; i > 0; i--) {
      const j = randInt(i + 1);
      const t = a[i];
      a[i] = a[j];
      a[j] = t;
    }
    return a;
  }

  function rotatePcs(pcs, k) {
    const n = pcs.length;
    if (!n) return [];
    const i = ((k % n) + n) % n;
    return pcs.slice(i).concat(pcs.slice(0, i));
  }

  function symbolFromAnalysis(r) {
    if (!r || !r.matched) return '';
    return (r.rootName || '') + (r.suffix || '');
  }

  function create(deps) {
    const $ = deps.$;
    const t = deps.t;
    const M = deps.M;
    const state = deps.state;
    const ui = deps.ui;
    const names = deps.names;
    const esc = deps.esc;
    const tuningMidis = deps.tuningMidis;
    const placeChordPcs = deps.placeChordPcs;
    const midisFromPcsAscending = deps.midisFromPcsAscending;
    const playMidi = deps.playMidi;
    const playSimultaneous = deps.playSimultaneous;
    const render = deps.render;
    const applyTabUI = deps.applyTabUI;
    const saveState = deps.saveState;
    const setExploreRoot = deps.setExploreRoot;
    const populateSelects = deps.populateSelects;
    const degreeRoman = deps.degreeRoman;
    const peekDrillsDisplay = deps.peekDrillsDisplay || function () {};
    const relockDrillsDisplay = deps.relockDrillsDisplay || function () {};

    const panel = $('#drills-panel');
    const bar = $('#drill-bar');

    let round = null;
    let score = { correct: 0, attempted: 0, streak: 0 };
    let feedback = null;
    let bound = false;

    function active() {
      return !!(round && state.tab === 'drills');
    }

    function mode() {
      if (!active() || !round) return null;
      if (round.type === 'findDegree') return 'find';
      if (round.type === 'buildCadence') return 'build';
      if (round.type === 'nameChord') return 'name';
      return null;
    }

    function clearChordBoard() {
      state.chordNotes = [];
      state.pinRoot = null;
      ui.chordResult = null;
      ui.scaleSel = null;
    }

    function end(opts) {
      round = null;
      feedback = null;
      clearChordBoard();
      if (opts && opts.silent) return;
      if (state.tab === 'drills') {
        renderDrills();
        render();
      }
    }

    function markResult(ok) {
      score.attempted += 1;
      if (ok) {
        score.correct += 1;
        score.streak += 1;
        feedback = { ok: true, text: t('drills.correct') };
      } else {
        score.streak = 0;
        feedback = { ok: false, text: t('drills.tryAgain') };
      }
    }

    function beginRound(setup) {
      relockDrillsDisplay();
      setup();
    }

    function nextFindDegree() {
      beginRound(() => {
      clearChordBoard();
      const root = randInt(12);
      const modeIndex = randInt(7);
      setExploreRoot(root);
      state.scaleFamily = 'major';
      state.modeIndex = modeIndex;
      state.scaleCategory = 'church';
      state.showCagedShapes = false;
      state.showQuartal = false;
      state.showNps = false;
      state.showBerklee = false;
      populateSelects();
      const ordered = M.scaleOrdered('major', modeIndex);
      const deg = ordered[randInt(ordered.length)];
      const targetPc = (root + deg) % 12;
      round = {
        type: 'findDegree',
        rootPc: root,
        family: 'major',
        modeIndex: modeIndex,
        degreeRel: deg,
        targetPc: targetPc,
        answered: false,
        revealed: false
      };
      feedback = null;
      });
    }

    function cadencePrompt(step) {
      const nm = names();
      const ch = CADENCE[step];
      const midis = midisFromPcsAscending(ch.pcs);
      const r = M.analyzeChord(midis, ch.rootPc, { names: nm, compoundIntervals: true });
      const sym = symbolFromAnalysis(r) || (nm[ch.rootPc] + '7');
      return {
        step: step,
        pcs: ch.pcs.slice(),
        rootPc: ch.rootPc,
        roman: ch.roman,
        symbol: sym
      };
    }

    function nextBuildCadence() {
      beginRound(() => {
      clearChordBoard();
      setExploreRoot(0);
      state.scaleFamily = 'major';
      state.modeIndex = 0;
      state.scaleCategory = 'church';
      populateSelects();
      const step0 = cadencePrompt(0);
      round = {
        type: 'buildCadence',
        step: 0,
        steps: CADENCE.length,
        current: step0,
        answered: false,
        revealed: false
      };
      feedback = null;
      });
    }

    function advanceCadenceStep() {
      const next = round.step + 1;
      if (next >= round.steps) {
        round.answered = true;
        feedback = { ok: true, text: t('drills.cadenceDone') };
        return;
      }
      clearChordBoard();
      relockDrillsDisplay();
      round.step = next;
      round.current = cadencePrompt(next);
      round.answered = false;
      round.revealed = false;
      feedback = null;
    }

    function nextNameChord(attempt) {
      const tries = (attempt || 0) + 1;
      if (tries > 12) return;
      relockDrillsDisplay();
      clearChordBoard();
      const pick = NAME_POOL[randInt(NAME_POOL.length)];
      const midis = tuningMidis();
      // Rotate pcs so the intended root is often NOT the lowest placed tone.
      const rotated = rotatePcs(pick.pcs, randInt(pick.pcs.length));
      // Compact shapes only: fretted notes within 5 fret positions (opens ignored).
      const voicing = placeChordPcs(rotated, midis, state.fretCount, null, { maxFrettedSpan: 4 });
      if (!voicing || !voicing.length) {
        nextNameChord(tries);
        return;
      }
      const analysisMidis = voicing.map((nt) => midis[nt.s] + nt.f);
      const correctR = M.analyzeChord(analysisMidis, pick.rootPc, { names: names(), compoundIntervals: true });
      const correct = symbolFromAnalysis(correctR);
      if (!correct) {
        nextNameChord(tries);
        return;
      }

      state.chordNotes = voicing.map((nt) => ({ s: nt.s, f: nt.f, id: ++ui.noteSeq }));
      // No pin / no live analysis highlight — otherwise one root letter kills the quiz.
      state.pinRoot = null;
      ui.chordResult = null;

      const choices = buildNameChoices(analysisMidis, pick.rootPc, correct);
      round = {
        type: 'nameChord',
        pcs: pick.pcs.slice(),
        rootPc: pick.rootPc,
        correct: correct,
        choices: choices,
        answered: false,
        locked: true,
        revealed: false
      };
      feedback = null;
    }

    /** Prefer other valid readings of the same tones, then near-miss symbols. */
    function buildNameChoices(analysisMidis, intendedRoot, correct) {
      const nm = names();
      const used = {};
      used[correct] = true;
      const sameSet = [];
      const pcs = Array.from(new Set(analysisMidis.map((m) => ((m % 12) + 12) % 12)));
      pcs.forEach((r) => {
        if (r === intendedRoot) return;
        const reading = M.analyzeChord(analysisMidis, r, { names: nm, compoundIntervals: true });
        const sym = symbolFromAnalysis(reading);
        if (!sym || used[sym]) return;
        used[sym] = true;
        sameSet.push(sym);
      });

      const near = [];
      const correctQ = chordQualityOf(correct);
      const pool = shuffle(NAME_POOL);
      for (let i = 0; i < pool.length && near.length < 6; i++) {
        const d = pool[i];
        const dr = M.analyzeChord(midisFromPcsAscending(d.pcs), d.rootPc, { names: nm, compoundIntervals: true });
        const sym = symbolFromAnalysis(dr);
        if (!sym || used[sym]) continue;
        used[sym] = true;
        if (chordQualityOf(sym) === correctQ) near.unshift(sym);
        else near.push(sym);
      }

      const fake = [];
      const rootLetters = shuffle([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]);
      for (let i = 0; i < rootLetters.length && fake.length < 4; i++) {
        const sym = nm[rootLetters[i]] + DISTRACTOR_SUFFIXES[randInt(DISTRACTOR_SUFFIXES.length)];
        if (used[sym]) continue;
        used[sym] = true;
        fake.push(sym);
      }

      const distractors = shuffle(sameSet).concat(near).concat(fake);
      return shuffle([correct].concat(distractors.slice(0, 3)));
    }

    function start(typeId) {
      if (TYPES.every((x) => x.id !== typeId)) return;
      state.tab = 'drills';
      applyTabUI();
      if (typeId === 'findDegree') nextFindDegree();
      else if (typeId === 'buildCadence') nextBuildCadence();
      else nextNameChord();
      saveState();
      render();
    }

    function placedPcs() {
      const midis = tuningMidis();
      return state.chordNotes.map((nt) => (((midis[nt.s] + nt.f) % 12) + 12) % 12);
    }

    function covers(need) {
      const have = {};
      placedPcs().forEach((p) => { have[p] = true; });
      return need.every((p) => have[p]);
    }

    function onNeckClick(stringIdx, fret, opts) {
      if (!active() || !round || round.answered) return false;
      const midis = tuningMidis();
      const midi = midis[stringIdx] + fret;
      const pc = ((midi % 12) + 12) % 12;

      if (round.type === 'findDegree') {
        const ok = pc === round.targetPc;
        markResult(ok);
        if (ok) {
          round.answered = true;
          playMidi(midi);
        }
        renderDrills();
        return true;
      }

      if (round.type === 'nameChord') {
        return true;
      }

      if (round.type === 'buildCadence') {
        if (opts && opts.remove) {
          const i = state.chordNotes.findIndex((nt) => nt.s === stringIdx && nt.f === fret);
          if (i >= 0) state.chordNotes.splice(i, 1);
          if (state.pinRoot && state.pinRoot.s === stringIdx && state.pinRoot.f === fret) state.pinRoot = null;
          feedback = null;
          render();
          return true;
        }
        const i = state.chordNotes.findIndex((nt) => nt.s === stringIdx);
        if (i >= 0) state.chordNotes[i] = { s: stringIdx, f: fret, id: state.chordNotes[i].id };
        else state.chordNotes.push({ s: stringIdx, f: fret, id: ++ui.noteSeq });
        if (!state.pinRoot && state.chordNotes.length) {
          state.pinRoot = { s: state.chordNotes[0].s, f: state.chordNotes[0].f };
        }
        feedback = null;
        render();
        return true;
      }
      return false;
    }

    function checkBuild() {
      if (!round || round.type !== 'buildCadence' || round.answered) return;
      const ok = covers(round.current.pcs);
      markResult(ok);
      if (ok) {
        const midis = tuningMidis();
        playSimultaneous(state.chordNotes.map((nt) => midis[nt.s] + nt.f));
        advanceCadenceStep();
      }
      renderDrills();
      render();
    }

    function answerName(sym) {
      if (!round || round.type !== 'nameChord' || round.answered) return;
      const ok = sym === round.correct;
      markResult(ok);
      if (ok) {
        round.answered = true;
        round.locked = false;
        const midis = tuningMidis();
        const analysisMidis = state.chordNotes.map((nt) => midis[nt.s] + nt.f);
        ui.chordResult = M.analyzeChord(analysisMidis, round.rootPc, {
          names: names(),
          compoundIntervals: true
        });
        // Reveal intended root on the lowest matching fretted tone.
        const rootNote = state.chordNotes.find((nt) => {
          const pc = (((midis[nt.s] + nt.f) % 12) + 12) % 12;
          return pc === round.rootPc;
        });
        if (rootNote) state.pinRoot = { s: rootNote.s, f: rootNote.f };
        playSimultaneous(state.chordNotes.map((nt) => midis[nt.s] + nt.f));
      }
      renderDrills();
      render();
    }

    function skipOrNext() {
      if (!round) return;
      if (round.type === 'findDegree') {
        nextFindDegree();
      } else if (round.type === 'buildCadence') {
        if (round.answered || round.step >= round.steps - 1 && feedback && feedback.ok) {
          nextBuildCadence();
        } else {
          advanceCadenceStep();
        }
      } else if (round.type === 'nameChord') {
        nextNameChord();
      }
      render();
    }

    function revealAnswer() {
      if (!round) return;
      round.revealed = true;
      peekDrillsDisplay();

      let text = '';
      if (round.type === 'findDegree') {
        const degLab = FORMULA[round.degreeRel] || degreeRoman(round.degreeRel);
        text = t('drills.answerFind', {
          note: names()[round.targetPc],
          degree: degLab
        });
      } else if (round.type === 'buildCadence') {
        const cur = round.current;
        const notes = cur.pcs.map((p) => names()[p]).join(' · ');
        text = t('drills.answerBuild', {
          chord: cur.symbol,
          notes: notes
        });
      } else if (round.type === 'nameChord') {
        text = t('drills.answerName', { chord: round.correct || '—' });
        round.locked = false;
        const midis = tuningMidis();
        if (state.chordNotes.length) {
          const analysisMidis = state.chordNotes.map((nt) => midis[nt.s] + nt.f);
          ui.chordResult = M.analyzeChord(analysisMidis, round.rootPc, {
            names: names(),
            compoundIntervals: true
          });
          const rootNote = state.chordNotes.find((nt) => {
            const pc = (((midis[nt.s] + nt.f) % 12) + 12) % 12;
            return pc === round.rootPc;
          });
          if (rootNote) state.pinRoot = { s: rootNote.s, f: rootNote.f };
        }
      }
      feedback = { ok: null, reveal: true, text: text };
      renderDrills();
      render();
    }

    function promptHtml() {
      if (!round) return '';
      if (round.type === 'findDegree') {
        const degLab = FORMULA[round.degreeRel] || degreeRoman(round.degreeRel);
        return t('drills.promptFind', { degree: degLab, root: names()[round.rootPc] });
      }
      if (round.type === 'buildCadence') {
        const cur = round.current;
        return t('drills.promptBuild', {
          roman: cur.roman,
          chord: cur.symbol,
          step: String(round.step + 1),
          total: String(round.steps)
        });
      }
      if (round.type === 'nameChord') {
        return t('drills.promptName');
      }
      return '';
    }

    function renderPicker() {
      if (!panel) return;
      const cards = TYPES.map((tp) =>
        '<button type="button" class="drill-type-card" data-drill-start="' + esc(tp.id) + '">' +
        '<span class="drill-type-title">' + esc(t(tp.titleKey)) + '</span>' +
        '<span class="drill-type-blurb">' + esc(t(tp.blurbKey)) + '</span>' +
        '</button>'
      ).join('');
      panel.innerHTML =
        '<div class="drills-head">' +
        '<h2>' + esc(t('drills.title')) + '</h2>' +
        '<p class="drills-hint">' + esc(t('drills.hint')) + '</p>' +
        '</div>' +
        '<div class="drill-type-grid">' + cards + '</div>';
      panel.classList.remove('hidden');
    }

    function renderBar() {
      if (!bar || !round) return;
      let fbClass = '';
      if (feedback) {
        if (feedback.reveal) fbClass = ' reveal';
        else fbClass = feedback.ok ? ' ok' : ' bad';
      }
      const fb = feedback
        ? '<span class="drill-feedback' + fbClass + '">' + esc(feedback.text) + '</span>'
        : '';
      const scoreHtml = '<span class="drill-score">' +
        esc(t('drills.score', { correct: String(score.correct), attempted: String(score.attempted), streak: String(score.streak) })) +
        '</span>';

      let actions = '';
      if (round.type === 'buildCadence' && !round.answered) {
        actions += '<button type="button" class="drill-btn primary" data-drill-check="1">' + esc(t('drills.check')) + '</button>';
      }
      if (round.type === 'nameChord' && !round.answered) {
        actions += '<div class="drill-choices">' + round.choices.map((c) =>
          '<button type="button" class="drill-choice" data-drill-choice="' + esc(c) + '">' + esc(c) + '</button>'
        ).join('') + '</div>';
      }
      actions += '<button type="button" class="drill-btn ghost" data-drill-reveal="1">' +
        esc(t('drills.showAnswer')) + '</button>';
      actions += '<button type="button" class="drill-btn" data-drill-next="1">' +
        esc(round.answered ? t('drills.next') : t('drills.skip')) + '</button>';
      actions += '<button type="button" class="drill-btn ghost" data-drill-exit="1">' + esc(t('drills.exit')) + '</button>';

      bar.innerHTML =
        '<div class="drill-bar-main">' +
        '<div class="drill-prompt">' + esc(promptHtml()) + '</div>' +
        fb +
        scoreHtml +
        '</div>' +
        '<div class="drill-bar-actions">' + actions + '</div>';
      bar.classList.remove('hidden');
    }

    function renderDrills() {
      if (state.tab !== 'drills') {
        if (panel) panel.classList.add('hidden');
        if (bar) bar.classList.add('hidden');
        return;
      }
      if (!round) {
        if (bar) {
          bar.classList.add('hidden');
          bar.innerHTML = '';
        }
        renderPicker();
        return;
      }
      if (panel) panel.classList.add('hidden');
      renderBar();
    }

    function bind() {
      if (bound) return;
      bound = true;
      document.addEventListener('click', (e) => {
        const start = e.target.closest && e.target.closest('[data-drill-start]');
        if (start) {
          e.preventDefault();
          startDrill(start.getAttribute('data-drill-start'));
          return;
        }
        if (!bar || !bar.contains(e.target)) return;
        if (e.target.closest('[data-drill-exit]')) {
          e.preventDefault();
          end();
          return;
        }
        if (e.target.closest('[data-drill-check]')) {
          e.preventDefault();
          checkBuild();
          return;
        }
        if (e.target.closest('[data-drill-reveal]')) {
          e.preventDefault();
          revealAnswer();
          return;
        }
        if (e.target.closest('[data-drill-next]')) {
          e.preventDefault();
          skipOrNext();
          return;
        }
        const choice = e.target.closest('[data-drill-choice]');
        if (choice) {
          e.preventDefault();
          answerName(choice.getAttribute('data-drill-choice'));
        }
      });
    }

    function startDrill(id) {
      start(id);
    }

    return {
      renderDrills: renderDrills,
      bind: bind,
      start: start,
      end: end,
      active: active,
      mode: mode,
      onNeckClick: onNeckClick,
      notesLocked: function () {
        return !!(round && round.type === 'nameChord' && round.locked && !round.answered && !round.revealed);
      },
      answerRevealed: function () {
        return !!(round && round.revealed);
      },
      types: TYPES
    };
  }

  window.FretDrills = {
    create: create,
    TYPES: TYPES
  };
})();
