(function () {
  const BLACK_PCS = { 1: true, 3: true, 6: true, 8: true, 10: true };
  const OCTAVE_SPAN = 48;

  function create(deps) {
    const $ = deps.$;
    const t = deps.t;
    const M = deps.M;
    const state = deps.state;
    const names = deps.names;
    const degreeRoman = deps.degreeRoman;
    const rootPc = deps.rootPc;
    const tuningMidis = deps.tuningMidis;
    const playMidi = deps.playMidi;
    const showTip = deps.showTip;
    const hideTip = deps.hideTip;
    const tipRow = deps.tipRow;
    const render = deps.render;

    const piano = $('#piano');
    let playFlashTimer = 0;

    function isBlack(midi) {
      return !!BLACK_PCS[((midi % 12) + 12) % 12];
    }

    function lowestOpenMidi() {
      const midis = tuningMidis();
      if (!midis || !midis.length) return 40;
      let low = midis[0];
      for (let i = 1; i < midis.length; i++) {
        if (midis[i] < low) low = midis[i];
      }
      return low;
    }

    function rangeStart(root) {
      const pc = ((root % 12) + 12) % 12;
      let start = lowestOpenMidi();
      while (((start % 12) + 12) % 12 !== pc) start++;
      return start;
    }

    function scaleContext() {
      const root = rootPc();
      const ordered = M.scaleOrdered(state.scaleFamily, state.modeIndex) || [0];
      const pcs = new Set(ordered.map((v) => (root + v) % 12));
      let highlightPc = root;
      if (state.tab === 'explore' && state.highlightDegree >= 0) {
        highlightPc = (root + state.highlightDegree) % 12;
      }
      return { root: root, pcs: pcs, highlightPc: highlightPc };
    }

    function keyClass(pc, ctx) {
      const inScale = ctx.pcs.has(pc);
      if (inScale && pc === ctx.highlightPc) return 'root';
      if (inScale) return 'scale';
      if (state.fullChart) return 'plain';
      return 'out';
    }

    function tipHtmlFor(midi, pc, ctx) {
      const dash = t('tip.emDash');
      const nm = names();
      const intIdx = (pc - ctx.root + 12) % 12;
      const inScale = ctx.pcs.has(pc);
      const octaveName = nm[pc] + (Math.floor(midi / 12) - 1);
      const deg = degreeRoman(intIdx);
      const iv = M.qualityFull(intIdx);
      return tipRow(t('tip.note'), octaveName) +
        tipRow(t('tip.degree'), deg, pc === ctx.root && inScale ? 'tr' : '') +
        tipRow(t('tip.interval'), iv || dash, 'ti');
    }

    function flashKey(el) {
      if (!el) return;
      el.classList.add('playing');
      if (playFlashTimer) clearTimeout(playFlashTimer);
      playFlashTimer = setTimeout(() => {
        playFlashTimer = 0;
        if (piano) piano.querySelectorAll('.piano-key.playing').forEach((k) => k.classList.remove('playing'));
      }, 160);
    }

    function playKey(el) {
      if (!el || !el.dataset.midi) return;
      const midi = +el.dataset.midi;
      playMidi(midi);
      flashKey(el);
    }

    function renderPiano() {
      if (!piano) return;
      piano.classList.remove('hidden');
      piano.classList.toggle('piano-collapsed', !state.showPiano);

      const head =
        '<div class="piano-head">' +
        '<span class="piano-title">' + t('controls.piano.title') + '</span>' +
        '<label class="switch" title="' + t('controls.piano.toggle') + '">' +
        '<input type="checkbox" aria-label="' + t('controls.piano.toggle') + '"' +
        (state.showPiano ? ' checked' : '') + '>' +
        '<span class="switch-track" aria-hidden="true"></span>' +
        '</label>' +
        (state.showPiano ? '<span class="piano-range">' + t('controls.piano.rangeHint') + '</span>' : '') +
        '</div>';

      if (!state.showPiano) {
        piano.innerHTML = head;
        return;
      }

      const ctx = scaleContext();
      const start = rangeStart(ctx.root);
      const end = start + OCTAVE_SPAN;
      const midis = [];
      for (let m = start; m <= end; m++) midis.push(m);

      const whites = midis.filter((m) => !isBlack(m));
      const blacks = midis.filter((m) => isBlack(m));
      const whiteW = 100 / whites.length;
      const blackW = whiteW * 0.62;
      const nm = names();

      const whiteHtml = whites.map((midi) => {
        const pc = ((midi % 12) + 12) % 12;
        const cls = keyClass(pc, ctx);
        const label = state.showNames ? nm[pc] : '';
        return '<button type="button" class="piano-key white ' + cls + '" data-midi="' + midi + '" data-pc="' + pc + '"' +
          ' aria-label="' + nm[pc] + (Math.floor(midi / 12) - 1) + '">' +
          (label ? '<span class="piano-label">' + label + '</span>' : '') +
          '</button>';
      }).join('');

      const blackHtml = blacks.map((midi) => {
        const pc = ((midi % 12) + 12) % 12;
        const cls = keyClass(pc, ctx);
        const wi = whites.filter((w) => w < midi).length;
        const left = wi * whiteW - blackW / 2;
        return '<button type="button" class="piano-key black ' + cls + '" data-midi="' + midi + '" data-pc="' + pc + '"' +
          ' style="left:' + left + '%;width:' + blackW + '%"' +
          ' aria-label="' + nm[pc] + (Math.floor(midi / 12) - 1) + '"></button>';
      }).join('');

      piano.innerHTML = head +
        '<div class="piano-keys" role="group" aria-label="' + t('controls.piano.aria') + '">' +
        '<div class="piano-whites">' + whiteHtml + '</div>' +
        '<div class="piano-blacks">' + blackHtml + '</div>' +
        '</div>';
    }

    function bindPianoEvents() {
      if (!piano || piano.dataset.bound) return;
      piano.dataset.bound = '1';

      piano.addEventListener('change', (e) => {
        const input = e.target;
        if (!input || input.type !== 'checkbox' || !input.closest('.piano-head .switch')) return;
        state.showPiano = !!input.checked;
        render();
      });

      piano.addEventListener('pointerdown', (e) => {
        if (e.target.closest('.switch')) return;
        const key = e.target.closest('.piano-key');
        if (!key || !piano.contains(key)) return;
        e.preventDefault();
        playKey(key);
      });

      piano.addEventListener('pointerover', (e) => {
        const key = e.target.closest('.piano-key');
        if (!key || !piano.contains(key)) return;
        if (e.pointerType === 'touch') return;
        const midi = +key.dataset.midi;
        const pc = +key.dataset.pc;
        showTip(tipHtmlFor(midi, pc, scaleContext()), e.clientX, e.clientY);
      });

      piano.addEventListener('pointermove', (e) => {
        const key = e.target.closest('.piano-key');
        if (!key || !piano.contains(key)) return;
        if (e.pointerType === 'touch') return;
        if (deps.tipVisible && deps.tipVisible()) deps.positionTip(e.clientX, e.clientY);
      });

      piano.addEventListener('pointerleave', () => {
        hideTip();
      });
    }

    return {
      renderPiano: renderPiano,
      bindPianoEvents: bindPianoEvents
    };
  }

  window.FretPianoView = { create: create };
})();
