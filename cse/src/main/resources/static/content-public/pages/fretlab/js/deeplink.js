(function (root) {
  /**
   * Shareable deep-link codec for Fretboard Lab.
   * URL overlay only — does not replace localStorage.
   *
   * Preferred form (hash, embed-safe):
   *   #r=0&f=major&m=3&l=caged
   *   #t=chords&pcs=0,4,7,11&cr=0
   *   #t=theory&a=building&pcs=0,4,7&cr=0
   *
   * Also accepts the same keys in location.search.
   */
  const LAYER_NONE = 'none';
  const TABS = { explore: 1, chords: 1, drills: 1, theory: 1 };

  function parseQueryString(raw) {
    let s = String(raw || '');
    if (s.charAt(0) === '#' || s.charAt(0) === '?') s = s.slice(1);
    if (!s) return null;
    let params;
    try {
      params = new URLSearchParams(s);
    } catch (e) {
      return null;
    }
    const out = { ver: 1 };
    const ver = params.get('ver');
    if (ver != null && ver !== '') {
      const v = parseInt(ver, 10);
      if (Number.isFinite(v)) out.ver = v;
    }

    const t = params.get('t');
    if (t && TABS[t]) out.t = t;

    const r = params.get('r');
    if (r != null && r !== '') {
      const n = parseInt(r, 10);
      if (Number.isFinite(n)) out.r = ((n % 12) + 12) % 12;
    }

    const f = params.get('f');
    if (f) out.f = String(f);

    const m = params.get('m');
    if (m != null && m !== '') {
      const n = parseInt(m, 10);
      if (Number.isFinite(n) && n >= 0) out.m = n | 0;
    }

    const l = params.get('l');
    if (l === LAYER_NONE || l === 'caged' || l === 'quartal' || l === 'nps' || l === 'berklee') {
      out.l = l;
    }

    const spell = params.get('spell');
    if (spell === 'fit' || spell === 'flat' || spell === 'sharp') out.spell = spell;

    ['names', 'degrees', 'intervals'].forEach((k) => {
      const v = params.get(k);
      if (v === '0' || v === '1') out[k] = v === '1';
    });

    const pcsRaw = params.get('pcs');
    if (pcsRaw) {
      const pcs = pcsRaw.split(',').map((x) => parseInt(x, 10)).filter((n) => Number.isFinite(n));
      if (pcs.length) out.pcs = pcs.map((n) => ((n % 12) + 12) % 12);
    }

    const cr = params.get('cr');
    if (cr != null && cr !== '') {
      const n = parseInt(cr, 10);
      if (Number.isFinite(n)) out.cr = ((n % 12) + 12) % 12;
    }

    const a = params.get('a');
    if (a) out.a = String(a).slice(0, 64);

    const vox = params.get('vox');
    if (vox) out.vox = String(vox).slice(0, 200);

    return out;
  }

  function isDefaultExplore(slice) {
    if (!slice) return true;
    if (slice.t && slice.t !== 'explore') return false;
    if (slice.a || (slice.pcs && slice.pcs.length) || slice.vox) return false;
    if (slice.l && slice.l !== LAYER_NONE) return false;
    if (slice.spell && slice.spell !== 'sharp') return false;
    if (slice.names === false || slice.degrees === false || slice.intervals === true) return false;
    if (slice.f && slice.f !== 'major') return false;
    if (slice.m != null && slice.m !== 0) return false;
    if (slice.r != null && slice.r !== 0) return false;
    return true;
  }

  function hasShareable(slice) {
    if (!slice || typeof slice !== 'object') return false;
    if (isDefaultExplore(slice)) return false;
    if (slice.pcs && slice.pcs.length) return true;
    if (slice.vox) return true;
    if (slice.a) return true;
    if (slice.t && slice.t !== 'explore') return true;
    if (slice.f && slice.f !== 'major') return true;
    if (slice.r != null && slice.r !== 0) return true;
    if (slice.m != null && slice.m !== 0) return true;
    if (slice.l && slice.l !== LAYER_NONE) return true;
    if (slice.spell && slice.spell !== 'sharp') return true;
    if (slice.names === false || slice.degrees === false || slice.intervals === true) return true;
    return false;
  }

  function parseLocation(loc) {
    loc = loc || (typeof root.location !== 'undefined' ? root.location : null);
    if (!loc) return null;
    const fromHash = parseQueryString(loc.hash || '');
    if (hasShareable(fromHash)) return fromHash;
    const fromSearch = parseQueryString(loc.search || '');
    if (hasShareable(fromSearch)) return fromSearch;
    return null;
  }

  function layerFromState(state) {
    if (!state) return LAYER_NONE;
    if (state.showCagedShapes) return 'caged';
    if (state.showQuartal) return 'quartal';
    if (state.showNps) return 'nps';
    if (state.showBerklee) return 'berklee';
    return LAYER_NONE;
  }

  /**
   * Build a compact shareable slice from live app state.
   * opts.articleId — current Theory article id
   * opts.encodeVoicing — fn(notes) → string
   */
  function sliceFromState(state, opts) {
    opts = opts || {};
    if (!state) return null;
    const slice = { ver: 1 };
    const tab = state.tab;
    if (TABS[tab]) slice.t = tab;

    if (Number.isFinite(state.scaleKey)) slice.r = ((state.scaleKey % 12) + 12) % 12;
    if (state.scaleFamily) slice.f = state.scaleFamily;
    if (Number.isFinite(state.modeIndex)) slice.m = state.modeIndex | 0;

    const layer = layerFromState(state);
    if (layer !== LAYER_NONE) slice.l = layer;

    if (state.noteSpell === 'fit' || state.noteSpell === 'flat' || state.noteSpell === 'sharp') {
      if (state.noteSpell !== 'sharp') slice.spell = state.noteSpell;
    }

    if (state.showNames === false) slice.names = false;
    if (state.showDegrees === false) slice.degrees = false;
    if (state.showIntervals) slice.intervals = true;

    if (tab === 'theory' && opts.articleId) slice.a = String(opts.articleId);

    const notes = state.chordNotes;
    if (notes && notes.length) {
      if (typeof opts.encodeVoicing === 'function') {
        const vox = opts.encodeVoicing(notes);
        if (vox) slice.vox = vox;
      }
      const midis = typeof opts.tuningMidis === 'function' ? opts.tuningMidis() : null;
      if (midis && midis.length) {
        const pcs = [];
        const seen = {};
        notes.forEach((nt) => {
          const pc = (((midis[nt.s] + nt.f) % 12) + 12) % 12;
          if (seen[pc]) return;
          seen[pc] = true;
          pcs.push(pc);
        });
        if (pcs.length) slice.pcs = pcs;
      }
      if (state.pinRoot && midis && midis.length) {
        slice.cr = (((midis[state.pinRoot.s] + state.pinRoot.f) % 12) + 12) % 12;
      } else if (slice.pcs && slice.pcs.length) {
        slice.cr = slice.pcs[0];
      }
    }

    return hasShareable(slice) ? slice : null;
  }

  function build(slice) {
    if (!hasShareable(slice)) return '';
    const p = new URLSearchParams();
    p.set('ver', String(slice.ver != null ? slice.ver : 1));
    if (slice.t && slice.t !== 'explore') p.set('t', slice.t);
    if (slice.r != null) p.set('r', String(slice.r));
    if (slice.f) p.set('f', slice.f);
    if (slice.m != null) p.set('m', String(slice.m));
    if (slice.l && slice.l !== LAYER_NONE) p.set('l', slice.l);
    if (slice.spell && slice.spell !== 'sharp') p.set('spell', slice.spell);
    if (slice.names === false) p.set('names', '0');
    if (slice.degrees === false) p.set('degrees', '0');
    if (slice.intervals === true) p.set('intervals', '1');
    if (slice.a) p.set('a', slice.a);
    if (slice.pcs && slice.pcs.length) p.set('pcs', slice.pcs.join(','));
    if (slice.cr != null) p.set('cr', String(slice.cr));
    if (slice.vox) p.set('vox', slice.vox);
    const s = p.toString();
    return s ? '#' + s : '';
  }

  root.FretDeepLink = {
    parseQueryString: parseQueryString,
    parseLocation: parseLocation,
    hasShareable: hasShareable,
    sliceFromState: sliceFromState,
    layerFromState: layerFromState,
    build: build
  };
})(typeof self !== 'undefined' ? self : this);
