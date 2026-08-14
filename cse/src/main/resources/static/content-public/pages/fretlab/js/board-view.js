(function () {
  const INLAY_FRETS = [3, 5, 7, 9, 12, 15, 17, 19, 21, 24];
  const MARKER_R = 17.5;

  function create(deps) {
    const $ = deps.$;
    const t = deps.t;
    const M = deps.M;
    const G = deps.G;
    const state = deps.state;
    const ui = deps.ui;
    const names = deps.names;
    const degreeRoman = deps.degreeRoman;
    const tuningMidis = deps.tuningMidis;
    const rootPc = deps.rootPc;
    const freshShapes = deps.freshShapes;
    const playMidi = deps.playMidi;
    const playSequence = deps.playSequence;
    const playSimultaneous = deps.playSimultaneous;
    const render = deps.render;
    const getChordScales = deps.getChordScales;
    const proposalKeyPc = deps.proposalKeyPc;
    const getDrillMode = deps.getDrillMode || function () { return null; };
    const drillNotesLocked = deps.drillNotesLocked || function () { return false; };
    const drillAnswerRevealed = deps.drillAnswerRevealed || function () { return false; };
    const onDrillNeckClick = deps.onDrillNeckClick || function () { return false; };
    const getTheoryBoardMode = deps.getTheoryBoardMode || function () { return null; };

    function boardMode() {
      const drillMode = getDrillMode();
      if (drillMode === 'find') return 'explore';
      if (drillMode === 'build' || drillMode === 'name') return 'chords';
      if (state.tab === 'theory') return getTheoryBoardMode() || 'explore';
      return state.tab;
    }

    function exploreLike() {
      return boardMode() === 'explore';
    }

    const neck = $('#neck');
    const strLabels = $('#string-labels');
    const numLabels = $('#string-numbers');
    const boardWrap = $('#board-wrap');
    const tip = $('#tip');
    const ctxMenu = $('#ctx-menu');

    let ghostPlus = null;
    let ignoreChordNoteClickUntil = 0;
    let markerHost = neck;
    const layers = { rails: null, music: null, chords: null };
    const boardFp = { rails: '', music: '', chords: '' };
    let neckResizeObs = null;
    let lastNeckWidth = 0;
    let kbCursor = { s: 0, f: 0 };
    let kbCursorEl = null;
    let kbRevealFromControls = false;
    let kbCursorListener = null;

    const x = (i) => G.x(i, state.fretCount);
    const bandCenter = (i) => G.bandCenter(i, state.fretCount);
    const leftBound = (i) => G.leftBound(i, state.fretCount);
    const rightBound = (i) => G.rightBound(i, state.fretCount);
    const hexA = G.hexA;
    const yArea = () => G.yArea(state.stringCount);
    const yPx = G.yPx;
    const fretFromX = (pct) => G.fretFromX(pct, state.fretCount);

    function midiLabel(midi) {
      const m = midi | 0;
      const pc = ((m % 12) + 12) % 12;
      return names()[pc] + (Math.floor(m / 12) - 1);
    }

    function positionTip(px, py) {
      const r = tip.getBoundingClientRect();
      const maxX = Math.max(8, window.innerWidth - r.width - 8);
      const maxY = Math.max(8, window.innerHeight - r.height - 8);
      tip.style.left = Math.max(8, Math.min(px + 14, maxX)) + 'px';
      tip.style.top = Math.max(8, Math.min(py + 20, maxY)) + 'px';
    }

    function showTip(html, px, py) {
      tip.innerHTML = html;
      tip.style.display = 'block';
      positionTip(px, py);
    }

    function hideTip() {
      tip.style.display = 'none';
    }

    function tipVisible() {
      return tip.style.display !== 'none';
    }

    function tipRow(key, val, valCls) {
      return '<div class="tip-row"><span class="tip-k">' + key + '</span><span class="tip-v' + (valCls ? ' ' + valCls : '') + '">' + val + '</span></div>';
    }

    function tipHtml(label) {
      const dash = t('tip.emDash');
      const drillMode = getDrillMode();
      const hideTheory = (drillMode === 'find' || drillMode === 'name') && !drillAnswerRevealed();
      const deg = hideTheory ? dash : (label.tipDegree || dash);
      const iv = hideTheory ? dash : (label.tipIntervalHtml || label.tipInterval || dash);
      const note = hideTheory ? dash : (label.octaveName || label.name || dash);
      return tipRow(t('tip.note'), note) +
        tipRow(t('tip.fret'), label.fretText || dash) +
        tipRow(t('tip.degree'), deg, !hideTheory && label.root ? 'tr' : '') +
        tipRow(t('tip.interval'), iv, 'ti');
    }

    function positionCtx(px, py) {
      const r = ctxMenu.getBoundingClientRect();
      const maxX = Math.max(8, window.innerWidth - r.width - 8);
      const maxY = Math.max(8, window.innerHeight - r.height - 8);
      ctxMenu.style.left = Math.max(8, Math.min(px, maxX)) + 'px';
      ctxMenu.style.top = Math.max(8, Math.min(py, maxY)) + 'px';
    }

    function closeCtx() {
      ctxMenu.style.display = 'none';
    }

    function openChordMenu(px, py, nt) {
      const pinned = state.pinRoot && state.pinRoot.s === nt.s && state.pinRoot.f === nt.f;
      ctxMenu.innerHTML =
        '<button type="button" class="ctx-item" data-act="pin">' +
        '<span class="ctx-check">' + (pinned ? '\u2726' : '') + '</span>' +
        '<span>' + (pinned ? t('ctx.unpinRoot') : t('ctx.pinAsRoot')) + '</span>' +
        '</button>' +
        '<button type="button" class="ctx-item" data-act="iv">' +
        '<span class="ctx-check">' + (state.showIntervalGhost ? '\u2713' : '') + '</span>' +
        '<span>' + t('ctx.relativeDegreesIntervals') + '</span>' +
        '</button>';
      ctxMenu.dataset.s = nt.s;
      ctxMenu.dataset.f = nt.f;
      ctxMenu.style.display = 'block';
      positionCtx(px, py);
    }

    function relText(semis, degreeOverride, join) {
      return M.formatRel(semis, false, state.showIntervals, degreeOverride, join);
    }

    function makeMarker(pc, fret, stringIdx, midis, label, cls, color, colorR) {
      const m = document.createElement('div');
      m.className = 'marker ' + cls;
      const classes = cls.split(/\s+/);
      const isRootHighlight = classes.indexOf('root') >= 0;
      if (color) {
        m.style.setProperty('--mc', color);
        m.style.setProperty('--mcL', color);
        if (!isRootHighlight) m.classList.add('shaped');
      }
      if (colorR) m.style.setProperty('--mcR', colorR);
      const left = bandCenter(fret);
      m.style.left = left + '%';
      m.style.top = label.top + 'px';
      m.style.zIndex = 10;
      m.dataset.sf = stringIdx + ':' + fret;

      const nameTxt = label.name;
      const degTxt = label.degree;
      const intTxt = label.interval;

      m.innerHTML =
        '<span class="mtop">' +
        (nameTxt ? '<span class="mn">' + nameTxt + '</span>' : '') +
        (degTxt ? '<span class="ms">' + degTxt + '</span>' : '') +
        '</span>' +
        (intTxt ? '<span class="ml"></span><span class="mi">' + intTxt + '</span>' : '') +
        (label.charTip === 'up' || label.charTip === 'down'
          ? '<span class="char-tip char-' + label.charTip + '" aria-hidden="true">' + (label.charTip === 'up' ? '\u2191' : '\u2193') + '</span>'
          : '');

      const midi = midis[stringIdx] + fret;
      m.addEventListener('click', (e) => {
        e.stopPropagation();
        const drillMode = getDrillMode();
        if (drillMode === 'find') {
          onDrillNeckClick(stringIdx, fret, {});
          return;
        }
        if (drillMode === 'name') {
          if (drillNotesLocked()) return;
          return;
        }
        if (drillMode === 'build') {
          onDrillNeckClick(stringIdx, fret, { remove: true });
          return;
        }
        if (state.tab === 'chords') {
          if (Date.now() < ignoreChordNoteClickUntil) return;
          removeAt(stringIdx, fret);
          return;
        }
        playMidi(midi);
      });
      m.addEventListener('mouseenter', (e) => {
        showTip(tipHtml(label), e.clientX, e.clientY);
      });
      m.addEventListener('mouseleave', hideTip);
      markerHost.appendChild(m);
      return m;
    }

    function pinRootPc(midis) {
      if (!state.pinRoot) return null;
      const m = midis[state.pinRoot.s];
      if (m == null) return null;
      return (((m + state.pinRoot.f) % 12) + 12) % 12;
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
      ghostPlus.className = 'marker ghost-plus';
      ghostPlus.innerHTML = '<span class="place-plus">+</span>';
      ghostPlus.style.display = 'none';
      const host = layers.chords || neck;
      host.appendChild(ghostPlus);
    }

    function canPlaceNotes() {
      const drillMode = getDrillMode();
      if (drillMode === 'build') return true;
      if (drillMode === 'find' || drillMode === 'name') return false;
      return state.tab === 'chords';
    }

    function clampKbCursor() {
      const n = Math.max(1, state.stringCount | 0);
      const N = Math.max(0, state.fretCount | 0);
      kbCursor.s = Math.max(0, Math.min(n - 1, kbCursor.s | 0));
      kbCursor.f = Math.max(0, Math.min(N, kbCursor.f | 0));
    }

    function shouldShowKbCursor() {
      return canPlaceNotes() && (document.activeElement === neck || kbRevealFromControls);
    }

    function notifyKbCursor() {
      if (typeof kbCursorListener === 'function') kbCursorListener(kbCursor.s, kbCursor.f);
    }

    function setKbCursor(s, f, opts) {
      kbCursor.s = s | 0;
      kbCursor.f = f | 0;
      clampKbCursor();
      if (opts && opts.fromControls) kbRevealFromControls = true;
      updateKbCursorVisual();
      if (!(opts && opts.silent)) notifyKbCursor();
    }

    function getKbCursor() {
      clampKbCursor();
      return { s: kbCursor.s, f: kbCursor.f };
    }

    function updateKbCursorVisual() {
      clampKbCursor();
      const host = layers.chords || neck;
      if (!host) return;
      if (!kbCursorEl || kbCursorEl.parentNode !== host) {
        kbCursorEl = document.createElement('div');
        kbCursorEl.className = 'kb-cursor';
        kbCursorEl.setAttribute('aria-hidden', 'true');
        host.appendChild(kbCursorEl);
      }
      if (!shouldShowKbCursor()) {
        kbCursorEl.style.display = 'none';
        return;
      }
      const area = yArea();
      kbCursorEl.style.left = bandCenter(kbCursor.f) + '%';
      kbCursorEl.style.top = yPx(kbCursor.s, area) + 'px';
      kbCursorEl.style.display = 'block';
    }

    function placeAt(s, f) {
      const drillMode = getDrillMode();
      s = s | 0;
      f = f | 0;
      setKbCursor(s, f, { silent: true });
      if (drillMode === 'build') {
        onDrillNeckClick(kbCursor.s, kbCursor.f, {});
        notifyKbCursor();
        return;
      }
      if (state.tab !== 'chords') return;
      if (Date.now() < ignoreChordNoteClickUntil) return;
      const i = state.chordNotes.findIndex((nt) => nt.s === kbCursor.s);
      if (state.pinRoot && i >= 0 && state.pinRoot.s === state.chordNotes[i].s && state.pinRoot.f === state.chordNotes[i].f) {
        state.pinRoot = null;
      }
      if (i >= 0) state.chordNotes[i] = { s: kbCursor.s, f: kbCursor.f, id: state.chordNotes[i].id };
      else state.chordNotes.push({ s: kbCursor.s, f: kbCursor.f, id: ++ui.noteSeq });
      notifyKbCursor();
      render();
    }

    function removeAt(s, f) {
      const drillMode = getDrillMode();
      s = s | 0;
      const exact = f != null && f !== '';
      if (exact) f = f | 0;
      if (exact) setKbCursor(s, f, { silent: true });
      else setKbCursor(s, kbCursor.f, { silent: true });

      if (drillMode === 'build') {
        let i = -1;
        if (exact) i = state.chordNotes.findIndex((nt) => nt.s === s && nt.f === f);
        else i = state.chordNotes.findIndex((nt) => nt.s === s);
        if (i >= 0) onDrillNeckClick(state.chordNotes[i].s, state.chordNotes[i].f, { remove: true });
        notifyKbCursor();
        return;
      }
      if (state.tab !== 'chords') return;
      if (Date.now() < ignoreChordNoteClickUntil) return;
      let i = -1;
      if (exact) i = state.chordNotes.findIndex((nt) => nt.s === s && nt.f === f);
      else i = state.chordNotes.findIndex((nt) => nt.s === s);
      if (i < 0) {
        notifyKbCursor();
        return;
      }
      const nt = state.chordNotes[i];
      state.chordNotes.splice(i, 1);
      if (state.pinRoot && state.pinRoot.s === nt.s && state.pinRoot.f === nt.f) state.pinRoot = null;
      notifyKbCursor();
      render();
    }

    function toggleAt(s, f) {
      s = s | 0;
      f = f | 0;
      const i = state.chordNotes.findIndex((nt) => nt.s === s && nt.f === f);
      if (i >= 0) removeAt(s, f);
      else placeAt(s, f);
    }

    function moveKbCursor(ds, df) {
      clampKbCursor();
      setKbCursor(kbCursor.s + (ds | 0), kbCursor.f + (df | 0), { silent: false });
    }

    function clearKbReveal() {
      kbRevealFromControls = false;
      updateKbCursorVisual();
    }

    function placeGhostPlus(pct, y) {
      if (!ghostPlus) return;
      const drillMode = getDrillMode();
      if (drillMode === 'name' && drillNotesLocked()) {
        ghostPlus.style.display = 'none';
        return;
      }
      if (state.tab !== 'chords' && drillMode !== 'build') {
        ghostPlus.style.display = 'none';
        return;
      }
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
      const hideRoot = getDrillMode() === 'name' && drillNotesLocked();
      const chordRootPc = hideRoot ? -1 : (ui.chordResult ? ui.chordResult.rootPc : -1);
      const isRoot = pc === chordRootPc;
      const isPinned = !hideRoot && state.pinRoot && state.pinRoot.s === nt.s && state.pinRoot.f === nt.f;
      const rel = chordRootPc >= 0 ? (pc - chordRootPc + 12) % 12 : -1;
      const cdesc = ui.chordResult && ui.chordResult.matched ? ui.chordResult.c : null;
      const role = rel >= 0 ? M.chordRoleText(rel, cdesc, state.compoundIntervals) : '';
      const ivAbbr = rel >= 0 ? M.chordIntervalText(rel, cdesc, state.compoundIntervals) : '';
      const lbl = {
        name: state.showNames ? names()[pc] : '',
        degree: rel >= 0 && state.showDegrees ? degreeRoman(rel) : '',
        interval: rel >= 0 && state.showIntervals ? ivAbbr : '',
        tipDegree: rel >= 0 ? (role || degreeRoman(rel)) : '',
        tipInterval: ivAbbr,
        tipIntervalHtml: rel >= 0 ? M.chordIntervalFull(rel, cdesc, state.compoundIntervals) : '',
        octaveName: midiLabel(midi),
        fretText: nt.f === 0 ? t('tip.fretOpen') : String(nt.f),
        root: isRoot,
        top: yPx(nt.s, area)
      };
      const m = makeMarker(pc, nt.f, nt.s, midis, lbl, isRoot ? 'root' : 'scale', null);
      if (isPinned) {
        const star = document.createElement('span');
        star.className = 'pin-star';
        star.textContent = '*';
        m.appendChild(star);
      }
      function openNoteMenu(clientX, clientY) {
        if (getDrillMode() === 'name' && drillNotesLocked()) return;
        ignoreChordNoteClickUntil = Date.now() + 450;
        openChordMenu(clientX, clientY, nt);
        if (typeof navigator !== 'undefined' && navigator.vibrate) {
          try { navigator.vibrate(12); } catch (err) { /* ignore */ }
        }
      }
      m.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (getDrillMode() === 'name' && drillNotesLocked()) return;
        openNoteMenu(e.clientX, e.clientY);
      });
      let lpTimer = null;
      let lpOrigin = null;
      function clearLongPress() {
        if (lpTimer) {
          clearTimeout(lpTimer);
          lpTimer = null;
        }
        lpOrigin = null;
      }
      m.addEventListener('pointerdown', (e) => {
        if (e.pointerType === 'mouse' && e.button !== 0) return;
        if (e.pointerType === 'mouse') return;
        clearLongPress();
        lpOrigin = { x: e.clientX, y: e.clientY };
        const px = e.clientX;
        const py = e.clientY;
        lpTimer = setTimeout(() => {
          lpTimer = null;
          lpOrigin = null;
          openNoteMenu(px, py);
        }, 480);
      });
      m.addEventListener('pointermove', (e) => {
        if (!lpOrigin) return;
        const dx = e.clientX - lpOrigin.x;
        const dy = e.clientY - lpOrigin.y;
        if ((dx * dx + dy * dy) > 100) clearLongPress();
      });
      m.addEventListener('pointerup', clearLongPress);
      m.addEventListener('pointercancel', clearLongPress);
    }

    function renderScaleGhost(midis, area) {
      const scaleSel = ui.scaleSel;
      if (!scaleSel || !scaleSel.ivs) return;
      const N = state.fretCount;
      const ghostRootPc = scaleSel.rootPc;
      const ivs = scaleSel.ivs;
      const abs = new Set(ivs.map((v) => (ghostRootPc + v) % 12));
      midis.forEach((base, s) => {
        for (let f = 0; f <= N; f++) {
          const pc = (((base + f) % 12) + 12) % 12;
          const rel = (pc - ghostRootPc + 12) % 12;
          const inScale = abs.has(pc);
          const name = state.showNames ? names()[pc] : '';
          const degree = state.showDegrees ? degreeRoman(rel, scaleSel.family, scaleSel.modeIndex) : '';
          const interval = state.showIntervals ? relText(rel) : '';
          if (!inScale && !name && !degree && !interval) continue;
          const m = document.createElement('div');
          m.className = 'marker ' + (inScale ? (pc === ghostRootPc ? 'root' : 'scale') : 'plain') + ' soft ghost-scale';
          m.style.left = bandCenter(f) + '%';
          m.style.top = yPx(s, area) + 'px';
          m.style.zIndex = 3;
          m.style.pointerEvents = 'none';
          m.style.cursor = 'default';
          m.innerHTML =
            '<span class="mtop">' +
            (name ? '<span class="mn">' + name + '</span>' : '') +
            (degree ? '<span class="ms">' + degree + '</span>' : '') +
            '</span>' +
            (interval ? '<span class="ml"></span><span class="mi">' + interval + '</span>' : '');
          markerHost.appendChild(m);
        }
      });
    }

    function renderIntervalGhost(midis, area) {
      if (!state.showIntervalGhost || (!state.showDegrees && !state.showIntervals)) return;
      const chordRootPc = ui.chordResult ? ui.chordResult.rootPc : null;
      if (chordRootPc == null) return;
      const n = midis.length;
      const N = state.fretCount;
      const notes = state.chordNotes.filter((nt) => nt.s >= 0 && nt.s < n && nt.f >= 0 && nt.f <= N);
      if (!notes.length) return;

      const occupied = {};
      let minF = N;
      let maxF = 0;
      notes.forEach((nt) => {
        occupied[nt.s + ':' + nt.f] = true;
        if (nt.f < minF) minF = nt.f;
        if (nt.f > maxF) maxF = nt.f;
      });

      // Cap DOM: only label a padded window around the current voicing (not the full neck).
      const pad = 4;
      let fLo = Math.max(0, minF - pad);
      let fHi = Math.min(N, maxF + pad);
      const minSpan = 8;
      if (fHi - fLo < minSpan) {
        const mid = (minF + maxF) >> 1;
        fLo = Math.max(0, mid - (minSpan >> 1));
        fHi = Math.min(N, fLo + minSpan);
        fLo = Math.max(0, fHi - minSpan);
      }

      for (let s = 0; s < n; s++) {
        const base = midis[s];
        for (let f = fLo; f <= fHi; f++) {
          if (occupied[s + ':' + f]) continue;
          const pc = (((base + f) % 12) + 12) % 12;
          const rel = (pc - chordRootPc + 12) % 12;
          const degree = state.showDegrees ? degreeRoman(rel) : '';
          const interval = relText(rel);
          if (!degree && !interval) continue;
          const m = document.createElement('div');
          m.className = 'marker plain soft ghost-interval';
          m.style.left = bandCenter(f) + '%';
          m.style.top = yPx(s, area) + 'px';
          m.style.zIndex = 2;
          m.style.pointerEvents = 'none';
          m.style.cursor = 'default';
          m.innerHTML =
            '<span class="mtop">' +
            (degree ? '<span class="ms">' + degree + '</span>' : '') +
            '</span>' +
            (interval ? '<span class="ml"></span><span class="mi">' + interval + '</span>' : '');
          markerHost.appendChild(m);
        }
      }
    }

    function renderChordNotes(midis, area) {
      const n = midis.length;
      const N = state.fretCount;
      const valid = state.chordNotes.filter((nt) => nt.s >= 0 && nt.s < n && nt.f >= 0 && nt.f <= N);
      const hideRoot = getDrillMode() === 'name' && drillNotesLocked();
      if (hideRoot) {
        // Keep markers neutral — live analysis would paint the guessed root.
        ui.chordResult = null;
        ui.scaleSel = null;
        ui.chordScaleFocus = null;
      } else {
        ui.chordResult = M.analyzeChord(valid.map((nt) => midis[nt.s] + nt.f), pinRootPc(midis), { names: names(), compoundIntervals: state.compoundIntervals });
        if (ui.chordResult && ui.chordResult.matched && ui.scalePropFollow) ui.scalePropKey = ui.chordResult.rootPc;
        if (!ui.chordResult || !ui.chordResult.matched) {
          ui.scaleSel = null;
          ui.chordScaleFocus = null;
        } else if (ui.scaleSel) {
          const keyPc = proposalKeyPc(ui.chordResult);
          const sc = getChordScales(ui.chordResult, keyPc);
          const ok = sc && ui.scaleSel.rootPc === keyPc && sc.full.concat(sc.partial).some((e) => e.name === ui.scaleSel.name && e.family === ui.scaleSel.family);
          if (!ok) ui.scaleSel = null;
        }
      }
      const cnt = $('#chord-count');
      if (cnt) cnt.textContent = t('controls.chords.noteCount', { count: valid.length });
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
      playSequence(notes, ui.chordResult ? ui.chordResult.rootPc : null);
    }

    function playChordSimultaneous() {
      if (!state.chordNotes.length) return;
      const midis = tuningMidis();
      const midiList = state.chordNotes
        .filter((nt) => nt.s >= 0 && nt.s < midis.length && nt.f >= 0 && nt.f <= state.fretCount)
        .map((nt) => midis[nt.s] + nt.f);
      if (midiList.length) playSimultaneous(midiList);
    }

    function markerLabels(mk, o) {
      const markerRootPc = o.rootPc;
      const intIdx = (mk.pc - markerRootPc + 12) % 12;
      const inScale = o.pcs.has(mk.pc);
      let degree = '';
      let interval = '';
      let name = '';
      const midiVal = o.midis[mk.stringIdx] + mk.fret;
      if (state.showNames) name = o.nm[mk.pc];
      const octaveName = midiLabel(midiVal);
      if (state.showDegrees) degree = degreeRoman(intIdx);
      interval = relText(intIdx);
      const charTip = inScale && o.charAlts && o.charAlts[intIdx] ? o.charAlts[intIdx] : null;
      return {
        name: name,
        degree: degree,
        interval: interval,
        tipDegree: degreeRoman(intIdx),
        tipInterval: M.QUALITIES[intIdx],
        tipIntervalHtml: M.qualityFull(intIdx),
        octaveName: octaveName,
        fretText: mk.fret === 0 ? t('tip.fretOpen') : String(mk.fret),
        root: mk.pc === markerRootPc && inScale,
        charTip: charTip
      };
    }

    function computeData() {
      const drillMode = getDrillMode();
      const effectiveTab = boardMode();
      const data = window.FretCaged.computeData({
        M: M,
        midis: tuningMidis(),
        fretCount: state.fretCount,
        root: rootPc(),
        tab: effectiveTab,
        showCagedShapes: effectiveTab === 'explore' && state.showCagedShapes && !drillMode,
        cagedRepeats: state.cagedRepeats,
        scaleFamily: state.scaleFamily,
        modeIndex: state.modeIndex,
        shapes: state.shapes,
        showCharOriginals: state.showCharOriginals,
        highlightDegree: drillMode === 'find' ? -1 : state.highlightDegree,
        fullChart: state.fullChart
      });
      data.quartal = null;
      data.nps = null;
      data.berklee = null;
      if (drillMode) return data;
      if (exploreLike() && state.showQuartal && window.FretQuartal) {
        state.quartalStartString = window.FretQuartal.clampStartString(
          state.quartalStartString, state.stringCount, state.quartalHeight
        );
        data.quartal = window.FretQuartal.computeVoicings({
          M: M,
          midis: tuningMidis(),
          fretCount: state.fretCount,
          root: rootPc(),
          scaleFamily: state.scaleFamily,
          modeIndex: state.modeIndex,
          height: state.quartalHeight,
          startString: state.quartalStartString,
          repeats: state.quartalRepeats
        });
        const keep = {};
        (data.quartal.voicings || []).forEach((v) => {
          v.notes.forEach((nt) => { keep[nt.s + ':' + nt.f] = true; });
        });
        data.markers = data.markers.filter((mk) => keep[mk.stringIdx + ':' + mk.fret]);
      } else if (exploreLike() && state.showNps && window.FretNps) {
        data.nps = window.FretNps.computePatterns({
          M: M,
          midis: tuningMidis(),
          fretCount: state.fretCount,
          root: rootPc(),
          scaleFamily: state.scaleFamily,
          modeIndex: state.modeIndex,
          notesPerString: state.npsCount,
          repeats: state.npsRepeats,
          forms: state.npsForms
        });
        const owners = {};
        (data.nps.patterns || []).forEach((p) => {
          p.notes.forEach((nt) => {
            const id = nt.s + ':' + nt.f;
            if (!owners[id]) owners[id] = [];
            owners[id].push(p);
          });
        });
        data.markers = data.markers.filter((mk) => owners[mk.stringIdx + ':' + mk.fret]).map((mk) => {
          const hits = owners[mk.stringIdx + ':' + mk.fret]
            .slice()
            .sort((a, b) => (a.startFret - b.startFret) || (a.degreeRel - b.degreeRel));
          const next = Object.assign({}, mk);
          const classes = (mk.cls || '').split(/\s+/);
          const isRootHighlight = classes.indexOf('root') >= 0;
          next.forms = hits.map((h) => h.key);
          if (hits.length >= 2 && !isRootHighlight) {
            if (classes.indexOf('split') < 0) next.cls = (mk.cls + ' split').trim();
            next.color = hits[0].color;
            next.colorR = hits[hits.length - 1].color;
          } else {
            next.color = hits[0].color;
          }
          return next;
        });
      } else if (exploreLike() && state.showBerklee && window.FretBerklee) {
        data.berklee = window.FretBerklee.computePatterns({
          M: M,
          midis: tuningMidis(),
          fretCount: state.fretCount,
          root: rootPc(),
          scaleFamily: state.scaleFamily,
          modeIndex: state.modeIndex,
          repeats: state.berkleeRepeats,
          forms: state.berkleeForms
        });
        const owners = {};
        (data.berklee.patterns || []).forEach((p) => {
          p.notes.forEach((nt) => {
            const id = nt.s + ':' + nt.f;
            if (!owners[id]) owners[id] = [];
            owners[id].push(p);
          });
        });
        data.markers = data.markers.filter((mk) => owners[mk.stringIdx + ':' + mk.fret]).map((mk) => {
          const hits = owners[mk.stringIdx + ':' + mk.fret]
            .slice()
            .sort((a, b) => (a.startFret - b.startFret) || (a.degreeRel - b.degreeRel));
          const next = Object.assign({}, mk);
          const classes = (mk.cls || '').split(/\s+/);
          const isRootHighlight = classes.indexOf('root') >= 0;
          next.forms = hits.map((h) => h.key);
          if (hits.length >= 2 && !isRootHighlight) {
            if (classes.indexOf('split') < 0) next.cls = (mk.cls + ' split').trim();
            next.color = hits[0].color;
            next.colorR = hits[hits.length - 1].color;
          } else {
            next.color = hits[0].color;
          }
          return next;
        });
      }
      return data;
    }

    function drawDegreeBoxes(patterns, area, className, labelKey) {
      patterns.forEach((pat) => {
        const b = document.createElement('div');
        b.className = className;
        b.dataset.form = pat.key;
        const l = leftBound(pat.visLo);
        const r = rightBound(pat.visHi);
        const yTop = Math.min(yPx(pat.minS, area), yPx(pat.maxS, area));
        const yBot = Math.max(yPx(pat.minS, area), yPx(pat.maxS, area));
        b.style.left = l + '%';
        b.style.width = (r - l) + '%';
        b.style.top = (yTop - 15 - area.step / 2) + 'px';
        b.style.height = (yBot - yTop + 30 + area.step) + 'px';
        b.style.borderColor = pat.color;
        b.style.background = hexA(pat.color, 0.08);
        b.style.zIndex = 1;
        const top = document.createElement('div');
        top.className = 'box-top';
        const playBtn = document.createElement('button');
        playBtn.type = 'button';
        playBtn.className = 'box-play';
        playBtn.innerHTML = '&#9654;';
        playBtn.title = t('controls.shapes.play');
        playBtn.setAttribute('aria-label', t('controls.shapes.play'));
        playBtn.style.background = pat.color;
        playBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          playSequence(pat.notes);
        });
        top.appendChild(playBtn);
        const lab = document.createElement('div');
        lab.className = 'box-label';
        lab.textContent = t(labelKey, { degree: degreeRoman(pat.degreeRel) });
        lab.style.background = pat.color;
        top.appendChild(lab);
        b.appendChild(top);
        b.addEventListener('click', (e) => {
          e.stopPropagation();
          playMidi(pat.rootMidi);
        });
        markerHost.appendChild(b);
      });
    }

    function drawNpsBoxes(patterns, area) {
      drawDegreeBoxes(patterns, area, 'nps-box', 'controls.nps.boxLabel');
    }

    function drawBerkleeBoxes(patterns, area) {
      drawDegreeBoxes(patterns, area, 'berklee-box', 'controls.berklee.boxLabel');
    }

    function drawQuartalLadder(voicings, area) {
      const w = neck.clientWidth || neck.offsetWidth || 1;
      const h = area.height || 1;
      const strokeW = MARKER_R * 1.45;
      const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      svg.setAttribute('viewBox', '0 0 ' + w + ' ' + h);
      svg.setAttribute('preserveAspectRatio', 'none');
      svg.setAttribute('class', 'quartal-lines');

      voicings.forEach((v) => {
        const notes = v.notes.slice().sort((a, b) => a.s - b.s);
        if (notes.length >= 2) {
          const pts = notes.map((nt) =>
            ((bandCenter(nt.f) / 100) * w).toFixed(2) + ',' + yPx(nt.s, area).toFixed(2)
          ).join(' ');
          const play = (e) => {
            e.stopPropagation();
            playSequence(v.notes);
          };
          const poly = document.createElementNS('http://www.w3.org/2000/svg', 'polyline');
          poly.setAttribute('class', 'quartal-link');
          poly.setAttribute('points', pts);
          poly.setAttribute('fill', 'none');
          poly.setAttribute('stroke', v.color);
          poly.setAttribute('stroke-width', String(strokeW));
          poly.setAttribute('stroke-linecap', 'round');
          poly.setAttribute('stroke-linejoin', 'round');
          poly.style.cursor = 'pointer';
          poly.addEventListener('click', play);
          svg.appendChild(poly);
        }

        const bass = notes[0] || v.notes[0];
        const lab = document.createElement('div');
        lab.className = 'quartal-deg';
        lab.textContent = t('controls.quartal.boxLabel', { degree: degreeRoman(v.bassRel) });
        lab.style.left = bandCenter(bass.f) + '%';
        lab.style.top = yPx(bass.s, area) + 'px';
        lab.style.background = v.color;
        lab.title = t('controls.quartal.boxLabel', { degree: degreeRoman(v.bassRel) });
        lab.addEventListener('click', (e) => {
          e.stopPropagation();
          playSequence(v.notes);
        });
        markerHost.appendChild(lab);
      });

      markerHost.appendChild(svg);
    }

    function flagMapFp(obj) {
      if (!obj) return '';
      return Object.keys(obj).filter((k) => obj[k]).sort().join(',');
    }

    function uiLang() {
      try {
        return window.FretI18n && typeof window.FretI18n.lang === 'function' ? window.FretI18n.lang() : 'en';
      } catch (e) {
        return 'en';
      }
    }

    function ensureLayers() {
      if (!neck) return false;
      if (layers.rails && layers.music && layers.chords &&
          layers.rails.parentNode === neck &&
          layers.music.parentNode === neck &&
          layers.chords.parentNode === neck) {
        return true;
      }
      neck.innerHTML = '';
      ['rails', 'music', 'chords'].forEach((id) => {
        const el = document.createElement('div');
        el.className = 'neck-layer neck-' + id;
        el.id = 'neck-' + id;
        neck.appendChild(el);
        layers[id] = el;
      });
      boardFp.rails = '';
      boardFp.music = '';
      boardFp.chords = '';
      ghostPlus = null;
      return true;
    }

    function railsFingerprint(midis, area) {
      return [
        midis.join(','),
        state.fretCount,
        state.stringCount,
        area.height,
        area.topPad,
        area.step,
        uiLang()
      ].join('|');
    }

    function musicFingerprint(midis) {
      const drillMode = getDrillMode() || '';
      return [
        boardMode(),
        drillMode,
        midis.join(','),
        state.fretCount,
        state.scaleKey,
        state.scaleFamily,
        state.modeIndex,
        state.showCagedShapes ? 1 : 0,
        state.showQuartal ? 1 : 0,
        state.showNps ? 1 : 0,
        state.showBerklee ? 1 : 0,
        state.cagedRepeats === false ? 0 : 1,
        state.showRootLines ? 1 : 0,
        state.showCharOriginals !== false ? 1 : 0,
        state.showCharTips ? 1 : 0,
        state.fullChart ? 1 : 0,
        state.highlightDegree,
        flagMapFp(state.shapes),
        state.quartalHeight,
        state.quartalStartString,
        state.quartalRepeats === false ? 0 : 1,
        state.npsCount,
        state.npsRepeats === false ? 0 : 1,
        flagMapFp(state.npsForms),
        state.berkleeRepeats === false ? 0 : 1,
        flagMapFp(state.berkleeForms),
        state.showNames ? 1 : 0,
        state.showDegrees ? 1 : 0,
        state.showIntervals ? 1 : 0,
        state.noteSpell || '',
        uiLang(),
        lastNeckWidth
      ].join('|');
    }

    function chordsFingerprint(midis) {
      const mode = boardMode();
      const drillMode = getDrillMode() || '';
      const wantChords = mode === 'chords' || drillMode === 'build' || drillMode === 'name';
      if (!wantChords) return 'off';
      const notes = (state.chordNotes || []).map((nt) => nt.s + ':' + nt.f + ':' + (nt.id || '')).join(',');
      const pin = state.pinRoot ? state.pinRoot.s + ':' + state.pinRoot.f : '';
      const scaleSel = ui.scaleSel
        ? [ui.scaleSel.name, ui.scaleSel.family, ui.scaleSel.rootPc, ui.scaleSel.modeIndex].join(':')
        : '';
      return [
        mode,
        drillMode,
        midis.join(','),
        state.fretCount,
        notes,
        pin,
        state.showIntervalGhost ? 1 : 0,
        state.showNames ? 1 : 0,
        state.showDegrees ? 1 : 0,
        state.showIntervals ? 1 : 0,
        state.compoundIntervals ? 1 : 0,
        drillNotesLocked() ? 1 : 0,
        scaleSel,
        uiLang()
      ].join('|');
    }

    function drawRails(area, midis) {
      const host = layers.rails;
      const n = midis.length;
      const N = state.fretCount;
      host.innerHTML = '';
      strLabels.innerHTML = '';
      numLabels.innerHTML = '';

      midis.forEach((m, i) => {
        const lbl = document.createElement('div');
        lbl.className = 'string-label';
        lbl.textContent = midiLabel(m);
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
        const tt = n > 1 ? i / (n - 1) : 0;
        line.style.setProperty('--string-t', String(tt));
        if (i <= n - 3) {
          const ratio = n > 3 ? i / (n - 3) : 0;
          const period = 10 - (10 - 3.5) * ratio;
          line.style.backgroundImage = 'repeating-linear-gradient(45deg, var(--bg) 0 1.5px, transparent 1.5px ' + period + 'px)';
        }
        host.appendChild(line);
      });

      for (let i = 0; i <= N; i++) {
        const fl = document.createElement('div');
        fl.className = i === 0 ? 'fret-line nut' : 'fret-line';
        fl.style.left = x(i) + '%';
        fl.style.top = area.topPad + 'px';
        fl.style.height = n * area.step + 'px';
        host.appendChild(fl);
      }

      INLAY_FRETS.forEach((f) => {
        if (f > N) return;
        const double = f === 12 || f === 24;
        const d = document.createElement('div');
        d.className = 'inlay' + (double ? ' double' : '');
        d.style.left = bandCenter(f) + '%';
        d.style.top = (area.topPad + (n + 0.75) * area.step) + 'px';
        host.appendChild(d);
      });

      for (let i = 0; i <= N; i++) {
        const fl = document.createElement('div');
        fl.className = 'fret-num';
        fl.textContent = i;
        fl.style.left = bandCenter(i) + '%';
        host.appendChild(fl);
      }
    }

    function drawMusic(area, midis) {
      const host = layers.music;
      host.innerHTML = '';
      markerHost = host;
      const nm = names();
      const data = computeData();
      const labelOpts = {
        rootPc: rootPc(),
        ordered: data.ordered,
        pcs: data.pcs,
        nm: nm,
        midis: midis,
        area: area,
        charAlts: (exploreLike() || getDrillMode() === 'find') && state.showCharTips && getDrillMode() !== 'find'
          ? M.characteristicAlts(state.scaleFamily, state.modeIndex)
          : null
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
        const playBtn = document.createElement('button');
        playBtn.type = 'button';
        playBtn.className = 'box-play';
        playBtn.innerHTML = '&#9654;';
        playBtn.title = t('controls.shapes.play');
        playBtn.setAttribute('aria-label', t('controls.shapes.play'));
        playBtn.style.background = box.color;
        playBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          playSequence(box.notes);
        });
        top.appendChild(playBtn);
        const lab = document.createElement('div');
        lab.className = 'box-label';
        lab.textContent = t('controls.shapes.boxLabel', { key: box.key });
        lab.style.background = box.color;
        top.appendChild(lab);
        b.appendChild(top);
        b.addEventListener('click', (e) => {
          e.stopPropagation();
          playMidi(box.rootMidi);
        });
        host.appendChild(b);
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
          poly.setAttribute('stroke-width', '3');
          poly.setAttribute('stroke-dasharray', '7 5');
          poly.setAttribute('vector-effect', 'non-scaling-stroke');
          poly.setAttribute('fill', 'none');
          poly.setAttribute('stroke-linecap', 'round');
          svg.appendChild(poly);
        });
        if (svg.childNodes.length) host.appendChild(svg);
      }

      if (data.quartal && data.quartal.voicings && data.quartal.voicings.length) {
        drawQuartalLadder(data.quartal.voicings, area);
      }
      if (data.nps && data.nps.patterns && data.nps.patterns.length) {
        drawNpsBoxes(data.nps.patterns, area);
      }
      if (data.berklee && data.berklee.patterns && data.berklee.patterns.length) {
        drawBerkleeBoxes(data.berklee.patterns, area);
      }
      markerHost = neck;
    }

    function drawChords(area, midis) {
      const host = layers.chords;
      host.innerHTML = '';
      ghostPlus = null;
      kbCursorEl = null;
      markerHost = host;
      const drillMode = getDrillMode();
      if (boardMode() === 'chords' || drillMode === 'build' || drillMode === 'name') {
        renderChordNotes(midis, area);
        if (state.tab === 'chords') {
          renderIntervalGhost(midis, area);
          renderScaleGhost(midis, area);
        }
      }
      updateKbCursorVisual();
      markerHost = neck;
    }

    function drawBoard() {
      if (!neck || !ensureLayers()) return;
      const area = yArea();
      const midis = tuningMidis();
      neck.style.height = area.height + 'px';
      lastNeckWidth = neck.clientWidth || neck.offsetWidth || lastNeckWidth || 0;

      const fpRails = railsFingerprint(midis, area);
      const fpMusic = musicFingerprint(midis);
      const fpChords = chordsFingerprint(midis);
      const dirtyRails = fpRails !== boardFp.rails;
      const dirtyMusic = dirtyRails || fpMusic !== boardFp.music;
      const dirtyChords = dirtyRails || fpChords !== boardFp.chords;

      if (!dirtyRails && !dirtyMusic && !dirtyChords) return;

      if (dirtyRails) {
        drawRails(area, midis);
        boardFp.rails = fpRails;
      }
      if (dirtyMusic) {
        drawMusic(area, midis);
        boardFp.music = fpMusic;
      }
      if (dirtyChords) {
        drawChords(area, midis);
        boardFp.chords = fpChords;
      }
    }

    function bindNeckResize() {
      if (!neck || neckResizeObs) return;
      const onWidth = () => {
        const w = neck.clientWidth || neck.offsetWidth || 0;
        if (!w || w === lastNeckWidth) return;
        lastNeckWidth = w;
        if (state.showQuartal || (state.showCagedShapes && state.showRootLines)) {
          boardFp.music = '';
          drawBoard();
        }
      };
      if (typeof ResizeObserver !== 'undefined') {
        neckResizeObs = new ResizeObserver(() => onWidth());
        neckResizeObs.observe(neck);
      } else {
        window.addEventListener('resize', onWidth);
        neckResizeObs = { disconnect: function () {} };
      }
    }

    function bindNeckEvents() {
      bindNeckResize();
      neck.setAttribute('tabindex', '0');
      neck.setAttribute('role', 'application');
      neck.setAttribute('aria-label', t('controls.chords.neckAria'));

      ctxMenu.addEventListener('click', (e) => {
        const btn = e.target.closest ? e.target.closest('.ctx-item') : null;
        if (!btn) return;
        const s = parseInt(ctxMenu.dataset.s, 10);
        const f = parseInt(ctxMenu.dataset.f, 10);
        if (btn.dataset.act === 'pin') {
          if (state.pinRoot && state.pinRoot.s === s && state.pinRoot.f === f) state.pinRoot = null;
          else state.pinRoot = { s: s, f: f };
          render();
        } else if (btn.dataset.act === 'iv') {
          state.showIntervalGhost = !state.showIntervalGhost;
          render();
        }
        closeCtx();
      });

      neck.addEventListener('click', (e) => {
        const drillMode = getDrillMode();
        if (drillMode === 'build') {
          if (Date.now() < ignoreChordNoteClickUntil) return;
          const r = neck.getBoundingClientRect();
          const pct = ((e.clientX - r.left) / r.width) * 100;
          const y = e.clientY - r.top;
          const g = gridAt(pct, y);
          placeAt(g.s, g.f);
          return;
        }
        if (state.tab !== 'chords') return;
        if (Date.now() < ignoreChordNoteClickUntil) return;
        const r = neck.getBoundingClientRect();
        const pct = ((e.clientX - r.left) / r.width) * 100;
        const y = e.clientY - r.top;
        const g = gridAt(pct, y);
        placeAt(g.s, g.f);
      });

      neck.addEventListener('keydown', (e) => {
        if (!canPlaceNotes()) return;
        if (e.key === 'ArrowUp') {
          e.preventDefault();
          moveKbCursor(1, 0);
        } else if (e.key === 'ArrowDown') {
          e.preventDefault();
          moveKbCursor(-1, 0);
        } else if (e.key === 'ArrowLeft') {
          e.preventDefault();
          moveKbCursor(0, -1);
        } else if (e.key === 'ArrowRight') {
          e.preventDefault();
          moveKbCursor(0, 1);
        } else if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          toggleAt(kbCursor.s, kbCursor.f);
        } else if (e.key === 'Delete' || e.key === 'Backspace') {
          e.preventDefault();
          removeAt(kbCursor.s);
        }
      });

      neck.addEventListener('focus', () => {
        updateKbCursorVisual();
      });
      neck.addEventListener('blur', () => {
        updateKbCursorVisual();
      });

      function updatePlaceGhost(clientX, clientY) {
        const drillMode = getDrillMode();
        if (state.tab !== 'chords' && drillMode !== 'build') return;
        if (drillMode === 'name' && drillNotesLocked()) return;
        const r = neck.getBoundingClientRect();
        const pct = ((clientX - r.left) / r.width) * 100;
        const y = clientY - r.top;
        placeGhostPlus(pct, y);
      }

      neck.addEventListener('pointermove', (e) => {
        updatePlaceGhost(e.clientX, e.clientY);
      });
      neck.addEventListener('pointerdown', (e) => {
        const drillMode = getDrillMode();
        if (state.tab !== 'chords' && drillMode !== 'build') return;
        if (e.pointerType === 'mouse' && e.button !== 0) return;
        updatePlaceGhost(e.clientX, e.clientY);
      });
      neck.addEventListener('pointerleave', () => {
        if (ghostPlus) ghostPlus.style.display = 'none';
      });

      boardWrap.addEventListener('mousemove', (e) => {
        if (tipVisible()) {
          positionTip(e.clientX, e.clientY);
        }
      });

      document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeCtx();
      });
      document.addEventListener('click', (e) => {
        if (ctxMenu && !ctxMenu.contains(e.target)) closeCtx();
      });
      document.addEventListener('contextmenu', (e) => {
        if (ctxMenu && !ctxMenu.contains(e.target)) closeCtx();
      });
      window.addEventListener('scroll', closeCtx, true);
      window.addEventListener('resize', closeCtx);
    }

    return {
      drawBoard: drawBoard,
      renderChordNotes: renderChordNotes,
      computeData: computeData,
      gridAt: gridAt,
      makeGhostPlus: makeGhostPlus,
      placeGhostPlus: placeGhostPlus,
      openChordMenu: openChordMenu,
      closeCtx: closeCtx,
      playChord: playChord,
      playChordSimultaneous: playChordSimultaneous,
      bindNeckEvents: bindNeckEvents,
      showTip: showTip,
      hideTip: hideTip,
      positionTip: positionTip,
      tipVisible: tipVisible,
      tipRow: tipRow,
      midiLabel: midiLabel,
      placeAt: placeAt,
      removeAt: removeAt,
      toggleAt: toggleAt,
      getKbCursor: getKbCursor,
      setKbCursor: setKbCursor,
      updateKbCursorVisual: updateKbCursorVisual,
      clearKbReveal: clearKbReveal,
      syncNeckAria: function () {
        if (neck) neck.setAttribute('aria-label', t('controls.chords.neckAria'));
      },
      setKbCursorListener: function (fn) {
        kbCursorListener = typeof fn === 'function' ? fn : null;
      }
    };
  }

  window.FretBoardView = {
    INLAY_FRETS: INLAY_FRETS,
    MARKER_R: MARKER_R,
    create: create
  };
})();
