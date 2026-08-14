(function () {
  const FORMULA = ['1', '♭2', '2', '♭3', '3', '4', '♯4', '5', '♭6', '6', '♭7', '7'];

  function create(deps) {
    const $ = deps.$;
    const t = deps.t;
    const M = deps.M;
    const state = deps.state;
    const ui = deps.ui;
    const names = deps.names;
    const theoryInfo = deps.theoryInfo;
    const degreeRoman = deps.degreeRoman;
    const tuningMidis = deps.tuningMidis;
    const scaleDisplayName = deps.scaleDisplayName;
    const placeChordPcs = deps.placeChordPcs;
    const chordDiagramSvg = deps.chordDiagramSvg;
    const chordDisplaySuffix = deps.chordDisplaySuffix;
    const chordSymbolStackHtml = deps.chordSymbolStackHtml;
    const encodeVoicing = deps.encodeVoicing;
    const decodeVoicing = deps.decodeVoicing;
    const midisFromPcsAscending = deps.midisFromPcsAscending;
    const playBlocks = deps.playBlocks;
    const playSimultaneous = deps.playSimultaneous;
    const applySharedScale = deps.applySharedScale;
    const openScaleChordInAnalyzer = deps.openScaleChordInAnalyzer;
    const openTheoryArticle = deps.openTheoryArticle;
    const saveState = deps.saveState;

    function clearPlaying() {
      const root = $('#scale-details');
      if (!root) return;
      root.querySelectorAll('.sd-chord-chip.playing').forEach((el) => el.classList.remove('playing'));
    }

    function resolveTarget() {
      if (state.tab === 'profiles' || state.tab === 'theory' || state.tab === 'drills') return null;
      if (state.tab === 'chords') {
        if (ui.scaleSel && ui.scaleSel.family && M.SCALES[ui.scaleSel.family]) {
          return { root: ui.scaleSel.rootPc, family: ui.scaleSel.family, modeIndex: ui.scaleSel.modeIndex | 0 };
        }
        if (ui.chordScaleFocus && M.SCALES[ui.chordScaleFocus.family]) {
          return { root: ui.chordScaleFocus.rootPc, family: ui.chordScaleFocus.family, modeIndex: ui.chordScaleFocus.modeIndex | 0 };
        }
        return null;
      }
      return {
        root: state.scaleKey,
        family: state.scaleFamily,
        modeIndex: state.modeIndex
      };
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
      return '<svg viewBox="0 0 200 200" role="img" aria-label="' + t('scaleDetails.wheelAria') + '">' + parts.join('') + '</svg>';
    }

    function scaleChordEntries(root, ordered, height) {
      const chords = M.scaleChords(root, ordered, height != null ? height : 3);
      if (!chords.length) return [];
      const nm = names();
      const midis = tuningMidis();
      let lastRootMidi = null;
      return chords.map((ch) => {
        const analysisMidis = midisFromPcsAscending(ch.pcs);
        const r = M.analyzeChord(analysisMidis, ch.rootPc, {
          names: nm,
          compoundIntervals: state.compoundIntervals
        });
        // Climbing roots: each next diagram/play starts above the previous bass.
        const voicing = placeChordPcs(ch.pcs, midis, state.fretCount, lastRootMidi);
        if (voicing && voicing.length) {
          lastRootMidi = midis[voicing[0].s] + voicing[0].f;
        }
        return {
          degreeIndex: ch.degreeIndex,
          rootPc: ch.rootPc,
          pcs: ch.pcs,
          analysis: r,
          voicing: voicing,
          diagram: voicing ? chordDiagramSvg(voicing, midis.length, ch.rootPc, midis) : '',
          label: r ? (r.rootName + chordDisplaySuffix(r)) : nm[ch.rootPc]
        };
      });
    }

    function scaleChordSectionHtml(labelKey, spoilerKey, entries, ordered, family, modeIndex) {
      if (!entries.length) return '';
      const playLabel = t('scaleDetails.playChord');
      const seqLabel = t('scaleDetails.playSequence');
      const open = !!ui.scaleChordSpoilers[spoilerKey];
      return '<div class="sd-chords">' +
        '<details class="cr-shapes-spoiler"' + (open ? ' open' : '') + ' data-spoiler="' + spoilerKey + '">' +
          '<summary class="cr-shapes-summary">' + t(labelKey) +
            ' <span class="cr-shapes-count">(' + entries.length + ')</span>' +
          '</summary>' +
          '<div class="cr-shapes-body">' +
            '<div class="sd-chords-toolbar">' +
              '<button type="button" class="sd-chords-seq" title="' + seqLabel + '">' +
                '<span class="sd-chords-seq-icon">▶</span> ' + seqLabel +
              '</button>' +
            '</div>' +
            '<div class="sd-chords-row">' +
            entries.map((ch) => {
              const roman = degreeRoman(ordered[ch.degreeIndex], family, modeIndex);
              return '<div class="sd-chord-chip"' +
                ' data-pcs="' + ch.pcs.join(',') + '"' +
                ' data-root="' + ch.rootPc + '"' +
                (ch.voicing ? ' data-voicing="' + encodeVoicing(ch.voicing) + '"' : '') +
                '>' +
                '<button type="button" class="sd-chord-body" title="' + t('scaleDetails.chordGo', { name: ch.label }) + '">' +
                  '<span class="sd-chord-deg">' + roman + '</span>' +
                  (ch.diagram || '') +
                  '<span class="sd-chord-name-row">' +
                    '<span class="sd-chord-sym cr-name">' + chordSymbolStackHtml(ch.analysis, { hooks: false }) + '</span>' +
                  '</span>' +
                '</button>' +
                '<button type="button" class="sd-chord-play"' +
                  ' title="' + playLabel + '" aria-label="' + playLabel + '">▶</button>' +
                '</div>';
            }).join('') +
            '</div>' +
          '</div>' +
        '</details>' +
      '</div>';
    }

    function midiListFromChordChip(chip) {
      if (!chip) return null;
      const midis = tuningMidis();
      const voicing = decodeVoicing(chip.dataset.voicing);
      if (voicing && voicing.length) return voicing.map((nt) => midis[nt.s] + nt.f);
      const pcs = (chip.dataset.pcs || '').split(',').map(Number).filter((n) => Number.isFinite(n));
      return pcs.length ? midisFromPcsAscending(pcs) : null;
    }

    function playScaleChordChip(chip) {
      clearPlaying();
      const midiList = midiListFromChordChip(chip);
      if (midiList && midiList.length) playSimultaneous(midiList);
    }

    function playScaleChordSectionSequence(details) {
      if (!details) return;
      const chips = details.querySelectorAll('.sd-chord-chip');
      const lists = [];
      const chipEls = [];
      chips.forEach((chip) => {
        const list = midiListFromChordChip(chip);
        if (list && list.length) {
          lists.push(list);
          chipEls.push(chip);
        }
      });
      if (!lists.length) return;
      playBlocks(lists, null, (i) => {
        clearPlaying();
        if (i >= 0 && chipEls[i]) chipEls[i].classList.add('playing');
      });
    }

    function renderScaleDetails() {
      const el = $('#scale-details');
      const target = resolveTarget();
      if (!target) {
        el.classList.add('hidden');
        el.innerHTML = '';
        return;
      }
      const fam = M.SCALES[target.family];
      if (!fam) {
        el.classList.add('hidden');
        el.innerHTML = '';
        return;
      }
      el.classList.remove('hidden');
      const nm = names();
      const root = target.root;
      const modeIndex = target.modeIndex;
      const ordered = M.scaleOrdered(target.family, modeIndex);
      const modeName = fam.modes ? fam.modes[modeIndex] : null;
      const displayName = scaleDisplayName(target.family, modeIndex);

      const theory = theoryInfo(modeName, target.family);
      const mood = theory ? theory.mood : null;
      const desc = theory ? theory.desc : '';

      const title = nm[root] + ' ' + displayName;
      const parent = M.parentScale(target.family, modeIndex, root);
      const parentName = parent
        ? nm[parent.rootPc] + ' ' + scaleDisplayName(parent.family, parent.modeIndex)
        : '';
      const parentHtml = parent
        ? '<ul class="sd-bullets">' +
          '<li>' + t('scaleDetails.parentScale') + ' ' +
          '<button type="button" class="sd-parent-link"' +
          ' data-root="' + parent.rootPc + '"' +
          ' data-family="' + parent.family + '"' +
          ' data-mode="' + parent.modeIndex + '"' +
          ' title="' + t('scaleDetails.parentScaleGo', { name: parentName }) + '">' +
          parentName + '</button></li>' +
          '</ul>'
        : '';

      const len = ordered.length;
      const steps = [];
      for (let i = 0; i < len; i++) {
        const b = i + 1 < len ? ordered[i + 1] : ordered[0] + 12;
        steps.push(b - ordered[i]);
      }
      const stepHtml = steps.map((d) => {
        const lab = d === 1 ? t('scaleDetails.stepHalf') : d === 2 ? t('scaleDetails.stepWhole') : String(d);
        const cls = d === 1 ? 'sd-step H' : d === 2 ? 'sd-step W' : 'sd-step';
        return '<span class="' + cls + '">' + lab + '</span>';
      }).join('<span class="sd-step-join">·</span>');

      const degHtml = ordered.map((v, i) => {
        const pc = (root + v) % 12;
        const cls = i === 0 ? 'sd-root' : (i === 2 || i === 4 ? 'sd-chord' : 'sd-scale');
        return '<div class="sd-deg ' + cls + '">' +
          '<span class="sd-deg-num">' + degreeRoman(v, target.family, modeIndex) + '</span>' +
          '<span class="sd-deg-note">' + nm[pc] + '</span>' +
          '<span class="sd-deg-int">' + M.QUALITIES[v] + '</span>' +
          '</div>';
      }).join('');

      const formula = ordered.map((v) => FORMULA[v]).join(' · ');
      const charTxt = theory && theory.char ? theory.char : '';
      const charHtml = charTxt
        ? '<p class="sd-char">' + t('scaleDetails.characteristic') +
          '<button type="button" class="sd-char-link" data-theory-article="modes" title="' + t('scaleDetails.hookCharTitle', { interval: charTxt }) + '">' + charTxt + '</button>' +
          '</p>'
        : '';

      let relatedHtml = '';
      if (fam.modes && fam.modes.length > 1) {
        const parent = M.parentScale(target.family, modeIndex, root);
        const base = fam.base || M.scaleOrdered(target.family, 0);
        if (parent && base && base.length === fam.modes.length) {
          const chips = fam.modes.map((modeName, i) => {
            if (i === modeIndex) return '';
            const info = theoryInfo(modeName, target.family);
            const modeRoot = (parent.rootPc + (base[i] % 12) + 12) % 12;
            const char = info && info.char ? info.char : '';
            const label = names()[modeRoot] + ' ' + scaleDisplayName(target.family, i);
            const title = char
              ? t('scaleDetails.relatedTitle', { name: label, char: char })
              : t('scaleDetails.relatedTitlePlain', { name: label });
            return '<button type="button" class="sd-hook-chip" data-root="' + modeRoot + '" data-family="' + target.family + '" data-mode="' + i + '" title="' + title + '">' +
              '<span class="sd-hook-name">' + label + '</span>' +
              (char ? '<span class="sd-hook-char-lab">' + char + '</span>' : '') +
              '</button>';
          }).join('');
          if (chips.replace(/\s/g, '')) {
            relatedHtml =
              '<div class="sd-related">' +
              '<span class="sd-related-label">' + t('scaleDetails.related') + '</span>' +
              '<div class="sd-related-chips">' + chips + '</div>' +
              '</div>';
          }
        }
      }

      const learnHtml =
        '<div class="sd-learn">' +
        '<span class="sd-learn-label">' + t('scaleDetails.learn') + '</span>' +
        '<button type="button" class="sd-learn-btn" data-theory-article="intervals">' + t('scaleDetails.learnIntervals') + '</button>' +
        '<button type="button" class="sd-learn-btn" data-theory-article="modes">' + t('scaleDetails.learnModes') + '</button>' +
        '<button type="button" class="sd-learn-btn" data-theory-article="building">' + t('scaleDetails.learnBuilding') + '</button>' +
        '<button type="button" class="sd-learn-btn" data-theory-article="harmony">' + t('scaleDetails.learnHarmony') + '</button>' +
        '<button type="button" class="sd-learn-btn" data-theory-article="systems">' + t('legend.learnSystems') + '</button>' +
        '</div>';

      const chordEntries = scaleChordEntries(root, ordered, 3);
      const seventhEntries = scaleChordEntries(root, ordered, 4);
      const chordsHtml =
        scaleChordSectionHtml('scaleDetails.chords', 'chords', chordEntries, ordered, target.family, modeIndex) +
        scaleChordSectionHtml('scaleDetails.chords7', 'chords7', seventhEntries, ordered, target.family, modeIndex);

      el.innerHTML =
        '<div class="sd-head"><h3>' + title + '</h3></div>' +
        '<div class="sd-body">' +
          '<div class="sd-wheel">' + scaleWheelSvg(nm, root, ordered) + '</div>' +
          '<div class="sd-main">' +
            parentHtml +
            (mood ? '<p class="sd-mood">' + mood.join('  ·  ') + '</p>' : '') +
            '<p class="sd-desc">' + desc + '</p>' +
            charHtml +
            relatedHtml +
            '<p class="sd-formula">' + formula + '</p>' +
            '<div class="sd-steps">' + stepHtml + '</div>' +
            '<div class="sd-degrees">' + degHtml + '</div>' +
            chordsHtml +
            learnHtml +
          '</div>' +
        '</div>';
    }

    function bindScaleDetailsEvents() {
      $('#scale-details').addEventListener('click', (e) => {
        const seqBtn = e.target.closest ? e.target.closest('.sd-chords-seq') : null;
        if (seqBtn) {
          e.preventDefault();
          e.stopPropagation();
          const details = seqBtn.closest('details');
          playScaleChordSectionSequence(details);
          return;
        }
        const playBtn = e.target.closest ? e.target.closest('.sd-chord-play') : null;
        if (playBtn) {
          e.preventDefault();
          e.stopPropagation();
          playScaleChordChip(playBtn.closest('.sd-chord-chip'));
          return;
        }
        const chordBody = e.target.closest ? e.target.closest('.sd-chord-body') : null;
        const chordChip = chordBody
          ? chordBody.closest('.sd-chord-chip')
          : (e.target.closest ? e.target.closest('.sd-chord-chip') : null);
        if (chordChip && !e.target.closest('.sd-chord-play')) {
          e.preventDefault();
          const pcs = (chordChip.dataset.pcs || '').split(',').map(Number).filter((n) => Number.isFinite(n));
          const rootPc = parseInt(chordChip.dataset.root, 10);
          const voicing = decodeVoicing(chordChip.dataset.voicing);
          if (pcs.length && Number.isFinite(rootPc)) openScaleChordInAnalyzer(pcs, rootPc, voicing);
          return;
        }
        const link = e.target.closest ? e.target.closest('.sd-parent-link, .sd-hook-chip[data-family]') : null;
        if (link) {
          e.preventDefault();
          const rootPc = parseInt(link.dataset.root, 10);
          const family = link.dataset.family;
          const modeIndex = parseInt(link.dataset.mode, 10);
          if (!M.SCALES[family] || !Number.isFinite(rootPc)) return;
          if (state.tab === 'chords') {
            const ordered = M.scaleOrdered(family, modeIndex);
            const name = names()[rootPc] + ' ' + scaleDisplayName(family, modeIndex);
            ui.scaleSel = {
              rootPc: rootPc,
              name: name,
              ivs: ordered.slice(),
              family: family,
              modeIndex: modeIndex
            };
            if (ui.chordScaleFocus) {
              ui.chordScaleFocus = { rootPc: rootPc, family: family, modeIndex: modeIndex, name: name };
            }
          }
          applySharedScale(rootPc, family, modeIndex);
          return;
        }
        const learn = e.target.closest ? e.target.closest('[data-theory-article]') : null;
        if (learn && typeof openTheoryArticle === 'function') {
          e.preventDefault();
          openTheoryArticle(learn.getAttribute('data-theory-article'));
        }
      });

      $('#scale-details').addEventListener('toggle', (e) => {
        const det = e.target;
        if (!det || !det.classList || !det.classList.contains('cr-shapes-spoiler')) return;
        const key = det.getAttribute('data-spoiler');
        if (key && Object.prototype.hasOwnProperty.call(ui.scaleChordSpoilers, key)) {
          ui.scaleChordSpoilers[key] = !!det.open;
          saveState();
        }
      }, true);
    }

    return {
      renderScaleDetails: renderScaleDetails,
      bindScaleDetailsEvents: bindScaleDetailsEvents,
      resolveTarget: resolveTarget,
      scaleChordEntries: scaleChordEntries,
      clearPlaying: clearPlaying
    };
  }

  window.FretScaleDetails = {
    FORMULA: FORMULA,
    create: create
  };
})();
